package nacao;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.NoSuchWindowException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Point;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.remote.Augmenter;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;
import tools.Logger;
import tools.SysConfig;

public class NacaoOrgSearcherChrome extends Searcher{

	String searchPage="https://s.nacao.org.cn";
	String searchWindowHandle;
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
	FirefoxProfile profile = new FirefoxProfile();
//	public String[] curProxy;
	
	public NacaoOrgSearcherChrome()
	{
		profile.setPreference("startup.homepage_welcome_url.additional",searchPage);
	}
	
	public int initDriver() throws Exception
	{
		try
		{
			if(driver!=null) driver.quit();
			logger.info("Initializing web driver...");
			driver=new ChromeDriver();
			driver.get(searchPage);
			driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
			driver.manage().window().maximize();
			searchWindowHandle=driver.getWindowHandle();
			logger.info("Checking orgCode input...");
			waitForOrgCodeInput();
			return 0;
		}
		catch(WebDriverException e)
		{
			logger.info(e.toString());
			driver=null;
		}
		catch(Exception e){
			logger.info(e.toString());
//			e.printStackTrace();
			
		}
		return initDriver();
	}

	public NACAO search(String orgCode) throws Exception
	{
		curOrgCode=orgCode;
		NACAO nacao=null;
		//step-1：提交查询请求
		int step1Status=submitOrgCode();
		//切换到查询结果窗口
		System.out.println("step1Status:"+step1Status);
		System.out.println(driver.getWindowHandles());
		if(step1Status==1 || switchToWindow()==false)
		{
			float updateStatus=2;
			nacao=new NACAO(orgCode,updateStatus);
			return nacao;
		}
		
		//切换到查询结果窗口
//		switchToWindow();
//		SysConfig.logInfo("step-2 start...");
		//step-2：输入验证码
		int step2Status=breakValidateCode();
		if(step2Status==1)
		{
			float updateStatus=3.1f;
			nacao=new NACAO(orgCode,updateStatus);
			driver.switchTo().defaultContent();
		}
		else
		{
			//step-3：解析查询页面结果
			nacao=parseSearchResult();
		}
		
		//关闭当前查询结果窗口，并返回查询窗口
		driver.close();
//		SysConfig.logInfo("switching to search page...");
		driver.switchTo().window(searchWindowHandle);
		return nacao;
	}

	//step-1：提交查询请求
	public int submitOrgCode() throws IOException
	{
		int res=0;
		try
		{
//			System.out.println("输入代码");
			orgCodeInput.clear();
			orgCodeInput.sendKeys(curOrgCode);
			orgCodeSubmit.click();
			
			for(int i=0;i<SysConfig.MAX_TRY_TIMES && driver.getWindowHandles().size()==1;i++)
			{
				System.out.println("i:"+i);
				Thread.sleep(100);
			}
			
//			System.out.println("提交完成");
		}
		catch (Exception e)
		{
			logger.info("Submit orgCode failed.");
			logger.info(e.toString());
//			e.printStackTrace();
			res=1;
		}
		return res;
	}
	
	//step-2：输入验证码
	public int breakValidateCode() throws IOException, InterruptedException
	{
		int res=0;
		try
		{
			WebElement validateIframe=waitForValidateIframe();
			Point iframeLocation = validateIframe.getLocation();
			iframeX=iframeLocation.getX();
			iframeY=iframeLocation.getY();
			driver.switchTo().frame(validateIframe);		
			waitForValidateInput();
			//截图识别验证码
			screenShot(); //截图
			String validateCode=recongnizeValidateCode();
//			System.out.println("validateCode:"+validateCode);
			//识别完成后，删除验证码图片
			new File(SysConfig.getValidateCodeSavePath(curOrgCode)).delete();
			if(validateCode!=null && !validateCode.equals(""))
			{
				validateInput.sendKeys(validateCode);
				validateSubmit.click();
			}
			else
			{
				res=1;
			}
			
		}
		catch (Exception e)
		{
			logger.info("Break validate code failed.");
			logger.info(e.toString());
			
//			e.printStackTrace();
			res=1;
		}

		return res;
	}
	
	//step-3：解析查询页面结果
	public NACAO parseSearchResult() throws Exception
	{
		driver.switchTo().defaultContent();
		String orgName=null; //机构名称
		String orgType=null; //机构类型
		String validPeriod=null; //有效期
		String issuingAuthority=null; //颁发单位
		String registeredCode=null; //机构登记证号
		String registeredAddress=null; //地址
		int certificateExists=0;
		float updateStatus=0;
		NACAO nacao=null;
		WebElement loadResult=null;
		try
		{			
			loadResult=waitForLoadResult();
		}
		catch (Exception e1)
		{
			e1.printStackTrace();
			updateStatus=3.2f;
			nacao=new NACAO(curOrgCode,updateStatus);
			return nacao;
		}
		if("检索结果0条".equals(loadResult.getText().trim()))
		{
			updateStatus=1;
			nacao=new NACAO(curOrgCode,updateStatus);
		}
		
		else
		{
			try
			{
				//机构名称
				WebElement nameEle = driver.findElement(By.xpath(".//*[@id='biaodan']/table/tbody/tr/td[2]"));
				orgName=nameEle.getText();
				//编号
				WebElement registerNbrEle = driver.findElement(By.xpath(".//*[@id='biaodan']/table/tbody/tr/td[3]"));
				registeredCode=registerNbrEle.getText();
				//证书
				WebElement certificateEle = driver.findElement(By.xpath(".//*[@id='biaodan']/table/tbody/tr/td[4]"));
				if(!"*".equals(certificateEle.getText()))
				{
					certificateExists=1;
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
			}
			catch (Exception e)
			{
				logger.info(e.toString());
				e.printStackTrace();
				updateStatus=3;
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
	
	//识别验证码
	public String recongnizeValidateCode() throws IOException
	{
		String validateCode=null;
		String cmd="cmd /c "+SysConfig.ZZJGDM_OCR+" "+SysConfig.getValidateCodeSavePath(curOrgCode);
		
		try
		{
			Process process = runtime.exec(cmd);
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line=null;
			int i=0;
			while((line=reader.readLine())!=null)
			{
				if((++i)==7)
				{
					validateCode=line;
					reader.close();
					break;
				}
			}
		}
		catch (Exception e)
		{
			logger.info("Recongnize validate code failed.Try again...");
			logger.info(e.toString());
//			return recongnizeValidateCode();
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
				return d.findElement(loadResultTdXpath);
        	}        	       	
        });
	}

	public boolean switchToWindow(){  
	    boolean flag = false;  
	    try {  
//	        String currentHandle = driver.getWindowHandle();  
	        Set<String> handles = driver.getWindowHandles();  
	        for (String s : handles) {  
	            if (s.equals(searchWindowHandle))  
	                continue;  
	            else {  
	                driver.switchTo().window(s);  
	                flag = true;
                    break;    
	            }  
	        }  
	    } catch (NoSuchWindowException e) {  
	        e.fillInStackTrace();  
	        flag = false;  
	    }  
	    return flag;  
	}  

	//截图获取验证码
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

//	@Override
//	public int reset() throws Exception {
//		// TODO Auto-generated method stub
//		int res=0;
//		res=initDriver();
//		return res;
//		
//	}
	
	public static void main(String[] args) throws Exception
	{
		
		NacaoOrgSearcherChrome searcher= new NacaoOrgSearcherChrome();
		searcher.setLogger(new Logger("test"));
		searcher.addProxyFactory();
		searcher.initDriver();
		System.out.println(searcher.search("802100433"));
//		String[] codeArray={"802100433","596247871","59502609X","808220081","67452250X","574064548","228560207",
//    			"669084461","500011128","579539434","576652132"};
//		for(String code:codeArray)
//		{
//			NACAO nacao=searcher.search(code);
//			String[] colsAndVals=nacao.getColsAndVals();
//			colsAndVals[0]+=",lastUpdateTime";
//			colsAndVals[1]+=",getDate()";
//			String insertSql=String.format("insert into %s(%s) values(%s)","NacaoOrg",colsAndVals[0],colsAndVals[1]);
////			logger.info(insertSql);
////			dbClient.execute(insertSql);
//		}
	}
}
