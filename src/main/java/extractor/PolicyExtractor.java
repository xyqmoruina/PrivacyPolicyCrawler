package extractor;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.remote.DesiredCapabilities;

import whois.DomainParser;
import whois.Whois;
import whois.WhoisResult;

/**
 * Extract the privacy policy of given website
 * 
 * @author jero
 *
 */
public class PolicyExtractor {
	private String policyUrl;
	private String policy;
	private WhoisResult whoisResult;
	private ExtractResult extractResult;
	private WebDriver driver;
	private final DomainParser parser=new DomainParser();

//	public enum Status {
//		SUCCESS, FAIL, REFERTOWHOIS, RESET
//	};

	private ExtractResult.Status status;

	public PolicyExtractor() {
		policyUrl = "";
		policy = "";
		whoisResult = new WhoisResult();
		status = ExtractResult.Status.FAIL;
		extractResult=new ExtractResult();
		DesiredCapabilities capabilities = DesiredCapabilities.htmlUnit();
		capabilities.setCapability("intl.accept_languages", "en-us");
		driver= new HtmlUnitDriver(capabilities);
		driver.manage().deleteAllCookies();
		// driver=new FirefoxDriver(capabilities);
	}

	public PolicyExtractor(boolean ifFirefox) {
		policyUrl = "";
		policy = "";
		whoisResult = new WhoisResult();
		status = ExtractResult.Status.FAIL;
		extractResult=new ExtractResult();
		
		if(ifFirefox){
			DesiredCapabilities capabilities=DesiredCapabilities.firefox();
			capabilities.setCapability("intl.accept_languages", "en-us");
			driver = new FirefoxDriver(capabilities);
		}
		else{
			DesiredCapabilities capabilities = DesiredCapabilities.htmlUnit();
			capabilities.setCapability("intl.accept_languages", "en-us");
			driver= new HtmlUnitDriver(capabilities);
		}
		driver.manage().deleteAllCookies();
		
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
		String[] terms = { "Privacy", "Privacy Policy","privacy" };
		driver.manage().timeouts().pageLoadTimeout(15, TimeUnit.SECONDS);//wait 15s for server to response
		try {
			System.out.println("Extracting: " + url);
			driver.get(url);
			Thread.sleep(3000);
			for (int i = 0; i < terms.length; i++) {
				List<WebElement> list = driver.findElements(By.partialLinkText(terms[i]));
				if (list.size() > 0) {
					href = list.get(0).getAttribute("href");
					break;
				}
			}
		}catch(TimeoutException te){ 
			System.err.println("Server not response");
		}
		catch (Exception e) {
			System.err.println("Unkown error");
			//e.printStackTrace();
			// driver.quit();
			// return "";// unnecessary?
		}
		// driver.quit();
		return href;
	}

	/**
	 * Extract the privacy policy url and its content of given url, resutl
	 * status refer to {@link #getStatus()} and get result from
	 * {@link #getPolicyUrl()} and {@link #getPoicy()}.
	 * 
	 */
	public void extract(String url) {
		driver.manage().deleteAllCookies();
		policyUrl = getPolicyHref(url);
		
		try {
			if (!"".equals(policyUrl)) {
				//disable page load timeout
				driver.manage().timeouts().pageLoadTimeout(-1, TimeUnit.SECONDS);
				driver.get(policyUrl);
				Thread.sleep(5000);
				policy = driver.getPageSource();
				status = ExtractResult.Status.SUCCESS;
			} else {
				Whois whois = new Whois();
				if (whois.getWhois(url)) {
					status = ExtractResult.Status.REFERTOWHOIS;
					whoisResult = whois.getWhoisResult();
					policyUrl = getPolicyHref("http://" + whoisResult.getRegistrantUrl());
					if (!"".equals(policyUrl)) {
						//disable page load timeout
						driver.manage().timeouts().pageLoadTimeout(-1, TimeUnit.SECONDS);
						driver.get(policyUrl);
						Thread.sleep(5000);
						policy = driver.getPageSource();
					}
				}

			}
		}catch(TimeoutException te){ 
			te.printStackTrace();
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}
		//driver.quit();

	}
	
	/**
	 * Quit driver
	 */
	public void quit() {
		driver.quit();
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		PolicyExtractor extractor = new PolicyExtractor(true);
		extractor.extract("http://craigslist.org");
//		System.out.println(extractor.getPolicyUrl() + "\n" + extractor.getStatus() + "\n"
//				+ extractor.getPolicy());
		ExtractResult result=extractor.getExtractResult();
		System.out.println(result.getPolicyUrl()+"\t"+result.getStatus()+"\t"+result.getPolicy());
		extractor.quit();
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

//	public Status getStatus() {
//		return status;
//	}
//
//	public void setStatus(Status status) {
//		this.status = status;
//	}

	public WhoisResult getWhoisResult() {
		return whoisResult;
	}

	public ExtractResult getExtractResult() {
		extractResult.setPolicyUrl(policyUrl);
		extractResult.setPolicy(policy);
		extractResult.setWhoisResult(whoisResult);
		extractResult.setStatus(status);
		return extractResult;
	}

	public void setExtractResult(ExtractResult extractResult) {
		this.extractResult = extractResult;
	}
}
