package test;

import java.io.IOException;
import java.net.MalformedURLException;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import tools.ExtendedHtmlUnitDriver;

public class SeleniumTest {

	public static void main(String[] args) throws InterruptedException, FailingHttpStatusCodeException, MalformedURLException, IOException
	{
		ExtendedHtmlUnitDriver driver = new ExtendedHtmlUnitDriver(true);
		driver.get("https://www.baidu.com/");
		WebElement x = driver.findElement(By.xpath("//input[@id='su']"));
		System.out.println(x);
	}
}
