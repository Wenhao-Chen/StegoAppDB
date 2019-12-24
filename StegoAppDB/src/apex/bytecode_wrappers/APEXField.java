package apex.bytecode_wrappers;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import util.P;

public class APEXField {

	
	public String subSignature, signature;
	public List<String> declarations;
	public Set<String> modifiers;
	
	private APEXClass c;
	
	public APEXField(List<String> declarations, APEXClass c)
	{

		if (declarations.isEmpty())
		{
			P.e("Empty field declaration???");
			System.exit(1);
			return;
		}
		
		this.c = c;
		modifiers = new HashSet<>();
		this.declarations = declarations;
		parseDeclaration(declarations.get(0));
	}
	
	public String getType()
	{
		return subSignature.substring(subSignature.indexOf(":")+1);
	}
	
	public String getName()
	{
		return subSignature.substring(0, subSignature.indexOf(":"));
	}
	
	
	private void parseDeclaration(String declaration)
	{
		if (declaration.contains(" = "))
			declaration = declaration.substring(0, declaration.indexOf(" = "));
		
		String[] parts = declaration.trim().split(" ");
		for (int i = 1; i < parts.length-1; i++)
			modifiers.add(parts[i]);
		
		subSignature = parts[parts.length-1];
		signature = c.dexName+"->"+subSignature;
	}
	
	
	@Override
	public String toString()
	{
		return subSignature;
	}
}
