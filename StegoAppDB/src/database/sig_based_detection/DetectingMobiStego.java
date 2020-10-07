package database.sig_based_detection;

import java.awt.Color;
import java.awt.image.BufferedImage;

public class DetectingMobiStego {

	public static boolean identify(BufferedImage image)
	{
		Payload payload = new Payload();
		
		for (int y = 0; y < image.getHeight(); y++)
		{
			for (int x = 0; x < image.getWidth(); x++)
			{
				Color c = new Color(image.getRGB(x, y));
				int[] channels = {c.getRed(), c.getGreen(), c.getBlue()};
				for (int channel : channels)
				{
					int twoBits = channel&3;
					payload.addBit(twoBits>>1&1);
					payload.addBit(twoBits&1);
					if (payload.bytes.size()==3) // first 3 bytes should be "@!#"
					{
						String first3bytes = new String(payload.getBytes(0, 2));
						if (!first3bytes.equals(Signatures.MS_header))
							return false;
					}
					if (payload.bytes.size()>6) // last 3 bytes should be"#!@"
					{
						String last3bytes = new String(payload.getBytes(payload.bytes.size()-3, payload.bytes.size()-1));
						if (last3bytes.equals(Signatures.MS_tail))
							return true;
					}
				}
			}
		}
		return false;
	}
	
	public static String extract(BufferedImage image)
	{
		Payload payload = new Payload();
		
		for (int y = 0; y < image.getHeight(); y++)
		{
			for (int x = 0; x < image.getWidth(); x++)
			{
				Color c = new Color(image.getRGB(x, y));
				int[] channels = {c.getRed(), c.getGreen(), c.getBlue()};
				for (int channel : channels)
				{
					int twoBits = channel&3;
					payload.addBit(twoBits>>1&1);
					payload.addBit(twoBits&1);
					if (payload.bytes.size()==3) // first 3 bytes should be "@!#"
					{
						String first3bytes = new String(payload.getBytes(0, 2));
						if (!first3bytes.equals(Signatures.MS_header))
							return null;
					}
					if (payload.bytes.size()>6) // last 3 bytes should be"#!@"
					{
						String last3bytes = new String(payload.getBytes(payload.bytes.size()-3, payload.bytes.size()-1));
						if (last3bytes.equals(Signatures.MS_tail))
						{
							return new String(payload.getBytes(3, payload.bytes.size()-4));
						}
					}
				}
			}
		}
		return null;
	}
	
	
	private static void updatePayload(Payload payload, int channel)
	{
		int twoBits = channel&3;
		payload.addBit(twoBits>>1&1);
		payload.addBit(twoBits&1);
	}
}
