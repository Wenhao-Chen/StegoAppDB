package database.sig_based_detection;

import java.awt.Color;
import java.awt.image.BufferedImage;

public class DetectingStegMaster {

	public static boolean identify(BufferedImage image)
	{
		Payload payload = new Payload();
		for (int x = 0; x < image.getWidth(); x++)
		{
			for (int y = 0; y < image.getHeight(); y++)
			{
				Color c = new Color(image.getRGB(x, y));
				int hundreds = c.getRed()%10;
				int tens = c.getGreen()%10;
				int ones = c.getBlue()%10;
				byte b = (byte) (hundreds*100+tens*10+ones);
				payload.bytes.add(b);
				
				if (payload.bytes.size()==Signatures.SMT_header.length())
				{
					String header = new String(payload.getBytes(0, payload.bytes.size()-1));
					if (!header.equals(Signatures.SMT_header))
						return false;
				}
				
				int l = Signatures.SMT_header.length()+Signatures.SMT_tail.length();
				if (payload.bytes.size()>l)
				{
					int from = payload.bytes.size()-Signatures.SMT_tail.length();
					int to = payload.bytes.size()-1;
					String tail = new String(payload.getBytes(from, to));
					if (tail.equals(Signatures.SMT_tail))
						return true;
				}
			}
		}
		return false;
	}
}
