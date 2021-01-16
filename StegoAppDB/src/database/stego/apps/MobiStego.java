package database.stego.apps;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import database.stego.MessageDictionary;
import database.stego.StegoStats;
import util.Images;
import util.P;

public class MobiStego {

	public static final String fullName = "MobiStego";
	public static final String abbrName = "MS";
	
	public static boolean validate(File stego, File infoF)
	{
		Map<String, String> info = StegoStats.load(infoF);
		File dict = new File("E:/message_dictionary/"+info.get("Input Dictionary"));
		int startLine = Integer.parseInt(info.get("Dictionary Starting Line"));
		int inputlength = Integer.parseInt(info.get("Input Message Length"));
		
		String recordMessage = MessageDictionary.getMessage(dict, startLine, inputlength);
		
		return validate(stego, recordMessage);
	}
	
	public static boolean validate(File stego, String recordedMessage) {
		String extractedMessage = extract(Images.loadImage(stego));
		return recordedMessage.equals(extractedMessage);
	}
	
	public static String extract(BufferedImage img)
	{
		int x = 0, y = 0;
		List<Byte> bytes = new ArrayList<>();
		String currentByte = "";
		// ("@!#"+message+"#!@")
		while (x < img.getWidth() && y < img.getHeight())
		{
			currentByte += extract2BitsR(img, x, y);
			if (currentByte.length()==8)
			{
				byte currentB = (byte)Integer.parseInt(currentByte, 2);
				bytes.add(currentB);
				if (!matchHeader(bytes))
					return null;
				if (matchTail(bytes))
					return carveMessage(bytes);
				currentByte = "";
			}
			currentByte += extract2BitsG(img, x, y);
			if (currentByte.length()==8)
			{
				byte currentB = (byte)Integer.parseInt(currentByte, 2);
				bytes.add(currentB);
				if (!matchHeader(bytes))
					return null;
				if (matchTail(bytes))
					return carveMessage(bytes);
				currentByte = "";
			}
			currentByte += extract2BitsB(img, x, y);
			if (currentByte.length()==8)
			{
				byte currentB = (byte)Integer.parseInt(currentByte, 2);
				bytes.add(currentB);
				if (!matchHeader(bytes))
					return null;
				if (matchTail(bytes))
					return carveMessage(bytes);
				currentByte = "";
			}

			x++;
			if (x >= img.getWidth())
			{
				x = 0;
				y++;
			}
		}
		return null;
	}
	
	private static String carveMessage(List<Byte> bytes)
	{
		byte[] bb = new byte[bytes.size()-6];
		for (int i = 0; i < bb.length; i++)
		{
			bb[i] = bytes.get(i+3);
		}
		return new String(bb);
	}
	
	private static boolean matchHeader(List<Byte> bytes)
	{
		if (bytes.size()>2)
		{
			byte[] bb = new byte[3];
			for (int i = 0; i < 3; i++)
				bb[i] = bytes.get(i);
			String s = new String(bb);
			return s.equals("@!#");
		}
		return true;
	}
	
	private static boolean matchTail(List<Byte> bytes)
	{
		if (bytes.size()>6)
		{
			byte[] bb = new byte[3];
			for (int i = 0; i < 3; i++)
				bb[i] = bytes.get(bytes.size()-3+i);
			String s = new String(bb);
			return s.equals("#!@");
		}
		return false;
	}
	
	public static String extract2BitsR(BufferedImage img, int x, int y)
	{
		Color c = new Color(img.getRGB(x, y));
		return P.pad(Integer.toBinaryString(c.getRed()&3), 2);
	}
	public static String extract2BitsG(BufferedImage img, int x, int y)
	{
		Color c = new Color(img.getRGB(x, y));
		return P.pad(Integer.toBinaryString(c.getGreen()&3), 2);
	}
	public static String extract2BitsB(BufferedImage img, int x, int y)
	{
		Color c = new Color(img.getRGB(x, y));
		return P.pad(Integer.toBinaryString(c.getBlue()&3), 2);
	}
	
	public static boolean compareRGB(File f1, File f2)
	{
		BufferedImage img1 = Images.loadImage(f1);
		BufferedImage img2 = Images.loadImage(f2);
		
		if (img1.getWidth()!=img2.getWidth())
			return false;
		if (img1.getHeight()!=img2.getHeight())
			return false;
		for (int x = 0; x < img1.getWidth(); x++)
		{
			for (int y = 0; y < img1.getHeight(); y++)
			{
				int p1 = img1.getRGB(x, y);
				int p2 = img2.getRGB(x, y);
				Color c1 = new Color(p1, true);
				Color c2 = new Color(p2, true);
				if (!c1.equals(c2))
					return false;
			}
		}
		return true;
	}
	
	
	public BufferedImage cover, stego;
	public int capacity, embedded, changed;
	private int mIndex, shiftIndex;
	private byte[] messageBytes;

	public MobiStego(File inputF)
	{
		this(Images.loadImage(inputF));
	}
	public MobiStego(BufferedImage cover)
	{
		this.cover = cover;
	}
	
	public int getMessageLength(int rate)
    {
        return (int) (capacity*rate/100f - 48);
    }
	
	public void embed(String message, String outPath)
	{
		embed(message, new File(outPath));
	}
	
	public void embed(String message, File out)
	{
        changed = 0;
        messageBytes = ("@!#"+message+"#!@").getBytes(Charset.forName("UTF-8"));
        mIndex = 0;
        shiftIndex = 0;
        stego = Images.copyImage(cover, BufferedImage.TYPE_INT_ARGB);
        
        for (int y = 0; y < stego.getHeight(); y++)
        {
        	for (int x = 0; x < stego.getWidth(); x++)
        	{
        		int pixel = stego.getRGB(x, y);
        		Color color = new Color(pixel);
        		int oldRed = color.getRed();
        		int oldGreen = color.getGreen();
        		int oldBlue = color.getBlue();
        		
        		int newRed = oldRed;
        		int newGreen = oldGreen;
        		int newBlue = oldBlue;
        		
        		if (!allEmbedded())
        		{
        			newRed = oldRed&0xfc|getNext2bitsMessage();
        			recordChange(oldRed, newRed);
        		}
        		
        		if (!allEmbedded())
        		{
        			newGreen = oldGreen&0xfc|getNext2bitsMessage();
        			recordChange(oldGreen, newGreen);
        		}
        		
        		if (!allEmbedded())
        		{
        			newBlue = oldBlue&0xfc|getNext2bitsMessage();
        			recordChange(oldBlue, newBlue);
        		}
        		int newPixel = new Color(newRed, newGreen, newBlue, 0xff).getRGB();
        		stego.setRGB(x, y, newPixel);
        	}
        }
        embedded = messageBytes.length*8;
        Images.saveImage(stego, "png", out);
        
	}
	
    private boolean allEmbedded()
    {
        return mIndex>=messageBytes.length;
    }
    
    private void recordChange(int i1, int i2)
    {
        if ((i1&1) != (i2&1))
            changed++;
        if ((i1&2) != (i2&2))
            changed++;
    }
    
    
    private byte getNext2bitsMessage()
    {
        byte result = messageBytes[mIndex];
        if (shiftIndex==0)
            result = (byte) ((result>>6)&0x3);
        else if (shiftIndex == 1)
            result = (byte) ((result>>4)&0x3);
        else if (shiftIndex == 2)
            result = (byte) ((result>>2)&0x3);
        else if (shiftIndex == 3)
            result = (byte) (result &0x3);
        //P.i("reading message " + Integer.toBinaryString(messageBytes[mIndex])+" " + Integer.toBinaryString(result));
        shiftIndex++;
        if (shiftIndex>3)
        {
            mIndex++;
            shiftIndex = 0;
        }
        return result;
    }
	
	
}
