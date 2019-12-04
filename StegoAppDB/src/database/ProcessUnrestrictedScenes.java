package database;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import database.objects.DBDevice;
import database.objects.DBImage;
import database.objects.DBScene;
import database.objects.MainDB;
import ui.ProgressUI;
import util.F;
import util.P;

public class ProcessUnrestrictedScenes {

	
	public static void main(String[] args) 
	{
		ProgressUI ui = ProgressUI.create("label", 20);
		File root = new File("E:/stegodb_March2019/_records/DatabaseCSVs");
		for (File dir : root.listFiles())
		{
			int count = 0;
			for (File csv : dir.listFiles())
			{
				if (csv.getName().endsWith("general.csv"))
				{
					ui.newLine(dir.getName()+"  "+(++count)+"  "+csv.getName());
					List<String> old = F.readLinesWithoutEmptyLines(csv);
					boolean append = false;
					for (String s : old)
					{
						int index = s.lastIndexOf(",");
						String newS = s.substring(0, index)+",no-label";
						F.writeLine(newS, csv, append);
						if (!append)
							append = true;
					}
				}
			}
		}
		ui.newLine("Done");
	}
	
	
	static class Scene {
		List<File> jpgs, dngs;
		String name;
		Scene(String s) {
			name = s;
			jpgs = new ArrayList<>();
			dngs = new ArrayList<>();
		}
		int count() {
			return jpgs.size()+dngs.size();
		}
		void delete() {
			for (File f : jpgs) f.delete();
			for (File f : dngs) f.delete();
		}
	}
	
	public static void removeSceneIndexAndLabel()
	{
		for (File device : new File("E:/stegodb_March2019").listFiles())
		{
			File ori = new File(device, "originals");
			if (!ori.exists())
				continue;
			P.p(device.getName());
			for (File f : ori.listFiles())
				removeSceneIndexAndLabel(f);
		}
	}
	
	private static void removeSceneIndexAndLabel(File f)
	{
		String ext = F.getFileExt(f).toLowerCase();
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
		if (sceneIDParts.length==4) // remove scene index
		{
			parts[1] = sceneIDParts[0]+"-"+sceneIDParts[1]+"-"+sceneIDParts[2];
		}
		String newName = String.join("_", parts).replace(".JPG", ".jpg").replace(".DNG", ".dng");
		if (!f.getName().equals(newName))
		{
			P.pf("  [new]%s\n  [old]%s", newName, f.getName());
			f.renameTo(new File(f.getParentFile(), newName));
		}
	}

}
