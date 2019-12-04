package apex.symbolic.solver;

import apex.symbolic.Expression;

public class Condition {

	
	// result = (0 if b==c) or (1 if b>c) or (-1 if b<c)
	public static Expression cmp(Expression vA, Expression vB, String type)
	{
		int cmp = compare(vA, vB, type);
		if (cmp != Undetermined)
			return Expression.newLiteral("I", cmp+"");
		
		Expression exp = new Expression("I","cmp_cond");
		exp.add(vA.clone());
		exp.add(vB.clone());
		return exp;
	}
	
	public static final int Greater = 1;
	public static final int Less = -1;
	public static final int Equal = 0;
	public static final int Undetermined = 2;
	
	public static int compare(Expression vA, Expression vB, String type)
	{
		int val = Undetermined;
		try
		{
			if (vA.isLiteral()&&!vA.isSymbolic && vB.isLiteral() && !vB.isSymbolic)
			{
				String val1 = vA.children.get(0).root;
				String val2 = vB.children.get(0).root;
				if (type.equals("F"))
				{
					float f1 = Arithmetic.parseFloat(val1);
					float f2 = Arithmetic.parseFloat(val2);
					val = f1==f2?Equal:(f1>f2?Greater:Less);
				}
				else if (type.equals("D"))
				{
					double d1 = Arithmetic.parseDouble(val1);
					double d2 = Arithmetic.parseDouble(val2);
					val = d1==d2?Equal:(d1>d2?Greater:Less);
				}
				else if (type.equals("J"))
				{
					long l1 = Arithmetic.parseLong(val1);
					long l2 = Arithmetic.parseLong(val2);
					val = l1==l2?Equal:(l1>l2?Greater:Less);
				}
				else if (type.equals("I"))
				{
					int i1 = Arithmetic.parseInt(val1);
					int i2 = Arithmetic.parseInt(val2);
					val = i1==i2?Equal:(i1>i2?Greater:Less);
				}
			}
			return val;
		}
		catch (Exception e)
		{
			return Undetermined;
		}
	}
	
	
}
