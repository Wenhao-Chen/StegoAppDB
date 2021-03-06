package apex.symbolic;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import apex.APEXApp;
import apex.bytecode_wrappers.APEXBlock;
import apex.bytecode_wrappers.APEXMethod;
import apex.symbolic.solver.Arithmetic;
import util.Dalvik;
import util.Graphviz;
import util.P;

public class MethodContext {

	public APEXMethod m;
	private Map<String, Register> regs;
	public Set<APEXBlock> visitedBlocks = new HashSet<>();
	public int stmtIndex;
	
	public boolean paramValid;
	
	private MethodContext() {}
	public MethodContext(APEXApp app, APEXMethod m, VM_interface vm)
	{
		this(app, m, vm, null);
	}
	
	public MethodContext(APEXApp app, APEXMethod m, VM_interface vm, List<Expression> args)
	{
//		P.pf("initializing MC for [%s]: [%s]...\n", app.packageName, m.signature);
//		P.pf("-- args -- (size %d)\n", args==null?0:args.size());
//		if (args!=null)
//		for (Expression arg : args) {
//			P.p("  "+arg.toString());
//		}
		//Graphviz.makeCFG(app, m.signature);
		this.m = m;
		regs = new TreeMap<>();
		stmtIndex = 0;
		
		paramValid = true;
		Register prev = null;
		int locals = m.getLocalRegisterCount();
		for (int i = 0; i < locals; i++)
		{
			Register r = new Register("v"+i, null);
			regs.put(r.name, r);
			if (prev != null)
				prev.next = r;
			prev = r;
		}

		ArrayList<String> dexNames = m.getParamTypeDexNames();
		int j = 0, i = 0;
		for (String dexName : dexNames)
		{
			//P.p("[temp] reading "+dexName+" type from index "+i);
			Register r = new Register("p"+i);
			regs.put(r.name, r);
			regs.put("v"+(locals+i), r);
			if (prev != null)
				prev.next = r;
			prev = r;
			
			if (args == null)
			{
				if (Dalvik.isPrimitiveType(dexName))
				{
					r.value = Expression.newLiteral(dexName, "$"+r.name);
				}
				else if (Dalvik.isArrayType(dexName))
				{
					APEXArray arr = vm.createNewArray(dexName, "$"+r.name, null, "Symbolic Input Param");
					r.value = arr.reference;
				}
				else {
					APEXObject obj = vm.createNewObject(dexName, "$"+r.name, "Symbolic Input Param", true);
					r.value = obj.reference;
				}
				r.value.isSymbolic = true;
			}
			else
			{
				Expression input = args.get(j);
				//NOTE: in bytecode, null is represented as a literal 0. For example: "const/4, v0, 0x0"
				if (Dalvik.isPrimitiveType(input.type) && !Dalvik.isPrimitiveType(dexName) && Arithmetic.parseInt(input.getLiteralValue())==0)
				{
					
				}
				else if ((Dalvik.isPrimitiveType(input.type) && !Dalvik.isPrimitiveType(dexName)) ||
						(!Dalvik.isPrimitiveType(input.type) && Dalvik.isPrimitiveType(dexName)))
				{
					vm.crashed = vm.shouldStop = true;
					return;
					/*
					P.e("    [Invoke Parameter Type Mismatch] param "+i+": "+input.type+" vs "+dexName+". "+m.signature);
					MethodContext mc = vm.methodStack.peek();
					P.e("    "+mc.m.statements.get(mc.stmtIndex).getUniqueID());
					vm.print();
					P.pause();
					paramValid = false;
					return;
					*/
				}
				r.value = args.get(j++).clone();
			}
			i++;
			
			if (Dalvik.isWideType(dexName))
			{
				Register low = new Register("p"+i++);
				regs.put(low.name, low);
				r.next = low;
				r.isWideHigh = true;
				low.isWideLow = true;
				prev = low;
			}
			
		}
	}
	
	public void move(String vA, String vB)
	{
		move(vA, vB, false);
	}
	
	public void move(String vA, String vB, boolean wide) // 0 - single reg, 1 - wide reg, 2 - obj
	{
		assign(vA, read(vB), wide);
	}
	
	public Expression read(String vA)
	{
		Register reg = regs.get(vA);
		if (reg!=null && reg.value==null)
			reg.value = Expression.newLiteral("I", "0");
		return reg==null?Expression.newLiteral("I", "0"):reg.value;
	}
	
	public void assign(String vA, Expression value)
	{
		if (value.type==null)
		{
			P.p("assigning null type: "+vA +" "+value.toString());
		}
		assign(vA, value, Dalvik.isWideType(value.type));
	}
	
	public void assign(String vA, Expression value, boolean wide)
	{
		Register r = regs.get(vA);
		if (r == null)
		{
			//print();
			return;
		}
		r.value = value==null?null:value.clone();
		r.isWideLow = false;
		r.isWideHigh = false;
		if (wide)
		{
			r.isWideHigh = true;
			if (r.next != null)
				r.next.isWideLow = true;
		}
	}
	
	public void assignConst(String vA, String val, String type)
	{
		boolean wide = type.equals("J");
		if (val.contains("#"))
		{
			if (type.equals("I"))
			{
				type = "F";
				val = val.substring(val.indexOf(" # ")+3);
			}
			else if (type.equals("J"))
			{
				type = "D";
				val = val.substring(val.indexOf(" # ")+3);
			}
		}
		assign(vA, Expression.newLiteral(type, val), wide);
	}
	
	public void print(PrintStream out)
	{
		out.println(" ----- " + m.signature);
		for (Map.Entry<String, Register> entry: regs.entrySet())
		{
			Register r = entry.getValue();
			Expression value = r.value;
			out.println("  "+entry.getKey()+"\t=\t"+(r.isWideLow?"(low 16)":
				(value==null?"null":value.toString()))
					);
		}
	}
	
	public void print()
	{
		print(System.out);
	}
	
	public void writeToFile(PrintWriter out)
	{
		out.println(" ----- " + m.signature);
		for (Map.Entry<String, Register> entry: regs.entrySet())
		{
			Register r = entry.getValue();
			Expression value = r.value;
			out.println("  "+entry.getKey()+"\t=\t"+(r.isWideLow?"(low 16)":
				(value==null?"null":value.toString()))
					);
		}
	}
	
	public MethodContext clone()
	{
		MethodContext mc = new MethodContext();
		mc.m = m;
		mc.regs = new TreeMap<>();
		mc.stmtIndex = stmtIndex;
		for (String name : regs.keySet())
			mc.regs.put(name, regs.get(name).clone());
		
		mc.visitedBlocks.addAll(visitedBlocks);
		
		return mc;
	}
	
	
}
