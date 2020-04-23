package app_analysis.trees.kMedoid;

import java.io.File;
import java.util.Map;

import ui.ProgressUI;
import util.P;

public class KNN {

	
	static ProgressUI ui;
	static int index;
	public static void main(String[] args)
	{
		ui = ProgressUI.create("Expression Tree progress");
		Map<File, File> treeDirs = ETUtil.getTreeDirs();
		int total = treeDirs.size();
		index = 0;
		treeDirs.forEach((apk,dir)-> {
			P.p(++index+"/"+total+" "+apk.getName()+"...");
		});
		
		P.p("All done. "+treeDirs.size()+" apps.");
	}
	
	

	
	

	
}
