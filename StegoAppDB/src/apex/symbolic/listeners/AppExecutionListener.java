package apex.symbolic.listeners;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import apex.APEXApp;
import apex.bytecode_wrappers.APEXClass;
import apex.bytecode_wrappers.APEXMethod;
import apex.bytecode_wrappers.APEXStatement;
import apex.symbolic.Expression;
import apex.symbolic.MethodContext;
import apex.symbolic.VM;
import app_analysis.common.Dirs;
import ui.ProgressUI;
import util.F;
import util.Mailjet;
import util.P;


public class AppExecutionListener implements ExecutionListener{
	
	
	static void temp() {
		Map<String, Set<String>> map = new LinkedHashMap<>();
		String currApp = null;
		List<String> list = F.readLinesWithoutEmptyLines(new File(Dirs.Desktop, "output.txt"));
		String[] labels = {"Y", "Cb", "Cr", "JPEG header", "JPEG EOI", "DCT_jpeg6a"};
		for (String l : list) {
			if (l.startsWith("--")) {
				if (currApp != null) {
					System.out.print(currApp);
					for (String label : labels)
						System.out.printf("\t%s", map.get(currApp).contains(label)?"Yes":"No");
					System.out.print("\n");
				}
				currApp = l.substring(l.indexOf("app ")+4);
			} else {
				String label = l.substring(0, l.indexOf(":")).trim();
				map.computeIfAbsent(currApp, k->new HashSet<>()).add(label);
			}
		}
		
	}
	
	static boolean anyTrue(boolean[] arr) {
		for (boolean b : arr)
		if (b)
			return true;
		return false;
	}
	
	
	static String name, methodSig;
	static int counter;
	static File currTreeDir;
	static ProgressUI ui;
	public static void main(String[] args) {
		
		String[] labels = {"Y", "Cb", "Cr", "JPEG header", "JPEG EOI", "DCT_jpeg6a"};
		VM.listener = new AppExecutionListener();
		
		String[] names = new String[] {
				"kcich.stegoqr.apk",
				"ca.repl.free.camopic.apk",
				"com.paranoiaworks.unicus.android.sse.apk",
				"info.guardianproject.pixelknot.apk",
		};
		String[] sigs = new String[] {
				"?",
				"\"Lca/repl/camopic/a/d;->a(ILjava/io/InputStream;Lca/repl/camopic/a/e;)V\"",
				"Lf5/james/JpegEncoder;->WriteCompressedData(Ljava/io/BufferedOutputStream;)V",
				"Linfo/guardianproject/f5android/plugins/f5/james/JpegEncoder;->WriteCompressedData(Ljava/io/BufferedOutputStream;)V"
		};
		List<File> apks = Dirs.getAllFiles();
		
		ui = ProgressUI.create("");
		long time = System.currentTimeMillis();
		
		name = names[3];
		methodSig = sigs[3];
		
		File root = new File(Dirs.Desktop, "matrix_encoding");
		File appDir= new File(root, name);
		currTreeDir = new File(appDir, "trees");
		
		int total = apks.size(), curr = 1;;
		for (int i=0; i<apks.size(); i++) {
			File apk = apks.get(i);
			if (name==null || apk.getName().equals(name)) {
				ui.newLine(String.format("%03d/%d: %s\n",curr++, total, apk.getName()));
				APEXApp app = new APEXApp(apk);
				/*boolean[] res = findDCT(app);
				if (anyTrue(res)) {
					System.out.print(app.packageName);
					for (int j=0; j<labels.length; j++)
						System.out.printf("\t%s", res[j]?"Yes":"No");
					System.out.print('\n');
				}*/
				
				APEXMethod m = app.getMethod(methodSig);
				Queue<VM> q = new LinkedList<>();
				q.add(new VM(app, m));
				int run = 0;
				while (!q.isEmpty()) {
					VM vm = q.poll();
					P.pf("- run %d, queue length %d\n", ++run, q.size());
					vm.execute(true);
					q.addAll(vm.otherVMs);
				}
				
			}
		}
		
		time = (System.currentTimeMillis() - time)/1000;
		P.p("All done.");
		Mailjet.email("Done. "+time+" seconds.");
	}
	
	static boolean[] findDCT(APEXApp app) {
		if (app.malfunction)
			return new boolean[0];
		boolean printApp = true;
		boolean jpegHeader = false;
		boolean jpegEOI = false;
		boolean y=false, cb=false, cr=false, dct_6a=false;
		for (APEXClass c : app.getNonLibraryClasses())
		for (APEXMethod m : c.methods.values())
		if (name==null || methodSig==null || m.signature.equals(methodSig)) {
			boolean[] Y  = new boolean[3]; double[] Yco = {0.299, 0.587, 0.114};
			boolean[] Cb = new boolean[3]; double[] Cbco = {-0.16874, 0.33126, 0.5};
			boolean[] Cr = new boolean[3]; double[] Crco = {0.5, 0.41869, 0.08131};
			boolean[] DCT_jpeg6a = new boolean[4]; double[] jpeg6a = {0.707106781, 0.382683433, 0.5411961, 1.306562965};
			// 0.299 * (float)r + 0.587 * (float)g + 0.114
			//-0.16874 * (float)r - 0.33126 * (float)g + 0.5 * (float)b
			// 0.5 * (float)r - 0.41869 * (float)g - 0.08131 * (float)b
			for (APEXStatement s : m.statements) {
				String[] args = s.getArguments();
				if (s.smali.startsWith("fill-array-data")) {
					ArrayList<String> data = m.getSupplementalData(args[1]);
					jpegHeader = jpegHeader || hasFFandD8(data);
					jpegEOI = jpegEOI || hasFFandD9(data);
				}
				for (int i=0; i<3; i++) {
					if (s.smali.contains("# "+Yco[i]))
						Y[i] = true;
					if (s.smali.contains("# "+Cbco[i]))
						Cb[i] = true;
					if (s.smali.contains("# "+Crco[i]))
						Cr[i] = true;
				}
				for (int i=0; i<jpeg6a.length; i++)
					if (s.smali.contains("# "+jpeg6a[i]))
						DCT_jpeg6a[i] = true;
			}
			y = y || Y[0] && Y[1] && Y[2];
			cb = cb || Cb[0] && Cb[1] && Cb[2];
			cr = cr || Cr[0] && Cr[1] && Cr[2];
			dct_6a = dct_6a || DCT_jpeg6a[0] && DCT_jpeg6a[1] && DCT_jpeg6a[2] && DCT_jpeg6a[3];
		}
		return new boolean[] {y, cb, cr, jpegHeader, jpegEOI, dct_6a};
	}
	
	static boolean hasFFandD8(List<String> list) {
		List<String> ele = new ArrayList<>();
		for (String s : list) {
			if (s.charAt(4)=='.'||s.charAt(4)==':')
				continue;
			ele.add(s.trim());
		}
		return ele.size()==2 && ele.get(0).equals("-0x1t") && ele.get(1).equals("-0x28t");
	}
	
	static boolean hasFFandD9(List<String> list) {
		List<String> ele = new ArrayList<>();
		for (String s : list) {
			if (s.charAt(4)=='.'||s.charAt(4)==':')
				continue;
			ele.add(s.trim());
		}
		return ele.size()==2 && ele.get(0).equals("-0x1t") && ele.get(1).equals("-0x27t");
	}
	
	@Override
	public void preStatementExecution(VM vm, MethodContext mc, APEXStatement s) {
		 if (s.smali.startsWith("if")) {
			//P.p("  [if] "+ s.index+" "+s.smali);
			if (s.index==43 && name.startsWith("ca.repl")) {
				mc.assign("v2", Expression.newLiteral("I", "0x2"));
			}
			if (name.startsWith("com.para")) {
				switch (s.index) {
					case 65:
					case 245:
					case 281:
						mc.assign("v4", Expression.newLiteral("I", "0x3"));
						break;
					case 254:
					case 263:
					case 303:
					case 312:
						mc.assign("v4", Expression.newLiteral("I", "0x1"));
						break;
					case 71:
					case 77:
					case 237:
						mc.assign("v12", Expression.newLiteral("I", "0x8"));
						break;
					default:
						//printIfArgs(mc, s.getArguments());
				}
			}
		}
	}
	
	static void printIfArgs(MethodContext mc, String[] args) {
		for (int i=0; i<2; i++) {
			P.pf("  %s = %s\n", args[i], mc.read(args[i]).toStringRaw());
		}
		P.pause();
	}
	
	@Override
	public void postStatementExecution(VM vm, MethodContext mc, APEXStatement s) {
		if (s.smali.startsWith("xor-int")) {
			//P.p("  [hit] "+s.index+" "+s.smali);
			//P.pause("        "+mc.read(s.getArguments()[0]).toStringRaw());
			Expression exp = mc.read(s.getArguments()[0]);
			String sig = s.getSignatureAsFileName()+"_"+counter++;
			F.writeObject(exp, new File(currTreeDir, sig+".expression"));
			exp.toDotGraph(sig, currTreeDir, false);
			exp.toDotGraph(sig, currTreeDir, true);
		}
	}

}
