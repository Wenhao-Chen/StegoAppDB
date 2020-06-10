package app_analysis.trees;

import java.io.File;
import java.util.List;
import java.util.TreeSet;

import app_analysis.common.Dirs;
import util.P;

public class CountTreeMatchingResults {

	
	public static void main(String[] args) {
		
		// first collect all ref names
		TreeSet<String> allRefNames = new TreeSet<>();
		for (File f : new File("C:\\Users\\C03223-Stego2\\Desktop\\ref_trees").listFiles()) 
		if (f.getName().endsWith(".pdf")){
			String name = f.getName();
			name = name.substring(0, name.indexOf('_', name.indexOf('_')+1));
			allRefNames.add(name);
		}
		P.pf("count");
		for (String s : allRefNames)
			P.pf("\t"+s);
		P.p("");
		File root = new File("C:\\Users\\C03223-Stego2\\Desktop\\normalized_trees");
		List<File> apks = Dirs.getFiles(Dirs.Stego_Github, Dirs.Stego_Others, Dirs.Stego_PlayStore);
		for (File apk : apks) {
			File appDir = new File(root, apk.getName());
			TreeSet<String> matchedRefs = new TreeSet<>();
			for (File f : appDir.listFiles()) 
			if (f.getName().endsWith(".pdf")) {
				String name = f.getName();
				name = name.substring(0, name.indexOf('_', name.indexOf('_')+1));
				matchedRefs.add(name);
			}
			P.pf(""+matchedRefs.size());
			for (String ref : allRefNames)
				P.pf((matchedRefs.contains(ref)?"\tYes":"\tNo"));
			P.p("");
		}
	}
}
