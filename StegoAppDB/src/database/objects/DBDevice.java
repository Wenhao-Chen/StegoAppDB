package database.objects;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import ui.ProgressUI;
import util.F;
import util.P;

public class DBDevice {

	public static ProgressUI ui;
	
	public String name, model, index;
	private Map<String, DBScene> scenes;
	private Map<String, String> oldSceneLabels;
	public File deviceDir, originalsDir, grayPNGDir, stegosDir;
	public File exifDir, tifDir, colorPNGDir, databaseCSVsDir;
	public File validationRecordF, sceneLabelsF, originalImagesF;
	private Boolean isAndroid;
	
	
	public DBDevice(File deviceDir, MainDB db)
	{
		this.deviceDir = deviceDir;
		this.name = deviceDir.getName();
		String[] parts = name.split("-");
		model = parts[0];
		index = parts[1];
		this.originalsDir = new File(deviceDir, "originals");
		this.originalsDir.mkdirs();
		this.colorPNGDir = new File(deviceDir, "cropped");
		this.colorPNGDir.mkdirs();
		this.stegosDir = new File(deviceDir, "stegos");
		this.stegosDir.mkdirs();
		String[] stegoApps = isAndroid()?DBStego.AndroidApps:DBStego.iOSApps;
		for (String app : stegoApps)
		{
			File appDir = new File(stegosDir, app);
			appDir.mkdirs();
		}
		File deviceAuxDir = new File(db.auxDir, name);
		deviceAuxDir.mkdirs();
		this.tifDir = new File(deviceAuxDir, "tif");
		this.tifDir.mkdirs();
		this.grayPNGDir = new File(deviceAuxDir, "grayPNG");
		this.grayPNGDir.mkdirs();
		this.exifDir = new File(deviceAuxDir, "exif");
		this.exifDir.mkdirs();
		this.validationRecordF = new File(db.validationRecordsDir, "records_"+name+".csv");
		this.sceneLabelsF = new File(db.sceneLabelsDir, "labels_"+name+".csv");
		this.databaseCSVsDir = new File(db.databaseCSVsDir, name);
		this.databaseCSVsDir.mkdirs();
		this.originalImagesF = new File(db.originalImageNamesDir, name+".txt");
		init();
		saveOriginalImageNames();
	}
	
	public void init()
	{
		//P.p("parsing " + deviceDir.getName());
		scenes = new TreeMap<>();
		oldSceneLabels = new HashMap<>();
		if (sceneLabelsF.exists())
		{
			List<String> labels = new ArrayList<>();
			for (String line : labels)
				oldSceneLabels.put(line.substring(0, line.indexOf(",")), line.substring(line.indexOf(",")+1));
		}
		
		for (File f : originalsDir.listFiles())
		{
			if (f.isDirectory())
			{
				P.e("This folder is in the wrong place: " + f.getAbsolutePath());
				continue;
			}
			String ext = F.getFileExt(f).toLowerCase();
			if (!ext.equals("jpg") && !ext.equals("dng"))
			{
				P.e("This file shouldn't be here: " + f.getAbsolutePath());
				continue;
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
                File newF = new File(f.getParentFile(), newName);
                f.renameTo(newF);
                addImageToScene(newF);
            }
            else
            {
                addImageToScene(f);
            }
			
/*			String[] parts = f.getName().split("_");
			String sceneID = parts[1];
			String lastPart = parts[parts.length-1];
			if (sceneID.split("-").length==4 || lastPart.endsWith("JPG") || lastPart.endsWith("DNG"))
			{
				parts[1] = sceneID.substring(0, sceneID.lastIndexOf("-"));
				parts[parts.length-1] = lastPart.toLowerCase();
				File newF = new File(originalsDir, String.join("_", parts));
				f.renameTo(newF);
				addImageToScene(newF);
			}
			else
			{
				addImageToScene(f);
			}*/
		}
	}
	
	private void addImageToScene(File f)
	{
		String[] parts = f.getName().split("_");
		String sceneID = parts[1];
		String label = oldSceneLabels.getOrDefault(sceneID, "no-label");
							
		int imageIndex = Integer.parseInt(parts[2].substring(4));
		if (!scenes.containsKey(sceneID))
		{
			scenes.put(sceneID, new DBScene(sceneID, label, this, scenes.size()+1));
		}
		DBScene scene = scenes.get(sceneID);
		scene.addImage(f, imageIndex);
	}
	
	public List<DBScene> getScenes()
	{
		return new ArrayList<>(scenes.values());
	}
	
	public void saveOriginalImageNames()
	{
		PrintWriter out = F.initPrintWriter(this.originalImagesF);
		for (DBScene scene : scenes.values())
		{
			for (DBImage image : scene.jpeg_images)
				out.write(image.original.getName()+"\n");
			for (DBImage image : scene.raw_images)
				out.write(image.original.getName()+"\n");
		}
		out.close();
	}

	public boolean validateOriginals()
	{
		boolean passed = true;
		
		Map<String, Boolean[]> records = new TreeMap<>();
		for (DBScene scene : scenes.values())
			records.put(scene.id, new Boolean[]{false,false,false,false});
		
		if (validationRecordF.exists())
		{
			List<String> oldRecord = F.readLines(validationRecordF);
			for (String line : oldRecord)
			{
				String[] parts = line.split(",");
				if (parts.length!=8)
					continue;
				int csvIndex = 4;
				boolean complete = parts[csvIndex++].equals("true");
				boolean readable = parts[csvIndex++].equals("true");
				boolean goodManualSettings = parts[csvIndex++].equals("true");
				boolean stegoExtractable = parts[csvIndex++].equals("true");
				records.put(parts[0], new Boolean[] {complete, readable, goodManualSettings, stegoExtractable});
			}
		}
		
		F.writeLine(String.join(",", recordEntries), validationRecordF, false);
		int count = 1, total = scenes.size();
		for (DBScene scene : scenes.values())
		{
			if (ui!=null)
				ui.newLine("Validating originals for "+name+" scene "+(count++)+"/"+total);

			Boolean[] record = records.get(scene.id);
			
			if (!record[1])
				record[1] = scene.validateReadability();
			if (!record[2])
				record[2] = scene.validateManualExposureSettings();
			records.put(scene.id, record);
			if (!record[1] || !record[2])
				passed = false;
			
			F.writeLine(convertRecord(scene.id, records.get(scene.id)), validationRecordF, true);
		}

		return passed;
	}
	
	
	public boolean validateAll()
	{
		return validateAll(false);
	}
	
	
	static final String[] recordEntries = {
			"Scene ID",
			"Original Image Count",
			"PNG Count",
			"Stego Count",
			"Completeness",
			"Originals Readable",
			"Good Manual Exposure Settings",
			"Stego Message Extractable"
	};
	public boolean validateAll(boolean redo)
	{
		boolean passed = true;
		
		Map<String, Boolean[]> records = new TreeMap<>();
		for (DBScene scene : scenes.values())
			records.put(scene.id, new Boolean[]{false,false,false,false});
		
		if (!redo && validationRecordF.exists())
		{
			List<String> oldRecord = F.readLines(validationRecordF);
			for (String line : oldRecord)
			{
				String[] parts = line.split(",");
				if (parts.length!=8)
					continue;
				int csvIndex = 4;
				boolean complete = parts[csvIndex++].equals("true");
				boolean readable = parts[csvIndex++].equals("true");
				boolean goodManualSettings = parts[csvIndex++].equals("true");
				boolean stegoExtractable = parts[csvIndex++].equals("true");
				records.put(parts[0], new Boolean[] {complete, readable, goodManualSettings, stegoExtractable});
			}
		}
		
		F.writeLine(String.join(",", recordEntries), validationRecordF, false);
		int count = 1, total = scenes.size();
		for (DBScene scene : scenes.values())
		{
			if (ui!=null)
				ui.newLine("Validating all files for "+name+" scene "+(count++)+"/"+total);

			Boolean[] record = records.get(scene.id);
			
			if (!record[0])
				record[0] = scene.validateCompleteness();
			if (!record[1])
				record[1] = scene.validateReadability();
			if (!record[2])
				record[2] = scene.validateManualExposureSettings();
			if (!record[3])
				record[3] = scene.validateStegoMessageExtractable();
			records.put(scene.id, record);
			for (boolean b : record)
			{
				if (!b)
				{
					passed = false;
					break;
				}
			}
			
			F.writeLine(convertRecord(scene.id, records.get(scene.id)), validationRecordF, true);
		}

		return passed;
	}
	
	private String convertRecord(String sceneID, Boolean[] validationResults)
	{
		DBScene scene = scenes.get(sceneID);
		StringBuilder line = new StringBuilder(sceneID);
		line.append(","+scene.getOriginalsCount());
		line.append(","+scene.getPNGCount());
		line.append(","+scene.getStegosCount());
		for (Boolean b : validationResults)
			line.append(","+b);
		return line.toString();
	}
	
	
	public int makePNGs()
	{
		try
		{
			String path = "E:/"+name+"_matlab_color2gray_jobs_"+P.getTimeString()+"_.txt";
			PrintWriter out = new PrintWriter(new FileWriter(path));
			int result = makePNGs(out);
			out.close();
			P.p("matlab jobs for "+name+" is at: " + path);
			return result;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return 0;
	}
	
	public int makePNGs(PrintWriter out)
	{
		int count = 1, total = scenes.size();
		int matlabJobCount = 0;
		for (DBScene scene : scenes.values())
		{
			if (ui!=null)
				ui.newLine("making PNGs for "+name+" scene "+(count++)+"/"+total);
			matlabJobCount += scene.makePNGs(out);
		}
		return matlabJobCount;
	}
	

	public void makePictographStegos(boolean redo)
	{
		int count = 1, total = scenes.size();
		for (DBScene scene : scenes.values())
		{
			if (ui!=null)
				ui.newLine("making Pictograph stegos for "+name+" scene "+(count++)+"/"+total);
			scene.makePictographStegos(redo);
		}
	}
	
	public void makePasslokStegos(boolean redo)
	{
		int count = 1, total = scenes.size();
		for (DBScene scene : scenes.values())
		{
			if (ui!=null)
				ui.newLine("making Passlok stegos for "+name+" scene "+(count++)+"/"+total);
			scene.makePasslokStegos(redo);
		}
	}
	

	
	public void writeCSVs(PrintWriter general, PrintWriter relation)
	{
		writeCSVs(general, relation, false);
	}
	
	public void writeCSVs(PrintWriter general, PrintWriter relation, boolean redo)
	{		
		int count = 1, total = scenes.size();
		for (DBScene scene : scenes.values())
		{
			if (ui!=null)
				ui.newLine("making CSV for "+name+" scene "+(count++)+"/"+total);
			scene.writeCSVs(general, relation, redo);
		}
	}

	
	public boolean isAndroid()
	{
		if (isAndroid == null)
		{
			isAndroid = !name.startsWith("iPhone");
		}
		return isAndroid;
	}
	
	public boolean isIPhone()
	{
		if (isAndroid == null)
		{
			isAndroid = !name.startsWith("iPhone");
		}
		return !isAndroid;
	}

	public void saveSceneLabels()
	{
		List<String> labels = new ArrayList<>();
		for (Map.Entry<String, DBScene> entry : scenes.entrySet())
		{
			labels.add(entry.getKey()+","+entry.getValue().label);
		}
		F.write(labels, sceneLabelsF, false);
	}
}
