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

	public WebDriver driver;
	
	
	public WebElement waitForCertificateIframe()
	{
		return (new WebDriverWait(driver,1,100)).until(new ExpectedCondition<WebElement>() 
		{
        	public WebElement apply(WebDriver d) 
        	{
        		return driver.findElement(By.xpath(".//*[@id='J_Top']/div/div[1]/div/div/div/div[3]/div[1]/div/a"));
        	}        	       	
        });
	}
	
	public SeleniumTest()
	{
		
		System.out.println("start firefox browser...");
//        System.setProperty("webdriver.firefox.bin", 
//                "C:/Program Files (x86)/Mozilla Firefox/firefox.exe");
//        File file = new File("files/firebug-2.0.7-fx.xpi");
        FirefoxProfile profile = new FirefoxProfile();
//        try {
//            profile.addExtension(file);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        profile.setPreference("extensions.firebug.currentVersion", "2.0.13");
        //active firebug extensions
        profile.setPreference("extensions.firebug.allPagesActivation", "on");    
        driver = new FirefoxDriver(profile);
//        driver.get("http://www.baidu.com");
//        System.out.println("start firefox browser succeed...");   
        
		driver.get("https://www.taobao.com/");
		String handle1=driver.getWindowHandle();
		System.out.println("1 -> "+driver.getWindowHandles());
		
		
		WebElement ele = waitForCertificateIframe();
		
		ele.click();
		switchToWindow();
		String handle2=driver.getWindowHandle();
		
		System.out.println("2 -> "+driver.getWindowHandles());
		driver.get("www.baidu.com");
//		switchToWindow();
		System.out.println("3 -> "+driver.getWindowHandles());
//		driver.close();
//		driver.switchTo().window(handle2);
		driver.close();
		driver.switchTo().window(handle1);
		ele.click();

	}
	
	public boolean switchToWindow(){  
	    boolean flag = false;  
	    try {  
	        String currentHandle = driver.getWindowHandle();  
	        Set<String> handles = driver.getWindowHandles();  
	        for (String s : handles) {  
	            if (s.equals(currentHandle))  
	                continue;  
	            else {  
	                driver.switchTo().window(s);  
	                flag = true;
                    System.out.println("Switch to window: successfully!");  
                    break;    
	            }  
	        }  
	    } catch (NoSuchWindowException e) {  
	        System.out.println("Window: " 
	                + " cound not found!");
	        e.fillInStackTrace();
	        flag = false;  
	    }  
	    return flag;  
	} 
	
	public static void main(String[] args)
	{
//		System.out.println("a:"+null);
		SeleniumTest test=new SeleniumTest();
	}
}
