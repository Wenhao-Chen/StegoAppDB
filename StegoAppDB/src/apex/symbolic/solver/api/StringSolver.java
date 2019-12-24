package apex.symbolic.solver.api;

import java.util.List;
import java.util.Random;

import apex.bytecode_wrappers.APEXStatement;
import apex.symbolic.APEXArray;
import apex.symbolic.APEXObject;
import apex.symbolic.Expression;
import apex.symbolic.MethodContext;
import apex.symbolic.VM;
import apex.symbolic.solver.Arithmetic;
import util.P;

public class StringSolver extends SolverInterface{

	@Override
	public void solve(String invokeSig, List<Expression> params, APEXStatement s, MethodContext mc, String[] paramRegs, VM vm)
	{
		if (invokeSig.equals("Ljava/lang/String;->equals(Ljava/lang/Object;)Z"))
		{
			Expression s1 = params.get(0);
			Expression s2 = params.get(1);
			//printStringInfo(s1, vm);
			//printStringInfo(s2, vm);
			if (s1.isLiteral() && s2.isLiteral())
			{
				int val = s1.toString().equals(s2.toString())?1:0;
				vm.recentResult = Expression.newLiteral("I", val+"");
			}
			else
				vm.createSymbolicMethodReturn("I", invokeSig, params, s);
		}
		else if (invokeSig.equals("Ljava/lang/String;->split(Ljava/lang/String;)[Ljava/lang/String;"))
		{
			Expression s1 = params.get(0);
			Expression s2 = params.get(1);
			//printStringInfo(s1, vm);
			//printStringInfo(s2, vm);
			if (s1.isLiteral() && s2.isLiteral())
			{
				String[] parts = s1.toString().split(s2.toString());
				APEXArray arr = vm.createNewArray("[Ljava/lang/String;", "solver", Expression.newLiteral("I", parts.length+""), s.getUniqueID());
				for (int i = 0; i < parts.length; i++)
					arr.aput(s, Expression.newLiteral("I", i+""), Expression.newLiteral("Ljava/lang/String;", parts[i]), vm);
				vm.recentResult = arr.reference;
			}
			else
				vm.createSymbolicMethodReturn("[Ljava/lang/String;", invokeSig, params, s);
		}
		else if (invokeSig.equals("Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V"))
		{
			APEXObject sb = vm.heap.get(params.get(0).getObjID());
			Expression s1 = params.get(1);
			sb.stringBuilderAppendHistory.clear();
			sb.stringBuilderAppendHistory.add(s1.clone());
		}
		else if (invokeSig.equals("Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;"))
		{
			APEXObject sb = vm.heap.get(params.get(0).getObjID());
			Expression s1 = params.get(1);
			if (sb.stringBuilderAppendHistory.isEmpty() || !s1.isLiteral())
				sb.stringBuilderAppendHistory.add(s1.clone());
			else
			{
				// if this and previous append are both literal strings, merge them
				Expression prevAppend = sb.stringBuilderAppendHistory.get(sb.stringBuilderAppendHistory.size()-1);
				if (prevAppend.isLiteral())
					prevAppend.children.get(0).root += s1.getLiteralValue();
			}
			vm.recentResult = sb.reference;
		}
		else if (invokeSig.equals("Ljava/lang/StringBuilder;->toString()Ljava/lang/String;"))
		{
			if (params.get(0).getObjID()==null)
			{
				vm.crashed = true;
				vm.shouldStop = true;
				return;
			}
			APEXObject sb = vm.heap.get(params.get(0).getObjID());
			if (sb.stringBuilderAppendHistory.isEmpty())
			{
				vm.recentResult = Expression.newLiteral("Ljava/lang/String;", "");
			}
			else if (sb.stringBuilderAppendHistory.size()==1 && sb.stringBuilderAppendHistory.get(0).isLiteral())
			{
				vm.recentResult = Expression.newLiteral("Ljava/lang/String;", sb.stringBuilderAppendHistory.get(0).getLiteralValue());
			}
			else
			{
				String wholeBody = "";
				for (Expression part : sb.stringBuilderAppendHistory)
				{
					if (part.isLiteral())
						wholeBody += part.toString()+" + ";
					else
						wholeBody += vm.heap.get(part.getObjID()).root+" + ";
				}
				wholeBody = wholeBody.substring(0, wholeBody.length()-3);
				
				APEXObject string = vm.createNewObject("Ljava/lang/String;", wholeBody, s.getUniqueID(), true);
				vm.recentResult = string.reference;
			}
		}
		else if (invokeSig.equals("Ljava/lang/String;->isEmpty()Z"))
		{
			Expression s1 = params.get(0);
			if (s1.isLiteral())
			{
				int val = s1.toString().isEmpty()?1:0;
				vm.recentResult = Expression.newLiteral("I", val+"");
			}
			else
				vm.createSymbolicMethodReturn("I", invokeSig, params, s);
		}
		else if (invokeSig.equals("Ljava/lang/String;->charAt(I)C"))
		{
			Expression s1 = params.get(0);
			Expression index = params.get(1);
/*			if (s1.isReference())
			{
				s1 = Expression.newLiteral("Ljava/lang/String;", randomAlphaNumericalString());
				mc.assign(paramRegs[0], s1);
			}
*/			
			if (s1.isLiteral() && index.isLiteral() && !index.isSymbolic)
			{
				int i = Arithmetic.parseInt(index.getLiteralValue());
				if (i<s1.toString().length())
				{
					char c = s1.toString().charAt(i);
					vm.recentResult = Expression.newLiteral("I", ""+(int)c);
					return;
				}
			}
			vm.createSymbolicMethodReturn("I", invokeSig, params, s);
		}
		else if (invokeSig.equals("Ljava/lang/String;->length()I"))
		{
			Expression s1 = params.get(0);
			if (s1.isLiteral())
			{
				vm.recentResult = Expression.newLiteral("I", s1.toString().length()+"");
			}
			else
				vm.createSymbolicMethodReturn("I", invokeSig, params, s);
		}
		else if (invokeSig.equals("Ljava/lang/String;->getBytes()[B"))
		{
			Expression str = params.get(0);
/*
			if (str.isReference()) // concretize the String
			{
				str = Expression.newLiteral("Ljava/lang/String;", randomAlphaNumericalString());
				mc.assign(paramRegs[0], str);
			}
*/			
			if (str.isLiteral() && !str.isSymbolic)
			{
				byte[] bytes = str.getLiteralValue().getBytes();
				APEXArray arr = vm.createNewArray("[B", "String.getBytes()", Expression.newLiteral("I", bytes.length+""), s.getUniqueID());
				for (int i = 0; i < bytes.length; i++)
				{
					arr.aput(s, Expression.newLiteral("I", i+""), Expression.newLiteral("I", (int)bytes[i]+""), vm);
				}
				vm.recentResult = arr.reference;
			}
			else
			{
				vm.createSymbolicMethodReturn("[B", invokeSig, params, s);
			}
			
		}
		else
		{
			P.p("forgot about this API??? " + invokeSig+". "+ s.getUniqueID());
			P.pause();
		}
	}
	
	public static String randomAlphaNumericalString()
	{
		Random rng = new Random();
		int length = rng.nextInt(10)+4;
		
		char a = 'a', A = 'A', z = 'z', Z = 'Z', i0 = '0', i9 = '9';
		
		char[] res = new char[length];
		int index = 0;
		while (index < length)
		{
			char c = (char)rng.nextInt(256);
			if ((c >= a && c <= z) || (c >= A && c <= Z) || (c >= i0 && c <= i9))
			{
				res[index++] = c;
			}
		}
		return new String(res);
	}

}
