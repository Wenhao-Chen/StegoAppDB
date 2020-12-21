package app_analysis.trees.test_cfg;

import java.io.File;
import java.util.List;

import app_analysis.common.Dirs;
import util.P;

public class CFG {
	
	
	public static void main(String[] args) {
		
		
		List<File> apks = Dirs.getStegoFiles();
		
		for (File apk : apks) {
			if (apk.getName().contains("dinaga"))
				P.pause("found "+apk.getName());
		}
		
	}
	
	
}
