package tools;

import nacao.NacaoUpdateJob;


public class JobPannel {

	
	public static void run(String[] args) throws Exception
	{
		JobConfig jobConf=new JobConfig(args);
		if(jobConf.jobName.equals("NacaoUpdateJob"))
		{
			NacaoUpdateJob.run(jobConf);
		}
		else if(jobConf.jobName.equals("ParallelExecCmd"))
		{
			String cmd=jobConf.getString("cmd");
//			cmd="java -jar D:/SpiderMan/SpiderMan_fat.jar --jobName=NacaoOrgUpdateJob --provCode=310";
			cmd="java -jar G:/git/SpiderMan/SpiderMan/SpiderMan_fat.jar --jobName=NacaoOrgUpdateJob --provCode=310";			
			int processNbr=Integer.valueOf(jobConf.getString("processNbr"));
			for(int i=0;i<processNbr;i++)
			{
				Thread job = new Thread(new ParallelExecCmd(cmd));
				job.start();
			}
		}
		else if(jobConf.jobName.equals("ConnectNetWork"))
		{
			ConnectNetWork.reconnect();
		}
		
		else
		{
			System.out.println("Please input right jobName!");
		}
	}
	
	public static void main(String[] args) throws Exception
	{
		run(args);
//		System.out.println(Boolean.valueOf("true")==true);
//		System.out.println(Boolean.valueOf("flase")==true);
//		System.out.println(Boolean.valueOf("flase")==false);
	}
}
