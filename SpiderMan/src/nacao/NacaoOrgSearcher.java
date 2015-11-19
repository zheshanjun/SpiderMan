package nacao;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.NoSuchWindowException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Point;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.remote.Augmenter;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;
import tools.MSSQLClient;
import tools.SysConfig;

public class NacaoOrgSearcher extends Searcher{

	String searchPage="https://s.nacao.org.cn";
	String searchWindowHandle;
	
//	WebDriver driver=null; 
	
	By validateIframeXpath=By.xpath(".//*[@id='ym-ml']/div/div/div/iframe");
	WebElement orgCodeInput=null;
	By orgCodeInputXpath=By.xpath(".//input[@name='tit0']");
		
	WebElement orgCodeSubmit=null;
	By orgCodeSubmitXpath=By.xpath(".//a[@onclick='submitForm(0)']/input[@type='button']");

	By loadResultTdXpath=By.xpath(".//*[@id='biaodan']/table/tbody/tr/td[@align][not(@valign)]");

	WebElement certificateIframe=null;
	By certificateIframeXpath=By.xpath(".//*[@id='highslide-wrapper-0']/div[1]/div/div/div[2]/iframe");
	
	Runtime runtime=Runtime.getRuntime();
	int iframeX,iframeY;
	String curOrgCode=null;
	
	WebElement validateInput=null;
	By validateInputXpath=By.xpath(".//*[@id='validateCodeId']");
	WebElement validateSubmit=null;
	By validateSubmitXpath=By.xpath("html/body/div[1]/table/tbody/tr[3]/td[3]/input");
	
	public String[] curProxy;
	
	
	public int initDriver() throws Exception
	{
		if(driver!=null) driver.quit();
		logger.info("Initializing web driver...");
		FirefoxProfile profile = new FirefoxProfile(); 
		
		profile.setPreference("startup.homepage_welcome_url.additional",searchPage);
		
		if(proxyFactory!=null)
		{
			String[] proxy=proxyFactory.getProxy(curProxy);
			curProxy=proxy;
			logger.info("curProxy:"+Arrays.toString(curProxy));
			
			String proxyIP=proxy[0];
			int proxyPort=Integer.valueOf(proxy[1]);

			profile.setPreference("network.proxy.type",1);
			profile.setPreference("network.proxy.http",proxyIP);
	        profile.setPreference("network.proxy.http_port",proxyPort);
	        profile.setPreference("network.proxy.ssl",proxyIP);
	        profile.setPreference("network.proxy.ssl_port",proxyPort);
		}
		else
		{
			logger.info("proxyFactory is null.");
		}
		
		try
		{
			driver=new FirefoxDriver(profile);
			driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
			driver.manage().window().maximize();
			searchWindowHandle=driver.getWindowHandle();
			logger.info("Checking orgCode input...");
			
			waitForOrgCodeInput();
			return 0;
		}
		catch(Exception e){
			logger.info(e.toString());
//			e.printStackTrace();
			return initDriver();
		}
	}
	

	
	public NACAO search(String orgCode) throws Exception
	{
		curOrgCode=orgCode;
		NACAO nacao=null;
//		SysConfig.logInfo("step-1 start...");
		//step-1���ύ��ѯ����
		int step1Status=submitOrgCode();
		if(step1Status==1)
		{
			float updateStatus=2;
			nacao=new NACAO(orgCode,updateStatus);
			return nacao;
		}
		
		//�л�����ѯ�������
		switchToWindow();
//		SysConfig.logInfo("step-2 start...");
		//step-2��������֤��
		int step2Status=breakValidateCode();
		if(step2Status==1)
		{
			float updateStatus=3.1f;
			nacao=new NACAO(orgCode,updateStatus);
			return nacao;
		}
		
//		SysConfig.logInfo("step-3 start...");
		//step-3��������ѯҳ����
		nacao=parseSearchResult();
		
//		SysConfig.logInfo("closing window...");
		//�رյ�ǰ��ѯ������ڣ������ز�ѯ����
		driver.close();
//		SysConfig.logInfo("switching to search page...");
		driver.switchTo().window(searchWindowHandle);
		return nacao;
	}

	//step-1���ύ��ѯ����
	public int submitOrgCode() throws IOException
	{
		int res=0;
		try
		{
//			System.out.println("�������");
			orgCodeInput.clear();
			orgCodeInput.sendKeys(curOrgCode);
			orgCodeSubmit.click();
//			System.out.println("�ύ���");
		}
		catch (Exception e)
		{
			logger.info(e.toString());
//			e.printStackTrace();
			res=1;
		}
		return res;
	}
	
	//step-2��������֤��
	public int breakValidateCode() throws IOException, InterruptedException
	{
		int res=0;
		WebElement validateIframe=null;
		try
		{
			validateIframe=waitForValidateIframe();
		}
		catch (Exception e)
		{
			logger.info(e.toString());
//			e.printStackTrace();
			res=1;
			return res;
		}
		Point iframeLocation = validateIframe.getLocation();
		iframeX=iframeLocation.getX();
		iframeY=iframeLocation.getY();
		driver.switchTo().frame(validateIframe);		
		waitForValidateInput();

		//��ͼʶ����֤��
		screenShot(); //��ͼ
		String validateCode=recongnizeValidateCode(); //ʶ��		
		System.out.println("validateCode:"+validateCode);
		//ʶ����ɺ�ɾ����֤��ͼƬ
		new File(SysConfig.getValidateCodeSavePath(curOrgCode)).delete(); 
		validateInput.sendKeys(validateCode);
		validateSubmit.click();
		
		return res;
	}
	
	//step-3��������ѯҳ����
	public NACAO parseSearchResult() throws Exception
	{
		driver.switchTo().defaultContent();
		String orgName=null; //��������
		String orgType=null; //��������
		String validPeriod=null; //��Ч��
		String issuingAuthority=null; //�䷢��λ
		String registeredCode=null; //�����Ǽ�֤��
		String registeredAddress=null; //��ַ
		int certificateExists=0;
		float updateStatus=0;
		NACAO nacao=null;
		WebElement loadResult=null;
		try
		{			
			loadResult=waitForLoadResult();
		}
		catch (TimeoutException e1)
		{
			e1.printStackTrace();
			updateStatus=3.2f;
			nacao=new NACAO(curOrgCode,updateStatus);
			return nacao;
		}
		if("�������0��".equals(loadResult.getText().trim()))
		{
			updateStatus=1;
			nacao=new NACAO(curOrgCode,updateStatus);
		}
		
		else
		{
			//��������
			WebElement nameEle = driver.findElement(By.xpath(".//*[@id='biaodan']/table/tbody/tr/td[2]"));
			orgName=nameEle.getText();
			//���
			WebElement registerNbrEle = driver.findElement(By.xpath(".//*[@id='biaodan']/table/tbody/tr/td[3]"));
			registeredCode=registerNbrEle.getText();
			//֤��
			WebElement certificateEle = driver.findElement(By.xpath(".//*[@id='biaodan']/table/tbody/tr/td[4]"));
			if(!"*".equals(certificateEle.getText()))
			{
				certificateExists=1;
				try
				{
					WebElement imageEle = certificateEle.findElement(By.xpath("/html/body/form/div/table/tbody/tr[2]/td/span/table/tbody/tr/td[4]/a/img"));
					imageEle.click();
					certificateIframe=waitForCertificateIframe();
					String imageSrc = certificateIframe.getAttribute("src");
					
					driver.get(imageSrc);
					By orgTypeXpath = By.xpath(".//*[@id='jglx']");
					By registeredAddressXpath = By.xpath(".//*[@id='jgdz']");
					By validPeriodXpath = By.xpath(".//*[@id='bzrq_zfrq']");
					By issuingAuthorityXpath = By.xpath(".//*[@id='bzjgmc']");
					
					WebElement orgTypeEle = driver.findElement(orgTypeXpath);
					orgType=orgTypeEle.getText().trim();
					WebElement registeredAddressEle = driver.findElement(registeredAddressXpath);
					registeredAddress=registeredAddressEle.getText().trim();
					WebElement validPeriodEle = driver.findElement(validPeriodXpath);
					validPeriod=validPeriodEle.getText().trim();
					WebElement issuingAuthorityEle = driver.findElement(issuingAuthorityXpath);
					issuingAuthority=issuingAuthorityEle.getText().trim();
				}
				catch (Exception e)
				{
					logger.info(e.toString());
					e.printStackTrace();
					updateStatus=3;
				}
				
			}
			nacao= new NACAO(curOrgCode,0);
			nacao.setCertificateExists(certificateExists);
			nacao.setOrgName(orgName);
			nacao.setOrgType(orgType);
			nacao.setValidPeriod(validPeriod);
			nacao.setIssuingAuthority(issuingAuthority);
			nacao.setRegisteredCode(registeredCode);
			nacao.setRegisteredAddress(registeredAddress);
		}
		return nacao;
	}
	
	//ʶ����֤��
	public String recongnizeValidateCode() throws IOException
	{
		String validateCode=null;
		
		String cmd="cmd /c "+SysConfig.ZZJGDM_OCR+" "+SysConfig.getValidateCodeSavePath(curOrgCode);
//		System.out.println(cmd);
		Process process = runtime.exec(cmd);
		BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
		String line=null;
		int i=0;
		while((line=reader.readLine())!=null)
		{
//			System.out.println(line);
			if((++i)==7)
			{
				validateCode=line;
				reader.close();
				break;
			}
		}
		return validateCode;
	}

	public boolean waitForOrgCodeInput()
	{
		return (new WebDriverWait(driver,SysConfig.WAIT_IN_SECONDS,SysConfig.SLEEP_IN_MILLIS)).until(new ExpectedCondition<Boolean>() 
		{
        	public Boolean apply(WebDriver d) 
        	{
        		orgCodeInput=d.findElement(orgCodeInputXpath);
        		orgCodeSubmit=d.findElement(orgCodeSubmitXpath);
        		return (orgCodeInput!=null && orgCodeSubmit!=null);
        	}
        });
	}
	
	public boolean waitForValidateInput()
	{
		return (new WebDriverWait(driver,SysConfig.WAIT_IN_SECONDS,SysConfig.SLEEP_IN_MILLIS)).until(new ExpectedCondition<Boolean>() 
		{
        	public Boolean apply(WebDriver d) 
        	{
        		validateInput = driver.findElement(validateInputXpath);
        		validateSubmit = driver.findElement(validateSubmitXpath);
        		return (validateInput!=null && validateInput!=null);
        	}        	       	
        });
	}
	
	public WebElement waitForCertificateIframe()
	{
		return (new WebDriverWait(driver,SysConfig.WAIT_IN_SECONDS,SysConfig.SLEEP_IN_MILLIS)).until(new ExpectedCondition<WebElement>() 
		{
        	public WebElement apply(WebDriver d) 
        	{
        		return d.findElement(certificateIframeXpath);
        	}        	       	
        });
	}
	
	public WebElement waitForValidateIframe()
	{
		return (new WebDriverWait(driver,SysConfig.WAIT_IN_SECONDS,SysConfig.SLEEP_IN_MILLIS)).until(new ExpectedCondition<WebElement>() 
		{
        	public WebElement apply(WebDriver d) 
        	{
        		return d.findElement(validateIframeXpath);
        	}        	       	
        });
	}
	
	public WebElement waitForLoadResult()
	{
		return (new WebDriverWait(driver,SysConfig.WAIT_IN_SECONDS,SysConfig.SLEEP_IN_MILLIS)).until(new ExpectedCondition<WebElement>() 
		{
			public WebElement apply(WebDriver d) 
        	{
//				loadResultTd=
				return d.findElement(loadResultTdXpath);
        	}        	       	
        });
	}

	public boolean switchToWindow(){  
	    boolean flag = false;  
	    try {  
	        String currentHandle = driver.getWindowHandle();  
	        Set<String> handles = driver.getWindowHandles();  
	        for (String s : handles) {  
	            if (s.equals(currentHandle))  
	                continue;  
	            else {  
	                driver.switchTo().window(s);  
	                flag = true;
//                    System.out.println("Switch to window: successfully!");  
                    break;    
	            }  
	        }  
	    } catch (NoSuchWindowException e) {  
//	        System.out.println("Window: " + " could not found!");
	        e.fillInStackTrace();  
	        flag = false;  
	    }  
	    return flag;  
	}  
	
	//��ͼ��ȡ��֤��
	public void screenShot() throws IOException
	{
		WebElement validateImage = driver.findElement(By.xpath(".//img[@id='validateImage']"));
//		System.out.println(validateImage.getAttribute("src"));
		Point imgLocation = validateImage.getLocation();
		Dimension imgSize = validateImage.getSize();
		
		WebDriver augmentedDriver = new Augmenter().augment(driver);
		byte[] takeScreenshot = ((TakesScreenshot) augmentedDriver).getScreenshotAs(OutputType.BYTES);
		
		BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(takeScreenshot));
		BufferedImage croppedImage = originalImage.getSubimage(
				iframeX+imgLocation.getX(),
				iframeY+imgLocation.getY(),
				imgSize.getWidth(),
				imgSize.getHeight());
		ImageIO.write(croppedImage, "png", new File(SysConfig.getValidateCodeSavePath(curOrgCode)));
	}

	@Override
	public int reset() throws Exception {
		// TODO Auto-generated method stub
		int res=0;
		res=initDriver();
		return res;
		
	}
	
	public static void main(String[] args) throws Exception
	{
		MSSQLClient dbClient = new MSSQLClient(
				String.format("jdbc:sqlserver://%s:1433;DatabaseName=%s",SysConfig.MSSQL_HOST,SysConfig.MSSQL_DB),
				SysConfig.MSSQL_USER, //user
				SysConfig.MSSQL_PWD, //pwd
				true //autoCommit
				);
		
		NacaoOrgSearcher searcher= new NacaoOrgSearcher();
		searcher.addProxyFactory();
		searcher.initDriver();
		String[] codeArray={"802100433","596247871","59502609X","808220081","67452250X","574064548","228560207",
    			"669084461","500011128","579539434","576652132"};
		for(String code:codeArray)
		{
			NACAO nacao=searcher.search(code);
			String[] colsAndVals=nacao.getColsAndVals();
			colsAndVals[0]+=",lastUpdateTime";
			colsAndVals[1]+=",getDate()";
			String insertSql=String.format("insert into %s(%s) values(%s)","NacaoOrg",colsAndVals[0],colsAndVals[1]);
//			logger.info(insertSql);
//			dbClient.execute(insertSql);
		}
	}
}
