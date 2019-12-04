package stego.apps;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

import stego.MessageDictionary;
import stego.StegoStats;
import util.Images;

public class SteganographyM {

	
	public static final String fullName = "SteganographyM";
	public static final String abbrName = "SM";
	
	
	public static boolean validate(File stegoF, File infoF)
	{
		Map<String, String> info = StegoStats.load(infoF);

		File dict = new File("E:/message_dictionary/"+info.get("Input Dictionary"));
		int startLine = Integer.parseInt(info.get("Dictionary Starting Line"));
		int inputlength = Integer.parseInt(info.get("Input Message Length"));
		String recordMessage = MessageDictionary.getMessage(dict, startLine, inputlength);
		String recordPassword = info.get("Password");
		
		return validate(stegoF, recordMessage, recordPassword);
	}
	
	public static boolean validate(File stego, String recordedMessage, String recordedPassword)
	{
		String extractedMessage = extract(Images.loadImage(stego), recordedPassword);
		return recordedMessage.equals(extractedMessage);
	}
	
	public static String extract(BufferedImage img, String password)
	{
		BitSet bitSet = new BitSet(img.getWidth()*img.getHeight());
		bitSet.clear();
		Random rng = initRNG(password);
		
		String currentByte = "";
		List<Byte> bytes = new ArrayList<>();
		int x,y,pixelIndex, msgLength = -1;
		while (bitSet.cardinality()<=img.getWidth()*img.getHeight())
		{
			
			do
            {
                x = rng.nextInt(img.getWidth());    // a/g/a
                y = rng.nextInt(img.getHeight());      // a/g/b
                pixelIndex = x * img.getWidth() + y;
            }
            while (bitSet.get(pixelIndex));
			bitSet.set(pixelIndex);
			
			int color = img.getRGB(x, y);
			int channelLSB = channelLSBs[rng.nextInt(3)];
			int bit = (color>>channelLSB)&1;
			currentByte = bit+currentByte;
			if (currentByte.length()==8)
			{
				bytes.add((byte)Integer.parseInt(currentByte, 2));
				if (bytes.size()==4 && !compare(bytes, prefix))
				{
					return null;
				}
				else if (bytes.size()==10)
				{
					List<Byte> lengthBytes = new ArrayList<>();
					for (int i = 4; i < bytes.size(); i++)
						lengthBytes.add(bytes.get(i));
					msgLength = (int) Pictograph.parseLength(lengthBytes);
				}
				else if (msgLength>0 && bytes.size()==10+msgLength)
				{
					byte[] msgBytes = new byte[msgLength];
					for (int i = 10; i < bytes.size(); i++)
					{
						msgBytes[i-10] = bytes.get(i);
					}
					return new String(msgBytes);
				}
				currentByte = "";
			}
		}
		return null;
		
	}
	
	static boolean compare(List<Byte> b1, byte[] b2)
	{
		if (b1.size()!=b2.length)
			return false;
		for (int i = 0; i < b1.size(); i++)
			if (b1.get(i)!=b2[i])
				return false;
		return true;
	}
	
		
	
	private static final byte[] prefix = new byte[] {0x52, 0x48, 0x43, 0x50};
    private static final byte[] channelLSBs = new byte[] {0,8,16};
    public static final int MaxPasswordLength = 8;
    
    public BufferedImage cover, stego;
    public int capacity, embedded, changed;

    private Random rng;
    private BitSet bitSet;
    
    public SteganographyM(File inputF)
    {
    	this(Images.loadImage(inputF));
    }
    
	public SteganographyM(BufferedImage cover)
	{
		this.cover = cover;
		capacity = cover.getWidth()*cover.getHeight()*3;
		bitSet = new BitSet(cover.getWidth()*cover.getHeight());
	}
	
	public int getPayloadLength(int rate)
    {
        return (int) (capacity*rate/100f-80);
    }
	
	public void embed(String message, String password, File out)
	{
		stego = Images.copyImage(cover);
		embedded = changed = 0;
		bitSet.clear();
		rng = initRNG(password);
		
		embed(prefix);
        embed(getLengthBytes(message.length()));
        embed(message.getBytes());
        Images.saveImage(stego, "png", out);
        
	}
	
	
	
    private void embed(byte[] bytes)
    {
        for (byte b : bytes)
        {
            for (int i = 0; i < 8; i++)
            {
                int payloadBit = b>>i&1;
                int x, y, pixelIndex;
                do
                {
                    x = rng.nextInt(stego.getWidth());    // a/g/a
                    y = rng.nextInt(stego.getHeight());      // a/g/b
                    pixelIndex = x * stego.getWidth() + y;    // the original code might be faulty.
                }
                while (bitSet.get(pixelIndex));
                bitSet.set(pixelIndex);
                int oldColor = stego.getRGB(x, y);
                int channelLSB = channelLSBs[rng.nextInt(3)];

                int newColor = payloadBit==1? (1<<channelLSB)|oldColor : ((1<<channelLSB)^-0x1)&oldColor;
                stego.setRGB(x, y, newColor);
                //P.i("setting ("+x+","+y+") from " + Integer.toHexString(oldColor) + " to " + Integer.toHexString(newColor) +". Bit is " + payloadBit+". Channel is "+channelLSB);
                embedded++;
                if (newColor != oldColor)
                    changed++;
            }
        }
    }
    
    private byte[] getLengthBytes(int length)
    {
        byte[] bytes = new byte[] {0,0,0,0,0,0};
        for (int i = 5; i > 1; i--)
        {
            bytes[i] = (byte)(length>>((5-i)*8)&255);
        }

        return bytes;
    }
	
	
	
    private static Random initRNG(String password)
    {
        byte[] pwBytes = password.getBytes();
        byte[] bytes = new byte[] {0,0,0,0,0,0,0,0};
        for (int i = 0; i < pwBytes.length && i < bytes.length; i++)
        {
            bytes[i] = pwBytes[i];
        }
        int[] bitsToShift = new int[] {0x38, 0x30, 0x28, 0x20, 0x18, 0x10, 0x8, 0};
        long seed = 0;
        for (int i = 0; i < 8; i++)
        {
            long l = (long)(bytes[i] & 0xff);
            seed |= (l<<bitsToShift[i]);
        }
        return new Random(seed);
    }
}
