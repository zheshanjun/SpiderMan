package test;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import tools.ProxyFactory3;
import tools.ProxyFactory;
import tools.SysConfig;

public class Test {

	public WebDriver driver;
//	public static 
	public WebElement waitForIP()
	{
		return (new WebDriverWait(driver,10,100)).until(new ExpectedCondition<WebElement>() 
		{
        	public WebElement apply(WebDriver d) 
        	{
        		return driver.findElement(By.xpath(".//*[@id='1']/div[1]/div[1]/div[2]/table/tbody/tr/td"));
        	}        	       	
        });
	}
	
	public Test() throws Exception
	{
		String page="https://www.baidu.com/s?wd=ip";
		
		ProxyFactory pf=new ProxyFactory();
		
		FirefoxProfile profile = new FirefoxProfile();
		profile.setPreference("startup.homepage_welcome_url.additional",page);
		
		String[] proxy=pf.getProxy();
		
		
		profile.setPreference("network.proxy.type",1);
		profile.setPreference("network.proxy.http",proxy[0]);
        profile.setPreference("network.proxy.http_port",Integer.valueOf(proxy[1]));
        profile.setPreference("network.proxy.ssl",proxy[0]);
        profile.setPreference("network.proxy.ssl_port",Integer.valueOf(proxy[1]));
        System.out.println("1->"+System.currentTimeMillis());
        driver=new FirefoxDriver(profile);
//        driver.get(page);
        System.out.println("2->"+System.currentTimeMillis());
        driver.manage().timeouts();
        System.out.println("3->"+System.currentTimeMillis());
		WebElement ele=waitForIP();
        System.out.println("4->"+System.currentTimeMillis());
        
        
//        WebElement ele=driver.findElement(By.xpath(".//*[@id='1']/div[1]/div[1]/div[2]/table/tbody/tr/td"));
        System.out.println("5->"+System.currentTimeMillis());
//        System.out.println(System.currentTimeMillis());
		System.out.println(ele.getText());
//		System.out.println(System.currentTimeMillis());
		driver.quit();
        
        
	}
	
	public static void main(String[] args) throws Exception
	{	
		int delay=5;
		long lastUpdateTs=System.currentTimeMillis();
		
		for(int i=0;i<15;i++)
		{
			if(delay>0)
			{
				long curTs=System.currentTimeMillis();
				long costMillis=curTs-lastUpdateTs; //本次更新消耗时间
				System.out.println(costMillis);
				lastUpdateTs=curTs;
				if(costMillis<(delay*1000))
				{
					System.out.println(new Date());
					
					TimeUnit.MILLISECONDS.sleep(delay*1000-costMillis);
//					Thread.sleep();
				}
			}
		}
	}
}
