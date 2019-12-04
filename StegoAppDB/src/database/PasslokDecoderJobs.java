package database;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import util.F;
import util.P;

public class PasslokDecoderJobs {

	static final File downloadDir = new File("F:/TEMP_Passlok_Coefficients");
	
	public static void main(String[] args)
	{
		File jobsDir = new File("E:/stegodb_March2019/_records/PasslokDecoderJobs");
		int total = 0;
		for (File device : new File("E:/stegodb_March2019").listFiles())
		{
			String name = device.getName();
			if (name.startsWith("_")||name.startsWith("i"))
				continue;
			File stegoDir = new File(device, "stegos");
			File passlokDir = new File(stegoDir, "Passlok");
			int count = 0;
			List<String> jobs = new ArrayList<>();
			for (File f : passlokDir.listFiles())
			{
				if (f.getName().endsWith(".jpg") && !f.getName().endsWith("rate-00.jpg"))
				{
					if (!coeffFilesExist(f))
					{
						jobs.add(f.getName());
						count++;
					}
				}
			}
			total += count;
			P.pf("%-15s %-5d %.2f GB", name, count, (float)count*100/1024);
			F.write(jobs, new File(jobsDir, name+".log"), false);
		}
		double space = (double)total*100/1024;
		P.p("space: " + space);
	}
	
	static boolean coeffFilesExist(File stegoF)
	{
		File[] coeffFiles = new File[3];
		for (int i=1; i<=3; i++)
		{
			coeffFiles[i-1] = new File(downloadDir, stegoF.getName()+"_"+i+".txt");
			if (!coeffFiles[i-1].exists())
				return false;
		}
		return true;
	}

}
