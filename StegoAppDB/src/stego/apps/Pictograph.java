package stego.apps;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import stego.MessageDictionary;
import stego.StegoStats;
import ui.ProgressUI;
import util.Images;
import util.P;

/**
 * Embedding process:
 *	first 16 bits (2 bytes) are signatures
 *		0 for plaintext only
 *		1 for ciphertext only
 *		2 for image only
 *		3 for plaintext and image
 *		4 for ciphertext and image
 *  next 64 bits (8 bytes) are the length of text
 *  next 64 bits are (8 bytes) the length of image
 *  next is the payload
 *  
 *  Embedding technique is green channel LS2B.
 *  Embedding path is lexicographical, row by row.
 */


public class Pictograph {
	
	public static void main(String[] args)
	{
		ui = ProgressUI.create("Pictograph", 20);
		File dir = new File("E:/stegodb_March2019/iPhone6s-1/stegos/Pictograph");
		for (File f : dir.listFiles())
		{
			String name = f.getName();
			if (name.endsWith(".png"))
			{
				File statsF = new File(dir, name.substring(0, name.lastIndexOf("."))+".csv");
				if (statsF.exists())
				{
					validate(f, statsF);
				}
			}
		}
	}

	public static final String fullName = "Pictograph";
	public static final String abbrName = "PG";
	
	
	public static ProgressUI ui;

	public static boolean validate(File stego, File infoF)
	{
		Map<String, String> info = StegoStats.load(infoF);
		String dictName = info.get("Input Dictionary");
		int startLine = Integer.parseInt(info.get("Dictionary Starting Line"));
		int recordedLength = Integer.parseInt(info.get("Input Message Length"));
		String recordedMessage = MessageDictionary.getMessage(dictName, startLine, recordedLength);
		//P.p("dict: " + dictName);
		return validate(stego, recordedLength, recordedMessage);
	}
	
	public static boolean validate(File stego, int recordedLength, String recordedMessage)
	{
		if (ui != null)
		{
			ui.newLine("validating "+stego.getAbsolutePath());
		}
		Object[] extracted = extract(stego);
		if (extracted==null)
		{
			P.p("[pictograph] extracted == null for file " + stego.getAbsolutePath());
			return false;
		}
		long length = (long) extracted[0];
		String message = (String) extracted[1];
		
		if (ui != null)
		{
			ui.newLine("  length correct: " +(length==recordedLength)+" extracted/recorded = " + length+"/"+recordedLength);
			ui.newLine("  message correct: " + message.equals(recordedMessage)+". length extracted/recorded = " + message.length()+"/"+recordedMessage.length());
			if (!message.equals(recordedMessage))
			{
				P.p("\n----extracted first 15 char:\n" + message.substring(0, 200));
				P.p("----recorded first 15 char:\n" + message.substring(0, 200));
				int i, maxIndex = Math.min(message.length(), recordedMessage.length());
				for (i = 0; i < maxIndex && message.charAt(i)==recordedMessage.charAt(i); i++);
				P.p("  mismatch index = "+i +". "+(int)message.charAt(i)+" vs "+(int)recordedMessage.charAt(i));
				P.pause();
			}
		}
		return length==recordedLength && message.equals(recordedMessage);
	}
	
	public static Object[] extract(File stego)
	{
		if (stego==null)
		{
			P.e("[Pictograph extract] NULL FILE");
			return null;
		}
		if (!stego.exists())
		{
			P.e("[Pictograph extract] File not exist: " + stego.getAbsolutePath());
			return null;
		}
		String ext = stego.getName().substring(stego.getName().lastIndexOf(".")+1);
		if (!ext.equalsIgnoreCase("png"))
		{
			P.e("[Pictograph Extractor] Returning NULL because file is not a PNG: "+stego.getAbsolutePath());
			return null;
		}
		BufferedImage img = Images.loadImage(stego);
		int x = 0, y = 0;
		long length = -1;
		String currentByte = "";
		List<Byte> bytes = new ArrayList<>();
		while (x < img.getWidth() && y < img.getHeight())
		{
			String next2Bits = extract2Bits(img, x, y);
			currentByte += next2Bits;
			if (currentByte.length()==8)
			{
				byte currentB = (byte)Integer.parseInt(currentByte, 2);
				bytes.add(currentB);
				//P.p("new byte(" +bytes.size()+"): " + currentByte+" "+currentB+" "+Integer.toBinaryString(currentB));
				// first 2 bytes should be 0.
				if (bytes.size()==2)
				{
					byte b0 = bytes.get(0);
					byte b1 = bytes.get(1);
					if (b0!=0 || b1!=0)
					{
						P.e("first 2 bytes of payload is " + b0 +" and " + b1+". Should both be 0.");
						return null;
					}
				}
				// the next 8 bytes is length of the plaintext
				else if (bytes.size()==10)
				{
					List<Byte> lengthBytes = new ArrayList<>();
					for (int i = 2; i < 10; i++)
						lengthBytes.add(bytes.get(i));
					length = parseLength(lengthBytes)/8;
				}
				else if (length > 0  && bytes.size()==18+length)
				{
					byte[] payloadBytes = new byte[(int) length];
					for (int i = 18; i < bytes.size(); i++)
					{
						payloadBytes[i-18] = bytes.get(i);
					}
					String payload = new String(payloadBytes);
					return new Object[] {length, payload};
				}
				
				currentByte = "";
			}
			x++;
			if (x >= img.getWidth())
			{
				x = 0;
				y++;
			}
		}
		P.e("Nothing found.");
		return null;
	}
	
	
	public static long parseLength(List<Byte> bytes)
	{
		String s = "";
		long l = 0;
		for (int i = 0; i < bytes.size(); i++)
		{
			String ss = Integer.toBinaryString(bytes.get(i));
			if (ss.length()>8)
				ss = ss.substring(ss.length()-8);
			s += P.pad(ss, 8);
		}
		l = Long.parseLong(s, 2);
		return l;
	}
	
	public static String extract2Bits(BufferedImage img, int x, int y)
	{
		Color c = new Color(img.getRGB(x, y));
		int green = c.getGreen();
		return P.pad(Integer.toBinaryString(green&3), 2, "0");
	}
	

	
	public BufferedImage cover, stego;
	
	public int capacity, embedded, changed;
	
	private int x, y;
	
	public String inputPath, coverPath;
	
	public Pictograph(File input)
	{
		this(Images.loadImage(input));
		inputPath = input.getAbsolutePath();
	}
	
	public Pictograph(BufferedImage c)
	{
		cover = c;
		capacity = cover.getWidth()*cover.getHeight()*2;
	}
	
	// Returns input message length in bits
	public int calculateInputLength(float rate)
	{
		if (rate<0 || rate>1)
			return 0;
		return (int)(capacity*rate)-16-64-64;
	}
	
	public void embed(float rate, File out)
	{
		String prefix = out.getName().substring(0, out.getName().lastIndexOf("."));
        File statsF = new File(out.getParentFile(), prefix+".csv");
		embed(rate, out, statsF);
	}
	
	public void embed(float rate, File out, File statsF)
	{
		if (inputPath==null||coverPath==null)
		{
			System.err.println("Input path or cover path is NULL!");
			System.exit(1);
		}
		
		if (rate==0)
		{
			Images.saveImage(cover, "png", out);
			return;
		}
		int messageLength = calculateInputLength(rate)/8;
		MessageDictionary.InputMessage messageInfo = MessageDictionary.randomMessage(messageLength);
        
        long time = System.currentTimeMillis();
        embed(messageInfo.message, out);
        
        StegoStats stats = new StegoStats();
        String deviceName = out.getName().substring(0, out.getName().indexOf("_"));
        int index = inputPath.indexOf(deviceName);
        if (index>=0)
        	inputPath = inputPath.substring(index);
        index = coverPath.indexOf(deviceName);
        if (index>=0)
        	coverPath = coverPath.substring(index);
        stats.inputImageName = inputPath;
        stats.coverImageName = coverPath;
        stats.stegoApp = "Pictograph";
        stats.capacity = capacity;
        stats.embedded = embedded;
        stats.embeddingRate = (float)stats.embedded/(float)stats.capacity;
        stats.changed = changed;
        stats.dictionary = messageInfo.dictName;
        stats.dictStartLine = messageInfo.lineIndex;
        stats.messageLength = messageLength;
        stats.password = "N/A";
        stats.time = System.currentTimeMillis()-time;
        stats.saveToFile(statsF.getAbsolutePath());
	}
	
	// plaintext embedding
	public void embed(String message, File out)
	{
		if (ui != null)
			ui.newLine("Pictograph embedding: " + out.getAbsolutePath());
		initStego();
		boolean[] payload = getPayload(message);
		
		for (int i = 0; i < payload.length/2; i++)
		{
			boolean bit1 = payload[i*2];
			boolean bit2 = payload[i*2+1];
			int twobits = 0;
			if (bit1)
				twobits+=2;
			if (bit2)
				twobits+=1;
			
			int rgb = stego.getRGB(x, y);
			Color c = new Color(rgb);
			int r = c.getRed();
			int g = c.getGreen();
			int b = c.getBlue();
			
			int newG = (g&0xfc) + twobits;
			Color newC = new Color(r, newG, b);
			
			stego.setRGB(x, y, newC.getRGB());
			embedded+=2;
			recordChange(g, newG);
			
			x++;
			if (x >= stego.getWidth())
			{
				x = 0;
				y++;
			}
		}
		Images.saveImage(stego, "png", out);
	}
	
    private void recordChange(int i1, int i2)
    {
        if ((i1&1) != (i2&1))
            changed++;
        if ((i1&2) != (i2&2))
            changed++;
    }
	
	private boolean[] getPayload(String message)
	{
		boolean[] sigs = getArray(0, 16);
		boolean[] length_p = getArray(message.length()*8, 64);
		boolean[] length_i = getArray(0, 64);
		List<Boolean> messages = toArray(message);
		List<Boolean> payload = new ArrayList<>();
		for (boolean b : sigs)
			payload.add(b);
		for (boolean b: length_p)
			payload.add(b);
		for (boolean b : length_i)
			payload.add(b);
		for (boolean b : messages)
			payload.add(b);
		boolean[] r = new boolean[payload.size()];
		for (int i = 0; i < r.length; i++)
			r[i] = payload.get(i);
		return r;
	}
	
	private List<Boolean> toArray(String m)
	{
		byte[] bytes = m.getBytes();
		List<Boolean> result = new ArrayList<>();
		for (byte b : bytes)
		{
			boolean[] a = getArray(b, 8);
			for (boolean bb : a)
				result.add(bb);
		}
		return result;
	}

	
	private boolean[] getArray(int l, int arrayLength)
	{
		boolean[] result = new boolean[arrayLength];
		String binary = P.pad(Integer.toBinaryString(l), arrayLength);
		for (int i = 0; i < binary.length(); i++)
		{
			result[i] = binary.charAt(i)=='1';
		}
		return result;
	}

	
	private void initStego()
	{
		stego = Images.copyImage(cover, BufferedImage.TYPE_INT_RGB);
		x = y = embedded = changed = 0;
	}	

}