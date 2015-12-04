package nacao;

import java.io.IOException;
public class NACAO {

	public String orgCode; //��֯��������
	public String orgName=null; //��������
	public String orgType=null; //��������
	public String validPeriod=null; //��Ч��
	public String issuingAuthority=null; //�䷢��λ
	public String registeredCode=null; //�����Ǽ�֤��
	public String registeredAddress=null; //��ַ
	public String reservea=null; //���ͳһ���ô���
	public int certificateExists=-1; //����֤���Ƿ���ڣ����ڣ�1�������ڣ�0
	public float updateStatus=-1;
	/*
	 * updateStatus->{0��������1���޴˻������룬2���ύ��ѯ�����쳣��3.x��ҳ����Ӧ��ʱ��4������֤�����ʧ�ܣ�5����֤����֤ʧ��,6�����������,7��δ֪}
	 * 
	 * 3.1����֤��ȴ���ʱ��������
	 * 3.2����ѯ����ȴ���ʱ��������
	 * 3.3��Ԥ����������
	 * 3.4��Ԥ����������
	 * 3.5��Ԥ����������
	 * 3.6����ѯ����س�ʱ���ٶ�Ӧ�ã�
	 * 3.7����ѯ������س�ʱ���ٶ�Ӧ�ã�
	 * 3.8��Ԥ�����ٶ�Ӧ�ã�
	 * 3.9��Ԥ�����ٶ�Ӧ�ã�
	 */
	public NACAO(String code,float updateStatus)
	{
		this.orgCode=code;
		this.updateStatus=updateStatus;
	}
	
	public void setUpdateStatus(float updateStatus)
	{
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
	public void setReservea(String reservea)
	{
		this.reservea=reservea;
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
		if(reservea!=null)
		{
			cols.append("reservea,");
			vals.append("'"+reservea+"',");
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
//		NACAO nacao=new NACAO("000000019","���ϸ��¼�����ҵ��������ί�ᾭó�ּ����ල��",0,1);
//		String[] colsAndVals=nacao.getColsAndVals();
//		String cols=colsAndVals[0]+",certificateSavePath,lastUpdateTime";
//		
//		String vals=colsAndVals[1]+",getDate()";
//		String insertSql=String.format("insert into %s(%s) values(%s)","BaiduApp",cols,vals);
		System.out.println(generateCode(830901));
		
	}
}
