package app_analysis.trees.exas;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bouncycastle.crypto.modes.CTSBlockCipher;

import app_analysis.common.Dirs;
import app_analysis.trees.exas.TreeFeature.Options;
import util.F;
import util.Mailjet;
import util.P;

public class Experiment_cross_val {

	public static void main(String[] args)
	{
		//warmupMatchingResult();
		//cleanupExpressionTrees();
		//cross_validate(50, 30);
		read_results();
	}
	
	static void read_results() {
		File appLabelFile = new File(Dirs.NotesRoot, "StegoApp_Labels.txt");
		Map<String, String> appLabels = new HashMap<String, String>();
		for (String s : F.readLinesWithoutEmptyLines(appLabelFile)) {
			String[] partStrings = s.split(" , ");
			appLabels.put(partStrings[0], partStrings[1]);
		}
		
		List<String> groupNameStrings = Reference.getReferenceGroupNames();
		Map<String, Integer> map = new HashMap<String, Integer>();
		for (String string : groupNameStrings)
			map.put(string, map.size());
		File reportRootDir = new File(Dirs.NotesRoot, "Experiment_cross_val");
		for (File dir : reportRootDir.listFiles()) {
			String[] parts = dir.getName().split("_");
			int num_total = 120;
			int num_refs = Integer.parseInt(parts[1]);
			int index = Integer.parseInt(parts[2]);
			int num_testing = num_total - num_refs;
			int false_pos = 0, false_neg_2 = 0, false_neg = 0;
			File resultFile = new File(dir, "Raw Results.txt");
			List<String> lines = F.readLinesWithoutEmptyLines(resultFile);
			Set<String> falsePosApps = new HashSet<String>();
			for (int i=1; i<lines.size(); i++) {
				parts = lines.get(i).split("\t");
				String app = parts[0];
				String actualLabel = appLabels.get(app);
				double maxScore = -1.0;
				String testedLabel = "N/A";
				for (int j=1; j<parts.length; j++) {
					double score = Double.parseDouble(parts[j]);
					if (score > 0.5 && score > maxScore) {
						maxScore = score;
						testedLabel = groupNameStrings.get(j-1);
					}
				}
				//P.pause();
				if (actualLabel.equals("N/A") && maxScore > 0.5) {
					false_pos++;
					falsePosApps.add(app);
				}
				if (!actualLabel.equals("N/A") && maxScore < 0.5)
					false_neg++;
				if (!actualLabel.equals("N/A") && !actualLabel.equals(testedLabel))
					false_neg_2++;
			}
			
			P.pf("%d\t%d\t%d\t%d\t%d\t%d\n", num_refs, num_testing, index, false_neg, false_neg_2, false_pos);
			if(num_refs == 60) {
				P.p("-- false pos --");
				for (String string : falsePosApps)
					P.p("   "+string);
				P.pause();
			}
			
		}
	}
	// problems: 
	//   some stego apps don't have trees
	//   when selecting reference apps, only select stego apps that have trees?
	static void cross_validate(int count_ref_apps, int count_runs) {
		List<File> apks = Dirs.getStegoFiles();
		String appSet = "stegos";
		long t = System.currentTimeMillis();
		int N = 4;
		boolean horizontal = true;
		boolean PQ = true;
		boolean redo = false;
		Options opt = new Options(appSet, N, horizontal, PQ, redo);
		
		File appLabelFile = new File(Dirs.NotesRoot, "StegoApp_Labels.txt");
		
		Map<String, String> appLabels = new HashMap<String, String>();
		List<String> stegoAppList = new ArrayList<String>();
		Map<String, List<TreeWrapper>> allApps = Experiment1.getTrees(apks);
		
		// load stego app labels
		for (String s : F.readLinesWithoutEmptyLines(appLabelFile)) {
			String[] partStrings = s.split(" , ");
			appLabels.put(partStrings[0], partStrings[1]);
			if (!partStrings[1].equals("N/A"))
				stegoAppList.add(partStrings[0]);
		}
		P.pf("total/ref/test apps: %d/%d/%d\n", allApps.size(), count_ref_apps, allApps.size()-count_ref_apps);
		P.p("labeled stego apps: "+stegoAppList.size());
		
		File reportRootDir = new File(Dirs.NotesRoot, "Experiment_cross_val");
		reportRootDir.mkdirs();
		for (int i=0; i < count_runs; i++) {
			P.pf("--- Experiment run: %d/%d\n", i+1, count_runs);
			Map<String, List<TreeWrapper>> refGroups = new HashMap<String, List<TreeWrapper>>();
			Map<String, List<TreeWrapper>> testApps = new HashMap<String, List<TreeWrapper>>();
			/** 1. load reference apps and test apps */
			Set<String> refAppNames = randomSelect(stegoAppList, count_ref_apps);
			for (String appName : allApps.keySet()) {
				List<TreeWrapper> treeWrappers = allApps.get(appName);
				if (refAppNames.contains(appName)) {
					String refGroup = appLabels.get(appName);
					refGroups.computeIfAbsent(refGroup, k->new ArrayList<>())
							 .addAll(treeWrappers);
				} else {
					testApps.put(appName, treeWrappers);
				}
			}
			P.pf("\tref groups: %d\n", refGroups.size());
//			for (String group : refGroups.keySet()) {
//				P.pf("%12s: %d\n", group, refGroups.get(group).size());
//			}
//			P.p("\n\n");
			/** 2. match */
			int falsePositives = 0, falseNegatives = 0;
			Map<String, Map<String, Double>> allAppScores = new HashMap<String, Map<String,Double>>();
			Map<String, String> matchedLabels = new HashMap<String, String>();
			for (String testApp : testApps.keySet()) {
				double maxAppScore = -1.0;
				String matchedLabel = "N/A";
				List<TreeWrapper> appTrees = testApps.get(testApp);
				Map<String, Double> appScores = allAppScores.computeIfAbsent(testApp, k->new HashMap<String, Double>());
				for (String refGroup : refGroups.keySet()) {
					double groupScore = -1.0;
					List<TreeWrapper> refTrees = refGroups.get(refGroup);
					for (TreeWrapper appTree : appTrees)
					for (TreeWrapper refTree : refTrees) {
						double sim1 = appTree.getSim1(refTree, opt);
						groupScore = Math.max(groupScore, sim1);
					}
					appScores.put(refGroup, groupScore);
					if (groupScore > maxAppScore) {
						maxAppScore = groupScore;
						matchedLabel = refGroup;
					}
				}
				matchedLabels.put(testApp, matchedLabel);
				String actualLabel = appLabels.get(testApp);
				if (!actualLabel.equals(matchedLabel)) {
					if (actualLabel.equals("N/A"))
						falsePositives++;
					else
						falseNegatives++;
				}
				//P.pf("%s %s %f %s\n", matchedLabel, appLabels.get(testApp), maxAppScore, testApp);
			}
			P.pf("\tFalse Negatives = %d/%f\n", falseNegatives, (double)falseNegatives/testApps.size());
			P.pf("\tFalse Positives = %d/%f\n", falsePositives, (double)falsePositives/testApps.size());
			/** 3. report */
			int index = i+1;
			File reportDir = new File(reportRootDir, "Result_"+count_ref_apps+"_"+index);
			while (reportDir.exists())
				reportDir = new File(reportRootDir, "Result_"+count_ref_apps+"_"+(++index));
			reportDir.mkdirs();
			// save reference app names
			File refAppNamesFile = new File(reportDir, "Reference Apps.txt"); 
			F.write(refAppNames, refAppNamesFile, false);
			// save raw data
			File rawResultsFile = new File(reportDir, "Raw Results.txt");
			PrintWriter out = F.initPrintWriter(rawResultsFile); 
			List<String> refGroupNames = Reference.getReferenceGroupNames();
			out.print("App Name");
			for (String name : refGroupNames)
				out.printf("\t%s", name);
			out.print("\n");
			for (String app : allAppScores.keySet()) {
				out.print(app);
				Map<String, Double> appScores = allAppScores.get(app);
				for (String group : refGroupNames)
					out.printf("\t%f", appScores.getOrDefault(group, -1.0));
				out.print("\n");
			}
			out.close();
		}
		
		
		//--------------
		t = (System.currentTimeMillis()-t)/1000;
		P.p("Done. Time: " + t + " seconds");
		P.p("\n\n");
		Mailjet.email("cross done "+t+" seconds");
	}
	
	static Set<String> randomSelect(List<String> stegoAppList, int count) {
		List<String> list = new ArrayList<String>(stegoAppList);
		Collections.shuffle(list);
		return new HashSet<>(list.subList(0, count));
	}

	// use a new folder instead of "Medoid_ExpressionTrees"
	// after trimming, there should be much less trees from apps
	static void cleanupExpressionTrees() {
		File rootFile = new File("C:\\workspace\\app_analysis\\notes");
		File medDir = new File(rootFile, "Medoid_ExpressionTrees");
		File trimmedDir = new File(rootFile, "Trimmed_ExpressionTrees");
		int total_old = 0, total_new = 0;
		for (File oldAppDir : medDir.listFiles()) {
			File newAppDir = new File(trimmedDir, oldAppDir.getName());
			newAppDir.mkdirs();
			File[] expFiles = oldAppDir.listFiles();
			File[] newFiles = newAppDir.listFiles();
			int before = expFiles!=null? expFiles.length : 0;
			int after = newFiles!=null? newFiles.length : 0;
			total_old += before;
			total_new += after;
//			if (expFiles != null && expFiles.length>0) {
//				before = expFiles.length;
//				Set<Integer> hashSet = new HashSet<Integer>();
//				for (File expFile : expFiles)
//				if (expFile.getName().endsWith(".expression")) {
//					Expression expression = (Expression) F.readObject(expFile);
//					TreeRefUtils.trim2(expression);
//					if (expression!=null && hashSet.add(expression.toStringRaw().hashCode())) {
//						File newExpFile = new File(newAppDir, expFile.getName());
//						if (!newExpFile.exists())
//							F.writeObject(expression, newExpFile);
//					}
//				}
//				after = hashSet.size();
//			}
			P.pf("%s before/after = %d/%d\n", oldAppDir.getName(), before, after);
		}
		P.pf("total old/new = %d/%d\n", total_old, total_new);
	}
	
	static void warmupMatchingResult() {
		List<File> apks = Dirs.getFiles(Dirs.Stego_Github, Dirs.Stego_Others, Dirs.Stego_PlayStore);
		String appSet = "stegos";
		
		int N = 4;
		boolean horizontal = true;
		boolean PQ = true;
		boolean redo = false;
		Options opt = new Options(appSet, N, horizontal, PQ, redo);
		
		long t = System.currentTimeMillis();
		
		// compare between every single app
		Map<String, List<TreeWrapper>> allAppTrees = Experiment1.getTrees(apks);
		List<String> appList = new ArrayList<String>(allAppTrees.keySet());
		for (int i=0; i<appList.size(); i++)
		for (int j=i+1; j<appList.size(); j++) {
			P.pf("i/j = %d/%d\n", i, j);
			List<TreeWrapper> l1 = allAppTrees.get(appList.get(i));
			List<TreeWrapper> l2 = allAppTrees.get(appList.get(j));
			for (int x=0; x<l1.size(); x++)
			for (int y=0; y<l2.size(); y++)
			{
				TreeWrapper t1 = l1.get(x);
				TreeWrapper t2 = l2.get(y);
				t1.touchSim1(t2, opt);
			}
		}
		t = (System.currentTimeMillis()-t)/1000;
		P.p("Done. Time: " + t + " seconds");
		P.p("\n\n");
		Mailjet.email("cross matching warm up done. time: "+t);
	}

}
