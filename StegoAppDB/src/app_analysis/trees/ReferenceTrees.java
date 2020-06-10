package app_analysis.trees;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import apex.symbolic.Expression;
import app_analysis.common.Dirs;
import ui.ProgressUI;
import util.F;
import util.P;

public class ReferenceTrees {

	
	public static String singleApp = null;
	
	public static void main(String[] args) {
//		String name = "edu.pens.stegano.blackberry.pembayaran.embed_-2140417732_91.expression";
//		Expression exp = (Expression) F.readObject(new File(
//				"C:\\workspace\\app_analysis\\notes\\Medoid_ExpressionTrees\\edu.pens.stegano.blackberry.apk",
//				name));
//		for (String label : TreeRefUtils.trees.keySet()) {
//			if (contains(exp, TreeRefUtils.trees.get(label))) {
//				P.p("contains "+label);
//			}
//		}
//		aria.photoshop.Photoshop.DecreaseColorDepth_-240438107_10_full
		singleApp = "jubatus";
		go();
	}

	public static void go() {
		List<File> apks = Dirs.getFiles(Dirs.Stego_Github, Dirs.Stego_PlayStore, Dirs.Stego_Others);
		batchMatch(apks);
	}
	

	static ProgressUI ui = ProgressUI.create("tree");
	static void batchMatch(List<File> apks) {
		Set<String> rootVals = new HashSet<>();
		File treeRoot = new File("C:\\workspace\\app_analysis\\notes\\ExpressionTrees");
		File trimmedRoot = new File("C:\\workspace\\app_analysis\\notes\\Medoid_ExpressionTrees");
		File resF = new File(Dirs.Desktop, "res.csv");
		
		PrintWriter out = F.initPrintWriter(resF);
		int total = 0, goodCount = 0, matchCount = 0, appHitCount = 0, appIncon = 0;
		long time = System.currentTimeMillis();
		Map<String, Integer> allResult = new TreeMap<>();
		List<String> todoApps = new ArrayList<>();
		for (File apk : apks) {
			//if (!apk.getName().startsWith("com.artb"))
			//	continue;
			if (singleApp != null && !apk.getName().startsWith(singleApp))
				continue;
			File trimmedTreeDir = new File(trimmedRoot, apk.getName());
			if (singleApp != null)
				for (File f : trimmedTreeDir.listFiles())
					f.delete();
			File appDir = new File(treeRoot, apk.getName());
			trimTreeCount(apk, appDir, trimmedTreeDir);
			File appDir2 = new File("C:\\workspace\\app_analysis\\graphs\\expression", apk.getName());
			trimTreeCount(apk, appDir2, trimmedTreeDir);
			File[] treeFiles = trimmedTreeDir.listFiles((f,n)->n.endsWith(".expression"));
			if (treeFiles == null) {
				P.p(apk.getName()+"\t0\t0");
				continue;
			}
			
			Map<String, Integer> appResult = new HashMap<>();
			int count_match_app = 0;
			total += treeFiles.length;
			File normalizedDir = new File("C:\\Users\\C03223-Stego2\\Desktop\\normalized_trees", apk.getName());
			normalizedDir.mkdirs();
			for (File treeF: treeFiles) {
				// NOTE: skip the super large trees
				String n = treeF.getName();
				n = n.substring(0, n.lastIndexOf("."))+"_full.pdf";
				File pdfF = new File(trimmedTreeDir, n);
				ui.newLine("matching "+treeF.getAbsolutePath());
				if (pdfF.length() > 90000 || treeF.length()>90000) {
					total--;
					//P.p("skipping "+pdfF.length()+" "+treeF.getAbsolutePath());
					continue;
				}
				//if (curr++ % 100 == 0)
				//	P.p("doing "+curr+" to "+(curr+99));
				Object obj = F.readObject(treeF);
				if (obj == null) {
					total--;
					continue;
				}
				Expression exp = (Expression) obj;
				TreeRefUtils.normalize(exp);
				
//				exp.toDotGraph("temp"+new Random().nextInt(1000000), Dirs.Desktop, false);
//				P.pause(treeF.getName());
//				P.p(exp.toString());
//				P.pause();
				
				rootVals.add(exp.root);
				boolean matched = false;
				for (String label : TreeRefUtils.trees.keySet()) {
					if (contains(exp, TreeRefUtils.trees.get(label))) {
						matched = true;
//						if (apk.getName().startsWith("ir.")) {
//							P.p(treeF.getAbsolutePath());
//							P.pause();
//						}
						appResult.put(label, appResult.getOrDefault(label, 0)+1);
						allResult.put(label, allResult.getOrDefault(label, 0)+1);
						exp.toDotGraph(label+"_"+appResult.get(label), normalizedDir, false);
					}
				}
				if (matched)
					count_match_app++;
			}
			if (count_match_app > 0)
				appHitCount++;
			else if (treeFiles.length>0){
				appIncon++;
				todoApps.add(apk.getName());
			}
				
			P.pf("%s %d %d", apk.getName(), treeFiles.length, count_match_app);
			if (!appResult.isEmpty()) {
				for (String key : appResult.keySet())
					P.pf("  [%s %d]", key, appResult.get(key));
			}
			P.p("");
		}
		long secs = (System.currentTimeMillis()-time)/1000;
		double mins = secs / 60.0;
		P.pf("Total trees: %d, good ones: %d, matched: %d. Total time: %.2f minutes\n", 
				total, goodCount, matchCount, mins);
		//for (int i=0; i<=4; i++)
		//	P.pf("structure %d has %d trees\n", i, counts[i]);
		P.p("ref match results:");
		for (String key : allResult.keySet()) {
			P.pf(" %s: %d\n", key, allResult.get(key));
		}
		P.p("apps hit: "+appHitCount);
		P.p("apps inconclusive: "+appIncon);
		out.close();
		P.p("--- todo apps:");
		P.p(todoApps);
	}
	
	static String match(Expression exp) {
		Map<String, Expression> trees = TreeRefUtils.trees;
		for (String label : trees.keySet()) {
			if (equals(exp, trees.get(label))) {
				return label;
			}
		}
		return null;
	}
	
	static boolean temp = false;
	static boolean contains(Expression exp1, Expression exp2) {
//		if (temp) {
//			P.p("exp1: "+exp1.toString());
//			if (exp2 != null)
//				P.p("exp2: "+exp2.toString());
//			P.pause();
//		}
		if (exp1 == null)
			return false;
		if (exp2 == null || exp2.root.contentEquals("X")) {
			return true;
		}

		// see if exp1 and exp2 strictly matches
		if (equals(exp1, exp2)) {
			return true;
		}
		boolean res = false;
		// now exp1 and exp2 doesn't match, recursively compare exp1's children with exp2
		for (Expression child : exp1.children)
			if (contains(child, exp2)) {
				res = true;
			}
		return res;
	}
	
	public static boolean isStego(Expression exp) {
		for (Expression ref : TreeRefUtils.trees.values())
			if (contains(exp, ref))
				return true;
		return false;
	}
	
	static boolean equals(Expression exp, Expression pattern) {
		if (pattern == null || pattern.root.contentEquals("X")) {
			return true;
		}
		if (pattern.root.contentEquals("Y")) {
			boolean res = false;
			String s = exp.root;
			if (s.contentEquals("$GetPixel")) {
				res = true;
			}
			if (s.contentEquals("return")) {
				String sig = exp.children.get(0).root;
				res =  sig.contentEquals("$Alpha") || sig.contentEquals("$Red") ||
						sig.contentEquals("$Green") || sig.contentEquals("$Blue");
			}
			if (res) {
				exp.shouldHighlight = true;
				exp.highlightColor = "orange";
			}
			return res;
		}
		if (exp == null || !exp.root.contentEquals(pattern.root))
			return false;
		List<Expression> children1 = exp.children;
		List<Expression> children2 = pattern.children;
		if (children1.size() != children2.size())
			return false;
		for (int i=0; i<children1.size(); i++) {
			if (!equals(children1.get(i), children2.get(i)))
				return false;
		}
		exp.shouldHighlight = true;
		return true;
	}
	
	static void trimTreeCount(File apk, File treeDir, File trimmedDir) {
		trimmedDir.mkdirs();
		File[] treeFiles = treeDir.listFiles((f,n)->n.endsWith(".expression"));
		if (treeFiles == null)
			return;
		
		//int old = treeFiles.length;
		Set<Long> lengths = new HashSet<>();
		for (File expF : treeFiles) {
			if (lengths.add(expF.length())) {
				String name = expF.getName();
				name = name.substring(0, name.indexOf(".expression"));
				File[] files = new File[] {
						expF, new File(treeDir, name+"_full.dot"), new File(treeDir, name+"_full.pdf")
				};
				for (File f : files)
					F.copyToFolder(f, trimmedDir);
			}
		}
		//P.pf("%d %d\n", old, lengths.size());
	}
	
	static final Set<String> rootVals = new HashSet<>(Arrays.asList("&","+","|","return"));
	// return 0 - argb, 1 - rgb, 2 - A|R|G|B, 3- A+R+G+B, 4, other, -1 - skip?
	static int getStructureType(Expression exp) {
		if (!rootVals.contains(exp.root))
			return -1;
		if (exp.root.contentEquals("return")) {
			String sig = exp.children.get(0).root;
			if (sig.contains("Landroid/graphics/Color;->argb(IIII)I"))
				return 0;
			if (sig.contentEquals("Landroid/graphics/Color;->rgb(III)I"))
				return 1;
			return -1;
		}
		if (exp.root.contentEquals("|") && findConsecutive(exp, "|")>=3)
			return 2;
		if (exp.root.contentEquals("+") && findConsecutive(exp, "+")>=3)
			return 3;
		
		return 4;
	}

	
	static int findConsecutive(Expression exp, String symbol) {
		if (exp == null || !exp.root.contentEquals(symbol))
			return 0;
		
		int max = 0;
		for (Expression child : exp.children)
			max = Math.max(max, findConsecutive(child, symbol));
		return max+1;
	}
	
	
	static void old(File[] files, File appDir, File outDir, PrintWriter out) {
		Expression[] refTrees = TreeRefUtils.refTrees;
		int total = files==null?0:files.length, matched = 0;
		int[] individualMatchCount = new int[refTrees.length];
		P.pf("----- %s %d ----\n", appDir.getName(), total);
		if(files!=null) for(File f : files) {
			Expression exp = (Expression) F.readObject(f.getAbsolutePath());
			//String name = "exp_"+outDir.list().length;
			String name = f.getName().substring(0, f.getName().lastIndexOf("_"));
			boolean match = false;
			boolean[] matchResult = new boolean[refTrees.length];
			for (int i=0; i<refTrees.length; i++) {
				if (contains(exp, refTrees[i])) {
					P.pf("%s match with LSB%d:\n%s\n", name, i, exp.toString());
					matchResult[i] = true;
					match = true;
					individualMatchCount[i]++;
				}
			}
			
			if (match) {
				exp.toDotGraph(name, outDir, false);
				matched++;
			}
		}
		String indivMatchResult = "";
		for (int i : individualMatchCount)
			indivMatchResult += "\t"+i;
		out.printf("%s\t%d\t%d%s\n", appDir.getName(), total, matched, indivMatchResult);
		out.flush();
	}

	
	
	
	
	
}
