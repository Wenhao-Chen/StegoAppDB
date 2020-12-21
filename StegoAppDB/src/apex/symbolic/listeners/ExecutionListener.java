package apex.symbolic.listeners;

import apex.bytecode_wrappers.APEXStatement;
import apex.symbolic.MethodContext;
import apex.symbolic.VM;

public interface ExecutionListener {

	public abstract void preStatementExecution(VM vm, MethodContext mc, APEXStatement s);
	public abstract void postStatementExecution(VM vm, MethodContext mc, APEXStatement s);
}
