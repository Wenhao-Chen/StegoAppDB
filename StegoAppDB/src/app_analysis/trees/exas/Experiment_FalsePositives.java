package app_analysis.trees.exas;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;

import com.clust4j.algo.KMedoidsParameters;

import apex.symbolic.Expression;
import app_analysis.common.Dirs;
import app_analysis.trees.TreeRefUtils;
import app_analysis.trees.exas.TreeFeature.Options;
import util.F;
import util.Graphviz;
import util.P;

public class Experiment_FalsePositives {
	
	static List<FPTree> list;
	static List<FPTree> counterRef;
	static double[][] scores;
	static File dir = new File(Dirs.Desktop, "FPTreeMedoids");
	static Options opt;
	
	static {
		list = allTrees();
		//counterRef = Arrays.asList(list.get(0));
		int N = 4;
		boolean horizontal = true;
		boolean PQ = true;
		boolean redo = false;
		opt = new Options("FalsePositives", N, horizontal, PQ, redo);
//		for (FPTree t : counterRef) {
//			t.getFullExpression().toDotGraph2("t"+t.index, dir, true);
//			t.getFullExpression().toDotGraph2("t"+t.index, dir, false);
//		}
	}
	
	static class Pair {
		String func;
		int count;
		Pair(String s) {func = s; count = 0;}
	}
	
	static String subS(String s, int x) {
		int r = 0;
		while (r < s.length() && s.charAt(r)!='_')
			r++;
		int l = r;
		while (l>0 && x > 0) {
			if (s.charAt(--l)=='.')
				x--;
			else if (s.charAt(l)==' ')
				x=0;
		}
		return s.substring(l+1, r);
	}
	
	public static Set<String> get194() {
		Set<String> apps = new HashSet<>();
		
		return apps;
	}

	public static void main(String[] args)
	{
//		List<String> names = new ArrayList<String>();
//		Map<String, Pair> M = new HashMap<>();
//		for (String lineString : F.readLinesWithoutEmptyLines(new File(Dirs.Desktop, "194functions.txt"))) {
//			//String left = lineString.substring(0, lineString.indexOf("_"));
//			
//			//String func = left.substring(left.lastIndexOf(".")+1);
//			String func = subS(lineString, 5);
//			//P.p(func);
//			M.computeIfAbsent(func, k->new Pair(func)).count++;
//		}
//		M.values().stream().sorted((p1,p2)->p2.count-p1.count).forEach(p->P.p(p.count+" "+p.func));
//		P.pause();
		
		Map<String, List<FPTree>> map = loadFPTrees();
		
		P.p("total tree count: "+list.size());
		scores = getScores(list, opt, false);
		simpleCount();
		
		//counterRef();
		/*
		int[] test = new int[] {0, 29, 33, 5, 115, 63, 39};
		for (int i: test) {
			FPTree t = list.get(i);
			P.pf("Index: %d\nFrom App: %s\nName: %s\n\n\n", i, t.app, t.expF.getName());
			t.getFullExpression().toDotGraph2("t"+i, dir, false);
			t.getFullExpression().toDotGraph2("t"+i, dir, true);
		}
		*/
	}
	
	static void counterRef() {
		int[] counters = new int[] {0};
		int numMatched = 0;
		for (int i=0; i<list.size(); i++) {
			FPTree t1 = list.get(i);
			double score = -1.0;
			for (int j=0; j<list.size(); j++)
				score = Math.max(score, scores[i][j]);
			if (score > 0.9)
				numMatched++;
		}
		P.p("matched "+numMatched);
	}
	

	static Expression counterRef2;
	static boolean similarToCounterRef(Expression exp, Options opt) {
		if (counterRef2 == null) {
			counterRef2 = (Expression) F.readObject("C:\\workspace\\app_analysis\\notes\\Medoid_ExpressionTrees\\com.appslowcost.ps.apk\\com.seattleclouds.modules.podcast.b.b.doInBackground_-25082261_1.expression");
			TreeRefUtils.trim(counterRef2);
		}
		if (sim(exp, counterRef2, opt) > 0.8)
			return true;
		return false;
	}
	
	static void simpleCount() {
		double thresh = 0.9;
		int[][] counts = new int[list.size()][2];
		Set<String>[] matchedApp = new HashSet[list.size()];
		for (int i=0; i<list.size(); i++) {
//			FPTree t = list.get(i);
//			t.getFullExpression().toDotGraph2("t"+t.index, dir, true);
//			t.getFullExpression().toDotGraph2("t"+t.index, dir, false);
			int count = 0;
			matchedApp[i] = new HashSet<String>();
			for (int j=0; j<list.size(); j++) {
				if (scores[i][j] > thresh) {
					count++;
					matchedApp[i].add(list.get(j).app);
				}
			}
			counts[i][0] = i;
			counts[i][1] = matchedApp[i].size();
		}
		Arrays.sort(counts, (c1,c2)->c2[1]-c1[1]);
		for (int[] ii : counts)
			P.p(ii[0]+"  "+ii[1]+" "+list.get(ii[0]).app+" " + list.get(ii[0]).expF.getName());
		
		TreeMap<Integer, List<Integer>> map = new TreeMap<>(Collections.reverseOrder());
		for (int[] pair : counts)
			map.computeIfAbsent(pair[1], k-> new ArrayList<>()).add(pair[0]);
		
		for (int k : map.keySet()) {
			P.p("[]"+k+" "+map.get(k).size());
			
			File toDir = new File(Dirs.Desktop, "cluster_"+k);
			toDir.mkdirs();
			for (int i : map.get(k)) {
				File fullF = new File(dir, "t"+i+"_full.pdf");
				File trimmedF = new File(dir, "t"+i+"_full.pdf");
				F.copyToFolder(fullF, toDir);
				F.copyToFolder(trimmedF, toDir);
			}
		}
		File f194 = new File(Dirs.Desktop, "the194.txt");
		PrintWriter out = F.initPrintWriter(f194);
		map.firstEntry().getValue().forEach(k->{
			out.println(list.get(k).expF.getAbsolutePath());
		});
		out.close();
	}
	
	static void kMedoid(List<FPTree> list, double[][] scores) {
		RealMatrix mat = MatrixUtils.createRealMatrix(scores);
		int k = (int) Math.sqrt(list.size());
		k = 20;
		int[] labels = new KMedoidsParameters(k).fitNewModel(mat).getLabels();
		Set<Integer> medoidSet = parseLabels(labels, k);
		P.pf("k = %d, # med = %d\n", k, medoidSet.size());
		
		File dir = new File(Dirs.Desktop, "FPTreeMedoids");
		dir.mkdirs();
		Graphviz.skipComplexOnes = false;
		for (int i : medoidSet) {
			FPTree t = list.get(i);
			P.p("saving med "+i+" "+t.app+" "+t.expF.getName());
			t.getFullExpression().toDotGraph(t.expF.getName(), dir, false);
		}
	}
	
	static Set<Integer> parseLabels(int[] labels, int k) {
		Set<Integer> medoids = new HashSet<Integer>();
		for (int i=0; i<labels.length; i++)
			if (labels[i] != i)
				medoids.add(labels[i]);
		String text = "graph G{\n";
		text += "\tnode [shape=circle];\n";
		for (int i : medoids)
			text += "\t "+i+" [color=red,fontcolor=red];\n";
		for (int i=0; i<labels.length; i++)
			if (labels[i] != i)
				text += "\t"+i+" -- "+labels[i]+";\n";
		text += "}";
		Graphviz.makeDotGraph(text, "FPTreeCluster_k_"+k, Dirs.Desktop);
		return medoids;
	}
	
	static class FPTree {
		File expF;
		String app;
		Expression exp;
		int index;
		FPTree(File f) {
			expF = f;
			app = expF.getParentFile().getName();
		}
		Expression getExpression() {
			if (exp == null) {
				exp = (Expression) F.readObject(expF);
				TreeRefUtils.trim(exp);
			}
			return exp;
		}
		Expression getFullExpression() {
			return (Expression) F.readObject(expF);
		}
	}
	
	static double[][] getScores(List<FPTree> list, Options opt, boolean redo) {
		double[][] scores = new double[list.size()][list.size()];
		File dirFile = new File(Dirs.Desktop, "FPTreeMedoids");
		File scoreFile = new File(dirFile, opt.toString()+"_scores.txt");
		if (!redo && scoreFile.exists()) {
			int i=0;
			for (String line : F.readLinesWithoutEmptyLines(scoreFile)) {
				String[] parts = line.split(" ");
				for (int j=0; j<parts.length; j++) {
					scores[i][j] = Double.parseDouble(parts[j]);
//					if (scores[i][j] == 1) {
//						FPTree t1 = list.get(i);
//						FPTree t2 = list.get(j);
//						File tempDir = new File(Dirs.Desktop, "temp");
//						t1.getFullExpression().toDotGraph("t"+i, tempDir, false);
//						t2.getFullExpression().toDotGraph("t"+j, tempDir, false);
//						P.pause("1.0 "+i+" "+j);
//					}
				}
					
				i++;
			}
		}
		else {
			for (int i=0; i<list.size(); i++) {
				P.p("-- iteration "+(i+1)+"/"+list.size());
				FPTree t1 = list.get(i);
				scores[i][i] = 1.0;
				for (int j=i+1; j<list.size(); j++) {
					FPTree t2 = list.get(j);
					scores[i][j] = scores[j][i] = sim(t1.getExpression(), t2.getExpression(), opt);
				}
			}
			
			PrintWriter out = F.initPrintWriter(scoreFile);
			for (int i=0; i<list.size(); i++) {
				out.print(scores[i][0]);
				for (int j=1; j<list.size(); j++) {
					out.print(" "+scores[i][j]);
				}
				out.print('\n');
			}
			out.close();
		}
		return scores;
	}
	
	static double sim(Expression exp1, Expression exp2, Options opt) {
		Map<String, Integer> f1 = TreeFeature.collectFeatures(exp1, opt);
		Map<String, Integer> f2 = TreeFeature.collectFeatures(exp2, opt);
		Map<String, Integer> dict = new HashMap<String, Integer>();
		for (String f : f1.keySet())
			dict.putIfAbsent(f, dict.size());
		for (String f : f2.keySet())
			dict.putIfAbsent(f, dict.size());
		
		return Experiment1.sim_norm1_2(f1, f2, dict);
	}
	
	static Map<String, List<FPTree>> loadFPTrees() {
		Map<String, List<FPTree>> map = new HashMap<String, List<FPTree>>();
		File namesFile = new File(Dirs.Desktop, "fpTrees.txt");
		for (String pathString : F.readLinesWithoutEmptyLines(namesFile)) {
			File expFile = new File(pathString);
			String app = expFile.getParentFile().getName();
			map.computeIfAbsent(app, k->new ArrayList<>()).add(new FPTree(expFile));
		}
		return map;
	}
	
	static List<FPTree> allTrees() {
		List<FPTree> list = new ArrayList<Experiment_FalsePositives.FPTree>();
		int index = 0;
		File namesFile = new File(Dirs.Desktop, "fpTrees.txt");
		for (String pathString : F.readLinesWithoutEmptyLines(namesFile)) {
			File expFile = new File(pathString);
			FPTree t = new FPTree(expFile);
			t.index = index++;
			list.add(t);
		}
		return list;
	}

}
