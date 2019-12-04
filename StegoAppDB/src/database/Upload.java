package database;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Set;

import ui.ProgressUI;
import util.P;

public class Upload {


	public static final File localRoot = new File("E:/stegodb_March2019");
	public static final File remoteRoot = new File("Z:/stegodb_March2019");
	
	static ProgressUI ui = ProgressUI.create("Compare", 20);
	
	static Set<String> devicesTODO;
	static{
		devicesTODO = new HashSet<>();
		devicesTODO.add("OnePlus5-2");
		devicesTODO.add("Pixel1-2");
	}
	
	static int bad;
	public static void main(String[] args)
	{
		remoteRoot.mkdir();
		ZtoE();
		EtoZ();
	}
	
	public static void ZtoE()
	{
		for (File remoteDevice : remoteRoot.listFiles())
		{
			//if (!devicesTODO.contains(remoteDevice.getName()))
			//	continue;
			bad = 0;
			long time = System.currentTimeMillis();
			ZtoE(remoteDevice);
			time = (System.currentTimeMillis()-time)/1000;
			double min = (double)time/60;
			P.p(remoteDevice.getName()+" "+bad+" files to remove from Z: drive. Exec time = "+min+" minutes.");
		}
	}
	
	private static void ZtoE(File f)
	{
		ui.newLine("ZtoE "+f.getAbsolutePath());
		if (f.isDirectory())
		{
			for (File ff : f.listFiles())
				ZtoE(ff);
		}
		else
		{
			File localF = new File(f.getAbsolutePath().replace("Z:", "E:"));
			if (!localF.exists() || localF.length()!=f.length())
			{
				bad++;
				f.delete();
			}
		}
	}
	
	public static void EtoZ()
	{
		for (File localDevice : localRoot.listFiles())
		{
			//if (!devicesTODO.contains(localDevice.getName()))
			//	continue;
			if (localDevice.getName().startsWith("_"))
				continue;
			long time = System.currentTimeMillis();
			bad = 0;
			EtoZ(localDevice);
			time = (System.currentTimeMillis()-time)/1000;
			double min = (double)time/60;
			P.p(localDevice.getName()+" "+bad+" files to upload. Exec time = "+min+" minutes.");
		}
	}
	
	
	private static void EtoZ(File f)
	{
		ui.newLine("EtoZ "+f.getAbsolutePath());
		if (f.isDirectory())
		{
			for (File ff : f.listFiles())
				EtoZ(ff);
		}
		else
		{
			File remoteF = new File(f.getAbsolutePath().replace("E:", "Z:"));
			if (!remoteF.exists() || remoteF.length()!=f.length())
			{
				bad++;
				try
				{
					Files.copy(f.toPath(), remoteF.toPath(), StandardCopyOption.REPLACE_EXISTING);
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		}
	}
	
	public static void compareLocalAndRemote()
	{
		for (File localDevice : localRoot.listFiles())
		{
			if (localDevice.getName().startsWith("_") || localDevice.getName().startsWith("i"))
				continue;
			P.p("--- "+localDevice.getName()+" ---");
			File remoteDevice = new File(remoteRoot, localDevice.getName());
			remoteDevice.mkdirs();
			
			bad = 0;
			compare(localDevice, remoteDevice);
			if (bad>0)
			{
				P.p("  bad="+bad);
			}
		}
	}
	
	private static void compare(File f1, File f2)
	{
		ui.newLine((f1==null?"NULL":f1.getName())+" vs "+(f2==null?"NULL":f2.getName()));
		if (f1==null)
		{
			//P.p("  NULL vs " +f2.getName());
			bad++;
			return;
		}
		if (f2==null)
		{
			//P.p("  "+f1.getName()+" vs NULL");
			bad++;
			return;
		}
		if (!f1.getName().equals(f2.getName()))
		{
			//P.p("  diff name: "+f1.getName()+" vs "+f2.getName());
			bad++;
			return;
		}
		if (f1.isFile() && f2.isFile())
		{
			if (f1.length()!=f2.length())
			{
				//P.p("  diff length: " + f1.getName()+" vs " + f2.getName()+". "+f1.length()+" vs "+f2.length());
				bad++;
				return;
			}
		}
		else if (f1.isDirectory() && f2.isDirectory())
		{
			File[] ff1 = f1.listFiles();
			File[] ff2 = f2.listFiles();
			int max = Math.max(ff1.length, ff2.length);
			for (int i = 0; i < max; i++)
			{
				File fff1 = i<ff1.length?ff1[i]:null;
				File fff2 = i<ff2.length?ff2[i]:null;
				compare(fff1, fff2);
			}
		}
		else
		{
			//P.p("  diff type: "+f1.getName()+" vs "+f2.getName()+". "+f1.isDirectory()+" "+f2.isDirectory());
			bad++;
		}
	}
	
}
