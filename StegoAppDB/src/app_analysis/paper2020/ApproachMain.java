package app_analysis.paper2020;

import java.io.File;

import apex.APEXApp;
import app_analysis.asiaccs.FindingEntryPoints;
import ui.ProgressUI;
import util.F;
import util.P;

public class ApproachMain {

	static final String ApkRoot = "C:\\workspace\\app_analysis\\apks";
	
	public static void main(String[] args)
	{
		File[] apkDirs = {
				new File(ApkRoot, "stego_github"),
				new File(ApkRoot, "stego_other"),
				new File(ApkRoot, "stego_playstore"),
				new File("F:\\VirusShare_Android_2018")
		};
		
		APEXApp.defaultDecodedDir = new File("F:\\VirusShare_Decoded");
		APEXApp.justDecode = true;
		
		findIncompleteDecodedDirs();
		//P.pause();
		FindingEntryPoints.epDir = new File("F:\\z_App_Data\\EntryPoints");
		
		
		ProgressUI ui = ProgressUI.create("Decoding Malware", 20);
		int total = apkDirs[3].list().length;
		int index = 1;
		for (File apk : apkDirs[3].listFiles())
		{
			ui.newLine(String.format("Decoding %d/%d: %s", index++, total, apk.getName()));
			APEXApp app = new APEXApp(apk, true);
			//FindingEntryPoints.findOrLoad(apk);
		}
	}
	
	
	static void findIncompleteDecodedDirs()
	{
		for (File dir : new File("F:\\VirusShare_Decoded").listFiles())
		{
			File mani = new File(dir, "AndroidManifest.xml");
			if (!mani.exists() || mani.length()==0)
			{
				P.p("deleting bad decoded dir: "+dir.getAbsolutePath());
				F.delete(dir);
			}
		}
	}
	
	

}
