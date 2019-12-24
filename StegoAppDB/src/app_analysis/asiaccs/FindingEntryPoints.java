package app_analysis.asiaccs;

import java.io.File;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import apex.APEXApp;
import apex.bytecode_wrappers.APEXClass;
import apex.bytecode_wrappers.APEXMethod;
import apex.bytecode_wrappers.APEXStatement;
import apex.graphs.CallGraph;
import apex.graphs.CallGraph.Vertex;
import app_analysis.APISignatures;
import ui.ProgressUI;
import util.F;
import util.P;

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
	

	static final File epDir = new File(Template.notesDir, "EntryPoints");
	public static Set<APEXMethod> findOrLoad(APEXApp app)
	{
		epDir.mkdirs();
		File epFile = new File(epDir, app.apk.getName()+".ep");
		if (epFile.exists())
			return app.getMethods(F.readLinesWithoutEmptyLines(epFile));
		return find(app, epFile);
	}
	
	public static Set<APEXMethod> find(APEXApp app, File epFile)
	{
		CallGraph cg = new CallGraph(app);
		cg.updateMethodCallGraph();
		Set<APEXMethod> entries = new HashSet<>();
		Map<APEXMethod, Boolean> shouldGo = new HashMap<>();
		for (APEXClass c : app.getNonLibraryClasses())
		for (APEXMethod m : c.methods.values())
		if (shouldGoIn(app, shouldGo, m))
		{
			Vertex v = cg.vertices.get(m.signature);
			if (v!=null && v.in_degree==0)
				entries.add(m);
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
