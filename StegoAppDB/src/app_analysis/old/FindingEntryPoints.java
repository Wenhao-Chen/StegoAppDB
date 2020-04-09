package app_analysis.old;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
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
import app_analysis.APISignatures;
import ui.ProgressUI;
import util.F;

public class FindingEntryPoints {
	
	static ProgressUI ui;

	
	private static Set<String> importantAPIs;
	static{
		importantAPIs = new HashSet<>();
		for (String api : APISignatures.bitmapAPIs)
			importantAPIs.add(api);
		for (String api : APISignatures.LoadBitmapSigs)
			importantAPIs.add(api);
		//for (String api : APISignatures.canvasAPIs)
		//	importantAPIs.add(api);
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
	
	public static List<String> findOrLoad(File f)
	{
		File epFile = new File(epDir, f.getName()+".ep");
		if (epFile.exists())
			return F.readLinesWithoutEmptyLines(epFile);
		List<String> res = new ArrayList<>();
		find(new APEXApp(f), epFile).stream().forEach(k->res.add(k.signature));
		return res;
	}
	
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
					if (v!=null && v.in_degree==0)
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
	


}
