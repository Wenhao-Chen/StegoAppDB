package app_analysis.oakland;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import apex.APEXApp;
import apex.bytecode_wrappers.APEXMethod;
import apex.symbolic.Expression;
import apex.symbolic.VM;
import apex.symbolic.listeners.ExecutionListener;
import app_analysis.trees.exas.TreeFeature.Options;
import ui.ProgressUI;
import util.F;

public abstract class DetectionFramework {

	static class RefTree {
		String id;
		Expression exp;
		public RefTree(String s, Expression e) {
			id = s;
			exp = e;
		}
	}
	File apk;
	APEXApp app;
	boolean redo;
	List<RefTree> ref_trees;
	ExecutionListener execListener;
	File appDir, treeDir, spatialResultF, jpegResultF;
	Options treeMatchingOptions;
	
	static ProgressUI ui_method;
	
	DetectionFramework(File apk) {
		this(apk, false);
	}
	
	DetectionFramework(File apk, boolean redo) {
		this.apk = apk;
		this.redo = redo;
		appDir = new File("C:\\workspace\\app_analysis\\notes\\JDart_Trees", apk.getName());
		appDir.mkdirs();
		treeDir = new File(appDir, "trees");
		spatialResultF = new File(appDir, "spatial_result.txt");
		jpegResultF = new File(appDir, "jpeg_result.txt");
	}
	
	
	public abstract Set<APEXMethod> scopeMethods();
	public abstract void initReferenceTrees();
	public abstract void initExecListener();
	public abstract void initTreeMatchingOptions();
	
	public void detect() {
		//TODO: implement redo option
		
		if (ui_method == null)
			ui_method = ProgressUI.create("within app");
		/** Tree Extraction */
		app = new APEXApp(apk);
		if (app.malfunction)
			return;
		Set<APEXMethod> methods = scopeMethods();
		if (execListener == null)
			initExecListener();
		VM.listener = execListener;
		int mCount = 0;
		for (APEXMethod m : methods) {
			VM.branchVisitTimes.clear();
			Queue<VM> q = new LinkedList<>();
			q.add(new VM(app, m));
			while (!q.isEmpty()) {
				ui_method.newLine(String.format(
						"[sym] %d/%d %s %d", 
						++mCount, methods.size(), m.signature, q.size()));
				VM vm = q.poll();
				vm.execute(true);
				q.addAll(vm.otherVMs);
			}
		}
		
		/** Tree Matching */
		if (ref_trees == null)
			initReferenceTrees();
		List<File> expFiles = new ArrayList<>();
		if (treeDir.listFiles()!=null)
		for (File expF : treeDir.listFiles()) 
		if (expF.getName().endsWith(".expression"))
			expFiles.add(expF);
		int treeCount = 0;
		for (File expF : expFiles) {
			ui_method.newLine(String.format("[tree] %d/%d %s", ++treeCount, expFiles.size(), expF.getName()));
			Expression exp = (Expression) F.readObject(expF);
			
			for (RefTree refExp : ref_trees) {
				
			}
		}
	}
	

}
