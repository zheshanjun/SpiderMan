package tools;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class ConnectNetWork {

	static String myAdslTitle="�������";
	static String myAdslName="051689023875111";
	static String myAdslPass="320322";

    /**
     * ִ��CMD����,������String�ַ���
     */
    public static String executeCmd(String strCmd) throws Exception {
        Process p = Runtime.getRuntime().exec("cmd /c " + strCmd);
        StringBuilder sbCmd = new StringBuilder();
        BufferedReader br = new BufferedReader(new InputStreamReader(p
                .getInputStream()));
        String line;
        while ((line = br.readLine()) != null) {
            sbCmd.append(line + "\n");
        }
        return sbCmd.toString();
    }

    /**
     * ����ADSL
     */
    
    public static boolean connAdsl(String adslTitle, String adslName, String adslPass) throws Exception {
        System.out.println("���ڽ�������.");
        String adslCmd = "rasdial " + adslTitle + " " + adslName + " "
                + adslPass;
        String tempCmd = executeCmd(adslCmd);
        // �ж��Ƿ����ӳɹ�
        if (tempCmd.indexOf("������") > 0) {
            System.out.println("�ѳɹ���������.");
            return true;
        } else {
            System.err.println(tempCmd);
            System.err.println("��������ʧ��");
            return false;
        }
    }

    /**
     * �Ͽ�ADSL
     */
    public static boolean cutAdsl(String adslTitle) throws Exception {
        String cutAdsl = "rasdial " + adslTitle + " /disconnect";
        String result = executeCmd(cutAdsl);
       
        if (result.indexOf("û������")!=-1){
            System.err.println(adslTitle + "���Ӳ�����!");
            return false;
        } else {
            System.out.println("�����ѶϿ�");
            return true;
        }
    }
    
    public static boolean reconnect() throws Exception
    {
    	return reconnect(0);
    }
    
    public static boolean reconnect(int tryTimes) throws Exception
    {
    	if(tryTimes==SysConfig.MAX_TRY_TIMES)
    	{
    		return false;
    	}
    	
    	boolean cutRes=cutAdsl(myAdslTitle);
    	if(cutRes==true)
    	{
    		Thread.sleep(2000);
    	}
    	boolean connRes=connAdsl(myAdslTitle, myAdslName, myAdslPass);
    	if(connRes==true)
    	{
    		Thread.sleep(1000);
    		return connRes;
    	}
    	else
    	{
    		return reconnect(tryTimes+1);
    	}
    	
    }
    
    public static void main(String[] args) throws Exception {

    	System.out.println("�û��ĵ�ǰ����Ŀ¼:\n"+System.getProperty("user.dir"));
    }
}