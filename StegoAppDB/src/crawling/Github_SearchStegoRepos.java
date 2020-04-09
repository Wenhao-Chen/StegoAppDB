package crawling;

import java.io.File;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import util.F;
import util.P;

public class Github_SearchStegoRepos {

	
	static String chrome_driver_path = "C:/libs/chromedriver.exe";
	
	public static void main(String[] args)
	{
		System.setProperty("webdriver.chrome.driver", chrome_driver_path);
		WebDriver driver = new ChromeDriver();
		
		driver.get("https://github.com/login");
		driver.findElement(By.name("login")).sendKeys(email);
		driver.findElement(By.name("password")).sendKeys(getPW());
		driver.findElement(By.name("commit")).click();
		driver.findElement(By.cssSelector("input[type=\"text\"]")).sendKeys("steg\n");
		List<WebElement> eles = driver.findElements(By.cssSelector("a[class=\"filter-item\"]"));
		for (WebElement ele : eles)
		{
			String href = ele.getAttribute("href");
			if (href != null && href.contains("l=Java&"))
			{
				ele.click();
				P.sleep(1000);
				break;
			}
		}
		driver.get("https://github.com/search?l=Java&p=80&q=steg&type=Repositories");
		P.pause();
		
		PrintWriter out = F.initPrintWriter(new File("E:/repos.txt"), true);
		while (true)
		{
			eles = driver.findElements(By.cssSelector("a[class=\"v-align-middle\"]"));
			for (WebElement ele : eles)
			{
				String href = ele.getAttribute("href");
				P.p("checking "+href);
				if (href!=null)
				{
					out.println(href);
					out.flush();
				}
			}
			
			WebElement next = driver.findElement(By.cssSelector("a[class=\"next_page\"]"));
			if (next == null)
				break;
			next.click();
			P.sleep(2000);
		}
		out.close();
		
		P.pause();
		Set<String> urls = new HashSet<>();
		Random rng = new Random();
		for (int i=1; i<=93; i++)
		{
			driver.get("https://github.com/search?l=Java&p="+i+"&q=steg&type=Repositories");
		    eles = driver.findElements(By.className("v-align-middle"));
		    for (WebElement ele : eles)
		    {
		    	String url = ele.getAttribute("href");
		    	if (url != null)
		    	{
		    		urls.add(url);
		    		System.out.println(url);
		    	}
		    }
		    try
		    {
		    	Thread.sleep(rng.nextInt(1000)+2000);
		    }
		    catch (Exception e)
		    {
		    	e.printStackTrace();
		    }
		}
	    driver.quit();
	    P.p("total urls: "+urls.size());
	}
	
	
	
	
	static String getPW()
	{
		char[] arr = new char[code.length];
		for (int i=0; i<arr.length; i++)
		{
			arr[i] = (char)code[i];
		}
		return new String(arr);
	}
	
	
	static String email = "xjtujack@gmail.com";
	static int[] code = {106, 97, 99, 107, 94, 56, 56, 48, 53, 48, 56 };
}
