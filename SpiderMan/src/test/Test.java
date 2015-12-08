package test;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.sql.PreparedStatement;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchWindowException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.microsoft.sqlserver.jdbc.SQLServerException;

import tools.ProxyFactory3;
import tools.MSSQLClient;
import tools.ProxyFactory;
import tools.SysConfig;

public class Test {

	public WebDriver driver;
	public String handle=null;
	
	
	public Test() throws Exception
	{
		String page="https://www.taobao.com/";
		
//		System.setProperty("webdriver.chrome.driver", 
//				SysConfig.workDir+"\\chromedriver.exe"); 
//		driver=new ChromeDriver();
//		System.out.println(driver.getWindowHandles());
		FirefoxProfile profile = new FirefoxProfile();
		profile.setPreference("startup.homepage_welcome_url.additional",page);
        driver=new FirefoxDriver(profile);
		
//		driver.get(page);
		System.out.println(driver.getWindowHandles());
        driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
		driver.manage().window().maximize();
        handle=driver.getWindowHandle();
        System.out.println("handle:"+handle);
//		driver.quit();
	}
	
	public void clickMore()
	{
//		WebElement input=waitForInput();
		WebElement submit=waitForSubmit();
//		input.sendKeys("ÁúÈª½£");
		submit.click();
		System.out.println("driver.getWindowHandles():"+driver.getWindowHandles());
		driver.close();
		for(String handle:driver.getWindowHandles())
		{
			driver.switchTo().window(handle);
			driver.close();
			
		}
		System.out.println("driver.getWindowHandles():"+driver.getWindowHandles());
		driver.quit();
//		for(int i=0;i<50 && driver.getWindowHandles().size()==1;i++)
//		{
//			try {
//				Thread.sleep(100);
//				System.out.println(i);
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
//		System.out.println(driver.getWindowHandles());
	}
	
	public WebElement waitForSubmit()
	{
		return (new WebDriverWait(driver,SysConfig.WAIT_IN_SECONDS,SysConfig.SLEEP_IN_MILLIS))
				.until(new ExpectedCondition<WebElement>() 
		{
        	public WebElement apply(WebDriver d) 
        	{
        		return d.findElement(By.xpath("/html/body/div[2]/div[2]/div/div[1]/div/div/div/div[3]/div[1]/div/a"));
//        		return d.findElement(By.xpath("html/body/div[2]/div[2]/div/div[1]/div/div/div/div[2]/div[2]/form/div[1]/button"));
        	}
        });
	}
	
	public WebElement waitForInput()
	{
		return (new WebDriverWait(driver,SysConfig.WAIT_IN_SECONDS,SysConfig.SLEEP_IN_MILLIS))
				.until(new ExpectedCondition<WebElement>() 
		{
        	public WebElement apply(WebDriver d) 
        	{
        		return d.findElement(By.xpath("//*[@id='q']"));
        	}
        });
	}
	
	public boolean switchToWindow(){  
	    boolean flag = false;  
	    try {  
	        Set<String> handles = driver.getWindowHandles();  
	        for (String s : handles) { 
	        	System.out.println("s:"+s);
	            if (s.equals(handle))  
	                continue;  
	            else {  
	                driver.switchTo().window(s);  
	                flag = true;
                    break;    
	            }  
	        }  
	    } catch (NoSuchWindowException e) {  
	        e.fillInStackTrace();  
	        flag = false;  
	    }  
	    return flag;  
	}  
	
//	public Boolean id
	
	public static HashMap<String,String> parseQuery(String pageSrc) throws MalformedURLException, UnsupportedEncodingException
	{
		pageSrc=URLDecoder.decode(pageSrc,"utf8");
		pageSrc=URLDecoder.decode(pageSrc,"utf8");
		URL url=new URL(pageSrc);
		
		String query=url.getQuery();
		
		String[] parameterArr=query.split("&");
		for(String parameter:parameterArr)
		{
			String[] info=parameter.split("=");
			System.out.println(Arrays.toString(info));
		}
		return null;
		
	}
	
	public static void main(String[] args) throws Exception
	{	
		Test test=new Test();
		test.clickMore();
//		for(int i=0;i<5;i++)
//		{
//			test.clickMore();
//			System.out.println("++++++++++++++"+i);
//		}
	}
}
