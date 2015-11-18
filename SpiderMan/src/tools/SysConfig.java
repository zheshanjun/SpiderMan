package tools;

import java.io.IOException;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SysConfig {

//	public static String CERTIFICATE_SAVE_PATH="G:\\git\\SpiderMan\\data\\certificate\\"; //机构证书截图保存路径
//	public static String VALIDATE_CODE_SAVE_PATH="G:\\git\\SpiderMan\\data\\validate\\"; //验证码截图保存路径
//	public static String LOG_FILE_SAVE_PATH="G:\\git\\SpiderMan\\logs\\"; //日志文件保存路径
//	
//	public static String BDYY_OCR="C:\\Users\\likai\\Desktop\\yzm\\bdyy\\bdyy.bat"; //百度应用验证码识别程序
//	public static String ZZJGDM_OCR="C:\\Users\\likai\\Desktop\\yzm\\zzjgdm\\zzjgdm.bat"; //官网验证码识别程序
//	public static String LIAONING_OCR="C:\\Users\\likai\\Desktop\\yzm\\liaoning\\liaoning.bat"; //辽宁识别程序
	
//	public static String MSSQL_HOST="localhost"; //主机
//	public static String MSSQL_USER="sa"; //用户名
//	public static String MSSQL_PWD="likai123"; //密码
//	public static String MSSQL_DB="pachong"; //数据库
	
	public static String CERTIFICATE_SAVE_PATH="D:\\SpiderMan\\data\\certificate\\"; //机构证书截图保存路径
	public static String VALIDATE_CODE_SAVE_PATH="D:\\SpiderMan\\data\\validate\\"; //验证码截图保存路径
	public static String LOG_FILE_SAVE_PATH="D:\\SpiderMan\\logs\\"; //日志文件保存路径
	
	public static String BDYY_OCR="D:\\SpiderMan\\yzm\\bdyy\\bdyy.bat"; //百度应用验证码识别程序
	public static String ZZJGDM_OCR="D:\\SpiderMan\\yzm\\zzjgdm\\zzjgdm.bat"; //官网验证码识别程序
	public static String LIAONING_OCR="D:\\SpiderMan\\yzm\\liaoning\\liaoning.bat"; //辽宁识别程序
//	
	public static String MSSQL_HOST="115.28.63.225"; //主机
	public static String MSSQL_USER="sa"; //用户名
	public static String MSSQL_PWD="lengjing20151016"; //密码
	public static String MSSQL_DB="pachong"; //数据库

	public static int MAX_TRY_TIMES=10;
	public static int WAIT_IN_SECONDS=30;
	public static int SLEEP_IN_MILLIS=250; //default value is 500
	
	public static SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	public static String getCertificateSavePath(String orgCode)
	{
		return CERTIFICATE_SAVE_PATH+"certificate_"+orgCode+".png";
	}
	
	public static String getValidateCodeSavePath(String orgCode)
	{
		return VALIDATE_CODE_SAVE_PATH+"validateCode_"+orgCode+".png";
	}
	
//	public static void logInfo(String info)
//	{
//		System.out.println(sdf.format(new Date())+" -> "+info);
//	}
	
	public static void main(String[] args) throws IOException
	{
		
	}
	
	

}
