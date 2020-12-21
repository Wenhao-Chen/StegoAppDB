package crawling;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import app_analysis.common.Dirs;
import util.F;
import util.P;


// Requires:
// 1. A list of package names for the apps to download (located in ImageApps.imageAppRecord)
// 2. Install gplaycli: <pip install gplaycli> or go to https://github.com/matlink/gplaycli
// 3. Download PlaystoreDownloader: https://github.com/2MRaza/PlaystoreDownloader

public class PlayStore_Download {

	static final File ImageAppRecord = new File("C:\\workspace\\app_analysis\\apks\\ImageAppNames.txt");
	static final File ImageAppDir = new File("C:\\workspace\\app_analysis\\apks\\image_related");
	
	public static void main(String[] ssssar)
	{
		/*
		List<String> toDownload = new ArrayList<>();
		for (String name : F.readLinesWithoutEmptyLines(ImageAppRecord)) {
			File apk = new File(ImageAppDir, name+".apk");
			if (apk.exists())
				continue;
			toDownload.add(name);
		}

		for (int i=0; i<toDownload.size(); i++) {
			//File tokenF = new File("C:\\Users\\C03223-Stego2\\.cache\\gplaycli\\token");
			//tokenF.delete();
			P.pf("-- progress %d/%d\n", i+1, toDownload.size());
			String name = toDownload.get(i);
			File apk = new File(ImageAppDir, name+".apk");
			
			downloadWith_PlaystoreDownloader(name, apk.getAbsolutePath());
			//downloadWith_gplaycli(name, apk.getAbsolutePath());
		}
		*/
		File apk = new File(Dirs.Download, "com.stegappasaurus.apk");
		downloadWith_PlaystoreDownloader("com.stegappasaurus", apk.getAbsolutePath());
	}
	
	static void downloadWith_PlaystoreDownloader(String packageName, String outPath) {
		String[] args = new String[] {
				"py", "C:\\libs\\PlaystoreDownloader\\download.py", "\""+packageName+"\"",
				"-o", outPath,
				"-c", "C:\\libs\\PlaystoreDownloader\\credentials.json"
		};
		P.exec(String.join(" ", args), true, true);
	}
	
	static void downloadWith_gplaycli(String packageName, String outPath) {
		String confPath = "C:\\Users\\C03223-Stego2\\Documents\\gplaycli.conf";
		String[] args = new String[] {
				"gplaycli", "-c", confPath,
				"-d", packageName,
				"-f", outPath
		};
		P.exec(String.join(" ", args), true, true);
	}

}
