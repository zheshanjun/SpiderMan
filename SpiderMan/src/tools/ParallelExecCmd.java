package tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ParallelExecCmd implements Runnable{

	public String cmd;
	public ParallelExecCmd(String cmd)
	{
		this.cmd=cmd;
	}
	@Override
	public void run() {
		// TODO Auto-generated method stub
		Runtime runtime=Runtime.getRuntime();
		try {
			Process process = runtime.exec("cmd /c "+cmd);
			InputStream inputStream = process.getInputStream();
//			InputStream error = process.getErrorStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
			String line = null;
			while ((line = reader.readLine()) != null) 
			{
				System.out.println(line);
				Thread.yield();
			}

//			BufferedReader reader2 = new BufferedReader(new InputStreamReader(error));
//			String line2 = null;
//			while ((line2 = reader2.readLine()) != null) 
//			{
//				System.out.println(line2);
//				Thread.yield();
//			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
//		System.out.println("**********");
	}
}
