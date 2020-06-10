package app_analysis.trees.exas;

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
import app_analysis.trees.TreeRefUtils;
import app_analysis.trees.exas.TreeFeature.Options;
import util.F;
import util.Mailjet;
import util.P;

public class Experiment1 {

	static Set<String> shortList = new HashSet<>(Arrays.asList(
			"com.olmaredo.gliol.olmaredostego.apk",
			"com.talixa.pocketstego.apk",
			"jubatus.android.davinci.apk"));
	static boolean onlyProcessShortList = false;
	
	public static void main(String[] args) {
		List<File> apks = Dirs.getFiles(Dirs.Stego_Github, Dirs.Stego_Others, Dirs.Stego_PlayStore);
		String appSet = "stegos";
		
		//apks = Dirs.getFiles(Dirs.ImageApps);
		//appSet = "image_editing";
		int N = 4;
		boolean horizontal = true;
		boolean PQ = true;
		boolean redo = false;
		Options opt = new Options(appSet, N, horizontal, PQ, redo);
		long t = System.currentTimeMillis();
		
		Map<String, List<TreeWrapper>> allAppTrees = getTrees(apks);
		Map<String, List<Expression>> allRefTrees = Reference.getReferenceTrees();
		Map<String, List<Map<String, Integer>>> allRefFeatures = new HashMap<>();
		Map<String, List<Integer>> allRefHashes = new HashMap<>();
		
		// 1. generate tree features and get sparse feature space
		P.p("gathering sparse feature space...");
		for (File apk : apks) {
			for (TreeWrapper tree : allAppTrees.get(apk.getName())) {
				Map<String, Integer> features = tree.getFeatures(opt);
				opt.updateFeatureDict(features);
			}
		}
		for (Map.Entry<String, List<Expression>> entry : allRefTrees.entrySet()) {
			List<Map<String, Integer>> groupRefFeatures = new ArrayList<>();
			List<Integer> groupRefHashes = new ArrayList<>();
			for (Expression exp : entry.getValue()) {
				Map<String, Integer> features = TreeFeature.collectFeatures(exp, opt);
				opt.updateFeatureDict(features);
				groupRefFeatures.add(features);
				groupRefHashes.add(exp.toStringRaw().hashCode());
			}
			allRefFeatures.put(entry.getKey(), groupRefFeatures);
			allRefHashes.put(entry.getKey(), groupRefHashes);
		}
		
		P.p("feature count: "+opt.featureDict.size());
		
		// 2. calculate similarities between app trees and reference trees
		Map<String, Map<String, Double>> allAppScores = new HashMap<>();
		for (File apk : apks) {
			double maxSim1_overall = -1.0;
			String matchedGroup = "none";
			//P.p("comparing "+apk.getName()+" trees");
			Map<String, Double> appScores = new HashMap<>();
			if (!onlyProcessShortList || shortList.contains(apk.getName()))
			for (String refGroup : allRefTrees.keySet()) {
				double maxSim1_group = -1.0;
				List<Map<String, Integer>> groupRefFeatures = allRefFeatures.get(refGroup);
				List<Integer> groupRefHashes = allRefHashes.get(refGroup);
				for (TreeWrapper tree : allAppTrees.get(apk.getName())) {
					for (int i=0; i<groupRefFeatures.size(); i++) {
						double sim1 = tree.getSim1(groupRefFeatures.get(i), groupRefHashes.get(i), opt);
						maxSim1_group = Math.max(maxSim1_group, sim1);
					}
				}
				appScores.put(refGroup, maxSim1_group);
				if (maxSim1_group > maxSim1_overall) {
					maxSim1_overall = maxSim1_group;
					matchedGroup = refGroup;
				}
			}
			allAppScores.put(apk.getName(), appScores);
			if (!onlyProcessShortList || shortList.contains(apk.getName()))
				P.p(matchedGroup+" "+maxSim1_overall+" "+apk.getName());
		}
		
		opt.saveDict();
		P.p("Done. Time: "+(System.currentTimeMillis()-t)/1000+" seconds");
		P.p("\n\n");
		
		P.pf("%s", "App Name");
		for (String group : allRefTrees.keySet()) {
			P.pf("\t%s", group);
		}
		P.p("");
		for (File apk : apks)
		if (!onlyProcessShortList || shortList.contains(apk.getName()))
		if (allAppScores.containsKey(apk.getName())) {
			P.pf(apk.getName());
			Map<String, Double> appScores = allAppScores.get(apk.getName());
			for (String group : allRefTrees.keySet()) {
				double score = appScores.getOrDefault(group, -1.0);
				P.pf("\t%f", score);
			}
			P.p("");
		}
		Mailjet.email(opt.appSet+" experiment 1 done.");
	}
	
	static void test(Options opt) {
		Expression refTree = Reference.getReferenceTrees().get("and_mask").get(62);
		refTree.toDotGraph("tempp", Dirs.Desktop, false);
		Map<String, Integer> refFeatures = TreeFeature.collectFeatures(refTree, opt);
		File dir = new File("C:\\workspace\\app_analysis\\notes\\Trimmed_ExpressionTrees\\com.talixa.pocketstego.apk");
		String tempName = "temppp";
		for (File expF : dir.listFiles())
		if (expF.getName().endsWith(".expression")) {
			Expression appTree = (Expression) F.readObject(expF);
			appTree.toDotGraph(tempName, Dirs.Desktop, false); 
			TreeRefUtils.trim2(appTree);
			appTree.toDotGraph(tempName+"_t", Dirs.Desktop, false); 
			tempName += "p";
			//P.p(expF.getAbsolutePath());
			double sim1 = sim_norm1(appTree, refFeatures, opt);
			P.p(sim1+"   "+ expF.getName());
			
			P.pause();
		}
	}
	
	static double sim_norm1(Expression exp, Map<String, Integer> refFeatures, Options opt) {
		if (exp == null || refFeatures == null)
			return -1.0;
		double sim1 = sim_norm1(TreeFeature.collectFeatures(exp, opt), refFeatures, opt.featureDict);
		for (Expression child : exp.children)
			sim1 = Math.max(sim1, sim_norm1(child, refFeatures, opt));
		return sim1;
	}
	
	static double sim_norm1(Map<String, Integer> f1, Map<String, Integer> f2, Map<String, Integer> featureDict) {
		int[] v1 = new int[featureDict.size()];
		int[] v2 = new int[featureDict.size()];
		for (Map.Entry<String, Integer> entry : f1.entrySet()) {
			//if (!featureDict.containsKey(entry.getKey()))
			//	P.p("unknown feature: " +entry.getKey());
			v1[featureDict.get(entry.getKey())] = entry.getValue();
		}
		for (Map.Entry<String, Integer> entry : f2.entrySet())
			v2[featureDict.get(entry.getKey())] = entry.getValue();
		
		int[] v1_v2 = new int[v1.length];
		for (int i=0; i<v1.length; i++)
			v1_v2[i] = Math.abs(v1[i]-v2[i]);
		return 1 - 2*norm1(v1_v2)/(norm1(v1)+norm1(v2));
	}
	
	static double norm1(int[] v) {
		double res = 0;
		for (int i : v)
			res += i;
		return res;
	}
	
	static void debug(TreeWrapper tree, Map<String, Integer> features, Options opt) {
		P.p("\ntree: "+tree.expF.getAbsolutePath());
		tree.visualize();
		opt.printNodeDict();
		for (Map.Entry<String, Integer> entry : features.entrySet()) {
			P.pf("%20s : %d\n", entry.getKey(), entry.getValue());
		}
		P.pause();
	}
	
	
	static Map<String, List<TreeWrapper>> getTrees(List<File> apks) {
		Map<String, List<TreeWrapper>> map = new HashMap<>();
		File medoidRoot = new File(Dirs.NotesRoot, "Trimmed_ExpressionTrees");
		for (File apk : apks) {
			String app = apk.getName();
			List<TreeWrapper> trees = new ArrayList<>();
			File appDir = new File(medoidRoot, app);
			for (File expF : appDir.listFiles())
			if (expF.getName().endsWith(".expression"))
				trees.add(new TreeWrapper(expF, app, true));
			map.put(app, trees);
		}
		return map;
	}
}
