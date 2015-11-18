package tools;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class MSSQLClient{

	public Connection conn;
	public int fetchSize=1;
	public String driverName = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
//	private static String connectionURL = "jdbc:sqlserver://172.16.0.129:1433; DatabaseName=BaiduBaike"; 

	public MSSQLClient(String connectionURL,String user,String pwd,boolean autoCommit) throws ClassNotFoundException, SQLException
	{
		Class.forName(driverName);
		conn = DriverManager.getConnection(connectionURL, user, pwd);
		conn.setAutoCommit(autoCommit);
	}
	
	public ResultSet execute(String sql) throws SQLException
	{
//		System.out.println("->\n"+sql);
		ResultSet rs=null;
        try  
        {  	
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setFetchSize(fetchSize);
            ps.execute();
            rs=ps.getResultSet();
            
        }catch(Exception ex)  
        {  
            System.out.println("Exception: "+ex.getMessage());  
            System.out.println(sql);
//            commit();
            conn.close(); 
        }  
        
        return rs;
	}
	
	public void commit() throws SQLException
	{
		conn.commit();
	}
	
	public static void test() throws SQLException, ClassNotFoundException
	{
		MSSQLClient msc=new MSSQLClient("jdbc:sqlserver://115.28.63.225:1433;DatabaseName=pachong",
				"sa","lengjing20151016",true);
//		String sql="select top 1 * from used";
		String sql="select 1; select 2";
		ResultSet res = msc.execute(sql);

		res.next();
		System.out.println(res.getString(1));
		res.next();
		System.out.println(res.getString(1));
	}
	
	public static void main(String[] args) throws ClassNotFoundException, SQLException
	{
		test();
	}
}
