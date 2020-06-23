package app_analysis.trees;

import java.io.File;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import apex.APEXApp;
import apex.bytecode_wrappers.APEXMethod;
import apex.symbolic.APEXObject;
import apex.symbolic.APEXObject.BitmapAccess;
import apex.symbolic.VM;
import app_analysis.common.Dirs;
import app_analysis.old.FindingEntryPoints;
import app_analysis.old.MatchingExpressionTrees;
import app_analysis.old.Template;
import ui.ProgressUI;
import util.F;
import util.P;

public class ExtractExpressionTrees_stegos {

	static ProgressUI ui;
	static String appToDo = null;
	static int entryPointMode = 2;
	public static void main(String[] args)
	{
		if (ui == null)
			ui = ProgressUI.create("Method", 20);
		ProgressUI ui_overall = ProgressUI.create("App", 20);
		
		appToDo = "com.aksel";
		entryPointMode = 1; // mode 1 trace back to CG entry points, mode 2 just takes the methods that contain invoke-bitmap APIs
		File treeRoot = new File(Template.notesDir, "ExpressionTrees");
		File notesRoot = new File(Template.notesDir, "Notes");
		List<File> apks = Dirs.getFiles(Dirs.Stego_Github, Dirs.Stego_Others, Dirs.Stego_PlayStore);
		int total = apks.size(), i = 1;
		for (File apk : apks)
		{
			ui_overall.newLine(String.format("doing %5d/%d: %s", i++, total, apk.getName()));
			if (appToDo!=null && !apk.getName().startsWith(appToDo))
				continue;
			File appTreeDir = new File(treeRoot, apk.getName());
			appTreeDir.mkdirs();
			File notesDir = new File(notesRoot, apk.getName());
			notesDir.mkdirs();
			
			if (appToDo != null)
				for (File f : appTreeDir.listFiles())
					f.delete();
			
			boolean[] cond123 = testExtract(appTreeDir, new APEXApp(apk, false));
			ReferenceTrees.singleApp = apk.getName();
			ReferenceTrees.go();
			
			P.p("app done: "+apk.getName());
			File notesF = new File(notesDir, apk.getName()+"_cond123.log");
			F.writeLine(String.format("%b %b %b", cond123[0], cond123[1], cond123[2]), notesF, false);
		}
		
		P.p("done");
	}

	public static boolean[] testExtract(File expDir, APEXApp app)
	{
		boolean[] cond123 = new boolean[3];
		// find entry point methods
		File epFile = new File(FindingEntryPoints.epDir, app.packageName+".apk.ep");
		Set<APEXMethod> entries = //FindingEntryPoints.findOrLoad(app);
									entryPointMode==1?FindingEntryPoints.find(app, epFile): FindingEntryPoints.find2(app, epFile);
		P.p("--- entry point methods ---");
		entries.forEach(k->P.p("  "+k.signature));
		int total = entries.size(), index = 0;
		int treeCount = 0;
		// start symbolic execution for each entry point method
		// execute until: (1) all paths explored, (2) path limit reached
		Map<String, Integer> map = new HashMap<>();
		for (APEXMethod m : entries)
		{
			String entryHash = m.c.getJavaName()+"."+m.getName()+"_"+m.signature.hashCode();
			int expIndex = 1;
			int pathCount = 0, limit = Integer.MAX_VALUE;
			// for a new method, clear the VM path pruning counter
			VM.branchVisitTimes.clear();
			Deque<VM> q = new ArrayDeque<>();
			q.add(new VM(app, m));
			index++;
			while (!q.isEmpty() && pathCount < limit)
			{
				ui.newLine(String.format("doing %3d/%d/%03d/%d: %s", index, total, pathCount, q.size(), m.signature));
				VM vm = q.poll();
				//vm.allocateConcreteBitmap = true;
				vm.execute(true);
				// after a path is executed without crashing, collect expression trees
				if (!vm.crashed)
				{
					pathCount++;
					// check the image alteration summaries (bitmapHistory)
					// for embedding formulas
					for (APEXObject obj : vm.heap.values())
					for (BitmapAccess ba : obj.bitmapHistory)
					if (ba.action.equals("setPixel"))
					{
						String name = entryHash+"_"+expIndex++;
						ba.c.toDotGraph(name, expDir, true);
						ba.c.toDotGraph(name, expDir, false);
						map.put(ba.stmt, map.getOrDefault(ba.stmt, 0)+1);
						
						F.writeObject(ba.c, new File(expDir, name+".expression"));
						treeCount++;
						
						cond123[0] = MatchingExpressionTrees.HasGetPixel(ba.c);
						cond123[1] = MatchingExpressionTrees.HasGetPixel(ba.c, ba.x, ba.y);
					}
					
				}
				else
				{
					//P.p("    "+m.signature+" crashed");
				}
				q.addAll(vm.otherVMs);
			}
			//P.p("  method "+m.signature+" did "+pathCount+" paths.");
		}
		P.pf("Found %d expression trees for app %s. Cond 1/2/3 = %b/%b/%b\n", treeCount, app.packageName, cond123[0], cond123[1], cond123[2]);
		for (String stmt : map.keySet())
			P.pf("  %s %d\n", stmt, map.get(stmt));
		return cond123;
	}
	
}
