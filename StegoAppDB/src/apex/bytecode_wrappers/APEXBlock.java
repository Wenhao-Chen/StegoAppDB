package apex.bytecode_wrappers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class APEXBlock {

	public APEXMethod m;
	public List<APEXStatement> statements;
	private List<String> label;
	public int index, subBlockIndex;
	
	APEXBlock(APEXMethod m)
	{
		this.m = m;
		statements = new ArrayList<>();
		subBlockIndex = 0;
		label = new ArrayList<>();
	}
	
	public List<String> getLabel()
	{
		return label.isEmpty()? new ArrayList<String>(Arrays.asList(":main")):label;
	}
	
	void addStatement(APEXStatement s)
	{
		statements.add(s);
		if (statements.size()==1)
		{
			setLabel(s.debugInfo.blockLabels);
		}
	}
	
	void setLabel(ArrayList<String> label)
	{
		this.label = new ArrayList<>();
		if (label != null)
		{
			for (String s : label)
				this.label.add(s.trim());
			if (label.isEmpty())
				this.label.add(":main");
		}
	}
	
	boolean isEmpty()
	{
		return statements.isEmpty();
	}
	
	public APEXStatement getFirstStatement()
	{
		return statements.get(0);
	}
	
	public APEXStatement getLastStatement()
	{
		return statements.get(statements.size()-1);
	}
	
	public String getLabelString()
	{
		String result = "";
		for (String s : getLabel())
			result += s;
		if (subBlockIndex>0)
			result += "("+subBlockIndex+")";
		return result;
	}
	
	public String toDotGraphString()
	{
		String result = (subBlockIndex==0&&!getLabelString().startsWith(":main"))?
				getLabelString()+"\\l"
				:"";
		for (int i = 0; i < statements.size(); i++)
		{
			result += statements.get(i).toDotGraphString() + "\\l";
		}
		return result;
	}
}
