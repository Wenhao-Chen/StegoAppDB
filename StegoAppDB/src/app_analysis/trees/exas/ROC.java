package app_analysis.trees.exas;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import app_analysis.common.Dirs;
import app_analysis.trees.exas.TreeFeature.Options;
import app_analysis.trees.exas.raw_results.App;
import util.F;
import util.P;

public class ROC {

	static final File pyF = new File("C:\\workspace\\app_analysis\\roc_pr.py");
	
	public static void draw(
			Options opt, 
			Map<String, Map<String, Double>> allAppScores, 
			Map<String, Double> maxAppScores,
			Set<String> refGroups) {
		
		List<String> testApps = new ArrayList<>(allAppScores.keySet());
		int N = testApps.size();
		int[] truth = new int[N];
		for (int i=0; i<N; i++)
			truth[i] = App.isStego(testApps.get(i))?1:0;
		
		// try overall max score for each app
		double[] overall_score = new double[N];
		for (int i=0; i<N; i++)
			overall_score[i] = (1+maxAppScores.get(testApps.get(i)))/2;
		
		File scoreF = new File(Dirs.Desktop, opt.toString()+"_scores.txt");
		F.write(overall_score, scoreF, false);
		File truthF = new File(Dirs.Desktop, opt.toString()+"_truth.txt");
		F.write(truth, truthF, false);
		P.exec("py "+
				pyF.getAbsolutePath()+" roc "+
				scoreF.getAbsolutePath()+" "+
				truthF.getAbsolutePath(), false);
		P.exec("py "+
				pyF.getAbsolutePath()+" pr "+
				scoreF.getAbsolutePath()+" "+
				truthF.getAbsolutePath(), false);
	}
	
	
//	public static void draw2(
//			Options opt, 
//			Map<String, Map<String, Double>> allAppScores, 
//			Map<String, Double> maxAppScores,
//			Set<String> refGroups) {
//		
//		List<String> testApps = new ArrayList<>(allAppScores.keySet());
//		int N = testApps.size();
//		boolean[] truth = new boolean[N];
//		for (int i=0; i<N; i++)
//			truth[i] = App.isStego(testApps.get(i));
//		
//		// try overall max score for each app
//		double[] overall_score = new double[N];
//		for (int i=0; i<N; i++)
//			overall_score[i] = (1+maxAppScores.get(testApps.get(i)))/2;
//		
//		Roc roc = new Roc(overall_score, truth);
//		//P.p("AUC: "+roc.computeAUC());
//		File output = new File(Dirs.Desktop, opt.toString()+".png");
//		List<RocCoordinates> roc_coordinates = roc.computeRocPointsAndGenerateCurve(output.getAbsolutePath());
//	}
	
	
	
}
