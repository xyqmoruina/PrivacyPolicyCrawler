package extractor;

import java.net.URLEncoder;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.remote.DesiredCapabilities;

import db.Policy;

/**
 * Extract the privacy policy of given website
 * 
 * @author jero
 *
 */
public class PolicyExtractor {
	private String url;
	private String policyUrl;
	private String policy;
	private WhoisResult whoisResult;
	private WebDriver driver;

	public enum Status {
		SUCCESS, FAIL, REFERTOWHOIS
	};

	private Status status;

	public PolicyExtractor(String url) {
		this.url = url;
		policyUrl = "";
		policy = "";
		whoisResult = new WhoisResult();
		status = Status.FAIL;
		DesiredCapabilities capabilities = DesiredCapabilities.htmlUnit();
		capabilities.setCapability("intl.accept_languages", "en-us");
		driver = new HtmlUnitDriver(capabilities);
		driver.manage().deleteAllCookies();
		// driver=new FirefoxDriver(capabilities);

	}

	/**
	 * Trivially search for link for privacy policy of driver's currently
	 * visiting websites.
	 * 
	 * @param driver
	 * @return link of privacy policy, null if not found
	 */
	private String getPolicyHref() {
		String href = "";
		String[] terms = { "Privacy", "Privacy Policy" };
		try {
			System.out.println("Extracting: " + url);
			driver.get(url);
			Thread.sleep(5000);
			for (int i = 0; i < terms.length; i++) {
				List<WebElement> list = driver.findElements(By.partialLinkText(terms[i]));
				if (list.size() > 0) {
					href = list.get(0).getAttribute("href");
					break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			// driver.quit();
			// return "";// unnecessary?
		}
		// driver.quit();
		return href;
	}

	/**
	 * Trivially search for link for privacy policy of driver's currently
	 * visiting websites.
	 * 
	 * @param driver
	 * @return link of privacy policy, null if not found
	 */
	private String getPolicyHref(String url) {
		String href = "";
		String[] terms = { "Privacy", "Privacy Policy" };
		try {
			System.out.println("Extracting: " + url);
			driver.get(url);
			Thread.sleep(5000);
			for (int i = 0; i < terms.length; i++) {
				List<WebElement> list = driver.findElements(By.partialLinkText(terms[i]));
				if (list.size() > 0) {
					href = list.get(0).getAttribute("href");
					break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			// driver.quit();
			return "";// unnecessary?
		}
		// driver.quit();
		return href;
	}

	/**
	 * Extract the privacy policy url and its content of given url, resutl
	 * status refer to {@link #getStatus()} and get result from
	 * {@link #getPolicyUrl()} and {@link #getPoicy()}.
	 * 
	 * @throws InterruptedException
	 */
	public void extract() {
		System.out.println("Visiting " + url);
		policyUrl = getPolicyHref();
		try {
			if (!"".equals(policyUrl)) {
				driver.get(policyUrl);
				Thread.sleep(5000);
				policy = driver.getPageSource();
				status = Status.SUCCESS;
			} else {
				Whois whois = new Whois();
				if (whois.getWhois(url)) {
					whoisResult = whois.getWhoisResult();
					policyUrl = getPolicyHref("http://" + whoisResult.getRegistrantUrl());
					if (!"".equals(policyUrl)) {
						driver.get(policyUrl);
						Thread.sleep(5000);
						policy = driver.getPageSource();
						status = Status.REFERTOWHOIS;
					}
				} else {
					
				}

			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		driver.quit();

	}

	public void extractByFirefox() {
		DesiredCapabilities capabilities = DesiredCapabilities.htmlUnit();
		capabilities.setCapability("intl.accept_languages", "en-us");
		driver = new FirefoxDriver(capabilities);
		driver.manage().deleteAllCookies();

		System.out.println("Visiting " + url);
		policyUrl = getPolicyHref();
		try {
			if (!"".equals(policyUrl)) {
				driver.get(policyUrl);
				Thread.sleep(5000);
				policy = driver.getPageSource();
				status = Status.SUCCESS;
			} else {
				Whois whois = new Whois();
				if (whois.getWhois(url)) {
					whoisResult = whois.getWhoisResult();
					policyUrl = getPolicyHref("http://" + whoisResult.getRegistrantUrl());
					if (!"".equals(policyUrl)) {
						driver.get(policyUrl);
						Thread.sleep(5000);
						policy = driver.getPageSource();
						status = Status.REFERTOWHOIS;
					}
				}

			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		driver.quit();

	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		PolicyExtractor extractor = new PolicyExtractor("http://ebay.com");
		extractor.extractByFirefox();
		System.out.println(extractor.getPolicyUrl());
		/*
		 * extractor.extractByFirefox();
		 * System.out.println(extractor.getStatus()); switch
		 * (extractor.getStatus()) { case SUCCESS: System.out.println(
		 * "Success: " + extractor.getPolicyUrl()); break; case REFERTOWHOIS:
		 * System.out.println("WHOIS");
		 * System.out.println(extractor.getWhoisResult().getRegistrantOrg() +
		 * " " + extractor.getWhoisResult().getRegistrantUrl());
		 * System.out.println(extractor.getPolicyUrl()); break; default:
		 * System.out.println("Fail");
		 * 
		 * }
		 */

	}

	public String getPolicyUrl() {
		return policyUrl;
	}

	public void setPolicyUrl(String policyUrl) {
		this.policyUrl = policyUrl;
	}

	public String getPolicy() {
		return policy;
	}

	public void setPolicy(String policy) {
		this.policy = policy;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public WhoisResult getWhoisResult() {
		return whoisResult;
	}
}
