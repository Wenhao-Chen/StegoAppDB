package apex.symbolic.solver.api;

import java.util.List;

import apex.bytecode_wrappers.APEXStatement;
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
			if (s1.isLiteral() && !s1.isSymbolic)
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
			else {
//				P.p("-parse int: "+s1.toString());
//				APEXObject obj = vm.heap.get(s1.getObjID());
//				obj.print();
//				//P.pause();
				Expression strExp = params.get(0);
				if (strExp.isBinaryBitOfImageData) {
					vm.recentResult = strExp.clone();
//					P.p("--- parse int:");
//					P.p(vm.recentResult.toString());
//					P.pause();
				}
				else
					vm.createSymbolicMethodReturn("I", invokeSig, params, s);
			}
		}
		else if (invokeSig.equals("Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;"))
		{
			Expression i = params.get(0);
			APEXObject obj = vm.createNewObject("Ljava/lang/Integer;", "wrapper", s.getUniqueID(), i.isSymbolic);
			obj.primitiveExpr = i.clone();
			vm.recentResult = obj.reference;
		}
		else if (invokeSig.contentEquals("Ljava/lang/Integer;->valueOf(Ljava/lang/String;I)Ljava/lang/Integer;") || 
				 invokeSig.contentEquals("Ljava/lang/Integer;->valueOf(Ljava/lang/String;)Ljava/lang/Integer;")) {
			vm.createSymbolicMethodReturn("Ljava/lang/Integer;", invokeSig, params, s);
			Expression strExp = params.get(0);
//			P.p("-value of "+strExp.toString());
//			P.p(s.getUniqueID());
//			P.pause();
			if (strExp.isReference() && vm.heap.get(strExp.getObjID()).imageDataExp!=null) {
				APEXObject obj = vm.heap.get(vm.recentResult.getObjID());
				obj.primitiveExpr = vm.heap.get(strExp.getObjID()).imageDataExp.clone();
			}
		}
		else if (invokeSig.equals("Ljava/lang/Integer;->intValue()I"))
		{
			if (params.get(0)==null || params.get(0).getObjID()==null)
			{
				vm.crashed = vm.shouldStop = true;
				return;
			}
			APEXObject obj = vm.heap.get(params.get(0).getObjID());
//			if (s.index==127) {
//				P.p("prim: "+(obj.primitiveExpr != null));
//				if (obj.primitiveExpr!=null)
//					P.p(obj.primitiveExpr.toString());
//			}
			if (obj.primitiveExpr != null) {
				vm.recentResult = obj.primitiveExpr.clone();
			}
			else
				vm.createSymbolicMethodReturn("I", invokeSig, params, s);
		}
		else if (invokeSig.contentEquals("Ljava/lang/Integer;->toBinaryString(I)Ljava/lang/String;")) {
			Expression intExp = params.get(0);
			if (intExp.isLiteral() && !intExp.isSymbolic) {
				try
				{
					String str = Integer.toBinaryString(Arithmetic.parseInt(intExp.getLiteralValue()));
					vm.recentResult = Expression.newLiteral("Ljava/lang/String;", str);
				}
				catch (Exception e)
				{
					vm.crashed = true;
					vm.shouldStop = true;
				}
			}
			else {
				vm.createSymbolicMethodReturn("Ljava/lang/String;", invokeSig, params, s);
				String expS = intExp.toString();
				// if the string is the binary string of an image data, remember the image data expression
				if (expS.contains("Bitmap.getPixel") || expS.contains("Bitmap;->getPixel")) {
					APEXObject str = vm.heap.get(vm.recentResult.getObjID());
					str.imageDataExp = intExp.clone();
					
					//P.p("- binary string: "+params.get(0).toString());
//					P.p(s.getUniqueID());
//					P.pause();
				}
			}
			
			
		}
		else if (invokeSig.equals("Ljava/lang/Long;->valueOf(J)Ljava/lang/Long;"))
		{
			Expression i = params.get(0);
			APEXObject obj = vm.createNewObject("Ljava/lang/Long;", "wrapper", s.getUniqueID(), i.isSymbolic);
			obj.primitiveExpr = i.clone();
			vm.recentResult = obj.reference;
		}
		else if (invokeSig.equals("Ljava/lang/Long;->intValue()I")) {
			if (params.get(0)==null || params.get(0).getObjID()==null)
			{
				vm.crashed = vm.shouldStop = true;
				return;
			}
			APEXObject obj = vm.heap.get(params.get(0).getObjID());
			if (obj.primitiveExpr != null) {
				vm.recentResult = obj.primitiveExpr.clone();
				vm.recentResult.type = "I";
			}
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
			if (params.get(0).getObjID() == null) {
				vm.crashed = vm.shouldStop = true;
				return;
			}
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
		else if (invokeSig.equals("Ljava/lang/Math;->floor(D)D"))
		{
			Expression d = params.get(0);
			if (d.isLiteral() && !d.isSymbolic)
			{
				vm.recentResult = Expression.newLiteral("D", ""+Math.floor(Arithmetic.parseDouble(d.toString())));
			}
			else
				vm.createSymbolicMethodReturn("D", invokeSig, params, s);
		}
		else if (invokeSig.equals("Ljava/lang/Math;->round(D)J"))
		{
			Expression d = params.get(0);
			if (d.isLiteral() && !d.isSymbolic)
			{
				vm.recentResult = Expression.newLiteral("D", ""+Math.round(Arithmetic.parseDouble(d.toString())));
			}
			else {
				vm.recentResult = d.clone();
				vm.recentResult.type = "J";
			}
				
		}
		else if (invokeSig.contentEquals("Ljava/lang/Math;->cos(D)D")) {
			Expression d = params.get(0);
			if (d.isLiteral() && !d.isSymbolic)
			{
				vm.recentResult = Expression.newLiteral("D", ""+Math.cos(Arithmetic.parseDouble(d.toString())));
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
