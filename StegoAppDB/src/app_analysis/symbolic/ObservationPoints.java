package app_analysis.symbolic;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import apex.APEXApp;
import apex.bytecode_wrappers.APEXClass;
import apex.bytecode_wrappers.APEXMethod;
import apex.bytecode_wrappers.APEXStatement;
import app_analysis.common.Dirs;
import util.P;

@SuppressWarnings("unchecked")
public class ObservationPoints {

	public static void main(String[] args)
	{
		//TODO: get some statistics on the Bitmap APIs
		List<File> stegoApps = Dirs.getFiles(Dirs.Stego_Github);
		for(File f : stegoApps) if(f.getName().endsWith(".apk")) {
			APEXApp app = new APEXApp(f);
			collectOPStatements(app);
		}
	}

	// simply check every smali statement that is an invoke statement
	// if the invoke signature belongs to one of the 4 groups of 
	// Bitmap api, count them.
	public static List<APEXStatement>[] collectOPStatements(APEXApp app) {
		if (app == null)
			return null;
		
		List<APEXStatement>[] obpoints = new ArrayList[4];
		for (int i=0; i<4; i++)
			obpoints[i] = new ArrayList<>();
		
		for (APEXClass c : app.getNonLibraryClasses())
		for (APEXMethod m : c.methods.values())
		for (APEXStatement s : m.statements) 
		if (s.isInvokeStmt()) {
			String sig = s.getInvokeSignature();
			if (isCreateBitmapAPI(sig))
				obpoints[0].add(s);
			if (isReadBitmapAPI(sig))
				obpoints[1].add(s);
			if (isWriteBitmapAPI(sig))
				obpoints[2].add(s);
			if (isSinkBitmapAPI(sig))
				obpoints[3].add(s);
		}
		P.pf("%s\t%d\t%d\t%d\t%d\n", app.packageName, 
				obpoints[0].size(), obpoints[1].size(),
				obpoints[2].size(), obpoints[3].size());
		/*
		P.pf("==== Observation Points for '%s'", app.packageName);
		final String[] names = {"Create", "Read", "Write", "Sink"};
		for (int i=0; i<4; i++) if (!obpoints[i].isEmpty()){
			P.pf("- %s Bitmap -\n", names[i]);
			for (String sig : obpoints[i])
				P.pf("  %s\n", sig);
		}
		P.p("\n\n"); */
		return obpoints;
	}
	
	public static Set<APEXMethod>[] collectOPMethods(APEXApp app) {
		if (app == null)
			return null;
		
		Set<APEXMethod>[] obpoints = new HashSet[4];
		for (int i=0; i<4; i++)
			obpoints[i] = new HashSet<>();
		for (APEXClass c : app.getNonLibraryClasses())
		for (APEXMethod m : c.methods.values())
		for (APEXStatement s : m.statements) 
		if (s.isInvokeStmt()) {
			String sig = s.getInvokeSignature();
			if (isCreateBitmapAPI(sig))
				obpoints[0].add(m);
			if (isReadBitmapAPI(sig))
				obpoints[1].add(m);
			if (isWriteBitmapAPI(sig))
				obpoints[2].add(m);
			if (isSinkBitmapAPI(sig))
				obpoints[3].add(m);
		}
		return obpoints;
	}
	
	public static Set<APEXMethod>[] collectOPMethods(File apk) {
		if (!apk.getName().endsWith(".apk"))
			return null;
		return collectOPMethods(new APEXApp(apk));
	}
	
	public static List<APEXStatement>[] collectOPStatements(File apk) {
		if (!apk.getName().endsWith(".apk"))
			return null;
		return collectOPStatements(new APEXApp(apk));
	}
	
	private static Set<String> apis_create_bitmap, apis_write_bitmap, apis_read_bitmap, apis_sink_bitmap;
	
	public static boolean isCreateBitmapAPI(String methodSig) {
		if (apis_create_bitmap == null)
			apis_create_bitmap = new HashSet<>(Arrays.asList(API_Create_Bitmap));
		return apis_create_bitmap.contains(methodSig);
	}
	
	public static boolean isReadBitmapAPI(String methodSig) {
		if (apis_read_bitmap == null)
			apis_read_bitmap = new HashSet<>(Arrays.asList(API_Read_Bitmap));
		return apis_read_bitmap.contains(methodSig);
	}
	
	public static boolean isWriteBitmapAPI(String methodSig) {
		if (apis_write_bitmap == null)
			apis_write_bitmap = new HashSet<>(Arrays.asList(API_Write_Bitmap));
		return apis_write_bitmap.contains(methodSig);
	}
	
	public static boolean isSinkBitmapAPI(String methodSig) {
		if (apis_sink_bitmap == null)
			apis_sink_bitmap = new HashSet<>(Arrays.asList(API_Sink_Bitmap));
		return apis_sink_bitmap.contains(methodSig);
	}
	
	/*TODO: add the 3rd party APIs into API_Create_Bitmap and API_Sink_Bitmap:
		Lcom/squareup/picasso
		Lcom/bumptech/glide
		Ljp/wasabeef/glide
		Lcom/facebook/imagepipeline
		etc
	*/
	public static final String[] API_Create_Bitmap = {
			"Landroid/graphics/BitmapFactory;->decodeFileDescriptor(Ljava/io/FileDescriptor;Landroid/graphics/Rect;Landroid/graphics/BitmapFactory$Options;)Landroid/graphics/Bitmap;",
			"Landroid/graphics/BitmapFactory;->decodeResourceStream(Landroid/content/res/Resources;Landroid/util/TypedValue;Ljava/io/InputStream;Landroid/graphics/Rect;Landroid/graphics/BitmapFactory$Options;)Landroid/graphics/Bitmap;",
			"Landroid/graphics/BitmapFactory;->decodeByteArray([BII)Landroid/graphics/Bitmap;",
			"Landroid/graphics/BitmapFactory;->decodeStream(Ljava/io/InputStream;Landroid/graphics/Rect;Landroid/graphics/BitmapFactory$Options;)Landroid/graphics/Bitmap;",
			"Landroid/graphics/BitmapFactory;->decodeFile(Ljava/lang/String;)Landroid/graphics/Bitmap;",
			"Landroid/graphics/BitmapFactory;->decodeByteArray([BIILandroid/graphics/BitmapFactory$Options;)Landroid/graphics/Bitmap;",
			"Landroid/graphics/BitmapFactory;->decodeFileDescriptor(Ljava/io/FileDescriptor;)Landroid/graphics/Bitmap;",
			"Landroid/graphics/BitmapFactory;->decodeResource(Landroid/content/res/Resources;ILandroid/graphics/BitmapFactory$Options;)Landroid/graphics/Bitmap;",
			"Landroid/graphics/BitmapFactory;->decodeResource(Landroid/content/res/Resources;I)Landroid/graphics/Bitmap;",
			"Landroid/graphics/BitmapFactory;->decodeFile(Ljava/lang/String;Landroid/graphics/BitmapFactory$Options;)Landroid/graphics/Bitmap;",
			"Landroid/graphics/BitmapFactory;->decodeStream(Ljava/io/InputStream;)Landroid/graphics/Bitmap;",
			"Landroid/provider/MediaStore$Images$Media;->getBitmap(Landroid/content/ContentResolver;Landroid/net/Uri;)Landroid/graphics/Bitmap;",
			"Landroid/graphics/Bitmap;->createBitmap(Landroid/graphics/Bitmap;IIIILandroid/graphics/Matrix;Z)Landroid/graphics/Bitmap;",
			"Landroid/graphics/Bitmap;->createBitmap([IIILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;",
			"Landroid/graphics/Bitmap;->createBitmap(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;",
			"Landroid/graphics/Bitmap;->createBitmap(Landroid/graphics/Bitmap;IIII)Landroid/graphics/Bitmap;",
			"Landroid/graphics/Bitmap;->createBitmap([IIIIILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;",
			"Landroid/graphics/Bitmap;->createScaledBitmap(Landroid/graphics/Bitmap;IIZ)Landroid/graphics/Bitmap;",
	};
	
	public static final String[] API_Sink_Bitmap = {
			"Landroid/graphics/Bitmap;->compress(Landroid/graphics/Bitmap$CompressFormat;ILjava/io/OutputStream;)Z",
	};
	
	
	public static final String[] API_Read_Bitmap = {
			"Landroid/graphics/Bitmap;->getPixel(II)I",
			"Landroid/graphics/Bitmap;->getPixels([IIIIIII)V",
			"Landroid/graphics/Bitmap;->copyPixelsToBuffer(Ljava/nio/Buffer;)V",
	};
	
	public static final String[] API_Write_Bitmap = {
			"Landroid/graphics/Bitmap;->setPixel(III)V",
			"Landroid/graphics/Bitmap;->setPixels([IIIIIII)V",
			"Landroid/graphics/Bitmap;->copyPixelsFromBuffer(Ljava/nio/Buffer;)V",
	};
	
	// NOTE: currently not considering Canvas APIs
	public static final String[] API_Write_Bitmap_by_Canvas = {
			"Landroid/graphics/Canvas;-><init>()V",
			"Landroid/graphics/Canvas;-><init>(Landroid/graphics/Bitmap;)V",
			"Landroid/graphics/Canvas;->drawARGB(IIII)V",
			"Landroid/graphics/Canvas;->drawArc(Landroid/graphics/RectF;FFZLandroid/graphics/Paint;)V",
			"Landroid/graphics/Canvas;->drawBitmap(Landroid/graphics/Bitmap;FFLandroid/graphics/Paint;)V",
			"Landroid/graphics/Canvas;->drawBitmap(Landroid/graphics/Bitmap;Landroid/graphics/Matrix;Landroid/graphics/Paint;)V",
			"Landroid/graphics/Canvas;->drawBitmap(Landroid/graphics/Bitmap;Landroid/graphics/Rect;Landroid/graphics/Rect;Landroid/graphics/Paint;)V",
			"Landroid/graphics/Canvas;->drawBitmap(Landroid/graphics/Bitmap;Landroid/graphics/Rect;Landroid/graphics/RectF;Landroid/graphics/Paint;)V",
			"Landroid/graphics/Canvas;->drawBitmapMesh(Landroid/graphics/Bitmap;II[FI[IILandroid/graphics/Paint;)V",
			"Landroid/graphics/Canvas;->drawCircle(FFFLandroid/graphics/Paint;)V",
			"Landroid/graphics/Canvas;->drawColor(I)V",
			"Landroid/graphics/Canvas;->drawColor(ILandroid/graphics/PorterDuff$Mode;)V",
			"Landroid/graphics/Canvas;->drawLine(FFFFLandroid/graphics/Paint;)V",
			"Landroid/graphics/Canvas;->drawLines([FIILandroid/graphics/Paint;)V",
			"Landroid/graphics/Canvas;->drawLines([FLandroid/graphics/Paint;)V",
			"Landroid/graphics/Canvas;->drawOval(FFFFLandroid/graphics/Paint;)V",
			"Landroid/graphics/Canvas;->drawOval(Landroid/graphics/RectF;Landroid/graphics/Paint;)V",
			"Landroid/graphics/Canvas;->drawPaint(Landroid/graphics/Paint;)V",
			"Landroid/graphics/Canvas;->drawPath(Landroid/graphics/Path;Landroid/graphics/Paint;)V",
			"Landroid/graphics/Canvas;->drawPoint(FFLandroid/graphics/Paint;)V",
			"Landroid/graphics/Canvas;->drawRGB(III)V",
			"Landroid/graphics/Canvas;->drawRect(FFFFLandroid/graphics/Paint;)V",
			"Landroid/graphics/Canvas;->drawRect(Landroid/graphics/Rect;Landroid/graphics/Paint;)V",
			"Landroid/graphics/Canvas;->drawRect(Landroid/graphics/RectF;Landroid/graphics/Paint;)V",
			"Landroid/graphics/Canvas;->drawRoundRect(FFFFFFLandroid/graphics/Paint;)V",
			"Landroid/graphics/Canvas;->drawRoundRect(Landroid/graphics/RectF;FFLandroid/graphics/Paint;)V",
			"Landroid/graphics/Canvas;->drawText(Ljava/lang/CharSequence;IIFFLandroid/graphics/Paint;)V",
			"Landroid/graphics/Canvas;->drawText(Ljava/lang/String;FFLandroid/graphics/Paint;)V",
			"Landroid/graphics/Canvas;->drawText(Ljava/lang/String;IIFFLandroid/graphics/Paint;)V",
			"Landroid/graphics/Canvas;->drawText([CIIFFLandroid/graphics/Paint;)V",
			"Landroid/graphics/Canvas;->drawTextOnPath(Ljava/lang/String;Landroid/graphics/Path;FFLandroid/graphics/Paint;)V",
			"Landroid/graphics/Canvas;->setBitmap(Landroid/graphics/Bitmap;)V",
	};
}
