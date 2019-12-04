package app_analysis.temp;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import apex.APEXApp;
import apex.code_wrappers.APEXMethod;
import apex.code_wrappers.APEXStatement;
import apex.symbolic.VM;
import app_analysis.APISignatures;
import app_analysis.Dirs;
import util.P;

public class CheckSteganosaurus {

	static File apk = new File(Dirs.GithubStego, "app.steganosaurus.apk");
	
	static class AppProfile {
		APEXApp app;
		Map<APEXMethod, Boolean> containsBitmapAPIs;
		
		private static Set<String> importantAPIs;
		static{
			importantAPIs = new HashSet<>();
			for (String api : APISignatures.bitmapAPIs)
				importantAPIs.add(api);
			for (String api : APISignatures.LoadBitmapSigs)
				importantAPIs.add(api);
		}
		
		static AppProfile init(APEXApp app)
		{
			AppProfile profile = new AppProfile();
			profile.app = app;
			profile.containsBitmapAPIs = new HashMap<>();
			
			return profile;
		}
		
		public boolean shouldGoIn(APEXMethod m)
		{
			if (m==null)
				return false;
			if (containsBitmapAPIs.containsKey(m))
				return containsBitmapAPIs.get(m);
			if (m.statements == null || m.statements.isEmpty())
			{
				containsBitmapAPIs.put(m, false);
				return false;
			}
			containsBitmapAPIs.put(m, false);
			for (APEXStatement s : m.statements)
			{
				if (s.isInvokeStmt())
				{
					String sig = s.getInvokeSignature();
					if (importantAPIs.contains(sig))
					{
						containsBitmapAPIs.put(m, true);
						return true;
					}
					APEXMethod nestedM = app.getNonLibraryMethod(sig);
					if (nestedM!=null && nestedM!=m && shouldGoIn(nestedM))
					{
						containsBitmapAPIs.put(m, true);
						return true;
					}
				}
			}
			
			return false;
		}
	}
	
	public static void main(String[] args)
	{
		APEXApp app = new APEXApp(apk);
		P.p(app.packageName);
		//AppProfile p = AppProfile.init(app);
		
		APEXMethod m = app.getMethod(
				"Lapp/steganosaurus/Utility/Steganograph;->encodePicture(Landroid/graphics/Bitmap;Ljava/lang/String;)Landroid/graphics/Bitmap;");

		Queue<VM> vms = new LinkedList<>();
		VM vm = new VM(app, m);
		vm.allocateConcreteBitmap = true;
		vms.add(vm);
		while (!vms.isEmpty())
		{
			vm = vms.poll();
			vm.execute(true);
			vm.print();
			vms.addAll(vm.otherVMs);
			vm = null;
			System.gc();
			P.pause();
		}
		
	}
	
	
}
