package apex.symbolic.solver.api;

import java.util.List;

import apex.bytecode_wrappers.APEXStatement;
import apex.symbolic.APEXObject;
import apex.symbolic.Expression;
import apex.symbolic.MethodContext;
import apex.symbolic.VM;
import util.P;

public abstract class SolverInterface {

	
	public abstract void solve(String invokeSig, List<Expression> params, APEXStatement s, MethodContext mc, String[] paramRegs, VM vm);

}
