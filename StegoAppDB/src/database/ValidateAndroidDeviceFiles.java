package database;

import java.io.File;

import app_analysis.common.Dirs;
import ui.AndroidCommandCenter;
import util.Images;
import util.P;

public class ValidateAndroidDeviceFiles {

	public static void main(String[] args)
	{
		String deviceName = "Samsung S8-1";
		String id = AndroidCommandCenter.deviceIDs.get(deviceName);
		File stegoRoot = new File("E:\\stegodb_March2019\\"+deviceName.replace(" ", "")+"\\stegos");
		File badF = new File(Dirs.Desktop, deviceName+".txt");
		checkReadability(id, stegoRoot, badF);
	}
	
	static void checkReadability(String id, File stegoRoot, File badF) {
		//PrintWriter out = F.initPrintWriter(badF);
		int bad_count = 0;
		for (File appDir : stegoRoot.listFiles())
		for (File img : appDir.listFiles())
		if (img.getName().endsWith(".png"))
		{
			boolean readable = Images.checkReadability(img);
			if (!readable) {
				adb(id, "shell rm /sdcard/Download/StegoDB_March2019/stegos/"+appDir.getName()+"/"+img.getName(), null);
				bad_count++;
			}
		}
		P.p("bad count: "+bad_count);
	}
	
	static void adb(String id, String cmd, String outFileName) {
		String command = AndroidCommandCenter.adbPath+" -s "+id+" "+cmd;
		P.p("[cmd]"+command);
		if (outFileName != null) {
			File outFile = new File(Dirs.Desktop, outFileName+".txt");
			P.exec(command, false, outFile);
		}
		else
			P.exec(command, false);
	}

}
