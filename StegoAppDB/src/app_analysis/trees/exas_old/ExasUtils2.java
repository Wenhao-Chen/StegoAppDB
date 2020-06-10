package app_analysis.trees.exas_old;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import apex.symbolic.Expression;

public class ExasUtils2 {

	
	public static double sim_1_norm(Expression exp1, String refRoot, int[] ref_vectors, Map<String, Integer> dict,
			Map<String, Integer> featureDict, int nPath, boolean doPQNode, boolean doHorizontal) {
		
		double score = -1.0;
		if (exp1.root.equals(refRoot)) {
			Map<String, Integer> exas = new HashMap<>();
			collectFeatures(exas, exp1, dict, nPath, doPQNode, doHorizontal);
			int[] vectors = generateVector(featureDict, exas);
			score = sim_1_norm(vectors, ref_vectors);
		}
		
		for (Expression child : exp1.children)
			score = Math.max(score, sim_1_norm(child, refRoot, ref_vectors, dict, featureDict, nPath, doPQNode, doHorizontal));
		
		return score;
	}
	
	public static double sim_2_norm(Expression exp1, String refRoot, int[] ref_vectors, Map<String, Integer> dict,
			Map<String, Integer> featureDict, int nPath, boolean doPQNode, boolean doHorizontal) {
		
		double score = -1.0;
		if (exp1.root.equals(refRoot)) {
			Map<String, Integer> exas = new HashMap<>();
			collectFeatures(exas, exp1, dict, nPath, doPQNode, doHorizontal);
			int[] vectors = generateVector(featureDict, exas);
			score = sim_2_norm(vectors, ref_vectors);
		}
		
		for (Expression child : exp1.children)
			score = Math.max(score, sim_2_norm(child, refRoot, ref_vectors, dict, featureDict, nPath, doPQNode, doHorizontal));
		
		return score;
	}
	
	public static double sim_max_norm(Expression exp1, String refRoot, int[] ref_vectors, Map<String, Integer> dict,
			Map<String, Integer> featureDict, int nPath, boolean doPQNode, boolean doHorizontal) {
		
		double score = -1.0;
		if (exp1.root.equals(refRoot)) {
			Map<String, Integer> exas = new HashMap<>();
			collectFeatures(exas, exp1, dict, nPath, doPQNode, doHorizontal);
			int[] vectors = generateVector(featureDict, exas);
			score = sim_max_norm(vectors, ref_vectors);
		}
		
		for (Expression child : exp1.children)
			score = Math.max(score, sim_max_norm(child, refRoot, ref_vectors, dict, featureDict, nPath, doPQNode, doHorizontal));
		
		return score;
	}
	
	public static void collectFeatures(Map<String, Integer> exas, Expression exp, Map<String, Integer> dict,
				int nPath, boolean doPQNode, boolean doHorizontal) {
		ExasUtils2.collectNPath(exas, dict, exp, nPath);
		if (doPQNode)
			ExasUtils2.collectPQNode(exas, dict, exp);
		if (doHorizontal)
			ExasUtils2.collectHorizontal2Path(exas, dict, exp);
	}
	
	public static void collectNPath(Map<String, Integer> exas, Map<String, Integer> dict, 
			Expression root, int N) {
		if (root == null)
			return;
		Queue<List<Expression>> q = new LinkedList<>();
		// add every single node to q
		Queue<Expression> q0 = new LinkedList<>();
		q0.add(root);
		while (!q0.isEmpty()) {
			Expression exp = q0.poll();
			q.add(new ArrayList<>(Arrays.asList(exp)));
			for (Expression child : exp.children)
				q0.add(child);
		}
		while (!q.isEmpty()) {
			// count occurrence of current path
			List<Expression> list = q.poll();
			StringBuilder sb = new StringBuilder();
			for (Expression exp : list) {
				if (sb.length()>0)
					sb.append("-");
				sb.append(dict.get(exp.root));
			}
			String key = sb.toString();
			exas.put(key, exas.getOrDefault(key, 0)+1);
			// expand if possible
			if (list.size()<N) {
				Expression last = list.get(list.size()-1);
				for (Expression child : last.children) {
					List<Expression> newList = new ArrayList<>(list);
					newList.add(child);
					q.add(newList);
				}
			}
		}
	}
	
	// this (p,q)-node feature might be redundant in our AST:
	// p is always 1 for every node because our tree structure
	// q is fixed for every node except "return" where it can have multiple children
	public static void collectPQNode(Map<String, Integer> exas, Map<String, Integer> dict, Expression root) {
		if (root == null)
			return;
		int P = 1, Q;
		Queue<Expression> q = new LinkedList<>();
		q.add(root);
		while (!q.isEmpty()) {
			Expression exp = q.poll();
			Q = exp.children.size();
			String pq = "PQ"+dict.get(exp.root)+"-"+P+"-"+Q;
			exas.put(pq, exas.getOrDefault(pq, 0)+1);
		}
	}
	
	public static void collectHorizontal2Path(Map<String, Integer> exas, Map<String, Integer> dict, Expression root) {
		if (root == null)
			return;
		Queue<Expression> q = new LinkedList<>();
		q.add(root);
		while (!q.isEmpty()) {
			Expression exp = q.poll();
			if (exp.children.size()>1) {
				for (int i=1; i<exp.children.size(); i++) {
					Expression left = exp.children.get(i-1);
					Expression right = exp.children.get(i);
					String key = dict.get(left.root)+"-"+dict.get(right.root);
					exas.put(key, exas.getOrDefault(key, 0)+1);
				}
			}
			for (Expression child : exp.children)
				q.add(child);
		}
	}
	
	public static int[] generateVector(Map<String, Integer> featureDict, Map<String, Integer> features) {
		int[] vector = new int[featureDict.size()];
		for (Map.Entry<String, Integer> entry : features.entrySet()) {
			if (!featureDict.containsKey(entry.getKey())) {
				continue;
//				P.p("key not found: "+ entry.getKey());
//				P.pause();
			}
			vector[featureDict.get(entry.getKey())] = entry.getValue();
		}
			
		return vector;
	}
	
	public static int[] subtract(int[] v1, int[] v2) {
		if (v1.length != v2.length)
			return null;
		int[] v = new int[v1.length];
		for (int i=0; i<v.length; i++)
			v[i] = v1[i] - v2[i];
		return v;
	}
	
	public static double sim_1_norm(int[] v1, int[] v2) {
		int[] v12 = subtract(v1, v2);
		return 1.0 - 2.0*L1_norm(v12) / (L1_norm(v1) + L1_norm(v2));
	}
	
	public static double sim_2_norm(int[] v1, int[] v2) {
		int[] v12 = subtract(v1, v2);
		return 1.0 - 2.0*L2_norm(v12) / (L2_norm(v1) + L2_norm(v2));
	}
	
	public static double sim_max_norm(int[] v1, int[] v2) {
		int[] v12 = subtract(v1, v2);
		return 1.0 - 2.0*Lmax_norm(v12) / (Lmax_norm(v1)+Lmax_norm(v2));
	}
	
	public static double Lmax_norm(int[] v) {
		if (v == null)
			return 0;
		int max = 0;
		for (int i : v)
			max = Math.max(max, Math.abs(i));
		return max;
	}
	
	public static double L1_norm(int[] v) {
		if (v == null)
			return 0;
		double sum = 0;
		for (int i : v)
			sum += Math.abs(i);
		return sum;
	}
	
	public static double L2_norm(int[] v) {
		if (v == null)
			return 0;
		double sum = 0;
		for (int i : v)
			sum += i*i;
		return Math.sqrt(sum);
	}

	
}
