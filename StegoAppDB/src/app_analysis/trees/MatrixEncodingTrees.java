package app_analysis.trees;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import apex.symbolic.Expression;
import app_analysis.common.Dirs;
import app_analysis.trees.exas.Experiment1;
import app_analysis.trees.exas.TreeFeature;
import app_analysis.trees.exas.TreeFeature.Options;
import util.F;
import util.Mailjet;
import util.P;

public class MatrixEncodingTrees {

	static Set<String> jpegApps = new HashSet<>(Arrays.asList(
			"kcich.stegoqr.apk",
			"ca.repl.free.camopic.apk",
			"com.paranoiaworks.unicus.android.sse.apk",
			"info.guardianproject.pixelknot.apk"));
	
	public static void main(String[] args)
	{
		File recordF = new File(Dirs.Desktop, "matrix_records.txt");
		List<String> lines = F.readLinesWithoutEmptyLines(recordF);
		Map<String, String> map = new HashMap<>();
		for (String line : lines) {
			String[] parts = line.split("\t");
			String name = parts[0];
			if (map.containsKey(name)) {
				
				double d1 = 0, d2 = 0;
				for (int i=1; i<parts.length; i++)
					d1 += Double.parseDouble(parts[i]);
				String[] parts2 = map.get(name).split("\t");
				for (int i=1; i<parts2.length; i++)
					d2 += Double.parseDouble(parts2[i]);
				if (d1 > d2)
					map.put(name, line);
			}
			else
				map.put(name, line);
		}
//		int levels = 3;
//		Expression refTree = getRef2(levels);
		
		Options opt = initOptions();
		List<Expression> refTrees = new ArrayList<>();
		File refDir = new File(Dirs.Desktop, "matrix_encoding_reference_trees_new");
		refDir.mkdirs();
		for (int i=1; i<=32; i++) {
			Expression t = getRef2(i);
			refTrees.add(t);
			t.toDotGraph("ref_n_"+i, refDir, false);
		}
		List<File> apks = Dirs.getAllFiles();
		long time = System.currentTimeMillis();
		Set<String> alreadyDone = new HashSet<>(F.readLinesWithoutEmptyLines(new File(Dirs.Desktop, "Matrix_already_done.txt")));
		File root = new File(Dirs.Desktop, "matrix_encoding");
		for (File apk : apks) {
			if (map.containsKey(apk.getName())) {
				P.p(map.get(apk.getName()));
				continue;
			}
				
			File appDir = new File(root, apk.getName());
			//if (!jpegApps.contains(appDir.getName()))
			//	continue;
			String maxTree = "";
			File treeDir = new File(appDir, "trees");
			int count = 0;
			double[] scores = new double[refTrees.size()];
			String scoreString = "";
			for (double score : scores)
				scoreString += "\t"+score;
			P.p(appDir.getName()+"\t"+count+scoreString);
			/*if (treeDir.listFiles() != null && !alreadyDone.contains(apk.getName())) {
				P.p(apk.getName()+" "+treeDir.list().length);
				/*
				for (File expF : treeDir.listFiles()) {
					if (expF.getName().endsWith(".expression")) {
						count++;
						Expression exp = (Expression) F.readObject(expF);
						//P.p("[before] "+exp.toStringRaw());
						TreeRefUtils.trim2(exp);
						
						//P.p("[after]  "+exp.toStringRaw());
						//exp.toDotGraph("temp_trimmed", Dirs.Desktop, false);
						//P.pause();
						for (int i=0; i<refTrees.size(); i++) {
							double sim = sim1(exp, refTrees.get(i), opt);
							if (sim > scores[i]) {
								scores[i] = sim;
								maxTree = expF.getAbsolutePath();
							}
						}
					}
				}
				
			}*/
			
		}
		time = (System.currentTimeMillis()-time)/1000;
		Mailjet.email("matrix done. "+time+" seconds.");
	}
	
	static double max(double[] arr) {
		double d = Double.MIN_VALUE;
		for (double dd : arr)
			d = Math.max(d, dd);
		return d;
	}
	
	static double sim1(Expression exp1, Expression exp2, Options opt) {
		Map<String, Integer> f1 = TreeFeature.collectFeatures(exp1, opt);
		Map<String, Integer> f2 = TreeFeature.collectFeatures(exp2, opt);
		return Experiment1.sim_norm1(f1, f2, opt.featureDict);
	}
	
	static TreeFeature.Options initOptions() {
		int N = 4;
		boolean horizontal = true;
		boolean PQ = true;
		boolean redo = false;
		Options opt = new Options("matrix_encoding", N, horizontal, PQ, redo);
		long t = System.currentTimeMillis();
		return opt;
	}
	
	static Expression getRef2(int levels) {
		Expression coeffLSB = new Expression("&").add("literal").add("literal");
		Expression l = new Expression("^").add("literal").add(coeffLSB);
		while (--levels > 0) {
			Expression p = new Expression("^").add(l).add("literal");
			l = p;
		}
		return l;
	}

	
	static Expression getRef(int levels) {
		Expression l = new Expression("^").add("literal").add("literal");
		while (--levels > 0) {
			Expression p = new Expression("^").add(l).add("literal");
			l = p;
		}
		return l;
	}
}
