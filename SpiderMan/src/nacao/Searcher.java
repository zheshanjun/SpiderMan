package nacao;

import java.io.IOException;

import org.openqa.selenium.NoSuchWindowException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.remote.UnreachableBrowserException;

import tools.Logger;
import tools.ProxyFactory;
import tools.SysConfig;

public abstract class Searcher {

	ProxyFactory proxyFactory=null;
	WebDriver driver=null;

	public Logger logger;
	//ä¯ÀÀÆ÷×´Ì¬£¬-1£ºÎ´³õÊ¼»¯£¬0£ºÕý³££¬1£º±ÀÀ£
	public int driverStatus=-1;
	public String fireFoxPath=null;
	public String searchWindowHandle=null;
	
	public abstract NACAO search(String orgCode) throws Exception;

	public abstract int initDriver() throws Exception;
	
//	public abstract  int reset() throws Exception;

	public void addProxyFactory() throws Exception
	{
		proxyFactory=new ProxyFactory(3);
	}

	public void setLogger(Logger logger)
	{
		this.logger=logger;
	}
	
	public void setFireFoxPath(String fireFoxPath)
	{
		this.fireFoxPath=fireFoxPath;
	}
	
	public void deleteProxyFactory()
	{
		proxyFactory=null;
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
