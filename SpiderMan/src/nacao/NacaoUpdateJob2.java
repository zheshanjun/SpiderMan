package nacao;

import java.sql.ResultSet;
import java.util.Date;

import tools.ConnectNetWork;
import tools.JobConfig;
import tools.Logger;
import tools.MSSQLClient;
import tools.SysConfig;

public class NacaoUpdateJob2 {

	public MSSQLClient dbClient;	
	public static String processID;
	public int totalUpdateCnt=0;
	public int totalUpdateBatchCnt=0;
	
	public String host; //��ǰ�����IP
	public Searcher searcher;
	
	public int batchSize=80;
	public int startBaseCode;

	public int stopBaseCode;
	public String changeIP="null";
	public Logger logger;
	
	static String codeTable;
	static String processTable;
	
	Date stopTime=SysConfig.sdf.parse(SysConfig.sdf.format(new Date()).substring(0,10)+" 17:45:00");
	
	public NacaoUpdateJob2(String ip) throws Exception
	{
		host=ip;
		dbClient=new MSSQLClient(
				String.format("jdbc:sqlserver://%s:1433;DatabaseName=%s",SysConfig.MSSQL_HOST,SysConfig.MSSQL_DB),
				SysConfig.MSSQL_USER, //user
				SysConfig.MSSQL_PWD, //pwd
				false //autoCommit
				);
		registerProcess();
		logger=new Logger(processID.replace(":","#"));
	}

	public void initSearcher(String fireFoxPath) throws Exception
	{
		searcher=new NacaoOrgSearcher();
		searcher.setLogger(logger);
		if(!fireFoxPath.equals("default"))
		{
			logger.info("fireFoxPath:"+fireFoxPath);
			searcher.setFireFoxPath(fireFoxPath);
		}
		searcher.initDriver();
	}
	
	public void registerProcess() throws Exception
	{
		String sql0=String.format("select convert(varchar(19),GETDATE(),120)+'_'+right('000'+cast(count(*) as varchar(3)),3) "
				+"from "
				+ processTable+"(tablockx) "
				+"where substring(processID,1,10)=convert(varchar(10),GETDATE(),120)");
		ResultSet res0 = dbClient.execute(sql0);
		res0.next();
		processID=res0.getString(1);
		res0.close();
		
		String sql1=String.format("insert into "+processTable+"(processID,host) values('%s','%s')", processID,host);
		dbClient.execute(sql1);
		dbClient.commit();
	}
	
	public void run() throws Exception
	{
		
		while(startBaseCode<69725481)
		{
			//10����û�и��¹��Ľ��̣���Ϊ����������,�ӹܸý�������
			String sql0="select min(orgCode) from "
					+ codeTable+"(tablockx) "
					+ "where updateStatus=-1 "
					+ "and DATEDIFF(minute, lastUpdateTime,GETDATE())>10";
			ResultSet res0= dbClient.execute(sql0);
			if(res0.next()==true && res0.getObject(1)!=null)
			{
				startBaseCode=Integer.valueOf(res0.getString(1).substring(0,8));
				res0.close();
				stopBaseCode=startBaseCode-(startBaseCode%batchSize)+batchSize-1;
				String sql1=String.format("update "+codeTable+" set lastUpdateTime=getDate() "
						+ "where orgCode between '%08d' and '%08d'",startBaseCode,stopBaseCode);
				dbClient.execute(sql1);
				dbClient.commit();
			}
			else
			{
				String sql2="select max(orgCode) from "+codeTable+"(tablockx) ";
				ResultSet res2= dbClient.execute(sql2);
				if(res2.next()==true && res2.getObject(1)!=null)
				{
					startBaseCode=Integer.valueOf(res2.getString(1).substring(0,8))+1;
					res2.close();
				}
				else
				{
					ResultSet res= dbClient.execute("select max(lastUpdateBaseCode) from "+processTable+"(tablockx)");
					if(res.next()==true && res.getObject(1)!=null)
					{
						startBaseCode=Integer.valueOf(res.getString(1))+1;
					}
					res.close();
				}

				stopBaseCode=startBaseCode-(startBaseCode%batchSize)+batchSize-1;
				for(int i=startBaseCode;i<=stopBaseCode;i++)
				{
					String sql3=String.format("insert into "+codeTable+"(orgCode,updateStatus,lastUpdateTime) "
							+ "values('%s','-1',getDate())",NACAO.generateCode(i));
					dbClient.execute(sql3);
				}
				dbClient.commit();
			}
			
			updateBatch(startBaseCode,stopBaseCode);
			searcher.quitDriver();
			//�������adsl��ip,�ز�һ��
			if(changeIP.equals("ADSL"))
			{
				logger.info("ADSL is reconnecting...");
				if(ConnectNetWork.reconnect()==false)
				{
					logger.info("ADSL reconnecting failed!");
					logger.close();
					System.exit(1);
				}
				else
				{
					logger.info("ADSL reconnecting succeed!");
				}
			}
			searcher.initDriver();
			
			//��ֹͣʱ����޸Ľ���״̬���˳�����
			if(new Date().after(stopTime))
			{
				dbClient.execute(String.format("update "+processTable+" set lastUpdateStatus='9' where processID='%s'",processID));
				dbClient.commit();
				dbClient.close();
				
				searcher.quitDriver();
				logger.info("Time is up,job completed!");
				logger.close();
				System.exit(0);
			}
		}
	}
	
	public int updateBatch(int startBaseCode,int stopBaseCode) throws Exception
	{
		logger.info("Current batch start base code:"+startBaseCode);
		logger.info("Current process update numbers:"+totalUpdateCnt);
		int lastUpdateStatus=0;
		
		dbClient.statement.clearBatch();
		
		for(int baseCode=startBaseCode;baseCode<=stopBaseCode;baseCode++)
		{
			String orgCode=NACAO.generateCode(baseCode);
			try
			{
				totalUpdateCnt++;				
				NACAO nacao=null;
				for(int i=0;i<SysConfig.MAX_TRY_TIMES;i++)
				{
					nacao=searcher.search(orgCode);
					if(nacao.updateStatus==0 || nacao.updateStatus==1)
					{
						break;
					}
					else if(nacao.updateStatus==3)
					{
						logger.info("Time out exception,searcher again...");
						continue;
					}
					else if(nacao.updateStatus==2)
					{
						logger.info("Submit search request failed!");
						continue;
					}
					else if(nacao.updateStatus==6)
					{
						logger.info("Web driver may be died!");
						throw new Exception("Web driver may be died!");
					}
					else
					{
						logger.info("Search job failed.Check log for more information.");
						throw new Exception("Search job failed.Check log for more information.");
					}
				}
				if(nacao.updateStatus==0 || nacao.updateStatus==1)
				{
					String colsAndVals=nacao.toString();
					colsAndVals+=",lastUpdateTime=getDate(),";
					colsAndVals+="host="+"'"+host+"'";
					String updateSql=String.format("update "+codeTable+" set %s where orgCode='%s'",colsAndVals,orgCode);
					
					dbClient.statement.addBatch(updateSql);
					logger.info("orgCode:"+orgCode+",updateStatus:"+nacao.updateStatus);
				}
				else
				{
					logger.info("Search job failed after tried "+SysConfig.MAX_TRY_TIMES+" times");
					throw new Exception("Search job failed after tried "+SysConfig.MAX_TRY_TIMES+" times");
				}
				
			}
			catch (Exception e)
			{
				e.printStackTrace();
				lastUpdateStatus=1;
				if(searcher.driverStatus==0)
				{
					searcher.quitDriver();
				}
				logger.close();
				
				String sql4=String.format("update "+processTable+" set "
						+ "lastUpdateBaseCode='%08d',"
						+ "lastUpdateStatus='%d',"
						+ "totalUpdateCnt='%d',"
						+ "lastUpdateTime=getDate() "
						+ "where processID='%s'",baseCode,lastUpdateStatus,totalUpdateCnt,processID);
				dbClient.statement.addBatch(sql4);
				dbClient.statement.executeBatch();
				dbClient.commit();
				dbClient.commit();
				System.exit(1);
			}
		}
		String sql4=String.format("update "+processTable+" set "
				+ "lastUpdateBaseCode='%08d',"
				+ "lastUpdateStatus='%d',"
				+ "totalUpdateCnt='%d',"
				+ "lastUpdateTime=getDate() "
				+ "where processID='%s'",stopBaseCode,lastUpdateStatus,totalUpdateCnt,processID);
		dbClient.statement.addBatch(sql4);
		dbClient.statement.executeBatch();
		dbClient.commit();

		return lastUpdateStatus;
	}
	
	public static void run(JobConfig jobConf) throws Exception
	{
		codeTable=jobConf.getString("codeTable");
		processTable=jobConf.getString("processTable");
		NacaoUpdateJob2 job = new NacaoUpdateJob2(jobConf.getString("host"));
		if(jobConf.hasProperty("changeIP"))
		{
			job.changeIP=jobConf.getString("changeIP");
		}
		if(jobConf.hasProperty("startBaseCode"))
		{
			job.startBaseCode=jobConf.getInteger("startBaseCode");
		}
		if(jobConf.hasProperty("fireFoxPath"))
		{
			job.initSearcher(jobConf.getString("fireFoxPath"));
		}
		else
		{
			job.initSearcher("default");
		}
		job.run();
	}

	public static void main(String[] args) throws Exception
	{
		codeTable="NacaoOrg2";
		processTable="ProcessStatus2";
		NacaoUpdateJob2 job = new NacaoUpdateJob2("localhost");
		job.startBaseCode=57000000;
		job.initSearcher("default");
		job.run();
	}
}
