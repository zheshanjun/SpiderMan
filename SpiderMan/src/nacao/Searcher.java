package nacao;

import org.openqa.selenium.WebDriver;
import tools.Logger;
import tools.ProxyFactory;

public abstract class Searcher {

	ProxyFactory proxyFactory=null;
	WebDriver driver=null;

	public Logger logger;
	public int driverStatus=0;
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
	
	public void deleteProxyFactory()
	{
		proxyFactory=null;
	}
	
	public void quit()
	{
		driver.quit();
	}
}
