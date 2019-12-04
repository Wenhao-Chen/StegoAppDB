package database.objects;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import stego.apps.MobiStego;
import stego.apps.Pictograph;
import stego.apps.PocketStego;
import stego.apps.SteganographyM;
import stego.apps.passlok.Passlok;
import stego.apps.pixelknot.PixelKnot;
import ui.ProgressUI;

@SuppressWarnings("serial")
public class DBStego {
	
	public static ProgressUI ui;
	
	public static final String[] AndroidApps = {
			MobiStego.fullName, PixelKnot.fullName, PocketStego.fullName, SteganographyM.fullName, Passlok.fullName};
	
	public static final String[] iOSApps = {Pictograph.fullName};
	
	
	public static final Map<String, String> appNameCodes = new HashMap<String, String>(){{
		put(MobiStego.fullName, MobiStego.abbrName);
		put(PixelKnot.fullName, PixelKnot.abbrName);
		put(PocketStego.fullName, PocketStego.abbrName);
		put(SteganographyM.fullName, SteganographyM.abbrName);
		put(Passlok.fullName, Passlok.abbrName);
		put(Pictograph.fullName, Pictograph.abbrName);
	}};
	
	public static final Set<String> spatialApps = new HashSet<String>() {{
		add(MobiStego.fullName);
		add(PocketStego.fullName);
		add(SteganographyM.fullName);
		add(Pictograph.fullName);
	}};
	
	public static final Set<String> jpegApps = new HashSet<String>() {{
		add(PixelKnot.fullName);
		add(Passlok.fullName);
	}};
	

	
	public static String getAbbrAppName(String fullName)
	{
		if (appNameCodes.containsKey(fullName))
			return appNameCodes.get(fullName);
		return fullName;
	}
	
	public File stegoImage, statsFile, coverImage, inputImage;
	public float embeddingRate;
	public boolean isCover;
	
	public boolean exists()
	{
		if (isCover)
			return stegoImage.exists();
		return stegoImage.exists()&&statsFile.exists();
	}
	
	public boolean validate()
	{
		if (!exists())
			return false;
		if (isCover)
			return true;
		
		if (ui != null)
			ui.newLine("Validating "+stegoImage.getAbsolutePath());
		String appName = stegoImage.getParentFile().getName();
		if (appName.equals(Pictograph.fullName))
		{
			return Pictograph.validate(stegoImage, statsFile);
		}
		else if (appName.equals(MobiStego.fullName))
		{
			return MobiStego.validate(stegoImage, statsFile);
		}
		else if (appName.equals(PocketStego.fullName))
		{
			return PocketStego.validate(stegoImage, statsFile);
		}
		else if (appName.equals(SteganographyM.fullName))
		{
			return SteganographyM.validate(stegoImage, statsFile);
		}
		else if (appName.equals(PixelKnot.fullName))
		{
			return PixelKnot.validate(stegoImage, statsFile);
		}
		else if (appName.equals(Passlok.fullName))
		{
			return true;
			//return Passlok.validate(stegoImage, statsFile);
		}
		return false;
	}
	
}
