package nacao;

import java.sql.ResultSet;
import java.sql.SQLException;
import tools.JobConfig;
import tools.Logger;
import tools.MSSQLClient;
import tools.SysConfig;

public class NacaoUpdateJob {

	public MSSQLClient dbClient;	
//	public SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	public String imageSavePath="D:\\SpiderMan\\ImageNacaoOrg\\";
	public static String processID;
	public int totalUpdateCnt=0;
	public String today;
	public String localhost;
	public Searcher searcher;
	public String dstTableName;
	
	public int batchSize=100;
	public int resetFlag=0;
	public int startBaseCode=79300;
	public boolean useProxy=false;
	public Logger logger;
	
	public long lastUpdateTs=System.currentTimeMillis();

	public NacaoUpdateJob() throws Exception
	{
		dbClient=new MSSQLClient(
				String.format("jdbc:sqlserver://%s:1433;DatabaseName=%s",SysConfig.MSSQL_HOST,SysConfig.MSSQL_DB),
				SysConfig.MSSQL_USER, //user
				SysConfig.MSSQL_PWD, //pwd
				false //autoCommit
				);
		registerProcess();
		logger=new Logger(processID.replace(":","#"));
	}
	
	public void setLocalhost(String ip)
	{
		this.localhost=ip;
	}
	
	public void initSearcher() throws Exception
	{
		
		if("BaiduApp".equals(dstTableName))
		{
			searcher=new BaiduAppSearcher();
//			searcher.addProxyFactory();
		}
		else if("NacaoOrg".equals(dstTableName))
		{
			searcher=new NacaoOrgSearcher();
//			searcher.addProxyFactory();
		}
		
		if(useProxy==true)
		{
			searcher.addProxyFactory();
		}
		searcher.setLogger(logger);
		searcher.initDriver();
	}
	
	public void setDstTableName(String tableName) throws Exception
	{
		dstTableName=tableName;
	}
	
	public void registerProcess() throws SQLException
	{
		String sql0=String.format("select convert(varchar(19),GETDATE(),120)+'_'+right('000'+cast(count(*) as varchar(3)),3) "
				+"from ProcessStatus(tablockx) "
				+"where substring(processID,1,10)=convert(varchar(10),GETDATE(),120)");
		ResultSet res0 = dbClient.execute(sql0);
		res0.next();
		processID=res0.getString(1);
		String sql1=String.format("insert into ProcessStatus(processID) values('%s')", processID);
		dbClient.execute(sql1);
		dbClient.commit();
	}
	
	public void run() throws Exception
	{
		
		while(startBaseCode<99999999)
		{
			//5分钟没有更新过的进程，认为进程已死亡
			String sql0="update ProcessStatus "
					+"set lastUpdateStatus=1,"
					+ "lastUpdateBaseCode=case when lastUpdateStatus='0' then right('00000000'+cast(lastUpdateBaseCode+1 as varchar(8)),8) else lastUpdateBaseCode end "
					+"where lastUpdateStatus in ('-1','0') "
					+"and DATEDIFF(minute, lastUpdateTime,GETDATE())>5 ";
			dbClient.execute(sql0);
			dbClient.commit();
			
			String sql1="select lastUpdateBaseCode,processID from ProcessStatus(tablockx) "
					+"where lastUpdateBaseCode= "
					+"(select min(lastUpdateBaseCode) from ProcessStatus where lastUpdateStatus='1')";
			ResultSet res1= dbClient.execute(sql1);
			//如果有更新失败的进程，则从此进程的失败处开始更新
			if(res1.next()==true && res1.getObject(1)!=null)
			{
				int lastUpdateBaseCode=Integer.valueOf(res1.getString(1));
				String failedProcessID=res1.getString(2);
				String sql4=String.format("update ProcessStatus set lastUpdateStatus='9' where processID='%s'",failedProcessID);
				dbClient.execute(sql4);
				startBaseCode=lastUpdateBaseCode;
			}
			//否则从正在更新中的下一个batchSize整数倍点开始更新
			else
			{
				String sql2="select max(maxUpdateBaseCode) from ProcessStatus where maxUpdateBaseCode is not null";
				ResultSet res2 = dbClient.execute(sql2);
				if(res2.next()==true && res2.getObject(1)!=null)
				{
					int lastUpdateBaseCode=Integer.valueOf(res2.getString(1));
					startBaseCode=lastUpdateBaseCode-(lastUpdateBaseCode%batchSize)+batchSize;
				}
			}
//			dbClient.commit();
			int lastUpdateStatus=-1;
			String sql3=String.format("update ProcessStatus set lastUpdateBaseCode='%08d',lastUpdateStatus='%d',"
					+ "totalUpdateCnt='%d',lastUpdateTime=getDate(),maxUpdateBaseCode=case when maxUpdateBaseCode>'%08d' then maxUpdateBaseCode else '%08d' end "
					+ "where processID='%s'",startBaseCode,lastUpdateStatus,totalUpdateCnt,startBaseCode,startBaseCode,processID);
			dbClient.execute(sql3);
			dbClient.commit();
			updateBatch(startBaseCode);
		}
	}
	
	public void updateBatch(int startBaseCode) throws Exception
	{
//		initSearcher();
		logger.info("Current batch start base code:"+startBaseCode);
		logger.info("Current process update numbers:"+totalUpdateCnt);
		while(true)
		{
			String orgCode=NACAO.generateCode(startBaseCode);
			int lastUpdateStatus=0;
			try
			{
				if((resetFlag++)==batchSize)
				{
					searcher.reset();
					resetFlag=0;
				}

				totalUpdateCnt++;
				NACAO nacao=searcher.search(orgCode);
				
				//验证码错误，重新查询
				while(nacao.updateStatus==5)
				{
					logger.info("Validate code recongnized failed,search again...");
					nacao=searcher.search(orgCode);
				}
				//超时错误，重新查询
				while(nacao.updateStatus>3 && nacao.updateStatus<4)
				{
					logger.info("Time out exception for status:"+nacao.updateStatus+",search again...");
//					searcher.reset();
//					resetFlag=0;
//					logger.info("rebuild web driver succeed!");
					nacao=searcher.search(orgCode);
				}
				
				String[] colsAndVals=nacao.getColsAndVals();
				colsAndVals[0]+=",lastUpdateTime";
				colsAndVals[1]+=",getDate()";
				if(nacao.certificateExists==1 && dstTableName.equals("BaiduApp"))
				{
					String certificateSavePath=localhost+"@"+SysConfig.getCertificateSavePath(orgCode);
					colsAndVals[0]+=",certificateSavePath";
					colsAndVals[1]+=",'"+certificateSavePath+"'";
				}

				String insertSql=String.format("insert into %s(%s) values(%s)",dstTableName,colsAndVals[0],colsAndVals[1]);
//				SysConfig.logInfo(insertSql);
				dbClient.execute(insertSql);
				logger.info("orgCode:"+orgCode+",updateStatus:"+nacao.updateStatus);
			} catch (Exception e)
			{
				e.printStackTrace();
				lastUpdateStatus=1;
			}
			
			String sql=String.format("update ProcessStatus set lastUpdateBaseCode='%08d',lastUpdateStatus='%d',totalUpdateCnt='%d',lastUpdateTime=getDate() "
					+ ",maxUpdateBaseCode=case when maxUpdateBaseCode>'%08d' then maxUpdateBaseCode else '%08d' end "
					+ "where processID='%s'",startBaseCode,lastUpdateStatus,totalUpdateCnt,startBaseCode,startBaseCode,processID);
			dbClient.execute(sql);
			dbClient.commit();
			
			//如果本次抓取时间不超过5秒，则休息一秒
			long curTs=System.currentTimeMillis();
			if(curTs-lastUpdateTs<5000)
			{
//				logger.info("Too fast,have a sleep...");
				Thread.sleep(1000);
			}
			lastUpdateTs=curTs;

			if(lastUpdateStatus==1)
			{
				searcher.quit();
				logger.close();
				System.exit(1);
			}
			else if((++startBaseCode)%batchSize==0)
			{
//				dbClient.commit();
				break;	
			}
		}
//		searcher.reset();
	}
	
	public static void run(JobConfig jobConf) throws Exception
	{
		NacaoUpdateJob job = new NacaoUpdateJob();
		job.useProxy=jobConf.getBoolean("useProxy");
		job.setDstTableName(jobConf.getString("dstTableName"));
		job.setLocalhost(jobConf.getString("localhost"));
		job.initSearcher();
		job.run();
	}

	public static void main(String[] args) throws Exception
	{
		NacaoUpdateJob job = new NacaoUpdateJob();
		job.useProxy=false;
		job.setDstTableName("NacaoOrg");
		job.setLocalhost("localhost");
		job.initSearcher();
		job.run();
	}
}
