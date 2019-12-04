package util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import ui.ProgressUI;

public class DNG {
	
	
	public static void main(String[] args)
	{
		File root = new File("H:/CameraID/Fullsize");
		File outDir = new File("I:/CameraID/Fullsize/tif");
		outDir.mkdirs();
		List<File> dngs = new ArrayList<>();
		List<String> tifs = new ArrayList<>();
		for (File device : root.listFiles())
		{
			if (device.getName().startsWith("JPEG"))
				continue;
			File rawDir = new File(device, "raw");
			for (File dng : rawDir.listFiles())
			{
				if (!dng.getName().endsWith(".dng"))
					continue;
				File tif = new File(outDir, dng.getName().replace(".dng", ".tif"));
				if (!tif.exists() || tif.length()<200000)
				{
					dngs.add(dng);
					tifs.add(tif.getAbsolutePath());
				}
			}
		}
		
		int threadCount = 8;
		// 8 thread: 20 seconds, 1120
		// 6 thread: 15 seconds, 1580
		
		TempThread[] threads = new TempThread[threadCount];
		for (int i = 0; i < threads.length;i ++)
			threads[i] = new TempThread("thread "+(i+1));
		int index = 0, threadIndex = 0;
		while (index < dngs.size())
		{
			threads[threadIndex].dngs.add(dngs.get(index));
			threads[threadIndex].tifs.add(tifs.get(index));
			index++;
			threadIndex = (threadIndex+1)%threads.length;
		}
		
		for (TempThread thread : threads)
			thread.go();
	}
	
	static class TempThread{
		List<File> dngs;
		List<String> tifs;
		String id;
		private Thread thread;
		public TempThread(String id)
		{
			this.id = id;
			dngs = new ArrayList<>();
			tifs = new ArrayList<>();
		}
		public void go()
		{
			if (thread != null && thread.isAlive())
				return;
			thread = new Thread(new Runnable() {
				@Override
				public void run()
				{
					ProgressUI ui = ProgressUI.create("DNG to TIFF "+id, 20);
					for (int i = 0; i < dngs.size(); i++)
					{
						File dng = dngs.get(i);
						String tif = tifs.get(i);
						ui.newLine("Progress "+(i+1)+"/"+dngs.size()+": "+dng.getAbsolutePath());
						long time = System.currentTimeMillis();
						Images.dngToTiff(dng, tif);
						double secs = (System.currentTimeMillis()-time)/1000.0;
						ui.newLine("Exec time: " + secs+" seconds.");
					}
					ui.newLine("All Done.");
				}
			});
			thread.start();
		}
	}

	

	
	public static boolean validate(File f)
	{
		
		return true;
	}
}
