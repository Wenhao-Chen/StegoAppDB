package app_analysis.asiaccs;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

public class Template {
	
	static final String apk_root = "C:/workspace/app_analysis/apks";
	static final String stego_root = apk_root+"/stego";
	static final String water_root = apk_root+"/watermarking";
	static final String selfie_root = apk_root+"/beautifying";
	
	static final File root = new File("C:\\workspace\\app_analysis");
	static final File notesDir = new File(root, "notes");

	public static List<File> getAPKs()
	{
		List<File> apks = new ArrayList<>();
		for (File dir : new File(apk_root).listFiles()) 
		if (!dir.getName().contentEquals("instrumented"))
		//if (dir.getName().contains("stego"))
		for (File f : dir.listFiles())
		if (f.getName().endsWith(".apk"))
			apks.add(f);
		return apks;
	}
	
	public static List<File> getStegoAPKs()
	{
		List<File> apks = new ArrayList<>();
		for (File dir : new File(apk_root).listFiles()) 
		if (dir.getName().contains("stego"))
		for (File f : dir.listFiles())
		if (f.getName().endsWith(".apk"))
			apks.add(f);
		return apks;
	}
	
	public static TreeSet<File> orderFiles(List<File> list)
	{
		TreeSet<File> apks = new TreeSet<File>(new Comparator<File>() {
			@Override
			public int compare(File f1, File f2)
			{
				return f1.getName().compareTo(f2.getName());
			}
		});
		apks.addAll(list);
		return apks;
	}
}
