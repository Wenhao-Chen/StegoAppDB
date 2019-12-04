package util;

import java.io.File;

public class Apktool {

	public static final String apktoolPath = "C:\\libs\\Apktool\\brut.apktool\\apktool-cli\\build\\libs\\apktool-cli-all.jar";
	
	public static void decode(File apk, File outDir)
	{
		String apkPath = apk.getAbsolutePath();
		if (apkPath.contains(" "))
			apkPath = "\""+apkPath+"\"";
		
		String outPath = outDir.getAbsolutePath();
		if (outPath.contains(" "))
			outPath = "\""+outPath+"\"";
		
		String cmd = "java -jar "+apktoolPath+" d "+apkPath+" -f -o "+outPath;
		P.exec(cmd, true, true);
		P.p("\nAPK file decoded into: "+outPath+"\n");
	}
	
	
	public static void build(File smaliDir, File unsigned)
	{
		String smaliPath = smaliDir.getAbsolutePath();
		if (smaliPath.contains(" "))
			smaliPath = "\""+smaliPath+"\"";
		
		String outPath = unsigned.getAbsolutePath();
		if (outPath.contains(" "))
			outPath = "\""+outPath+"\"";
		
		String cmd = "java -jar "+apktoolPath+" b --use-aapt2 -f -o "+outPath+" "+smaliPath;
		P.exec(cmd, true, true);
		P.p("\nUnsigned APK created: "+unsigned.getAbsolutePath());
	}
}
