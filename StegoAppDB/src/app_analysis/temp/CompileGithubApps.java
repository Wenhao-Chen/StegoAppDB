package app_analysis.temp;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import ui.ProgressUI;
import util.F;
import util.P;

public class CompileGithubApps {

	
	public static void main(String[] args)
	{
		File root = new File("C:\\workspace\\app_analysis\\apks\\github_stego");
		File oldRoot = new File("C:\\workspace\\app_analysis\\apks\\stego");
		Set<String> old = new HashSet<String>(Arrays.asList(oldRoot.list()));
		for (String f : root.list())
		{
			if (old.contains(f))
				P.p(f);
		}
	}
	
	
	static void buildAPKs()
	{
		ProgressUI ui = ProgressUI.create("Apps", 20);
		File root = new File("E:\\crawled_from_github\\android");
		File remoteDir = new File("C:\\workspace\\app_analysis\\apks\\github_stego");

		int count_gradle = 0, count_apk = 0;
		System.setProperty("ANDROID_HOME", "C:\\libs\\Android\\SDK");
		for (File dir : root.listFiles())
		{
			ui.newLine("doing "+dir.getName());
			File gradlewF = hasFile(dir, "gradlew.bat");
			File debugApk = dir.getName().contentEquals("gbastien1_Steganosaurus")?hasFile(dir, "steganosaurus-debug.apk"):
							dir.getName().contentEquals("paspao_MobiStego")?hasFile(dir, "MobiStego-debug.apk"):
							hasFile(dir, "app-debug.apk");
			if (debugApk!=null)
			{
				count_apk++;
				File to = new File(remoteDir, dir.getName()+".apk");
				F.copy(debugApk, to);
			}
			else if (gradlewF != null)
			{
				count_gradle++;
				gradleBuild(gradlewF);
				P.pause();
			}
		}
		P.pf("apk count: %d, gradle count: %d\n", count_apk, count_gradle);
	}
	
	static void gradleBuild(File gradlewF)
	{
		ProcessBuilder pb = new ProcessBuilder();
		pb.directory(gradlewF.getParentFile());
		pb.command(gradlewF.getAbsolutePath(), "assembleDebug");
		try
		{
			Process p = pb.start();
			P.printProcessStreams(p);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	static File hasFile(File f, String name)
	{
		if (f.isDirectory())
		{
			for (File ff : f.listFiles())
			{
				File fff = hasFile(ff, name);
				if (fff != null)
					return fff;
			}
		}
		else if (f.getName().contentEquals(name))
			return f;
		return null;
	}
}
