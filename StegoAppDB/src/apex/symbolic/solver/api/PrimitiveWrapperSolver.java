package apex.symbolic.solver.api;

import java.util.List;

import apex.code_wrappers.APEXStatement;
import apex.symbolic.APEXObject;
import apex.symbolic.Expression;
import apex.symbolic.MethodContext;
import apex.symbolic.VM;
import apex.symbolic.solver.Arithmetic;
import util.P;

public class PrimitiveWrapperSolver extends SolverInterface {

	@Override
	public void solve(String invokeSig, List<Expression> params, APEXStatement s, MethodContext mc, String[] paramRegs, VM vm)
	{
		if (invokeSig.equals("Ljava/lang/Integer;->parseInt(Ljava/lang/String;)I"))
		{
			Expression s1 = params.get(0);
/*			P.p("parseint");
			APISolver.printStringInfo(s1, vm);
			APEXObject obj = vm.heap.get("$obj_3");
			P.p("--- obj3");
			obj.print();
			P.p("--- obj2");
			vm.heap.get("$obj_2").print();
			P.pause();*/
			if (s1.isLiteral())
			{
				try
				{
					int val = Integer.parseInt(s1.toString());
					vm.recentResult = Expression.newLiteral("I", val+"");
				}
				catch (Exception e)
				{
					vm.crashed = true;
					vm.shouldStop = true;
				}
			}
			else
				vm.createSymbolicMethodReturn("I", invokeSig, params, s);
		}
		else if (invokeSig.equals("Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;"))
		{
			Expression i = params.get(0);
			APEXObject obj = vm.createNewObject("Ljava/lang/Integer;", "wrapper", s.getUniqueID(), i.isSymbolic);
			obj.primitiveExpr = i.clone();
			vm.recentResult = obj.reference;
		}
		else if (invokeSig.equals("Ljava/lang/Integer;->intValue()I"))
		{
			APEXObject obj = vm.heap.get(params.get(0).getObjID());
			if (obj.primitiveExpr != null)
				vm.recentResult = obj.primitiveExpr;
			else
				vm.createSymbolicMethodReturn("I", invokeSig, params, s);
		}
		else if (invokeSig.equals("Ljava/lang/Boolean;->valueOf(Z)Ljava/lang/Boolean;"))
		{
			Expression i = params.get(0);
			APEXObject obj = vm.createNewObject("Ljava/lang/Boolean;", "wrapper", s.getUniqueID(), i.isSymbolic);
			obj.primitiveExpr = i.clone();
			vm.recentResult = obj.reference;
		}
		else if (invokeSig.equals("Ljava/lang/Boolean;->booleanValue()Z"))
		{
			APEXObject obj = vm.heap.get(params.get(0).getObjID());
			if (obj.primitiveExpr != null)
				vm.recentResult = obj.primitiveExpr;
			else
				vm.createSymbolicMethodReturn("I", invokeSig, params, s);
		}
		else if (invokeSig.equals("Ljava/lang/Math;->ceil(D)D"))
		{
			Expression d = params.get(0);
			if (d.isLiteral() && !d.isSymbolic)
			{
				vm.recentResult = Expression.newLiteral("D", ""+Math.ceil(Arithmetic.parseDouble(d.toString())));
			}
			else
				vm.createSymbolicMethodReturn("D", invokeSig, params, s);
		}
		else
		{
			P.p("Forgot about this API??? " + invokeSig);
			P.pause();
		}
	}
	
	public static String getPrimitiveName(String wrapperName)
	{
		switch (wrapperName)
		{
		case "Ljava/lang/Integer;":
			return "I";
		case "Ljava/lang/Float;":
			return "F";
		case "Ljava/lang/Double;":
			return "D";
		case "Ljava/lang/Long;":
			return "J";
		case "Ljava/lang/Boolean;":
			return "Z";
		case "Ljava/lang/Byte;":
			return "B";
		case "Ljava/lang/Short;":
			return "S";
		case "Ljava/lang/Character;":
			return "C";
		}
		return "";
	}

}
