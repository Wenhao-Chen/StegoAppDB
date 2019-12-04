package apex.symbolic.solver;

import apex.symbolic.Expression;

public class Logic {

	public static Expression not(Expression vA)
	{
		if (vA.isLiteral() && !vA.isSymbolic)
		{
			String val = vA.children.get(0).root;
			if (vA.type.equals("J"))
				return Expression.newLiteral("J", ~Arithmetic.parseLong(val)+"");
			else
				return Expression.newLiteral("I", ~Arithmetic.parseInt(val)+"");
		}
		Expression exp = new Expression(vA.type, "~");
		exp.add(vA.clone());
		exp.related_to_pixel = vA.related_to_pixel;
		return exp;
	}
	
	public static Expression and(Expression vA, Expression vB)
	{
		if (vA.isLiteral() && !vA.isSymbolic && vB.isLiteral() && !vB.isSymbolic)
		{
			String val1 = vA.children.get(0).root;
			String val2 = vB.children.get(0).root;
			if (vA.type.equals("J"))
				return Expression.newLiteral("J", (Arithmetic.parseLong(val1)&Arithmetic.parseLong(val2))+"");
			else
				return Expression.newLiteral("I", (Arithmetic.parseInt(val1)&Arithmetic.parseInt(val2))+"");
		}
		Expression exp = new Expression(vA.type, "&");
		exp.add(vA.clone());
		exp.add(vB.clone());
		exp.related_to_pixel = vA.related_to_pixel || vB.related_to_pixel;
		return exp;
	}
	
	public static Expression or(Expression vA, Expression vB)
	{
		if (vA.isLiteral() && !vA.isSymbolic && vB.isLiteral() && !vB.isSymbolic)
		{
			String val1 = vA.children.get(0).root;
			String val2 = vB.children.get(0).root;
			if (vA.type.equals("J"))
				return Expression.newLiteral("J", (Arithmetic.parseLong(val1)|Arithmetic.parseLong(val2))+"");
			else
				return Expression.newLiteral("I", (Arithmetic.parseInt(val1)|Arithmetic.parseInt(val2))+"");
		}
		Expression exp = new Expression(vA.type, "|");
		exp.add(vA.clone());
		exp.add(vB.clone());
		exp.related_to_pixel = vA.related_to_pixel || vB.related_to_pixel;
		return exp;
	}
	
	public static Expression xor(Expression vA, Expression vB)
	{
		if (vA.isLiteral() && !vA.isSymbolic && vB.isLiteral() && !vB.isSymbolic)
		{
			String val1 = vA.children.get(0).root;
			String val2 = vB.children.get(0).root;
			if (vA.type.equals("J"))
				return Expression.newLiteral("J", (Arithmetic.parseLong(val1)^Arithmetic.parseLong(val2))+"");
			else
				return Expression.newLiteral("I", (Arithmetic.parseInt(val1)^Arithmetic.parseInt(val2))+"");
		}
		Expression exp = new Expression(vA.type, "^");
		exp.add(vA.clone());
		exp.add(vB.clone());
		exp.related_to_pixel = vA.related_to_pixel || vB.related_to_pixel;
		return exp;
	}
	
	public static Expression shl(Expression vA, Expression vB)
	{
		if (vA.isLiteral() && !vA.isSymbolic && vB.isLiteral() && !vB.isSymbolic)
		{
			String val1 = vA.children.get(0).root;
			String val2 = vB.children.get(0).root;
			if (vA.type.equals("J"))
				return Expression.newLiteral("J", (Arithmetic.parseLong(val1)<<Arithmetic.parseLong(val2))+"");
			else
				return Expression.newLiteral("I", (Arithmetic.parseInt(val1)<<Arithmetic.parseInt(val2))+"");
		}
		Expression exp = new Expression(vA.type, "<<");
		exp.add(vA.clone());
		exp.add(vB.clone());
		exp.related_to_pixel = vA.related_to_pixel || vB.related_to_pixel;
		return exp;
	}
	
	public static Expression shr(Expression vA, Expression vB)
	{
		if (vA.isLiteral() && !vA.isSymbolic && vB.isLiteral() && !vB.isSymbolic)
		{
			String val1 = vA.children.get(0).root;
			String val2 = vB.children.get(0).root;
			if (vA.type.equals("J"))
				return Expression.newLiteral("J", (Arithmetic.parseLong(val1)>>Arithmetic.parseLong(val2))+"");
			else
				return Expression.newLiteral("I", (Arithmetic.parseInt(val1)>>Arithmetic.parseInt(val2))+"");
		}
		Expression exp = new Expression(vA.type, ">>");
		exp.add(vA.clone());
		exp.add(vB.clone());
		exp.related_to_pixel = vA.related_to_pixel || vB.related_to_pixel;
		return exp;
	}
	
	public static Expression ushr(Expression vA, Expression vB)
	{
		if (vA.isLiteral() && !vA.isSymbolic && vB.isLiteral() && !vB.isSymbolic)
		{
			String val1 = vA.children.get(0).root;
			String val2 = vB.children.get(0).root;
			if (vA.type.equals("J"))
				return Expression.newLiteral("J", (Arithmetic.parseLong(val1)>>>Arithmetic.parseLong(val2))+"");
			else
				return Expression.newLiteral("I", (Arithmetic.parseInt(val1)>>>Arithmetic.parseInt(val2))+"");
		}
		Expression exp = new Expression(vA.type, ">>>");
		exp.add(vA.clone());
		exp.add(vB.clone());
		exp.related_to_pixel = vA.related_to_pixel || vB.related_to_pixel;
		return exp;
	}
	
}
