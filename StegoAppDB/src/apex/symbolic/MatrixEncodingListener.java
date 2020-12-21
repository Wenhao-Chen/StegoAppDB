package apex.symbolic;

import java.io.File;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import apex.APEXApp;
import apex.bytecode_wrappers.APEXClass;
import apex.bytecode_wrappers.APEXMethod;
import apex.bytecode_wrappers.APEXStatement;
import apex.symbolic.listeners.ExecutionListener;
import app_analysis.common.Dirs;
import ui.ProgressUI;
import util.F;
import util.Mailjet;
import util.P;

public class MatrixEncodingListener implements ExecutionListener{

	static ProgressUI ui;
	
	public static void main(String[] args) {
		
		// settings
		long time = System.currentTimeMillis();
		ui = ProgressUI.create("matrix encoding");
		List<File> apks = Dirs.getAllFiles();
		int count = 1, total = apks.size();
		VM.listener = new MatrixEncodingListener();
		File root = new File(Dirs.Desktop, "matrix_encoding"); root.mkdirs();
		File recordF = new File (root, "processed.txt");
		Set<String> alreadyProcessed = new HashSet<>(F.readLines(recordF));
		boolean redo = false;
		Set<String> apps_to_skip = new HashSet<>(Arrays.asList(
				"com.nitramite.cryptography.apk"));
		
		// time limits: individual path, all path for a method, all methods for an app
		VM.timeLimit_seconds = 40;
		int method_time_limit = 200;
		int app_time_limit = 20*60;
		
		// main processing loop
		for (File apk : apks) {
			ui.newLine(String.format("%03d/%d - %s\n", count++, total, apk.getName()));
			if (apps_to_skip.contains(apk.getName()) || 
					!redo && alreadyProcessed.contains(apk.getName()))
				continue;
			APEXApp app = new APEXApp(apk);
			if (app.malfunction)
				continue;
			File appRecordDir = new File(root, apk.getName()); appRecordDir.mkdirs();
			File appTreeDir = new File(appRecordDir, "trees"); appTreeDir.mkdirs();
			currTreeDir = appTreeDir;
			expIndex = new HashMap<>();
			File appStatsF = new File(appRecordDir, "stats.txt");
			PrintWriter out_stats = F.initPrintWriter(appStatsF);
			int aput_xor_Methods = 0;
			long app_start_time = System.currentTimeMillis();
			
			for (APEXClass c : app.getNonLibraryClasses()) {
				long app_time = (System.currentTimeMillis()-app_start_time)/1000;
				if (app_time > app_time_limit) {
					out_stats.println("app timeout");
					P.p("[app timeout]"+apk.getName());
					break;
				}
				Set<APEXMethod> methods = new HashSet<>(c.methods.values());
				for (APEXMethod m : methods) {
					if (hasAputAndXor(m)) {
						aput_xor_Methods++;
						long method_start_time = System.currentTimeMillis();
						Queue<VM> q = new LinkedList<>();
						q.add(new VM(app, m));
						while (!q.isEmpty()) {
							long method_time = System.currentTimeMillis()-method_start_time;
							method_time /= 1000;
							if (method_time > method_time_limit) {
								out_stats.println("method timeout: "+m.signature);
								break;
							}
							VM vm = q.poll();
							vm.execute(true);
							q.addAll(vm.otherVMs);
						}
						c.methods.remove(m.signature);
					}
				}
			}
			P.p(app.packageName+" "+aput_xor_Methods);
			out_stats.println(app.packageName+" "+aput_xor_Methods);
			out_stats.close();
			F.writeLine(apk.getName(), recordF, true);
		}
		
		time = (System.currentTimeMillis()-time)/1000;
		P.p("Done. time "+time+" seconds.");
		Mailjet.email("Done. time "+time+" seconds.");
	}
	
	static boolean hasAputAndXor(APEXMethod m) {
		boolean aput = false, xor = false;
		for (APEXStatement s : m.statements)
		if (s.smali.startsWith("aput "))
			aput = true;
		else if (s.smali.startsWith("xor-int"))
			xor = true;
		return aput && xor;
	}

	static File currTreeDir;
	static Map<String, Integer> expIndex;
	@Override
	public void preStatementExecution(VM vm, MethodContext mc, APEXStatement s) {
		if (s.smali.startsWith("aput")) {
			Expression exp = mc.read(s.getArguments()[0]);
			String sig = s.getSignatureAsFileName();
			int index = expIndex.getOrDefault(sig, 0)+1;
			expIndex.put(sig, index);
			sig = sig+"_"+index;
			F.writeObject(exp, new File(currTreeDir, sig+".expression"));
			exp.toDotGraph(sig, currTreeDir, false);
			exp.toDotGraph(sig, currTreeDir, true);
		}
	}

	@Override
	public void postStatementExecution(VM vm, MethodContext mc, APEXStatement s) {
		
	}

}
