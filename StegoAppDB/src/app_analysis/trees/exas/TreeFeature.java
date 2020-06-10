package app_analysis.trees.exas;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import apex.symbolic.Expression;
import app_analysis.common.Dirs;
import util.F;
import util.P;

public class TreeFeature {
	
	static class Options {
		boolean horizontal, pqNode, redo;
		int N;
		String appSet;
		Map<String, Integer> nodeDict, featureDict;
		File nodeDictF, featureDictF;
		private static final String sperator = " , ";
		public Options(String s, int n, boolean h, boolean p, boolean r) {
			appSet = s;
			N = n;
			horizontal = h;
			pqNode = p;
			redo = r;
			nodeDict = new HashMap<>();
			featureDict = new HashMap<>();
			nodeDictF = new File(Dirs.ExasRoot, "NodeDict_"+appSet+".txt");
			featureDictF = new File(Dirs.ExasRoot, "FeatureDict_"+toString()+".txt");
			loadDict();
		}
		public String toString() {
			return appSet+"_"+N+"_"+horizontal+"_"+pqNode;
		}
		
		public Integer lookupNodeVal(String val) {
			return nodeDict.computeIfAbsent(val, k->nodeDict.size());
		}
		
		public void loadDict() {
			if (nodeDictF.exists())
				for (String line : F.readLinesWithoutEmptyLines(nodeDictF)) {
					String[] parts = line.split(sperator);
					nodeDict.put(parts[0], Integer.parseInt(parts[1]));
				}
			
			if (featureDictF.exists())
				for (String line : F.readLinesWithoutEmptyLines(featureDictF)) {
					String[] parts = line.split(sperator);
					featureDict.put(parts[0], Integer.parseInt(parts[1]));
				}
		}
		
		public void saveDict() {
			F.write(nodeDict, sperator, nodeDictF, false);
			F.write(featureDict, sperator, featureDictF, false);
		}
		
		public void updateFeatureDict(Map<String, Integer> features) {
			for (String feature : features.keySet())
				featureDict.putIfAbsent(feature, featureDict.size());
		}
		
		public void printNodeDict() {
			String[] arr = new String[nodeDict.size()];
			for (Map.Entry<String, Integer> entry : nodeDict.entrySet())
				arr[entry.getValue()] = entry.getKey();
			P.p("\n--------------------- node dictionary ---------------------");
			for (int i=0; i<arr.length; i++)
				P.pf("%2d: %s\n", i, arr[i]);
			P.p("-----------------------------------------------------------\n");
		}
	}
	
	public static Map<String, Integer> collectFeatures(TreeWrapper tree, Options opt) {
		Map<String, Integer> features;
		File featureF = new File(tree.recordDir, "Features_"+opt.toString()+".txt");
		if (!opt.redo && featureF.exists()) {
			features = new HashMap<>();
			for (String line : F.readLinesWithoutEmptyLines(featureF)) {
				String[] parts = line.split(" , ");
				features.put(parts[0], Integer.parseInt(parts[1]));
			}
		} else {
			features = collectFeatures(tree.getExpressionTrimmed(), opt);
			F.write(features, " , ", featureF, false);
		}
		return features;
	}
	
	public static Map<String, Integer> collectFeatures(Expression exp, Options opt) {
		Map<String, Integer> features = new HashMap<>();
		if (exp == null)
			return features;
		
		collectNPath(features, exp, opt);
		if (opt.horizontal)
			collectHorizontal(features, exp, opt);
		if (opt.pqNode)
			collectPQ(features, exp, opt);
		return features;
	}
	
	static void collectNPath(Map<String, Integer> features, Expression exp, Options opt) {
		Queue<List<Expression>> q = new LinkedList<>();
		
		// first add all size-1 path to the queue
		Queue<Expression> singles = new LinkedList<>();
		singles.add(exp);
		while (!singles.isEmpty()) {
			Expression e = singles.poll();
			q.add(new ArrayList<>(Arrays.asList(e)));
			for (Expression child : e.children)
				singles.add(child);
		}
		
		// for each path:
		//  record occurrence
		//  then add all the extended path to queue (if path size < N)
		while (!q.isEmpty()) {
			List<Expression> path = q.poll();
			StringBuilder sb = new StringBuilder();
			for (Expression e : path) {
				if (sb.length()>0)
					sb.append("-");
				sb.append(opt.lookupNodeVal(e.root));
			}
			String nPath = sb.toString();
			features.put(nPath, features.getOrDefault(nPath, 0)+1); // record occurrence
			
			if (path.size()<opt.N) {
				Expression e = path.get(path.size()-1);
				for (Expression child : e.children) { // extend the path
					List<Expression> extended = new ArrayList<>(path);
					extended.add(child);
					q.add(extended);
				}
			}
		}
	}
	
	static void collectHorizontal(Map<String, Integer> features, Expression exp, Options opt) {
		
		Queue<Expression> q = new LinkedList<>();
		q.add(exp);
		while (!q.isEmpty()) {
			Expression e = q.poll();
			for (int i=0; i<e.children.size()-1; i++) {
				Expression first = e.children.get(i);
				Expression second = e.children.get(i+1);
				String horizontal = 
						opt.lookupNodeVal(first.root)
						+ "-" +
						opt.lookupNodeVal(second.root);
				features.put(horizontal, features.getOrDefault(horizontal, 0)+1);
			}
			for (Expression child : e.children)
				q.add(child);
		}
	}
	
	static void collectPQ(Map<String, Integer> features, Expression exp, Options opt) {
		Queue<Expression> q = new LinkedList<>();
		q.add(exp);
		int P = 1; // P is always 1 because we are dealing with trees
		while (!q.isEmpty()) {
			Expression e = q.poll();
			int Q = e.children.size();
			String pq = String.format("PQ-%d-%d-%d", 
					opt.lookupNodeVal(e.root),P, Q);
			features.put(pq, features.getOrDefault(pq, 0)+1);
			for (Expression child : e.children)
				q.add(child);
		}
	}
	
}
