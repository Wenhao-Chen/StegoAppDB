package apex.symbolic.listeners;

import java.util.List;

import apex.bytecode_wrappers.APEXStatement;
import apex.symbolic.MethodContext;
import apex.symbolic.VM;

public interface OpListener {

	public abstract void beforeOp(VM vm, MethodContext mc, APEXStatement s, List<String> inArgs, String outArg);
	public abstract void afterOp(VM vm, MethodContext mc, APEXStatement s, List<String> inArgs, String outArg);
}
