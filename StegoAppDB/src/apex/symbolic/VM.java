package apex.symbolic;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;

import apex.APEXApp;
import apex.bytecode_wrappers.APEXBlock;
import apex.bytecode_wrappers.APEXClass;
import apex.bytecode_wrappers.APEXField;
import apex.bytecode_wrappers.APEXMethod;
import apex.bytecode_wrappers.APEXStatement;
import apex.symbolic.APEXObject.BitmapAccess;
import apex.symbolic.solver.Arithmetic;
import apex.symbolic.solver.Condition;
import apex.symbolic.solver.Logic;
import apex.symbolic.solver.api.APISolver;
import apex.symbolic.solver.api.BitmapSolver;
import ui.ProgressUI;
import util.Dalvik;
import util.P;

public class VM extends VM_interface{
	
	
	public static int pathCount = 0;
	
	public Stack<MethodContext> methodStack;
	public Map<String, APEXObject> heap;
	public Expression recentResult;
	public List<Expression> pathCondition;
	public List<APEXStatement> execLog;
	
	private int objCounter;
	private Set<String> loadedClasses;
	public boolean crashed;
	
	public APEXApp app;
	public boolean stepExecute = false;
	public boolean success = false;
	public boolean shouldStop = false;
	public List<VM> otherVMs;
	
	public boolean allocateConcreteBitmap = false;
	public boolean spawnClones = true;
	public static long timeLimit_seconds = 0;
	public long startTime = -1;
	
	public static boolean printDebug = false;
	
	public Set<APEXStatement> visitedBranches;
	
	public static Map<String, Integer> branchVisitTimes = new HashMap<>();
	public static int maxBranchVisitTime = 200;
	
	private Map<String, Integer> branchCounter = new HashMap<>();
	private Map<APEXStatement, Expression> prevCond = new HashMap<>();
	
	public static ProgressUI ui_execLog = ProgressUI.create("Execution Log", 20);
	
	public List<BitmapAccess> bitmapAccess;
	
	public VM clone()
	{
		VM vm = new VM();
		vm.app = app;
		vm.crashed = crashed;
		vm.otherVMs = new ArrayList<>();
		vm.visitedBranches = new HashSet<>();
		vm.visitedBranches.addAll(visitedBranches);
		vm.execLog = new ArrayList<APEXStatement>(execLog);
		vm.allocateConcreteBitmap = this.allocateConcreteBitmap;
		vm.spawnClones = spawnClones;
		vm.bitmapAccess = new ArrayList<>(bitmapAccess);
		
		vm.methodStack = new Stack<>();
		Stack<MethodContext> temp = new Stack<>();
		for (MethodContext mc : methodStack)
			temp.push(mc.clone());
		for (MethodContext mc : temp)
			vm.methodStack.push(mc);
		
		vm.heap = new TreeMap<>();
		for (String id : heap.keySet())
			vm.heap.put(id, heap.get(id).clone());
		
		vm.recentResult = recentResult==null?null:recentResult.clone();
		
		vm.pathCondition = new ArrayList<>();
		for (Expression cond : pathCondition)
			vm.pathCondition.add(cond.clone());
		
		vm.objCounter = objCounter;
		vm.loadedClasses = new HashSet<>();
		for (String c : loadedClasses)
			vm.loadedClasses.add(c);
		
		for (String k : branchCounter.keySet())
			vm.branchCounter.put(k, branchCounter.get(k));
		for (APEXStatement k : prevCond.keySet())
			vm.prevCond.put(k, prevCond.get(k));
		
		return vm;
	}
	

	private VM() {};
	
	public VM(APEXApp app)
	{
		this.app = app;
		methodStack = new Stack<>();
		heap = new TreeMap<>();
		loadedClasses = new HashSet<>();
		pathCondition = new ArrayList<>();
		execLog = new ArrayList<>();
		otherVMs = new ArrayList<>();
		visitedBranches = new HashSet<>();
		crashed = false;
		bitmapAccess = new ArrayList<>();
		
		objCounter = 0;
		createNewObject("StaticFields", "dummy", "ClassLoader", false);
	}
	
	public VM(APEXApp app, APEXMethod m)
	{
		this(app);
		getReady(m);
	}
	
	public void getReady(APEXMethod m)
	{
		getReady(m, null);
	}
	
	public void getReady(APEXMethod m, List<Expression> params)
	{
		MethodContext mc = new MethodContext(app, m, this, params);
		methodStack.push(mc);
	}
	
	public void execute(APEXMethod m)
	{
		execute(m, null);
	}
	
	public void execute(APEXMethod m, List<Expression> params)
	{
		if (printDebug)
			P.p("===== executing method "+m.signature);
		getReady(m, params);
		
		execute(true);
	}
	
	public void execute(boolean continuous)
	{
		//this.branchVisitTimes = new HashMap<>();
		while (!methodStack.isEmpty() && !shouldStop)
		{
			MethodContext mc = methodStack.peek();
			if (mc.stmtIndex >= mc.m.statements.size())
			{
				P.p("[stmt index out of bound], popping...");
				methodStack.pop();
				continue;
			}
			APEXStatement s = mc.m.statements.get(mc.stmtIndex++);
			//P.p("[exec_log] "+s.index);
			ui_execLog.newLine(mc.m.signature+" " + s.index+"  "+s.smali);
			executeStatement(s);
			
			if (!continuous)
			{
				print();
				P.pause();
				P.p("====== Hit Enter to Continue Execution ======");
			}
		}
	}
	
	public void executeStatement(APEXStatement s)
	{
		Expression cond = null;
		if (startTime == -1)
			startTime = System.currentTimeMillis();
		if (VM.timeLimit_seconds > 0 && System.currentTimeMillis() - startTime > 1000*VM.timeLimit_seconds) {
			this.shouldStop = true;
			return;
		}
		MethodContext mc = methodStack.peek();
		if (printDebug && s.index==0)
			P.p("  [Method Start] "+s.m.signature);
		if (printDebug)
			P.p("  [STMT] " + s.getUniqueID());
		if (s.block.statements.get(0)==s)
		{
			mc.visitedBlocks.add(s.block);
			//String label = s.m.signature+s.block.getLabelString();
			//note: shouldn't increase the visit times here because we can't distinguish
			// whether this branch is visited deterministically or symbolically
			//branchVisitTimes.put(label, branchVisitTimes.getOrDefault(label, 0)+1);]
//			int visitTimes = branchVisitTimes.getOrDefault(label, 0);
//			if (visitTimes > maxBranchVisitTime)
//			{
//				ui_execLog.newLine("stopping branch "+s.getUniqueID());
//				shouldStop = true;
//				return;
//			}
//			if (branchCounter.getOrDefault(label, 0) > 200) {
//				shouldStop = true;
//				return;
//			}
//			branchCounter.put(label, branchCounter.getOrDefault(label, 0)+1);
		}
		execLog.add(s);
		
		String op = s.getOpcode();
		String[] args = s.getArguments();
		switch (op)
		{
		case "nop"                     : 
			break;
		case "move"                    : 	// vA, vB
		case "move/from16"             : 	// vAA, vBBBB
		case "move/16"                 : 	// vAAAA, vBBBB
			mc.move(args[0], args[1], false);
			break;
		case "move-wide"               : 	// vA, vB
		case "move-wide/from16"        : 	// vAA, vBBBB
		case "move-wide/16"            : 	// vAAAA, vBBBB
			mc.move(args[0], args[1], true);
			break;
		case "move-object"             : 	// vA, vB
		case "move-object/from16"      : 	// vAA, vBBBB
		case "move-object/16"          : 	// vAAAA, vBBBB
			if (mc.read(args[1]) == null) {
				this.crashed = this.shouldStop = true;
				break;
			}
			mc.move(args[0], args[1], false);
			break;
		case "move-result"             : 	// vAA
			if (recentResult==null)
			{
				this.crashed = true;
				this.shouldStop = true;
				return;
			}
			mc.assign(args[0], recentResult);
			break;
		case "move-result-wide"        : 	// vAA
			mc.assign(args[0], recentResult, true);
			break;
		case "move-result-object"      : 	// vAA
			mc.assign(args[0], recentResult);
			break;
		case "move-exception"          : 	// vAA
			break;
		case "return-void"             : 
			methodStack.pop();
			break;
		case "return"                  : 	// vAA
		case "return-wide"             : 	// vAA
			recentResult = mc.read(args[0]);
			methodStack.pop();
			break;
		case "return-object"           : 	// vAA
			recentResult = mc.read(args[0]);
			// sometimes the return value is a null object (const/4 v0, 0x0) but we don't know it yet
			if (recentResult.type.equals("I") && !recentResult.isSymbolic && Arithmetic.parseInt(recentResult.getLiteralValue())==0)
			{
				String type = mc.m.subSignature.substring(mc.m.subSignature.lastIndexOf(")")+1);
				if (type.startsWith("["))
					recentResult = createNewArray(type, "null", Expression.newLiteral("I", "0"), s.getUniqueID()+" "+s.smali).reference;
				else
					
					recentResult = createNewObject(type, "null", s.getUniqueID(), false).reference;
			}
			methodStack.pop();
			break;
		case "const/4"                 : 	// vA, #+B
		case "const/16"                : 	// vAA, #+BBBB
		case "const"                   : 	// vAA, #+BBBBBBBB
		case "const/high16"            : 	// vAA, #+BBBB0000
			mc.assignConst(args[0], args[1], "I");
			break;
		case "const-wide/16"           : 	// vAA, #+BBBB
		case "const-wide/32"           : 	// vAA, #+BBBBBBBB
		case "const-wide"              : 	// vAA, #+BBBBBBBBBBBBBBBB
		case "const-wide/high16"       : 	// vAA, #+BBBB000000000000
			mc.assignConst(args[0], args[1], "J");
			break;
		case "const-string"            : 	// vAA, string@BBBB
		case "const-string/jumbo"      : 	// vAA, string@BBBBBBBB
			mc.assignConst(args[0], args[1].substring(1, args[1].length()-1), "Ljava/lang/String;");
			break;
		case "const-class"             : 	// vAA, type@BBBB
			mc.assignConst(args[0], args[1], "Ljava/lang/Class;");
			break;
		case "monitor-enter"           : 	// vAA
		case "monitor-exit"            : 	// vAA
			//NOTE: ignoring the above 2 for now
			break;
		case "check-cast"              : 	// vAA, type@BBBB
		{
			//NOTE: apparently some apps do check-cast null... "com.sg.steganography.apk"
			String objID = mc.read(args[0]).getObjID();
			if (objID != null)
			{
				APEXObject obj = heap.get(objID);
				obj.reference.type = args[1];
				if (args[1].startsWith("[") && !args[1].equals(obj.type))
				{
					APEXArray arr = this.createNewArray(obj.type, obj.root, null, obj.birth);
					mc.assign(args[0], arr.reference);
				}
				obj.type = args[1];
			} else {
				crashed = shouldStop = true;
			}
			break;
		}
		case "instance-of"             : 	// vA, vB, type@CCCC
			if (app.instanceOf(mc.read(args[1]).type, args[2]))
				mc.assign(args[0], Expression.newLiteral("I", "1"));
			else
				mc.assign(args[0], Expression.newLiteral("I", "0"));
			break;
		case "array-length"            : 	// vA, vB
		{
			if (mc.read(args[1]).getObjID()==null)
			{
				crashed = true;
				shouldStop = true;
				return;
			}
			APEXObject obj = heap.get(mc.read(args[1]).getObjID());
			if (!(obj instanceof APEXArray))
			{
				crashed = shouldStop = true;
				return;
			}
			APEXArray arr = (APEXArray) obj;
			mc.assign(args[0], arr.length);
			break;
		}
		case "new-instance"            : 	// vAA, type@BBBB
			APEXObject obj = createNewObject(args[1], "new-instance", s.getUniqueID()+" "+s.smali, false);
			mc.assign(args[0], obj.reference);
			break;
		case "new-array"               : 	// vA, vB, type@CCCC
		{
			APEXArray arr = createNewArray(args[2], "array", mc.read(args[1]), s.getUniqueID()+" "+s.smali);
			mc.assign(args[0], arr.reference);
			break;
		}
		case "filled-new-array"        : 	// {vC, vD, vE, vF, vG}, type@BBBB. 
		case "filled-new-array/range"  : 	// {vCCCC .. vNNNN}, type@BBBB
		{	// with these two statements, the array content must be single-word
			String[] eleRegs = s.getParamRegisters();
			APEXArray arr = createNewArray(args[1], "array", Expression.newLiteral("I", eleRegs.length+""), s.getUniqueID()+" "+s.smali);
			for (int i = 0; i < eleRegs.length; i++)
			{
				Expression exp = mc.read(eleRegs[i]);
				arr.aput(s, i, exp, this);
				if (Dalvik.isWideType(exp.type))
					i++;
			}
				
			recentResult = arr.reference;
			break;
		}
		case "fill-array-data"         : 	// vAA, +BBBBBBBB
		{	// there must be a "new-array" statement prior to this one, and the array content must be primitive
			APEXArray arr = (APEXArray) heap.get(mc.read(args[0]).getObjID());
			String eleType = arr.type.substring(1);
			ArrayList<String> data = mc.m.getSupplementalData(args[1]);
			int index = 0;
			for (String line : data)
			{
				if (line.charAt(4)=='.'||line.charAt(4)==':')
					continue;
				String val = line.trim();
				if (val.contains("#")) // float or double
					val = val.substring(val.indexOf("#")+1).trim();
				else if (val.endsWith("t"))
					val = val.substring(0, val.length()-1);
				arr.aput(s, index++, Expression.newLiteral(eleType, val), this);
			}
			break;
		}
		case "throw"                   : 	// vAA
			methodStack.pop(); //NOTE: not really handling exceptions right now
			this.shouldStop = true;
			break;
		case "goto"                    : 	// +AA
		case "goto/16"                 : 	// +AAAA
		case "goto/32"                 : 	// +AAAAAAAA
			mc.stmtIndex = mc.m.getBlock(args[0]).getFirstStatement().index;
			break;
		case "packed-switch"           : 	// vAA, +BBBBBBBB
		case "sparse-switch"           : 	// vAA, +BBBBBBBB
		{
			Expression vA = mc.read(args[0]);
			Map<Integer, String> switchMap = s.getSwitchMap();
			if (vA.isLiteral() && !vA.isSymbolic) // can solve branch
			{
				int val = Arithmetic.parseInt(vA.children.get(0).root);
				if (switchMap.containsKey(val)) {
					mc.stmtIndex = mc.m.getBlock(switchMap.get(val)).getFirstStatement().index;
				}
			}
			else	// can't solve branch, need to avoid infinite loop here
			{
				String flow = "!=";
				String jump = "==";
				boolean hasPrevCond = false;
				for (int val : switchMap.keySet())
				{
					Expression jumpCond = Expression.newCondition("!=", vA, Expression.newLiteral("I", val+""));
					if (hasPrevCond(jumpCond))
					{
						mc.stmtIndex = mc.m.getBlock(switchMap.get(val)).getFirstStatement().index;
						hasPrevCond = true;
						break;
					}
				}
				if (!hasPrevCond)
				{
					Set<String> branches = new HashSet<>();
					for (int val : switchMap.keySet())
					{
						String label = switchMap.get(val);
						if (branches.contains(label))
							continue;
						branches.add(label);
						int jumpIndex = mc.m.getBlock(label).getFirstStatement().index;
						this.SymbolicPathing(mc, s, flow, jump, vA, Expression.newLiteral("I", val+""), jumpIndex, false);
					}
				}
			}
			break;
		}
		// vA = (0 if b==c) or (1 if b>c) or (-1 if b<c)
		case "cmpl-float"              : 	// vAA, vBB, vCC
		case "cmpg-float"              : 	// vAA, vBB, vCC
			if (mc.read(args[1])==null || mc.read(args[2])==null) {
				this.crashed = this.shouldStop = true;
				break;
			}
			mc.assign(args[0], Condition.cmp(mc.read(args[1]), mc.read(args[2]), "F"));
			break;
		case "cmpl-double"             : 	// vAA, vBB, vCC
		case "cmpg-double"             : 	// vAA, vBB, vCC
			if (mc.read(args[1])==null || mc.read(args[2])==null) {
				this.crashed = this.shouldStop = true;
				break;
			}
			mc.assign(args[0], Condition.cmp(mc.read(args[1]), mc.read(args[2]), "D"));
			break;
		case "cmp-long"                : 	// vAA, vBB, vCC
			if (mc.read(args[1])==null || mc.read(args[2])==null) {
				this.crashed = this.shouldStop = true;
				break;
			}
			mc.assign(args[0], Condition.cmp(mc.read(args[1]), mc.read(args[2]), "J"));
			break;
		case "if-eq"                   : 	// vA, vB, +CCCC
		{
//			if (s.index==9 && s.smali.equals("if-eq v3, v4, :cond_1")) {
//				P.p("v3 = "+mc.read("v3").toString());
//			}
			int decision = 0;
			String flow = "!=";
			String jump = "==";
			Expression jumpCond = Expression.newCondition(jump, mc.read(args[0]), mc.read(args[1]));
			Expression flowCond = Expression.newCondition(flow, mc.read(args[0]), mc.read(args[1]));
			int jumpIndex = mc.m.getBlock(args[2]).getFirstStatement().index;
			int cmp = Condition.compare(mc.read(args[0]), mc.read(args[1]), "I");
			if (cmp == Condition.Undetermined)
			{
				if (pathCondition.contains(flowCond)) {decision=1;} // flow
				else if (pathCondition.contains(jumpCond)) {// jump
					mc.stmtIndex = jumpIndex;
					decision = 2;
				}
				else {// jump and symbolically flow 
					decision = 3;
					this.SymbolicPathing(mc, s, flow, jump, mc.read(args[0]), mc.read(args[1]), jumpIndex);
				}
					
			}
			else if (cmp == Condition.Equal) {
				mc.stmtIndex = jumpIndex;
				decision = 4;
			}
			cond = mc.stmtIndex == s.index+1? flowCond : jumpCond;
			break;
		}
		case "if-ne"                   : 	// vA, vB, +CCCC
		{
			String flow = "==";
			String jump = "!=";
			Expression jumpCond = Expression.newCondition(jump, mc.read(args[0]), mc.read(args[1]));
			Expression flowCond = Expression.newCondition(flow, mc.read(args[0]), mc.read(args[1]));
			int jumpIndex = mc.m.getBlock(args[2]).getFirstStatement().index;
			int cmp = Condition.compare(mc.read(args[0]), mc.read(args[1]), "I");
			if (cmp == Condition.Undetermined)
			{
				
				if (pathCondition.contains(flowCond)) {}
				else if (pathCondition.contains(jumpCond))
					mc.stmtIndex = jumpIndex;
				else
					this.SymbolicPathing(mc, s, flow, jump, mc.read(args[0]), mc.read(args[1]), jumpIndex);
			}
			else if (cmp != Condition.Equal)
				mc.stmtIndex = jumpIndex;
			cond = mc.stmtIndex == s.index+1? flowCond : jumpCond;
			break;
		}
		case "if-lt"                   : 	// vA, vB, +CCCC
		{
			String flow = ">=";
			String jump = "<";
			Expression jumpCond = Expression.newCondition(jump, mc.read(args[0]), mc.read(args[1]));
			Expression flowCond = Expression.newCondition(flow, mc.read(args[0]), mc.read(args[1]));
			int jumpIndex = mc.m.getBlock(args[2]).getFirstStatement().index;
			int cmp = Condition.compare(mc.read(args[0]), mc.read(args[1]), "I");
			if (cmp == Condition.Undetermined)
			{
				if (pathCondition.contains(flowCond)) {
					//P.p("already did this...");
				}
				else if (pathCondition.contains(jumpCond))
					mc.stmtIndex = jumpIndex;
				else
					this.SymbolicPathing(mc, s, flow, jump, mc.read(args[0]), mc.read(args[1]), jumpIndex);
			}
			else if (cmp == Condition.Less)
				mc.stmtIndex = jumpIndex;
			cond = mc.stmtIndex == s.index+1? flowCond : jumpCond;
			break;
		}
		case "if-ge"                   : 	// vA, vB, +CCCC
		{
			int decision = 0;
			String flow = "<";
			String jump = ">=";
			if (mc.read(args[0]) == null || mc.read(args[1])==null) {
				this.crashed = this.shouldStop = true;
				break;
			}
			Expression jumpCond = Expression.newCondition(jump, mc.read(args[0]), mc.read(args[1]));
			Expression flowCond = Expression.newCondition(flow, mc.read(args[0]), mc.read(args[1]));
			int jumpIndex = mc.m.getBlock(args[2]).getFirstStatement().index;
			int cmp = Condition.compare(mc.read(args[0]), mc.read(args[1]), "I");
			if (cmp == Condition.Undetermined)
			{
				if (pathCondition.contains(flowCond)) {
					decision = 1;
				}
				else if (pathCondition.contains(jumpCond)) {
					mc.stmtIndex = jumpIndex;
					decision = 2;
				}
				else {
					this.SymbolicPathing(mc, s, flow, jump, mc.read(args[0]), mc.read(args[1]), jumpIndex);
					decision = 3;
				}
				
			}
			else if (cmp != Condition.Less) {
				mc.stmtIndex = jumpIndex;
				decision = 4;
			}
				
			cond = mc.stmtIndex == s.index+1? flowCond : jumpCond;
			
			if (s.index==210 && s.smali.equals("if-ge v0, v2, :cond_15")) {
				P.p("210 cmp: "+cmp+". decision = "+decision);
			}
			break;
		}
		case "if-gt"                   : 	// vA, vB, +CCCC
		{
			String flow = "<=";
			String jump = ">";
			Expression jumpCond = Expression.newCondition(jump, mc.read(args[0]), mc.read(args[1]));
			Expression flowCond = Expression.newCondition(flow, mc.read(args[0]), mc.read(args[1]));
			int jumpIndex = mc.m.getBlock(args[2]).getFirstStatement().index;
			int cmp = Condition.compare(mc.read(args[0]), mc.read(args[1]), "I");
			if (cmp == Condition.Undetermined)
			{
				if (pathCondition.contains(flowCond)) {}
				else if (pathCondition.contains(jumpCond))
					mc.stmtIndex = jumpIndex;
				else
				this.SymbolicPathing(mc, s, flow, jump, mc.read(args[0]), mc.read(args[1]), jumpIndex);
			}
			else if (cmp == Condition.Greater)
				mc.stmtIndex = jumpIndex;
			cond = mc.stmtIndex == s.index+1? flowCond : jumpCond;
			break;
		}
		case "if-le"                   : 	// vA, vB, +CCCC
		{
			String flow = ">";
			String jump = "<=";
			Expression jumpCond = Expression.newCondition(jump, mc.read(args[0]), mc.read(args[1]));
			Expression flowCond = Expression.newCondition(flow, mc.read(args[0]), mc.read(args[1]));
			int jumpIndex = mc.m.getBlock(args[2]).getFirstStatement().index;
			int cmp = Condition.compare(mc.read(args[0]), mc.read(args[1]), "I");
			if (cmp == Condition.Undetermined)
			{
				if (pathCondition.contains(flowCond)) {}
				else if (pathCondition.contains(jumpCond))
					mc.stmtIndex = jumpIndex;
				else
				this.SymbolicPathing(mc, s, flow, jump, mc.read(args[0]), mc.read(args[1]), jumpIndex);
			}
			else if (cmp != Condition.Greater)
				mc.stmtIndex = jumpIndex;
			cond = mc.stmtIndex == s.index+1? flowCond : jumpCond;
			break;
		}
		case "if-eqz"                  : 	// vAA, +BBBB
		{

			int jumpIndex = mc.m.getBlock(args[1]).getFirstStatement().index;
			String flow = "!=";
			String jump = "==";
			Expression vA = mc.read(args[0]);
			if (vA==null) {
				this.shouldStop = this.crashed = true;
				break;
			}
			Expression vB = Expression.newLiteral("I", "0");
			if (vA.root.equals("cmp_cond"))
			{
				Expression jumpCond = Expression.newCondition(jump, vA.children.get(0), vA.children.get(1));
				Expression flowCond = Expression.newCondition(flow, vA.children.get(0), vA.children.get(1));
				if (pathCondition.contains(flowCond)) {}
				else if (pathCondition.contains(jumpCond))
					mc.stmtIndex = jumpIndex;
				else
					this.SymbolicPathing(mc, s, flow, jump, vA.children.get(0), vA.children.get(1), jumpIndex);
				cond = mc.stmtIndex == s.index+1? flowCond : jumpCond;
			}
			else
			{
				//this if statement is special because it can be used for object as well: "if (obj == null) ..."
				int val = (!Dalvik.isPrimitiveType(vA.type))?
						Condition.Undetermined:Condition.compare(vA, vB, "I");
				Expression jumpCond = Expression.newCondition(jump, vA, vB);
				Expression flowCond = Expression.newCondition(flow, vA, vB);
				if (val == Condition.Undetermined)
				{
					if (pathCondition.contains(flowCond)) { }
					else if (pathCondition.contains(jumpCond)) {
						mc.stmtIndex = jumpIndex;
					}
					else
						this.SymbolicPathing(mc, s, flow, jump, vA, vB, jumpIndex);
				}
				else if (val == Condition.Equal)
					mc.stmtIndex = jumpIndex;
				cond = mc.stmtIndex == s.index+1? flowCond : jumpCond;
			}

			break;
		}
		case "if-nez"                  : 	// vAA, +BBBB
		{
			int jumpIndex = mc.m.getBlock(args[1]).getFirstStatement().index;
			String flow = "==";
			String jump = "!=";
			Expression vA = mc.read(args[0]);
			Expression vB = Expression.newLiteral("I", "0");
			if (vA.root.equals("cmp_cond"))
			{
				Expression jumpCond = Expression.newCondition(jump, vA.children.get(0), vA.children.get(1));
				Expression flowCond = Expression.newCondition(flow, vA.children.get(0), vA.children.get(1));
				if (pathCondition.contains(flowCond)) {}
				else if (pathCondition.contains(jumpCond))
					mc.stmtIndex = jumpIndex;
				else
					SymbolicPathing(mc, s, flow, jump, vA.children.get(0), vA.children.get(1), jumpIndex);
				cond = mc.stmtIndex == s.index+1? flowCond : jumpCond;
			}
			else
			{
				int val = (!Dalvik.isPrimitiveType(vA.type))?
						Condition.Undetermined:Condition.compare(vA, vB, "I");
				Expression jumpCond = Expression.newCondition(jump, vA, vB);
				Expression flowCond = Expression.newCondition(flow, vA, vB);
				if (val == Condition.Undetermined)
				{
					if (pathCondition.contains(flowCond)) {}
					else if (pathCondition.contains(jumpCond))
						mc.stmtIndex = jumpIndex;
					else
					this.SymbolicPathing(mc, s, flow, jump, vA, vB, jumpIndex);
				}
				else if (val != Condition.Equal)
					mc.stmtIndex = jumpIndex;
				cond = mc.stmtIndex == s.index+1? flowCond : jumpCond;
			}
			break;
		}
		case "if-ltz"                  : 	// vAA, +BBBB
		{
			int jumpIndex = mc.m.getBlock(args[1]).getFirstStatement().index;
			String flow = ">=";
			String jump = "<";
			Expression vA = mc.read(args[0]);
			Expression vB = Expression.newLiteral("I", "0");
			if (vA.root.equals("cmp_cond"))
			{
				Expression jumpCond = Expression.newCondition(jump, vA.children.get(0), vA.children.get(1));
				Expression flowCond = Expression.newCondition(flow, vA.children.get(0), vA.children.get(1));
				if (pathCondition.contains(flowCond)) {}
				else if (pathCondition.contains(jumpCond))
					mc.stmtIndex = jumpIndex;
				else
					SymbolicPathing(mc, s, flow, jump, vA.children.get(0), vA.children.get(1), jumpIndex);
				cond = mc.stmtIndex == s.index+1? flowCond : jumpCond;
			}
			else
			{
				int val = Condition.compare(vA, vB, "I");
				Expression jumpCond = Expression.newCondition(jump, vA, vB);
				Expression flowCond = Expression.newCondition(flow, vA, vB);
				if (val == Condition.Undetermined)
				{
					if (pathCondition.contains(flowCond)) {}
					else if (pathCondition.contains(jumpCond))
						mc.stmtIndex = jumpIndex;
					else
					this.SymbolicPathing(mc, s, flow, jump, vA, vB, jumpIndex);
				}
				else if (val == Condition.Less)
					mc.stmtIndex = jumpIndex;
				cond = mc.stmtIndex == s.index+1? flowCond : jumpCond;
			}
			break;
		}
		case "if-gez"                  : 	// vAA, +BBBB
		{
			int jumpIndex = mc.m.getBlock(args[1]).getFirstStatement().index;
			String flow = "<";
			String jump = ">=";
			Expression vA = mc.read(args[0]);
			Expression vB = Expression.newLiteral("I", "0");
			if (vA.root.equals("cmp_cond"))
			{
				Expression jumpCond = Expression.newCondition(jump, vA.children.get(0), vA.children.get(1));
				Expression flowCond = Expression.newCondition(flow, vA.children.get(0), vA.children.get(1));
				if (pathCondition.contains(flowCond)) {}
				else if (pathCondition.contains(jumpCond))
					mc.stmtIndex = jumpIndex;
				else
					SymbolicPathing(mc, s, flow, jump, vA.children.get(0), vA.children.get(1), jumpIndex);
				cond = mc.stmtIndex == s.index+1? flowCond : jumpCond;
			}
			else
			{
				int val = Condition.compare(vA, vB, "I");
				Expression jumpCond = Expression.newCondition(jump, vA, vB);
				Expression flowCond = Expression.newCondition(flow, vA, vB);
				if (val == Condition.Undetermined)
				{
					if (pathCondition.contains(flowCond)) {}
					else if (pathCondition.contains(jumpCond))
						mc.stmtIndex = jumpIndex;
					else
					this.SymbolicPathing(mc, s, flow, jump, vA, vB, jumpIndex);
				}
				else if (val != Condition.Less)
					mc.stmtIndex = jumpIndex;
				cond = mc.stmtIndex == s.index+1? flowCond : jumpCond;
			}
			break;
		}
		case "if-gtz"                  : 	// vAA, +BBBB
		{
			int jumpIndex = mc.m.getBlock(args[1]).getFirstStatement().index;
			String flow = "<=";
			String jump = ">";
			Expression vA = mc.read(args[0]);
			Expression vB = Expression.newLiteral("I", "0");
			if (vA.root.equals("cmp_cond"))
			{
				Expression jumpCond = Expression.newCondition(jump, vA.children.get(0), vA.children.get(1));
				Expression flowCond = Expression.newCondition(flow, vA.children.get(0), vA.children.get(1));
				if (pathCondition.contains(flowCond)) {}
				else if (pathCondition.contains(jumpCond))
					mc.stmtIndex = jumpIndex;
				else
					SymbolicPathing(mc, s, flow, jump, vA.children.get(0), vA.children.get(1), jumpIndex);
				cond = mc.stmtIndex == s.index+1? flowCond : jumpCond;
			}
			else
			{
				int val = Condition.compare(vA, vB, "I");
				Expression jumpCond = Expression.newCondition(jump, vA, vB);
				Expression flowCond = Expression.newCondition(flow, vA, vB);
				if (val == Condition.Undetermined)
				{
					if (pathCondition.contains(flowCond)) {}
					else if (pathCondition.contains(jumpCond))
						mc.stmtIndex = jumpIndex;
					else
					this.SymbolicPathing(mc, s, flow, jump, vA, vB, jumpIndex);
				}
				else if (val == Condition.Greater)
					mc.stmtIndex = jumpIndex;
				cond = mc.stmtIndex == s.index+1? flowCond : jumpCond;
			}
			break;
		}
		case "if-lez"                  : 	// vAA, +BBBB
		{
			int jumpIndex = mc.m.getBlock(args[1]).getFirstStatement().index;
			String flow = ">";
			String jump = "<=";
			Expression vA = mc.read(args[0]);
			Expression vB = Expression.newLiteral("I", "0");
			if (vA.root.equals("cmp_cond"))
			{
				Expression jumpCond = Expression.newCondition(jump, vA.children.get(0), vA.children.get(1));
				Expression flowCond = Expression.newCondition(flow, vA.children.get(0), vA.children.get(1));
				if (pathCondition.contains(flowCond)) {}
				else if (pathCondition.contains(jumpCond))
					mc.stmtIndex = jumpIndex;
				else
					SymbolicPathing(mc, s, flow, jump, vA.children.get(0), vA.children.get(1), jumpIndex);
				cond = mc.stmtIndex == s.index+1? flowCond : jumpCond;
			}
			else
			{
				int val = Condition.compare(vA, vB, "I");
				Expression jumpCond = Expression.newCondition(jump, vA, vB);
				Expression flowCond = Expression.newCondition(flow, vA, vB);
				if (val == Condition.Undetermined)
				{
					if (pathCondition.contains(flowCond)) {}
					else if (pathCondition.contains(jumpCond))
						mc.stmtIndex = jumpIndex;
					else
						SymbolicPathing(mc, s, flow, jump, vA, vB, jumpIndex);
				}
				else if (val != Condition.Greater)
					mc.stmtIndex = jumpIndex;
				cond = mc.stmtIndex == s.index+1? flowCond : jumpCond;
			}
			break;
		}
		case "aget"                    : 	// vAA, vBB, vCC
		case "aget-wide"               : 	// vAA, vBB, vCC
		case "aget-object"             : 	// vAA, vBB, vCC
		case "aget-boolean"            : 	// vAA, vBB, vCC
		case "aget-byte"               : 	// vAA, vBB, vCC
		case "aget-char"               : 	// vAA, vBB, vCC
		case "aget-short"              : 	// vAA, vBB, vCC
		{
			String objID = mc.read(args[1]).getObjID();
			if (objID == null)
			{
				crashed = true;
				shouldStop = true;
				return;
			}
			APEXObject arrObj = heap.get(objID);
			if (!(arrObj instanceof APEXArray))
			{
				this.shouldStop = this.crashed = true;
				break;
			}
			APEXArray arr = (APEXArray) arrObj;
			Expression ele = arr.aget(mc.read(args[2]), this, s);
			mc.assign(args[0], ele);
			break;
		}
		case "aput"                    : 	// vAA, vBB, vCC
		case "aput-wide"               : 	// vAA, vBB, vCC
		case "aput-object"             : 	// vAA, vBB, vCC
		case "aput-boolean"            : 	// vAA, vBB, vCC
		case "aput-byte"               : 	// vAA, vBB, vCC
		case "aput-char"               : 	// vAA, vBB, vCC
		case "aput-short"              : 	// vAA, vBB, vCC
		{
			Expression arrObj = mc.read(args[1]);
			if (!arrObj.isReference())
			{
				shouldStop = true;
				crashed = true;
				return;
			}
			obj = heap.get(mc.read(args[1]).getObjID());
			if (!(obj instanceof APEXArray))
			{
				shouldStop = crashed = true;
				return;
			}
			APEXArray arr = (APEXArray) obj;
			arr.aput(s, mc.read(args[2]), mc.read(args[0]), this);
			if (s.smali.equals("aput v6, v2, v4") && (s.index==348||s.index==314)) {
				P.p(args[0]+" = "+mc.read(args[0]).toString());
				P.p(args[2]+" = "+mc.read(args[2]).toString());
			}
			break;
		}
		case "iget"                    : 	// vA, vB, field@CCCC
		case "iget-wide"               : 	// vA, vB, field@CCCC
		case "iget-object"             : 	// vA, vB, field@CCCC
		case "iget-boolean"            : 	// vA, vB, field@CCCC
		case "iget-byte"               : 	// vA, vB, field@CCCC
		case "iget-char"               : 	// vA, vB, field@CCCC
		case "iget-short"              : 	// vA, vB, field@CCCC
		{
			Expression objRef = mc.read(args[1]);
			if (!objRef.isReference())
			{
				shouldStop = true;
				crashed = true;
				return;
			}
			mc.assign(args[0], getField(objRef.getObjID(), args[2], s));
			break;
		}
		case "iput"                    : 	// vA, vB, field@CCCC
		case "iput-wide"               : 	// vA, vB, field@CCCC
		case "iput-object"             : 	// vA, vB, field@CCCC
		case "iput-boolean"            : 	// vA, vB, field@CCCC
		case "iput-byte"               : 	// vA, vB, field@CCCC
		case "iput-char"               : 	// vA, vB, field@CCCC
		case "iput-short"              : 	// vA, vB, field@CCCC
		{
			Expression objRef = mc.read(args[1]);
			if (!objRef.isReference() || mc.read(args[0])==null)
			{
				shouldStop = true;
				crashed = true;
				return;
			}
			putField(mc.read(args[0]), objRef.getObjID(), args[2]);
			break;
		}
		case "sget"                    : 	// vAA, field@BBBB
		case "sget-wide"               : 	// vAA, field@BBBB
		case "sget-object"             : 	// vAA, field@BBBB
		case "sget-boolean"            : 	// vAA, field@BBBB
		case "sget-byte"               : 	// vAA, field@BBBB
		case "sget-char"               : 	// vAA, field@BBBB
		case "sget-short"              : 	// vAA, field@BBBB
			mc.assign(args[0], sget(args[1], s), op.equals("sget-wide"));
			break;
		case "sput"                    : 	// vAA, field@BBBB
		case "sput-wide"               : 	// vAA, field@BBBB
		case "sput-object"             : 	// vAA, field@BBBB
		case "sput-boolean"            : 	// vAA, field@BBBB
		case "sput-byte"               : 	// vAA, field@BBBB
		case "sput-char"               : 	// vAA, field@BBBB
		case "sput-short"              : 	// vAA, field@BBBB
			if (mc.read(args[0])==null) {
				this.crashed = this.shouldStop = true;
				break;
			}
			sput(mc.read(args[0]), args[1]);
			break;
		case "invoke-virtual"          : 	// {vC, vD, vE, vF, vG}, meth@BBBB
		case "invoke-super"            : 	// {vC, vD, vE, vF, vG}, meth@BBBB
		case "invoke-direct"           : 	// {vC, vD, vE, vF, vG}, meth@BBBB
		case "invoke-static"           : 	// {vC, vD, vE, vF, vG}, meth@BBBB
		case "invoke-interface"        : 	// {vC, vD, vE, vF, vG}, meth@BBBB
		case "invoke-virtual/range"    : 	// {vCCCC .. vNNNN}, meth@BBBB
		case "invoke-super/range"      : 	// {vCCCC .. vNNNN}, meth@BBBB
		case "invoke-direct/range"     : 	// {vCCCC .. vNNNN}, meth@BBBB
		case "invoke-static/range"     : 	// {vCCCC .. vNNNN}, meth@BBBB
		case "invoke-interface/range"  : 	// {vCCCC .. vNNNN}, meth@BBBB
		{
			String[] paramRegs = s.getParamRegisters();
			List<Expression> params = new ArrayList<>();
			for (int i = 0; i < paramRegs.length; i++)
			{
				Expression exp = mc.read(paramRegs[i]);
				if (exp == null) {
					this.crashed = this.shouldStop = true;
					return;
				}
				params.add(exp);
				if (Dalvik.isWideType(exp.type)) // skip a register if this is a wide value
					i++;
			}

			APEXMethod m = app.getNonLibraryMethod(args[1]);
			if (m == null)
			{
				String returnType = args[1].substring(args[1].lastIndexOf(")")+1);

				if (APISolver.canSolve(args[1]))
				{
					APISolver.solve(args[1], params, s, mc, paramRegs, this);
				}
				else
				{
					//P.p("solve this API?? "+args[1]+". "+s.getUniqueID());
					if (!returnType.equals("V"))
						createSymbolicMethodReturn(returnType, args[1], params, s);
				}
			}
			else
			{
				MethodContext nestedMC = new MethodContext(app, m, this, params);
				if (!nestedMC.paramValid)
				{
					crashed = true;
					shouldStop = true;
					return;
				}
				methodStack.push(nestedMC);
			}
			break;
		}
		case "neg-int"                 : 	// vA, vB
			mc.assign(args[0], Arithmetic.sub(Expression.newLiteral("I", "0"), mc.read(args[1]), "I"));
			break;
		case "not-int"                 : 	// vA, vB
			mc.assign(args[0], Logic.not(mc.read(args[1])));
			break;
		case "neg-long"                : 	// vA, vB
			mc.assign(args[0], Arithmetic.sub(Expression.newLiteral("J", "0"), mc.read(args[1]), "J"));
			break;
		case "not-long"                : 	// vA, vB
			mc.assign(args[0], Logic.not(mc.read(args[1])));
			break;
		case "neg-float"               : 	// vA, vB
			mc.assign(args[0], Arithmetic.sub(Expression.newLiteral("F", "0"), mc.read(args[1]), "F"));
			break;
		case "neg-double"              : 	// vA, vB
			mc.assign(args[0], Arithmetic.sub(Expression.newLiteral("D", "0"), mc.read(args[1]), "D"));
			break;
		case "int-to-long"             : 	// vA, vB
			mc.assign(args[0], Arithmetic.cast(mc.read(args[1]), "I", "J"));
			break;
		case "int-to-float"            : 	// vA, vB
			mc.assign(args[0], Arithmetic.cast(mc.read(args[1]), "I", "F"));
			break;
		case "int-to-double"           : 	// vA, vB
			mc.assign(args[0], Arithmetic.cast(mc.read(args[1]), "I", "D"));
			break;
		case "long-to-int"             : 	// vA, vB
			mc.assign(args[0], Arithmetic.cast(mc.read(args[1]), "J", "I"));
			break;
		case "long-to-float"           : 	// vA, vB
			mc.assign(args[0], Arithmetic.cast(mc.read(args[1]), "J", "F"));
			break;
		case "long-to-double"          : 	// vA, vB
			mc.assign(args[0], Arithmetic.cast(mc.read(args[1]), "J", "D"));
			break;
		case "float-to-int"            : 	// vA, vB
			mc.assign(args[0], Arithmetic.cast(mc.read(args[1]), "F", "I"));
			break;
		case "float-to-long"           : 	// vA, vB
			mc.assign(args[0], Arithmetic.cast(mc.read(args[1]), "F", "J"));
			break;
		case "float-to-double"         : 	// vA, vB
			//P.p(s.getUniqueID());
			mc.assign(args[0], Arithmetic.cast(mc.read(args[1]), "F", "D"));
			break;
		case "double-to-int"           : 	// vA, vB
			mc.assign(args[0], Arithmetic.cast(mc.read(args[1]), "D", "I"));
			break;
		case "double-to-long"          : 	// vA, vB
			mc.assign(args[0], Arithmetic.cast(mc.read(args[1]), "D", "J"));
			break;
		case "double-to-float"         : 	// vA, vB
			mc.assign(args[0], Arithmetic.cast(mc.read(args[1]), "D", "F"));
			break;
		case "int-to-byte"             : 	// vA, vB
			mc.assign(args[0], Arithmetic.cast(mc.read(args[1]), "I", "B"));
			break;
		case "int-to-char"             : 	// vA, vB
			mc.assign(args[0], Arithmetic.cast(mc.read(args[1]), "I", "C"));
			break;
		case "int-to-short"            : 	// vA, vB
			mc.assign(args[0], Arithmetic.cast(mc.read(args[1]), "I", "S"));
			break;
		case "add-int"                 : 	// vAA, vBB, vCC
			mc.assign(args[0], Arithmetic.add(mc.read(args[1]), mc.read(args[2]), "I"));
			break;
		case "sub-int"                 : 	// vAA, vBB, vCC
			mc.assign(args[0], Arithmetic.sub(mc.read(args[1]), mc.read(args[2]), "I"));
			break;
		case "mul-int"                 : 	// vAA, vBB, vCC
			mc.assign(args[0], Arithmetic.mul(mc.read(args[1]), mc.read(args[2]), "I"));
			break;
		case "div-int"                 : 	// vAA, vBB, vCC
		{
			Expression divResult = Arithmetic.div(mc.read(args[1]), mc.read(args[2]), "I");
			if (divResult != null)
				mc.assign(args[0], divResult);
			else
			{
				shouldStop = true;
				crashed = true;
			}
			break;
		}
		case "rem-int"                 : 	// vAA, vBB, vCC
		{
			Expression remResult = Arithmetic.rem(mc.read(args[1]), mc.read(args[2]), "I");
			if (remResult != null)
				mc.assign(args[0], remResult);
			else
			{
				shouldStop = true;
				crashed = true;
			}
			break;
		}
		case "and-int"                 : 	// vAA, vBB, vCC
			mc.assign(args[0], Logic.and(mc.read(args[1]), mc.read(args[2])));
			break;
		case "or-int"                  : 	// vAA, vBB, vCC
			mc.assign(args[0], Logic.or(mc.read(args[1]), mc.read(args[2])));
			break;
		case "xor-int"                 : 	// vAA, vBB, vCC
			mc.assign(args[0], Logic.xor(mc.read(args[1]), mc.read(args[2])));
			break;
		case "shl-int"                 : 	// vAA, vBB, vCC
			mc.assign(args[0], Logic.shl(mc.read(args[1]), mc.read(args[2])));
			break;
		case "shr-int"                 : 	// vAA, vBB, vCC
			mc.assign(args[0], Logic.shr(mc.read(args[1]), mc.read(args[2])));
			break;
		case "ushr-int"                : 	// vAA, vBB, vCC
			mc.assign(args[0], Logic.ushr(mc.read(args[1]), mc.read(args[2])));
			break;
		case "add-long"                : 	// vAA, vBB, vCC
			mc.assign(args[0], Arithmetic.add(mc.read(args[1]), mc.read(args[2]), "J"));
			break;
		case "sub-long"                : 	// vAA, vBB, vCC
			mc.assign(args[0], Arithmetic.sub(mc.read(args[1]), mc.read(args[2]), "J"));
			break;
		case "mul-long"                : 	// vAA, vBB, vCC
			mc.assign(args[0], Arithmetic.mul(mc.read(args[1]), mc.read(args[2]), "J"));
			break;
		case "div-long"                : 	// vAA, vBB, vCC
		{
			Expression divResult = Arithmetic.div(mc.read(args[1]), mc.read(args[2]), "J");
			if (divResult != null)
				mc.assign(args[0], divResult);
			else
			{
				shouldStop = true;
				crashed = true;
			}
			break;
		}
		case "rem-long"                : 	// vAA, vBB, vCC
		{
			Expression remResult = Arithmetic.rem(mc.read(args[1]), mc.read(args[2]), "J");
			if (remResult != null)
				mc.assign(args[0], remResult);
			else
			{
				shouldStop = true;
				crashed = true;
			}
			break;
		}
		case "and-long"                : 	// vAA, vBB, vCC
			mc.assign(args[0], Logic.and(mc.read(args[1]), mc.read(args[2])));
			break;
		case "or-long"                 : 	// vAA, vBB, vCC
			mc.assign(args[0], Logic.or(mc.read(args[1]), mc.read(args[2])));
			break;
		case "xor-long"                : 	// vAA, vBB, vCC
			mc.assign(args[0], Logic.xor(mc.read(args[1]), mc.read(args[2])));
			break;
		case "shl-long"                : 	// vAA, vBB, vCC
			mc.assign(args[0], Logic.shl(mc.read(args[1]), mc.read(args[2])));
			break;
		case "shr-long"                : 	// vAA, vBB, vCC
			mc.assign(args[0], Logic.shr(mc.read(args[1]), mc.read(args[2])));
			break;
		case "ushr-long"               : 	// vAA, vBB, vCC
			mc.assign(args[0], Logic.ushr(mc.read(args[1]), mc.read(args[2])));
			break;
		case "add-float"               : 	// vAA, vBB, vCC
			mc.assign(args[0], Arithmetic.add(mc.read(args[1]), mc.read(args[2]), "F"));
			break;
		case "sub-float"               : 	// vAA, vBB, vCC
			mc.assign(args[0], Arithmetic.sub(mc.read(args[1]), mc.read(args[2]), "F"));
			break;
		case "mul-float"               : 	// vAA, vBB, vCC
			mc.assign(args[0], Arithmetic.mul(mc.read(args[1]), mc.read(args[2]), "F"));
			break;
		case "div-float"               : 	// vAA, vBB, vCC
		{
			Expression divResult = Arithmetic.div(mc.read(args[1]), mc.read(args[2]), "F");
			if (divResult != null)
				mc.assign(args[0], divResult);
			else
			{
				shouldStop = true;
				crashed = true;
			}
			break;
		}
		case "rem-float"               : 	// vAA, vBB, vCC
		{
			Expression remResult = Arithmetic.rem(mc.read(args[1]), mc.read(args[2]), "F");
			if (remResult != null)
				mc.assign(args[0], remResult);
			else
			{
				shouldStop = true;
				crashed = true;
			}
			break;
		}
		case "add-double"              : 	// vAA, vBB, vCC
			mc.assign(args[0], Arithmetic.add(mc.read(args[1]), mc.read(args[2]), "D"));
			break;
		case "sub-double"              : 	// vAA, vBB, vCC
			mc.assign(args[0], Arithmetic.sub(mc.read(args[1]), mc.read(args[2]), "D"));
			break;
		case "mul-double"              : 	// vAA, vBB, vCC
			mc.assign(args[0], Arithmetic.mul(mc.read(args[1]), mc.read(args[2]), "D"));
			break;
		case "div-double"              : 	// vAA, vBB, vCC
		{
			Expression divResult = Arithmetic.div(mc.read(args[1]), mc.read(args[2]), "D");
			if (divResult != null)
				mc.assign(args[0], divResult);
			else
			{
				shouldStop = true;
				crashed = true;
			}
			break;
		}
		case "rem-double"              : 	// vAA, vBB, vCC
		{
			Expression remResult = Arithmetic.rem(mc.read(args[1]), mc.read(args[2]), "D");
			if (remResult != null)
				mc.assign(args[0], remResult);
			else
			{
				shouldStop = true;
				crashed = true;
			}
			break;
		}
		case "add-int/2addr"           : 	//  vA, vB
			mc.assign(args[0], Arithmetic.add(mc.read(args[0]), mc.read(args[1]), "I"));
			break;
		case "sub-int/2addr"           : 	//  vA, vB
			mc.assign(args[0], Arithmetic.sub(mc.read(args[0]), mc.read(args[1]), "I"));
			break;
		case "mul-int/2addr"           : 	//  vA, vB
			mc.assign(args[0], Arithmetic.mul(mc.read(args[0]), mc.read(args[1]), "I"));
			break;
		case "div-int/2addr"           : 	//  vA, vB
		{
			Expression divResult = Arithmetic.div(mc.read(args[0]), mc.read(args[1]), "I");
			if (divResult != null)
				mc.assign(args[0], divResult);
			else
			{
				shouldStop = true;
				crashed = true;
			}
			break;
		}
		case "rem-int/2addr"           : 	//  vA, vB
		{
			Expression remResult = Arithmetic.rem(mc.read(args[0]), mc.read(args[1]), "I");
			if (remResult != null)
				mc.assign(args[0], remResult);
			else
			{
				shouldStop = true;
				crashed = true;
			}
			break;
		}
		case "and-int/2addr"           : 	//  vA, vB
			mc.assign(args[0], Logic.and(mc.read(args[0]), mc.read(args[1])));
			break;
		case "or-int/2addr"            : 	//  vA, vB
			mc.assign(args[0], Logic.or(mc.read(args[0]), mc.read(args[1])));
			break;
		case "xor-int/2addr"           : 	//  vA, vB
			mc.assign(args[0], Logic.xor(mc.read(args[0]), mc.read(args[1])));
			break;
		case "shl-int/2addr"           : 	//  vA, vB
			mc.assign(args[0], Logic.shl(mc.read(args[0]), mc.read(args[1])));
			break;
		case "shr-int/2addr"           : 	//  vA, vB
			mc.assign(args[0], Logic.shr(mc.read(args[0]), mc.read(args[1])));
			break;
		case "ushr-int/2addr"          : 	// vA, vB
			mc.assign(args[0], Logic.ushr(mc.read(args[0]), mc.read(args[1])));
			break;
		case "add-long/2addr"          : 	// vA, vB
			mc.assign(args[0], Arithmetic.add(mc.read(args[0]), mc.read(args[1]), "J"));
			break;
		case "sub-long/2addr"          : 	// vA, vB 
			mc.assign(args[0], Arithmetic.sub(mc.read(args[0]), mc.read(args[1]), "J"));
			break;
		case "mul-long/2addr"          : 	// vA, vB 
			mc.assign(args[0], Arithmetic.mul(mc.read(args[0]), mc.read(args[1]), "J"));
			break;
		case "div-long/2addr"          : 	// vA, vB
		{
			Expression divResult = Arithmetic.div(mc.read(args[0]), mc.read(args[1]), "J");
			if (divResult != null)
				mc.assign(args[0], divResult);
			else
			{
				shouldStop = true;
				crashed = true;
			}
			break;
		}
		case "rem-long/2addr"          : 	// vA, vB
		{
			Expression remResult = Arithmetic.rem(mc.read(args[0]), mc.read(args[1]), "J");
			if (remResult != null)
				mc.assign(args[0], remResult);
			else
			{
				shouldStop = true;
				crashed = true;
			}
			break;
		}
		case "and-long/2addr"          : 	// vA, vB
			mc.assign(args[0], Logic.and(mc.read(args[0]), mc.read(args[1])));
			break;
		case "or-long/2addr"           : 	// vA, vB
			mc.assign(args[0], Logic.or(mc.read(args[0]), mc.read(args[1])));
			break;
		case "xor-long/2addr"          : 	// vA, vB
			mc.assign(args[0], Logic.xor(mc.read(args[0]), mc.read(args[1])));
			break;
		case "shl-long/2addr"          : 	// vA, vB
			mc.assign(args[0], Logic.shl(mc.read(args[0]), mc.read(args[1])));
			break;
		case "shr-long/2addr"          : 	// vA, vB
			mc.assign(args[0], Logic.shr(mc.read(args[0]), mc.read(args[1])));
			break;
		case "ushr-long/2addr"         : 	// vA, vB
			mc.assign(args[0], Logic.ushr(mc.read(args[0]), mc.read(args[1])));
			break;
		case "add-float/2addr"         : 	// vA, vB
			mc.assign(args[0], Arithmetic.add(mc.read(args[0]), mc.read(args[1]), "F"));
			break;
		case "sub-float/2addr"         : 	// vA, vB
			mc.assign(args[0], Arithmetic.sub(mc.read(args[0]), mc.read(args[1]), "F"));
			break;
		case "mul-float/2addr"         : 	// vA, vB
			mc.assign(args[0], Arithmetic.mul(mc.read(args[0]), mc.read(args[1]), "F"));
			break;
		case "div-float/2addr"         : 	// vA, vB
		{
			Expression divResult = Arithmetic.div(mc.read(args[0]), mc.read(args[1]), "F");
			if (divResult != null)
				mc.assign(args[0], divResult);
			else
			{
				shouldStop = true;
				crashed = true;
			}
			break;
		}
		case "rem-float/2addr"         : 	// vA, vB
		{
			Expression remResult = Arithmetic.rem(mc.read(args[0]), mc.read(args[1]), "F");
			if (remResult != null)
				mc.assign(args[0], remResult);
			else
			{
				shouldStop = true;
				crashed = true;
			}
			break;
		}
		case "add-double/2addr"        : 	// vA, vB
			mc.assign(args[0], Arithmetic.add(mc.read(args[0]), mc.read(args[1]), "D"));
			break;
		case "sub-double/2addr"        : 	// vA, vB
			mc.assign(args[0], Arithmetic.sub(mc.read(args[0]), mc.read(args[1]), "D"));
			break;
		case "mul-double/2addr"        : 	// vA, vB
			mc.assign(args[0], Arithmetic.mul(mc.read(args[0]), mc.read(args[1]), "D"));
			break;
		case "div-double/2addr"        : 	// vA, vB
		{
			Expression divResult = Arithmetic.div(mc.read(args[0]), mc.read(args[1]), "D");
			if (divResult != null)
				mc.assign(args[0], divResult);
			else
			{
				shouldStop = true;
				crashed = true;
			}
			break;
		}
		case "rem-double/2addr"        : 	// vA, vB
		{
			Expression remResult = Arithmetic.rem(mc.read(args[0]), mc.read(args[1]), "D");
			if (remResult != null)
				mc.assign(args[0], remResult);
			else
			{
				shouldStop = true;
				crashed = true;
			}
			break;
		}
		case "add-int/lit16"           : 	// vA, vB, #+CCCC
			mc.assign(args[0], Arithmetic.add(mc.read(args[1]), Expression.newLiteral("I", args[2]), "I"));
			break;
		case "rsub-int"                : 	// vA, vB, #+CCCC
			mc.assign(args[0], Arithmetic.sub(Expression.newLiteral("I", args[2]), mc.read(args[1]), "I"));
			break;
		case "mul-int/lit16"           : 	// vA, vB, #+CCCC
			mc.assign(args[0], Arithmetic.mul(mc.read(args[1]), Expression.newLiteral("I", args[2]), "I"));
			break;
		case "div-int/lit16"           : 	// vA, vB, #+CCCC
		{
			Expression divResult = Arithmetic.div(mc.read(args[1]), Expression.newLiteral("I", args[2]), "I");
			if (divResult != null)
				mc.assign(args[0], divResult);
			else
			{
				shouldStop = true;
				crashed = true;
			}
			break;
		}
		case "rem-int/lit16"           : 	// vA, vB, #+CCCC
		{
			Expression remResult = Arithmetic.rem(mc.read(args[1]), Expression.newLiteral("I", args[2]), "I");
			if (remResult != null)
				mc.assign(args[0], remResult);
			else
			{
				shouldStop = true;
				crashed = true;
			}
			break;
		}
		case "and-int/lit16"           : 	// vA, vB, #+CCCC
			mc.assign(args[0], Logic.and(mc.read(args[1]), Expression.newLiteral("I", args[2])));
			break;
		case "or-int/lit16"            : 	// vA, vB, #+CCCC
			mc.assign(args[0], Logic.or(mc.read(args[1]), Expression.newLiteral("I", args[2])));
			break;
		case "xor-int/lit16"           : 	// vA, vB, #+CCCC
			mc.assign(args[0], Logic.xor(mc.read(args[1]), Expression.newLiteral("I", args[2])));
			break;
		case "add-int/lit8"            : 	// vAA, vBB, #+CC
			mc.assign(args[0], Arithmetic.add(mc.read(args[1]), Expression.newLiteral("I", args[2]), "I"));
			break;
		case "rsub-int/lit8"           : 	// vAA, vBB, #+CC
			mc.assign(args[0], Arithmetic.sub(Expression.newLiteral("I", args[2]), mc.read(args[1]), "I"));
			break;
		case "mul-int/lit8"            : 	// vAA, vBB, #+CC
			mc.assign(args[0], Arithmetic.mul(mc.read(args[1]), Expression.newLiteral("I", args[2]), "I"));
			break;
		case "div-int/lit8"            : 	// vAA, vBB, #+CC
		{
			Expression divResult = Arithmetic.div(mc.read(args[1]), Expression.newLiteral("I", args[2]), "I");
			if (divResult != null)
				mc.assign(args[0], divResult);
			else {
				shouldStop = true;
				crashed = true;
			}
			break;
		}
		case "rem-int/lit8"            : 	// vAA, vBB, #+CC
		{
			Expression remResult = Arithmetic.rem(mc.read(args[1]), Expression.newLiteral("I", args[2]), "I");
			if (remResult != null)
				mc.assign(args[0], remResult);
			else
				shouldStop =  crashed = true;
			break;
		}
		case "and-int/lit8"            : 	// vAA, vBB, #+CC
			mc.assign(args[0], Logic.and(mc.read(args[1]), Expression.newLiteral("I", args[2])));
			break;
		case "or-int/lit8"             : 	// vAA, vBB, #+CC
			mc.assign(args[0], Logic.or(mc.read(args[1]), Expression.newLiteral("I", args[2])));
			break;
		case "xor-int/lit8"            : 	// vAA, vBB, #+CC
			mc.assign(args[0], Logic.xor(mc.read(args[1]), Expression.newLiteral("I", args[2])));
			break;
		case "shl-int/lit8"            : 	// vAA, vBB, #+CC
			if (mc.read(args[1])==null) {
				this.crashed = this.shouldStop = true;
				break;
			}
			mc.assign(args[0], Logic.shl(mc.read(args[1]), Expression.newLiteral("I", args[2])));
			break;
		case "shr-int/lit8"            : 	// vAA, vBB, #+CC
			mc.assign(args[0], Logic.shr(mc.read(args[1]), Expression.newLiteral("I", args[2])));
			break;
		case "ushr-int/lit8"           : 	// vAA, vBB, #+CC
			mc.assign(args[0], Logic.ushr(mc.read(args[1]), Expression.newLiteral("I", args[2])));
			break;
			
		case "invoke-polymorphic"      : 	// {vC, vD, vE, vF, vG}, meth@BBBB, proto@HHHH
		case "invoke-polymorphic/range": 	// {vCCCC .. vNNNN}, meth@BBBB, proto@HHHH
		case "invoke-custom"           : 	// {vC, vD, vE, vF, vG}, call_site@BBBB
		case "invoke-custom/range"     : 	// {vCCCC .. vNNNN}, call_site@BBBB
		case "const-method-handle"     : 	// vAA, method_handle@BBBB
		case "const-method-type"       : 	// vAA, proto@BBBB
			P.p("TODO: handle this statement: " + op+". Hit Enter to continue.");
			P.pause();
			break;
		}
		
//		//NOTE: experimental
//		if (s.smali.startsWith("and-int/2addr v0, v2") && s.index==14) {
//			P.p("v0 = "+mc.read("v0").toString());
//			P.p("v2 = "+mc.read("v2").toString());
//			P.pause();
//		}

		//Note: this is to break out of loops that take too many iterations
//		if (s.smali.startsWith("const/16") && s.index==358) {
//			P.p("v0 = "+mc.read("v0").toString());
//			P.p("v2 = "+mc.read("v2").toString());
//			P.pause("-------");
//		}
		if (s.smali.startsWith("if")) {
//			if (s.index==210 || s.index==203 || s.index==221) {
//				P.p(s.index+" "+s.smali);
//			}
			if (!prevCond.containsKey(s))
				prevCond.put(s, cond);
			else {
				Expression prev = prevCond.get(s);
				String label = s.m.signature+s.block.getLabelString();
				if (prev.equals(cond)) {
					// if condition is the same as last time we visited this if statement
					// we are going to remember it
					// if this happens 10 times in a row
					// we are going to skip executions
					//P.p("[same cond] "+cond.toString()+" "+s.smali+" "+s.index+" "+s.m.signature);
					if (branchCounter.getOrDefault(label, 0) > 2) {
						int jumpIndex = mc.m.getBlock(args[args.length-1]).getFirstStatement().index;
						int flowIndex = s.index+1;
						APEXBlock flowBlock = mc.m.statements.get(flowIndex).block;
						APEXBlock jumpBlock = mc.m.statements.get(jumpIndex).block;
						// if both branches have been visited, we can safely exit (TODO: can we???)
						if (mc.visitedBlocks.contains(flowBlock) && mc.visitedBlocks.contains(jumpBlock)) {
							if (mc.m.signature.endsWith(")V"))
								methodStack.pop();
							else
								shouldStop = true;
							return;
						}
						mc.stmtIndex = mc.stmtIndex==flowIndex ? jumpIndex : flowIndex;
						return;
					}
					branchCounter.put(label, branchCounter.getOrDefault(label, 0)+1);
				}
				else {
					// if condition is different from last time
					prevCond.put(s, cond);
					//branchCounter.put(label, 0);
				}
					
			}
		}
	}
	
	private void SymbolicPathing(MethodContext mc, APEXStatement s, String flow, String jump, Expression left, Expression right, int jumpIndex, boolean flowThrough)
	{
		int index = -1;
		if (s.index==index) {
			P.p("[sym path] "+s.smali);
		}
		Expression flowCond = Expression.newCondition(flow, left, right);
		flowCond.note = s.getUniqueID();
		Expression jumpCond = Expression.newCondition(jump, left, right);
		jumpCond.note = s.getUniqueID();
		
		// NOTE: if flowThrough==true, then this VM will flowThough and cloned VM will jump
		//					otherwise, this VM jumps and clone will flow through otherwise
		
		APEXStatement flowS = mc.m.statements.get(s.index+1);
		APEXStatement jumpS = mc.m.statements.get(jumpIndex);
		String flowLabel = flowS.m.signature + flowS.block.getLabelString();
		String jumpLabel = jumpS.m.signature + jumpS.block.getLabelString();
		
		boolean canFlow = branchVisitTimes.getOrDefault(flowLabel, 0) < VM.maxBranchVisitTime;
		boolean canJump = branchVisitTimes.getOrDefault(jumpLabel, 0) < VM.maxBranchVisitTime;
		
		if (!canFlow && !canJump) { // both branches have exceeded max visit count
			if (s.index==index) P.p("[sym path] can't go to either branch");
			this.shouldStop = true;
		}
		else if (!canFlow) { // flow branch has exceeded max visit count, so we can only jump
			pathCondition.add(jumpCond);
			mc.stmtIndex = jumpIndex;
			branchVisitTimes.put(jump, branchVisitTimes.getOrDefault(jumpLabel, 0)+1);
			if (s.index==index) P.p("[sym path] jump without cloning. "+s.getUniqueID()+" "+s.smali);
		}
		else if (!canJump) { // jump branch exceeded visit limit, can only flow
			pathCondition.add(flowCond);
			mc.stmtIndex = s.index+1;
			branchVisitTimes.put(flowLabel, branchVisitTimes.getOrDefault(flowLabel, 0)+1);
			if (s.index==index) P.p("[sym path] flow without cloning. "+s.getUniqueID()+" "+s.smali);
		}
		else {
			// now we can both jump and flow, choose one path and let another VM to do the other
			
			Expression thisCond = flowThrough? flowCond : jumpCond;
			Expression otherCond = flowThrough? jumpCond : flowCond;
			int thisNextStmtIndex = flowThrough? s.index+1 : jumpIndex;
			int otherNextStmtIndex = flowThrough? jumpIndex : s.index+1;
			String thisNextLabel = flowThrough? flowLabel : jumpLabel;
			
			branchVisitTimes.put(thisNextLabel, branchVisitTimes.getOrDefault(thisNextLabel, 0)+1);
			if (!visitedBranches.contains(s)) {
				visitedBranches.add(s);
				VM otherVM = this.clone();
				otherVM.pathCondition.add(otherCond);
				otherVM.methodStack.peek().stmtIndex = otherNextStmtIndex;
				otherVMs.add(otherVM);
				if (s.index==index)
					P.p("cloning VM at "+s.getUniqueID());
				VM.pathCount++;
			}
			pathCondition.add(thisCond);
			mc.stmtIndex = thisNextStmtIndex;
		}
		
	}
	
	private void SymbolicPathing(MethodContext mc, APEXStatement s, String flow, String jump, Expression left, Expression right, int jumpIndex)
	{
		SymbolicPathing(mc, s, flow, jump, left, right, jumpIndex, false);
	}
	
	public void createSymbolicMethodReturn(String returnType, String invokeSig, List<Expression> params, APEXStatement s)
	{
		recentResult = new Expression(returnType, "return");
		recentResult.isSymbolic = true;
		recentResult.add(new Expression(invokeSig));
		for (Expression param : params)
		{
			if (param != null) {
				recentResult.add(param.clone());
			}
		}
		if (!Dalvik.isPrimitiveType(returnType))
		{
			if (Dalvik.isArrayType(returnType))
				recentResult = this.createNewArray(returnType, recentResult.toString(), null, s.getUniqueID()+" "+s.smali).reference;
			else
				recentResult = this.createNewObject(returnType, recentResult.toString(), s.getUniqueID()+" "+s.smali, true).reference;
		}
	}
	
	@Override
	public APEXObject createNewObject(String type, String root, String birth, boolean symbolic)
	{
		String id = "$obj_"+objCounter;
		objCounter++;
		APEXObject obj = new APEXObject(id, type, root, birth);
		if (symbolic)
		{
			if (this.allocateConcreteBitmap && type.equals("Landroid/graphics/Bitmap;"))
			{
				//P.p("creating symbolic bitmap "+birth);
				BitmapSolver.initConcreteBitmap(obj);
			}
			obj.isSymbolic = true;
			obj.reference.isSymbolic = true;
		}
		heap.put(id, obj);
		loadClass(type, obj.reference, false);
		return obj;
	}
	
	@Override
	public APEXArray createNewArray(String type, String root, Expression length, String birth)
	{
		String id = "$obj_"+objCounter;
		objCounter++;
		APEXArray array = new APEXArray(id, type, root, birth, length);
		heap.put(id, array);
		return array;
	}
	
	public Expression sget(String fieldSig, APEXStatement s)
	{
		return getField("$obj_0", fieldSig, s);
	}
	public void sput(Expression exp, String fieldSig)
	{
		putField(exp, "$obj_0", fieldSig);
	}
	
	public Expression getField(String objName, String fieldSig, APEXStatement s)
	{
		//P.p("[get field] " + s.getUniqueID());
		String className = fieldSig.substring(0, fieldSig.indexOf("->"));
		if (objName.contentEquals("$obj_0"))
			loadClass(className, null, false);
		Expression exp = heap.get(objName).fields.get(fieldSig);
		if (exp == null) // need to create symbolic value
		{
			//P.p("  creating symbolic field "+fieldSig);
			String name = fieldSig.substring(fieldSig.indexOf("->")+2, fieldSig.indexOf(":"));
			String type = fieldSig.substring(fieldSig.indexOf(":")+1);
			if (Dalvik.isPrimitiveType(type))
			{
				exp = Expression.newLiteral(type, objName+"."+name);
			}
			else if (Dalvik.isArrayType(type))
			{
				APEXArray arr = createNewArray(type, objName+"."+name, null, s.getUniqueID()+" "+s.smali);
				arr.isSymbolic = true;
				exp = arr.reference;
			}
			else
			{
				APEXObject obj = createNewObject(type, objName+"."+name, s.getUniqueID()+" "+s.smali, true);
				exp = obj.reference;
			}
			exp.isSymbolic = true;
			heap.get(objName).fields.put(fieldSig, exp);
		}
		return exp;
	}
	
	public void putField(Expression exp, String objName, String fieldSig)
	{
		//P.p("putField params: " +exp.toString()+" || "+objName+" || " + fieldSig);
		APEXObject obj = heap.get(objName);
		if (obj != null)
			obj.fields.put(fieldSig, exp.clone());
	}
	
	/**
	 * initiate the static field values and run the <clinit>()V method.
	 * if needed, run the default constructor
	 * */
	private void loadClass(String dexName, Expression objRef, boolean instantiate)
	{
		if (loadedClasses.contains(dexName))
			return;
		loadedClasses.add(dexName);
		APEXClass c = app.classes.get(dexName);
		if (c == null || c.isLibraryClass())
			return;
		
		//P.p("loading class " +dexName);
		for (APEXField f : c.fields.values())
		{
			String decl = f.declarations.get(0);
			if (f.modifiers.contains("static") && decl.contains(" = "))
			{
				sput(Expression.newLiteral(f.getType(), decl.substring(decl.indexOf(" = ")+3)), f.signature);
			}
		}
		
		Stack<MethodContext> temp = new Stack<>();
		while (!methodStack.isEmpty())
			temp.push(methodStack.pop());
		
		APEXMethod m = c.getMethodBySubsignature("<clinit>()V");
		if (m != null)
			execute(m);
		if (instantiate) {
			m = c.getMethodBySubsignature("<init>()V");
			if (m != null)
			{
				//P.p("executing "+m.signature);
				List<Expression> params = new ArrayList<>();
				params.add(objRef);
				execute(m, params);
			}
		}
		
		while (!temp.isEmpty())
			methodStack.push(temp.pop());
	}
	
	private boolean hasPrevCond(Expression cond)
	{
		for (Expression exp : pathCondition)
			if (exp.equals(cond))
				return true;
		return false;
	}
	

	public void print(PrintStream out)
	{
		out.println("------------------- VM Snapshot -----------");
		out.println("prev stmt: "+execLog.get(execLog.size()-1).getUniqueID());
		out.println("================ Method Stack ==========");
		for (MethodContext m : methodStack)
		{
			m.print(out);
		}
		out.println("================ Heap ==========");
		for (Map.Entry<String, APEXObject> entry : heap.entrySet())
		{
			APEXObject obj = entry.getValue();
			obj.print(out);
		}
		out.println("================ Path Condition ============");
		for (Expression cond : pathCondition)
			out.println("  "+cond.toString());
		out.println("======== Recent Result: " + (recentResult==null?"null":recentResult.toString()));
	}
	
	public void print()
	{
		print(System.out);
	}
	
	public void writeToFile(File f, boolean append)
	{
		P.p("------------------- VM Snapshot -----------", f);
		/*out.println("================ Execution Log =========");
		for (APEXStatement s : execLog)
		{
			out.println("\t"+s.getUniqueID());
		}
		out.println("================ Method Stack ==========");
		for (MethodContext m : methodStack)
		{
			m.writeToFile(out);
		}*/
		P.p("================ Heap ==========", f);
		for (Map.Entry<String, APEXObject> entry : heap.entrySet())
		{
			APEXObject obj = entry.getValue();
			obj.print(f);
/*			out.println(" ----- "+entry.getKey()+": "+obj.type);
			out.println("    *** birth: "+obj.birth);
			for (Map.Entry<String, Expression> fieldEntry : obj.fields.entrySet())
			{
				out.println("\t"+fieldEntry.getKey()+"\t=\t"+fieldEntry.getValue().toString());
			}
			if (obj.concreteBitmap!=null)
			{
				out.println("    *** Bitmap Pixel Values -----");
				for (Expression[] row : obj.concreteBitmap)
				{
					for (Expression i : row)
					{
						out.print(i.toString());
					}
					out.print("\n");
				}
			}
			else if (!obj.bitmapHistory.isEmpty())
			{
				out.println("    *** Bitmap Access History -----");
				for (int i = 0; i < obj.bitmapHistory.size(); i++)
				{
					BitmapAccess access = obj.bitmapHistory.get(i);
					out.println("\t\t"+access.action+"("+access.x.toString()+", "+access.y.toString()+(access.c==null?"":", "+access.c.toString())+")");
				}
			}
			if (obj instanceof APEXArray)
			{
				((APEXArray)obj).writeToFile(out);
			}*/
		}
		P.p("================ Path Condition ============", f);
		for (Expression cond : pathCondition)
			P.p("  "+cond.toString(), f);
		//out.println("======== Recent Result: " + (recentResult==null?"null":recentResult.toString()));
	}
	
	
	public void visualize()
	{
		//TODO
	}
}
