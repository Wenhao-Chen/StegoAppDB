package apex.symbolic.solver;

import java.util.Arrays;

import apex.symbolic.Expression;

public class Logic {

	public static boolean solveConstants = true;
	
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
		if (vA == null)
			return vB.clone();
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
		//NOTE: if vA is in the form of "getPixel() >> [24, 16, 8]" or "getPixel()",
		//      and vB is exactly 0xff:
		//          transform exp into Color.alpha/red/green/blue(getPixel())
		if (vA.related_to_pixel && 
				vB.isLiteral() && !vB.isSymbolic &&
				Arithmetic.parseInt(vB.getLiteralValue()) == 255) {
			// blue channel doesn't need right shift
			if (vA.isReturnValue() && 
					vA.getInvokeSig().contentEquals("Landroid/graphics/Bitmap;->getPixel(II)I")) {
				exp = new Expression("I", "return")
						.add("Landroid/graphics/Color;->blue(I)I")
						.add(vA.clone());
			} else if (vA.root.contentEquals(">>") || vA.root.contentEquals(">>>")) {
				int shifted = vA.pixel_value_shifted;
				Expression pixel = vA.children.get(0);
				if (shifted==0) {
					exp = new Expression("I", "return")
							.add("Landroid/graphics/Color;->blue(I)I")
							.add(pixel.clone());
				} else if (shifted==8) {
					exp = new Expression("I", "return")
							.add("Landroid/graphics/Color;->green(I)I")
							.add(pixel.clone());
				} else if (shifted == 16) {
					exp = new Expression("I", "return")
							.add("Landroid/graphics/Color;->red(I)I")
							.add(pixel.clone());
				} else if (shifted == 24) {
					exp = new Expression("I", "return")
							.add("Landroid/graphics/Color;->alpha(I)I")
							.add(pixel.clone());
				}
			}
		}
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
		if (solveConstants && vA.isLiteral() && !vA.isSymbolic && vB.isLiteral() && !vB.isSymbolic)
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
		//NOTE: detect if this is an operation that tries to separate pixel channels
		if (vA.isReturnValue() && 
			vA.getInvokeSig().contentEquals("Landroid/graphics/Bitmap;->getPixel(II)I") &&
			vB.isLiteral() && !vB.isSymbolic) {
			exp.pixel_value_shifted = Arithmetic.parseInt(vB.getLiteralValue());
		}
		
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
		if (vA.root.contentEquals("return") && 
				vA.children.get(0).root.contentEquals("Landroid/graphics/Bitmap;->getPixel(II)I") &&
				vB.isLiteral() && !vB.isSymbolic) {
				exp.pixel_value_shifted = Arithmetic.parseInt(vB.getLiteralValue());
			}
		return exp;
	}
	
}
