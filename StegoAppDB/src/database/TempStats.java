package database;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import util.F;
import util.P;

/**
 * This class calculates the following statistics of the database:
 * 
 *   calculateFileSizes() - file sizes for each device in each category (original, cropped, native, stego)
 *   collectIPhoneSettings() - collect the ISO and exposure time settings of iPhone images
 * 
 * */


public class TempStats {

	public static void main(String[] args)
	{
		//collectIPhoneSettings();
		//calculateFileSizes();
		File root = new File("E:/stegodb_March2019");
		File rroot = new File("G:/stegodb_March2019_JPG_DNG_TIF");
		for (File f : root.listFiles())
		{
			if (f.getName().startsWith("_"))
				continue;
			
			File nat = new File(f, "Native");
			if (!nat.exists())
				continue;
			
			
			//P.p(nat.getAbsolutePath());
			
			File remote = new File(rroot, f.getName());
			
			File tgt = new File(remote, "Native");
			tgt.mkdirs();
			//P.p("  "+tgt.getAbsolutePath());
			for (File ff : nat.listFiles())
			{
				File fff = new File(tgt, ff.getName());
				P.p("copying "+ff.getAbsolutePath());
				F.copy(ff, fff);
			}
		}
	}
	
	/**
	 * 0 Image Path
1 Image Type
2 Image Format
3 Image Scene
4 Image Source Device
5 Image Width
6 Image Height
7 Make
8 Camera Model Name
9 Exposure Time
10 ISO
11 F Number
12 Focal Length
13 Exposure Mode
14 White Balance
15 Backup Machine
16 Backup Path
17 JPG Quality
18 TIF path for DNG
19 Device Model
20 Device Index
21 Scene Label

	 * 
	 * */
	
	static void collectIPhoneSettings()
	{
		File csv = new File("E:/stegodb_March2019/_records/general.csv");
		try
		{
			BufferedReader in = new BufferedReader(new FileReader(csv));
			String line;
			Map<String, TreeMap<String, Integer>> iso = new TreeMap<>();
			Map<String, TreeMap<String, Integer>> expo = new TreeMap<>();
			int j = 0;
			while ((line=in.readLine())!=null)
			{
				String[] parts = line.split(",");
				if (!parts[1].equals("original")) continue;
				if (!parts[4].startsWith("iPhone")) continue;
				String expoS = parts[9];
				String isoS = parts[10];
				
				iso.putIfAbsent(parts[4], new TreeMap<>());
				expo.putIfAbsent(parts[4], new TreeMap<>());
				
				TreeMap<String, Integer> isoOccur = iso.get(parts[4]);
				TreeMap<String, Integer> expoOccur = expo.get(parts[4]);
				
				isoOccur.put(isoS, isoOccur.getOrDefault(isoS, 0)+1);
				expoOccur.put(expoS, expoOccur.getOrDefault(expoS, 0)+1);
				/*
				int i = 0;
				for (String s : parts)
					P.p(i+++" "+s);
				P.pause();*/
				if (j++ %5000 == 4999)
					P.p("done "+j);
			}
			P.p("all done "+j);
			in.close();
			for (String name : iso.keySet())
			{
				P.p(name+" "+iso.get(name).size()+" "+expo.get(name).size());
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	static void calculateFileSizes()
	{
		File notesF = new File("E:/temp_sizes.log");
		Map<String, Long> record = new HashMap<>();
		for (String line : F.readLinesWithoutEmptyLines(notesF))
		{
			String[] parts = line.split(",");
			record.put(parts[0], Long.parseLong(parts[1]));
		}
		File root = new File("E:/stegodb_March2019");
		//P.p("DeviceName,Total Size,Originals Size,Cropped PNG Size,Native Images Size,Stegos Size");
		long total_all = 0;
		for (File d : root.listFiles())
		{
			if (d.getName().startsWith("_"))
				continue;
			
			long bytes_all = getSize(d, record);
			long bytes_ori = getSize(new File(d, "originals"), record);
			long bytes_png = getSize(new File(d, "cropped"), record);
			long bytes_nat = getSize(new File(d, "Native"), record);
			long bytes_ste = getSize(new File(d, "stegos"), record);
			
			//System.out.printf("%s,%.1f,%.1f,%.1f,%.1f,%.1f\n", d.getName(), 
			//		g(bytes_all), g(bytes_ori), g(bytes_png), g(bytes_nat), g(bytes_ste));
			total_all += bytes_all;
			
			if (d.getName().startsWith("iPhone"))
				continue;
			
			P.p("7zip a -tzip F:/stegodb_2019_zipped/"+d.getName()+".zip E:/stegodb_March2019/"+d.getName()+" -v49g");
		}
		PrintWriter out = F.initPrintWriter(notesF);
		for (String key : record.keySet())
			out.println(key+","+record.get(key));
		out.close();
		//P.p("total "+g(total_all));
	}
	
	static double g(long bytes)
	{
		return bytes/1024/1024/1024.0;
	}
	
	static long getSize(File f, Map<String, Long> record)
	{
		if (!f.exists())
			return 0;
		if (f.isDirectory())
		{
			if (record.containsKey(f.getAbsolutePath()))
				return record.get(f.getAbsolutePath());
			long l = 0;
			for (File ff : f.listFiles())
				l += getSize(ff, record);
			record.put(f.getAbsolutePath(), l);
			
			return l;
		}
		else
			return f.length();
	}

}
