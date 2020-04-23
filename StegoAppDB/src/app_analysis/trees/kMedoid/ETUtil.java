package app_analysis.trees.kMedoid;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import apex.symbolic.Expression;
import app_analysis.old.Template;
import ui.ProgressUI;
import util.F;

public class ETUtil {

	
	static Map<String, String> apiHash;
	static File hashF = new File("C:\\workspace\\app_analysis\\notes\\MethodHash");
	static final String treeRootDir = "C:\\workspace\\app_analysis\\notes\\ExpressionTrees";
	
	static ProgressUI ui = ProgressUI.create("ExpressionTreeUtil");
	
	public static double[][] getPairWiseDistances(List<ET> ets)
	{
		int n = ets.size();
		double[][] data = new double[n][n];

		for (int i=0; i<n; i++)
		for (int j=0; j<n; j++)
		{
			ui.newLine(String.format("Calcing distance between %d and %d (total %d)\n", i, j, n));
			data[i][j] = ets.get(i).distance(ets.get(j));
		}

		return data;
	}
	
	
	// Function to load all the expression tree files into a list of ET Objects.
	// If the app doesn't have usable expression trees, an empty list is returned
	static Map<File, File> treeDirs;
	public static List<ET> loadExpressionTrees(File apk)
	{
		if (treeDirs == null)
			treeDirs = getTreeDirs();
		
		List<ET> list = new ArrayList<>();
		File treeDir = treeDirs.get(apk);
		if (treeDir == null)
			return list;
		
		for (File expF : treeDir.listFiles())
			if (expF.getName().endsWith(".expression") && expF.length()<=10000)
				list.add(new ET(expF));
		
		return list;
	}
	
	// Load a hash map of <APK_File_Path, App_Tree_Dir>
	// This function only loads apps that are confirmed to have usable expression trees
	public static Map<File, File> getTreeDirs()
	{
		String tree_record_dir = "C:\\Users\\C03223-Stego2\\git\\StegoAppDB\\StegoAppDB\\src\\app_analysis\\trees";
		Map<File, File> map = new HashMap<>();
		
		File recordF = new File(tree_record_dir, "apps_with_good_trees");
		if (recordF.exists())
		{
			for (String apkPath : F.readLinesWithoutEmptyLines(recordF))
			{
				File apk = new File(apkPath);
				map.put(apk, new File(treeRootDir, apk.getName()));
			}
		}
		else
		{
			File appNamesF = new File(tree_record_dir, "stego_app_names");
			Set<String> appNames = new HashSet<>(F.readLinesWithoutEmptyLines(appNamesF));
			List<String> apkPaths = new ArrayList<>();
			for (File apk : Template.getStegoAPKs())
			if (appNames.contains(apk.getName()))
			{
				File dir1 = new File(treeRootDir, apk.getName());
				int count = 0;
				for (File expF : dir1.listFiles())
				if (expF.getName().endsWith(".expression"))
					count++;
				if (count > 0)
				{
					map.put(apk,  dir1);
					apkPaths.add(apk.getAbsolutePath());
				}
			}
			F.write(apkPaths, recordF, false);
		}
		return map;
	}
	
	
	// Function to create a hash string for a method signature
	//   A method signature will get a hash value that is unique - different from the hash values of any other method sigs
	//   The hash value is also consistent - the same method signature always get the same hash value
	// The hash function is a simple index-hash because we don't need to encode a lot of method sigs:
	//	 The hash value is determined by the index of this method sig, in a First-come-first-serve basis
	//	 The index is then encoded to a custom encoded string to limit its character count
	public static String getMethodHash(String sig)
	{
		if (apiHash == null)
		{
			apiHash = new HashMap<>();
			if (hashF.exists())
			for (String line : F.readLinesWithoutEmptyLines(hashF))
			{
				String[] parts = line.split(",");
				apiHash.put(parts[1], parts[0]);
			}
		}
		
		if (!apiHash.containsKey(sig))
		{
			String hash = encodeNumber(apiHash.size());
			apiHash.put(sig, hash);
			F.writeLine(hash+","+sig, hashF, true);
		}
		return apiHash.get(sig);
	}
	
	// Function to encode a base-10 number using a custom encoding function
	//   The goal is to keep the encoded number as few characters as possible
	//   The dictionary skips three characters "LRM" because
	//   they are preserved keywords for "literal", "reference", "method return"
	static String dict = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKNOPQSTUVWXYZ";
	public static String encodeNumber(int num)
	{
		if (num == 0)
			return "0";
		int base = dict.length();
		String res = "";
		while (num > 0)
		{
			res = dict.charAt(num%base) + res;
			num /= base;
		}
		return res;
	}
	
	// Function to convert our custom encoded number to base-10
	public static int toBase10(String hash)
	{
		int base = dict.length();
		int res = 0;
		for (char c : hash.toCharArray())
		{
			res = res*base + dict.indexOf(c);
		}
		return res;
	}
	
	// this is designed for super-trimmed expression only
	// The input expression must be already super-trimmed
	public static String ExpressionToString(Expression exp)
	{
		if (exp == null)
			return "";
		String res = "";
		// for method invoke, use pre-order form
		if (exp.root.contentEquals("M"))
		{
			res += exp.root+"(";
			for (Expression child : exp.children)
				res += ExpressionToString(child);
			res += ")";
		}
		else if (exp.root.contentEquals("L") || exp.root.contentEquals("R") || 
				exp.children.isEmpty())
		{
			res = exp.root;
		}
		// all other nodes have 2 children
		else
		{
			res = ExpressionToString(exp.children.get(0))+exp.root+
					ExpressionToString(exp.children.get(1));
		}
		return res;
	}
	
	// this is designed for super-trimmed expression only
	// The two input expression must both be already super-trimmed
	public static int EditDistance_Trimmed(Expression exp1, Expression exp2)
	{
		String s1 = ExpressionToString(exp1);
		String s2 = ExpressionToString(exp2);
		return minDistance(s1, s2);
	}
	
	// this is designed for FULL expression only
	// The input expression must not have been super-trimmed
	public static int EditDistance_Full(Expression e1, Expression e2)
	{
		return EditDistance_Trimmed(trim(e1), trim(e2));
	}
	
	// this is designed for FULL expression only
	// The input expression must not have been super-trimmed
	public static Expression trim(Expression exp)
	{
		if (exp == null)
			return null;
		
		if (exp.root.contentEquals("literal"))
			return new Expression("L");
		if (exp.root.contentEquals("reference"))
			return new Expression("R");
		if (exp.root.contentEquals("return"))
		{
			Expression res = new Expression("M");
			res.add(ETUtil.getMethodHash(exp.children.get(0).root));
			for (int i=1; i<exp.children.size(); i++)
				res.add(trim(exp.children.get(i)));
			return res;
		}
		
		Expression res = new Expression(exp.root);
		for (Expression child : exp.children)
			res.add(trim(child));
		return res;
	}
	
	
	// returns the minimum edit distance between two strings
    public static int minDistance(String word1, String word2) {
        int[][] distances = new int[word1.length()][word2.length()];
        for (int[] row: distances) {
            Arrays.fill(row, -1);
        }
        return minDistanceHelper(word1, word1.length()-1, 
                                 word2, word2.length()-1, distances);
    }
    
    // helper function for "int minDistance(String, String)"
    private static int minDistanceHelper(String w1, int index1, 
                                  String w2, int index2, int[][] distances) {
        if (index1 < 0) {
            return index2 + 1;
        } 
        
        if (index2 < 0) {
            return index1 + 1;
        }
        
        if(distances[index1][index2] != -1) {
            return distances[index1][index2];
        }
        if(w1.charAt(index1) == w2.charAt(index2))
            return distances[index1][index2] = minDistanceHelper(w1, index1-1, w2, index2-1, distances);
        
        int replaceLast = minDistanceHelper(w1, index1-1, w2, index2-1, distances);
        int addLast = minDistanceHelper(w1, index1, w2, index2-1, distances);
        int delLast = minDistanceHelper(w1, index1-1, w2, index2, distances);
        return distances[index1][index2] = 1 + Math.min(replaceLast, Math.min(addLast, delLast));
    }
}
