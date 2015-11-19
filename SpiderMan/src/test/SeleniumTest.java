package test;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchWindowException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriver.TargetLocator;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

public class SeleniumTest {

	public static void main(String[] args) throws InterruptedException
	{
		WebDriver driver=new FirefoxDriver();
		driver.get("www.taobao.com/");

		WebElement ele1 = driver.findElement(By.xpath(".//input[@id='kw']"));
		
		WebElement ele2=driver.findElement(By.xpath(".//input[@id='su']"));
		
		
		System.out.println("******");
		Thread.sleep(15000);
		System.out.println("******");
		
		ele1.sendKeys("∆Ê∑Âµ¿√º");
		ele2.click();

	}
}
