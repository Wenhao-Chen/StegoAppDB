package util;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ui.ProgressUI;

public class APK {

	public static final String aaptPath = "C:/libs/Android/SDK/build-tools/28.0.3/aapt.exe";

	public static void main(String[] args)
	{
		ProgressUI out = ProgressUI.create("Result", 50);
		File dir = new File("C:\\workspace\\app_analysis\\apks\\github_stego");
		File script = new File("C:/Users/C03223-Stego2/Desktop/rename.txt");
		/*
		  for (File f : dir.listFiles())
		  { 
			  String p = readPackageName(f);
			  P.p(p);
			  renameToPackageName(f); 
		  }
		  P.p("done");
		 */
		renameScript(script, dir);
	}

	private static void renameScript(File script, File dir)
	{
		List<String> lines = F.readLinesWithoutEmptyLines(script);
		for (String line : lines)
		{
			int index = line.indexOf(",");
			File apk = new File(dir, line.substring(0, index));
			if (!apk.exists())
				continue;
			String pk = line.substring(index + 1);
			File newF = new File(dir, pk + ".apk");
			// P.pf("[old]%s\n[new]%s\n", apk.getAbsolutePath(), newF.getAbsolutePath());
			boolean succ = apk.renameTo(newF);
			P.pf("%b %s ---> %s\n", succ, apk.getAbsolutePath(), newF.getAbsolutePath());
		}
	}

	public static void renameToPackageName(File apk)
	{
		String pk = readPackageName(apk);

		if (apk.getName().equals(pk + ".apk"))
			return;

		File newF = new File(apk.getParentFile(), pk + ".apk");

		boolean succ = apk.renameTo(newF);

		P.p(succ + "\trenaming " + apk.getName() + " to " + pk);
		if (!succ)
			F.writeLine(apk.getName() + "," + pk, new File("C:/Users/C03223-Stego2/Desktop/rename.txt"), true);
	}

	public static String readPackageName(File apk)
	{
		if (!apk.getName().endsWith(".apk"))
			return null;
		String path = apk.getAbsolutePath();
		if (path.contains(" "))
			path = "\"" + path + "\"";
		Process p = P.exec(aaptPath + " dump badging " + path + " | findstr /c:\"package: name\"", false);

		try
		{
			BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line = in.readLine();
			in.close();
			p.destroyForcibly();
			int left = line.indexOf("name='") + 6;
			int right = line.indexOf("'", left + 1);
			return line.substring(left, right);
		} catch (Exception e)
		{
			e.printStackTrace();
		}
		return null;

		/**/
	}
}
