package app_analysis.trees.kMedoid;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.BitmapEncoder.BitmapFormat;
import org.knowm.xchart.QuickChart;
import org.knowm.xchart.XYChart;

import com.clust4j.algo.KMedoidsParameters;

import ui.UI;
import util.F;
import util.Graphviz;
import util.P;

public class KMedoidsExperiment {
	
	
	
	public static void main(String[] args) {		
		//File apk = Template.findAPK("com.akseltorgard.steganography.apk");
		File labelsDirRoot = new File("C:\\workspace\\app_analysis\\notes\\KMedoids");
		String started = P.getTimeString("yyyy/MM/dd HH:mm:ss");
 		Set<File> apks = ETUtil.getTreeDirs().keySet();
		for (File apk : apks) {
			File recordDir = new File(labelsDirRoot, apk.getName());
			recordDir.mkdirs();
			//extractMedoids(apk);
			//int bestK = TestAndEvaluate(apk, recordDir);
			//P.p(apk.getName()+"\t"+bestK);
		}
		P.p("All Done. Started on "+started+". Ended on "+P.getTimeString("yyyy/MM/dd HH:mm:ss"));
		P.p("Total trees: "+treeCount);
	}
	
	
	
	static int app_index = 0;
	static int treeCount = 0;
	@SuppressWarnings({ "unchecked", "rawtypes" })
	static int TestAndEvaluate(File apk, File recordDir) {
		//P.pf("\n---App %d %s\n",++app_index, apk.getName());
		
//		if (elbowF.exists() && silouGraph.exists())
//		{
//			UI.showImage(elbowF, silouGraph);
//			return;
//		}
		List<ET> ets = ETUtil.loadExpressionTrees(apk);
		treeCount += ets.size();
		
		//P.pf("%s\t%d\n", apk.getName(), ets.size());
		
		int maxK = (int)Math.sqrt(ets.size());
		File maxKF = new File(recordDir, "maxK.txt");
		if (maxKF.exists())
			maxK = Integer.parseInt(F.readFirstLine(maxKF));
		
		if (maxK == 1)
			return 1;
		//int maxK = Math.min(ets.size(), 15);
		//P.p("--- Trying k in [1,"+maxK+"]");
		
		// see if we can load SSEs from file
		double[] SSEs = new double[maxK];
		Arrays.fill(SSEs, -1);
		for (int k=1; k<=maxK; k++)
		{
			File sseF = new File(recordDir, "SSE_"+k+".txt");
			if (sseF.exists())
				SSEs[k-1] = Double.parseDouble(F.readFirstLine(sseF));
		}
		double[] meanSilous = new double[maxK];
		Arrays.fill(meanSilous, -1);
		for (int k=1; k<=maxK; k++)
		{
			File meanSilouF = new File(recordDir, "Silouette_"+k+"_mean.txt");
			if (meanSilouF.exists())
				meanSilous[k-1] = Double.parseDouble(F.readFirstLine(meanSilouF));
		}
		double[][] distances = null;
		RealMatrix mat = null;
		for (int k=1; k<=maxK; k++)
		{
			// for each experiment, get 4 things:
			// (1) the kMedoid labels, (2) the cluster graph, (3) the SSE, (4) the Silouettes
			// If all 3 things are already written in file, no need to instantiate Result object
			File resultF = new File(recordDir, "Labels_k"+k+".csv");
			File clusterGraph = new File(recordDir, "Clusters_"+k+".pdf");
			File sseF = new File(recordDir, "SSE_"+k+".txt");
			File silouF = new File(recordDir, "Silouette_"+k+".txt");
			double[] silou = new double[ets.size()];
			if (!resultF.exists() || !clusterGraph.exists() || SSEs[k-1]==0 || !silouF.exists())
			{
				//P.p("  K="+k+" testing...");
				if (distances==null || mat == null)
				{
					distances = ETUtil.getPairWiseDistances(ets);
					mat = MatrixUtils.createRealMatrix(distances);
				}
				Result r = resultF.exists()?new Result(ets, resultF) :
						new Result(new KMedoidsParameters(k).fitNewModel(mat).getLabels(), k);
				SSEs[k-1] = r.getSSE(distances);
				silou = r.getSilouette(distances);
				if (silou == null) // returns null if there just aren't enough clusters
				{
					maxK = k-1;
					continue;
				}
				// 1. save labels to file
				if (!resultF.exists())
					r.saveLabelsToFile(ets, resultF);
				// 2. draw cluster graph
				Graphviz.makeDotGraph(r.toDotGraphString(), clusterGraph.getName().replace(".pdf", ""), recordDir);
				// 3. save SSE to file
				if (!sseF.exists())
					F.writeLine(SSEs[k-1]+"", sseF, false);
				// 4. write silouette vals to file
				if (!silouF.exists())
				{
					PrintWriter out = F.initPrintWriter(silouF);
					for (double d : silou)
						out.println(d);
					out.close();
				}
			}
			else // SSE already loaded, just need to load Silouettes
			{
				//P.p("  K="+k+" already done.");
				int index = 0;
				for (String line: F.readLinesWithoutEmptyLines(silouF))
				if (!line.contentEquals("-"))
					silou[index++] = Double.parseDouble(line);
			}
			
			//draw silouette graph
			if (meanSilous[k-1]==0)
			{
				meanSilous[k-1] = Arrays.stream(silou).average().getAsDouble();
				File meanSilouF = new File(recordDir, "Silouette_"+k+"_mean.txt");
				F.writeLine(meanSilous[k-1]+"", meanSilouF, false);
			}
		}
		F.write(maxK+"", maxKF, false);
		//show elbow graph
		double[] Ks = new double[maxK];
		for (int k=1; k<=maxK; k++)
			Ks[k-1] = k;
		SSEs = Arrays.copyOfRange(SSEs, 0, maxK);
		meanSilous = Arrays.copyOfRange(meanSilous, 0, maxK);
		
		int bestK = findBestK(SSEs, meanSilous);
		
		XYChart xyChart = QuickChart.getChart(apk.getName()+" Total= "+ets.size()+" K=[1,"+maxK+"]", "K", "Sum of Squared Distance", "SSD(k)", Ks, SSEs);
		//new SwingWrapper(xyChart).displayChart();
		
		XYChart xyChart_silou = QuickChart.getChart("Mean Silouette (bestK = "+bestK+")", "K", "Mean Silouette", "ms(k)", Ks, meanSilous);
		//new SwingWrapper(xyChart_silou).displayChart();
		try
		{
			File elbowF = new File(recordDir, "elbow.png");
			File silouGraph = new File(recordDir, "silou.png");
			BitmapEncoder.saveBitmap(xyChart, elbowF.getAbsolutePath(), BitmapFormat.PNG);
			BitmapEncoder.saveBitmap(xyChart_silou, silouGraph.getAbsolutePath(), BitmapFormat.PNG);
			UI.showImage(elbowF, silouGraph);
		} catch (IOException e)
		{
			e.printStackTrace();
		}
		return bestK;
	}
	
	//static int bestKSSE(double[] SSEs) {
		
	//}
	
	//static int bestKSilou(double[] meanSilous) {
		
	//}
	
	// find the best K that satisfied the elbow method and silouette method
	static int findBestK(double[] SSEs, double[] meanSilous) {
		// SSE: best K is the one that achieves highest (SSE[K] - SSE[K-1]) / (SSE[K+1] - SSE[K])
		// Silouette: best K is the one that has highest meanSilous[K]
		
		// first filter out bad Ks: if SSE[k] < SSE[k+1] or SSE[k]>SSE[0]: best K can only be in [1..k]
		int maxBest = SSEs.length-1;
		for (int i=1; i<SSEs.length-1; i++)
		if (SSEs[i] > SSEs[0])
		{
			maxBest = i-1;
			break;
		}
		
		//maxBest = SSEs.length-1;
		double[] ratios = new double[maxBest+1];
		ratios[0] = 1;
		for (int i=1; i<=maxBest; i++) {
			ratios[i] = SSEs[i] / SSEs[0];
		}
		
		double[] delta1 = new double[ratios.length];
		for (int i=1; i<=maxBest; i++) {
			delta1[i] = ratios[i-1] - ratios[i];
		}
		
		double[] delta2 = new double[ratios.length];
		for (int i=2; i<=maxBest; i++) {
			delta2[i] = delta1[i-1] - delta1[i];
		}
		
		double maxStrength = -1;
		int bestK = 0;
		double[] strength = new double[ratios.length];
		for (int i=1; i<maxBest; i++) {
			strength[i] = (delta2[i+1]-delta1[i+1])*meanSilous[i];
			if (strength[i] > 0 && strength[i] > maxStrength) {
				maxStrength = strength[i];
				bestK = i;
			}
		}
		
		// now find the K with highest strength
		if (SSEs[0]-SSEs[1] <= 0.2*(SSEs[bestK]-SSEs[bestK+1]))
			bestK = 0;
		
		return bestK+1;
		
		/*
		// first considers all the non-border Ks
		double[][] scores = new double[SSEs.length][2]; // [0] is score, [1] is K
		for (int i=1; i<maxBest; i++) {
			double deltaLeft = SSEs[i-1]-SSEs[i];
			double deltaRight = SSEs[i] - SSEs[i+1];
			scores[i][0] = deltaLeft - deltaRight;
			scores[i][0] *= meanSilous[i];
			if (deltaRight < 0 && scores[i][0]>0)
				scores[i][0] = -scores[i][0];
			scores[i][1] = i;
		}
		
		Arrays.sort(scores, (s1,s2)->{
			if (s1[0] < s2[0])
				return 1;
			if (s1[0] > s2[0])
				return -1;
			return (int)s1[1] - (int)s2[1];
		});
		
		int bestK = (int)scores[0][1];
		*/
		// then see if K==1 can compete with best K
		
	}
	
	static class Result {
		int k;
		int[] labels;
		Set<Integer> medoids;
		File recordDir;
		
		Result(int[] l, int k)
		{
			labels = l;
			this.k = k;
		}
		
		Result(List<ET> ets, File resultF)
		{
			labels = new int[ets.size()];
			String name = resultF.getName();
			k = Integer.parseInt(name.substring(name.indexOf("k")+1, name.indexOf(".")));
			
			Map<String, Integer> indices = new HashMap<>();
			for (int i=0; i<ets.size(); i++)
				indices.put(ets.get(i).hash, i);
			F.initReader(resultF).lines().forEach(line->{
				String[] parts = line.split(",");
				labels[indices.get(parts[0])] = indices.get(parts[1]);
			});
		}
		
		public double[] getSilouette(double[][] distances)
		{
			// first collect clusters
			List<Integer> clusterNames = new ArrayList<>(); // cluster name is the id of the medoid
			List<Integer> clusterCount = new ArrayList<>();
			Map<Integer, Integer> clusterIndex = new HashMap<>();
			
			for (int i=0; i<labels.length; i++)
			{
				// j is the medoid of the cluster
				int medoid = labels[i];
				if (!clusterIndex.containsKey(medoid)) // first time seeing this cluster
				{
					clusterIndex.put(medoid, clusterIndex.size());
					clusterNames.add(medoid); // use medoid id as the cluster name
					clusterCount.add(0);
				}
				int index = clusterIndex.get(medoid);
				clusterCount.set(index, clusterCount.get(index)+1);
			}
			
			if (clusterNames.size() < k)
				return null;
			
			// then for each data i , calculate mean distance between i and every cluster
			// meanDist[i][j] = mean distance between i and all points in the jth cluster
			double[][] meanDist = new double[labels.length][k];
			for (int i=0; i<labels.length; i++)
			for (int j=0; j<labels.length; j++)
			if (i != j)
			{
				// add the distance between i and j to the total distance of i to j's cluster
				int medoid = labels[j];
				int medoid_index = clusterIndex.get(medoid);
				meanDist[i][medoid_index] += distances[i][j];
			}
			
			for (int i=0; i<labels.length; i++)
			for (int j=0; j<k; j++)
			{
				// now divide the total distance with cluster size
				// use size-1 for data i's own cluster
				int count = clusterCount.get(j);
				if (labels[i] == clusterNames.get(j))
					count--;
				meanDist[i][j] /= count;
			}
			
			double[] silouette = new double[labels.length];
			
			for (int i=0; i<labels.length; i++)
			{
				int cluster_index = clusterIndex.get(labels[i]);
				double a = meanDist[i][cluster_index];
				double b = Double.MAX_VALUE;
				for (int j=0; j<k; j++) if (j!=cluster_index)
					b = Math.min(b, meanDist[i][j]);
				
				silouette[i] = (b-a) / Math.max(a, b);
			}
			
			return silouette;
		}

		
		// Sum of Squared Distance (SSE) between data points and their assigned
		// cluster medoids
		public double getSSE(double[][] distances)
		{
			double sum = 0;
			getMedoids();
			for (int i=0; i<labels.length; i++)
			if (!medoids.contains(i))
			{
				int m = labels[i];
				sum += distances[i][m]*distances[i][m];
			}
			return sum;
		}

		
		// find the medoids from the results. The medoids will have incoming edges
		Set<Integer> getMedoids()
		{
			if (medoids == null)
			{
				medoids = new HashSet<>();
				for (int i=0; i<labels.length; i++)
					if (labels[i] != i)
						medoids.add(labels[i]);
			}
			return medoids;
		}
		
		Set<Integer> getTrueMedoids()
		{
			Set<Integer> true_medoids = new HashSet<>();
			for (int i=0; i<labels.length; i++)
				if (labels[i] == i)
					true_medoids.add(i);
			return true_medoids;
		}
		
		String toDotGraphString()
		{
			String text = "graph G{\n";
			text += "\tnode [shape=circle];\n";
			
			for (int i : getMedoids())
				text += "\t "+i+" [color=red,fontcolor=red];\n";
			
			for (int i=0; i<labels.length; i++)
				if (labels[i] != i)
					text += "\t"+i+" -- "+labels[i]+";\n";
			
			text += "}";
			return text;
		}
		
		void saveLabelsToFile(List<ET> ets, File f)
		{
			PrintWriter out = F.initPrintWriter(f);
			for (int i=0; i<labels.length; i++)
				out.println(ets.get(i).hash+","+ets.get(labels[i]).hash);
			out.close();
		}
	}
	
	static void unitTestSilouette()
	{
		int[] labels = new int[] {
				0,5,0,0,0, 5,5,0,5,5
		};
		int k = 2;
		Random rng = new Random();
		double[][] dist = new double[10][10];
		for (int i=0; i<10; i++)
		for (int j=0; j<10; j++)
		if (i!=j)
		{
			if (dist[j][i] > 0)
				dist[i][j] = dist[j][i];
			else
			{
				boolean iLow = i<5, jLow = j<5;
				dist[i][j] = iLow^jLow? rng.nextInt(3)+10 : rng.nextInt(3)+1;
			}
		}
		
		P.pf("   ");
		for (int i=0; i<10; i++)
			P.pf(" %2d", i);
		P.pf("\n");
		for (int i=0; i<10; i++)
		{
			P.pf("[%d]", i);
			for (int j=0; j<10; j++)
				P.pf(" %2d", (int)dist[i][j]);
			P.pf("\n");
		}
			
		
		Result r = new Result(labels, k);
		double[] silou = r.getSilouette(dist);
		for (int i=0; i<silou.length; i++)
			P.pf("data %d has silou %.2f\n", i, silou[i]);
	}
	
}
