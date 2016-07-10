package db;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.codehaus.jackson.JsonParseException;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxBinary;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.remote.DesiredCapabilities;

import crawler.PPCrawler;
import edu.umass.cs.benchlab.har.HarEntries;
import edu.umass.cs.benchlab.har.HarEntry;
import edu.umass.cs.benchlab.har.HarLog;
import edu.umass.cs.benchlab.har.HarWarning;
import edu.umass.cs.benchlab.har.tools.HarFileReader;
import extractor.PolicyExtractor;
import whois.DomainParser;

public class Crawler {
	public void setNetworkMonitorProfile(FirefoxProfile profile, String harPath) {
		// FirefoxProfile profile = new FirefoxProfile();

		// Load extensions(adjust add-ons path)
		File harExport = new File(PPCrawler.class.getResource("/firefox/harexporttrigger.xpi").getFile());
		try {
			profile.addExtension(harExport);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Enable the automation without having a new HAR file created for every
		// loaded page.
		profile.setPreference("extensions.netmonitor.har.enableAutomation", true);
		// Set to a token that is consequently passed into all HAR API calls to
		// verify the user.
		profile.setPreference("extensions.netmonitor.har.contentAPIToken", "test");
		// Set if you want to have the HAR object available without the
		// developer toolbox being open.
		profile.setPreference("extensions.netmonitor.har.autoConnect", true);

		// Enable netmonitor
		profile.setPreference("devtools.netmonitor.enabled", true);
		// If set to true the final HAR file is zipped. This might represents
		// great disk-space optimization especially if HTTP response bodies are
		// included.
		profile.setPreference("devtools.netmonitor.har.compress", false);
		// Default name of the target HAR file. The default file name supports
		// formatters
		profile.setPreference("devtools.netmonitor.har.defaultFileName", "Autoexport_%yy%m%d_%H%M%S");
		// Default log directory for generate HAR files. If empty all
		// automatically generated HAR files are stored in <FF-profile>/har/logs
		profile.setPreference("devtools.netmonitor.har.defaultLogDir", harPath);
		// If true, a new HAR file is created for every loaded page
		// automatically.
		profile.setPreference("devtools.netmonitor.har.enableAutoExportToFile", true);
		// The result HAR file is created even if there are no HTTP requests.
		profile.setPreference("devtools.netmonitor.har.forceExport", true);
		// If set to true, HTTP response bodies are also included in the HAR
		// file (can produce significantly bigger amount of data).
		profile.setPreference("devtools.netmonitor.har.includeResponseBodies", false);
		// If set to true the export format is HARP (support for JSONP syntax
		// that is easily transferable cross domains)
		profile.setPreference("devtools.netmonitor.har.jsonp", false);
		// Default name of JSONP callback (used for HARP format)
		profile.setPreference("devtools.netmonitor.har.jsonpCallback", false);
		// Amount of time [ms] the auto-exporter should wait after the last
		// finished request before exporting the HAR file.
		profile.setPreference("devtools.netmonitor.har.pageLoadedTimeout", "2500");

		// return profile;
	}

	public void Secondary() throws IOException {
		// Configure mybatis from mybatis-config.xml
		SqlSessionFactoryBuilder builder = new SqlSessionFactoryBuilder();
		System.out.println(Resources.getResourceURL("mybatis"));
		SqlSessionFactory factory = builder.build(Crawler.class.getResourceAsStream("/mybatis/mybatis-config.xml"));// rely
																													// on
																													// mybatis
		// ExecutorService executor = Executors.newFixedThreadPool(20);
		ExecutorService executor = Executors.newWorkStealingPool();
		List<Future<?>> future = new ArrayList<Future<?>>();
		List<Policy> list;
		try (SqlSession session = factory.openSession()) {
			list = session.selectList("Policy.getFailed", 1);
			session.commit();
		}
		int c = 0;
		for (Policy p : list) {
			System.out.println(c++ + "\t" + p.getId() + "\t" + p.getMainsite() + "\t" + p.getConnectingUrl() + "\t"
					+ p.getPolicyUrl());
			Runnable task = () -> {
				PolicyExtractor extractor = new PolicyExtractor(true);
				extractor.extract(p.getConnectingUrl());
				p.setPolicyUrl(extractor.getPolicyUrl());
				p.setPolicy(extractor.getPolicy());
				extractor.quit();
				try (SqlSession session = factory.openSession()) {
					session.update("Policy.updatePolicyUrlById", p);
					session.commit();
				}

			};
			// executor.execute(task);
			// callables.add(task);
			future.add(executor.submit(task));
		}
		for (Future<?> f : future) {
			try {
				f.get();
			} catch (InterruptedException | ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		try {
			System.out.println("attempt to shutdown executor");
			executor.shutdown();
			executor.awaitTermination(5, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			System.err.println("tasks interrupted");
		} finally {
			if (!executor.isTerminated()) {
				System.err.println("cancel non-finished tasks");
			}
			executor.shutdownNow();
			System.out.println("shutdown finished");
		}
		c = 0;
	}

	public void update() {
		/*
		 * try (BufferedWriter bw =
		 * Files.newBufferedWriter(Paths.get("test.txt"),
		 * StandardOpenOption.APPEND)) { PolicyExtractor extractor = new
		 * PolicyExtractor(true); for (Policy p : list) {
		 * 
		 * extractor.extract(p.getConnectingUrl()); bw.write(c++ + "\t" +
		 * p.getId() + "\t" + p.getMainsite() + "\t" + p.getConnectingUrl() +
		 * "\t" + extractor.getPolicyUrl()); bw.newLine();
		 * System.out.println(c++ + "\t" + p.getId() + "\t" + p.getMainsite() +
		 * "\t" + p.getConnectingUrl() + "\t" + extractor.getPolicyUrl()); }
		 * extractor.quit(); }
		 */
		Policy p = new Policy("", "", "fds", "", 0, "", "");
		p.setId(1804);
		// System.out.println(session.update("Policy.updatePolicyUrlByID", p));
		// session.commit();
		// session.close();
	}

	public static List<HarEntry> readHar(String path) {
		// System.out.println(path);
		List<HarEntry> entry = null;
		// try (BufferedReader br=Files.newBufferedReader(Paths.get(path))) {
		try (InputStream s = Files.newInputStream(Paths.get(path))) {

			HarFileReader r = new HarFileReader();
			List<HarWarning> warnings = new ArrayList<HarWarning>();
			HarLog log = r.readHarFile(s, warnings);
			for (HarWarning w : warnings)
				System.out.println("File:" + path + " - Warning:" + w);
			HarEntries entries = log.getEntries();
			entry = entries.getEntries();
			int c = 0;
			for (HarEntry e : entry) {
				System.out.println(c++ + "\t" + e.getRequest().getUrl());
			}
		} catch (JsonParseException e) {
			e.printStackTrace();
		}
		// }
		// } catch (DirectoryIteratorException | IOException ex) {
		// // I/O error encounted during the iteration, the cause is an
		// ex.printStackTrace();
		// }
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return entry;
	}

	public static void main(String args[]) throws IOException {
		String binPath = "/Users/jero/codebase/project/libs/Firefox.app/Contents/MacOS/firefox";
		FirefoxBinary Binary = new FirefoxBinary(new File(binPath));
		FirefoxProfile profile = new FirefoxProfile();
		profile.setPreference("intl.accept_languages", "en-US");
		 WebDriver driver=new FirefoxDriver(Binary,profile);
		 driver.manage().deleteAllCookies();
		 
		 driver.get("http://www.pornhub.com/information#privacy");
		 try {
			Thread.sleep(15000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println(driver.getCurrentUrl());
		//System.out.println(driver.getPageSource());
		Files.write(Paths.get("pornhub.html"), driver.getPageSource().getBytes());
//		SqlSessionFactoryBuilder builder;
//		SqlSessionFactory factory;
//
//		// Configure mybatis from mybatis-config.xml
//		builder = new SqlSessionFactoryBuilder();
//		factory = builder.build(Crawler.class.getResourceAsStream("/mybatis/mybatis-config.xml"));
//		SqlSession session=factory.openSession();
//		Policy p=session.selectOne("Policy.selectById",26);
//		session.commit();
//		session.close();
//		System.out.println(p.getId()+"\n"+p.getPolicy());
		// driver.quit();
	}
}
