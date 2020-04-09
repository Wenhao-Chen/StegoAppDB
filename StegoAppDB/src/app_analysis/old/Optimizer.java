package app_analysis.old;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import apex.APEXApp;
import apex.bytecode_wrappers.APEXMethod;
import apex.bytecode_wrappers.APEXStatement;
import app_analysis.APISignatures;

public class Optimizer {

	public Map<APEXMethod, Boolean> hasImageEditing;
	private APEXApp app;
	
	
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
	
	public Optimizer(APEXApp app)
	{
		this.app = app;
		hasImageEditing = new HashMap<>();
	}
	
	
	
	public boolean shouldGoIn(APEXMethod m)
	{
		if (m==null)
			return false;
		if (hasImageEditing.containsKey(m))
			return hasImageEditing.get(m);
		if (m.statements == null || m.statements.isEmpty())
		{
			hasImageEditing.put(m, false);
			return false;
		}
		hasImageEditing.put(m, false);
		for (APEXStatement s : m.statements)
		{
			if (s.isInvokeStmt())
			{
				String sig = s.getInvokeSignature();
				if (importantAPIs.contains(sig))
				{
					hasImageEditing.put(m, true);
					return true;
				}
				APEXMethod nestedM = app.getNonLibraryMethod(sig);
				if (nestedM!=null && nestedM!=m && shouldGoIn(nestedM))
				{
					hasImageEditing.put(m, true);
					return true;
				}
			}
		}
		
		return false;
	}
}
