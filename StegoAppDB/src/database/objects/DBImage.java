package database.objects;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import database.CSSM;
import database.stego.StegoStats;
import database.stego.apps.Pictograph;
import database.stego.apps.passlok.Passlok;
import database.stego.apps.pixelknot.PixelKnot;
import util.Exif;
import util.F;
import util.Images;

public class DBImage {
	
	
	public File original, png, colorPNG, tif, exif;
	public int index;
	public Map<String, List<DBStego>> stegos;
	private DBScene scene;
	
	DBImage(File f, int index, DBScene scene)
	{
		original = f;
		this.index = index;
		this.scene = scene;
		String leftPart = original.getName().substring(0, original.getName().lastIndexOf("_o."));
		png = new File(scene.device.grayPNGDir, leftPart+"_c.png");
		
		colorPNG = new File(scene.device.colorPNGDir, leftPart+"_cc.png");
		tif = new File(scene.device.tifDir, leftPart+"_o.tif");
		exif = new File(scene.device.exifDir, f.getName()+".exif");
		
		stegos = new HashMap<>();
		for (File appDir : scene.device.stegosDir.listFiles())
		{
			String appName = appDir.getName();
			if (appName.equals(Passlok.fullName) && !Images.isJPEG(original)) // we only use original JPEG for passlok
				continue;
			File input = (appName.equals(PixelKnot.fullName) || appName.equals(Passlok.fullName))? original : png;
			
			String stegoNamePrefix = input.getName()+"_s_"+DBStego.getAbbrAppName(appName)+"_rate-";
			String stegoNameSuffix = DBStego.spatialApps.contains(appName)?".png":".jpg";
			List<DBStego> appStegos = new ArrayList<>();
			for (int rate = 0; rate <= 25; rate += 5)
			{
				String rateS = String.format("%02d", rate);
				DBStego stego = new DBStego();
				stego.stegoImage = new File(appDir, stegoNamePrefix + rateS + stegoNameSuffix);
				stego.statsFile = new File(appDir, stegoNamePrefix + rateS + ".csv");
				stego.isCover = (rate==0);
				stego.embeddingRate = (float)rate/100.0f;
				appStegos.add(stego);
				stego.coverImage = appStegos.get(0).stegoImage;
				stego.inputImage = input;
			}
			stegos.put(appName, appStegos);
		}
	}
	
	
	// the return number includes stego and cover images
	public int getStegosCount()
	{
		int count = 0;
		for (List<DBStego> app :stegos.values())
		{
			for (DBStego stego : app)
			{
				if (stego.exists())
					count++;
			}
		}
		return count;
	}
	
	
	public int getStegosCount(String app)
	{
		if (!stegos.containsKey(app))
			return 0;
		int count = 0;
		List<DBStego> appStegos = stegos.get(app);
		for (DBStego stego : appStegos)
		{
			if (stego.exists())
				count++;
		}
		return count;
	}
	
	
	public int makePNG(PrintWriter out)
	{
		if (png.exists())
			return 0;
		
		// make the color PNG
		if (!colorPNG.exists())
		{
			BufferedImage img = null;
			if (original.getName().endsWith(".dng"))
			{
				if (!tif.exists())
					Images.dngToTiff(original, tif.getAbsolutePath());
				img = Images.loadImage(tif);
			}
			else
			{
				img = Images.loadImage(original);
			}
			
			Images.centerCrop(img, colorPNG);
		}
		
		// add color to gray job to matlab script
		if (out != null)
		{
			try
			{
				out.write(colorPNG.getAbsolutePath()+","+png.getAbsolutePath()+"\n");
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		return 1;
	}
	
	public void makePictographStegos(boolean redo)
	{
		if (!stegos.containsKey(Pictograph.fullName))
			return;
		
		List<DBStego> appStegos = stegos.get(Pictograph.fullName);

		if (!redo && allExist(appStegos))
			return;
		
		Pictograph pg = new Pictograph(Images.loadImage(png));
		pg.inputPath = png.getAbsolutePath();
		pg.coverPath = appStegos.get(0).stegoImage.getAbsolutePath();
		for (DBStego stego : appStegos)
		{
			pg.embed(stego.embeddingRate, stego.stegoImage, stego.statsFile);
		}
	}
	
	public void makePasslokStegos(boolean redo)
	{
		if (!stegos.containsKey(Passlok.fullName))
			return;
		List<DBStego> appStegos = stegos.get(Passlok.fullName);
		if (!redo && allExist(appStegos))
			return;
		Passlok.makeStegos(original, appStegos);
	}
	
	private boolean allExist(List<DBStego> appStegos)
	{
		for (DBStego stego : appStegos)
			if (!stego.exists())
				return false;
		return true;
	}
	
	public boolean validateStegos()
	{
		for (List<DBStego> appStegos : stegos.values())
		{
			for (DBStego stego : appStegos)
			{
				if (!stego.validate())
					return false;
			}
		}
		return true;
	}
	
	public void writeCSVs(PrintWriter general, PrintWriter relation)
	{
		writeCSVs(general, relation, null, null);
	}
	
	public void writeCSVs(PrintWriter general, PrintWriter relation, PrintWriter scene_general, PrintWriter scene_relation)
	{
		Exif exif = new Exif(original, this.exif);
		Map<String, String> g = new HashMap<>();

		g.put("Image Scene", "Indoor");
		g.put("Image Source Device", scene.device.name);
		g.put("Exposure Time", "1/"+exif.expo_frac_of_second);
		g.put("ISO", ""+exif.iso);
		g.put("F Number", exif.data.get("F Number"));
		g.put("Focal Length", exif.data.get("Focal Length"));
		g.put("White Balance", exif.data.get("White Balance"));
		g.put("Backup Machine", "CO3223-stego2");
		g.put("TIF path for DNG", null);
		g.put("Device Model", scene.device.model);
		g.put("Device Index", scene.device.index);
		g.put("Scene Label", scene.label);
		
		// Fields that are individually different among image files
		// 	Image Path, Image Type, Image Format, 
		//  Image Width, Image Height, Backup Path, JPG Quality
		
		// original (JPEG or DNG)
		g.put("Image Path", trimPath(original));
		g.put("Image Format", F.getFileExt(original).toUpperCase());
		g.put("Image Width", exif.data.get("Image Width"));
		g.put("Image Height", exif.data.get("Image Height"));
		g.put("Make", exif.data.get("Make"));
		g.put("Camera Model Name", exif.data.get("Camera Model Name"));
		g.put("Backup Path", original.getAbsolutePath());
		g.put("Image Type", "original");
		g.put("JPG Quality", F.getFileExt(original).equalsIgnoreCase("jpg")?"90":null);
		g.put("Exposure Mode", exif.data.get("Exposure Mode"));
		if (g.get("Exposure Mode")==null || g.get("Exposure Mode").equals("null"))
			g.put("Exposure Mode", index==0?"Auto":"Manual");
		CSSM.writeGeneralCSVEntry(g, general);
		if (scene_general != null)
			CSSM.writeGeneralCSVEntry(g, scene_general);
		
		// NOTE: for non-originals, write null in these fields:
		//     Make, Camera Model Name, F Number, Focal Length
		g.put("Make", null);
		g.put("Camera Model Name", null);
		g.put("F Number", null);
		g.put("Focal Length", null);
		// input (PNG)
		int[] pngSize = Images.getImageDimension(png);
		g.put("Image Path", trimPath(png));
		g.put("Image Format", F.getFileExt(png).toUpperCase());
		g.put("Image Width", pngSize[0]+"");
		g.put("Image Height", pngSize[1]+"");
		g.put("Backup Path", png.getAbsolutePath());
		g.put("Image Type", "input");
		g.put("JPG Quality", F.getFileExt(png).equalsIgnoreCase("jpg")?"90":null);
		CSSM.writeGeneralCSVEntry(g, general);
		if (scene_general != null)
			CSSM.writeGeneralCSVEntry(g, scene_general);
		
		// stegos
		Map<String, String> r = new HashMap<>();
		r.put("Original Image Path", trimPath(original));
		for (Entry<String, List<DBStego>> entry : stegos.entrySet())
		{
			String appName = entry.getKey();
			List<DBStego> appStegos = entry.getValue();
			r.put("Embedding Method", appName);
			for (DBStego stego : appStegos)
			{
				//File stegoExifF = new File(scene.device.exifDir, stego.stegoImage.getName()+".exif");
				//Exif stegoExif = new Exif(stego.stegoImage, stegoExifF);
				//updateGeneralInfoForImage(g, stego.stegoImage, stegoExif);
				int[] size = Images.getImageDimension(stego.stegoImage);
				g.put("Image Path", trimPath(stego.stegoImage));
				g.put("Image Format", F.getFileExt(stego.stegoImage).toUpperCase());
				g.put("Image Width", size[0]+"");
				g.put("Image Height", size[1]+"");
				g.put("Backup Path", stego.stegoImage.getAbsolutePath());
				g.put("Image Type", stego.isCover?"cover":"stego");
				g.put("JPG Quality", F.getFileExt(stego.stegoImage).equalsIgnoreCase("jpg")?"90":null);
				CSSM.writeGeneralCSVEntry(g, general);
				if (scene_general != null)
					CSSM.writeGeneralCSVEntry(g, scene_general);
				if (!stego.isCover)
				{
					//write relations for this stego
					Map<String, String> stats = StegoStats.load(stego.statsFile);
					r.put("Stego Image Path", trimPath(stego.stegoImage));
					r.put("Embedding Rate", stats.get("Embedding Rate"));
					r.put("Message Dictionary", stats.get("Input Dictionary"));
					r.put("Message Starting Line", stats.get("Dictionary Starting Line"));
					r.put("Message Length", stats.get("Input Message Length"));
					r.put("Password", stats.get("Password"));
					
					r.put("Cover Image Path", trimPath(stego.coverImage));
					r.put("Input Image Path", trimPath(stego.inputImage));
					CSSM.writeRelationCSVEntry(r, relation);
					if (scene_relation != null)
						CSSM.writeRelationCSVEntry(r, scene_relation);
				}
			}
		}
	}

	
	
	private String trimPath(File image)
	{
		return image.getAbsolutePath().substring(image.getAbsolutePath().indexOf(scene.device.name));
	}
	
}
