package app_analysis.temp;

import java.io.File;
import java.util.LinkedList;
import java.util.PriorityQueue;

import apex.APEXApp;
import app_analysis.ExecutionEngine;
import ui.ProgressUI;
import util.Apktool;
import util.P;

public class AnalyzingPlayStoreApps {
	
	static final File root = new File("C:/workspace/app_analysis");
	static final File apkDir = new File(root, "apks");
	static final File decodedDir = new File(root, "decoded");
	static final File notesDir = new File(root, "notes");
	static final File symbolicNotesDir = new File(notesDir, "SymbolicNotes");

	public static void main(String[] args)
	{
		symbolicNotesDir.mkdirs();
		temp();
	}


	private static void temp()
	{
		int i = 0, total = apkDir.list().length;
		long total_time = System.currentTimeMillis();
		ProgressUI ui = ProgressUI.create("analyzing", 30);
		P.p("App No.,App Name,Class Count,Load Time");
		
		PriorityQueue<File> queue = new PriorityQueue<File>(total, (f1, f2)->{return (int) (f1.length()-f2.length());});
		for (File apk : apkDir.listFiles())
			queue.offer(apk);
		
		while (!queue.isEmpty())
		{
			File apk = queue.poll();
			ui.newLine("doing "+(++i)+"/"+total+"   "+apk.getName());
			File outDir = new File(decodedDir, apk.getName());
			APEXApp app = new APEXApp(apk, outDir, false);
			File recordF = new File(symbolicNotesDir, apk.getName()+"_brief.csv");
			File notesF = new File(symbolicNotesDir, apk.getName()+"_details.csv");
			ExecutionEngine.otherVMs = new LinkedList<>();
			ExecutionEngine.findImageAlterationMethods(app, recordF, notesF, false);
		}
		
		total_time = (System.currentTimeMillis()-total_time)/60000;
		P.p("All Done. Total time: " + total_time/1000+" minutes.");
	}
	
	private static void decodeApps()
	{
		ProgressUI ui = ProgressUI.create("decoding", 30);
		int i = 0, total = apkDir.list().length;
		for (File apk : apkDir.listFiles())
		{
			ui.newLine("doing "+(++i)+"/"+total+"   "+apk.getName());
			File out = new File(decodedDir, apk.getName());
			out.mkdirs();
			Apktool.decode(apk, out);
		}
	}
}
