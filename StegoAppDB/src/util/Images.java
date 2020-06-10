package util;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;

import ui.ProgressUI;

public class Images {

	public static String dngValidatePath = "C:/libs/dng_validate.exe";

	public static ProgressUI ui;
	
	public static void dngToTiff(File dng, String tif)
	{
		if (ui != null)
			ui.newLine("converting dng " + dng.getAbsolutePath()+" to TIF...");
		P.exec(dngValidatePath+" -tif \""+tif+"\" \""+dng.getAbsolutePath()+"\"", true);
	}
	
	public static BufferedImage copyImage(BufferedImage source)
	{
		return copyImage(source, source.getType());
	}
	
	public static int[] getImageDimension(File imgFile)
	{
		if (ui != null)
			ui.newLine("Reading image size: " + imgFile.getAbsolutePath());
		String ext = F.getFileExt(imgFile);
		if (ext == null)
			return null;
		
		Iterator<ImageReader> iter = ImageIO.getImageReadersBySuffix(ext);
		while (iter.hasNext())
		{
			ImageReader reader = iter.next();
			try
			{
				ImageInputStream stream = new FileImageInputStream(imgFile);
			    reader.setInput(stream);
			    int width = reader.getWidth(reader.getMinIndex());
			    int height = reader.getHeight(reader.getMinIndex());
			    return new int[] {width, height};
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			finally
			{
				reader.dispose();
			}
		}
		return null;
	}
	
	public static boolean isJPEG(File original)
	{
		String ext = original.getName().substring(original.getName().lastIndexOf(".")+1);
		return ext.equalsIgnoreCase("jpg");
	}
	
	public static boolean isDNG(File original)
	{
		String ext = original.getName().substring(original.getName().lastIndexOf(".")+1);
		return ext.equalsIgnoreCase("dng");
	}
	
	public static BufferedImage copyImage(BufferedImage source, int type)
	{
	    BufferedImage b = new BufferedImage(source.getWidth(), source.getHeight(), type);
	    Graphics g = b.getGraphics();
	    g.drawImage(source, 0, 0, null);
	    g.dispose();
	    return b;
	}
	
	public static BufferedImage loadImage(String path)
	{
		return loadImage(new File(path));
	}
	
	public static BufferedImage loadImage(File f)
	{
		try
		{
			return ImageIO.read(f);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			P.p("Error reading file: " + f.getAbsolutePath().replace('\\', '/'));
		}
		return null;
	}
	
	public static void centerCrop(BufferedImage img, File out)
	{
		if (ui != null)
			ui.newLine("making cc png "+out.getAbsolutePath()+"...");
		int x0 = img.getWidth()/2-256;
		int y0 = img.getHeight()/2-256;
		BufferedImage cropped = img.getSubimage(x0, y0, 512, 512);
		saveImage(cropped, "png", out);
	}
	
	public static BufferedImage scale(File original, File saveTo)
	{
		return scale(loadImage(original), saveTo);
	}
	
	public static BufferedImage scale(BufferedImage original, File saveTo)
	{
		BufferedImage outputImage = scale(original, 0.05);
		
		if (saveTo != null)
			Images.saveImage(outputImage, saveTo.getName().substring(saveTo.getName().lastIndexOf(".")+1), saveTo);
		
		return outputImage;
	}
	
	public static BufferedImage scale(BufferedImage source,double ratio)
	{
		  int w = (int) (source.getWidth() * ratio);
		  int h = (int) (source.getHeight() * ratio);
		  BufferedImage bi = getCompatibleImage(w, h);
		  Graphics2D g2d = bi.createGraphics();
		  double xScale = (double) w / source.getWidth();
		  double yScale = (double) h / source.getHeight();
		  AffineTransform at = AffineTransform.getScaleInstance(xScale,yScale);
		  g2d.drawRenderedImage(source, at);
		  g2d.dispose();
		  return bi;
		}

	private static BufferedImage getCompatibleImage(int w, int h) {
		  GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		  GraphicsDevice gd = ge.getDefaultScreenDevice();
		  GraphicsConfiguration gc = gd.getDefaultConfiguration();
		  BufferedImage image = gc.createCompatibleImage(w, h);
		  return image;
		}
	
	public static boolean checkReadability(File f)
	{
		if (ui != null)
			ui.newLine("checking readability for "+f.getAbsolutePath());
		try
		{
			ImageIO.read(f);
		}
		catch (Exception e)
		{
			P.p("[Un-readable Image] " + f.getAbsolutePath());
			return false;
		}
		return true;
	}
	
	public static void saveImage(BufferedImage image, String format, File out)
	{
		try
		{
			ImageIO.write(image, format, out);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			P.p("Error saving file: " + out.getAbsolutePath());
		}
	}
	
	
}
