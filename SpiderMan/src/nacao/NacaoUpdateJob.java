package nacao;

import java.sql.ResultSet;
import java.util.Date;
import com.microsoft.sqlserver.jdbc.SQLServerException;

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
	public String host; //��ǰ�����IP
	public Searcher searcher;
	public String dstTableName;
	
	public int batchSize=100;
	public int startBaseCode=82400;
	public String changeIP="null";
	public Logger logger;
	
	Date stopTime=SysConfig.sdf.parse(SysConfig.sdf.format(new Date()).substring(0,10)+" 17:45:00");
	
	public NacaoUpdateJob(String ip) throws Exception
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
		searcher.setLogger(logger);
		searcher.initDriver();
	}
	
	public void setDstTableName(String tableName) throws Exception
	{
		dstTableName=tableName;
	}
	
	public void registerProcess() throws Exception
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
			//10����û�и��¹��Ľ��̣���Ϊ����������
			String sql0="update ProcessStatus "
					+"set lastUpdateStatus=1,"
					+ "lastUpdateBaseCode=case when lastUpdateStatus='0' then right('00000000'+cast(lastUpdateBaseCode+1 as varchar(8)),8) else lastUpdateBaseCode end "
					+"where lastUpdateStatus in ('-1','0') "
					+"and DATEDIFF(minute, lastUpdateTime,GETDATE())>10 ";
			dbClient.execute(sql0);
			dbClient.commit();
			
//			String sql1="select lastUpdateBaseCode,processID from ProcessStatus(tablockx) "
//					+"where lastUpdateBaseCode= "
//					+"(select min(lastUpdateBaseCode) from ProcessStatus where lastUpdateStatus='1')";
			
			String sql1="select min(lastUpdateBaseCode) from ProcessStatus(tablockx) where lastUpdateStatus='1'";
			ResultSet res1= dbClient.execute(sql1);
			//����и���ʧ�ܵĽ��̣���Ӵ˽��̵�ʧ�ܴ���ʼ���£�����ʧ�ܽ���״̬��Ϊ9�����ڣ�
			if(res1.next()==true && res1.getObject(1)!=null)
			{
				String lastUpdateBaseCode=res1.getString(1);
//				String failedProcessID=res1.getString(2);
				String sql4=String.format("update ProcessStatus set lastUpdateStatus='9' where lastUpdateBaseCode='%s'",lastUpdateBaseCode);
				dbClient.execute(sql4);
				startBaseCode=Integer.valueOf(lastUpdateBaseCode);
				
				//�����˽����ѳɹ�������һ�����Σ���������ѭ�������»�ȡbatchSize��
				if(startBaseCode%batchSize==0)
				{
					dbClient.commit();
//					System.out.println("startBaseCode:"+startBaseCode);
					continue;
				}
			}
			//��������ڸ����е���һ��batchSize�������㿪ʼ����
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
			
			//�������adsl��ip,ÿ����5�֣��ز�һ��
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
		logger.info("Current batch start base code:"+startBaseCode);
		logger.info("Current process update numbers:"+totalUpdateCnt);
		while(true)
		{
			String orgCode=NACAO.generateCode(startBaseCode);
			int lastUpdateStatus=0;
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
					else
					{
						logger.info("Search job failed.Check log for more information.");
						throw new Exception("Search job failed.Check log for more information.");
					}
				}
				
				String[] colsAndVals=nacao.getColsAndVals();
				colsAndVals[0]+=",lastUpdateTime,host";
				colsAndVals[1]+=",getDate(),"+"'"+host+"'";

				String insertSql=String.format("insert into %s(%s) values(%s)",dstTableName,colsAndVals[0],colsAndVals[1]);
//				SysConfig.logInfo(insertSql);
				dbClient.execute(insertSql);
				logger.info("orgCode:"+orgCode+",updateStatus:"+nacao.updateStatus);
			}
			catch (SQLServerException e)
			{
				String message=e.getMessage();
				if(message.startsWith("Υ���� PRIMARY KEY Լ�� "))
				{
					logger.info("Other process has token over this code.");
					break;
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
				lastUpdateStatus=1;
			}
			
			String sql=String.format("update ProcessStatus set lastUpdateBaseCode='%08d',lastUpdateStatus='%d',totalUpdateCnt='%d',lastUpdateTime=getDate() "
					+ ",maxUpdateBaseCode=case when maxUpdateBaseCode>'%08d' then maxUpdateBaseCode else '%08d' end "
					+ "where processID='%s'",startBaseCode,lastUpdateStatus,totalUpdateCnt,startBaseCode,startBaseCode,processID);
			dbClient.execute(sql);
			dbClient.commit();

			if(lastUpdateStatus==1)
			{
				if(searcher.driverStatus!=-1)
				{
					searcher.quit();
				}
				logger.close();
				System.exit(1);
			}
			else if((++startBaseCode)%batchSize==0)
			{
				searcher.quit();
				searcher.initDriver();
				break;
			}
		}
	}
	
	public static void run(JobConfig jobConf) throws Exception
	{

		NacaoUpdateJob job = new NacaoUpdateJob(jobConf.getString("host"));
		if(jobConf.hasProperty("changeIP"))
		{
			job.changeIP=jobConf.getString("changeIP");
		}

		job.setDstTableName(jobConf.getString("dstTableName"));
//		job.setHost(jobConf.getString("host"));
		job.initSearcher();
		job.run();
	}

	public static void main(String[] args) throws Exception
	{
		NacaoUpdateJob job = new NacaoUpdateJob("localhost");
//		job.setDstTableName("NacaoOrg");
//		job.setHost("localhost");
//		job.delay=5;
//		job.initSearcher();
		job.run();
	}
}
