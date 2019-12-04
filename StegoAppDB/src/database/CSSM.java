package database;

import java.io.File;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import database.objects.MainDB;
import ui.ProgressUI;
import util.F;

public class CSSM {
	
	public static final String[] generalInfoFields = {
		"Image Path",
		"Image Type",
		"Image Format",
		"Image Scene",
		"Image Source Device",
		"Image Width",
		"Image Height",
		"Make",
		"Camera Model Name",
		"Exposure Time",
		"ISO",
		"F Number",
		"Focal Length",
		"Exposure Mode",
		"White Balance",
		"Backup Machine",
		"Backup Path",
		"JPG Quality",
		"TIF path for DNG",
		"Device Model",
		"Device Index",
		"Scene Label"
	};
	
	public static final String[] relationInfoFields = {
		"Stego Image Path",
		"Embedding Method",
		"Embedding Rate",
		"Message Dictionary",
		"Message Starting Line",
		"Message Length",
		"Password",
		"Cover Image Path",
		"Input Image Path",
		"Original Image Path"
	};
	
	public static void writeGeneralCSVEntry(Map<String, String> data, PrintWriter out)
	{
		
		for (int i = 0; i < generalInfoFields.length; i++)
		{
			String val = data.get(generalInfoFields[i]);
			out.write(val==null?"null":val);
			if (i<generalInfoFields.length-1)
				out.write(",");
		}
		out.write("\n");
	}
	
	public static void writeRelationCSVEntry(Map<String, String> data, PrintWriter out)
	{
		for (int i = 0; i < relationInfoFields.length; i++)
		{
			String val = data.get(relationInfoFields[i]);
			out.write(val==null?"null":val);
			if (i<relationInfoFields.length-1)
				out.write(",");
		}
		out.write("\n");
	}
	
	
	public static void main(String[] args)
	{
		ProgressUI ui = ProgressUI.create("fix naming mistakes", 20);
		List<String> deviceNames = F.readLines(new File("E:/stegodb_March2019/_records/DeviceNames"));
		for (String name : deviceNames)
		{
			File device = new File(MainDB.defaultRoot, name);
			File stegoDir = new File(device, "stegos");
			for (File appDir : stegoDir.listFiles())
			{
				for (File f : appDir.listFiles())
				{
					if (f.getName().endsWith(".csv"))
					{
						List<String> lines = F.readLines(f);
						for (int i = 0; i < lines.size(); i++)
						{
							String line = lines.get(i);
							if (line.startsWith("Cover Image") && line.contains("_rate_"))
							{
								line = line.replace("_rate_", "_rate-");
								ui.newLine("fixing CSV in "+f.getName());
								lines.set(i, line);
								break;
							}
						}
						F.write(lines, f, false);
					}
					
					if (f.getName().contains("_rate_"))
					{
						File newF = new File(appDir, f.getName().replace("_rate_", "_rate-"));
						ui.newLine("renaming "+newF.getName());
						f.renameTo(newF);
					}
				}
			}
		}
	}
}
