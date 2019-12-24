package apex.symbolic.solver.api;

import java.util.Arrays;
import java.util.List;

import apex.bytecode_wrappers.APEXMethod;
import apex.bytecode_wrappers.APEXStatement;
import apex.symbolic.APEXObject;
import apex.symbolic.Expression;
import apex.symbolic.MethodContext;
import apex.symbolic.VM;
import util.P;

public class ThreadSolver extends SolverInterface{

	@Override
	public void solve(String invokeSig, List<Expression> params, APEXStatement s, MethodContext mc, String[] paramRegs, VM vm)
	{
		if (invokeSig.equals("Ljava/lang/Thread;-><init>(Ljava/lang/Runnable;)V"))
		{
			APEXObject thread = vm.heap.get(params.get(0).getObjID());
			thread.RunnableRef = params.get(1).clone();
			//vm.heap.get(thread.RunnableRef.getObjID()).print();
			//P.pause();
		}
		else if (invokeSig.equals("Ljava/lang/Thread;->start()V") || invokeSig.equals("Ljava/lang/Thread;->run()V"))
		{
			APEXObject thread = vm.heap.get(params.get(0).getObjID());
			if (thread.RunnableRef != null)
			{
				//vm.heap.get(thread.RunnableRef.getObjID()).print();
				//P.pause();
				// find the run() method from the Runnable object
				String methodSig = thread.RunnableRef.type+"->run()V";
				APEXMethod nestedM = vm.app.getNonLibraryMethod(methodSig);
				if (nestedM != null)
				{
					//P.p("redirecting Thread.start()/run() to " + methodSig);
					MethodContext nestedMC = new MethodContext(vm.app, nestedM, vm, Arrays.asList(thread.RunnableRef.clone()));
					vm.methodStack.push(nestedMC);
				}
			}
		}
		else if (invokeSig.endsWith("->runOnUiThread(Ljava/lang/Runnable;)V"))
		{
			// this signature pattern seems to cover a lot of different APIs
			// including some 3rd party ones.
			// To get the runnable object, read from the last of the list
			// because some of such APIs are static methods
			Expression runnableRef = params.get(params.size()-1);
			String methodSig = runnableRef.type+"->run()V";
			APEXMethod nestedM = vm.app.getNonLibraryMethod(methodSig);
			if (nestedM != null)
			{
				//P.p("redirecting Activity.runOnUiThread(Runnable) to " + methodSig);
				//P.pause();
				MethodContext nestedMC = new MethodContext(vm.app, nestedM, vm, Arrays.asList(runnableRef.clone()));
				vm.methodStack.push(nestedMC);
			}
		}
		else
		{
			P.p("Forgot about this API?? " + invokeSig);
			P.pause();
		}
	}

}
