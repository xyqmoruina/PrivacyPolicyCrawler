package crawler;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;

public class Crawler {

	public static void main(String args[]) throws IOException {

		// Configure mybatis from mybatis-config.xml
		SqlSessionFactoryBuilder builder = new SqlSessionFactoryBuilder();
		System.out.println(Resources.getResourceURL("mybatis"));
		SqlSessionFactory factory = builder.build(Resources.getResourceAsStream("mybatis/mybatis-config.xml"));
		SqlSession session =factory.openSession();
		
		// Create a new instance of the Firefox driver
		// Notice that the remainder of the code relies on the interface,
		// not the implementation.
		FirefoxProfile profile = new FirefoxProfile();
		profile.setPreference("intl.accept_languages", "en-us");
		WebDriver driver = new FirefoxDriver(profile);

		// And now use this to visit Google
		driver.get("http://www.google.com");
		// Alternatively the same thing can be done like this
		// driver.navigate().to("http://www.google.com");

		System.out.println(driver.findElement(By.partialLinkText("Privacy")).getAttribute("href"));
		String mainsite="www.google.com";
		String url="www.google.com";
		String policyurl=driver.findElement(By.partialLinkText("Privacy")).getAttribute("href");
		LocalDate date=LocalDate.now();
		//LocalTime tiem=LocalTime.now();
	
		session.insert("Policy.insert", new Policy(mainsite, url, policyurl, date.toString()));
		session.commit();
		session.close();
		//System.out.println(driver.findElement(By.tagName("html")).getText());
		// Close the browser
		driver.quit();

	}
}
