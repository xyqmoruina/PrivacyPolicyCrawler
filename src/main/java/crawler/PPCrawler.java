package crawler;


import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static java.nio.file.StandardOpenOption.*;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;

import cn.edu.hfut.dmic.webcollector.crawldb.DBManager;
import cn.edu.hfut.dmic.webcollector.crawler.Crawler;
import cn.edu.hfut.dmic.webcollector.fetcher.Executor;
import cn.edu.hfut.dmic.webcollector.model.CrawlDatum;
import cn.edu.hfut.dmic.webcollector.model.CrawlDatums;
import cn.edu.hfut.dmic.webcollector.plugin.berkeley.BerkeleyDBManager;



public class PPCrawler {
	private static MyLogger logger;
	private int topN;
	private int threads;
	private int crawlingDepth;
	private int test;
	
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

	public void start() throws Exception {
		Executor executor = new Executor() {
			public void execute(CrawlDatum datum, CrawlDatums next) throws Exception {
				/*
				 * FirefoxProfile profile = new FirefoxProfile();
				 * profile.setPreference("intl.accept_languages", "en-gb");
				 * WebDriver driver = new FirefoxDriver(profile);
				 *TODO
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
	public void proxy(){
		//TODO Using proxy to find the requested links
		System.out.println("fdsfd");
	}
	/*
	public void proxy() {
		
		BrowserMobProxy proxy = new BrowserMobProxyServer();
		proxy.start(0);

		// get the Selenium proxy object
		Proxy seleniumProxy = ClientUtil.createSeleniumProxy(proxy);

		// configure it as a desired capability
		DesiredCapabilities capabilities = new DesiredCapabilities();
		capabilities.setCapability(CapabilityType.PROXY, seleniumProxy);

		// start the browser up
		WebDriver driver = new FirefoxDriver(capabilities);

		// enable more detailed HAR capture, if desired (see CaptureType for the
		// complete list)
		// proxy.enableHarCaptureTypes(CaptureType.REQUEST_CONTENT,
		// CaptureType.RESPONSE_CONTENT);

		// create a new HAR with the label "yahoo.com"
		proxy.newHar("www.bbc.com");

		// open yahoo.com
		driver.get("http://www.bbc.com");

		// get the HAR data
		Har har = proxy.getHar();
		try {
			har.writeTo(Files.newBufferedWriter(Paths.get("1.txt"), APPEND));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
*/
	public static void main(String[] args) throws Exception {
		PPCrawler ppc = new PPCrawler("links/bbc");
		ppc.start();
		//ppc.proxy();
	}
}
