package apex.graphs;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import apex.APEXApp;
import apex.code_wrappers.APEXBlock;
import apex.code_wrappers.APEXMethod;
import apex.code_wrappers.APEXStatement;
import util.Graphviz;

public class ControlFlowGraph {

	public static void main(String[] args)
	{
		File apk = new File("C:\\Users\\C03223-Stego2\\Desktop\\stego\\other_apps\\original_apks\\Camopic Lite_v1.1.1_apkpure.com.apk");
		String methodSig = 
				" Lca/repl/camopic/a/d;->a(ILjava/io/InputStream;Lca/repl/camopic/a/e;)V";
		
		APEXApp app = new APEXApp(apk);
		Graphviz.makeCFG(app, methodSig);
	}
	
	static class Vertex{
		int index;
		APEXBlock block;
		Vertex(APEXBlock b, int i) {index = i; block = b;}
	}
	
	static class Edge {
		Vertex from, to;
		String type;
		Edge(Vertex v1, Vertex v2, String s) {from = v1; to = v2; type = s;}
	}
	
	private Map<String, Vertex> vertices;
	private Map<Vertex, Set<Edge>> edges;
	private APEXApp app;
	private APEXMethod m;
	
	public ControlFlowGraph(APEXApp app, APEXMethod m)
	{
		vertices = new HashMap<>();
		edges = new HashMap<>();
		this.app = app;
		this.m = m;
		int index = 0;
		for (APEXBlock b : m.blocks)
		{
			vertices.put(b.getLabelString(), new Vertex(b, index++));
			//P.p(b.getLabelString()+" "+ b.subBlockIndex+"  "+b.statements.get(0).index+" "+b.statements.get(0).smali);
		}
		
		for (Vertex v : vertices.values())
		{
			APEXStatement s = v.block.getLastStatement();
			if (s.isGotoStmt())
			{
				addEdge(v, s.getGotoTargetStmt(), "");
			}
			else if (s.isIfStmt())
			{
				addEdge(v, s.getFlowThroughStmt(), "if_flow");
				addEdge(v, s.getIfJumpTargetStmt(), "if_jump");
			}
			else if (s.isSwitchStmt())
			{
				addEdge(v, s.getFlowThroughStmt(), "switch_flow");
				for (String label : s.getSwitchMap().values())
				{
					addEdge(v, m.getBlock(label).getFirstStatement(), "switch_jump");
				}
			}
			else if (!s.isReturnStmt() && !s.isThrowStmt())
			{
				addEdge(v, s.getFlowThroughStmt(), "");
			}
		}
	}
	
	private void addEdge(Vertex from, APEXStatement s, String type)
	{
		edges.putIfAbsent(from, new HashSet<>());
		edges.get(from).add(new Edge(from, vertices.get(s.block.getLabelString()), type));
	}
	
	public String getDotGraphString()
	{
		return getDotGraphString(new String[]{});
	}
	
	public String getDotGraphString(String... keywords)
	{
		String text = "digraph G {";
		text += "\tcompound=true;\n";
		text += "\tlabelloc=\"t\";\n";
		text += "\tlabel = \""+ String.join(" ", m.modifiers)+" "+m.signature+" (CFG)\";\n";
		text += "\tnode [shape=box];\n";
		text += "\t-1 [ label=\"start\" shape=ellipse color=green];\n";
		text += "\t-2 [ label=\"end\" shape=ellipse color=green];\n";
		// declare vertices
		for (Vertex v : vertices.values())
		{
			String label = v.block.toDotGraphString();
			text += "\t"+v.index+" [label=\""+v.block.toDotGraphString()+"\"";
			if (keywords != null)
			{
				for (String keyword : keywords)
				{
					if (label.contains(keyword))
					{
						text += " color=red fontcolor=red";
						break;
					}
				}
			}
			text += "];\n";
		}
		// declare edges
		text += "\t-1 -> 0 [label=\"\" color=green];\n"; // start edge: -1 to 0
		for (Vertex src : edges.keySet())
		{
			for (Edge edge : edges.get(src))
			{
				text += "\t"+edge.from.index+" -> "+edge.to.index+" [label=\""+edge.type+"\"];\n";
			}
		}
		// add return and throw edges to 
		for (Vertex src : vertices.values())
		{
			APEXStatement s = src.block.getLastStatement();
			if (s.isReturnStmt())
			{
				text += "\t"+src.index+" -> -2 [label=\"\" color=green];\n";
			}
			else if (s.isThrowStmt())
			{
				text += "\t"+src.index+" -> -2 [label=\"\" color=red];\n";
			}
		}
		text += "}\n";
		return text;
	}

}
