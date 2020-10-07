package database.sig_based_detection;

import java.awt.Color;
import java.awt.image.BufferedImage;

public class DetectingDavinci {

	public static boolean identify(BufferedImage image)
	{
		Payload payload = new Payload();
		for (int y = 0; y < image.getHeight(); y++)
		{
			for (int x = 0; x < image.getWidth(); x++)
			{
				Color c = new Color(image.getRGB(x, y), true);
				int bit = c.getAlpha()-254;
				payload.addBit(bit);
				boolean matchFirst4a = false, matchFirst4b = false;
				if (payload.bytes.size()==4) // first 4 bytes should be either: [0,0,0,4] or [0,0,0,3]
				{
					byte[] first4 = payload.getBytes(0, 3);
					matchFirst4a = Payload.compare(first4, Signatures.DV_first4_option1);
					matchFirst4b = Payload.compare(first4, Signatures.DV_first4_option2);
					if (!matchFirst4a && !matchFirst4b)
					{
						return false;
					}
				}
				if (payload.bytes.size()==8 && matchFirst4a) // the next 3 or 4 bytes should be "t2i" or "t2ip". "p" indicates password
				{
					byte[] signatureBytes = payload.getBytes(4, 7);
					String signature = new String(signatureBytes);
					if (!signature.equals("t2ip"))
						return false;
				}
				else if (payload.bytes.size()==7 && matchFirst4b)
				{
					byte[] signatureBytes = payload.getBytes(4, 6);
					String signature = new String(signatureBytes);
					if (!signature.equals("t2i"))
						return false;
				}
			}
		}
		return true;
	}
}
