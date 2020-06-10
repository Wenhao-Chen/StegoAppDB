package apex.bytecode_wrappers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import apex.symbolic.Expression;


public class APEXStatement {

	public String smali, smali_backup;
	public int index;
	
	public APEXMethod m;
	public APEXBlock block;
	
	List<String> instrumented_before, instrumented_after;
	
	public DebugInfo debugInfo;
	public class DebugInfo {
		boolean inTryBlock;
		boolean prologue = false;
		boolean isFirstStmtOfBlock = false;
		int lineNumber = -1;
		String tryStartLabel, tryEndLabel;
		ArrayList<String> blockLabels = new ArrayList<String>();
		ArrayList<String> comments = new ArrayList<String>();
		public ArrayList<String> otherStuff = new ArrayList<String>();
		ArrayList<String> catchInfo = new ArrayList<>();
		String catchAllInfo;
	}
	
	public APEXStatement(String line, int index, APEXMethod m)
	{
		smali = line.trim();
		smali_backup = smali;
		this.index = index;
		this.m = m;
		debugInfo = new DebugInfo();
	}
	
	public String getOpcode()
	{
		int index = smali.indexOf(" ");
		return index==-1?smali:smali.substring(0, index);
	}
	
	// some statements have # following it
	public String[] getArguments()
	{
		String op = getOpcode();
		String args = smali.substring(smali.indexOf(op)+op.length());
		args = args.trim();
		if (args.equals(""))	// Example: return-void
			return new String[0];
		if (args.startsWith("{"))	// Example: invoke-virtual {v1, v2, v3}, Lcom/example/A;->M(II)V
		{							// args[0] = {v1,v2,v3}, args[1]=Lcom/example/A;->M(II)V
			String firstArg = args.substring(0, args.indexOf("}")+1);
			String secondArg = args.substring(args.indexOf(firstArg)+firstArg.length()+2);
			return new String[] {firstArg, secondArg};	// they (invoke and filled-new-array)always have 2 arguments
		}
		if (op.startsWith("const-string")) // constant string might contain comma, so shouldn't use split here
		{
			String firstArg = args.substring(0, args.indexOf(", "));
			String secondArg = args.substring(args.indexOf(", ")+2);
			return new String[] {firstArg, secondArg};
		}
		return args.split(", ");
	}
	
	public String[] getParamRegisters()
	{
		String[] args = getArguments();
		if (args.length<1 || !args[0].startsWith("{") || args[0].equals("{}"))
			return new String[0];
		String regs = args[0].substring(1, args[0].length()-1);
		if (regs.contains(" .. "))
		{
			String left = regs.substring(0, regs.indexOf(" .. "));
			String right = regs.substring(regs.indexOf(" .. ")+4);
			int from = Integer.parseInt(left.substring(1));
			int to = Integer.parseInt(right.substring(1));
			if (left.charAt(0)=='v' && right.charAt(0)=='p')
			{
				int locals = m.getLocalRegisterCount();
				to += locals;
				String[] res = new String[to-from+1];
				for (int i = from; i <= to; i++)
					res[i-from] = (i<locals?"v"+i:"p"+(i-locals));
				return res;
			}
			else
			{
				char prefix = left.charAt(0);
				String[] res = new String[to-from+1];
				for (int i = from; i <= to; i++)
					res[i-from] = prefix+""+i;
				return res;
			}
		}
		else
		{
			return regs.split(", ");
		}
	}
	
	public String getCommentAtEndOfLine()
	{
		String s = smali;
		if (s.contains("\""))
			s = s.substring(s.lastIndexOf("\"")+1);
		s = s.substring(s.indexOf("#")+1).trim();
		return s;
	}
	
	public String getUniqueID()
	{
		return m.signature + ":" + index;
	}
	
	public boolean hasBlockLabel()
	{
		return !debugInfo.blockLabels.isEmpty();
	}
	
	public boolean isReturnStmt()
	{
		return (getOpcode().startsWith("return"));
	}
	
	public boolean isThrowStmt()
	{
		return (getOpcode().startsWith("throw"));
	}
	
	public boolean isIfStmt()
	{
		return (getOpcode().startsWith("if"));
	}
	
	public boolean isSwitchStmt()
	{
		String opcode = getOpcode();
		return (opcode.equals("packed-switch") || opcode.equals("sparse-switch"));
	}
	
	public boolean isGotoStmt()
	{
		return (getOpcode().startsWith("goto"));
	}
	
	public boolean isInvokeStmt()
	{
		return (getOpcode().startsWith("invoke"));
	}
	
	public String getInvokeSignature()
	{
		if (!this.isInvokeStmt())
			return null;
		return smali.substring(smali.lastIndexOf(", ")+2);
	}
	
	public APEXStatement getGotoTargetStmt()
	{
		if (!isGotoStmt())
			return null;
		String targetLabel = smali.substring(smali.indexOf(":"));
		return m.getBlock(targetLabel).getFirstStatement();
	}
	
	public APEXStatement getIfJumpTargetStmt()
	{
		if (!isIfStmt())
			return null;
		String targetLabel = smali.substring(smali.indexOf(":"));
		return m.getBlock(targetLabel).getFirstStatement();
	}
	
	public APEXStatement getFlowThroughStmt()
	{
		if (index < m.statements.size()-1)
			return m.statements.get(index+1);
		return null;
	}
	
	public Map<Integer, String> getSwitchMap()
	{
		if (!isSwitchStmt())
			return null;
		String dataLabel = getArguments()[1];
		ArrayList<String> data = m.getSupplementalData(dataLabel);
		Map<Integer, String> result = new HashMap<>();
		String op = getOpcode();
		if (op.equals("packed-switch"))
		{
			int caseNumber = 0;
			for (String s : data)
			{
				if (s.startsWith("    .packed-switch"))
				{
					String baseNumber = s.substring(s.lastIndexOf(" ")+1).replace("0x", "");
					caseNumber = Integer.parseInt(baseNumber, 16);
				}
				else if (s.startsWith("        :"))
				{
					result.put(caseNumber++, s.trim());
				}
			}
		}
		else
		{
			for (String s : data)
			{
				if (!s.contains(" -> "))
					continue;
				String entry = s.trim();
				int caseNumber = Integer.parseInt(entry.substring(0, entry.indexOf(" -> ")).replace("0x", ""), 16);
				String targetLabel = entry.substring(entry.indexOf(" -> ")+4);
				result.put(caseNumber, targetLabel);
			}
		}
		return result;
	}
	
	public List<String> getInstrumentedBody()
	{
		List<String> res = new ArrayList<>();
		
		if (debugInfo.prologue)
			res.add("    .prologue");
		
		if (debugInfo.lineNumber > 0)
			res.add("    .line " + debugInfo.lineNumber);
		res.addAll(debugInfo.otherStuff);
		
		if (debugInfo.isFirstStmtOfBlock)
			res.addAll(debugInfo.blockLabels);
		
		if (debugInfo.tryStartLabel != null)
			res.add(debugInfo.tryStartLabel);
		
		if (instrumented_before != null)
			res.addAll(instrumented_before);
		
		res.add("    "+smali);
		
		if (debugInfo.tryEndLabel != null)
			res.add(debugInfo.tryEndLabel);
		if (debugInfo.catchInfo != null)
			res.addAll(debugInfo.catchInfo);
		if (debugInfo.catchAllInfo != null)
			res.add(debugInfo.catchAllInfo);
		
		if (instrumented_after != null)
			res.addAll(instrumented_after);
		
		return res;
	}
	
	public void instrumentBefore(String stmt)
	{
		if (instrumented_before == null)
			instrumented_before = new ArrayList<>();
		instrumented_before.add(stmt);
	}
	
	public void instrumentAfter(String stmt)
	{
		if (instrumented_after == null)
			instrumented_after = new ArrayList<>();
		instrumented_after.add(stmt);
	}
	
	public String toDotGraphString()
	{
		return index + "\t"  + smali.replace("\\", "\\\\").replace("\"", "\\\"");
	}
	
	public void addDebugInfo(String line)
	{
		if (line.equals("    .prologue"))
		{
			debugInfo.prologue = true;
		}
		else if (line.startsWith("    .line "))
		{
			try
			{
				debugInfo.lineNumber = Integer.parseInt(line.substring(line.lastIndexOf(" ")+1));
			}
			catch (NumberFormatException e)
			{
				//NOTE: some apps use ridiculous line numbers
			}
		}
		else if (line.startsWith("    :try_start_"))
		{
			debugInfo.tryStartLabel = line;
		}
		else if (line.startsWith("    :try_end_"))
		{
			debugInfo.tryEndLabel = line;
		}
		else if (line.startsWith("    :"))	// all other kind of labels
		{
			debugInfo.blockLabels.add(line);
			debugInfo.isFirstStmtOfBlock = true;
		}
		else if (line.startsWith("    .catch "))
		{
			debugInfo.catchInfo.add(line);
		}
		else if (line.startsWith("    .catchall "))
		{
			debugInfo.catchAllInfo = line;
		}
		else if (line.startsWith("    #"))
		{
			debugInfo.comments.add(line);
		}
		else
		{
			debugInfo.otherStuff.add(line);
		}
	}
}
