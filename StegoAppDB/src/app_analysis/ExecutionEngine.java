package app_analysis;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;

import apex.APEXApp;
import apex.bytecode_wrappers.APEXClass;
import apex.bytecode_wrappers.APEXMethod;
import apex.graphs.CallGraph;
import apex.symbolic.APEXObject;
import apex.symbolic.APEXObject.BitmapAccess;
import apex.symbolic.Expression;
import apex.symbolic.VM;
import ui.ProgressUI;
import util.Dalvik;
import util.F;
import util.Graphviz;
import util.P;

public class ExecutionEngine {

	public static final int pathLimit = 100;
	public static ProgressUI ui, app_progress;
	public static Queue<VM> otherVMs;
	public static File output_summary = null;
	
	public static void findImageAlterationMethods(APEXApp app, File recordF, File notesF, boolean redo)
	{
		File expressionGraphDir = new File(Graphviz.defaultExpressionGraphDir, app.apk.getName());
		expressionGraphDir.mkdirs();
		
		if (redo)
		{
			recordF.delete();
			notesF.delete();
		}
		
		List<APEXMethod> methods = new ArrayList<>();
		Optimizer opt = new Optimizer(app);
		for (APEXClass c : app.getNonLibraryClasses())
		{
			for (APEXMethod m : c.methods.values())
			{
				if (opt.shouldGoIn(m) && !methods_to_skip.contains(m.signature))
					methods.add(m);
			}
		}
		CallGraph cg = new CallGraph(app);
		int num = 1, total = methods.size(), alter = 0,unknown = 0;
		Set<String> alreadyDone = new HashSet<>();
		Set<String> alreadyAlter = new HashSet<>();
		if (!redo && recordF.exists())
		{
			List<String> methodInfos = F.readLines(recordF);
			for (String line : methodInfos)
			{
				String[] parts = line.split(",");
				alreadyDone.add(parts[0]);
				if (!parts[1].equals("unknown"))
				{
					//P.p("   image alteration method: " + parts[0]);
					alreadyAlter.add(parts[0]);
					alter++;
				}
				else
					unknown++;
			}
		}
		num += alreadyDone.size();
		cg.updateMethodCallGraph(alreadyDone);
		File cgDir = new File("C:/workspace/app_analysis/graphs/cg");
		cgDir.mkdirs();
		Graphviz.makeDotGraph(cg.getDotGraph(), app.packageName+"_CG_big", cgDir);
		List<Set<CallGraph.Vertex>> island1 = cg.countIslands();
		cg.updateMethodCallGraph(alreadyAlter);
		Graphviz.makeDotGraph(cg.getDotGraph(), app.packageName+"_CG_trimmed", cgDir);
		List<Set<CallGraph.Vertex>> island2 = cg.countIslands();
		//P.p(app.packageName+" method count: " + methods.size()+". Island1/Island2 = " + island1+"/"+island2);
		for (APEXMethod m : methods)
		{
			if (alreadyDone.contains(m.signature))
				continue;
			P.p("doing method " + num+++"/"+total+": "+m.signature);
			String c = Dalvik.DexToJavaName(m.signature.substring(0, m.signature.indexOf("->")));
			String mm = m.signature.substring(m.signature.indexOf("->")+2, m.signature.indexOf("(")).replace("<", "").replace(">", "");
			String params = m.signature.substring(m.signature.indexOf("(")+1, m.signature.indexOf(")"));
			String methodHash = c+"_"+mm+"_"+params.hashCode();
			otherVMs = new LinkedList<>();
			Result res = exec(app, m);
			int setCount = 0;
			if (res.vm!=null)
			{
				for (APEXObject obj : res.vm.heap.values())
				{
					for (BitmapAccess a : obj.bitmapHistory)
					{
						if (a.action.startsWith("set"))
						{
							setCount++;
							
							String name = methodHash+"_"+setCount;
							
							if (a.c != null)
							{
								a.c.toDotGraph(name, expressionGraphDir, true);
								a.c.toDotGraph(name, expressionGraphDir, false);
								F.writeObject(a.c, new File(expressionGraphDir, name+".expression"));
							}
						}
					}
				}
			}
			if (setCount>0)
			{
				String type = "alter";
				F.writeLine(m.signature+","+type+","+res.numBranches, recordF, true);
				F.writeLine("CtrlF,"+m.signature+","+type+","+res.numBranches, notesF, true);
				P.p("   image alteration method: " + m.signature+". Num branches = "+res.numBranches);
				printPixelOrder3(res.vm, notesF);
				alter++;
			}
			else
			{
				String type = "unknown";
				F.writeLine(m.signature+","+type+","+res.numBranches, recordF, true);
			}
		}
		F.writeLine(String.format("%s\t%d\t%d\t%d", app.packageName, total, alter, unknown), output_summary, true);
		P.pf("%s\t%s\t%d\t%d\t%d\n", app.apk.getParentFile().getName(), app.packageName, island1.size(), island2.size(), alter);
		File resultF = new File(recordF.getParentFile(), app.apk.getName()+"_result.txt");
		F.writeLine(String.format("%s\t%s\t%d\t%d\t%d\n", app.apk.getParentFile().getName(), app.packageName, island1.size(), island2.size(), alter),
				resultF, false);
		for (Set<CallGraph.Vertex> set : island2)
		{
			F.writeLine("-- Island ---", resultF, true);
			for (CallGraph.Vertex v : set)
				F.writeLine("  "+v.m.signature, resultF, true);
			F.writeLine("", resultF, true);
		}
		//P.p(app.packageName+": # image alteration methods: "+alter+". # Connected CGs: "+island2);
	}
	
	public static void findImageAlterationMethods2(APEXApp app, File recordF, File notesF, boolean redo)
	{
		List<APEXMethod> methods = new ArrayList<>();
		Optimizer opt = new Optimizer(app);
		for (APEXClass c : app.getNonLibraryClasses())
		{
			//methods.addAll(c.methods.values());
			for (APEXMethod m : c.methods.values())
			{
				if (opt.shouldGoIn(m) && !methods_to_skip.contains(m.signature))
					methods.add(m);
			}
		}
		P.p("\n------------ app " + app.packageName+". Method count: " + methods.size());
		int num = 1, total = methods.size(), alter = 0, stego = 0;
		Set<String> alreadyDone = new HashSet<>();
		if (!redo && recordF.exists())
		{
			List<String> methodInfos = F.readLines(recordF);
			for (String line : methodInfos)
			{
				String[] parts = line.split(",");
				alreadyDone.add(parts[0]);
				if (parts[1].equals("alter"))
				{
					alter++;
					P.p("   image alteration method: " + parts[0]);
				}
				else if (parts[1].equals("stego"))
				{
					alter++;
					stego++;
					P.p("   potential stego method: " + parts[0]);
				}
			}
		}
		//CallGraph cg = Graphviz.makeCG(app, methods);
		if (redo)
			F.write("", recordF, false);
		
		for (APEXMethod m : methods)
		{
			if (alreadyDone.contains(m.signature))
				continue;
			//Graphviz.makeCFG(app, m.signature);
			otherVMs = new LinkedList<>();
			P.p("doing method " + num+++"/"+total+": "+m.signature);
			Result res = exec(app, m);
			if (res.type == 1)
			{
				F.writeLine(m.signature+",stego"+","+res.numBranches, recordF, true);
				F.writeLine("CtrlF,"+m.signature+",stego"+","+res.numBranches, notesF, true);
				P.p("   potential stego method: " + m.signature+". Num branches = "+res.numBranches);
				printPixelOrder3(res.vm, notesF);
				stego++;
				alter++;
			}
			else if (res.type == 2)
			{
				F.writeLine(m.signature+",alter"+","+res.numBranches, recordF, true);
				F.writeLine("CtrlF,"+m.signature+",alter"+","+res.numBranches, notesF, true);
				P.p("   image alteration method: " + m.signature+". Num branches = "+res.numBranches);
				printPixelOrder3(res.vm, notesF);
				alter++;
			}
			else
			{
				F.writeLine(m.signature+",unknown", recordF, true);
			}
				
		}
		P.p("Finished analyzing app "+app.packageName+". Found " + alter+" image alteration methods, "+ stego+" of those are probably stego methods.");
	}

	
	private static void printPixelOrder3(VM vm, File f)
	{
		Set<String> relevantObj = new TreeSet<>();
		for (APEXObject obj : vm.heap.values())
		{
			if (!obj.bitmapHistory.isEmpty())
			{
				relevantObj.addAll(obj.print(f));
			}
		}
		for (String id : relevantObj)
		{
			vm.heap.get(id).print(f);
		}
		P.p("===vm bitmap access====", f);
		for (BitmapAccess a : vm.bitmapAccess)
		{
			P.p(a.action+"  "+a.sig,f);
			for (Expression p : a.params)
				P.p("\t"+p.toString(),f);
		}
		P.p("====================================");
		vm.writeToFile(f, true);
	}

	
	static class Result {
		int type = -1;
		List<int[]> setPixelOrder = new ArrayList<>();
		List<String> setPixelOrder2 = new ArrayList<>();
		VM vm;
		int numBranches = 0;
	}
	
	public static Result exec(APEXApp app, APEXMethod m)
	{
		int i = 0;
		otherVMs.offer(new VM(app, m));
		VM.pathCount = 0;
		
		Result res = new Result();
		int maxOpCount = 0;
		
		while (!otherVMs.isEmpty() && i < pathLimit)
		{
			// Each VM represents one execution path for method m.
			VM vm = otherVMs.poll();
			vm.allocateConcreteBitmap = false;

			if (ui != null)
				ui.newLine("Running VM No."+i+" for "+m.signature);
			vm.execute(true);
			otherVMs.addAll(vm.otherVMs);
			int opCount = countImageOperations(vm);
			if (opCount > maxOpCount)
			{
				res.vm = vm;
				maxOpCount = opCount;
			}
			if (!vm.crashed)
			{
				i++;
			}
		}
		otherVMs.clear();
		res.numBranches = i;
		return res;
	}
	
	private static int countImageOperations(VM vm)
	{
		int res = 0;
		for (APEXObject obj : vm.heap.values())
		{
			res += obj.bitmapHistory.size();
		}
		return res;
	}


	public static void concreteExec(APEXApp app, APEXMethod m)
	{
		int i = 0;
		otherVMs.offer(new VM(app, m));
		otherVMs.peek().allocateConcreteBitmap = true;
		while (!otherVMs.isEmpty() && i < pathLimit)
		{
			VM vm = otherVMs.poll();
			if (ui != null)
				ui.newLine("Running VM No."+i+" for "+m.signature);
			vm.execute(true);
			if (!vm.crashed)
			{
				if (hasSetPixel(vm))
				{
					vm.print();
					P.p("good VM");
					P.pause();
				}
				//P.p("New VMs: " + vm.otherVMs.size());
				otherVMs.addAll(vm.otherVMs);
			}
			else
			{
				//P.p("crashed.");
			}
			i++;
		}
	}
	
	
	private static boolean hasSetPixel(VM vm)
	{
		for (APEXObject obj : vm.heap.values())
		{
			if (obj.type.equals("Landroid/graphics/Bitmap;"))
			{
				for (BitmapAccess access : obj.bitmapHistory)
				{
					if (access.action.equals("setPixel"))
					{
						return true;
					}
				}
			}
		}
		return false;
	}
	

	static final String apk_root = "C:/workspace/app_analysis/apks";
	static final String stego_root = apk_root+"/stego";
	static final String water_root = apk_root+"/watermarking";
	static final String selfie_root = apk_root+"/beautifying";
	
	static final Set<String> methods_to_skip = new HashSet<String>(){
		private static final long serialVersionUID = 1L;

	{
		add("Lca/repl/free/camopic/ac;->b(Ljava/io/InputStream;)[B");
		add("Lcom/github/gcacace/signaturepad/views/SignaturePad;->onTouchEvent(Landroid/view/MotionEvent;)Z");
		add("Lcom/github/gcacace/signaturepad/views/SignaturePad;->b(Lcom/github/gcacace/signaturepad/a/f;)V");
		add("Lcom/mopub/mobileads/VastVideoBlurLastVideoFrameTask;->doInBackground([Ljava/lang/String;)Ljava/lang/Boolean;");
		add("Lcom/mopub/mobileads/VastVideoBlurLastVideoFrameTask;->doInBackground([Ljava/lang/Object;)Ljava/lang/Object;");
		add("Lcom/olav/logolicious/util/FileUtil;->fileWrite(Ljava/lang/String;Ljava/lang/String;Z)Z");
		add("Lcom/nineoldandroids/animation/AnimatorSet;->playTogether(Ljava/util/Collection;)V");
		add("Lcom/olav/logolicious/util/StringUtil;->splitStr(Ljava/lang/String;Ljava/lang/String;)Ljava/util/ArrayList;");
		add("Lcom/instabug/library/util/BitmapUtils$4;->run()V");
		add("Lcom/paranoiaworks/unicus/android/sse/utils/Encryptor;->importTextFromSteganogram(Ljava/io/File;)Ljava/lang/String;");
		add("Lcom/paranoiaworks/unicus/android/sse/utils/Encryptor;->importTextFromSteganogram(Lcom/paranoiaworks/unicus/android/sse/misc/CryptFileWrapper;)Ljava/lang/String;");
	}
	};

	
	public static void main(String[] args)
	{			
		ui = ProgressUI.create("Symbolic Execution", 20);
		app_progress =ProgressUI.create("App", 20);
		VM.printDebug = false;
		APEXApp.verbose = false;
		
		Set<String> skip = new HashSet<>(Arrays.asList(
				//"jubatus.android.davinci.apk",
				//"com.dinaga.photosecret.apk",
				//"it.mobistego.apk",
				//"com.talixa.pocketstego.apk",
				//"ca.repl.free.camopic.apk",
				"com.fruiz500.passlok.apk",
				//"com.romancinkais.stegais.apk",
				//"info.guardianproject.pixelknot.apk",
				"com.paranoiaworks.unicus.android.sse.apk"
				//"sk.panacom.stegos.apk"
				));
		
		List<File> apks = new ArrayList<>();
		for (File dir : new File(apk_root).listFiles()) 
		if (!dir.getName().contentEquals("instrumented") && !dir.getName().contentEquals("beautifying"))
		for (File f : dir.listFiles())
		if (!skip.contains(f.getName()))
			apks.add(f);
		System.out.println("total number: "+apks.size());

		//apks.clear();
		//apks.add(new File("E:\\crawled_from_github\\android\\StegoBenchmark\\app\\build\\outputs\\apk\\debug\\app-debug.apk"));

		int i = 1;
		File summaryDir = new File("C:/workspace/app_analysis/notes/summaries");
		summaryDir.mkdirs();
		output_summary = new File(summaryDir, "summary_"+System.currentTimeMillis()+".txt");
		for (File apk : apks)
		{
			if (!apk.isFile()) 
				continue;
			
			app_progress.newLine(i++ +"/"+apks.size()+":  "+apk.getAbsolutePath());
			File recordF = new File("C:/workspace/app_analysis/notes/SymbolicExecution/"+apk.getName()+"_brief.csv");
			File notesF = new File("C:/workspace/app_analysis/notes/SymbolicExecution/"+apk.getName()+"_details.csv");
			APEXApp a = new APEXApp(apk, false);
			otherVMs = new LinkedList<>();
			findImageAlterationMethods(a, recordF, notesF, false);
		}
		
		P.p("All Done.");
	}
	

}
