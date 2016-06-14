package crawler;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.apache.ibatis.io.Resources;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxBinary;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import com.fasterxml.jackson.core.JsonParseException;
import cn.edu.hfut.dmic.webcollector.crawldb.DBManager;
import cn.edu.hfut.dmic.webcollector.crawler.Crawler;
import cn.edu.hfut.dmic.webcollector.fetcher.Executor;
import cn.edu.hfut.dmic.webcollector.model.CrawlDatum;
import cn.edu.hfut.dmic.webcollector.model.CrawlDatums;
import cn.edu.hfut.dmic.webcollector.plugin.berkeley.BerkeleyDBManager;
import edu.umass.cs.benchlab.har.HarBrowser;
import edu.umass.cs.benchlab.har.HarEntries;
import edu.umass.cs.benchlab.har.HarEntry;
import edu.umass.cs.benchlab.har.HarLog;
import edu.umass.cs.benchlab.har.HarWarning;
import edu.umass.cs.benchlab.har.tools.HarFileReader;
import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.BrowserMobProxyServer;
import net.lightbody.bmp.client.ClientUtil;
import net.lightbody.bmp.core.har.Har;
import net.lightbody.bmp.proxy.CaptureType;

public class PPCrawler {
	private static MyLogger logger;
	private int topN;
	private int threads;
	private int crawlingDepth;

	static {
		// turn off Selenium logging
		Logger logger = Logger.getLogger("com.gargoylesoftware.htmlunit");
		logger.setLevel(Level.OFF);
	}

	public PPCrawler(String crawlerDir) {
		logger = new MyLogger(crawlerDir);
		topN = 100;
		threads = 20;
		crawlingDepth = 1;
	}

	public PPCrawler(String crawlerDir, int topN, int threads, int crawlingDepth) {
		logger = new MyLogger(crawlerDir);
		this.topN = topN;
		this.threads = threads;
		this.crawlingDepth = crawlingDepth;
	}

	/**
	 * Trivially search for link for privacy policy of driver's currently
	 * visiting websites.
	 * 
	 * @param driver
	 * @return link of privacy policy, null if not found
	 */
	public static String getPolicyHref(WebDriver driver) {
		String[] terms = { "Privacy", "Privacy Policy" };
		for (int i = 0; i < terms.length; i++) {
			List<WebElement> list = driver.findElements(By.partialLinkText(terms[i]));
			if (list.size() > 0) {
				return list.get(0).getAttribute("href");
			}
		}
		return null;

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
	 * Using Firebug and Har Export Trigger to capture driver traffic
	 * 
	 * @param driver
	 * @throws IOException 
	 * @throws URISyntaxException 
	 */
	public void startWithExtension() throws IOException, URISyntaxException {
		/*
		File firebug = Paths.get("src/main/resources/firefox/firebug.xpi").toFile();
		File harExportTrigger = Paths.get("src/main/resources/firefox/harexporttrigger.xpi").toFile();
		File netExport = Paths.get("src/main/resources/firefox/netExport-0.9b7.xpi").toFile();
		*/

		ClassLoader classloader= getClass().getClassLoader();
		File firebug=new File(PPCrawler.class.getResource("/firefox/firebug.xpi").getFile());
		File netExport=new File(PPCrawler.class.getResource("/firefox/netExport-0.9b7.xpi").getFile());
		FirefoxProfile profile = new FirefoxProfile();
		
		try {
			profile.addExtension(firebug);
			// profile.addExtension(harExportTrigger);
			profile.addExtension(netExport);
		} catch (IOException e) {
			e.printStackTrace();
		}
		/*
		// setting fireBug
		profile.setPreference("extensions.firebug.currentVersion", "2.0.17");
		profile.setPreference("extensions.firebug.addonBarOpened", true);
		profile.setPreference("extensions.firebug.console.enableSites", true);
		profile.setPreference("extensions.firebug.script.enableSites", true);
		profile.setPreference("extensions.firebug.net.enableSites", true);
		profile.setPreference("extensions.firebug.previousPlacement", 1);
		profile.setPreference("extensions.firebug.allPagesActivation", "on");
		profile.setPreference("extensions.firebug.onByDefault", true);
		profile.setPreference("extensions.firebug.defaultPanelName", "net");

		// Setting netExport preferences
		profile.setPreference("extensions.firebug.netexport.alwaysEnableAutoExport", true);
		profile.setPreference("extensions.firebug.netexport.autoExportToFile", true);
		profile.setPreference("extensions.firebug.netexport.Automation", true);
		profile.setPreference("extensions.firebug.netexport.showPreview", false);
		profile.setPreference("extensions.firebug.netexport.defaultLogDir", "E:\\codebase\\project\\har\\");
		*/
		String domain = "extensions.firebug.";

		// Set default Firebug preferences
		profile.setPreference(domain + "currentVersion", "2.0.17"); //current version,  avoid open firebug start page
		profile.setPreference(domain + "allPagesActivation", "on"); //Firebug is activated for all pages by default
		profile.setPreference(domain + "defaultPanelName", "net"); //The Net panel is selected by default
		profile.setPreference(domain + "net.enableSites", true); //Firebug Net panel is enabled by default

		// Set default NetExport preferences
		profile.setPreference(domain + "netexport.alwaysEnableAutoExport", true); //Automatically export HAR when a page is loaded.
		profile.setPreference(domain + "netexport.showPreview", false); //Do not show a preview for exported data
		profile.setPreference(domain + "netexport.defaultLogDir", "E:\\codebase\\project\\har\\"); //Directory to store har file
		profile.setPreference(domain + "netexport.defaultFileName", "www.bbc.com");
		DesiredCapabilities capabilities = new DesiredCapabilities();
		capabilities.setCapability(FirefoxDriver.PROFILE, profile);
		profile.setPreference("intl.accept_languages", "en-us");
		//WebDriver driver = new FirefoxDriver(capabilities);
		File pathBinary = new File("E:\\codebase\\project\\Mozilla Firefox\\bin\\firefox.exe");
		FirefoxBinary Binary = new FirefoxBinary(pathBinary);      
		WebDriver driver = new FirefoxDriver(Binary,profile);
		//WebDriver driver =new FirefoxDriver();
		try {
			Thread.sleep(10000);
			driver.get("http://www.bbc.com");
			Thread.sleep(10000);
		} catch (InterruptedException ie) {
			ie.printStackTrace();
		}
		driver.quit();
		readHar("E:\\codebase\\project\\har\\www.bbc.co.uk+2016-06-14+10-36-06.har");

	}

	public void start() throws Exception {
		Executor executor = new Executor() {
			public void execute(CrawlDatum datum, CrawlDatums next) throws Exception {
				/*
				 * FirefoxProfile profile = new FirefoxProfile();
				 * profile.setPreference("intl.accept_languages", "en-gb");
				 * WebDriver driver = new FirefoxDriver(profile); TODO
				 */

				HtmlUnitDriver driver = new HtmlUnitDriver();
				DesiredCapabilities capabilities = DesiredCapabilities.htmlUnit();
				capabilities.setCapability("intl.accept_languages", "en-gb");
				String currentUrl = datum.getUrl();

				driver.get(currentUrl);
				// find the link of privacy policy page
				String policyurl = getPolicyHref(driver);

				if (policyurl != null) {
					logger.toLog("hreflog.txt", currentUrl + " Success");
					driver.get(policyurl);
					// write privacy policy pages to file for further extraction
					logger.toFile(URLEncoder.encode(currentUrl, "UTF-8") + ".txt",
							policyurl + "\n" + driver.getPageSource());
				} else {
					logger.toLog("hreflog.txt", currentUrl + " Fail");
				}

			}
		};

		// create a BerkeleyDBManager
		DBManager manager = new BerkeleyDBManager("crawl");
		// creating a crawler object need DBManager and executor
		Crawler crawler = new Crawler(manager, executor);
		// Logger logger = new Logger();
		HtmlUnitDriver driver = new HtmlUnitDriver();
		DesiredCapabilities capabilities = DesiredCapabilities.htmlUnit();
		capabilities.setCapability("intl.accept_languages", "en-gb");
		String url = "http://www.bbc.com";

		driver.get(url);
		// list all the connecting websites
		List<WebElement> list = driver.findElements(By.tagName("a"));
		String regex = url.replace("/", "\\/") + ".*";
		for (WebElement e : list) {
			String href = e.getAttribute("href");
			System.out.print(href);
			// TODO may not needed
			if (href.matches(regex)) {
				System.out.println(" Filter out");
			} else {
				System.out.println();
				crawler.addSeed(e.getAttribute("href"));
			}
		}

		driver.quit();
		crawler.start(1);
	}

	public void readHar(String fileName) {
		File f = new File(fileName);
		HarFileReader r = new HarFileReader();
		// HarFileWriter w = new HarFileWriter();
		try {
			System.out.println("Reading " + fileName);
			HarLog log = r.readHarFile(f);

			// Access all elements as objects
			HarBrowser browser = log.getBrowser();

			HarEntries entries = log.getEntries();
			List<HarEntry> entry = entries.getEntries();
			int count = 1;
			for (HarEntry e : entry) {
				System.out.println(count++ + " " + e.getRequest().getUrl());

			}

		} catch (JsonParseException e) {
			e.printStackTrace();
			System.err.println("Parsing error during test");
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("IO exception during test");
		}
	}

	public void readCorruptedHar(String harFile) {
		File f = new File(harFile);
		HarFileReader r = new HarFileReader();
		try {
			// All violations of the specification generate warnings
			List<HarWarning> warnings = new ArrayList<HarWarning>();
			HarLog l = r.readHarFile(f, warnings);

			for (HarWarning w : warnings)
				System.out.println("File:" + harFile + " - Warning:" + w);
		} catch (JsonParseException e) {
			e.printStackTrace();
			// fail("Parsing error during test");
		} catch (IOException e) {
			e.printStackTrace();
			// fail("IO exception during test");
		}
	}

	public void proxy() throws IOException, InterruptedException {

		BrowserMobProxy proxy = new BrowserMobProxyServer();
		proxy.start(0);

		// get the Selenium proxy object
		Proxy seleniumProxy = ClientUtil.createSeleniumProxy(proxy);

		// configure it as a desired capability
		DesiredCapabilities capabilities = new DesiredCapabilities();
		capabilities.setCapability(CapabilityType.PROXY, seleniumProxy);

		// start the browser up
		// WebDriver driver = new FirefoxDriver(capabilities);
		HtmlUnitDriver driver = new HtmlUnitDriver(capabilities);
		driver.setJavascriptEnabled(true);

		// enable more detailed HAR capture, if desired (see CaptureType for the
		// complete list)
		proxy.enableHarCaptureTypes(CaptureType.REQUEST_CONTENT);

		// CaptureType.RESPONSE_CONTENT);

		// create a new HAR with the label "yahoo.com"
		proxy.newHar("www.bbc.com");

		// open yahoo.com
		driver.get("http://www.bbc.com");
		Thread.sleep(5000);
		// get the HAR data
		Har har = proxy.getHar();
		EnumSet<CaptureType> set = proxy.getHarCaptureTypes();
		for (CaptureType c : set) {
			System.out.println(c);
		}
		// System.out.println(proxy.getHarCaptureTypes());
		// har.writeTo(System.out);
		har.writeTo(Files.newBufferedWriter(Paths.get("1.txt")));

		readHar("1.txt");
		System.out.println("fdsf");

		proxy.stop();

	}

	public static void main(String[] args) throws Exception {
		PPCrawler ppc = new PPCrawler("links/bbc");
		// ppc.start();
		// ppc.proxy();
		System.out.println(PPCrawler.class.getResource("/firefox/firebug.xpi"));
		File f= new File(PPCrawler.class.getResource("/firefox/firebug.xpi").getFile());
		System.out.println(f.exists());

		System.out.println(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd+HH:mm:ss")));
		//ppc.startWithExtension();
		//ppc.readHar("1.har");
		
	}
}
