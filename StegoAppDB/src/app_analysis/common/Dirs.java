package app_analysis.common;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import util.F;
import util.P;

public class Dirs {

	public static final File HOME = new File("C:/Users/C03223-Stego2");
	public static final File Desktop = new File(HOME, "Desktop");
	public static final File Download = new File(HOME, "Downloads");
	public static final File Workspace = new File("C:/workspace/app_analysis");
	
	
	public static final File apk_root = new File(Workspace, "apks");
	public static final File ImageApps = new File(apk_root, "image_related");
	public static final File Beautify = new File(apk_root, "beautifying");
	public static final File Watermark = new File(apk_root, "watermarking");
	public static final File PlayStore = new File(apk_root, "z_PlayStore");
	public static final File Stego_Github = new File(apk_root, "stego_github");
	public static final File Stego_PlayStore = new File(apk_root, "stego_playstore");
	public static final File Stego_Others = new File(apk_root, "stego_other");
	
	public static final File GraphRoot = new File(Workspace, "graphs");
	public static final File ICFGRoot = new File(GraphRoot, "icfg");
	public static final File NotesRoot = new File(Workspace, "notes");
	public static final File ExasRoot = new File(Dirs.NotesRoot, "Exas");
	
	public static final File malwareAPKs = new File("F:\\VirusShare_Android_2018");
	public static final File malwareDecoded = new File("F:\\VirusShare_Decoded");
	
	
	
	public static List<File> getFiles(File... dirs)
	{
		List<File> res = new ArrayList<>();
		
		for (File dir : dirs)
			for (File f : dir.listFiles())
				res.add(f);
		return res;
	}
	
	public static List<File> getStegoFiles() {
		return getFiles(Stego_Github, Stego_Others, Stego_PlayStore);
	}
	
	public static List<File> getAllFiles() {
		return getFiles(Dirs.Stego_Github, Dirs.Stego_Others, Dirs.Stego_PlayStore, Dirs.ImageApps);
	}
	
	public static List<File> get150Apps() {
		List<File> apks = getStegoFiles();
		List<File> images = getFiles(Dirs.ImageApps);
		Random rng = new Random();
		while (apks.size()<150) {
			apks.add(images.remove(rng.nextInt(images.size())));
		}
		return apks;
	}
	public static List<File> get150Apps2() {
		List<File> apks = getStegoFiles();
		List<String> lineStrings = F.readLinesWithoutEmptyLines(new File(Dirs.NotesRoot, "imageApps150.txt"));
		Set<String>[] imageApps = new HashSet[3];
		Set<String> allBadies = new HashSet<>();
		for (int i=0; i < 3; i++) {
			imageApps[i] = new HashSet<String>(Arrays.asList(lineStrings.get(i).split(" ")));
			allBadies.addAll(imageApps[i]);
		}
		Set<String> added = new HashSet<String>();
		for (File apk : apks)
			added.add(apk.getName());
		add(apks, added, imageApps[0], 10);
		add(apks, added, imageApps[1], 7);
		add(apks, added, imageApps[2], 5);
		for (File file : Dirs.getFiles(Dirs.ImageApps)) {
			if (!allBadies.contains(file.getName())) {
				apks.add(file);
				if (apks.size()>150)
					break;
			}
		}
		return apks;
	}
	
	static void add(List<File> apks, Set<String> added, Set<String> from, int count) {
		for (String name : from) {
			File f = new File(Dirs.ImageApps, name);
			if (f.exists() && added.add(name)) {
				apks.add(f);
				if (--count==0)
					return;
			}
		}
	}
	
}











