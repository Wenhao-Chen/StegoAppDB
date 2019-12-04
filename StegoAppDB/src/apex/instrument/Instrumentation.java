package apex.instrument;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import apex.APEXApp;
import apex.code_wrappers.APEXClass;
import apex.code_wrappers.APEXMethod;
import apex.code_wrappers.APEXStatement;
import ui.ProgressUI;
import util.Apktool;
import util.F;
import util.Jarsigner;
import util.P;

public class Instrumentation {
	
	static private ProgressUI ui;
	
	
	public static void main(String[] args)
	{
		File apkDir = new File("C:/workspace/app_analysis/apks");
		File instrumentedDir = new File("C:/workspace/app_analysis/apks/instrumented");
		instrumentedDir.mkdirs();
		File stegoDir = new File(apkDir, "stego");
		for (File input : stegoDir.listFiles()) if (input.getName().equals("ca.repl.free.camopic.apk"))
		{
			File output = new File(instrumentedDir, input.getName().replace(".apk", "_instrumented.apk"));
			//if (!output.exists())
				instrument(new APEXApp(input, true), output);
		}
	}
	
	
	public static void instrument(APEXApp app, File outputAPK)
	{
		instrument(app, outputAPK, true);
	}
	
	public static void instrument(APEXApp app, File outputAPK, boolean skipLibraryClasses)
	{
		if (ui == null)
			ui = ProgressUI.create("instrumentation", 20);
		
		int loggingMethodCount = 0;
		
		// 1. insert statements at method beginning and ending, and keep count of logging methods
		Collection<APEXClass> classes = skipLibraryClasses?app.getNonLibraryClasses():app.classes.values();
		for (APEXClass c : classes)
		{
			for (APEXMethod m : c.methods.values())
			{
				int count = c.methodNameIndex.get(m.signature);
				loggingMethodCount = Math.max(loggingMethodCount, count);
				
				if (m.statements.size()<1)
					continue;
				
				for (APEXStatement s : m.statements)
				{
					if (s.index==0)
						s.instrumentBefore(Logger.getMethodStartStatement(count));
					if (s.isReturnStmt())
						s.instrumentBefore(Logger.getMethodEndStatement(count));
				}
			}
			F.write(c.getInstrumentedBody(), c.smaliF, false);
			ui.newLine("  finished writing "+c.smaliF.getAbsolutePath()+"..");
			//P.pause();
		}
		
		// 2. add Logger class and create logging methods
		File loggerClassF = new File(app.smaliDir.getAbsolutePath()+"/"+Logger.path);
		
		Logger.writeSmaliClass(loggerClassF, loggingMethodCount);
		ui.newLine("  finished writing "+loggerClassF.getAbsolutePath());
		
		// 3. build and sign
		outputAPK.getParentFile().mkdirs();
		File unsigned = new File(outputAPK.getParentFile(), outputAPK.getName().replace(".apk", "_unsigned.apk"));
		outputAPK.delete();
		unsigned.delete();
		
		Apktool.build(app.outDir, unsigned);
		Jarsigner.signAPK(unsigned, outputAPK);
		ui.newLine("finished instrumenting "+app.packageName);
	}
}
