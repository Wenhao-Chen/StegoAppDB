package apex.bytecode_wrappers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import apex.instrument.Logger;
import util.Dalvik;
import util.P;

public class APEXMethod {

	public String declaration, signature, subSignature;
	
	public List<APEXStatement> statements;
	public List<APEXBlock> blocks;
	
	public int lineNumber;
	// All modifiers:
	// 	public private protected
	// 	static final
	// 	constructor abstract native synthetic
	// 	declared-synchronized bridge varargs
	public Set<String> modifiers;
	
	public APEXClass c;
	
	private DebugInfo debugInfo;
	private List<SupplementalData> supplementalDataBlocks;
	
	private class DebugInfo {
		int locals = -1, addedLocals = 0, lineNumber = 1;
		List<String> paramsInfo = new ArrayList<String>();
		List<String> annotations = new ArrayList<String>();
		List<String> catchInfo = new ArrayList<String>();
	}
	private class SupplementalData {
		String label;
		ArrayList<String> data;
		SupplementalData(String l, ArrayList<String> d)	{label = l; data = d;}
	}
	
	
	public APEXMethod(List<String> body, APEXClass c) throws Exception
	{
		if (body.isEmpty())
		{
			P.e("empty method declaration???");
			System.exit(1);
			return;
		}
		
		this.c = c;

		statements = new ArrayList<>();
		blocks = new ArrayList<>();
		modifiers = new HashSet<>();
		debugInfo = new DebugInfo();
		supplementalDataBlocks = new ArrayList<>();
		
		parseDeclaration(body.get(0));
		parseBody(body);
		sortBlockLabels();
	}
	
	
	private void sortBlockLabels()
	{
		String lastLabel = ":main";
		int subBlockIndex = 0;
		for (int i = 1; i < blocks.size(); i++)
		{
			APEXBlock block = blocks.get(i);
			if (block.getLabelString().equals(lastLabel))
			{
				block.subBlockIndex = ++subBlockIndex;
			}
			else
			{
				lastLabel = block.getLabelString();
				subBlockIndex = 0;
			}
		}
	}
	
	
	public String getName()
	{
		return subSignature.substring(0, subSignature.indexOf("("));
	}
	
	public ArrayList<String> getSupplementalData(String label)
	{
		for (SupplementalData d : supplementalDataBlocks)
		{
			if (d.label.equals(label))
				return d.data;
		}
		return null;
	}
	
	public APEXBlock getBlock(String label)
	{
		for (APEXBlock b : blocks)
		{
			if (b.getLabel().contains(label))
				return b;
		}
		return null;
	}
	
	public ArrayList<String> getParamTypeJavaNames()
	{
		ArrayList<String> result = new ArrayList<String>();
		ArrayList<String> dexParams = getParamTypeDexNames();
		for (String param : dexParams)
			result.add(Dalvik.DexToJavaName(param));
		return result;
	}
	
	public ArrayList<String> getParamTypeDexNames()
	{
		ArrayList<String> result = new ArrayList<String>();
		if (!modifiers.contains("static"))
		{
			result.add(c.dexName);
		}
		String params = subSignature.substring(subSignature.indexOf("(")+1, subSignature.indexOf(")"));
		int i = 0;
		while (i < params.length())
		{
			char c = params.charAt(i++);
			switch (c)
			{
				case 'L': // class name
				{
					String param = c + "";
					while (c != ';')
					{
						c = params.charAt(i++);
						param += c;
					}
					result.add(param);
					break;
				}
					
				case '[': // array
				{
					String param = c + "";
					while (c == '[')
					{
						c = params.charAt(i++);
						param += c;
					}
					if (c == 'L')
					{
						while (c != ';')
						{
							c = params.charAt(i++);
							param += c;
						}
					}
					result.add(param);
					break;
				}
				default: // primitive
				{
					result.add(c+"");
				}
			}
		}
		return result;
	}
	
	public int getLocalRegisterCount()
	{
		return debugInfo.locals;
	}
	
	public int getParamRegisterCount()
	{
		int result = 0;
		for (String pType : getParamTypeDexNames())
		{
			if (pType.equals("J") || pType.equals("D"))
				result += 2;
			else
				result++;
		}
		return result;
	}
	
	
	private void parseBody(List<String> body) throws Exception
	{
		if (body.size()<=2) // could be native
			return;
		int i = 1;
		boolean inTryBlock = false;
		List<String> stmtDebugInfo = new ArrayList<>();
		APEXBlock currentBlock = new APEXBlock(this);
		while (i < body.size())
		{
			String line = body.get(i);
			if (line.equals("") || line.equals(".end method"))
			{}
			else if (line.startsWith("    .locals "))
			{
				// declaring local register count. (parameter register index starts from the last local register index)
				debugInfo.locals = Integer.parseInt(line.substring(line.lastIndexOf(" ")+1));
			}
			else if (line.startsWith("    .param "))
			{
				// annotating parameter variable names in source code, example: 
				//		.param p1, "who"    # Landroid/graphics/drawable/Drawable;
				// it could be followed by an annotation block
				debugInfo.paramsInfo.add(line);
				if (body.get(i+1).startsWith("        .annotation "))
				{
					while (!line.equals("    .end param"))
					{
						line = body.get(++i);
						debugInfo.paramsInfo.add(line);
					}
				}
			}
			else if (line.startsWith("    .annotation "))
			{
				// method annotations
				debugInfo.annotations.add(line);
				while (!line.equals("    .end annotation"))
				{
					line = body.get(++i);
					debugInfo.annotations.add(line);
				}
				debugInfo.annotations.add("");
			}
			else if (line.startsWith("    .catch")) // including ".catch" and ".catchall"
			{
				// showing where to go when exception is caught
				debugInfo.catchInfo.add(line);
				statements.get(statements.size()-1).addDebugInfo(line);
			}
			else if (line.startsWith("    :array_") || line.startsWith("    :pswitch_data_") || line.startsWith("    :sswitch_data_"))
			{
				// supplemental data - array initialization, packed-switch map, and sparse-switch map
				String label = line.trim();
				String endLabel = "    .end array-data";
				if (line.startsWith("    :pswitch_data_"))
					endLabel = "    .end packed-switch";
				else if (line.startsWith("    :sswitch_data_"))
					endLabel = "    .end sparse-switch";
				ArrayList<String> data = new ArrayList<String>(stmtDebugInfo);	// because there might be .line in the supplemental data
				stmtDebugInfo.clear();
				while (!line.equals(endLabel))
				{
					data.add(line);
					line = body.get(++i);
				}
				data.add(line);
				supplementalDataBlocks.add(new SupplementalData(label, data));
			}
			else if (line.startsWith("    :try_start_"))
			{
				// try block label
				inTryBlock = true;
				stmtDebugInfo.add(line);
			}
			else if (line.startsWith("    :try_end_"))
			{
				// labeling end of try block
				inTryBlock = false;
				statements.get(statements.size()-1).addDebugInfo(line);
			}
			else if (line.startsWith("    .") || line.startsWith("    :") || line.startsWith("    #"))
			{
				// other debug info include:
				//   .line	.local  :(block_label)  #(comments)  
				stmtDebugInfo.add(line);
			}
			else if (!line.endsWith(Logger.CommentTag))
			{
				// smali statement
				APEXStatement s = new APEXStatement(line, statements.size(), this);
				for (String debugInfo : stmtDebugInfo)
					s.addDebugInfo(debugInfo);
				stmtDebugInfo.clear();
				s.debugInfo.inTryBlock = inTryBlock;
				if (!s.hasBlockLabel() && !statements.isEmpty())
				{
					APEXStatement prevStatement = statements.get(statements.size()-1);
					s.debugInfo.blockLabels = new ArrayList<String>(prevStatement.debugInfo.blockLabels);
				}
				if (s.debugInfo.isFirstStmtOfBlock && !currentBlock.isEmpty())	// beginning of a new block
				{
					currentBlock.index = blocks.size();
					blocks.add(currentBlock);
					currentBlock = new APEXBlock(this);
				}
				currentBlock.addStatement(s);
				
				s.block = currentBlock;
				statements.add(s);
				if (s.isGotoStmt()
					|| s.isReturnStmt()
					|| s.isThrowStmt()
					|| s.isIfStmt()
					|| s.isSwitchStmt()
					|| s.isInvokeStmt())
				{
					currentBlock.index = blocks.size();
					blocks.add(currentBlock);
					currentBlock = new APEXBlock(this);
				}
			}
			i++;
		}
	}
	
	private void parseDeclaration(String declarationLine)
	{
		declaration = declarationLine;
		String[] parts = declarationLine.split(" ");
		for (int i = 1; i < parts.length-1; i++)
			modifiers.add(parts[i]);
		subSignature = parts[parts.length-1];
		signature = c.dexName+"->"+subSignature;
	}
	
	public ArrayList<String> getInstrumentedBody()
	{
		ArrayList<String> result = new ArrayList<String>();
		result.add(declaration);
		if (debugInfo.locals > -1)
			result.add("    .locals " + (debugInfo.locals+debugInfo.addedLocals));
		if (debugInfo.addedLocals>0)
			result.add("    # added " + debugInfo.addedLocals + " locals");
		result.addAll(debugInfo.paramsInfo);
		if (debugInfo.annotations.size()>0)
		{
			result.addAll(debugInfo.annotations);
		}
		else
		{
			result.add("");
		}
		for (int i = 0; i < this.statements.size(); i++)
		{
			APEXStatement s = this.statements.get(i);
			result.addAll(s.getInstrumentedBody());
			if (i < this.statements.size()-1 || !supplementalDataBlocks.isEmpty())
				result.add("");
		}
		for (int i = 0; i < supplementalDataBlocks.size(); i++)
		{
			result.addAll(supplementalDataBlocks.get(i).data);
			if (i < supplementalDataBlocks.size()-1)
				result.add("");
		}
		result.add(".end method");
		return result;
	}
	
	@Override
	public String toString()
	{
		return subSignature;
	}
}
