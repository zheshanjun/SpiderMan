package tools;

import gs.LnGsUpdateJob;
import nacao.BaiduAppSearcher;
import nacao.NacaoUpdateJob;
import nacao.NacaoUpdateJob2;
import nacao.NacaoUpdateJob3;



public class JobPannel {

	
	public static void run(String[] args) throws Exception
	{
		JobConfig jobConf=new JobConfig(args);
//		if(jobConf.jobName.equals("NacaoUpdateJob"))
//		{
//			NacaoUpdateJob.run(jobConf);
//		}
		if(jobConf.jobName.equals("NacaoUpdateJob"))
		{
			NacaoUpdateJob2.run(jobConf);
		}
		else if(jobConf.jobName.equals("NacaoUpdateJob3"))
		{
			NacaoUpdateJob3.run(jobConf);
		}
		else if(jobConf.jobName.equals("LnGsUpdateJob"))
		{
			LnGsUpdateJob.run(jobConf);
		}
		else if(jobConf.jobName.equals("ConnectNetWork"))
		{
			ConnectNetWork.reconnect();
		}
		else if(jobConf.jobName.equals("test"))
		{
			BaiduAppSearcher.test(jobConf);
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
