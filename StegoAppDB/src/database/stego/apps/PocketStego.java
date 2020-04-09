package database.stego.apps;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Map;

import database.stego.MessageDictionary;
import database.stego.StegoStats;
import util.Images;

public class PocketStego {

	
	public static final String fullName = "PocketStego";
	public static final String abbrName = "PS";
	
	
	public static boolean validate(File stego, File infoF)
	{
		Map<String, String> info = StegoStats.load(infoF);

		File dict = new File("E:/message_dictionary/"+info.get("Input Dictionary"));
		int startLine = Integer.parseInt(info.get("Dictionary Starting Line"));
		int inputlength = Integer.parseInt(info.get("Input Message Length"));
		String recordMessage = MessageDictionary.getMessage(dict, startLine, inputlength);
		
		return validate(stego, inputlength, recordMessage);
	}
	
	public static boolean validate(File stego, int recordedLength, String recordedMessage)
	{
		String extractedMessage = extract(stego, recordedLength+1);
		if (!extractedMessage.endsWith("\u0000"))
		{
			return false;
		}
		return recordedMessage.equals(extractedMessage.substring(0, extractedMessage.length()-1));
	}
	
	public static String extract(File stego, int length)
	{
		BufferedImage img = Images.loadImage(stego);
		
		int x = 0, y = 0;
		String currentByte = "";
		byte[] bytes = new byte[length];
		for (int i = 0; i < length*8; i++)
		{
			currentByte += img.getRGB(x, y)&1;
			if (currentByte.length()==8)
			{
				bytes[i/8] = (byte)Integer.parseInt(currentByte, 2);
				currentByte = "";
			}
			
			y++;
			if (y >= img.getHeight())
			{
				y = 0;
				x++;
			}
		}
		return new String(bytes);
	}
}
