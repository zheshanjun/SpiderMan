package nacao;

import java.io.IOException;
public class NACAO {

	String orgCode; //组织机构代码
	String orgName=null; //机构名称
	String orgType=null; //机构类型
	String validPeriod=null; //有效期
	String issuingAuthority=null; //颁发单位
	String registeredCode=null; //机构登记证号
	String registeredAddress=null; //地址
	int certificateExists=-1; //机构证书是否存在，存在：1，不存在：0
	float updateStatus=-1;
	/*
	 * updateStatus->{0：正常，1：无此机构代码，2：提交查询请求异常，3.x：页面响应超时，4：机构证书解析失败，5：验证码验证失败}
	 * 
	 * 3.1：验证码等待超时（官网）
	 * 3.2：查询结果等待超时（官网）
	 * 3.3：预留（官网）
	 * 3.4：预留（官网）
	 * 3.5：预留（官网）
	 * 3.6：查询框加载超时（百度应用）
	 * 3.7：查询结果加载超时（百度应用）
	 * 3.8：预留（百度应用）
	 * 3.9：预留（百度应用）
	 */
	public NACAO(String code,float updateStatus)
	{
		this.orgCode=code;
		this.updateStatus=updateStatus;
	}
	
	public void setOrgName(String orgName)
	{
		this.orgName=orgName;
	}
	public void setOrgType(String orgType)
	{
		this.orgType=orgType;
	}
	public void setValidPeriod(String validPeriod)
	{
		this.validPeriod=validPeriod;
	}
	public void setIssuingAuthority(String issuingAuthority)
	{
		this.issuingAuthority=issuingAuthority;
	}
	public void setRegisteredCode(String registeredCode)
	{
		this.registeredCode=registeredCode;
	}
	public void setRegisteredAddress(String registeredAddress)
	{
		this.registeredAddress=registeredAddress;
	}
	public void setCertificateExists(int certificateExists)
	{
		this.certificateExists=certificateExists;
	}
	
	public String toString()
	{
		return "orgCode:"+orgCode
				+",orgName:"+orgName
				+",orgType:"+orgType
				+",validPeriod:"+validPeriod
				+",issuingAuthority:"+issuingAuthority
				+",registeredCode:"+registeredCode
				+",registeredAddress:"+registeredAddress
				+",updateStatus:"+updateStatus
				+",certificateExists:"+certificateExists;
	}
	
	public String[] getColsAndVals()
	{
		StringBuilder cols=new StringBuilder("orgCode,");
		StringBuilder vals=new StringBuilder("'"+orgCode+"',");
		
		if(orgName!=null)
		{
			cols.append("orgName,");
			vals.append("'"+orgName+"',");
		}
		if(orgType!=null)
		{
			cols.append("orgType,");
			vals.append("'"+orgType+"',");
		}
		if(validPeriod!=null)
		{
			cols.append("validPeriod,");
			vals.append("'"+validPeriod+"',");
		}
		if(issuingAuthority!=null)
		{
			cols.append("issuingAuthority,");
			vals.append("'"+issuingAuthority+"',");
		}
		if(registeredCode!=null)
		{
			cols.append("registeredCode,");
			vals.append("'"+registeredCode+"',");
		}
		if(registeredAddress!=null)
		{
			cols.append("registeredAddress,");
			vals.append("'"+registeredAddress+"',");
		}
		if(certificateExists!=-1)
		{
			cols.append("certificateExists,");
			vals.append(certificateExists+",");
		}
		cols.append("updateStatus");
		vals.append(updateStatus);
		
		return new String[]{cols.toString(),vals.toString()};
	}
	
	public static String generateCode(int baseCode)
	{
		return generateCode(String.format("%08d",baseCode));
	}
	public static String generateCode(String baseCode)
	{
		double[] weight={3,7,9,10,5,8,4,2};
		char checkCode;
		int s=0;
		for(int i=0;i<8;i++)
		{
			int c=(int)baseCode.charAt(i);
			if(c>=48 && c<58) c-=48;
			else c-=55;
			s+=weight[i]*c;
		}
		int c9=11-(s%11);
		if(c9==10)
			checkCode='X';
		else if(c9==11)
			checkCode='0';
		else
			checkCode=(char)(c9+48);
		return baseCode+checkCode;
	}
	
	public static void main(String[] args) throws IOException, InterruptedException
	{
//		NACAO nacao=new NACAO("000000019","济南高新技术产业开发区管委会经贸局技术监督处",0,1);
//		String[] colsAndVals=nacao.getColsAndVals();
//		String cols=colsAndVals[0]+",certificateSavePath,lastUpdateTime";
//		
//		String vals=colsAndVals[1]+",getDate()";
//		String insertSql=String.format("insert into %s(%s) values(%s)","BaiduApp",cols,vals);
//		System.out.println(insertSql);
	}
}
