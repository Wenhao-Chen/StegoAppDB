package app_analysis.asiaccs;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import apex.APEXApp;
import ui.ProgressUI;
import util.Apktool;
import util.F;
import util.P;

public class KeywordSearch {

	static ProgressUI ui, ui_file;
	/**
	 * Check the effectiveness of keyword searches
	 * Keys: "steg", "embed", "encode", "LSB", "f5"
	 * 
	 * Search for constant strings from the following areas:
	 * 	Class names
	 *  method names
 		variable names in debuginfo
 		value of string variables
 		XML strings
	 * */
	
	static String[] keywords = {
			"steg","stego","steganography",
			"embed","encode",
			"hide","LSB", "secret"
	};
	
	public static void main(String[] args)
	{
		
		if (ui == null)
			ui = ProgressUI.create("App", 20);
		
		if (ui_file == null)
			ui_file = ProgressUI.create("File", 20);
		
		Set<String> alreadyDone = new HashSet<>();
		Map<String, String> info = new HashMap<>();
		File keywordsF = new File(Template.notesDir, "keywords.csv");
		F.readLinesWithoutEmptyLines(keywordsF).forEach(s->{
			String apkName = s.substring(0, s.indexOf("\t"));
			alreadyDone.add(apkName);
			info.put(apkName, s);
		});
		TreeSet<File> apks = Template.orderFiles(Template.getAPKs());
		int total = apks.size(), i = 1;
		for (File apk : apks)
		{
			String group = apk.getParentFile().getName();
			ui.newLine(String.format("doing %5d/%d: %s", i++, total, apk.getName()));
			if (alreadyDone.contains(apk.getName()))
			{
				P.p(group+"\t"+info.get(apk.getName()));
				continue;
			}
				
			File outDir = new File(APEXApp.defaultDecodedDir, apk.getName());
			if (!outDir.exists())
				Apktool.decode(apk, outDir);
			Map<String, Integer> occurences = new TreeMap<>();
			Arrays.stream(keywords).forEach(s->{occurences.put(s, 0);});
			search(outDir, occurences);
			F.write(apk.getName(), keywordsF, true);
			Arrays.stream(keywords).forEach(k->{P.p(k);F.write("\t"+occurences.get(k), keywordsF, true);});
			F.write("\n", keywordsF, true);
		}
		P.p("done");
	}
	
	
	static void search(File root, Map<String, Integer> keywords)
	{
		// collect all the smali folders
		Queue<File> smalis = new LinkedList<>();
		Arrays.stream(root.listFiles()).forEach(dir->{
			if (dir.getName().contentEquals("smali") || dir.getName().startsWith("smali_classes"))
				smalis.add(dir);
		});
		while (!smalis.isEmpty())
		{
			File smali = smalis.poll();
			if (smali.isDirectory())
				smalis.addAll(Arrays.asList(smali.listFiles()));
			else if (smali.getName().endsWith(".smali"))
				searchFromSmali(smali, keywords);
		}
		
		// collect all the XMLs
		Queue<File> xmls = new LinkedList<>();
		xmls.add(new File(root, "res"));
		while (!xmls.isEmpty())
		{
			File xml = xmls.poll();
			if (xml.isDirectory())
				xmls.addAll(Arrays.asList(xml.listFiles()));
			else if (xml.getName().endsWith(".xml"))
				searchFromXML(xml, keywords);
		}
	}
	
	// Searching keywords from a smali file:
	//   read all the words (separated by space character) and match
	static void searchFromSmali(File f, Map<String, Integer> keywords)
	{
		if (ui_file!=null)
			ui_file.newLine("Scanning smali: "+f.getAbsolutePath());
		F.readLines(f).forEach(line->{ checkLine(line, keywords); });
	}
	
	
	// Search keywords from Android XML files. There are layout-related XMLs and resource-related XMLs. Check:
	//    any attribute values of "android:text"
	//    any text from <string> elements
	//    any text from <item> elements with parent <string-array>
	static void searchFromXML(File f, Map<String, Integer> keywords)
	{
		//f = new File("C:\\workspace\\app_analysis\\decoded\\sk.panacom.stegos.apk\\res\\values\\strings.xml");
		//P.p("searching from: "+f.getAbsolutePath());
		if (ui_file!=null)
			ui_file.newLine("Scanning XML: "+f.getAbsolutePath());
		try
		{
			Document doc = Jsoup.parse(f, "UTF-8");
			doc.getAllElements().forEach(e->{
				checkLine(e.attr("android:text"), keywords);
				if (e.tagName().contentEquals("string"))
					checkLine(e.text(), keywords);
				if (e.tagName().contentEquals("string-array"))
					e.getElementsByTag("item").forEach(item->{
						checkLine(item.text(), keywords);
					});
			});
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	static void checkLine(String s, Map<String, Integer> map)
	{
		if (s==null || s.isEmpty())
			return;
		Arrays.stream(s.split(" ")).forEach(word->{
			String w = word.toLowerCase();
			if (map.containsKey(w))
			{
				map.put(w, map.get(w)+1);
				P.p("keyword "+w+" "+map.get(w));
			}
		});
	}

}
