package crawler;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonParseException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxBinary;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import cn.edu.hfut.dmic.webcollector.crawldb.DBManager;
import cn.edu.hfut.dmic.webcollector.crawler.Crawler;
import cn.edu.hfut.dmic.webcollector.fetcher.Executor;
import cn.edu.hfut.dmic.webcollector.model.CrawlDatum;
import cn.edu.hfut.dmic.webcollector.model.CrawlDatums;
import cn.edu.hfut.dmic.webcollector.plugin.berkeley.BerkeleyDBManager;
import db.MyDB;
import db.Policy;
import edu.umass.cs.benchlab.har.HarEntries;
import edu.umass.cs.benchlab.har.HarEntry;
import edu.umass.cs.benchlab.har.HarLog;
import edu.umass.cs.benchlab.har.HarWarning;
import edu.umass.cs.benchlab.har.tools.HarFileReader;
import extractor.ExtractResult;
import extractor.PolicyExtractor;
import whois.DomainParser;
import whois.Tld;

public class PPCrawler {
	private static final int MAX_RETRY = 3;
	private static MyLogger logger;
	private MyDB db;
	private int topN;
	private int threads;
	private int crawlingDepth;
	private String crawlerDir;
	private Map<String, String> host;
	private final DomainParser parser = new DomainParser();
	static {
		// turn off Selenium logging
		Logger logger = Logger.getLogger("com.gargoylesoftware.htmlunit");
		logger.setLevel(Level.OFF);

	}

	public PPCrawler(String crawlerDir) {
		logger = new MyLogger(crawlerDir);
		this.setCrawlerDir(crawlerDir);
		topN = 100;
		threads = 20;
		crawlingDepth = 1;
		getWhoisHost();
		db = new MyDB();
	}

	public PPCrawler(String crawlerDir, int topN, int threads, int crawlingDepth) {
		logger = new MyLogger(crawlerDir);
		this.setCrawlerDir(crawlerDir);
		this.topN = topN;
		this.threads = threads;
		this.crawlingDepth = crawlingDepth;
		getWhoisHost();
		db = new MyDB();
	}

	public int getCrawlingDepth() {
		return crawlingDepth;
	}

	public void setCrawlingDepth(int crawlingDepth) {
		this.crawlingDepth = crawlingDepth;
	}

	public int getThreads() {
		return threads;
	}

	public void setThreads(int threads) {
		this.threads = threads;
	}

	public int getTopN() {
		return topN;
	}

	public void setTopN(int topN) {
		this.topN = topN;
	}

	/**
	 * Read whois server list. Path: resources/whois_server_list.txt TODO add a
	 * config.xml
	 */
	private void getWhoisHost() {
		try (Stream<String> stream = Files.lines(Paths.get(Tld.class.getResource("/whois_server_list.txt").toURI()))) {
			List<Tld> tlds = new ArrayList<Tld>();
			stream.forEach((record) -> {
				String[] r = record.split(" ");
				tlds.add(new Tld(r[0], r[1]));
			});
			host = tlds.stream().collect(Collectors.toMap(Tld::getTld, Tld::getWhoisHost));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

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

	/**
	 * Using Firebug and Har Export Trigger to capture driver traffic TODO
	 * absolute binary path of firefox 46.0
	 * 
	 * @param driver
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public List<String> findLinksByProxy(String url) {
		List<String> links = new ArrayList<String>();
		String binPath = "/Users/jero/codebase/project/libs/Firefox.app/Contents/MacOS/firefox";
		String harPath = System.getProperty("user.dir") + "/" + PPCrawler.logger.getHarDir();

		FirefoxBinary Binary = new FirefoxBinary(new File(binPath));

		// int retry = 0;
		// do {
		for (int retry = 0; retry < MAX_RETRY; retry++) {
			FirefoxProfile profile = new FirefoxProfile();
			setNetworkMonitorProfile(profile, harPath);
			WebDriver driver = new FirefoxDriver(Binary, profile);
			driver.manage().deleteAllCookies();
			try {

				Thread.sleep(1500); // allow firebug to load its net panel
				driver.get(url);
				Thread.sleep(20000); // while firebug is exporting HAR
				List<HarEntry> entries = readHar(harPath);
				for (HarEntry e : entries) {
					links.add(extractDomain(e.getRequest().getUrl()));
					// System.out.println("!!! " +
					// extractDomain(e.getRequest().getUrl()));
				}
				retry = MAX_RETRY;
			} catch (Exception ie) {
				ie.printStackTrace();
				// clean har dir
				try {
					Thread.sleep(10000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				logger.cleanDir(logger.getHarDir());
			}
			driver.quit();
		}
		// } while (retry++ < MAX_RETRY);

		return links;

	}

	/**
	 * Extract domain of given url(filter out the requesting resources)
	 * 
	 * @param url
	 * @return
	 */
	public String extractDomain(String url) {
		String[] str = url.split("//");

		// return str[0] + "//" +
		// parser.getRegistableDomain(str[1].replaceAll("/.*", ""));
		return str[0] + "//" + str[1].replaceAll("/.*", "");

	}

	/**
	 * 
	 * @param url
	 *            Format: http://www.example.com
	 * @param ifFirefox
	 *            whether using firefox(false by default)
	 */
	public void gather(String url, boolean ifFirefox) {
		Executor executor = new Executor() {
			public void execute(CrawlDatum datum, CrawlDatums next) throws Exception {
				String currentUrl = datum.getUrl();
				PolicyExtractor extractor = new PolicyExtractor(ifFirefox);
				extractor.extract(currentUrl);
				extractor.quit();
				storeResult(url, currentUrl, extractor.getExtractResult());

			}
		};

		// create a BerkeleyDBManager
		DBManager manager = new BerkeleyDBManager("crawl");
		// creating a crawler object need DBManager and executor
		Crawler crawler = new Crawler(manager, executor);
		// String url = "http://www.bbc.co.uk";

		crawler.addSeed(url);// add main site
		List<String> links = findLinksByProxy(url);

		for (String l : links) {
			// System.out.println("Add: "+l);
			logger.toLog("hreflog.txt", "Add: " + l);
			crawler.addSeed(l);
		}
		logger.toLog("hreflog.txt", "Total: " + links.size());
		try {
			crawler.setThreads(30);
			crawler.start(1);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Read all har file(suppress warning) under given path and return url in
	 * har entries
	 * 
	 * @param path
	 * @return
	 * @throws IOException
	 */
	public List<HarEntry> readHar(String path) throws Exception {
		// System.out.println(path);
		List<HarEntry> entry = null;
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(path))) {
			System.out.println(path);

			for (Path s : stream) {
				HarFileReader r = new HarFileReader();
				// read har file and suppress warnings
				List<HarWarning> warnings = new ArrayList<HarWarning>();
				System.out.println("Reading: " + s.toUri());
				HarLog log = r.readHarFile(s.toFile(), warnings);

				// HarLog log=r.readHarFile(new
				// File("Autoexport_16y0701_114203.har"),warnings);
				HarEntries entries = log.getEntries();
				entry = entries.getEntries();
				for (HarWarning w : warnings) {
					System.err.println("Warning: " + w + "in " + path);
				}
			}
		} catch (JsonParseException e) {
			System.err.println("Parsing: " + path + " fail");
		} catch (DirectoryIteratorException | IOException ex) {
			// I/O error encounted during the iteration, the cause is an
			ex.printStackTrace();
		}

		return entry;
	}

	private void storeResult(String mainUrl, String connectingUrl, ExtractResult result) {
		String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH:mm:ss"));
		switch (result.getStatus()) {
		case SUCCESS:
			logger.toLog("hreflog.txt", connectingUrl + " Success");
			// try {
			// logger.toFile(URLEncoder.encode(connectingUrl, "UTF-8") + ".txt",
			// result.getPolicyUrl() + "\n" + result.getPolicy());
			// } catch (UnsupportedEncodingException e) {
			// // TODO Auto-generated catch block
			// e.printStackTrace();
			// }
			db.insert(new Policy(mainUrl, connectingUrl, result.getPolicyUrl(), result.getPolicy(), 0, "", timestamp));
			break;
		case REFERTOWHOIS:
			logger.toLog("hreflog.txt", connectingUrl + " Refer to " + result.getWhoisResult().getWhoisReport());
			db.insert(new Policy(mainUrl, connectingUrl, result.getPolicyUrl(), result.getPolicy(), 1,
					result.getWhoisResult().getRegistrantUrl(), timestamp));
			break;
		default:
			logger.toLog("hreflog.txt", connectingUrl + " Fail");
			db.insert(new Policy(mainUrl, connectingUrl, "", "", 0, "", timestamp));

		}
	}

	public void gathering(String url, int nThreads) {
		// ExecutorService es = Executors.newFixedThreadPool(nThreads);
		ExecutorService es = Executors.newWorkStealingPool();

		List<Future<?>> future = new ArrayList<Future<?>>();

		// for (String url : urls) {
		List<String> links = findLinksByProxy(url);// new browser
		for (String link : links) {
			Runnable task = () -> {
				// new browser dedicates to one mainUrl
				PolicyExtractor extractor = new PolicyExtractor(true);
				extractor.extract(link);
				extractor.quit();
				storeResult(url, link, extractor.getExtractResult());
			};
			future.add(es.submit(task));
		}
		// wait for all task complete
		for (Future<?> f : future) {
			try {
				f.get();
			} catch (InterruptedException | ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		// Attemp to shutdown
		try {
			System.out.println("attempt to shutdown executor");
			es.shutdown();
			es.awaitTermination(5, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			System.err.println("tasks interrupted");
		} finally {
			if (!es.isTerminated()) {
				System.err.println("cancel non-finished tasks");
			}
			es.shutdownNow();
			System.out.println("shutdown finished");
		}
	}

	public static void main(String[] args) throws IOException {
		// choose top 100
		// List<String> list =
		// Files.lines(Paths.get(PPCrawler.class.getResource("/list.txt").getPath())).skip(20).limit(20)
		// .collect(Collectors.toList());
		
		// choose random 100 from top 1000
		List<String> totalList = Files.lines(Paths.get(PPCrawler.class.getResource("/top1000.txt").getPath()))
				.collect(Collectors.toList());
		//boxed map int to Integer
		List<Integer> indexs=new Random().ints(1,1001).distinct().limit(100).boxed().collect(Collectors.toList());
		List<String> list=new ArrayList<String>();
		
		for(Integer index:indexs){
			System.out.println(index.intValue()+1+"\t"+totalList.get(index.intValue()));
			list.add(totalList.get(index.intValue()));
		}
		int count = 0;
		for (String url : list) {
			System.out.println(count++ + "\t" + url);

			// PPCrawler ppc = new PPCrawler("links/" + url);
			// ppc.gather("http://" + url, true);
			// ppc.gathering("http://" + url, 20);

		}
//		String url = "bestbuy.com";
//		PPCrawler ppc = new PPCrawler("links/" + url);
//		ppc.gather("http://" + url, true);
	}

	public String getCrawlerDir() {
		return crawlerDir;
	}

	public void setCrawlerDir(String crawlerDir) {
		this.crawlerDir = crawlerDir;
	}
}
