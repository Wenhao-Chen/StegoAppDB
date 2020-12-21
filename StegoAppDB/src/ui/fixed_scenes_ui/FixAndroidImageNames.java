package ui.fixed_scenes_ui;

import java.io.File;

import util.P;

public class FixAndroidImageNames {
	public static void main(String[] args)
	{
		// Android photo names have a slightly different format from iOS photo names
		// 		Android:  "Pixel1-1_Scene-20201205-221852-001_JPG-00_I155_E60_10_o.jpg"
		// 		iOS: 	 "iPhone7-1_Scene-20201205-221852_JPG-00_I155_E60_10_o.jpg"
		// This function deletes the "-001" in the "Scene-20201205-221852-001" segment
		// of Android image names
		File root = new File("H:\\StegoAppDB_20Devices_FixedScenes_Dec2020");
		for (File d : root.listFiles())
		if (!d.getName().startsWith("iPhone")) {
			File originalsDir = new File(d, "originals");
			for (File f : originalsDir.listFiles()) {
				String[] parts = f.getName().split("_");
				String[] sceneParts = parts[1].split("-");
				if (sceneParts.length>3) {
					parts[1] = parts[1].substring(0, parts[1].lastIndexOf("-"));
					File newF = new File(originalsDir, String.join("_", parts));
					P.p(f.getName()+"\n"+newF.getName()+"\n");
					f.renameTo(newF);
				}
			}
		}
	}
}
