package crawler;

import java.io.File;
import java.io.IOException;
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
import java.util.Arrays;
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

		// Load extensions
		File harExport = new File(PPCrawler.class.getResource("/firefox/harexporttrigger.xpi").getFile()); // adjust
																											// path
																											// as
																											// needed
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
		FirefoxProfile profile = new FirefoxProfile();
		setNetworkMonitorProfile(profile, harPath);
		WebDriver driver = new FirefoxDriver(Binary, profile);
		driver.manage().deleteAllCookies();

		int retry = 0;
		do {
			try {
				Thread.sleep(1500); // allow firebug to load its net panel
				driver.get(url);
				Thread.sleep(20000); // while firebug is exporting HAR
				List<HarEntry> entries = readHar(harPath);
				for (HarEntry e : entries) {
					links.add(extractDomain(e.getRequest().getUrl()));
				}
				retry = MAX_RETRY;
			} catch (Exception ie) {
				ie.printStackTrace();
			} /*
				 * finally { driver.quit(); }
				 */
		} while (retry++ < MAX_RETRY);
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
				String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH:mm:ss"));
				switch (extractor.getStatus()) {
				case SUCCESS:
					logger.toLog("hreflog.txt", currentUrl + " Success");
					logger.toFile(URLEncoder.encode(currentUrl, "UTF-8") + ".txt",
							extractor.getPolicyUrl() + "\n" + extractor.getPolicy());
					db.insert(new Policy(url, currentUrl, extractor.getPolicyUrl(), extractor.getPolicy(), 0, "",
							timestamp));
					break;
				case REFERTOWHOIS:
					logger.toLog("hreflog.txt",
							currentUrl + " Refer to " + extractor.getWhoisResult().getWhoisReport());
					db.insert(new Policy(url, currentUrl, extractor.getPolicyUrl(), extractor.getPolicy(), 1,
							extractor.getWhoisResult().getRegistrantUrl(), timestamp));
					break;
				default:
					logger.toLog("hreflog.txt", currentUrl + " Fail");
					db.insert(new Policy(url, currentUrl, "", "", 0, "", timestamp));

				}
				extractor.quit();

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
			crawler.start(1);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Read all har file under given path and return url in har entries
	 * 
	 * @param path
	 * @return
	 * @throws IOException
	 */
	public List<HarEntry> readHar(String path) throws Exception {
		// System.out.println(path);
		List<HarEntry> entry = null;
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(path))) {
			for (Path s : stream) {
				HarFileReader r = new HarFileReader();

				// try {
				// System.out.println("Reading " + s.getFileName());
				HarLog log = r.readHarFile(s.toFile());
				HarEntries entries = log.getEntries();
				entry = entries.getEntries();

			}
		} catch (DirectoryIteratorException | IOException ex) {
			// I/O error encounted during the iteration, the cause is an
			ex.printStackTrace();
		}

		return entry;
	}

	public static void main(String[] args) throws IOException {

//		List<String> filter = new ArrayList<String>();
//		// non English
//		Arrays.asList("baidu.com", "taobao.com", "weibo.com", "qq.com", "sina.com.cn", "hao123.com", "tmall.com",
//				"sohu.com", "naver.com", "fc2.com","jd.com","alibaba.com","soso.com","xinhuanet.com").stream().forEach(url -> filter.add(url));
//		// adult content
//		Arrays.asList("xvideos.com","pornhub.com", "xhamster.com").stream().forEach(url->filter.add(url));
//		// require login
//		Arrays.asList("blogspot.com", "linkedin.com", "live.com", "vk.com", "blogger.com").stream().forEach(url -> filter.add(url));
//
//		List<String> tldFilter = Arrays.asList("com");// only gathering .com
//		List<String> list = Files.lines(Paths.get("top500.csv")).filter((record) -> {
//			String[] s = record.split("\\.");
//			return !filter.contains(record) && tldFilter.contains(s[s.length - 1]);// .com
//																					// websites
//																					// only
//		}
//
//		).limit(500).collect(Collectors.toList());// first line is empty
		List<String> list=Files.lines(Paths.get(PPCrawler.class.getResource("/list.txt").getPath())).collect(Collectors.toList());
		list.stream().forEach((url) -> {
			System.out.println(url);

			//PPCrawler ppc = new PPCrawler("links/" + url);
			//ppc.gather("http://" + url, true);
		});

		//PPCrawler ppc = new PPCrawler("links/" + "youtube.com");
		//ppc.gather("http://youtube.com", true);

	}

	public String getCrawlerDir() {
		return crawlerDir;
	}

	public void setCrawlerDir(String crawlerDir) {
		this.crawlerDir = crawlerDir;
	}
}
