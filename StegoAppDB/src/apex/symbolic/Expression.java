package apex.symbolic;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import apex.symbolic.solver.Arithmetic;
import util.Dalvik;
import util.Dirs;
import util.Graphviz;
import util.P;

@SuppressWarnings("unused")
public class Expression implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -3930106729253146944L;
	// possible root values:
	// literal 		-- the value is stored in child 0
	// reference 	-- the object name is stored in child 0. Object name is the key in heap
	// return		-- child 0 is the method invoke sig, child 1...n are the Expressions of each invoke param
	// arithmetic operators: [+-*/%] -- child 0 and 1 are the operands
	// logic operators: [&, |, ^, <<, >>, >>>, ~] -- child 0 and 1 are the operands. For the not(~) operation, there's only 1 operand
	public String type, root;
	public List<Expression> children;
	public boolean isSymbolic;
	public boolean related_to_pixel;
	
	public String note;
	
	private Expression() {};
	public Expression(String root)
	{
		this(null, root);
	}
	public Expression(String type, String root)
	{
		this.type = type;
		this.root = root;
		children = new ArrayList<>();
		isSymbolic = false;
		related_to_pixel = false;
	}
	
	public void toDotGraph2()
	{
		List<String> vertices = new ArrayList<>();
		Map<Integer, Set<Integer>> edges = new HashMap<>();
		dfs(vertices, edges, -1, this);
		
		String text = "digraph G{\n";
		text += "\tcompound=true;\n";
		text += "\tnode [shape=box];\n";
		for (int i = 0; i < vertices.size(); i++)
		{
			text += "\t"+Graphviz.toDotGraphString(i, vertices.get(i))+"\n";
		}
		for (int src : edges.keySet())
		{
			for (int dst : edges.get(src))
			{
				text += "\t"+src+" -> "+dst+" [label=\"\"];\n";
			}
		}
		text += "}";
		
		P.p(text);
		P.pause();
		
		Graphviz.makeDotGraph(text, "test", Dirs.Desktop, true);
	}
	
	private void dfs(List<String> vertices, Map<Integer, Set<Integer>> edges, int parent, Expression curr)
	{
		if (curr==null)
			return;
		int index = vertices.size();
		vertices.add(curr.root);
		if (parent!=-1)
		{
			edges.putIfAbsent(parent, new HashSet<>());
			edges.get(parent).add(index);
		}
		for (Expression child : curr.children)
		{
			dfs(vertices, edges, index, child);
		}
	}
	
	public void toDotGraph(String name, File dir, boolean trimmed)
	{
		File dotFile = new File(dir, name+(trimmed?"_timmed":"_full")+".dot");
		File pdfFile = new File(dir, name+(trimmed?"_timmed":"_full")+".pdf");
		if (dotFile.exists() && pdfFile.exists())
			return;
		
		Queue<Expression> queue = new LinkedList<>();
		queue.offer(this);
		
		Map<Expression, Integer> indices = new HashMap<>();
		Map<Integer, List<Integer>> edges = new HashMap<>();
		
		int index = 0;
		while (!queue.isEmpty())
		{
			Expression curr = queue.poll();
			int i = index;
			if (!indices.containsKey(curr))
				indices.put(curr, index++);
			else
				i = indices.get(curr);
			edges.putIfAbsent(i, new ArrayList<>());
			if (trimmed)
			{
				// skip the children when:
				if (curr.root.equals("reference") || curr.root.equals("literal"))
					continue;
			}
			for (Expression child : curr.children)
			{
				queue.add(child);
				if (!indices.containsKey(child))
					indices.put(child, index++);
				edges.get(i).add(index-1);
			}
		}
		
		String text = "digraph G{\n";
		text += "\tcompound=true;\n";
		text += "\tordering=out;\n";
		text += "\tnode [shape=box];\n";
		//text += "\tedge [dir=none];\n";
		for (Expression exp : indices.keySet())
		{
			text += "\t"+Graphviz.toDotGraphString(indices.get(exp), exp.root)+"\n";
		}
		for (int src : edges.keySet())
		{
			List<Integer> dsts = edges.get(src);
			for (int i = 0; i < dsts.size(); i++)
			{
				if (dsts.size()>2)
					text += "\t"+src+" -> "+dsts.get(i)+"[label=\""+(i+1)+"\"];\n";
				else
					text += "\t"+src+" -> "+dsts.get(i)+";\n";
			}
		}
		text += "}";
		
		Graphviz.makeDotGraph(text, name+(trimmed?"_timmed":"_full"), dir, true);
	}
	
	public String toStringRaw()
	{
		StringBuilder res = new StringBuilder();
		res.append('(').append(root);
		for (Expression child : children)
			res.append(child.toStringRaw());
		res.append(')');
		return res.toString();
	}

	public String toString()
	{
		if (children.size()==0)
			return root;
		else if (isLiteral())
		{
			if (isSymbolic)
				return children.get(0).root;
			if (type.contentEquals("I"))
				return Arithmetic.parseInt(children.get(0).root)+"";
			if (type.contentEquals("J"))
				return Arithmetic.parseLong(children.get(0).root)+"";
			if (type.contentEquals("F"))
				return Arithmetic.parseFloat(children.get(0).root)+"";
			if (type.contentEquals("D"))
				return Arithmetic.parseDouble(children.get(0).root)+"";
			return children.get(0).root;
		}
			
		else if (isReference())
			return children.get(0).root;
		else if (isReturnValue())
		{
			String res = children.get(0).toString(); // invokeParam
			String className = Dalvik.DexToJavaName(res.substring(0, res.indexOf("->")));
			if (className.contains("."))
				className = className.substring(className.lastIndexOf(".")+1);
			res = res.substring(res.indexOf("->")+2, res.indexOf("("));
			res = className+"."+res+"(";
			for (int i = 1; i < children.size(); i++)
			{
				res += children.get(i).toString();
				if (i < children.size()-1)
					res += ", ";
			}
			res += ")";
			return res;
		}
		else if (children.size()==1) // special case for "not-<type>" operations
		{
			return root+" "+children.get(0).toString();
		}
		else
		{
			return "("+children.get(0).toString()+" "+root+" "+children.get(1).toString()+")";
		}
	}
	
	public Expression clone()
	{
		Expression exp = new Expression(type, root);
		for (Expression child : children)
			exp.children.add(child.clone());
		exp.isSymbolic = isSymbolic;
		exp.note = note;
		exp.related_to_pixel = related_to_pixel;
		return exp;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		return toString().equals(obj.toString());
		/*if (!(obj instanceof Expression))
			return false;
		Expression exp = (Expression)obj;
		if (!root.equals(exp.root))
			return false;
		if (type==null && exp.type!=null)
			return false;
		if (type!=null && exp.type==null)
			return false;
		if (children.size()!=exp.children.size())
			return false;
		for (int i = 0; i < children.size(); i++)
			if (!children.get(i).equals(exp.children.get(i)))
				return false;
		return true;*/
	}
	
	public String getObjID()
	{
		if (!isReference())
			return null;
		return children.get(0).root;
	}
	
	public String getLiteralValue()
	{
		if (!isLiteral())
			return null;
		return children.get(0).root;
	}

	public void add(String s)
	{
		add(new Expression(s));
	}
	
	public void add(Expression exp)
	{
		children.add(exp);
	}

	public boolean isLiteral()
	{
		return root.equals("literal");
	}	
	public boolean isReference()
	{
		return root.equals("reference");
	}	
	public boolean isReturnValue()
	{
		return root.equals("return");
	}
	private static final String[] operators = {
			"+","-","*","/","%",			//0-4 are arithmetic
			"&","|","^","<<",">>",">>>","~", //5-11 are logical
			"==","!=","<","<=",">",">="		//12-17 are conditions
	};
	public boolean isArithmeticOperation()
	{
		for (int i = 0; i < 5; i++)
			if (root.equals(operators[i]))
				return true;
		return false;
	}
	public boolean isLogicOperation()
	{
		for (int i = 5; i < 12; i++)
			if (root.equals(operators[i]))
				return true;
		return false;
	}
	
	public boolean isCondition()
	{
		for (int i = 12; i<18; i++)
			if (root.equals(operators[i]))
				return true;
		return false;
	}
	
	public static Expression newReference(String objID, String objType)
	{
		Expression exp = new Expression("reference");
		exp.add(objID);
		exp.type = objType;
		return exp;
	}
	
	public static Expression newLiteral(String type, String val)
	{
		Expression exp = new Expression(type, "literal");
		exp.add(val);
		return exp;
	}
	
	public static Expression newCondition(String symbol, Expression left, Expression right)
	{
		Expression exp = new Expression("I", symbol);
		exp.add(left.clone());
		exp.add(right.clone());
		return exp;
	}
}
