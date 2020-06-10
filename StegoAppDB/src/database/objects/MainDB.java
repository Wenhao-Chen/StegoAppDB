package database.objects;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import util.Exif;
import util.F;
import util.P;

public class MainDB {

	public static final String defaultRoot = "E:/stegodb_March2019";
	
	
	public File rootDir, auxDir, recordsDir;
	public File validationRecordsDir, sceneLabelsDir, databaseCSVsDir, originalImageNamesDir;
	public List<String> deviceNames;
	public List<DBDevice> devices;
	public Set<String> devicesWithValidatedSceneContent;
	
	public static void main(String[] args)
	{
		MainDB db = new MainDB();
		Set<Integer> isos = new TreeSet<>();
		Set<Integer> expos = new TreeSet<>(Collections.reverseOrder());
		for (DBDevice d : db.devices) if(d.isIPhone()) for (DBScene scene: d.getScenes()) for(DBImage image: scene.jpeg_images)
		{
			Exif exif = new Exif(image.original, image.exif);
			if (!isos.contains((int)exif.iso))
			{
				isos.add((int)exif.iso);
				System.out.println((int)exif.iso);
			}
			if (!expos.contains(exif.expo_frac_of_second))
			{
				expos.add(exif.expo_frac_of_second);
				System.out.println("expo 1/"+exif.expo_frac_of_second);
			}
		}
		P.p("done");
		for (int iso : isos)
			System.out.println(iso);
		for (int expo: expos)
			System.out.println("1/"+expo);
	}
	
	public MainDB()
	{
		this(defaultRoot);
	}
	
	public MainDB(String rootDir)
	{
		this.rootDir = new File(rootDir);
		this.auxDir = new File(rootDir, "_aux_data");
		this.auxDir.mkdirs();
		this.recordsDir = new File(rootDir, "_records");
		this.recordsDir.mkdirs();
		this.validationRecordsDir = new File(recordsDir,  "ValidationRecords");
		this.validationRecordsDir.mkdirs();
		this.sceneLabelsDir = new File(recordsDir, "SceneLabels");
		this.sceneLabelsDir.mkdirs();
		this.databaseCSVsDir = new File(recordsDir, "DatabaseCSVs");
		this.databaseCSVsDir.mkdirs();
		this.originalImageNamesDir = new File(recordsDir, "OriginalImageNames");
		this.originalImageNamesDir.mkdirs();
		loadRecords();
		initDeviceDBs();
	}
	
	private void loadRecords()
	{
		deviceNames = F.readLines(new File(recordsDir, "DeviceNames"));
		Collections.sort(deviceNames);
		//NOTE: load other records such as processing history
		devicesWithValidatedSceneContent = new HashSet<>();
		List<String> sceneContentValidationRecord = F.readLines(new File(recordsDir, "SceneContentValidated.txt"));
		for (String line : sceneContentValidationRecord)
		{
			int index = line.indexOf("=");
			if (index == -1)
				continue;
			String name = line.substring(0, index);
			boolean goodContent = line.substring(index+1).equalsIgnoreCase("yes");
			if (goodContent)
				devicesWithValidatedSceneContent.add(name);
		}
	}
	
	private void initDeviceDBs()
	{
		System.out.print("parsing image data in "+rootDir.getAbsolutePath()+"... ");
		devices = new ArrayList<>();
		for (String name : deviceNames)
		{
			File deviceDir = new File(rootDir, name);
			deviceDir.mkdirs();
			devices.add(new DBDevice(deviceDir, this));
		}
		System.out.print("Done.\n");
	}
	
	public void validate()
	{
		validate(false);
	}
	
	public void validate(boolean redo)
	{
		P.p("Validating database...");
		for (DBDevice d : devices)
		{
			d.validateAll(redo);
		}
	}
	
	public DBDevice getDevice(String name)
	{
		for (DBDevice device : devices)
			if (device.name.equals(name))
				return device;
		return null;
	}
	
}
