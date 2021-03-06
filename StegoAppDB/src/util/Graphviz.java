package util;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import apex.APEXApp;
import apex.bytecode_wrappers.APEXMethod;
import apex.graphs.CallGraph;
import apex.graphs.ControlFlowGraph;
import app_analysis.common.Dirs;
import ui.ProgressUI;


public class Graphviz {

	public static final String toolPath = "C:/libs/graphviz-2.38/bin/dot.exe";
	public static final File defaultOutDir = new File("C:/workspace/app_analysis/graphs/cfg");
	public static final File defaultExpressionGraphDir = new File("C:/workspace/app_analysis/graphs/expression");
	public static String[] defaultKeywords = {"getPixel", "setPixel"};
	
	public static ProgressUI ui = ProgressUI.create("Graphviz", 20);
	private static ExecutorService executor = Executors.newFixedThreadPool(4);
	public static boolean skipComplexOnes = true;
	
	
	
	public static void main(String[] args)
	{
		File root = new File("C:\\workspace\\app_analysis\\apks\\github_stego");
		
		String apkName = "com.example.ajoy3.steganography.apk";
		String className = "Lcom/example/ajoy3/steganography/Decrypt;";
		String methodSubSig = "Decrypt(Landroid/view/View;)V";
		
		makeCFG(new APEXApp(new File(root, apkName)), className+"->"+methodSubSig, false);
	}
	
	public static String toDotGraphString(int index, String label)
	{
		return toDotGraphString(index, label, null, null, null, (String[])null);
	}
	public static String toDotGraphString(int index, String label, String color, String fontColor)
	{
		return toDotGraphString(index, label, color, fontColor, null, (String[])null);
	}
	
	public static String toDotGraphString(int index, String label, String color, String fontColor, String shape, String... keywords)
	{
		
		String result = index + " [label=\"" + label + "\"";
		
		if (keywords != null)
		{
			for (String keyword : keywords)
			{
				if (label.contains(keyword))
				{
					color = "red";
					fontColor = "red";
					break;
				}
			}
		}
		
		if (color != null)
			result += " color="+color;
		if (fontColor != null)
			result += " fontcolor=" + fontColor;
		if (shape != null)
			result += " shape="+shape;
		result += " ];";
		return result;
	}
	
	public static CallGraph makeCG(APEXApp app, List<APEXMethod> whiteList)
	{
		return makeCG(app, whiteList, false);
	}
	
	public static CallGraph makeCG(APEXApp app, List<APEXMethod> whiteList, boolean redo)
	{
		if (app == null)
			return null;

		Set<String> sigs = new HashSet<>();
		for (APEXMethod m : whiteList)
			sigs.add(m.signature);
		
		CallGraph cg = new CallGraph(app);
		cg.updateMethodCallGraph(sigs);
		
		File dir = new File(defaultOutDir, app.apk.getName());
		
		makeDotGraph(cg.getDotGraph(), "CallGraph", dir, redo);
		return cg;
	}
	
	public static File makeCFG(APEXApp app, String methodSig)
	{
		return makeCFG(app, methodSig, false);
	}
	
	static Set<String> methodsThatTakeTooLong;
	static{
		methodsThatTakeTooLong = new HashSet<>();
		methodsThatTakeTooLong.add("Laos;->a(Landroid/content/Context;Ljava/lang/String;)Laql;");
		methodsThatTakeTooLong.add("Ljp/co/cyberagent/android/gpuimage/grafika/filter/export/GPUImageBeautyFilterFactory;->a(Landroid/content/Context;Ljp/co/cyberagent/android/gpuimage/grafika/filter/export/GPUImageBeautyFilterFactory$BEAUTYCAM_FILTER_TYPE;)Laxu;");
	}
	
	public static File makeCFG(APEXApp app, String methodSig, boolean redo)
	{
		APEXMethod m = app.getMethod(methodSig);
		if (m == null || methodsThatTakeTooLong.contains(methodSig))
			return null;
		ControlFlowGraph cfg = new ControlFlowGraph(app, m);
		String c = Dalvik.DexToJavaName(methodSig.substring(0, methodSig.indexOf("->")));
		String mm = methodSig.substring(methodSig.indexOf("->")+2, methodSig.indexOf("(")).replace("<", "").replace(">", "");
		String params = methodSig.substring(methodSig.indexOf("(")+1, methodSig.indexOf(")"));
		
		File dir = new File(defaultOutDir, app.apk.getName());
		
		File f = makeDotGraph(cfg.getDotGraphString(defaultKeywords), c+"_"+mm+"_"+params.hashCode(), dir, redo);
		//P.p("CFG done: "+f.getAbsolutePath());
		return f;
	}
	
	public static File makeDotGraph(String dotGraphString, String name, File dir)
	{
		return makeDotGraph(dotGraphString, name, dir, false);
	}
	
	public static File makeDotGraph(String dotGraphString, String name, File dir, boolean redo)
	{
		if (ui != null)
			ui.newLine("making dotgraph "+name+" in folder "+dir.getAbsolutePath());
		
		dir.mkdirs();
		File dotFile = new File(dir, name+".dot");
		File pdfFile = new File(dir, name+".pdf");
		
		if (!redo && dotFile.exists() && dotFile.length()>0 && pdfFile.exists() && pdfFile.length()>0)
			return pdfFile;
		
		String dotFilePath = dotFile.getAbsolutePath();
		if (dotFilePath.contains(" "))
			dotFilePath = "\""+dotFilePath+"\"";
		String pdfFilePath = pdfFile.getAbsolutePath();
		if (pdfFilePath.contains(" "))
			pdfFilePath = "\""+pdfFilePath+"\"";
		
		if (skipComplexOnes) {
			Future<?> f = executor.submit(
					new DotGraphCallable(dotFile, dotFilePath, pdfFilePath, dotGraphString));
			try {
				f.get(10, TimeUnit.SECONDS);
			}
			catch (InterruptedException | ExecutionException | TimeoutException e) {
				return null;
			}
		} else {
			try {
				PrintWriter out = new PrintWriter(new FileWriter(dotFile));
				out.write(dotGraphString);
				out.flush();
				out.close();
				int id = dotFile.getName().hashCode();
				//P.pf("dotgraph %d: [*.dot length %d]\n", id, dotFile.length());
				//dot -Tpdf graph1.dot -o graph1.pdf
				P.exec(toolPath + " -Tpdf " + dotFilePath + " -o " + pdfFilePath, true, new File(Dirs.Desktop, "graphviz_log.txt"));
				//P.pf("dotgraph %d done\n", id);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		if (ui != null)
			ui.appendToLastLine(" Done.");
		return pdfFile;
	}
	
	static class DotGraphCallable implements Runnable {
		File dotFile;
		String dotFilePath, pdfFilePath, dotGraphString;
		DotGraphCallable(File dotFile, String dotFilePath, String pdfFilePath, String dotGraphString) {
			this.dotFile = dotFile;
			this.dotFilePath = dotFilePath;
			this.pdfFilePath = pdfFilePath;
			this.dotGraphString = dotGraphString;
		}
		@Override
		public void run()
		{
			try
			{
				PrintWriter out = new PrintWriter(new FileWriter(dotFile));
				out.write(dotGraphString);
				out.flush();
				out.close();
				if (dotFile.length()>200000) { // 200 KB
					return;
				}
				int id = dotFile.getName().hashCode();
				//P.pf("dotgraph %d: [*.dot length %d]\n", id, dotFile.length());
				//dot -Tpdf graph1.dot -o graph1.pdf
				P.exec(toolPath + " -Tpdf " + dotFilePath + " -o " + pdfFilePath, true);
				//P.pf("dotgraph %d done\n", id);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		
	}
	
}
