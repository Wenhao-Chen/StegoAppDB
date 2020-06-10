package app_analysis.old;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import apex.APEXApp;
import apex.bytecode_wrappers.APEXClass;
import apex.bytecode_wrappers.APEXMethod;
import apex.bytecode_wrappers.APEXStatement;
import apex.graphs.CallGraph;
import apex.graphs.CallGraph.Vertex;
import ui.ProgressUI;
import util.F;
import util.P;

public class FindingEntryPoints {
	
	static ProgressUI ui;

	
	private static Set<String> importantAPIs;
	private static Set<String> readBitmapAPIs, writeBitmapAPIs;
	static{
		importantAPIs = new HashSet<>();
		for (String api : APISignatures.bitmapAPIs)
			importantAPIs.add(api);
		for (String api : APISignatures.LoadBitmapSigs)
			importantAPIs.add(api);
		//for (String api : APISignatures.canvasAPIs)
		//	importantAPIs.add(api);
		readBitmapAPIs = new HashSet<>(Arrays.asList(
				"Landroid/graphics/Bitmap;->getPixel(II)I",
				"Landroid/graphics/Bitmap;->getPixels([IIIIIII)V",
				"Landroid/graphics/Bitmap;->copyPixelsToBuffer(Ljava/nio/Buffer;)V"
				));
		writeBitmapAPIs = new HashSet<>(Arrays.asList(
				"Landroid/graphics/Bitmap;->setPixel(III)V",
				"Landroid/graphics/Bitmap;->setPixels([IIIIIII)V",
				"Landroid/graphics/Bitmap;->copyPixelsFromBuffer(Ljava/nio/Buffer;)V"
				));
	}
	

	public static File epDir = new File(Template.notesDir, "EntryPoints");
	public static Set<APEXMethod> findOrLoad(APEXApp app)
	{
		epDir.mkdirs();
		File epFile = new File(epDir, app.apk.getName()+".ep");
		if (epFile.exists())
			return app.getMethods(F.readLinesWithoutEmptyLines(epFile));
		return find(app, epFile);
	}
	
	public static Set<APEXMethod> findOrLoad2(APEXApp app)
	{
		epDir.mkdirs();
		File epFile = new File(epDir, app.apk.getName()+".ep");
		if (epFile.exists())
			return app.getMethods(F.readLinesWithoutEmptyLines(epFile));
		return find2(app, epFile);
	}
	
	public static Set<APEXMethod> findOrLoad3(APEXApp app)
	{
		epDir.mkdirs();
		File epFile = new File(epDir, app.apk.getName()+".ep");
		if (epFile.exists())
			return app.getMethods(F.readLinesWithoutEmptyLines(epFile));
		return find3(app, epFile);
	}
	
	public static Set<APEXMethod> findOrLoad4(APEXApp app)
	{
		epDir.mkdirs();
		File epFile = new File(epDir, app.apk.getName()+".ep");
		if (epFile.exists())
			return app.getMethods(F.readLinesWithoutEmptyLines(epFile));
		return find4(app, epFile);
	}
	
	public static Set<APEXMethod> find4(APEXApp app, File epFile) {
		Set<APEXMethod> entries = new HashSet<>();
		if (app.packageName == null)
			return entries;
		CallGraph cg = new CallGraph(app);
		cg.updateMethodCallGraph();
		Set<APEXMethod> readers = new HashSet<>();
		Set<APEXMethod> writers = new HashSet<>();
		for (APEXClass c : app.getNonLibraryClasses())
		for (APEXMethod m : c.methods.values()) {
			if (matchInvoke(m, readBitmapAPIs))
				readers.add(m);
			if (matchInvoke(m, writeBitmapAPIs))
				writers.add(m);
		}
		P.pf("   find entry point 4 (%d %d)\n", readers.size(), writers.size());
		entries.addAll(cg.pathExists(readers, writers));
		
		if (epFile != null)
		{
			PrintWriter out = F.initPrintWriter(epFile);
			entries.forEach(e->{
				out.println(e.signature);
			});
			out.close();
		}
		return entries;
	}
	
	private static boolean matchInvoke(APEXMethod m, Set<String> sigs) {
		for (APEXStatement s : m.statements)
			if (s.isInvokeStmt() && sigs.contains(s.getInvokeSignature())) {
				return true;
			}
		return false;
	}
	
	public static List<String> findOrLoad(File f)
	{
		File epFile = new File(epDir, f.getName()+".ep");
		if (epFile.exists())
			return F.readLinesWithoutEmptyLines(epFile);
		List<String> res = new ArrayList<>();
		find(new APEXApp(f), epFile).stream().forEach(k->res.add(k.signature));
		return res;
	}
	
	// this is for malwares
	// only find methods that has getPixel, getPixels, copyPixelsToBuffer
	public static Set<APEXMethod> find3(APEXApp app, File epFile) {
		Set<APEXMethod> entries = new HashSet<>();
		if (app.packageName == null)
			return entries;
		CallGraph cg = new CallGraph(app);
		cg.updateMethodCallGraph();
		Map<APEXMethod, Boolean> shouldGo = new HashMap<>();
		for (APEXClass c : app.getNonLibraryClasses())
		{
			//P.p(" checking "+c.dexName);
			for (APEXMethod m : c.methods.values())
				if (shouldGoIn3(app, shouldGo, m))
				{
					Vertex v = cg.vertices.get(m.signature);
					if (v!=null && v.in_degree == 0)
						entries.add(m);
				}
		}
		
		if (epFile != null)
		{
			PrintWriter out = F.initPrintWriter(epFile);
			entries.forEach(e->{
				out.println(e.signature);
			});
			out.close();
		}
		return entries;
	}
	
	// find methods that contain image operations
	public static Set<APEXMethod> find2(APEXApp app, File epFile) {
		Set<APEXMethod> entries = new HashSet<>();
		if (app.packageName == null)
			return entries;
		//P.p(" generating CG for "+app.packageName);
		CallGraph cg = new CallGraph(app);
		cg.updateMethodCallGraph();
		Map<APEXMethod, Boolean> shouldGo = new HashMap<>();
		for (APEXClass c : app.getNonLibraryClasses())
		{
			//P.p(" checking "+c.dexName);
			for (APEXMethod m : c.methods.values())
				if (shouldGoIn2(app, shouldGo, m))
				{
					Vertex v = cg.vertices.get(m.signature);
					if (v!=null)
						entries.add(m);
				}
		}
		
		if (epFile != null)
		{
			PrintWriter out = F.initPrintWriter(epFile);
			entries.forEach(e->{
				out.println(e.signature);
			});
			out.close();
		}
		return entries;
	}
	
	// find the entry point methods of methods that contain image operations
	public static Set<APEXMethod> find(APEXApp app, File epFile)
	{
		Set<APEXMethod> entries = new HashSet<>();
		if (app.packageName == null)
			return entries;
		//P.p(" generating CG for "+app.packageName);
		CallGraph cg = new CallGraph(app);
		cg.updateMethodCallGraph();
		Map<APEXMethod, Boolean> shouldGo = new HashMap<>();
		for (APEXClass c : app.getNonLibraryClasses())
		{
			//P.p(" checking "+c.dexName);
			for (APEXMethod m : c.methods.values())
				if (shouldGoIn(app, shouldGo, m))
				{
					Vertex v = cg.vertices.get(m.signature);
					if (v!=null && v.in_degree==0) // only take entry point methods
					//if (v!=null)
						entries.add(m);
				}
		}
		
		if (epFile != null)
		{
			PrintWriter out = F.initPrintWriter(epFile);
			entries.forEach(e->{
				out.println(e.signature);
			});
			out.close();
		}
		return entries;
	}
	
	private static boolean shouldGoIn(APEXApp app, Map<APEXMethod, Boolean> map, APEXMethod m)
	{
		if (m==null)
			return false;
		if (map.containsKey(m))
			return map.get(m);
		map.put(m, false); // do this to avoid infinite loops caused by call graph circles
		boolean res = false;
		for (APEXStatement s : m.statements)
		if (s.isInvokeStmt())
		{
			String sig = s.getInvokeSignature();
			if (importantAPIs.contains(sig))
			{
				res = true;
				break;
			}
			//System.out.printf("testing %s\n", sig);
			
			APEXMethod nestedM = app.getNonLibraryMethod(sig);
			if (nestedM!=null && nestedM!=m && shouldGoIn(app, map, nestedM))
			{
				res = true;
				break;
			}
			
		}
		map.put(m, res);
		return res;
	}
	
	private static boolean shouldGoIn2(APEXApp app, Map<APEXMethod, Boolean> map, APEXMethod m)
	{
		if (m==null)
			return false;
		if (map.containsKey(m))
			return map.get(m);
		map.put(m, false); // do this to avoid infinite loops caused by call graph circles
		boolean res = false;
		for (APEXStatement s : m.statements)
		if (s.isInvokeStmt())
		{
			String sig = s.getInvokeSignature();
			if (importantAPIs.contains(sig))
			{
				res = true;
				break;
			}
		}
		map.put(m, res);
		return res;
	}
	
	private static boolean shouldGoIn3(APEXApp app, Map<APEXMethod, Boolean> map, APEXMethod m)
	{
		if (m==null)
			return false;
		if (map.containsKey(m))
			return map.get(m);
		map.put(m, false); // do this to avoid infinite loops caused by call graph circles
		boolean res = false;
		for (APEXStatement s : m.statements)
		if (s.isInvokeStmt())
		{
			String sig = s.getInvokeSignature();
			if (writeBitmapAPIs.contains(sig))
			{
				res = true;
				break;
			}
			
			APEXMethod nestedM = app.getNonLibraryMethod(sig);
			if (nestedM!=null && nestedM!=m && shouldGoIn3(app, map, nestedM))
			{
				res = true;
				break;
			}
		}
		map.put(m, res);
		return res;
	}
	


}
