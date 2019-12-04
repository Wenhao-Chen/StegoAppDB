package util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Crawler {
	
	public static void main(String[] args)
	{
		try
		{
			Document doc = Jsoup.connect("https://github.com/search?l=Java&q=steg&type=Repositories").get();
			Elements eles = doc.select("div");
			P.p("count = "+eles.size());
			for (Element e: eles)
			{
				P.p(e.attr("class"));
			}
		} 
		catch (Exception e)
		{
			e.printStackTrace();
		}

	}
	
	static void crawlFromText(File f)
	{
		Document doc;
		try
		{
			doc = Jsoup.parse(f, "UTF-8");
			Elements eles = doc.select("a");
			
			File root = new File("C:\\workspace\\app_analysis\\apks");
			Set<String> existing = new HashSet<>();
			getExistingAppPackageNames(root, existing);
			
			int count_total = 0;
			Set<String> names = new HashSet<>();
			for (Element e : eles)
			{
				String href = e.attr("href");
				if (href.startsWith("/store/apps/details") && !names.contains(href))
				{
					count_total++;
					String pk = href.substring(href.indexOf("=")+1);
					if (!existing.contains(pk))
					{
						names.add(pk);
					}
				}
					
			}
			System.out.println("Pulled "+count_total+" package names, "+names.size()+" are new.");
			for (String s : names)
				downloadFromApkPure(s);
				
		} catch (IOException e1)
		{
			e1.printStackTrace();
		}
	}
	
	static void getExistingAppPackageNames(File f, Set<String> names)
	{
		if (f.isDirectory())
		{
			for (File ff :f.listFiles())
				getExistingAppPackageNames(ff, names);
		}
		else if (f.getName().endsWith(".apk"))
			names.add(f.getName().substring(0, f.getName().lastIndexOf(".apk")));
	}
	
	static String randomKeyword()
	{
		Random rng = new Random();
		int length = rng.nextInt(9)+4;
		char[] arr = new char[length];
		
		for (int i = 0; i < length; i++)
			arr[i] = (char)(rng.nextInt(26)+'a');
		
		return new String(arr);
	}
	
	static final String chrome = "\"C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe\"";
	
	static List<String> searchFromPlayStore(String keyword)
	{
		String searchURL = "https://play.google.com/store/search?q="+keyword+"&c=apps";
		List<String> res = new ArrayList<>();
		
		try
		{
			Document doc = Jsoup.connect(searchURL).get();
			Elements eles = doc.select("div[class=\"WHE7ib mpg5gc\"]");
			for (Element e : eles)
			{
				String url = e.selectFirst("div[class=\"vU6FJ p63iDd\"]").selectFirst("a").attr("href");
				res.add(url.substring(url.lastIndexOf("=")+1));
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		return res;
	}
	
	static boolean downloadFromApkPure(String packageName)
	{
		boolean succ = false;
		String prefix = "https://apkpure.com";
		try
		{
			Document doc = Jsoup.connect(prefix+"/search?q="+packageName).get();
			Element e = doc.selectFirst("dl[class=\"search-dl\"]");
			if (e != null)
			{
				Element a = e.selectFirst("a");
				if (a != null)
				{
					//System.out.println(a);
					String title = a.attr("title");
					String href = a.attr("href");
					if (href!=null)
					{
						System.out.println("title: " + title);
						href = prefix+href;
						//System.out.println("href: "+href);
						String dlLink = href+"/download?from=details";
						chrome(dlLink);
						succ = true;
						Thread.sleep(1000);
					}
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return succ;
	}
	
	public static void chrome(String link)
	{
		P.exec(chrome+" "+link, false);
	}
}
