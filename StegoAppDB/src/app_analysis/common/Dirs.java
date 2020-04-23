package app_analysis.common;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Dirs {

	public static final File HOME = new File("C:/Users/C03223-Stego2");
	public static final File Desktop = new File(HOME, "Desktop");
	public static final File Download = new File(HOME, "Downloads");
	public static final File Workspace = new File("C:/workspace/app_analysis");
	
	
	public static final File apk_root = new File(Workspace, "apks");
	public static final File Beautify = new File(apk_root, "beautifying");
	public static final File Watermark = new File(apk_root, "watermarking");
	public static final File PlayStore = new File(apk_root, "z_PlayStore");
	public static final File Stego_Github = new File(apk_root, "stego_github");
	public static final File Stego_PlayStore = new File(apk_root, "stego_playstore");
	public static final File Stego_Others = new File(apk_root, "stego_other");
	
	public static final File GraphRoot = new File(Workspace, "graphs");
	public static final File ICFGRoot = new File(GraphRoot, "icfg");
	
	
	public static List<File> getFiles(File... dirs)
	{
		List<File> res = new ArrayList<>();
		
		for (File dir : dirs)
			for (File f : dir.listFiles())
				res.add(f);
		return res;
	}
}
