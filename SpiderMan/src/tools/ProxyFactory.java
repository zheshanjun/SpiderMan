package tools;

import java.net.SocketTimeoutException;
import java.util.Arrays;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class ProxyFactory {

	public int batchSize=10;
	public int curIdx=0;
	public String[][] proxyPool;
	public int maxTryTimes=10;
	String url="http://svip.kuaidaili.com/api/getproxy/?orderid=914549537948088&num=__num__&b_pcchrome=1&b_pcie=1&b_pcff=1&protocol=2&method=2&an_ha=1&quality=2&sort=1&format=text&sep=4";
//	String url="http://dev.kuaidaili.com/api/getproxy?orderid=914549537948088&num=__num__&protocol=2&area=上海";
	
	public ProxyFactory() throws Exception
	{
		this(10);
	}
	
	public ProxyFactory(int batchSize) throws Exception
	{
		this.batchSize=batchSize;
		proxyPool=new String[batchSize][2];
		url=url.replace("__num__", String.valueOf(batchSize));
		fillInProxyPool();
	}
	
	public void  fillInProxyPool() throws Exception
	{
//		System.out.println("filling in the proxy pool!");
		fillInProxyPool(1);
	}

	public void fillInProxyPool(int tryTimes) throws Exception
	{
		
		if(tryTimes>maxTryTimes) throw new Exception("获取快代理失败!");
		
		try {
			Connection conn = Jsoup.connect(url);
			Document doc = conn.get();	
			String[] proxyArr=doc.body().text().split("\\|");
			for(int i=0;i<proxyArr.length;i++)
			{
				proxyPool[i]=proxyArr[i].split(":");
			}
		} catch (SocketTimeoutException e)
		{
			Thread.sleep(1000);
			fillInProxyPool(tryTimes+1);
		}
		
	}
	
	public String[] getProxy() throws Exception
	{
//		SysConfig.logInfo("Changing proxy...");
		if(curIdx==batchSize)
		{
//			Thread.sleep(1000);
			fillInProxyPool();
			curIdx=0;
		}
		return proxyPool[curIdx++];
	}
	
	public String[] getProxy(String[] curProxy) throws Exception
	{
		String[] proxy=getProxy();
		if(curProxy!=null)
		{
			while(proxy[0].equals(curProxy[0]) && proxy[1].equals(curProxy[1]))
			{
				proxy=getProxy();
			}
		}
		
		return proxy;
	}
	
	public static void main(String[] args) throws Exception
	{
		ProxyFactory pf=new ProxyFactory(3);
		for(int i=0;i<10;i++)
		{
			System.out.println(Arrays.toString(pf.getProxy()));
		}
	}
	
}
