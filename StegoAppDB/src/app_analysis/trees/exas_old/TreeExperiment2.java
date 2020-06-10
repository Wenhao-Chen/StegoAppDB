package app_analysis.trees.exas_old;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import apex.symbolic.Expression;
import app_analysis.common.Dirs;
import app_analysis.trees.TreeRefUtils;
import ui.ProgressUI;
import util.F;
import util.P;

public class TreeExperiment2 {
	
	public static final File confirmedStegoRecord = new File(Dirs.NotesRoot, "ConfirmedStegoApps.txt");
	public static Set<String> ConfirmedStegos;
	public static final File ExasDir = new File("C:\\workspace\\app_analysis\\notes\\Exas");
	static int N = 3;
	static boolean horizontal = true, pq = false;
	static List<Expression> trimmedRefTrees;
	static {
		ConfirmedStegos = new HashSet<>(F.readLinesWithoutEmptyLines(confirmedStegoRecord));
		trimmedRefTrees = new ArrayList<>();
		for (Expression ref : TreeRefUtils.trees.values()) {
			Expression exp = ref.clone();
			TreeRefUtils.trim(exp);
			trimmedRefTrees.add(exp);
		}
	}

	
	public static void main(String[] args) {
		//go2("stegos", Dirs.getStegoFiles());
		go2("image_editing", Dirs.getFiles(Dirs.ImageApps));
		//go("stegos", Dirs.getStegoFiles(), 3, true, false, false, false);
		//go("stegos", Dirs.getStegoFiles(), 3, true, false, false);
		//go("stegos", Dirs.getStegoFiles(), 3, false, true, false);
		//go("stegos", Dirs.getStegoFiles(), 3, false, false, false);
	}
	
	static void go2(String appSet, List<File> apks) {
		ProgressUI ui = ProgressUI.create("tree experiment");
		
		P.p("loading tree exas...");
		Map<String, List<TreeExas2>> trees = Step1_LoadTreeExas(apks);
		Map<String, Integer> nodeDict = new HashMap<>();
		Map<String, Integer> featureDict = new HashMap<>();
		
		File nodeDictF = new File(Dirs.Desktop, appSet+"_nodeDict.txt");
		File featureDictF = new File(Dirs.Desktop, appSet+"_featureDict.txt");
		// 1. collect node dict
		P.p("1. collecting node dict");
		if (nodeDictF.exists()) {
			F.readLinesWithoutEmptyLines(nodeDictF).forEach(line->{
				String[] parts = line.split(" , ");
				nodeDict.put(parts[0], Integer.parseInt(parts[1]));
			});
		}
		else {
			for (List<TreeExas2> list: trees.values())
			for (TreeExas2 te : list) {
				ui.newLine("node dict "+te.ID);
				collectNodeDict(nodeDict, te.getExpression_Normalized());
			}
			ui.newLine("node dict ref");
			for (Expression ref : trimmedRefTrees)
				collectNodeDict(nodeDict, ref);
			PrintWriter out = F.initPrintWriter(nodeDictF);
			for (String s : nodeDict.keySet())
				out.println(s+" , "+nodeDict.get(s));
			out.close();
		}
		P.p("-- node dict size: "+nodeDict.size());
		
		// 2. collect feature dict
		P.p("2. collecting feature dict");
		if (featureDictF.exists()) {
			F.readLinesWithoutEmptyLines(featureDictF).forEach(line->{
				String[] parts = line.split(" , ");
				featureDict.put(parts[0], Integer.parseInt(parts[1]));
			});
		}
		else {
			for (List<TreeExas2> list: trees.values())
				for (TreeExas2 te : list) {
					ui.newLine("feature dict "+te.ID);
					collectFeatureDict(featureDict, nodeDict, te.getExpression_Normalized());
			}
			ui.newLine("feature dict ref");
			for (Expression ref : trimmedRefTrees)
				collectFeatureDict(featureDict, nodeDict, ref);
			PrintWriter out = F.initPrintWriter(featureDictF);
			for (String s : featureDict.keySet())
				out.println(s+" , "+featureDict.get(s));
			out.close();
		}
		P.p("-- feature count: "+featureDict.size());
		for (String feature : featureDict.keySet())
			P.p("  "+feature);
		P.pause();
		
		// 3. start matching
		List<int[]> all_ref_vectors = new ArrayList<>();
		for (Expression ref : trimmedRefTrees) {
			Map<String, Integer> features = new HashMap<>();
			ExasUtils2.collectFeatures(features, ref, nodeDict, N, pq, horizontal);
			all_ref_vectors.add(ExasUtils2.generateVector(featureDict, features));
		}
		for (File apk : apks) {
			String app = apk.getName();
			double s1, s2, s3;
			s1 = s2 = s3 = -1.0;
			if (trees.containsKey(app))
			for (TreeExas2 te : trees.get(app)) {
				if (te.getExpression_Normalized() == null)
					continue;
				for (int i=0; i<trimmedRefTrees.size(); i++) {
					Expression refExp = trimmedRefTrees.get(i);
					int[] ref_vectors = all_ref_vectors.get(i);
					double sim1 = ExasUtils2.sim_1_norm(te.getExpression_Normalized(), refExp.root,
							ref_vectors, nodeDict, featureDict, N, pq, horizontal);
					double sim2 = ExasUtils2.sim_2_norm(te.getExpression_Normalized(), refExp.root,
							ref_vectors, nodeDict, featureDict, N, pq, horizontal);
					double simMax = ExasUtils2.sim_max_norm(te.getExpression_Normalized(), refExp.root,
							ref_vectors, nodeDict, featureDict, N, pq, horizontal);
					s1 = Math.max(s1, sim1);
					s2 = Math.max(s2, sim2);
					s3 = Math.max(s3, simMax);
				}
			}
			P.pf("%s\t%f\t%f\t%f\n", app, s1, s2, s3);
		}
		P.p("all done.");
	}
	
	
	static void collectNodeDict(Map<String, Integer> nodeDict, Expression exp) {
		if (exp == null)
			return;
		nodeDict.putIfAbsent(exp.root, nodeDict.size());
		for (Expression child : exp.children)
			collectNodeDict(nodeDict, child);
	}
	
	static void collectFeatureDict(Map<String, Integer> featureDict, Map<String, Integer> nodeDict, Expression exp) {
		Map<String, Integer> features = new HashMap<>();
		ExasUtils2.collectFeatures(features, exp, nodeDict, N, pq, horizontal);
		for (String f : features.keySet())
			featureDict.putIfAbsent(f, featureDict.size());
	}
	
	static void go(String appSet, List<File> apks, int nPath, 
			boolean doHorizontal, boolean doPQNode, boolean redoDictionary, boolean redoFeatures) {
		long t;
		
		if (nPath < 1 || nPath > 3) {
			P.e("nPath "+nPath+" out of range [1, 3]");
			return;
		}
		P.p("---- Experiment Settings ------------------------------");
		P.pf("App Set:            %s\n", appSet);
		P.pf("nPath:              %d\n", nPath);
		P.pf("horizontal 2-path:  %s\n", doHorizontal?"ON":"OFF");
		P.pf("(p,q) node:         %s\n", doPQNode?"ON":"OFF");
		P.pf("Dictionary:         %s\n", redoDictionary?"Force re-collect":"Load or collect");
		P.pf("Feature collection: %s\n", redoFeatures?"Force re-collect":"Load of collect");
		
		P.p("----");
		
		// 1. load TreeExas into a map: <AppName, List<TreeExas>>
		// This step simply put the serialized Expression objects in a map
		P.pf("Step 1 - Loading TreeExas objects... ");
		t = System.currentTimeMillis();
		Map<String, List<TreeExas2>> trees = Step1_LoadTreeExas(apks);
		P.p("Done. "+(System.currentTimeMillis()-t)/1000+" seconds.");
		
		// 2. generate node value hash map (for speed)
		// This step reads all the trees once and create hash maps for node values
		// TODO: make sure the tree used here is same as the next step
		P.pf("Step 2 - Loading tree node value dictionary... ");
		t = System.currentTimeMillis();
		Map<String, Integer> dict = Step2_LoadDictionary(appSet, trees, redoDictionary);
		P.p("Done. "+(System.currentTimeMillis()-t)/1000+" seconds.");
		
		// 3. collect feature hash map
		// This step collects n-path, horizontal 2-path, and PQ-node features
		// features are written in the form of <hash>-<hash>-<hash>...
		// Example: A 3-path of "literal + literal" will be hashed into: 1-2-1
		//  (assuming literal's hash value is 1 and +'s hash value is 2 in dict)
		// Every unique feature string will be recorded in the feature dict
		// Example: 1-2-1 -> 1, 2-1-2 -> 2, ...
		P.pf("Step 3 - Loading tree features... ");
		Map<String, Integer> featureDict = new HashMap<>();
		List<Integer> featureCounts = new ArrayList<>();
		t = System.currentTimeMillis();
		for (String app : trees.keySet()) {
			for (TreeExas2 te : trees.get(app)) {
				Map<String, Integer> features = 
						te.getFeatures(dict, appSet, nPath, doPQNode, doHorizontal, redoFeatures);
				for (String f : features.keySet())
					featureDict.putIfAbsent(f, featureDict.size());
				featureCounts.add(features.size());
			}
		}
		
		// also process reference vectors
		List<Map<String, Integer>> all_ref_features = new ArrayList<>();
		List<int[]> all_ref_vectors = new ArrayList<>();
		for (Expression refExp : trimmedRefTrees) {
			Map<String, Integer> ref_features = new HashMap<>();
			ExasUtils2.collectFeatures(ref_features, refExp, dict, nPath, doPQNode, doHorizontal);
			for (String f : ref_features.keySet())
				featureDict.putIfAbsent(f, featureDict.size());
			all_ref_features.add(ref_features);
			all_ref_vectors.add(ExasUtils2.generateVector(featureDict, ref_features));
		}
		P.p("Done. "+(System.currentTimeMillis()-t)/1000+" seconds.");
		P.p("feature count: "+featureDict.size());
		// 4. compare App Trees vs Ref Tree
		// Now we have the feature dict, we can turn the feature occurences
		// of a tree into a vector where each number represent the occurence of a feature
		// e.g. vector[0] is the occurence of feature 0 (which might be 1-2-1) for example
		P.pf("Step 4 - Comparing App Trees vs Ref Tree... ");
		P.p("# features: "+featureDict.size());
		t = System.currentTimeMillis();
		// 4.2 compare tree vectors with ref vectors
		Map<String, Double> appScores1 = new HashMap<>();
		Map<String, Double> appScores2 = new HashMap<>();
		Map<String, Double> appScores3 = new HashMap<>();
//		File sim1F = new File(Dirs.Desktop, "sim1.txt");
//		File sim2F = new File(Dirs.Desktop, "sim2.txt");
//		File simMaxF = new File(Dirs.Desktop, "simMax.txt");
//		PrintWriter out1 = F.initPrintWriter(sim1F);
//		PrintWriter out2 = F.initPrintWriter(sim2F);
//		PrintWriter outMax = F.initPrintWriter(simMaxF);
		double max1 , max2, max3;
		TreeExas2 te1, te2, te3;
		max1 = max2 = max3 = -2.0;
		te1 = te2 = te3 = null;
		double thresh1, thresh2, thresh3;
		Set<String> good1, good2, good3;
		good1 = new HashSet<>();
		good2 = new HashSet<>();
		good3 = new HashSet<>();
		thresh1 = thresh2 = thresh3 = 0.5;
		for (Map.Entry<String, List<TreeExas2>> entry : trees.entrySet()) {
			String app = entry.getKey();
			List<TreeExas2> list = entry.getValue();
			for (TreeExas2 te : list) {
				Map<String, Integer> features = te.getFeatures(dict, appSet, nPath, doPQNode, doHorizontal, false);
				if (features.isEmpty())
					continue;
				int x = 0;
				for (Expression refExp : trimmedRefTrees) {
					int[] ref_vectors = all_ref_vectors.get(x++);
					double sim1 = ExasUtils2.sim_1_norm(te.getExpression_Normalized(), refExp.root,
							ref_vectors, dict, featureDict, nPath, doPQNode, doHorizontal);
					double sim2 = ExasUtils2.sim_2_norm(te.getExpression_Normalized(), refExp.root,
							ref_vectors, dict, featureDict, nPath, doPQNode, doHorizontal);
					double simMax = ExasUtils2.sim_max_norm(te.getExpression_Normalized(), refExp.root,
							ref_vectors, dict, featureDict, nPath, doPQNode, doHorizontal);
					appScores1.put(app, Math.max(appScores1.getOrDefault(app, -2.0), sim1));
					appScores2.put(app, Math.max(appScores2.getOrDefault(app, -2.0), sim2));
					appScores3.put(app, Math.max(appScores3.getOrDefault(app, -2.0), simMax));
//					int[] vectors = ExasUtils.generateVector(featureDict, features);
//					double sim1 = ExasUtils.sim_1_norm(vectors, ref_vectors);
//					double sim2 = ExasUtils.sim_2_norm(vectors, ref_vectors);
//					double simMax = ExasUtils.sim_max_norm(vectors, ref_vectors);
					if (sim1 > max1) {
						P.p("new high sim1: "+sim1+". "+te.expF.getAbsolutePath());
						max1 = sim1; te1 = te;
					}
					if (sim2 > max2) {
						P.p("new high sim2: "+sim2+". "+te.expF.getAbsolutePath());
						max2 = sim2; te2 = te;
					}
					if (simMax > max3) {
						P.p("new high sim3: "+simMax+". "+te.expF.getAbsolutePath());
						max3 = simMax; te3 = te;
					}
					if (sim1 > thresh1) {
						if (good1.add(app)) {
//							P.p("--- similar ---");
//							te.visualize();
//							for (String key : features.keySet()) {
//								P.p(key+" "+features.get(key));
//							}
							//P.pause();
						}
						P.p(te.expF.getAbsolutePath());
					}
					if (sim2 > thresh2) {
						good2.add(app);
					}
					if (simMax > thresh3) {
						good3.add(app);
					}
//					out1.println(sim1);
//					out2.println(sim2);
//					outMax.println(simMax);
				}
				double s1 = appScores1.getOrDefault(app, -2.0);
				double s2 = appScores2.getOrDefault(app, -2.0);
				double s3 = appScores3.getOrDefault(app, -2.0);
				P.pf("%s\t%.1f\t%.1f\t%.1f\n", app, s1, s2, s3);
				
//				te.visualize();
//				print(ref_vectors, "--- ref ---");
//				print(vectors, "--- VVV ---");
//				P.pf("sim1/2/max = %.2f/%.2f/%.2f\n", sim1, sim2, simMax);
//				P.pause();
			}
		}
//		out1.close();
//		out2.close();
//		outMax.close();
		P.p("Done. "+(System.currentTimeMillis()-t)/1000+" seconds.");
		
		// 5. calculate vector distances (1-norm, 2-norm, max-norm)
		
		// 5. try different threshold to 
		
		P.p("Experiment done.\n---------------------------------");
		
//		P.p("max sim1: "+max1+", "+(te1==null?"null":te1.expF.getAbsolutePath()));
//		P.p("max sim2: "+max2+", "+(te2==null?"null":te2.expF.getAbsolutePath()));
//		P.p("max simMax: "+max3+", "+(te3==null?"null":te3.expF.getAbsolutePath()));
		P.p("-- app scores --");
		for (File apk : apks) {
			P.pf("%s: %.1f/%.1f/%.1f\n", apk.getName(),
					appScores1.get(apk.getName()), 
					appScores2.get(apk.getName()), 
					appScores3.get(apk.getName()));
		}
		P.p("------");
		P.pf("thresh1/thresh2/thresh3 = %.1f/%.1f/%.1f\n", thresh1, thresh2, thresh3);
		P.pf("# good1/good2/good3 = %d/%d/%d\n", good1.size(), good2.size(), good3.size());
		
		F.write(good1, new File(Dirs.Desktop, "good1.txt"), false);
		F.write(good1, new File(Dirs.Desktop, "good2.txt"), false);
		F.write(good1, new File(Dirs.Desktop, "good3.txt"), false);
	}
	
	static void print(int[] vector, String label) {
		P.p(label);
		for (int i=0; i<vector.length; i++) {
			System.out.print(vector[i]+"  ");
			if (i%20 ==19)
				P.p("");
		}
		P.p("");
	}
	
	
	static Map<String, List<TreeExas2>> Step1_LoadTreeExas(List<File> apks) {
		Map<String, List<TreeExas2>> res = new HashMap<>();
		File treeRoot = new File(Dirs.NotesRoot, "Medoid_ExpressionTrees");
		
		for (File apk : apks) {
			File appTreeDir = new File(treeRoot, apk.getName());
			// Load all the unique expressions
			if (appTreeDir.isDirectory()) {
				for (File f : appTreeDir.listFiles())
					if (f.getName().endsWith(".expression")) {
						String name = f.getName().substring(0, f.getName().length()-11);
						TreeExas2 tex = new TreeExas2(f, apk.getName(), name);
						tex.isStego();
						res.computeIfAbsent(apk.getName(), k->new ArrayList<>()).add(tex);
					}
			}
		}
		return res;
	}
	
	static Map<String, Integer> Step2_LoadDictionary(String appSet, Map<String, List<TreeExas2>> trees, boolean redoDict) {
		File dictFile = new File(ExasDir, "Dict_"+appSet+".txt");
		Map<String, Integer> dict = new HashMap<>();
		if (!redoDict && dictFile.exists() && dictFile.length()>0) {
			for (String line : F.readLinesWithoutEmptyLines(dictFile))
				dict.put(line, dict.size());
		} else {
			Set<String> values = new TreeSet<>();
			for (List<TreeExas2> list : trees.values()) {
				for (TreeExas2 te : list) {
					collectNodeValues(values, te.getExpression_Normalized());
				}
			}
			for (Expression refExp : trimmedRefTrees)
				collectNodeValues(values, refExp);
			PrintWriter out = F.initPrintWriter(dictFile);
			int index = 0;
			P.p("  [new dict] "+values.size());
			for (String value : values) {
				dict.put(value, index);
				out.println(value);
				P.pf("    %2d: %s\n", index, value);
				index++;
			}
			out.close();
		}
		return dict;
	}
	
	// this assumes the tree is already trimmed (literal and reference have no children)
	private static void collectNodeValues(Set<String> set, Expression exp) {
		if (exp == null)
			return;
		set.add(exp.root);
		for (Expression c : exp.children)
			collectNodeValues(set, c);
	}
	
	private static String[] order(Map<String, Integer> dict) {
		String[] ordered = new String[dict.size()];
		for (Map.Entry<String, Integer> entry : dict.entrySet())
			ordered[entry.getValue()] = entry.getKey();
		return ordered;
	}

}
