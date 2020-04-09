package app_analysis.old;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import apex.APEXApp;
import apex.symbolic.Expression;
import ui.ProgressUI;
import util.F;
import util.Graphviz;
import util.P;

public class TreeEditDistance {

	static ProgressUI expressionUI = ProgressUI.create("Expressions", 20);
	static ProgressUI timeUI = ProgressUI.create("Time", 20);
	static int thresh = 100;
	
	static Map<String, String> code;
	
	public static void main(String[] args)
	{
		String method = "Lcom/qitvision/cryptego/GridActivity$NewPasswordCreation;->doInBackground([Ljava/lang/Void;)Ljava/lang/Boolean;";
		
		File stegaisApp = new File("C:/workspace/app_analysis/apks/stego/com.qitvision.cryptego.apk");
		Graphviz.defaultKeywords = new String[] {"v23"};
		APEXApp app = new APEXApp(stegaisApp);
		Graphviz.makeCFG(app, method, true);
		
		P.p("Done.");
		P.pause();
		code = new HashMap<>();
		File tmpDir = new File("C:/workspace/temp");
		tmpDir.mkdirs();

		String root = "C:/workspace/app_analysis/graphs/expression";
		
		
		Set<String> skip = new HashSet<>(Arrays.asList(
				"ca.repl.free.camopic.apk",
				"com.fruiz500.passlok.apk",
				"com.romancinkais.stegais.apk",
				"info.guardianproject.pixelknot.apk",
				"com.paranoiaworks.unicus.android.sse.apk",
				"sk.panacom.stegos.apk"
				));
		
		Map<String, Object[]> exps = new HashMap<>();
		
		P.p("Distance threhold: "+thresh+"\n");
		File[] appDirs = new File(root).listFiles();
		int count = 0;
		for(File appDir : appDirs) if(!skip.contains(appDir.getName()))
		{
			File clusterDir = new File(appDir, "clusters_reps");
			clusterDir.mkdirs();
			//[0]: List<String> expNames = new ArrayList<>();
			//[1]: Integer[][] distances;
			List<String> expNames = new ArrayList<>();
			Integer[][] distances = loadData(appDir, expNames, false);
			exps.put(appDir.getName(), new Object[] {expNames, distances});
			if (expNames.size()==0)
				continue;
			//P.p(expNames.size()+"");
			//P.p("------------ "+appDir.getName());
			//examineDistances(distances);
			//P.p("--\n\n");
			Set<Set<Integer>> clusters = groupDistances(appDir, expNames, distances);
			P.p(++count+"\t"+appDir.getName()+"\t"+distances.length+"\t"+clusters.size());
			
			int i = 0, total = clusters.size();
			for (Set<Integer> cluster : clusters)
			{
				// find the center of the cluster???
				int minSum = Integer.MAX_VALUE, minEx = -1;
				for (int ex1 : cluster)
				{
					int sum = 0;
					for (int ex2 : cluster)
						sum += distances[ex1][ex2];
					if (sum < minSum)
					{
						minSum = sum;
						minEx = ex1;
					}
				}
				P.p("  "+(++i)+"/"+total+"  center exp: "+expNames.get(minEx));
				String[] suffixes = {"_super_trimmed_full","_full","_timmed"};
				for (String suffix:suffixes)
				{
					File f = new File(appDir, expNames.get(minEx).replace(".expression", suffix+".pdf"));
					if (!f.exists())
					{
						P.p("Not exist!!\n +"+f.getAbsolutePath());
						P.pause();
					}
					else
					{
						File copyto = new File(clusterDir, f.getName());
						if (!copyto.exists())
							F.copy(f, copyto);
					}
				}
			}
/*			
			P.p("names count: "+expNames.size());
			P.p("==\n"+expNames.get(0)+"\n===");
			int[] arr = {196,181, 213};
			for (int index : arr)
			{
				File f = new File(appDir, expNames.get(index));
				Expression exp = (Expression) F.readObject(f.getAbsolutePath());
				trim(exp);
				exp.toDotGraph("temp_"+index, tmpDir, false);
			}*/
			
		}
		
		expressionUI.newLine("Finished loading all expressions");
/*		Object[] objs = exps.get("dev.nia.niastego.apk");
		List<String> expNames = (List<String>) objs[0];
		Integer[][] distances = (Integer[][]) objs[1];
		int n = expNames.size();
		for (int i = 0; i < n; i++)
		{
			for (int j=0; j<n;j++)
			{
				System.out.print(" "+distances[i][j]);
			}
			System.out.print("\n");
		}
		P.p("\n\n");
		examineDistances(distances);*/
		
/*		P.p("=== All Done. Method count: "+code.size());
		for (String m : code.keySet())
		{
			P.p("  "+m+"\t=\t"+code.get(m));
		}*/
		P.p("All Done.");
	}
	
	static Set<Set<Integer>> groupDistances(File appDir, List<String> exps, Integer[][] distances)
	{
		int n = distances.length;
		Map<Integer, Set<Integer>> map = new HashMap<>();
		for(int i=0; i<n; i++) for(int j=i+1; j<n; j++)
		{
			int d = distances[i][j];
			map.putIfAbsent(i, new HashSet<>(Arrays.asList(i)));
			map.putIfAbsent(j, new HashSet<>(Arrays.asList(j)));
			
			if (d < thresh)
			{
				Set<Integer> set = new HashSet<>();
				// i and j share the same set
				set.addAll(map.get(i));
				set.addAll(map.get(j));
				map.put(i, set);
				map.put(j, set);
			}
		}
		
		Set<Set<Integer>> clusters = new HashSet<>(map.values());
		//P.p(""+clusters.size());
		for (Set<Integer> cluster : clusters)
		{
			for(int i:cluster) for(int j:cluster) if(i!=j && distances[i][j]>thresh*0.8)
			{
				//P.p("check "+appDir.getName()+" "+exps.get(i)+" vs "+exps.get(j));
				//P.pause();
			}
		}
		return clusters;
	}
	
	static void examineDistances(Integer[][] distances)
	{
		int n = distances.length;
		Map<Integer, Integer> occur = new HashMap<>();
		for (int i=0; i<n; i++) for(int j=i+1; j<n; j++)
		{
			int d = distances[i][j];
			occur.put(d, occur.getOrDefault(d, 0)+1);
		}
		if (occur.size()==0)
			return;
		PriorityQueue<int[]> q = new PriorityQueue<>(occur.size(), (d1, d2)->{
				return d2[1]-d1[1];});
		
		for (int d : occur.keySet())
		{
			q.offer(new int[] {d, occur.get(d)});
		}
		while (!q.isEmpty())
		{
			int[] arr = q.poll();
			P.p(arr[0]+"  "+arr[1]);
		}
	}
	
	static Integer[][] loadData(File appDir, List<String> expNames, boolean redo)
	{
		String appName = appDir.getName();
		File expNameF = new File(appDir, appName+"_expression_names.txt");
		File resultF = new File(appDir, appName+"_distances.txt");
		List<Expression> appExps = new ArrayList<>();
		Integer[][] distances;
		if (!redo && expNameF.exists() && resultF.exists() && resultF.length()>0)
		{
			// load edit distances from file
			expressionUI.newLine("Already has distances for app "+appName+". Skipping it...");
			expNames.addAll(F.readLinesWithoutEmptyLines(expNameF));
			int n = expNames.size();
			distances = new Integer[n][n];
			List<String> prevResults = F.readLinesWithoutEmptyLines(resultF);
			if (prevResults.size()!=n)
			{
				P.e(appName+" numbers don't match: "+prevResults.size()+" rows of results but "+n+" expressions.");
				P.pause();
			}
			for (int i=0; i<n; i++)
			{
				String[] parts = prevResults.get(i).trim().split(" ");
				int j = 0;
				for (String s : parts) if (!s.isEmpty())
					distances[i][j++] = Integer.parseInt(s);
				if (j!=n)
				{
					P.e(appName+" numbers dont match: row count = "+n+", col = "+j);
					P.pause();
				}
			}
		}
		else
		{
			long time = System.currentTimeMillis();
			PrintWriter outNames = F.initPrintWriter(expNameF);
			// first load expression objects
			for(File f : appDir.listFiles()) if(f.getName().endsWith(".expression"))
			{
				outNames.println(f.getName());
				expNames.add(f.getName());
				expressionUI.newLine("loading expression "+f.getAbsolutePath());
				Expression exp = (Expression) F.readObject(f.getAbsolutePath());
				//exp.toDotGraph("full", tmpDir, false);
				trim(exp);
				String name = f.getName().replace(".expression", "_super_trimmed");
				exp.toDotGraph(name, appDir, false);
				//exp.toDotGraph("trimmed", tmpDir, false);
				appExps.add(exp);
			}
			outNames.close();
			time = (System.currentTimeMillis()-time)/1000;
			timeUI.newLine(String.format("[%s] load expressions: %d seconds", appName, time));
			int n = appExps.size();
			distances = new Integer[n][n];
			time = System.currentTimeMillis();
			// then calculate edit distances
			for(int i=0; i<n; i++) for(int j=0; j<n; j++)
			{
				if (i==j) { distances[i][j] = 0; continue; }
				if (distances[i][j]!=null)
					continue;
				expressionUI.newLine("calculating dist between "+i+" and "+j);
				String s1 = toString(appExps.get(i));
				//P.p("Example:\n"+s1);
				//P.pause();
				String s2 = toString(appExps.get(j));
				distances[i][j] = distances[j][i] = editDistDP(s1, s2);
			}
			time = (System.currentTimeMillis()-time)/1000;
			timeUI.newLine(String.format("[%s] calculating edit distances: %d seconds", appName, time));
			
			// save edit distances to file
			PrintWriter outDistances = F.initPrintWriter(resultF);
			for(int i=0; i<n; i++)
			{
				 for(int j=0; j<n; j++)
					 outDistances.printf(" %3d", distances[i][j]);
				 outDistances.print("\n");
			}
			outDistances.close();
		}
		//P.p("------- "+appName+ "has "+expNames.size()+" expressions -------");
		return distances;
	}
	
	static void trim(Expression e)
	{
		if (e == null)
			return;
		if (e.root.equals("literal") || e.root.equals("reference"))
			e.children.clear();
		if (e.root.equals("literal"))
			e.root = "l";
		else if (e.root.equals("reference"))
			e.root = "r";
		else if (e.root.equals("return"))
			e.root = "R";
		if (e.root.contains(";->"))
		{
			code.putIfAbsent(e.root, "M"+Integer.toHexString(code.size()));
			e.root = code.get(e.root);
		}
		for (Expression child : e.children)
			trim(child);
	}
	
	static String toString(Expression exp)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("(");
		sb.append(exp.root);

		for (Expression child : exp.children)
			sb.append(" "+toString(child));
		sb.append(")");
		return sb.toString();
	}
	
    static int min(int x,int y,int z) 
    { 
        if (x <= y && x <= z) return x; 
        if (y <= x && y <= z) return y; 
        else return z; 
    }
    
  
    static int editDistDP(String str1, String str2) 
    { 
    	int m = str1.length(), n = str2.length();
        // Create a table to store results of subproblems 
        int dp[][] = new int[m+1][n+1]; 
       
        // Fill d[][] in bottom up manner 
        for (int i=0; i<=m; i++) 
        { 
            for (int j=0; j<=n; j++) 
            { 
                // If first string is empty, only option is to 
                // insert all characters of second string 
                if (i==0) 
                    dp[i][j] = j;  // Min. operations = j 
       
                // If second string is empty, only option is to 
                // remove all characters of second string 
                else if (j==0) 
                    dp[i][j] = i; // Min. operations = i 
       
                // If last characters are same, ignore last char 
                // and recur for remaining string 
                else if (str1.charAt(i-1) == str2.charAt(j-1)) 
                    dp[i][j] = dp[i-1][j-1]; 
       
                // If the last character is different, consider all 
                // possibilities and find the minimum 
                else
                    dp[i][j] = 1 + min(dp[i][j-1],  // Insert 
                                       dp[i-1][j],  // Remove 
                                       dp[i-1][j-1]); // Replace 
            } 
        } 
   
        return dp[m][n]; 
    } 
}
