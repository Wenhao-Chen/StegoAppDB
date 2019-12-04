package util;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class DalvikCrawler {

	public static void main(String[] args)
	{
		int opWidth = 24+7;
		File d = new File("C:/Users/C03223-Stego2/Desktop/dalvik_bytecode.txt");
		List<String> lines = F.readLines(d);
		for (String s : lines) //
		{
			if (s.isEmpty())
				continue;
			int i = s.indexOf(" ");
			String op = i==-1?s:s.substring(0, i);
			op = "case \""+op+"\"";
			while (op.length()<opWidth)
				op+=" ";
			op += ": "+(i==-1?"":"\t// "+s.substring(i+1));
			P.p(op);
		}
	}
	
	
	public static void crawl()
	{
		String url = "https://source.android.com/devices/tech/dalvik/dalvik-bytecode";
		
		try
		{
			Document doc = Jsoup.connect(url).get();
			Elements trs = doc.selectFirst("table[class=\"instruc\"]").selectFirst("tbody").select("tr");
			for (Element tr : trs)
			{
				P.p(tr.child(1).text());
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
}
