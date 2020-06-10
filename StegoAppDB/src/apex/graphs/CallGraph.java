package apex.graphs;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import apex.APEXApp;
import apex.bytecode_wrappers.APEXClass;
import apex.bytecode_wrappers.APEXMethod;
import apex.bytecode_wrappers.APEXStatement;
import util.Graphviz;

public class CallGraph {

	
	private APEXApp app;
	private boolean whiteListed;

	public static class Vertex {
		public APEXMethod m;
		int index;
		public int in_degree = 0, out_degree = 0;
		Vertex(APEXMethod mm, int i) {m = mm; index = i;}
	}
	
	public static class Edge {
		Vertex from, to;
	}
	
	public Map<String, Vertex> vertices; // key is m.signature
	public Map<Vertex, Set<Vertex>> out_edges;
	public Map<Vertex, Set<Vertex>> in_edges;
	
	
	public CallGraph(APEXApp app)
	{
		this.app = app;
	}
	
	// return the sub graph that contains method "sig"
	public String getDotGraph(String sig)
	{
		return getDotGraph(new HashSet<>(Arrays.asList(sig)));
	}
	
	Map<Vertex, Map<Vertex, Boolean>> connectivity;
	public boolean pathExists(String srcMethod, String dstMethod) {
		Vertex src = vertices.get(srcMethod);
		Vertex dst = vertices.get(dstMethod);
		return pathExists(src, dst);
	}
	
	public Set<APEXMethod> pathExists(Set<APEXMethod> srcs, Set<APEXMethod> dsts) {
		Set<APEXMethod> result = new HashSet<>();
		if (dsts.isEmpty())
			return result;
		Set<Vertex> destinations = new HashSet<>();
		for (APEXMethod m : dsts)
			destinations.add(vertices.get(m.signature));
		
		Map<Vertex, Boolean> dp = new HashMap<>();
		for (APEXMethod src : srcs) {
			if (canReach(dp, vertices.get(src.signature), destinations))
				result.add(src);
		}
		
		return result;
	}
	
	private boolean canReach(Map<Vertex, Boolean> dp, Vertex from, Set<Vertex> dsts) {
		if (dsts.contains(from))
			return true;
		if (dp.containsKey(from))
			return dp.get(from);
		dp.put(from, false); // prevent infinite loop
		if (out_edges.containsKey(from))
		for (Vertex next : out_edges.get(from))
		if (canReach(dp, next, dsts)) {
			dp.put(from, true);
			return true;
		}
		return false;
	}
	
	// find paths between two vertices using bi-directional BFS
	public boolean pathExists(Vertex src, Vertex dst) {
		Set<Vertex> visited = new HashSet<>();
		Set<Vertex> setA = new HashSet<>(); // src set
		Set<Vertex> setB = new HashSet<>(); // dst set
		setA.add(src);
		setB.add(dst);
		boolean checkingSrc = true;
		while (!setA.isEmpty() && !setB.isEmpty()) {
			// always process the smaller queue
			checkingSrc = setA.size()<=setB.size();
			if (checkingSrc) {
				Set<Vertex> todo = new HashSet<>();
				for (Vertex v : setA) {
					if (setB.contains(v))
						return true;
					if (out_edges.containsKey(v))
					for (Vertex next : out_edges.get(v))
					if (visited.add(next)) {
						todo.add(next);
					}
				}
				setA = todo;
			} else {
				Set<Vertex> todo = new HashSet<>();
				for (Vertex v : setB) {
					if (setA.contains(v))
						return true;
					if (in_edges.containsKey(v))
					for (Vertex prev : in_edges.get(v))
					if (visited.add(prev)) {
						todo.add(prev);
					}
				}
				setB = todo;
			}
		}
		return false;
	}
	
	// return the subgraph that contains all methods in set "sigs"
	// and write numbers into the graph nodes
	// and map the numbers into method signatures in the title
	public String getDotGraphSimplified(Set<APEXMethod> from, Set<APEXMethod> to) {
		if (vertices==null || out_edges==null)
			updateMethodCallGraph();
		Set<Vertex> S = new HashSet<>();
		//TODO
		return null;
	}
	
	
	// return the subgraph that contains all methods in set "sigs"
	public String getDotGraph(Set<String> sigs)
	{
		if (vertices==null || out_edges==null)
			updateMethodCallGraph();
		// first find set S that contains all the methods that are connected to methods in "sigs"
		// then write dotgraphs with V and E that are related to methods in S
		
		Set<Vertex> S = new HashSet<>();
		for (String sig : sigs)
			dfs(S, vertices.get(sig));
		
		String text = "digraph G {\n";
		text += "\tcompound=true;\n";
		text += "\tlabelloc=\"t\";\n";
		text += "\tlabel = \"Partial Call Graph for "+app.packageName+" with methods:";
		for (String sig : sigs)
			text += "\\l    "+sig;
		text += "\";\n";
		text += "\tnode [shape=box];\n";
		
		for (Vertex v : S)
		{
			if (sigs.contains(v.m.signature))
				text += "\t"+Graphviz.toDotGraphString(v.index, v.m.signature, "red", "red", null, (String[])null)+"\n";
			else
				text += "\t"+Graphviz.toDotGraphString(v.index, v.m.signature)+"\n";
		}
		
		
		for (Vertex src : S)
		for (Vertex dst : out_edges.getOrDefault(src, new HashSet<>()))
		{
			text += "\t"+src.index+" -> "+dst.index+" [label=\"\"];\n";
		}
		
		text += "}";
		
		return text;
	}
	
	// collect all vertexes that are connected with v
	private void dfs(Set<Vertex> S, Vertex v)
	{
		if (S.contains(v))
			return;
		S.add(v);
		for (Vertex v1 : out_edges.getOrDefault(v, new HashSet<>()))
			dfs(S, v1);
		for (Vertex v1 : in_edges.getOrDefault(v, new HashSet<>()))
			dfs(S, v1);
	}
	
	public String getDotGraph()
	{
		if (vertices==null || out_edges==null)
			updateMethodCallGraph();
		
		
		String text = "digraph G {\n";
		text += "\tcompound=true;\n";
		text += "\tlabelloc=\"t\";\n";
		text += "\tlabel = \""+app.packageName+" Call Graph "+(whiteListed?" (with WhiteList)":"")+"\";\n";
		text += "\tnode [shape=box];\n";
		for (Vertex v : vertices.values())
		{
			text += "\t"+Graphviz.toDotGraphString(v.index, v.m.signature)+"\n";
		}
		for (Vertex src : out_edges.keySet())
		{
			for (Vertex dst : out_edges.get(src))
			{
				text += "\t"+src.index+" -> "+dst.index+" [label=\"\"];\n";
			}
		}
		
		text += "}";
		
		return text;
	}
	
	
	public void updateMethodCallGraph()
	{
		updateMethodCallGraph(null);
	}
	
	public List<Set<Vertex>> countIslands()
	{
		DefaultDirectedGraph<Vertex, DefaultEdge> jgrapht = new DefaultDirectedGraph<Vertex, DefaultEdge>(DefaultEdge.class);
		
		for (Vertex v : vertices.values())
			jgrapht.addVertex(v);
		
		for (Vertex v : out_edges.keySet())
		{
			for (Vertex dst : out_edges.get(v))
			{
				jgrapht.addEdge(v, dst);
			}
		}
		
		ConnectivityInspector<Vertex, DefaultEdge> ci = new ConnectivityInspector<>(jgrapht);
		return ci.connectedSets();
	}
	
	
	public void updateMethodCallGraph(Set<String> whiteList)
	{
		whiteListed = whiteList!=null&&!whiteList.isEmpty();
		int index = 0;
		vertices = new HashMap<>();
		out_edges = new HashMap<>();
		in_edges = new HashMap<>();
		for (APEXClass c : app.getNonLibraryClasses())
		{
			for (APEXMethod m : c.methods.values())
			{
				if (!m.statements.isEmpty() && (whiteList==null || whiteList.contains(m.signature)))
				{
					vertices.putIfAbsent(m.signature, new Vertex(m, index++));
					Vertex src = vertices.get(m.signature);
					for (APEXStatement s : m.statements)
					{
						if (s.isInvokeStmt())
						{
							APEXMethod nestedM = app.getNonLibraryMethod(s.getInvokeSignature());
							if (nestedM!=null && (whiteList==null || whiteList.contains(m.signature)))
							{
								vertices.putIfAbsent(nestedM.signature, new Vertex(nestedM, index++));
								Vertex dst = vertices.get(nestedM.signature);
								
								out_edges.putIfAbsent(src, new HashSet<>());
								out_edges.get(src).add(dst);
								
								in_edges.putIfAbsent(dst, new HashSet<>());
								in_edges.get(dst).add(src);
								
								src.out_degree++;
								dst.in_degree++;
								//P.p("new edge: "+src.m.signature+" to "+dst.m.signature);
							}
						}
					}
				}
			}
		}
	}
	

}
