package database;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import ui.ProgressUI;
import util.F;
import util.Images;
import util.P;

public class Backup {

	static final File root = new File("E:/stegodb_March2019");
	static final File auxRoot = new File(root, "_aux_data");
	static final File remoteRoot = new File("G:/stegodb_March2019_JPG_DNG_TIF");
	
	public static void main(String[] args)
	{
		ProgressUI ui = ProgressUI.create("Backup", 20);
		
		double total_gb = 0;
		for (File d1 : root.listFiles())
		{
			if (d1.getName().startsWith("_"))
				continue;
			long gb = 0;
			File d2 = new File(remoteRoot, d1.getName());
			d2.mkdirs();
			
			File aux = new File(auxRoot, d1.getName());
			
			File ori = new File(d1, "originals");
			File t1 = new File(aux, "tif");
			File j = new File(d2, "JPG"); j.mkdirs();
			File d = new File(d2, "DNG"); d.mkdirs();
			File t2 = new File(d2, "TIF"); t2.mkdirs();
			
			P.pf("---- %s ----\nORI: %s %d\nTIF: %s %d\nRemote JPG: %s %d\nRemote DNG: %s %d\nRemote TIF: %s %d\n",
					d1.getName(), 
					ori.getAbsolutePath(), ori.list().length,
					t1.getAbsolutePath(), t1.list().length,
					j.getAbsolutePath(), j.list().length,
					d.getAbsolutePath(), d.list().length,
					t2.getAbsolutePath(), t2.list().length);
			
			List<CopyJob> copyJobs = new ArrayList<>();
			List<TIFJob> tifJobs = new ArrayList<>();
			for (File f : ori.listFiles())
			{
				String ext = F.getFileExt(f);
				if (ext.equals("jpg"))
				{
					File f2 = new File(j, f.getName());
					if (!f2.exists() || f.length()!=f2.length())
					{
						copyJobs.add(new CopyJob(f, f2));
						gb += f.length();
					}
				}
				else if (ext.equals("dng"))
				{
					File f2 = new File(d, f.getName());
					if (!f2.exists() || f.length()!=f2.length())
					{
						copyJobs.add(new CopyJob(f, f2));
						gb += f.length();
					}
					File tif1 = new File(t1, f2.getName().replace(".dng", ".tif"));
					File tif2 = new File(t2, tif1.getName());
					
					if (!tif2.exists())
					{
						gb += 36500000;
						if (tif1.exists())
							copyJobs.add(new CopyJob(tif1, tif2));
						else
							tifJobs.add(new TIFJob(f, tif2));
					}
				}
				else
				{
					P.e("extention??? "+f.getAbsolutePath());
					P.pause();
				}
			}
			
			double dd = gb/(1024.0*1024.0*1024.0);
			
			P.pf("CopyJobs: %d, TIFJobs: %d, GBs: %.2f\n", copyJobs.size(), tifJobs.size(), dd);
			
			for (int i = 0; i < copyJobs.size(); i++)
			{
				CopyJob jj = copyJobs.get(i);
				ui.newLine("[COPY "+(i+1)+"/"+copyJobs.size()+"] "+jj.from.getAbsolutePath()+" to "+jj.to.getAbsolutePath());
				F.copy(jj.from, jj.to);
			}
			for (int i = 0; i < tifJobs.size(); i++)
			{
				TIFJob jj = tifJobs.get(i);
				ui.newLine("[TIFF "+(i+1)+"/"+tifJobs.size()+"] "+jj.from.getAbsolutePath()+" to "+ jj.to.getAbsolutePath());
				Images.dngToTiff(jj.from, jj.to.getAbsolutePath());
			}
		}
	}
	
	static class CopyJob {
		File from, to;
		CopyJob(File f1, File f2) {from=f1; to=f2;}
	}
	
	static class TIFJob {
		File from, to;
		TIFJob(File f1, File f2) {from=f1; to=f2;}
	}

}
