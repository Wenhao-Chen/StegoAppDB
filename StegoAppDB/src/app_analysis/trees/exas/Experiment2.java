package app_analysis.trees.exas;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import apex.symbolic.Expression;
import app_analysis.common.Dirs;
import app_analysis.trees.exas.TreeFeature.Options;
import util.F;
import util.Mailjet;
import util.P;

public class Experiment2 {

	static class RefGroup {
		String name;
		List<RefTree> trees;
	}
	static class RefTree {
		Expression exp;
		int hash;
		Map<TreeWrapper, Double> scores;
		TreeMap<Double, List<TreeWrapper>> sortedScores;
	}
	
	static Set<String> set = new HashSet<>(Arrays.asList(
			"com.chenqihong.stegodroid.apk",
			"com.facebook.fb_hack.apk",
			"com.hard.light.eight5.apk",
			"com.gmail.sapp.g.james.goodapp.apk",
			"com.sb.dev.steganographer.apk",
			"bearapps.com.steganography.apk",
			"com.goobers.steganography.apk",
			"com.nsoeaung.photomessenger.apk"
			));
	
	public static void main(String[] args) {
		long time = System.currentTimeMillis();
		String appSet = "stegos";
		int N = 4;
		boolean horizontal = true;
		boolean PQ = true;
		boolean redo = false;
		Options opt = new Options(appSet, N, horizontal, PQ, redo);
		
		List<File> apks = Dirs.getFiles(Dirs.Stego_Github, Dirs.Stego_Others, Dirs.Stego_PlayStore);
		Set<String> selectedApp = new HashSet<>();
		Set<TreeWrapper> selectedTrees = new HashSet<>();
		Map<String, List<TreeWrapper>> allAppTrees = Experiment1.getTrees(apks);
		
		// 1. select ref trees
		P.p("selecting reference trees from apps...");
		getAppRefTrees(apks, opt, selectedApp, selectedTrees);
		
		// 2. generate ref data
		P.p("generating reference data...");
		Map<String, List<Expression>> allRefTrees = new HashMap<>();
		Map<String, List<Map<String, Integer>>> allRefFeatures = new HashMap<>();
		Map<String, List<Integer>> allRefHashes = new HashMap<>();
		for (TreeWrapper selected : selectedTrees)
		if (set.contains(selected.appName)) {
			Expression exp = selected.getExpressionTrimmed();
			allRefTrees.put(selected.appName, Arrays.asList(exp));
			allRefFeatures.put(selected.appName, Arrays.asList(TreeFeature.collectFeatures(exp, opt)));
			allRefHashes.put(selected.appName, Arrays.asList(exp.toStringRaw().hashCode()));
		}
		
		// 2. detect other apps
		P.p("processing...");
		Map<String, Map<String, Double>> allAppScores = new HashMap<>();
		for (File apk : apks) {
			double maxSim1_overall = -1.0;
			String matchedGroup = "none";
			//P.p("comparing "+apk.getName()+" trees");
			Map<String, Double> appScores = new HashMap<>();
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
			P.p(matchedGroup+" "+maxSim1_overall+" "+apk.getName());
		}
		time = System.currentTimeMillis() - time;
		P.p("All Done. Time: "+(time/1000)+" seconds"); 
		P.p("\n\n");
		
		P.pf("%s", "App Name");
		for (String group : allRefTrees.keySet()) {
			P.pf("\t%s", group);
		}
		P.p("");
		for (File apk : apks)
		if (allAppScores.containsKey(apk.getName())) {
			P.pf(apk.getName());
			Map<String, Double> appScores = allAppScores.get(apk.getName());
			for (String group : allRefTrees.keySet()) {
				double score = appScores.getOrDefault(group, -1.0);
				P.pf("\t%f", score);
			}
			P.p("");
		}
		Mailjet.email(opt.appSet+" experiment 2 done.");
	}
	
	static void getAppRefTrees(List<File> apks, Options opt, Set<String> apps_overall, Set<TreeWrapper> appTrees) {
		
		
		
		// 1. get all ref hashes ready
		//P.p("1. getting ref trees ready");
		Map<String, List<Expression>> allRefTrees = Reference.getReferenceTrees();
		Map<String, RefGroup> refGroups = new HashMap<>();
		Map<Integer, RefTree> refLookup = new HashMap<>(); 
		for (String group : allRefTrees.keySet()) {
			RefGroup g = new RefGroup();
			g.name = group;
			g.trees = new ArrayList<>();
			List<Expression> exps = allRefTrees.get(group);
			for (Expression exp : exps) {
				RefTree t = new RefTree();
				t.exp  = exp;
				t.hash = exp.toStringRaw().hashCode();
				t.scores = new HashMap<>();
				t.sortedScores = new TreeMap<>();
				g.trees.add(t);
				refLookup.putIfAbsent(t.hash, t);
			}
			refGroups.put(group, g);
		}
		
		// 2. record the scores into each RefTree
		//P.p("2. recording ref tree scores");
		Map<String, List<TreeWrapper>> allAppTrees = Experiment1.getTrees(apks);
		Set<String> stegoApps = new HashSet<>(
				F.readLinesWithoutEmptyLines(new File(Dirs.NotesRoot, "ConfirmedStegoApps.txt")));
		for (File apk : apks)
		if (stegoApps.contains(apk.getName())){
			List<TreeWrapper> trees = allAppTrees.get(apk.getName());
			for (TreeWrapper tw : trees) {
				for (RefGroup g : refGroups.values())
				for (RefTree rt : g.trees) {
					double sim1 = tw.getSim1(null, rt.hash, opt);
					rt.scores.put(tw, sim1);
					rt.sortedScores.computeIfAbsent(sim1, k->new ArrayList<>()).add(tw);
				}
			}
		}
		
		// 3. select top M app trees from each RefTree
		//int M = 3;
		//P.p("3. picking trees");
		for (RefGroup g : refGroups.values()) {
			Set<String> apps = new HashSet<>();
			for (RefTree rt : g.trees) {
				// take the tree with highest score
				// if multiple trees have the highest score, take the smallest one
				List<TreeWrapper> tws = rt.sortedScores.lastEntry().getValue();
				Collections.sort(tws, (t1, t2)->t1.getSize()-t2.getSize());
				TreeWrapper selectedAppTree = rt.sortedScores.lastEntry().getValue().get(0);
				if (apps.add(selectedAppTree.appName))
					appTrees.add(selectedAppTree);
			}
			apps_overall.addAll(apps);
			//P.p("--- "+g.name+" "+apps.size());
		}
		//P.p("total: "+appTrees.size());
	}
	
	
}














