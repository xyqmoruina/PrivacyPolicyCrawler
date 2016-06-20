package crawler;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.SocketException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.net.whois.WhoisClient;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxBinary;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.remote.DesiredCapabilities;

import com.fasterxml.jackson.core.JsonParseException;

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
import edu.umass.cs.benchlab.har.tools.HarFileReader;
import extractor.PolicyExtractor;
import extractor.Tld;

public class PPCrawler {
	private static MyLogger logger;
	private MyDB db;
	private int topN;
	private int threads;
	private int crawlingDepth;
	private String crawlerDir;
	private Map<String, String> host;
	
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
		db=new MyDB();
	}

	public PPCrawler(String crawlerDir, int topN, int threads, int crawlingDepth) {
		logger = new MyLogger(crawlerDir);
		this.setCrawlerDir(crawlerDir);
		this.topN = topN;
		this.threads = threads;
		this.crawlingDepth = crawlingDepth;
		getWhoisHost();
		db=new MyDB();
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

	public void setNetworkMonitorProfile(FirefoxProfile profile, String harPath) throws IOException {
		// FirefoxProfile profile = new FirefoxProfile();

		// Load extensions
		File harExport = new File(PPCrawler.class.getResource("/firefox/harexporttrigger.xpi").getFile()); // adjust
																											// path
																											// as
																											// needed
		profile.addExtension(harExport);

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
	public List<String> findLinksByProxy(String url) throws IOException, URISyntaxException {
		List<String> links = new ArrayList<String>();
		String binPath = "/Users/jero/codebase/project/libs/Firefox.app/Contents/MacOS/firefox";
		String harPath = System.getProperty("user.dir") + "/" + PPCrawler.logger.getHarDir();

		FirefoxBinary Binary = new FirefoxBinary(new File(binPath));
		FirefoxProfile profile = new FirefoxProfile();
		setNetworkMonitorProfile(profile, harPath);
		WebDriver driver = new FirefoxDriver(Binary, profile);
		driver.manage().deleteAllCookies();
		
		try {
			Thread.sleep(5000); // allow firebug to load its net panel
			driver.get(url);
			Thread.sleep(20000); // while firebug is exporting HAR
			List<HarEntry> entries = readHar(harPath);
			for (HarEntry e : entries) {
				links.add(extractDomain(e.getRequest().getUrl()));
			}
		/*} catch (NullPointerException ne) {

			try {
				Thread.sleep(20000);
				List<HarEntry> entries = readHar(harPath);
				for (HarEntry e : entries) {
					links.add(extractDomain(e.getRequest().getUrl()));
				}
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}*/
		} catch (InterruptedException ie) {
			ie.printStackTrace();
		}
		driver.quit();
		
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

		return str[0] + "//" + str[1].replaceAll("/.*", "");

	}

	/**
	 * Find all link in page source
	 * @deprecated
	 * @param url
	 * @return
	 */
	public List<String> findLinksByPageSource(String url) {
		List<String> links = new ArrayList<String>();
		HtmlUnitDriver driver = new HtmlUnitDriver();
		DesiredCapabilities capabilities = DesiredCapabilities.htmlUnit();
		capabilities.setCapability("intl.accept_languages", "en-gb");
		// String url = "http://www.bbc.com";

		driver.get(url);
		// list all the connecting websites
		List<WebElement> list = driver.findElements(By.tagName("a"));
		driver.quit();
		String regex = url.replace("/", "\\/") + ".*";
		for (WebElement e : list) {
			String href = e.getAttribute("href");
			System.out.print(href);
			// TODO may not needed
			if (href.matches(regex)) {
				System.out.println(" Filter out");
			} else {
				System.out.println();
				links.add(e.getAttribute("href"));
			}
		}

		return links;

	}

	public String getPolicyByLinkText(WebDriver driver) {
		String[] terms = { "Privacy", "Privacy Policy", "Terms and Condition" };

		try {
			for (int i = 0; i < terms.length; i++) {
				List<WebElement> list = driver.findElements(By.partialLinkText(terms[i]));
				if (list.size() > 0) {
					return list.get(0).getAttribute("href");
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return null;
	}


	/**
	 * 
	 * @param url
	 *            protocol://url
	 * @throws Exception
	 */
	public void start(String url) throws Exception {
		Executor executor = new Executor() {
			public void execute(CrawlDatum datum, CrawlDatums next) throws Exception {
				String currentUrl = datum.getUrl();
				//System.out.println("***" + currentUrl);
				PolicyExtractor extractor = new PolicyExtractor(currentUrl);
				extractor.extract();
				String timestamp=LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-mm-dd-HH:MM:SS"));
				switch (extractor.getStatus()) {
				case SUCCESS:
					logger.toLog("hreflog.txt", currentUrl + " Success");
					logger.toFile(URLEncoder.encode(currentUrl, "UTF-8") + ".txt",
							extractor.getPolicyUrl() + "\n" + extractor.getPolicy());
					//new Policy(mainsite, connectingUrl, policyurl,0, "",date.toString()
					db.insert(new Policy(url, currentUrl, extractor.getPolicyUrl(), 0, "", timestamp));
					break;
				case REFERTOWHOIS:
					logger.toLog("hreflog.txt", currentUrl + " Refer to " + extractor.getWhoisReport());
					db.insert(new Policy(url, currentUrl, "", 1, extractor.getWhoisReport(), timestamp));
					break;
				default:
					logger.toLog("hreflog.txt", currentUrl + " Fail");
					db.insert(new Policy(url, currentUrl, "", 0, "", timestamp));

				}

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
		crawler.start(1);
	}

	/**
	 * Read all har file under given path and return url in har entries
	 * 
	 * @param path
	 * @return
	 * @throws IOException
	 */
	public List<HarEntry> readHar(String path) throws IOException {
		System.out.println(path);
		List<HarEntry> entry = null;
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(path))) {
			for (Path s : stream) {
				HarFileReader r = new HarFileReader();

				try {
					System.out.println("Reading " + s.getFileName());
					HarLog log = r.readHarFile(s.toFile());
					HarEntries entries = log.getEntries();
					entry = entries.getEntries();
					/*
					 * int count = 1; for (HarEntry e : entry) {
					 * System.out.println(count++ + " " +
					 * e.getRequest().getUrl());
					 * 
					 * }
					 */
				} catch (JsonParseException e) {
					e.printStackTrace();
					System.err.println("Parsing error during test");
				} catch (IOException e) {
					e.printStackTrace();
					System.err.println("IO exception during test");
				}
			}
		} catch (DirectoryIteratorException ex) {
			// I/O error encounted during the iteration, the cause is an
			// IOException
			throw ex.getCause();
		}

		return entry;
	}

	public String getWhois(String domainName) {

		StringBuilder result = new StringBuilder("");

		WhoisClient whois = new WhoisClient();
		try {

			// default is internic.net
			whois.connect(WhoisClient.DEFAULT_HOST);
			String whoisData1 = whois.query("=" + domainName);
			result.append(whoisData1);
			whois.disconnect();

		} catch (SocketException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return result.toString();

	}

	public static void main(String[] args) throws Exception {
		PPCrawler ppc = new PPCrawler("links/telegraph");

		ppc.start("http://telegraph.co.uk");
		// System.out.println(ppc.extractDomain("http://ichef.bbci.co.uk/images/ic/272x153/p03ydncg.jpg"));
		/*
		 * System.out.println(LocalDateTime.now().format(DateTimeFormatter.
		 * ofPattern("yyyy-MM-dd+HH:mm:ss"))); LocalDateTime now =
		 * LocalDateTime.now();
		 * System.out.println(System.getProperty("user.dir"));
		 * ppc.findLinksByProxy("fds"); String str =
		 * "www.bbc.com+2016-06-15+11-48-34.har"; String[] ss =
		 * str.replace(".har", "").split("\\+"); for (String s : ss) {
		 * System.out.println(s); // System.out.println(str.replace(".har",
		 * "")); } String timestampStr = ss[ss.length - 2] + "+" + ss[ss.length
		 * - 1]; DateTimeFormatter formatter =
		 * DateTimeFormatter.ofPattern("yyyy-MM-dd+HH-mm-ss"); LocalDateTime
		 * timestamp = LocalDateTime.parse(timestampStr, formatter);
		 * System.out.println(now.isAfter(timestamp)); // ppc.readHar("1.har");
		 */
	}

	public String getCrawlerDir() {
		return crawlerDir;
	}

	public void setCrawlerDir(String crawlerDir) {
		this.crawlerDir = crawlerDir;
	}
}
