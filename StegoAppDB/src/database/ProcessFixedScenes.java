package database;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import ui.ProgressUI;
import util.F;
import util.Images;
import util.P;

public class ProcessFixedScenes {

	static final File root = new File("G:/TEMP_18_Phones_Fixed_Scenes_Data");
	static final File root1 = new File("E:/stegodb_FixedScenes");
	
	public static void main(String[] arg)
	{
		//OneLastTime();
		//moveTifOut();
		compare();
	}
	
	public static void compare()
	{
		for (File d : root1.listFiles())
		{
			File ori1 = new File(d, "originals");
			File ori2 = remote(ori1);
			File dng1 = new File(ori1, "DNG");
			File dng2 = remote(dng1);
			File bad1 = new File(d, "bad");
			File bad2 = remote(bad1);
			File nat1 = new File(d, "Native");
			File nat2 = remote(nat1);
			
			P.pf("%s ori1/ori2 = %d/%d, dng1/dng2 = %d/%d, bad1/bad2 = %d/%d, nat1/nat2 = %d/%d",
					d.getName(), 
					ori1.list().length, ori2.list().length, 
					dng1.list().length, dng2.list().length,
					bad1.list().length, bad2.list().length,
					nat1.list().length, nat2.list().length);
			P.p(compare(ori1, ori2)+"");
			P.p(compare(dng1, dng2)+"");
			P.p(compare(bad1, bad2)+"");
			P.p(compare(nat1, nat2)+"");
		}
	}
	
	private static boolean compare(File dir1, File dir2)
	{
		int i = 0;
		File[] ff1 = dir1.listFiles();
		File[] ff2 = dir2.listFiles();
		while (i<ff1.length)
		{
			File f1 = ff1[i];
			File f2 = ff2[i++];
			
			if (!f1.getName().equals(f2.getName()))
				return false;
		}
		return true;
	}
	
	private static File remote(File f)
	{
		return new File(f.getAbsolutePath().replace(root1.getAbsolutePath(), root.getAbsolutePath()));
	}
	
	private static void moveTifOut()
	{
		for (File device : root.listFiles())
		{
			if (device.getName().equals("5dngs"))
				continue;
			File ori = new File(device, "originals");
			File dngDir = new File(ori,  "DNG");
			File tifDir = new File(dngDir, "TIFF");
			for (File tif : tifDir.listFiles())
			{
				File newF = new File(tif.getAbsolutePath().replace("TEMP_18_Phones_Fixed_Scenes_Data", "temp_tifs"));
				newF.getParentFile().mkdirs();
				tif.renameTo(newF);
			}
			tifDir.delete();
		}
	}
	
	public static void OneLastTime()
	{
		Images.ui = ProgressUI.create("DNG to TIF", 20);
		for (File device : root.listFiles())
		{
			if (device.getName().equals("5dngs"))
				continue;
			//toLowerCase(device);
			File newDir = new File(device, "new");
			newDir.delete();
			File ori = new File(device, "originals");
			File dngDir = new File(ori,  "DNG");
			File tifDir = new File(dngDir, "TIFF");
			//removeSceneIndexAndLabel(device);
			Map<String, Scene> scenes = new TreeMap<>();
			collectScenes(scenes, ori);
			collectScenes(scenes, dngDir);
			collectScenes(scenes, tifDir);
			P.p(device.getName()+" scene count " + scenes.size());
			for (Scene s : scenes.values())
			{
				int c = s.getCount();
				if (c != 30)
				{
					for (File dng : s.dngs)
					{
						File tif = new File(tifDir, dng.getName().replace(".dng", ".tif"));
						Images.dngToTiff(dng, tif.getAbsolutePath());
					}
				}
			}
			//P.p("");
		}
	}

	public static void removeSceneIndexAndLabel(File f)
	{
		if (f.isDirectory())
		{
			for (File ff : f.listFiles())
				removeSceneIndexAndLabel(ff);
		}
		else
		{
			String ext = F.getFileExt(f);
			if (!ext.equals("jpg") && !ext.equals("dng") && !ext.equals("tif") && !ext.equals("heic"))
			{
				P.p("??? "+f.getAbsolutePath());
				P.pause();
			}
			String[] parts = f.getName().split("_");
			if (parts.length==7) // remove scene label
			{
				String[] newParts = new String[6];
				System.arraycopy(parts, 0, newParts, 0, 5);
				newParts[5] = parts[6];
				parts = newParts;
			}
			String sceneID = parts[1];
			String[] sceneIDParts = sceneID.split("-");
			if (sceneIDParts.length==4)
			{
				parts[1] = sceneIDParts[0]+"-"+sceneIDParts[1]+"-"+sceneIDParts[2];
			}
			String newName = String.join("_", parts);
			if (!f.getName().equals(newName))
			{
				P.pf("[new]%s\n[old]%s", newName, f.getName());
				f.renameTo(new File(f.getParentFile(), newName));
			}
		}
	}

	
	public static class Scene {
		public String timeString;
		public List<File> jpgs, dngs, tifs;
		public Scene(String s) {timeString=s; jpgs = new ArrayList<>(); dngs = new ArrayList<>(); tifs = new ArrayList<>();}
		public int getCount() {return jpgs.size()+dngs.size()+tifs.size();}
		public void purge() {
			List<File> files = new ArrayList<>();
			files.addAll(jpgs); files.addAll(dngs); files.addAll(tifs);
			for (File f : files) f.delete();
		}
		public List<File> getAllFiles() {
			List<File> files = new ArrayList<>();
			files.addAll(jpgs); files.addAll(dngs); files.addAll(tifs);
			return files;
		}
	}
	
	private static void collectScenes(Map<String, Scene> scenes, File dir)
	{
		for (File f : dir.listFiles())
		{
			if (!f.isFile())
				continue;
			
			// Pixel1-1_Scene-20190409-212815_JPG-00_I466_E40_o.jpg
			String[] parts = f.getName().split("_");
			String sceneID = parts[1];
			scenes.putIfAbsent(sceneID, new Scene(sceneID));
			Scene scene = scenes.get(sceneID);
			String ext = F.getFileExt(f);
			if (ext.equals("jpg"))	scene.jpgs.add(f);
			else if (ext.equals("dng"))	scene.dngs.add(f);
			else if (ext.equals("tif"))	scene.tifs.add(f);
			else
			{
				P.p("ext?? " + f.getAbsolutePath());
				P.pause();
			}
		}
	}

	
	public static void toLowerCase(File f)
	{
		if (f.isDirectory())
		{
			for (File ff : f.listFiles())
			{
				toLowerCase(ff);
			}
		}
		else
		{
			String name = f.getName().substring(0, f.getName().lastIndexOf("."));
			String ext = f.getName().substring(f.getName().lastIndexOf(".")+1).toLowerCase();
			String newName = name+"."+ext;
			if (!f.getName().equals(newName))
			{
				File newF = new File(f.getParentFile(), newName);
				f.renameTo(newF);
				P.p("[old]"+f.getName()+"\n[new]"+newName);
			}
		}
	}
	
}
