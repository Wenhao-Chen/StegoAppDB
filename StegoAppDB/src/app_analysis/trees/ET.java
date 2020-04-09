package app_analysis.trees;

import java.io.BufferedReader;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import apex.symbolic.Expression;
import ui.ProgressUI;
import util.F;

public class ET {

	public static final String distanceRootDir = "C:\\workspace\\app_analysis\\notes\\TreeEditDistances";
	File expressionF, fullPDF, trimmedPDF, distanceF;
	String hash;
	String name;
	private Expression exp;
	private Map<String, Integer> distances;
	
	
	// Given an expression tree Object file, find the corresponding dotgraph files automatically
	ET(File expressionF)
	{
		this.expressionF = expressionF;
		name = expressionF.getName();
		name = name.substring(0, name.lastIndexOf("."));
		File treeDir = expressionF.getParentFile();
		fullPDF = new File(treeDir, name+"_full.pdf");
		trimmedPDF = new File(treeDir, name+"_trimmed.pdf");
		File distanceDir = new File(distanceRootDir, treeDir.getName());
		distanceDir.mkdirs();
		distanceF = new File(distanceDir, name+".distance");
		hash = treeDir.getName()+"_"+name;
	}
	
	// Load the expression file if needed, and return the Expression Object
	public Expression getExpression()
	{
		if (exp == null)
			exp = (Expression) F.readObject(expressionF.getAbsolutePath());
		return exp;
	}
	
	// Load the distance records if needed, and return the Distance Map
	public Map<String, Integer> getDistances()
	{
		if (distances == null)
		{
			distances = new HashMap<>();
			if (distanceF.exists())
			{
				try
				{
					BufferedReader in = F.initReader(distanceF);
					String line;
					while ((line=in.readLine())!=null)
					{
						String[] parts = line.split(",");
						distances.put(parts[0], Integer.parseInt(parts[1]));
					}
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		}
		return distances;
	}
	
	// Returns the distance between this Expression Tree and the given Expression Tree
	// The distance only needs to be calculated once
	public int distance(ET et)
	{
		if (et == this)
			return 0;
		getDistances(); // init the distance map
		if (!distances.containsKey(et.hash))
		{
			int dist = ETUtil.EditDistance_Full(this.getExpression(), et.getExpression());
			// update distance in both ways
			this.updateDistance(et.hash, dist);
			et.updateDistance(this.hash, dist);
			//F.writeLine(et.hash+","+dist, distanceF, true);
		}
		return distances.get(et.hash);
	}
	
	// Update the distance between this tree and the other tree (with hash 'hash')
	// If this is a newly calculated distance, save it to file too
	public void updateDistance(String hash, int dist)
	{
		getDistances();
		distances.computeIfAbsent(hash, k->{
			F.writeLine(k+","+dist, distanceF, true);
			return dist;
		});
	}

	
	
}
