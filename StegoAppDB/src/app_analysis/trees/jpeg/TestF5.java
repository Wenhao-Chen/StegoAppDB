package app_analysis.trees.jpeg;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.openqa.selenium.SearchContext;

import apex.APEXApp;
import apex.bytecode_wrappers.APEXClass;
import apex.bytecode_wrappers.APEXMethod;
import apex.symbolic.VM;
import apex.symbolic.solver.Logic;
import app_analysis.common.Dirs;
import util.F;
import util.P;

public class TestF5 {
	
	// kcich.stegoqr.apk
	// ca.repl.free.camopic.apk
	// com.paranoiaworks.unicus.android.sse.apk
	// info.guardianproject.pixelknot.apk

	public static void main(String[] args) {
		//quickSearch("0.33126");
		//P.pause("-done");
		File apkFile = new File(
				"C:\\workspace\\android-studio\\StegoAppScripts\\app\\build\\outputs\\apk\\debug\\app-debug.apk");
		//File pixelknot = new File(Dirs.Stego_Github, "info.guardianproject.pixelknot.apk");
		APEXApp app = new APEXApp(apkFile, true);
		P.p(app.packageName);
		
		APEXClass c = app.classes.get("Lwenhaoc/stegodb/stegoappscripts/apps_active/pixelknot/encode/F5CoreEmbed;");
		APEXMethod m = c.getMethodBySubsignature("tempEmbed(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");
		m = c.getMethodBySubsignature("cellToChange()V");
		P.p(m.signature);
		
		Logic.solveConstants = false;
		
		Queue<VM> q = new LinkedList<>();
		q.add(new VM(app, m));
		int run = 0;
		while (!q.isEmpty()) {
			VM vm = q.poll();
			P.pf("- run %d, queue length %d\n", ++run, q.size());
			vm.execute(true);
			q.addAll(vm.otherVMs);
		}
	}
	
	
	static void quickSearch(String key) {
		List<File> apks = Dirs.getAllFiles();
		for (File apkFile : apks) {
			File decodedDir = new File("C:\\workspace\\app_analysis\\decoded", apkFile.getName());
			boolean hasKey = search(decodedDir, key);
			if (hasKey)
				P.p("has key "+apkFile.getName());
		}
	}
	
	static boolean search(File f, String key) {
		if (f.isDirectory()) {
			for(File ff : f.listFiles()) 
			if (search(ff, key))
				return true;
		}
		else if (f.getName().endsWith(".smali")){
			for (String string : F.readLines(f))
				if (string.contains(key))
					return true;
		}
		return false;
	}

}
