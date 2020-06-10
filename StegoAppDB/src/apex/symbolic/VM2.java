package apex.symbolic;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import apex.APEXApp;
import apex.bytecode_wrappers.APEXClass;
import apex.bytecode_wrappers.APEXField;
import apex.bytecode_wrappers.APEXMethod;
import apex.bytecode_wrappers.APEXStatement;
import app_analysis.common.Dirs;
import util.P;

public class VM2 extends VM_interface{
	
	public static void main(String[] args) {
		List<File> stegos = Dirs.getFiles(Dirs.Stego_Github);
		for (File apk : stegos) {
			if (!apk.getName().startsWith("kcich"))
				continue;
			APEXApp app = new APEXApp(apk);
			VM2 vm = new VM2(app);
			vm.execute(app.getMethod("Lkcich/stegoqr/QRHide;->onCreate(Landroid/os/Bundle;)V"));
		}
	}
	
	/// VM components
	public Stack<MethodContext> methodStack = new Stack<>();
	public Map<String, APEXObject> heap = new HashMap<>();
	public List<Expression> pathCondition = new ArrayList<>();
	public Expression recentResult;
	
	/// Class loader components
	private int objCounter;
	private Set<String> loadedClasses = new HashSet<>();
	
	// Runtime components
	APEXApp app;
	public boolean crashed, shouldStop;
	static class BranchChoice {
		APEXStatement s;
		int ourChoice; // the index of the next statement which was chosen
		List<Integer> otherChoices = new ArrayList<>();
		BranchChoice(APEXStatement ss, int i, int...js) {
			s = ss;
			ourChoice = i;
			for (int j : js)
				otherChoices.add(j);
		}
	}
	List<BranchChoice> branchChoices = new ArrayList<>();
	
	public VM2(APEXApp app) {
		this.app = app;
	}
	
	public void go(APEXMethod m) {
		execute(m);
	}
	
	public void executeStatement(APEXStatement s) {
		if (s.index==0)
			P.p("----- "+s.m.signature+" ----");
		P.p(String.format("%4d", s.index)+"   "+s.smali);
		if (s.isInvokeStmt()) {
			APEXMethod invokeM = app.getNonLibraryMethod(s.getInvokeSignature());
			if (invokeM != null)
				execute(invokeM);
		}
		else if (s.isIfStmt()) {
			String[] args = s.getArguments();
			int jumpIndex = s.m.getBlock(args[args.length-1]).getFirstStatement().index;
			branchChoices.add(new BranchChoice(s, s.index+1, jumpIndex));
		}
	}
	
	public void execute(APEXMethod m)
	{
		execute(m, null);
	}
	
	public void execute(APEXMethod m, List<Expression> params)
	{
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
				methodStack.pop();
				continue;
			}
			APEXStatement s = mc.m.statements.get(mc.stmtIndex++);
			executeStatement(s);
			if (!continuous) {
				P.pause();
				P.p("====== Hit Enter to Continue Execution ======");
			}
		}
	}
	
	private void getReady(APEXMethod m, List<Expression> params)
	{
		MethodContext mc = new MethodContext(app, m, this, params);
		methodStack.push(mc);
	}
	
	@Override
	public APEXArray createNewArray(String type, String root, Expression length, String birth)
	{
		String id = "$obj_"+objCounter++;
		APEXArray array = new APEXArray(id, type, root, birth, length);
		heap.put(id, array);
		return array;
	}
	
	@Override
	public APEXObject createNewObject(String type, String root, String birth, boolean symbolic) {
		String id = "$obj_"+objCounter++;
		APEXObject obj = new APEXObject(id, type, root, birth);
		if (symbolic)
			obj.reference.isSymbolic = obj.isSymbolic = true;
		heap.put(id, obj);
		loadClass(type, obj.reference, false);
		return obj;
	}
	
	private void loadClass(String dexName, Expression objRef, boolean instantiate)
	{
		if (loadedClasses.contains(dexName))
			return;
		loadedClasses.add(dexName);
		APEXClass c = app.classes.get(dexName);
		if (c == null || c.isLibraryClass())
			return;
		
		for (APEXField f : c.fields.values()) {
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
	
	public void sput(Expression exp, String fieldSig)
	{
		putField(exp, "$obj_0", fieldSig);
	}
	
	private void putField(Expression exp, String objName, String fieldSig)
	{
		heap.get(objName).fields.put(fieldSig, exp.clone());
	}
}
