package app_analysis.asiaccs;

import java.io.File;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import apex.APEXApp;
import apex.bytecode_wrappers.APEXMethod;
import apex.symbolic.APEXObject;
import apex.symbolic.APEXObject.BitmapAccess;
import apex.symbolic.VM;
import ui.ProgressUI;
import util.Crawler;
import util.F;
import util.P;

public class ExtractExpressionTrees {

	static ProgressUI ui;

	public static void main(String[] args)
	{
		if (ui == null)
			ui = ProgressUI.create("Method", 20);
		ProgressUI ui_overall = ProgressUI.create("App", 20);
		
		File treeRoot = new File(Template.notesDir, "ExpressionTrees");
		treeRoot.mkdirs();
		File alreadyDoneF = new File(treeRoot, "AppsAlreadyExtracted.txt");
		File notesRoot = new File(Template.notesDir, "Notes");
		//Set<String> alreadyDone = new HashSet<>(F.readLinesWithoutEmptyLines(alreadyDoneF));
		//TreeSet<File> apks = Template.orderFiles(Template.getStegoAPKs());
		List<File> apks = Template.getStegoAPKs();
		int total = apks.size(), i = 1;
		for (File apk : apks)
		{
			ui_overall.newLine(String.format("doing %5d/%d: %s", i++, total, apk.getName()));
			File appTreeDir = new File(treeRoot, apk.getName());
			appTreeDir.mkdirs();
			File notesDir = new File(notesRoot, apk.getName());
			notesDir.mkdirs();
			File notesF = new File(notesDir, apk.getName()+"_cond123.log");
			if (notesF.exists())
			{
				int countTrees = appTreeDir.list().length/5;
				P.p(apk.getName()+"\t" + countTrees + "\t" + String.join("\t", F.readFirstLine(notesF).split(" ")));
				continue;
			}
			
			APEXApp app = new APEXApp(apk, false);
			boolean[] cond123 = testExtract(appTreeDir, app);
			F.writeLine(apk.getName(), alreadyDoneF, true);
			F.writeLine(String.format("%b %b %b", cond123[0], cond123[1], cond123[2]), notesF, false);
		}
		
		P.p("done");
		tempOperators.forEach(s -> {System.out.print(s);});
	}
	
	static Set<String> tempOperators = new HashSet<>();
	static Set<String> knownOperators = new HashSet<>(Arrays.asList(
			">>>", ">>","&", "|", "<<", "+", "-"
			));

	public static boolean[] testExtract(File expDir, APEXApp app)
	{
		boolean[] cond123 = new boolean[3];
		// find entry point methods
		Set<APEXMethod> entries = FindingEntryPoints.findOrLoad(app);
		int total = entries.size(), index = 0;
		int treeCount = 0;
		// start symbolic execution for each entry point method
		// execute until: (1) all paths explored, (2) path limit reached
		for (APEXMethod m : entries)
		{
			//if (!m.signature.startsWith("Lalexparunov/steganographer/activities/encrypt/EncryptInteractorImpl$EmbedSecretMessage;->doInBackground"))
			//	continue;
			//P.p("doing "+m.signature);
			String entryHash = m.c.getJavaName()+"."+m.getName()+"_"+m.signature.hashCode();
			int expIndex = 1;
			int pathCount = 0, limit = 200;
			Deque<VM> q = new ArrayDeque<>();
			q.add(new VM(app, m));
			index++;
			while (!q.isEmpty() && pathCount < limit)
			{
				ui.newLine(String.format("doing %3d/%d/%03d: %s", index, total, pathCount, m.signature));
				VM vm = q.poll();
				////P.p("[path]"+pathCount+"/"+limit+": "+vm.methodStack.peek().m.signature+" "+vm.methodStack.peek().stmtIndex);
				////String sig = vm.methodStack.peek().m.signature;
				////VM.printDebug = sig.startsWith("Lalexparunov/steganographer/activities/encrypt/EncryptInteractorImpl$EmbedSecretMessage;->doInBackground") ||
				////				sig.startsWith("Lalexparunov/steganographer/algorithms/Embedding;->embedSecretText");
				
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
						F.writeObject(ba.c, new File(expDir, name+".expression"));
						treeCount++;
						
						cond123[0] = MatchingExpressionTrees.HasGetPixel(ba.c);
						cond123[1] = MatchingExpressionTrees.HasGetPixel(ba.c, ba.x, ba.y);
						// find the operator of the GetPixel Value
						List<String> op = MatchingExpressionTrees.getOperatorForGetPixelValue(ba.c, ba.x, ba.y);
						cond123[2] = op.stream().anyMatch(s->(knownOperators.contains(s)));
						//new HashSet<>(op).forEach(s->{if (!knownOperators.contains(s)) P.p("new op: "+s);});
						tempOperators.addAll(op);
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
		return cond123;
	}
	
}
