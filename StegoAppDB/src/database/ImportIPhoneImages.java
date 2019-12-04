package database;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import util.F;
import util.P;

public class ImportIPhoneImages {

	
	private static String dataRoot = "E:/stegodb_March2019/_import_iphone";
	private static File imageDir = new File(dataRoot, "images");
	
	public static void main(String[] args)
	{
		F.changeExtToLowerCase(imageDir);
		List<String> names = F.readLines(new File("E:/stegodb_March2019/_records/DeviceNames"));
		for (String name: names)
		{
			File record = new File(dataRoot, name+".txt");
			if (!record.exists())
				continue;
			P.p("parsing "+record.getName());
			parse(record.getAbsolutePath(), name);
		}
	}
	
	
	
	private static void parse(String recordPath, String deviceName)
	{
		Set<String> goodScenes = new HashSet<>();
		Map<String, List<File>> images = new HashMap<>();
		File recordsFile = new File(recordPath);
		List<String> records = F.readLines(recordsFile);
		for (String record : records)
		{
			if (record.equals(""))
				continue;
			String[] parts = record.split(",");
			if (!parts[4].equals("confirm"))
				continue;
			
			String time = parts[0];
			goodScenes.add(time);
		}
		
		
		File imageDir = new File(dataRoot, "images");
		for (File f : imageDir.listFiles())
		{
			String[] parts = f.getName().split("_");
			if (parts.length != 6)
				continue;
			// iPhone6sPlus-2_Scene-20190401-120151_JPG-00_I80_E30_o
			String timeStamp = parts[1].substring(6);
			
			if (deviceName.equals(parts[0]) && goodScenes.contains(timeStamp))
			{
				if (!images.containsKey(timeStamp))
					images.put(timeStamp, new ArrayList<>());
				List<File> scene = images.get(timeStamp);
				scene.add(f);
			}
		}
		File originalsDir = new File("E:/stegodb_March2019/"+deviceName+"/originals");
		originalsDir.mkdirs();
		int good = 0;
		for (Map.Entry<String, List<File>> scene : images.entrySet())
		{
			if (scene.getValue().size()==20)
			{
				for (File f : scene.getValue())
				{
					File newF = new File(originalsDir, f.getName().replace(".JPG", ".jpg").replace(".DNG", ".dng"));
					f.renameTo(newF);
				}
				good++;
			}
		}
		P.p("good scene count: "+good);
	}
}
