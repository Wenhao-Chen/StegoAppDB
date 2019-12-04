package database;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import database.objects.DBDevice;
import database.objects.DBStego;
import database.objects.MainDB;
import stego.apps.Pictograph;
import stego.apps.passlok.Passlok;
import ui.ProgressUI;
import util.F;
import util.Images;
import util.P;

public class Validate{
	

	public static void main(String[] args)
	{
		MainDB db = new MainDB("E:/stegodb_March2019");
		Images.ui = ProgressUI.create("Image Operations", 20);
		Pictograph.ui = ProgressUI.create("Pictograph", 20);
		Passlok.ui = ProgressUI.create("Passlok", 20);
		DBDevice.ui = ProgressUI.create("Device Operations", 20);
		DBStego.ui = ProgressUI.create("Stego Validation", 20);
		
		File skipF = new File("E:/stegodb_March2019/_records/SkipValidation.txt");
		Set<String> toSkip = new HashSet<>();
		toSkip.addAll(F.readLinesWithoutEmptyLines(skipF));
		
		Set<DBDevice> ready = new HashSet<>();
		String jobPath = "E:/matlab_rgb2gray_jobs_"+P.getTimeString()+".txt";
		PrintWriter matlabJobs = F.initPrintWriter(jobPath);
		for (DBDevice d : db.devices)
		{
			//if (!d.name.equals("Pixel1-2"))
			//	continue;
			//// stage 1 - automatic validation: scene count, readability, manual settings
			P.p("-- "+d.name +" "+d.getScenes().size()+" scenes");
			if (!toSkip.contains(d.name))
			{
				int sceneCount = d.getScenes().size();
				if (sceneCount<100)
				{
					P.p("    not enough scenes - has "+sceneCount+" currently.");
					continue;
				}
				else
				{
					P.p("    scene count: "+sceneCount);
				}
				boolean readyForSceneContentVal = d.validateOriginals();
				
				if (!readyForSceneContentVal)
				{
					P.p("    there are some bad original images. Check the records file for details.");
					continue;
				}
				
				//// stage 2 - manual validation: scene content
				if (!db.devicesWithValidatedSceneContent.contains(d.name))
				{
					P.p("    need to manually validate scene content and update \""+db.recordsDir.getAbsolutePath()+"\\SceneContentValidated.txt\"");
					continue;
				}
				
				//// stage 3 - run matlab to convert color PNG to grayscale
				int matlabJobCount = d.makePNGs(matlabJobs);
				if (matlabJobCount > 0)
				{
					P.p("    need to run matlab rgb2gray script. Jobs recorded in: " + jobPath);
					matlabJobs.flush();
					continue;
				}
				
				//// stage 4 - generate stegos
				////	for iPhones: stegos are automatically generated here
				if (d.isIPhone())
				{
					P.p("    checking pictograph stegos...");
					d.makePictographStegos(false);
				}
				else
				{
					P.p("    checking passlok stegos... ");
					d.makePasslokStegos(false);
				}
				
				System.out.print("    validating... ");
				boolean validated = d.validateAll();
				System.out.print(validated?"passed. Ready for database.\n":"failed. Check the records file for details.\n");
				if (validated)
					ready.add(d);
			}
			else
				ready.add(d);
		}
		matlabJobs.close();
		List<String> list = new ArrayList<>();
		for (DBDevice d : ready)	list.add(d.name);
		F.write(list, skipF, false);
		
		//NOTE: generate the two CSVs for CSSM
		File generalCSV = new File(db.recordsDir, "general.csv");
		File relationCSV = new File(db.recordsDir, "relations.csv");
		
		P.p("==== Writing csv files for CSSM ====");
		PrintWriter general = F.initPrintWriter(generalCSV.getAbsolutePath());
		PrintWriter relation = F.initPrintWriter(relationCSV.getAbsolutePath());
		general.write(String.join(",", CSSM.generalInfoFields)+"\n");
		relation.write(String.join(",", CSSM.relationInfoFields)+"\n");
		
		for (DBDevice d : db.devices)
		{
			if (ready.contains(d))
			{
				P.p("  processing " + d.name+"...");
				d.writeCSVs(general, relation, false);
			}
		}
		general.close();
		relation.close();
		P.p("==== All Done. ====\nCSV files are located at " + db.recordsDir.getAbsolutePath());
	}
	

}
