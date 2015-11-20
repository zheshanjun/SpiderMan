package nacao;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Point;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.remote.Augmenter;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import tools.Logger;
import tools.ProxyFactory3;
import tools.SysConfig;

public class BaiduAppSearcherChrome extends Searcher{

	String searchPage="http://www.baidu.com/baidu?wd=%D7%E9%D6%AF%BB%FA%B9%B9%B4%FA%C2%EB&tn=monline_4_dg";
	String searchWindowHandle;
//	WebDriver driver=null; 
	
	By appWindowXpath=By.xpath(".//*[@tpl='app_normal']//span[text()='进入应用']");
//	By BAPPIframeXpath=By.xpath("/html/body/div[3]/div[3]/div[1]/div[3]/div[@tpl='app_normal']/div[1]/div[2]/div/div/div[1]/iframe");
	By BAPPIframeXpath=By.xpath(".//iframe[contains(@id,'BAPPIframe')]");
	
	By orgCodeInputXpath=By.xpath(".//*[@id='keyword_1']");
	By validateCodeInputXpath=By.xpath(".//*[@id='validateCode_1']");
	By validateImageXpath=By.xpath(".//*[@id='validateCodeImage_1']");
	By orgCodeSubmitXpath=By.xpath(".//*[@id='con_one_1']/div[1]/ul/li[2]/input[2]");	
	By validateCodeBreakResXpath=By.xpath(".//div[@class='content']/div[@class]");
//	By validateCodeBreakResXpath=By.xpath(".//div[@class='top_2']/div[@class='content']/div[@class]");

	WebElement BAPPIframe=null;
	WebElement orgCodeInput=null;
	WebElement validateCodeInput=null;
	WebElement validateImage=null;
	WebElement orgCodeSubmit=null;
	WebElement searchAgain=null;
	
	String curOrgCode;
	Runtime runtime=Runtime.getRuntime();
	int iframeX,iframeY;

	public BaiduAppSearcherChrome()
	{
		System.setProperty("webdriver.chrome.driver", 
				SysConfig.workDir+"\\chromedriver.exe"); 
	}

	public int initDriver() throws Exception
	{
		if(driver!=null) driver.close();
		logger.info("Initializing web driver...");

//		FirefoxProfile profile = new FirefoxProfile(); 
//		profile.setPreference("startup.homepage_welcome_url.additional",searchPage);
//		driver=new FirefoxDriver(profile);
		ChromeOptions options=new ChromeOptions();
		options.addArguments("excludeSwitches=ignore-certificate-errors");
//		options.add_experimental_option("excludeSwitches", ["ignore-certificate-errors"])
		driver=new ChromeDriver(options);
		driver.get(searchPage);
		driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
		driver.manage().window().maximize();
		logger.info("Loading search page...");
		
		try
		{
//			WebElement appButton=waitForAppWindow();
//			appButton.click();
			BAPPIframe = waitForBAPPIframe();
			driver.switchTo().frame(BAPPIframe);		
			waitForLoadingAppBlock();
			driver.switchTo().defaultContent();
			Point iframeLocation = BAPPIframe.getLocation();
			iframeX=iframeLocation.getX();
			iframeY=iframeLocation.getY();
			driver.switchTo().frame(BAPPIframe);
			
			logger.info("Initialize web driver succeed!");
			return 0;
			
		}
		catch (TimeoutException e) 
		{
			e.printStackTrace();
			logger.info("Load search page time out,web driver will be rebuilt");
			
			return initDriver();
		}
	}

	public int reset() throws Exception
	{
		logger.info("Resetting web driver...");
		try
		{
			String location = waitForTitleLocation().getAttribute("class");
			if(location.equals("top2"))
			{
				searchAgain=waitForSearchAgain();
				searchAgain.click();
			}
			waitForLoadingAppBlock();
			logger.info("Reset web driver succeed!");
			return 0;
		}
		catch (Exception e)
		{
			return initDriver();
		}
	}
	
	public NACAO search(String orgCode) throws IOException
	{
		curOrgCode=orgCode;
		return search(orgCode,1);
	}
	public NACAO search(String orgCode,int t) throws IOException
	{
		NACAO nacao=null;
		float updateStatus=-1;
		WebElement searchResultEle=null;
		
		driver.switchTo().defaultContent();
		driver.switchTo().frame(BAPPIframe);
		if(t>SysConfig.MAX_TRY_TIMES)
		{
			updateStatus=5;
			nacao=new NACAO(orgCode,updateStatus);
			return nacao;
		}
		try
		{
			waitForLoadingAppBlock();
		}
		catch(TimeoutException e)
		{
			e.printStackTrace();
			updateStatus=3.6f; //百度应用查询框加载超时
			nacao =new NACAO(orgCode,updateStatus);
			return nacao;
		}
//		System.out.println("waitForLoadingAppBlock over!");
		orgCodeInput.sendKeys(curOrgCode);
		
		String validateCode="";
		for(int j=0;j<SysConfig.MAX_TRY_TIMES;j++)
		{
			screenShotForValicateCode();
			validateCode=recongnizeValidateCode();
			//识别完成后，删除验证码图片
			new File(SysConfig.getValidateCodeSavePath(curOrgCode)).delete(); 
//			Thread.sleep(1000);
			//验证码识别失败，点击图片刷新
			if("".equals(validateCode))
			{
				validateImage.click();
			}
			else break;
		}
		System.out.println("validateCode:'"+validateCode+"'");
		validateCodeInput.sendKeys(validateCode);
		orgCodeSubmit.click();
		System.out.println("click over!");
//		String searchResultType;
		try
		{
			driver.switchTo().defaultContent();
			driver.switchTo().frame(BAPPIframe);
			searchResultEle=waitForSearchResult();
//			searchResultType=waitForSearchResult().getAttribute("class");
			searchAgain=waitForSearchAgain();
		}
		catch(TimeoutException e)
		{
			e.printStackTrace();
			updateStatus=3.7f; //查询结果加载超时
			nacao =new NACAO(orgCode,updateStatus);
			return nacao;
		}
//		System.out.println("waitForSearchResult over!");
//		String searchResultType=searchResultEle.getAttribute("class");
		//查询结果为机构名称
		if(searchResultEle.getAttribute("class").equals("details_word"))
		{
			String orgName=driver.findElement(By.xpath(".//*[@id='dmzContent']/div[1]/p[2]/span")).getText();
			
			nacao=new NACAO(curOrgCode,0);
			nacao.setOrgName(orgName);
			nacao.setCertificateExists(0);
			searchAgain.click();
			return nacao;
		}
		//查询结果为机构证书,保存证书截图
		else if(searchResultEle.getAttribute("class").equals("details_picture"))
		{
			WebElement certificate=driver.findElement(By.xpath(".//*[@id='dmzContent']/div[1]/p/img"));
			screenShotForCertificate(certificate);
			
			nacao=new NACAO(curOrgCode,0);
			nacao.setCertificateExists(1);
			
			searchAgain.click();
			return nacao;
		}
		else
		{
			String errorInfo=searchResultEle.getAttribute("class").trim();
			searchAgain.click();
			
			//机构代码不存在
			if(errorInfo.startsWith("您输入的信息，查询结果为 0 条"))
			{
				nacao=new NACAO(curOrgCode,1);
				return nacao;
			}
			//验证码识别失败
			else
			{
				return search(orgCode,t+1);	
			}
		}
	}
	
	public WebElement waitForSearchResult()
	{
		return (new WebDriverWait(driver,SysConfig.WAIT_IN_SECONDS,SysConfig.SLEEP_IN_MILLIS)).until(new ExpectedCondition<WebElement>() 
		{
        	public WebElement apply(WebDriver d) 
        	{
        		return driver.findElement(validateCodeBreakResXpath);
        	}        	       	
        });
	}
	
	public WebElement waitForSearchAgain()
	{
		return (new WebDriverWait(driver,SysConfig.WAIT_IN_SECONDS,SysConfig.SLEEP_IN_MILLIS)).until(new ExpectedCondition<WebElement>() 
		{
        	public WebElement apply(WebDriver d) 
        	{
        		return driver.findElement(By.linkText("<< 重新搜索"));
        	}        	       	
        });
	}
	
	public WebElement waitForTitleLocation()
	{
		return (new WebDriverWait(driver,SysConfig.WAIT_IN_SECONDS,SysConfig.SLEEP_IN_MILLIS)).until(new ExpectedCondition<WebElement>() 
		{
        	public WebElement apply(WebDriver d) 
        	{
        		return driver.findElement(By.xpath("html/body/div/div[@class]"));
        	}        	       	
        });
	}
	
	public WebElement waitForBAPPIframe()
	{
		return (new WebDriverWait(driver,SysConfig.WAIT_IN_SECONDS,SysConfig.SLEEP_IN_MILLIS)).until(new ExpectedCondition<WebElement>() 
		{
        	public WebElement apply(WebDriver d) 
        	{
        		return driver.findElement(BAPPIframeXpath);
        	}        	       	
        });
	}
	
	public WebElement waitForAppWindow()
	{
		return (new WebDriverWait(driver,SysConfig.WAIT_IN_SECONDS,SysConfig.SLEEP_IN_MILLIS)).until(new ExpectedCondition<WebElement>() 
		{
        	public WebElement apply(WebDriver d) 
        	{
        		return driver.findElement(appWindowXpath);
        	}        	       	
        });
	}
	
	public Boolean waitForLoadingAppBlock()
	{
		return (new WebDriverWait(driver,SysConfig.WAIT_IN_SECONDS,SysConfig.SLEEP_IN_MILLIS)).until(new ExpectedCondition<Boolean>() 
		{
        	public Boolean apply(WebDriver d) 
        	{
        		orgCodeInput=driver.findElement(orgCodeInputXpath);
        		validateCodeInput=driver.findElement(validateCodeInputXpath);
        		validateImage=driver.findElement(validateImageXpath);
        		orgCodeSubmit=driver.findElement(orgCodeSubmitXpath);
        		return (orgCodeInput!=null && validateCodeInput!=null && validateImage!=null && orgCodeSubmit!=null);
        	}        	       	
        });
	}

	//识别验证码
	public String recongnizeValidateCode() throws IOException
	{
		String validateCode=null;
		
		String cmd="cmd /c "+SysConfig.BDYY_OCR+" "+SysConfig.getValidateCodeSavePath(curOrgCode);
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
		return validateCode;
	}
	
	public void screenShotForCertificate(WebElement certificate) throws IOException
	{
		Point imgLocation = certificate.getLocation();
		Dimension imgSize = certificate.getSize();
		
		WebDriver augmentedDriver = new Augmenter().augment(driver);
		byte[] takeScreenshot = ((TakesScreenshot) augmentedDriver).getScreenshotAs(OutputType.BYTES);
		
		BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(takeScreenshot));
		BufferedImage croppedImage = originalImage.getSubimage(
				iframeX+imgLocation.getX(),
				iframeY+imgLocation.getY(),
				imgSize.getWidth(),
				imgSize.getHeight());
		ImageIO.write(croppedImage, "png", new File(SysConfig.getCertificateSavePath(curOrgCode)));
	}
	
	public void screenShotForValicateCode() throws IOException
	{
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
	
	public static void main(String[] args) throws Exception
	{
		BaiduAppSearcherChrome searcher=new BaiduAppSearcherChrome();
		searcher.setLogger(new Logger("test"));
		searcher.initDriver();
		String[] codeArray={"000000019","802100433","596247871"};
		for(String code:codeArray)
		{
			System.out.println(searcher.search(code));
		}

	}

}
