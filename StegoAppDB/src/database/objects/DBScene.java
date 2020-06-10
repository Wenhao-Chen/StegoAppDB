package database.objects;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import util.Exif;
import util.F;
import util.Images;
import util.P;

public class DBScene {

	public int index;
	public String id;
	public String label;
	public DBImage[] raw_images, jpeg_images;
	public DBDevice device;
	private File generalCSV, relationCSV;
	
	//Each scene should have 20 originals: 10 JPEG and 10 DNG
	//						 20 PNGs: 10 from JPEG and 10 from DNG
	//Stego Count:
	//Android: 5 stego apps: MobiStego, PocketStego, Steganography_M, PixelKnot, Passlok
	//		   90 input image: 3 spatial apps use 20 PNGs, PixelKnot uses 20 originals, Passlok uses 10 original JPEG
	//		   total of 90 covers and 450 stegos (each cover has 5 stegos with 5, 10, 15, 20, 25% embedding rates)
	//IPhone:  1 stego app: Pictograph
	//		   20 input images
	//		   total of 20 covers and 100 stegos
	public static final int OriginalImageCount = 20;
	public static final int PNGCount = 20;
	public static final int AndroidCoverCount = 90;
	public static final int AndroidStegoCount = 450;
	public static final int IPhoneCoverCount = 20;
	public static final int IPhoneStegoCount = 100;
	
	DBScene(String sceneID, String label, DBDevice device, int index)
	{
		id = sceneID;
		this.label = label;
		raw_images = new DBImage[10];
		jpeg_images = new DBImage[10];
		this.device = device;
		this.index = index;
		this.generalCSV = new File(device.databaseCSVsDir, sceneID+"_general.csv");
		this.relationCSV = new File(device.databaseCSVsDir, sceneID+"_relation.csv");
	}
	
	public void addImage(File original, int index)
	{
		String ext = original.getName().substring(original.getName().lastIndexOf(".")+1).toLowerCase();
		if (ext.equals("dng"))
			raw_images[index] = new DBImage(original, index, this);
		else if (ext.equals("jpg"))
			jpeg_images[index] = new DBImage(original, index, this);
	}
	
	public Boolean[] validateAll()
	{
		return new Boolean[] {
				validateCompleteness(), 
				validateReadability(), 
				validateManualExposureSettings(), 
				validateStegoMessageExtractable()
				};
	}
	
	public boolean validateCompleteness()
	{
		if (this.getOriginalsCount()!=OriginalImageCount)
			return false;
		if (this.getPNGCount()!=PNGCount)
			return false;
		int stegoCountTarget = device.isAndroid()?AndroidCoverCount+AndroidStegoCount:IPhoneCoverCount+IPhoneStegoCount;
		if (this.getStegosCount()!=stegoCountTarget)
			return false;
		
		return true;
	}
	
	public boolean validateReadability()
	{
		for (DBImage image : this.jpeg_images)
		{
			if (!image.original.exists() || !Images.checkReadability(image.original)) // check JPEG
				return false;
		}
		for (DBImage image : this.raw_images)
		{
			if (!image.original.exists()) // check DNG existence
				return false;
			//if (!image.tif.exists())
			//	Images.dngToTiff(image.original, image.tif.getAbsolutePath());
			//if (!Images.checkReadability(image.tif))
			//	return false;
		}
		
		return true;
	}
	
	private static final double[] isoMul = {1, 0.5, 0.5, 0.5, 1.75, 1.75, 1.75, 3, 3, 3};
	private static final double[] expoMul = {1, 0.5, 1.25, 2, 0.5, 1.25, 2, 0.5, 1.25, 2};
	private static final double tolerance_Android = 0.05;
	private static final double tolerance_IPhone = 0.2;
	public boolean validateManualExposureSettings()
	{
		double[] iso = new double[10];
		double[] expo = new double[10];
		double tolerance = device.isAndroid()?tolerance_Android:tolerance_IPhone;
		
		int i = 0;
		for (DBImage image : this.jpeg_images)
		{
			Exif exif = new Exif(image.original, image.exif);
			iso[i] = exif.iso;
			expo[i] = exif.expo;
			
			if (i > 0)
			{
				double targetISO = iso[0]*isoMul[i];
				double targetExpo = expo[0]*expoMul[i];
				if (iso[i]<targetISO*(1-tolerance) || iso[i]>targetISO*(1+tolerance))
				{
					P.p("[Bad manual ISO] "+image.original.getAbsolutePath());
					return false;
				}
				if (expo[i]<targetExpo*(1-tolerance) || expo[i]>targetExpo*(1+tolerance))
				{
					P.p("[Bad manual Expo] "+image.original.getAbsolutePath());
					return false;
				}
			}
			i++;
		}
		
		return true;
	}
	
	public boolean validateStegoMessageExtractable()
	{
		for (DBImage image : jpeg_images)
		{
			if (!image.validateStegos())
				return false;
		}
		for (DBImage image : raw_images)
		{
			if (!image.validateStegos())
				return false;
		}
		return true;
	}
	
	
	public int makePNGs()
	{
		return makePNGs(null);
	}
	public int makePNGs(PrintWriter out)
	{
		int matlabJobCount = 0;
		for (DBImage image : raw_images)
			matlabJobCount += image.makePNG(out);
		for (DBImage image : jpeg_images)
			matlabJobCount += image.makePNG(out);
		return matlabJobCount;
	}
	
	public void makePictographStegos(boolean redo)
	{
		for (DBImage image : jpeg_images)
		{
			image.makePictographStegos(redo);
		}
		for (DBImage image : raw_images)
		{
			image.makePictographStegos(redo);
		}
	}
	
	public void makePasslokStegos(boolean redo)
	{
		for (DBImage image : jpeg_images)
		{
			image.makePasslokStegos(redo);
		}
		for (DBImage image : raw_images)
		{
			image.makePasslokStegos(redo);
		}
	}
	
	

	
	public void writeCSVs(PrintWriter general, PrintWriter relation)
	{
		writeCSVs(general, relation, false);
	}
	
	public void writeCSVs(PrintWriter general, PrintWriter relation, boolean redo)
	{
		if (!redo && generalCSV.exists() && relationCSV.exists())
		{
			List<String> generalData = F.readLines(generalCSV);
			List<String> relationData = F.readLines(relationCSV);
			
			int stegoCount = device.isAndroid()?AndroidStegoCount:IPhoneStegoCount;
			int coverCount = device.isAndroid()?AndroidCoverCount:IPhoneCoverCount;
			int generalCount = OriginalImageCount+PNGCount+coverCount+stegoCount;
			if (generalCount == generalData.size() && stegoCount == relationData.size())
			{
				for (String line : generalData)
					general.write(line+"\n");
				for (String line : relationData)
					relation.write(line+"\n");
				return;
			}
		}
		
		
		PrintWriter scene_general = F.initPrintWriter(generalCSV.getAbsolutePath());
		PrintWriter scene_relation = F.initPrintWriter(relationCSV.getAbsolutePath());
		for (DBImage image : jpeg_images)
		{
			image.writeCSVs(general, relation, scene_general, scene_relation);
		}
		for (DBImage image : raw_images)
		{
			image.writeCSVs(general, relation, scene_general, scene_relation);
		}
		scene_general.close();
		scene_relation.close();
	}
	
	
	public int getOriginalsCount()
	{
		int count = 0;
		for (DBImage image : raw_images)
			if (image.original!=null && image.original.exists())
				count++;
		for (DBImage image : jpeg_images)
			if (image.original!=null && image.original.exists())
				count++;
		return count;
	}
	
	public int getPNGCount()
	{
		int count = 0;
		for (DBImage image : raw_images)
			if (image.colorPNG!=null && image.colorPNG.exists())
				count++;
		for (DBImage image : jpeg_images)
			if (image.colorPNG!=null && image.colorPNG.exists())
				count++;
		return count;
	}
	
	// this number includes stego and cover images
	public int getStegosCount()
	{
		int count = 0;
		for (DBImage image : raw_images)
			count += image.getStegosCount();
		for (DBImage image : jpeg_images)
			count += image.getStegosCount();
		return count;
	}

	public List<DBImage> getAllImages()
	{
		List<DBImage> result = new ArrayList<>();
		for (DBImage image : jpeg_images)
			result.add(image);
		for (DBImage image : raw_images)
			result.add(image);
		return result;
	}
}
