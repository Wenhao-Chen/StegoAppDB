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
import app_analysis.trees.exas.raw_results.App;
import util.F;
import util.Mailjet;
import util.P;

public class Experiment1 {

	static Set<String> shortList = new HashSet<>(Arrays.asList(
			"ir.hozebook.makaseb.apk",
			"ir.tahoorapp.bedaye.apk",
			"ir.tahoorapp.golestan.apk",
			"ir.tahoorapp.hafez.apk",
			"ir.tahoorapp.maghtal.apk",
			"ir.tahoorapp.manteq.apk",
			"ir.tahoorapp.medical.terminology.apk",
			"ir.tahoorapp.ney.nameh.apk"));
	static boolean onlyProcessShortList = false;
	
	static Set<String> refTreesToSkip = new HashSet<String>(Arrays.asList(
			"and_mask_1",
			"one_step_2",
			"one_step_11"
			));
	
	static boolean skipSomeRefTrees = false;
	
	static int averageTreeSize(List<TreeWrapper> trees) {
		int total = 0;
		if (trees.isEmpty())
			return total;
		for (TreeWrapper t : trees)
			total += t.getExpressionTrimmed().nodeCount();
		return (int)((double)total / trees.size() + 0.5);
	}
	
	public static void main(String[] args) {
		apks = Dirs.getStegoFiles(); appSet = "stegos";
		apks = Dirs.getAllFiles(); appSet = "both";
		apks = Dirs.get150Apps2(); appSet = "150apps";
		
		skipSomeRefTrees = false;
		int fp1 = go();
		skipSomeRefTrees = true; int fp2 = go(); P.p("fp1 vs fp2: "+fp1+" "+fp2);
		Mailjet.email("check result");
	}
	
	static List<File> apks;
	static String appSet;
	static int go() {
		
		int N = 4;
		boolean horizontal = true;
		boolean PQ = true;
		boolean redo = false;
		Options opt = new Options(appSet, N, horizontal, PQ, redo);
		long t = System.currentTimeMillis();
		
		Map<String, List<TreeWrapper>> allAppTrees = getTrees(apks);
//		for (File apkFile  : apks)
//			P.p(apkFile.getName()+"\t"+TreeWrapper.getOriginalTreeCount(apkFile.getName())
//					+"\t"+allAppTrees.get(apkFile.getName()).size()+"\t"+averageTreeSize(allAppTrees.get(apkFile.getName())));
//		P.pause("ready");
		
		Map<String, List<Expression>> allRefTrees = Reference.getReferenceTrees();
		int total_ref = 0;
		for (List<Expression> list : allRefTrees.values())
			total_ref += list.size();
		P.p("total ref trees: " + total_ref);
		//P.pause();
		Map<String, List<Map<String, Integer>>> allRefFeatures = new HashMap<>();
		Map<String, List<Integer>> allRefHashes = new HashMap<>();
		List<String> FPApps = new ArrayList<String>();
		
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
		
		Map<String, List<String>> badies = new HashMap<>();
		Map<String, Set<String>> badieApps = new HashMap<String, Set<String>>();
		// 2. calculate similarities between app trees and reference trees
		// for each app, save the one tree that has the highest matching score
		Map<String, Map<String, Double>> allAppScores = new HashMap<>();
		Map<String, Double> maxAppScores = new HashMap<>();
		File oneTreeDir = new File(Dirs.Desktop, "One Tree");
		oneTreeDir.mkdirs();
		for (File apk : apks) {
			boolean isStego = App.getStegoAppNames().contains(apk.getName());
			double maxSim1_overall = -1.0;
			String matchedGroup = "none";
			TreeWrapper maxTree = null;
			//P.p("comparing "+apk.getName()+" trees");
			Map<String, Double> appScores = new HashMap<>();
			if (!onlyProcessShortList || shortList.contains(apk.getName()))
			for (String refGroup : allRefTrees.keySet()) {
				double maxSim1_group = -1.0;
				List<Map<String, Integer>> groupRefFeatures = allRefFeatures.get(refGroup);
				List<Integer> groupRefHashes = allRefHashes.get(refGroup);
				TreeWrapper maxTreeInGroup = null;
				for (TreeWrapper tree : allAppTrees.get(apk.getName())) {
					for (int i=0; i<groupRefFeatures.size(); i++) {
						String refID = refGroup+"_"+i;
						if (skipSomeRefTrees && refTreesToSkip.contains(refID)) {
							continue;
						}
						double sim1 = tree.getSim1(groupRefFeatures.get(i), groupRefHashes.get(i), opt);
						if (sim1 > maxSim1_group) {
							maxSim1_group = sim1;
							maxTreeInGroup = tree;
							if (!isStego && sim1 > 0.9) {
								Expression bad = allRefTrees.get(refGroup).get(i);
								if (!badies.containsKey(refID)) {
									bad.toDotGraph("bad_"+refID, Dirs.Desktop, false);
								}
								badies.computeIfAbsent(refID, k->new ArrayList<>()).add("");
								badieApps.computeIfAbsent(refID, k->new HashSet<>()).add(apk.getName());
							}
						}
					}
				}
				appScores.put(refGroup, maxSim1_group);
				if (maxSim1_group > maxSim1_overall) {
					maxSim1_overall = maxSim1_group;
					matchedGroup = refGroup;
					maxTree = maxTreeInGroup;
				}
			}
			allAppScores.put(apk.getName(), appScores);
			maxAppScores.put(apk.getName(), maxSim1_overall);
			if (!onlyProcessShortList || shortList.contains(apk.getName()))
				P.p(matchedGroup+" "+maxSim1_overall+" "+apk.getName());
			if (maxSim1_overall > 0.9 && !isStego) {
				FPApps.add(apk.getName());
				File appOneTreeDir = new File(oneTreeDir, apk.getName());
				appOneTreeDir.mkdirs();
				File expF = new File(appOneTreeDir, maxTree.expF.getName());
				F.copy(maxTree.expF, expF);
				Expression exp = (Expression) F.readObject(expF);
				exp.toDotGraph(expF.getName(), appOneTreeDir, false);
			}
		}
		
		opt.saveDict();
		
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
		
		P.p("-- false positives using 0.9 thresh -- "+FPApps.size());
		//for (String app : FPApps)
		//	P.p(app);
		
		ROC.draw(opt, allAppScores, maxAppScores, allRefTrees.keySet());
		//Mailjet.email(opt.appSet+" experiment 1 done.");
		
		P.p("-- badies -- ");
		for (String badID : badieApps.keySet()) {
			P.pf(badID+" "+badies.get(badID).size()+" : ");
			for (String name : badieApps.get(badID))
				P.pf(" "+name);
			P.p("");
		}
		P.p("Done. Time: "+(System.currentTimeMillis()-t)/1000+" seconds");
		P.p("\n\n");
		return FPApps.size();
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
	
	//NOTE: this one use the formula 1 - 2*(|x1-x2|)/(|x1|+|x2|)
	// result is in range of -1 to 1
	static double sim_norm1(Map<String, Integer> f1, Map<String, Integer> f2, Map<String, Integer> featureDict) {
		
		for (Map.Entry<String, Integer> entry : f1.entrySet()) {
			if (!featureDict.containsKey(entry.getKey()))
				featureDict.put(entry.getKey(), featureDict.size());
		}
		int[] v1 = new int[featureDict.size()];
		int[] v2 = new int[featureDict.size()];
		for (Map.Entry<String, Integer> entry : f1.entrySet())
			v1[featureDict.get(entry.getKey())] = entry.getValue();
		
		for (Map.Entry<String, Integer> entry : f2.entrySet())
			v2[featureDict.get(entry.getKey())] = entry.getValue();
		
		int[] v1_v2 = new int[v1.length];
		for (int i=0; i<v1.length; i++)
			v1_v2[i] = Math.abs(v1[i]-v2[i]);
		return 1 - 2*norm1(v1_v2)/(norm1(v1)+norm1(v2));
	}
	
	//NOTE: this one use the formula 1 - (|x1-x2|)/(|x1|+|x2|)
		// result is in range of 0 to 1
	static double sim_norm1_2(Map<String, Integer> f1, Map<String, Integer> f2, Map<String, Integer> featureDict) {
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
		return 1 - norm1(v1_v2)/(norm1(v1)+norm1(v2));
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
