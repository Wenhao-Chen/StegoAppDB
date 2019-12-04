package util;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Exif {

	static final String exiftoolPath = "c:/libs/exiftool.exe";
	public Map<String, String> data;
	public double iso, expo;
	public int expo_frac_of_second;

	
	public Exif(File image, File exifF)
	{
		this(image, exifF, false);
	}
	
	public Exif(File image, File exifF, boolean force)
	{
		if (!force && exifF.exists())
		{
			// load from file
			List<String> exiftoolOutput = F.readLines(exifF);
			parseExiftoolOutput(exiftoolOutput);
		}
		else
		{
			// get from exiftool, then save to file
			List<String> exiftoolOutput = P.readInputStream(P.exec(exiftoolPath+" \""+image.getAbsolutePath()+"\"", false));
			new Thread(new Runnable() {
				@Override
				public void run()
				{
					F.write(exiftoolOutput, exifF, false);
				}
			}).start();
			parseExiftoolOutput(exiftoolOutput);
		}
	}
	
	private void parseExiftoolOutput(List<String> list)
	{
		data = new HashMap<>();
		for (String line : list)
		{
			if (line.equals(""))
				continue;
			int index = line.indexOf(":");
			String key = line.substring(0, index).trim();
			String val = line.substring(index+1).trim();
			data.put(key, val);
			if (key.equals("ISO"))
			{
				iso = Double.parseDouble(val);
			}
			else if (key.equals("Exposure Time"))
			{
				if (val.startsWith("1/"))
				{
					expo_frac_of_second = Integer.parseInt(val.substring(2));
					expo = 1.0/(double)expo_frac_of_second;
				}
				else
				{
					expo = Double.parseDouble(val);
					expo_frac_of_second = (int)(1.0/expo+0.5);
				}
			
			}
		}
	}
}
