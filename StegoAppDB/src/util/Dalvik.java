package util;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class Dalvik {

	public static final Map<String, String> primitiveTypeMap = new HashMap<String, String>() {
		private static final long serialVersionUID = 1L;
		{
			put("void",		"V");
			put("boolean",	"Z");
			put("byte",		"B");
			put("short",	"S");
			put("char",		"C");
			put("int",		"I");
			put("long",		"J");
			put("float",	"F");
			put("double",	"D");
		}
	};
	
	public static boolean isWideType(String dexType)
	{
		return dexType.equals("J")||dexType.equals("D");
	}
	
	public static String JavaToDexName(String javaName)
	{
		if (primitiveTypeMap.containsKey(javaName))
			return primitiveTypeMap.get(javaName);
		if (javaName.endsWith("[]"))
		{
			String elementJavaName = javaName.substring(0, javaName.length()-2);
			return "[" + JavaToDexName(elementJavaName);
		}
		if (javaName.contains("."))
		{
			return "L" + javaName.replace(".", "/") + ";";
		}
		return javaName;
	}
	
	public static String DexToJavaName(String dexName)
	{
		for (Entry<String, String> entry : primitiveTypeMap.entrySet())
		{
			if (entry.getValue().equals(dexName))
				return entry.getKey();
		}
		if (dexName.startsWith("["))
		{
			String elementDexName = dexName.substring(1);
			return DexToJavaName(elementDexName) + "[]";
		}
		if (dexName.endsWith(";"))
		{
			return dexName.substring(1, dexName.length()-1).replace("/", ".");
		}
		return dexName;
	}
	
	public static boolean isPrimitiveType(String dexName)
	{
		return primitiveTypeMap.containsValue(dexName);
	}
	
	public static boolean isArrayType(String dexName)
	{
		return dexName.startsWith("[");
	}
}
