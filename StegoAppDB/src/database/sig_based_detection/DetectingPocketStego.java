package database.sig_based_detection;

import java.awt.image.BufferedImage;

public class DetectingPocketStego {

	public static boolean identify(BufferedImage image)
	{
		Payload payload = new Payload();
		for (int x = 0; x < image.getWidth(); x++)
		{
			for (int y = 0; y < image.getHeight(); y++)
			{
				int pixel = image.getRGB(x, y);
				int bit = pixel&1;
				payload.addBit(bit);
				if (payload.bytes.size()>1)
				{
					String lastByte = new String(payload.getBytes(payload.bytes.size()-1, payload.bytes.size()-1));
					if (lastByte.equals(Signatures.PS_tail))
						return true;
				}
			}
		}
		return false;
	}
}
