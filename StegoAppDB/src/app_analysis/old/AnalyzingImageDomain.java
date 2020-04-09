package app_analysis.old;

import java.io.BufferedReader;
import java.io.File;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import apex.APEXApp;
import apex.bytecode_wrappers.APEXClass;
import apex.bytecode_wrappers.APEXMethod;
import apex.bytecode_wrappers.APEXStatement;
import app_analysis.APISignatures;
import ui.ProgressUI;
import util.F;
import util.P;

public class AnalyzingImageDomain {

	/**
	 * Summary:
	 * 
	 * APIs used for loading images files into Bitmaps
	 *   - 11 BitmapFactory methods (see wenhaoc.app_analysis.MethodSignatures.class)
	 *   - MediaStore.Images.Media.getBitmap()
	 *   - 3rd party libraries:
	 *   	- Lcom/nostra13/universalimageloader;
	 *   	- Picasso. Lcom/squareup/picasso;
	 *  	- Glide. com.bumptech.glide;, Ljp/wasabeef/glide;
	 *   	- Fresco. Lcom/facebook/imagepipeline;
	 *   	
	 *   
	 * API used for changing spatial image values:
	 *   - Bitmap.SetPixel()
	 *   - Bitmap.SetPixels()
	 *   - Bitmap.CopyPixelsFromBuffer()
	 * 
	 * 
	 * */
	
	static final String apkRoot = "C:/workspace/app_analysis/apks";
	static final String apkDir = apkRoot+"/stego";
	static final File decodedDir = new File("C:\\workspace\\app_analysis\\decoded");
	static ProgressUI ui;
	
	public static void main(String[] args)
	{

		ui = ProgressUI.create("progress", 20);
		
		File recordDir = new File("C:\\Users\\C03223-Stego2\\Desktop");
		File recordF = new File(recordDir, "invoke_record.csv");
		File newRecordF = new File(recordDir, "invoke_record_grouped.csv");
		PrintWriter out = F.initPrintWriter(newRecordF);
		Map<String, String> record = new HashMap<>();
		List<String> recordLines = F.readLinesWithoutEmptyLines(recordF);
		for (String line : recordLines)
			record.put(line.substring(0, line.indexOf("\t")), line);
		ProgressUI ui2 = ProgressUI.create("App progress", 20);
		int app_index = record.size();
		APEXApp.justDecode = true;
		for (File dir : new File(apkRoot).listFiles()) if (!dir.getName().equals("instrumented")) for(File f : dir.listFiles()) if (f.getName().endsWith(".apk"))
		{
			
			if (!record.containsKey(f.getName()))
			{
				ui2.newLine("doing app "+(++app_index)+": "+f.getAbsolutePath());
				APEXApp app = new APEXApp(f);
				ui2.newLine("processing...");
				boolean[] result = new boolean[10];
				// 0 - Android Bitmap loading APIs
				// 1 - Picasso bitmap load
				// 2 - Glide 
				// 3 - Fresco
				// 4 - Bitmap set pixel
				// 5 - Bitmap set pixels
				// 6 - Bitmap copy pixels from buffer
				// 7 - RGB to Y
				// 8 - RGB to Cb
				// 9 - RGB to Cr
				app.otherSmaliDirs.add(app.smaliDir);
				for (File smaliDir : app.otherSmaliDirs)
				{
					parseFile(smaliDir, result);
				}
				
				String entry = f.getName();
				for (boolean b : result)
					entry += "\t"+b;
				record.put(f.getName(), entry);
				F.writeLine(entry, recordF, true);
			}
			
			out.printf("%s\t%s\n", dir.getName(), record.get(f.getName()));
		}
		out.close();
		P.p("Done.");
	}
	
	static String[] rgbToY = {"0x3fd322d0e5604189L", "0x3fe2c8b439581062L", "0x3fbd2f1a9fbe76c9L"};
	static String[] rgbToCb = {"-0x403a66ba493c89f4L", "0x3fd5335d249e44faL", "0x3fe0000000000000L"};
	static String[] rgbToCr = {"0x3fe0000000000000L", "0x3fdacbd1244a6224L", "0x3fb4d0bb6ed67770L"};
	static void parseFile(File f, boolean[] result)
	{
		if (f.isDirectory()) for(File ff : f.listFiles())
		{
			parseFile(ff, result);
		}
		else if (f.getName().endsWith(".smali"))
		{
			boolean[] y = new boolean[3];
			boolean[] cb = new boolean[3];
			boolean[] cr = new boolean[3];
			ui.newLine("reading file "+f.getAbsolutePath());
			BufferedReader in = null;
			try
			{
				in = F.initReader(f);
				String line;
				while ((line=in.readLine())!=null)
				{
					if (line.startsWith("    invoke-")) 
					{
						String sig = line.substring(line.lastIndexOf(" ")+1);
						parseSig(sig, result);
					}
					else if (line.startsWith("    const-wide"))
					{
						for(int i=0; i<3; i++) if(line.contains(rgbToY[i]))
						{	y[i] = true;}
						for(int i=0; i<3; i++) if(line.contains(rgbToCb[i]))
						{	cb[i] = true;}
						for(int i=0; i<3; i++) if(line.contains(rgbToCr[i]))
						{	cr[i] = true;}
						
						if (y[0]&&y[1]&&y[2])
						{	result[7] = true; }
						if (cb[0]&&cb[1]&&cb[2])
						{	result[8] = true; }
						if (cr[0]&&cr[1]&&cr[2])
						{	result[9] = true; }
					}
				}
				
				in.close();
				
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		else
		{
			P.e("unknown file: "+f.getAbsolutePath());
			P.pause();
		}
	}
	
	static void parseSig(String sig, boolean[] result)
	{
		if (APISignatures.LoadBitmapSigs.contains(sig))
			result[0] = true;
		else if (sig.endsWith("Landroid/graphics/Bitmap;"))
		{
			if (sig.startsWith("Lcom/squareup/picasso"))
				result[1] = true;
			else if (sig.startsWith("com.bumptech.glide") || sig.startsWith("Ljp/wasabeef/glide"))
				result[2] = true;
			else if (sig.startsWith("Lcom/facebook/imagepipeline"))
				result[3] = true;
		}
		else if (sig.equals(sigSetPixel))
			result[4] = true;
		else if (sig.equals(sigSetPixels))
			result[5] = true;
		else if (sig.equals(sigcopyFromBuffer))
			result[6] = true;
	}
	
	static void process(APEXApp app)
	{
		boolean hasBFD = hasBitmapfactorydecode(app);
		boolean hasBSP = hasBitmapSetPixel(app);
		P.p(app.packageName+"\t"+hasBFD+"\t"+hasBSP);
	}
	
	static final String sigSetPixel = "Landroid/graphics/Bitmap;->setPixel(III)V";
	static final String sigSetPixels = "Landroid/graphics/Bitmap;->setPixels([IIIIIII)V";
	static final String sigcopyFromBuffer = "Landroid/graphics/Bitmap;->copyPixelsFromBuffer(Ljava/nio/Buffer;)V";
	static boolean hasBitmapSetPixel(APEXApp app)
	{
		for (APEXClass c : app.getNonLibraryClasses())
		for (APEXMethod m : c.methods.values())
		for (APEXStatement s : m.statements)
		if (s.isInvokeStmt())
		{
			String sig = s.getInvokeSignature();
			if (sig.equals(sigSetPixel) || sig.equals(sigSetPixels) || sig.contentEquals(sigcopyFromBuffer))
				return true;
		}
		return false;
	}
	
	static final String bfdSigPrefix = "Landroid/graphics/BitmapFactory;->decode";
	static boolean hasBitmapfactorydecode(APEXApp app)
	{
		for (APEXClass c : app.getNonLibraryClasses())
		for (APEXMethod m : c.methods.values())
		for (APEXStatement s : m.statements)
		if (s.isInvokeStmt())
		{
			if (APISignatures.LoadBitmapSigs.contains(s.getInvokeSignature()))
				return true;
		}
		return false;
	}

}
