package nacao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import tools.ConnectNetWork;
import tools.JobConfig;
import tools.Logger;
import tools.MSSQLClient;
import tools.SysConfig;

public class NacaoUpdateJob {

	public MSSQLClient dbClient;	
	public static String processID;
	public int totalUpdateCnt=0;
	public int totalUpdateBatchCnt=0;
	
	public String today;
	public String host; //当前任务的IP
	public Searcher searcher;
	public String dstTableName;
	
	public int batchSize=100;
	public int resetFlag=0;
	public int startBaseCode=82400;
	public String changeIP="null";
	public int delay=-1;
	public Logger logger;
	
	public long lastUpdateTs=System.currentTimeMillis();

	Date stopTime=SysConfig.sdf.parse(SysConfig.sdf.format(new Date()).substring(0,10)+" 17:30:00");
	
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
	
	public void setHost(String ip)
	{
		this.host=ip;
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
		
		if(changeIP.equals("proxy"))
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
		String sql1=String.format("insert into ProcessStatus(processID,host) values('%s','%s')", processID,host);
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
			//如果有更新失败的进程，则从此进程的失败处开始更新，并将失败进程状态设为9（过期）
			if(res1.next()==true && res1.getObject(1)!=null)
			{
				String lastUpdateBaseCode=res1.getString(1);
//				String failedProcessID=res1.getString(2);
				String sql4=String.format("update ProcessStatus set lastUpdateStatus='9' where lastUpdateBaseCode='%s'",lastUpdateBaseCode);
				dbClient.execute(sql4);
				startBaseCode=Integer.valueOf(lastUpdateBaseCode);
				
				//表明此进程已成功更新完一个批次，跳出本次循环，重新获取batchSize。
				if(startBaseCode%batchSize==0)
				{
					dbClient.commit();
//					System.out.println("startBaseCode:"+startBaseCode);
					continue;
				}
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

			if(new Date().after(stopTime))
			{
				searcher.quit();
				logger.info("Time is up,job completed!");
				logger.close();
				System.exit(0);
			}
			
			//如果设置adsl换ip,每更新5轮，重拨一次
			if(changeIP.equals("ADSL") && (++totalUpdateBatchCnt)%5==0)
			{
				boolean reconnectRes=ConnectNetWork.reconnect();
				if(reconnectRes==false)
				{
					searcher.quit();
					logger.info("ADSL reconnected failed!");
					logger.close();
					System.exit(1);
				}
			}
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
					String certificateSavePath=host+"@"+SysConfig.getCertificateSavePath(orgCode);
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
			
			//如果设置了延迟，且本次抓取时间不超过5秒，则休息一秒
			
			if(delay>0)
			{
				long curTs=System.currentTimeMillis();
				long costMillis=curTs-lastUpdateTs; //本次更新消耗时间
//				System.out.println(costMillis);
				lastUpdateTs=curTs;
				if(costMillis<(delay*1000))
				{
//					logger.info("Too fast,have a sleep...");
					TimeUnit.MILLISECONDS.sleep(delay*1000-costMillis);
				}
			}
			

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
		if(jobConf.hasProperty("changeIP"))
		{
			job.changeIP=jobConf.getString("changeIP");
		}
		if(jobConf.hasProperty("delay"))
		{
			job.delay=jobConf.getInteger("delay");
		}
		
		job.setDstTableName(jobConf.getString("dstTableName"));
		job.setHost(jobConf.getString("host"));
		job.initSearcher();
		job.run();
	}

	public static void main(String[] args) throws Exception
	{
		NacaoUpdateJob job = new NacaoUpdateJob();
//		job.setDstTableName("NacaoOrg");
//		job.setHost("localhost");
//		job.delay=5;
//		job.initSearcher();
		job.run();
	}
}
