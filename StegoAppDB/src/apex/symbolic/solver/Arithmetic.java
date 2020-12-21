package apex.symbolic.solver;

import apex.symbolic.Expression;

public class Arithmetic {

	// if 'lazy' set to true, every arithmetic operation will be a formula
	// even if both operands are literal values. For Example:
	// if v0 is literal value 0x1, v1 is literal value 0x2
	// add v0, v1 in lazy mode returns: add 0x1 0x1, while not-lazy mode returns: 0x2
	public static boolean lazy = false;
	
	public static Expression add(Expression vA, Expression vB, String type) {
		vA.type = type;
		vB.type = type;
		
		if (!lazy && vA.isLiteral() && !vA.isSymbolic && vB.isLiteral() && !vB.isSymbolic) {
			String val1 = vA.children.get(0).root;
			String val2 = vB.children.get(0).root;
			switch (type) {
			case "I":
				return Expression.newLiteral(type, (parseInt(val1)+parseInt(val2))+"");
			case "F":
				return Expression.newLiteral(type, (parseFloat(val1)+parseFloat(val2))+"");
			case "J":
				return Expression.newLiteral(type, (parseLong(val1)+parseLong(val2))+"");
			case "D":
				return Expression.newLiteral(type, (parseDouble(val1)+parseDouble(val2))+"");
			}
		}
		Expression exp = new Expression(type, "+");
		exp.add(vA.clone());
		exp.add(vB.clone());
		exp.related_to_pixel = vA.related_to_pixel || vB.related_to_pixel;
		return exp;
	}
	
	public static Expression sub(Expression vA, Expression vB, String type) {
		vA.type = type;
		vB.type = type;
		
		if (!lazy && vA.isLiteral() && !vA.isSymbolic && vB.isLiteral() && !vB.isSymbolic)
		{
			String val1 = vA.children.get(0).root;
			String val2 = vB.children.get(0).root;
			switch (type)
			{
			case "I":
				return Expression.newLiteral(type, (parseInt(val1)-parseInt(val2))+"");
			case "F":
				return Expression.newLiteral(type, (parseFloat(val1)-parseFloat(val2))+"");
			case "J":
				return Expression.newLiteral(type, (parseLong(val1)-parseLong(val2))+"");
			case "D":
				return Expression.newLiteral(type, (parseDouble(val1)-parseDouble(val2))+"");
			}
		}
		Expression exp = new Expression(type, "-");
		exp.add(vA.clone());
		exp.add(vB.clone());
		exp.related_to_pixel = vA.related_to_pixel || vB.related_to_pixel;
		return exp;
	}
	
	public static Expression mul(Expression vA, Expression vB, String type)
	{
		vA.type = type;
		vB.type = type;
		
		if (!lazy && vA.isLiteral() && !vA.isSymbolic && vB.isLiteral() && !vB.isSymbolic)
		{
			String val1 = vA.children.get(0).root;
			String val2 = vB.children.get(0).root;
			switch (type)
			{
			case "I":
				return Expression.newLiteral(type, (parseInt(val1)*parseInt(val2))+"");
			case "F":
				return Expression.newLiteral(type, (parseFloat(val1)*parseFloat(val2))+"");
			case "J":
				return Expression.newLiteral(type, (parseLong(val1)*parseLong(val2))+"");
			case "D":
				return Expression.newLiteral(type, (parseDouble(val1)*parseDouble(val2))+"");
			}
		}
		Expression exp = new Expression(type, "*");
		exp.add(vA.clone());
		exp.add(vB.clone());
		exp.related_to_pixel = vA.related_to_pixel || vB.related_to_pixel;
		return exp;
	}
	
	public static Expression div(Expression vA, Expression vB, String type)
	{
		vA.type = type;
		vB.type = type;
		
		if (!lazy && vA.isLiteral() && !vA.isSymbolic && vB.isLiteral() && !vB.isSymbolic)
		{
			String val1 = vA.children.get(0).root;
			String val2 = vB.children.get(0).root;
			if (val2.equals("0") || val2.equals("0x0"))
				return null;
			switch (type)
			{
			case "I":
				return Expression.newLiteral(type, (parseInt(val1)/parseInt(val2))+"");
			case "F":
				return Expression.newLiteral(type, (parseFloat(val1)/parseFloat(val2))+"");
			case "J":
				return Expression.newLiteral(type, (parseLong(val1)/parseLong(val2))+"");
			case "D":
				return Expression.newLiteral(type, (parseDouble(val1)/parseDouble(val2))+"");
			}
		}
		Expression exp = new Expression(type, "/");
		exp.add(vA.clone());
		exp.add(vB.clone());
		exp.related_to_pixel = vA.related_to_pixel || vB.related_to_pixel;
		return exp;
	}
	
	public static Expression rem(Expression vA, Expression vB, String type)
	{
		vA.type = type;
		vB.type = type;
		
		if (!lazy && vA.isLiteral() && !vA.isSymbolic && vB.isLiteral() && !vB.isSymbolic)
		{
			String val1 = vA.children.get(0).root;
			String val2 = vB.children.get(0).root;
			if (val2.equals("0") || val2.equals("0x0"))
				return null;
			switch (type)
			{
			case "I":
				return Expression.newLiteral(type, (parseInt(val1)%parseInt(val2))+"");
			case "F":
				return Expression.newLiteral(type, (parseFloat(val1)%parseFloat(val2))+"");
			case "J":
				return Expression.newLiteral(type, (parseLong(val1)%parseLong(val2))+"");
			case "D":
				return Expression.newLiteral(type, (parseDouble(val1)%parseDouble(val2))+"");
			}
		}
		
		Expression exp = new Expression(type, "%");
		exp.add(vA.clone());
		exp.add(vB.clone());
		exp.related_to_pixel = vA.related_to_pixel || vB.related_to_pixel;
		
		if (Arithmetic.parseInt(vB)==2 && vA.related_to_pixel) {
			boolean foundPixel = false;
			int bitIndexFromRight = 0;
			Expression temp = vA.clone();
			// see if we can find out if this value is in the form of
			// getPixel() % 2, getPixel() / 2 % 2, ...
			// if it is, find out exactly which bit is the value representing
			while (temp != null && !foundPixel) {
				if (temp.isReturnValue() && temp.getInvokeSig().equals("Landroid/graphics/Bitmap;->getPixel(II)I"))
					foundPixel = true;
				else if (temp.root.equals("/")) {
					Expression l = temp.children.get(0);
					Expression r = temp.children.get(1);
					if (Arithmetic.parseInt(r)==2) {
						bitIndexFromRight++;
						temp = l;
					} else
						break;
				}
				else
					break;
			}
			
			if (foundPixel) {
				exp.bitIndexFromRight = bitIndexFromRight;
				exp.pixelExp = temp.clone();
				
//				P.p(vA.toString());
//				P.p("-- index = " + bitIndexFromRight);
//				Expression res = new Expression("I", "&");
//				if (bitIndexFromRight==0)
//					res.add(temp.clone());
//				else {
//					Expression left = new Expression(">>");
//					left.add(temp.clone()).add(bitIndexFromRight+"");
//					res.add(left);
//				}
//				res.add(Expression.newLiteral("I", "0x1"));
//				res.isBinaryBitOfImageData = true;
//				exp = res;
//				exp.toDotGraph("name1212"+new Random().nextInt(10000), Dirs.Desktop, false);
//				P.pause(exp.toString());
//				P.pause();
			}
		}
		
		
		return exp;
	}
	
	public static Expression cast(Expression vB, String from, String to)
	{
		Expression vA = vB.clone();
		vA.related_to_pixel = vB.related_to_pixel;
		vB.type = from;
		vA.type = to;
		if (vB.isSymbolic || !vB.isLiteral())
		{
			return vA;
		}
		String val = vB.children.get(0).root;
		if (from.equals("I"))
		{
			if (to.equals("B"))
				vA.children.get(0).root = (byte)parseInt(val)+"";
			else if (to.equals("C"))
				vA.children.get(0).root = (char)parseInt(val)+"";
			else if (to.equals("S"))
				vA.children.get(0).root = (short)parseInt(val)+"";
			else
				vA.children.get(0).root = parseInt(val)+"";
		}
		else if (from.equals("F"))
		{
			float f = parseFloat(val);
			if (to.equals("I"))
				vA.children.get(0).root = (int)f+"";
			else if (to.equals("J"))
				vA.children.get(0).root = (long)f+"";
			else if (to.equals("D"))
				vA.children.get(0).root = (double)f+"";
		}
		else if (from.equals("J"))
		{
			long l = parseLong(val);
			if (to.equals("I"))
				vA.children.get(0).root = (int)l+"";
			else if (to.equals("F"))
				vA.children.get(0).root = (float)l+"";
			else if (to.equals("D"))
				vA.children.get(0).root = (double)l+"";
		}
		else if (from.equals("D"))
		{
			double d = parseDouble(val);
			if (to.equals("I"))
				vA.children.get(0).root = (int)d+"";
			else if (to.equals("F"))
				vA.children.get(0).root = (float)d+"";
			else if (to.equals("J"))
				vA.children.get(0).root = (double)d+"";
		}
		return vA;
	}
	
	public static int parseInt(Expression exp) {
		return parseInt(exp.toString());
	}
	
	public static int parseInt(String val)
	{
		try
		{
			if (val.startsWith("0x"))
				return Integer.parseInt(val.substring(2), 16);
			if (val.startsWith("-0x"))
				return Integer.parseInt("-"+val.substring(3), 16);
			if (val.contains("."))
			{
				return (int)Float.parseFloat(val.replace("f", ""));
			}
			return Integer.parseInt(val, 10);
		}
		catch (Exception e)
		{
			return 0;
		}
	}
	
	public static long parseLong(String val)
	{
		if (val.contains("0x"))
			return Long.parseLong(val.replace("0x", "").replace("L", ""), 16);
		if (val.contains(".")||val.contains("E"))
			return (long)Double.parseDouble(val);
		return Long.parseLong(val.replace("L", ""), 10);
	}
	
	public static float parseFloat(String val)
	{
		if (val.equals("NaN"))
			return Float.NaN;
		if (val.equals("Float.MAX_VALUE"))
			return Float.MAX_VALUE;
		if (val.equals("Float.MIN_VALUE"))
			return Float.MIN_VALUE;
		if (val.equals("Infinity"))
			return Float.POSITIVE_INFINITY;
		if (val.equals("Float.POSITIVE_INFINITY"))
			return Float.POSITIVE_INFINITY;
		if (val.equals("Float.NEGATIVE_INFINITY"))
			return Float.NEGATIVE_INFINITY;
		if (val.equals("-Infinity"))
			return Float.NEGATIVE_INFINITY;
		if (val.equals("(float)Math.PI"))
			return (float)Math.PI;
		if (val.equals("Float.NaN"))
			return Float.NaN;
		if (val.contains("."))
			return Float.parseFloat(val.replace("f", ""));
		if (val.startsWith("0x") || val.startsWith("-0x"))
		{
			Long l = Long.parseLong(val.replace("0x", ""), 16);
			return Float.intBitsToFloat(l.intValue());
		}
		for (char c : val.toLowerCase().toCharArray())
		{
			if (c>='a'&&c<='f')
			{
				Long l = Long.parseLong(val.replace("0x", ""), 16);
				return Float.intBitsToFloat(l.intValue());
			}
		}
		
		return Float.parseFloat(val.replace("f", ""));
	}
	
	public static double parseDouble(String val)
	{
		try {
			if (val.equals("Double.MAX_VALUE"))
				return Double.MAX_VALUE;
			if (val.equals("Double.MIN_VALUE"))
				return Double.MIN_VALUE;
			if (val.contentEquals("Double.POSITIVE_INFINITY"))
				return Double.MAX_VALUE;
			if (val.equals("Math.PI"))
				return Math.PI;
			if (val.contentEquals("Double.NaN"))
				return Double.NaN;
			else return Double.parseDouble(val.replace("0x", ""));
		}
		catch (Exception e) {
			return Double.NaN;
		}
	}
}
