package nacao;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchWindowException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.UnreachableBrowserException;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import tools.DataModel;
import tools.ExtendedHtmlUnitDriver;
import tools.Logger;
import tools.SysConfig;

public abstract class Searcher {

	public WebDriver driver=null;

	public Logger logger;
	//ä¯ÀÀÆ÷×´Ì¬£¬-1£ºÎ´³õÊ¼»¯£¬0£ºÕý³££¬1£º±ÀÀ£
	public int driverStatus=-1;
	public String fireFoxPath=null;
	public String searchWindowHandle=null;
	private Runtime runtime=Runtime.getRuntime();
	
	public abstract DataModel search(String code) throws Exception;

	public abstract int initDriver() throws Exception;
	

	public void setLogger(Logger logger)
	{
		this.logger=logger;
	}
	
	public void setFireFoxPath(String fireFoxPath)
	{
		this.fireFoxPath=fireFoxPath;
	}
	
	public WebElement waitForWebElement(final By eleXpath)
	{
		return waitForWebElement(eleXpath,SysConfig.WAIT_IN_SECONDS);
	}
	
	public WebElement waitForWebElement(final By eleXpath,int waitInSeconds)
	{
		return (new WebDriverWait(driver,waitInSeconds,SysConfig.SLEEP_IN_MILLIS)).until(new ExpectedCondition<WebElement>() 
		{
        	public WebElement apply(WebDriver d) 
        	{
        		return d.findElement(eleXpath);
        	}        	       	
        });
	}
	
	public WebElement waitForWebElement(final WebElement parentEle,final By eleXpath)
	{
		return waitForWebElement(parentEle,eleXpath,SysConfig.WAIT_IN_SECONDS);
	}
	
	public WebElement waitForWebElement(final WebElement parentEle,final By eleXpath,int waitInSeconds)
	{
		return (new WebDriverWait(driver,waitInSeconds,SysConfig.SLEEP_IN_MILLIS)).until(new ExpectedCondition<WebElement>() 
		{
        	public WebElement apply(WebDriver d) 
        	{
        		return parentEle.findElement(eleXpath);
        	}        	       	
        });
	}
	
	
	public String recongnizeValidateCode(String pluginPath,String imagePath) throws IOException
	{
		String validateCode=null;
		String cmd="cmd /c "+pluginPath+" "+imagePath;
		try
		{
			Process process = runtime.exec(cmd);
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line=null;
			int i=0;
			while((line=reader.readLine())!=null)
			{
				if((++i)==7)
				{
					validateCode=line.trim();
					reader.close();
					break;
				}
			}
		}
		catch (Exception e)
		{
			logger.info("Recongnize validate code failed.Try again...");
			logger.info(SysConfig.getError(e));
		}
//		new File(imagePath).delete();
		return validateCode;
	}
	
	public void quitDriver() throws IOException
	{
		logger.info("Driver is quitting...");
		try
		{
			driver.quit();
		}
		catch (RuntimeException e)
		{
			logger.info(SysConfig.getError(e));
		}
		driverStatus=-1;
		logger.info("Driver quitting succeed!");
	}
}
