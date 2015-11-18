package tools;

import java.net.SocketTimeoutException;
import java.util.Arrays;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class ProxyFactory3 {

	public MSSQLClient dbClient;	
	public int batchSize=10;
	public int curIdx=0;
	public String[][] proxyPool;
	public int maxTryTimes=10;
	String url="http://svip.kuaidaili.com/api/getproxy/?orderid=914549537948088&num=__num__&b_pcff=1&protocol=2&method=2&sp1=1&sp2=1&f_sp=1&quality=2&sort=0&format=text&sep=4";
//	String url="http://dev.kuaidaili.com/api/getproxy?orderid=914549537948088&num=__num__&protocol=2&area=…œ∫£";
	
	public ProxyFactory3() throws Exception
	{
		this(10);
	}
	
	public ProxyFactory3(int batchSize) throws Exception
	{
		this.batchSize=batchSize;
		
		dbClient=new MSSQLClient(
				String.format("jdbc:sqlserver://%s:1433;DatabaseName=%s",SysConfig.MSSQL_HOST,SysConfig.MSSQL_DB),
				SysConfig.MSSQL_USER, //user
				SysConfig.MSSQL_PWD, //pwd
				true //autoCommit
				);
		
		proxyPool=new String[batchSize][2];
		url=url.replace("__num__", String.valueOf(batchSize));
		fillInProxyPool();
	}
	
	
	
	public void fillInProxyPool() throws Exception
	{
		
		try {
			Connection conn = Jsoup.connect(url);
			Document doc = conn.get();

			String[] proxyArr=doc.body().text().split("\\|");
			
			for(String proxy:proxyArr)
			{
				
				int idx1=proxy.indexOf(":");
				int idx2=proxy.indexOf(",");
				String ip=proxy.substring(0, idx1);
				int port=Integer.valueOf(proxy.substring(idx1+1,idx2));
				float responseInSeconds=Float.valueOf(proxy.substring(idx2+1));
				
				String sql=String.format("insert into ProxyPool values('%s',%d,getDate(),%f,0)",ip,port,responseInSeconds);
				dbClient.execute(sql);
			}
			
		} catch (SocketTimeoutException e)
		{
//			SysConfig.logInfo("SocketTimeout...");
		}
		
//		SysConfig.logInfo("Sleeping...");
		Thread.sleep(5000);
		fillInProxyPool();
		
	}
	
	public String[] getProxy() throws Exception
	{
//		SysConfig.logInfo("Changing proxy...");
		if(curIdx==batchSize)
		{
			Thread.sleep(1000);
			fillInProxyPool();
			curIdx=0;
		}
		return proxyPool[curIdx++];
	}
	
	public static void main(String[] args) throws Exception
	{
		ProxyFactory3 pf=new ProxyFactory3(5);
		pf.fillInProxyPool();
	}
	
}
