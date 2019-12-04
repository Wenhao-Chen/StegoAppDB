package apex.graphs;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import apex.APEXApp;
import apex.code_wrappers.APEXClass;
import apex.code_wrappers.APEXMethod;
import apex.code_wrappers.APEXStatement;
import util.Graphviz;

public class CallGraph {

	
	private APEXApp app;
	private boolean whiteListed;

	public static class Vertex {
		APEXMethod m;
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
	
	public int countIslands()
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
		return ci.connectedSets().size();
	}
	
	private void dfs(Set<String> visited, String tovisit)
	{
		visited.add(tovisit);
		Vertex v = vertices.get(tovisit);
		if (out_edges.containsKey(v))
		{
			Set<Vertex> out = out_edges.get(v);
			for (Vertex vv : out)
			{
				if (!visited.contains(vv))
					dfs(visited, vv.m.signature);
			}
		}
		if (in_edges.containsKey(v))
		{
			for (Vertex vv : in_edges.get(v))
			{
				if (!visited.contains(vv))
					dfs(visited, vv.m.signature);
			}
		}
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
