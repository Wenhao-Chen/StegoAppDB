package app_analysis.trees;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import app_analysis.common.Dirs;
import util.F;

public class FilterTrees {

	public static void main(String[] args) {
		
		//for (File apk : Dirs.getFiles(Dirs.ImageApps))
		//	carryTrees(apk.getName());
		for (File apk : Dirs.getStegoFiles())
		if (apk.getName().contains("facebook"))
			carryTrees(apk.getName());
	}
	
	public static void carryTrees(String app) {
		
		File dir1 = new File("C:\\workspace\\app_analysis\\notes\\ExpressionTrees");
		File dir2 = new File("C:\\workspace\\app_analysis\\notes\\Medoid_ExpressionTrees");
		
		File dir = new File(dir1, app);
		File dirr = new File(dir2, app);
		
		F.delete(dirr);
		
		dirr.mkdirs();
		Set<Long> lengths = new HashSet<>();
		if (dir.exists() && dir.isDirectory())
		for (File f : dir.listFiles())
		if (f.getName().endsWith(".expression") && lengths.add(f.length())) {
			File newF = new File(dirr, f.getName());
			F.copy(f, newF);
		}
	}
}
