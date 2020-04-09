package app_analysis.old;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import apex.APEXApp;
import apex.bytecode_wrappers.APEXMethod;
import apex.symbolic.APEXObject;
import apex.symbolic.VM;
import apex.symbolic.APEXObject.BitmapAccess;
import apex.symbolic.solver.Arithmetic;
import util.F;
import util.P;

public class FixStegoDetection {
	
	static final File selectedTreeRoot = new File("C:\\workspace\\app_analysis\\notes\\Selected Expression Trees");
	static final File treeRoot = new File("C:\\workspace\\app_analysis\\notes\\ExpressionTrees");

	static final String[] appsToSolve = {
			"C:\\workspace\\app_analysis\\apks\\stego_github\\com.example.siyangzhang.steganography.apk", // this app embeds in HSV
			"C:\\workspace\\app_analysis\\apks\\stego_github\\com.example.stegnography.apk",
			"C:\\workspace\\app_analysis\\apks\\stego_github\\com.team.redacted.stegogram.apk",
			"C:\\workspace\\app_analysis\\apks\\stego_github\\com.umitsilwal.stegogram.apk",
			"C:\\workspace\\app_analysis\\apks\\stego_github\\com.zheryu.steganography_android.apk",
			"C:\\workspace\\app_analysis\\apks\\stego_other\\com.sg.steganography.apk",
			"C:\\workspace\\app_analysis\\apks\\stego_other\\data.main.apk",
			"C:\\workspace\\app_analysis\\apks\\stego_playstore\\com.meznik.Steganography.apk",
			"C:\\workspace\\app_analysis\\apks\\stego_playstore\\com.pixellato.bumble.apk",
			"C:\\workspace\\app_analysis\\apks\\stego_playstore\\com.pixeloop.stegnotool.apk",
			"C:\\workspace\\app_analysis\\apks\\stego_playstore\\com.software.brokoli.secretmessage.apk",
			"C:\\workspace\\app_analysis\\apks\\stego_playstore\\com.steganochipher.stegano.apk",
			"C:\\workspace\\app_analysis\\apks\\stego_playstore\\com.talixa.pocketstego.apk",
			"C:\\workspace\\app_analysis\\apks\\stego_playstore\\com.talpro213.steganographyimage.apk",
			"C:\\workspace\\app_analysis\\apks\\stego_playstore\\dev.nia.niastego.apk",
			"C:\\workspace\\app_analysis\\apks\\stego_playstore\\it.kryfto.apk",
			"C:\\workspace\\app_analysis\\apks\\stego_playstore\\it.mobistego.apk",
			"C:\\workspace\\app_analysis\\apks\\stego_playstore\\jubatus.android.davinci.apk",
			"C:\\workspace\\app_analysis\\apks\\stego_playstore\\stega.jj.bldg5.steganography.apk",
	};
	
	public static void main(String[] args)
	{
		APEXApp app = new APEXApp(new File(appsToSolve[3]));
		APEXMethod m = app.getMethod("Lcom/umitsilwal/stegogram/Steganography;->EncodeMessage(Ljava/io/File;Ljava/lang/String;)Ljava/io/File;");
		P.p(m.signature);
		
		VM.printDebug = true;
		VM.pathCount = 20000;
		VM.maxBranchVisitTime = 2000;
		Arithmetic.lazy = false;
		
		Queue<VM> q = new LinkedList<>();
		File treeDir = new File(treeRoot, app.apk.getName());
		treeDir.mkdirs();
		q.add(new VM(app, m));
		
		int count = 1;
		while (!q.isEmpty() && count < 200)
		{
			VM vm = q.poll();
			vm.execute(true);
			P.p("Done "+count++);
			int index = 1;
			boolean[] cond123 = new boolean[3];
			for (APEXObject obj : vm.heap.values())
			for (BitmapAccess ba : obj.bitmapHistory)
			if (ba.action.equals("setPixel"))
			{
				cond123[0] = MatchingExpressionTrees.HasGetPixel(ba.c);
				cond123[1] = MatchingExpressionTrees.HasGetPixel(ba.c, ba.x, ba.y);
				
			}
			q.addAll(vm.otherVMs);
		}
		P.p("All Done.");
	}
	
	
	
	
	static List<File> getAppsThatHaveNoTrees()
	{
		List<File> res = new ArrayList<>();
		Set<String> stegoAppNames = getStegoAppNames();
		for (File f : Template.getStegoAPKs())
		if (stegoAppNames.contains(f.getName()))
		{
			File treeDir = new File(selectedTreeRoot, f.getName());
			if (treeDir.list().length==0)
				res.add(f);
		}
		
		return res;
	}
	
	static Set<String> getStegoAppNames()
	{
		return new HashSet<>(F.readLinesWithoutEmptyLines(new File("C:\\Users\\C03223-Stego2\\git\\StegoAppDB\\StegoAppDB\\src\\app_analysis\\paper2020\\stego_app_names")));
	}
	
}
