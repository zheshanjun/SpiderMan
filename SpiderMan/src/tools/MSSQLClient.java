package tools;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MSSQLClient{

	public Connection conn;
	public int fetchSize=1;
	public String driverName = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
	
	public String connectionURL;
	public String user;
	public String pwd;
	public boolean autoCommit;
	
//	private static String connectionURL = "jdbc:sqlserver://172.16.0.129:1433; DatabaseName=BaiduBaike"; 

	public MSSQLClient(String connectionURL,String user,String pwd,boolean autoCommit) throws ClassNotFoundException, SQLException
	{
		this.connectionURL=connectionURL;
		this.user=user;
		this.pwd=pwd;
		this.autoCommit=autoCommit;
		buildConnection();
	}
	
	public void buildConnection() throws ClassNotFoundException, SQLException
	{
		Class.forName(driverName);
		conn = DriverManager.getConnection(connectionURL, user, pwd);
		conn.setAutoCommit(autoCommit);
	}
	
	public ResultSet execute(String sql) throws SQLException, ClassNotFoundException
	{
//		System.out.println("->\n"+sql);
		if(conn.isClosed())
		{
			buildConnection();
		}
		PreparedStatement ps = conn.prepareStatement(sql);
        ps.setFetchSize(fetchSize);
        ps.execute();
        ResultSet res=ps.getResultSet();
        return res;
	}
	
	public void close() throws SQLException
	{
		conn.close();
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
