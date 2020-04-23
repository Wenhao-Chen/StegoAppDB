package app_analysis.old;

import java.util.HashSet;
import java.util.Set;

public class APISignatures {

	
	public static final String[] BitmapFactorySigs = {
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
	};
	
	public static final Set<String> LoadBitmapSigs = new HashSet<String>() {
		private static final long serialVersionUID = 1L;

	{
		add("Landroid/provider/MediaStore$Images$Media;->getBitmap(Landroid/content/ContentResolver;Landroid/net/Uri;)Landroid/graphics/Bitmap;");
		for (String s : BitmapFactorySigs)
			add(s);
	}};
	
	public static final String[] canvasAPIs = {
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
	
	public static final String[] bitmapAPIs = {
			
		//0 (static) Returns a bitmap from subset of the source bitmap, 
		//	transformed by the optional matrix.
		"Landroid/graphics/Bitmap;->createBitmap(Landroid/graphics/Bitmap;IIIILandroid/graphics/Matrix;Z)Landroid/graphics/Bitmap;",
		
		//1 (static) Returns a immutable bitmap with the specified width and height, 
		//  with each pixel value set to the corresponding value in the colors array.
		"Landroid/graphics/Bitmap;->createBitmap([IIILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;",
		//2 (static) Returns a mutable bitmap with the specified width and height.
		"Landroid/graphics/Bitmap;->createBitmap(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;",
		//3 (static) Returns a bitmap from the specified subset of the source bitmap.
		"Landroid/graphics/Bitmap;->createBitmap(Landroid/graphics/Bitmap;IIII)Landroid/graphics/Bitmap;",
		//4 (static) Returns a immutable bitmap with the specified width and height, 
		//	with each pixel value set to the corresponding value in the colors array
		"Landroid/graphics/Bitmap;->createBitmap([IIIIILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;",
		
		//5 Returns a single pixel value
		"Landroid/graphics/Bitmap;->getPixel(II)I",
		
		//6 Set the value of a single pixel
		"Landroid/graphics/Bitmap;->setPixel(III)V",
		
		//7 Returns in pixels[] a copy of the data in the bitmap.
		"Landroid/graphics/Bitmap;->getPixels([IIIIIII)V",
		
		//8 Creates a new bitmap, scaled from an existing bitmap, when possible.
		"Landroid/graphics/Bitmap;->createScaledBitmap(Landroid/graphics/Bitmap;IIZ)Landroid/graphics/Bitmap;",
		
		//9 Copy the bitmap's pixels into the specified buffer (allocated by the caller).
		"Landroid/graphics/Bitmap;->copyPixelsToBuffer(Ljava/nio/Buffer;)V",
		
		//10 Write a compressed version of the bitmap to the specified outputstream.
		"Landroid/graphics/Bitmap;->compress(Landroid/graphics/Bitmap$CompressFormat;ILjava/io/OutputStream;)Z",
		
		//11 Replace pixels in the bitmap with the colors in the array.
		"Landroid/graphics/Bitmap;->setPixels([IIIIIII)V",
		
		//12 Copy the pixels from the buffer, beginning at the current position, overwriting the bitmap's pixels.
		"Landroid/graphics/Bitmap;->copyPixelsFromBuffer(Ljava/nio/Buffer;)V",
	};
	
	public static final String[] CFGKeywords = {
			"Ljava/lang/String;->charAt(I)C",
			"Landroid/graphics/Bitmap;->createBitmap(Landroid/graphics/Bitmap;IIIILandroid/graphics/Matrix;Z)Landroid/graphics/Bitmap;",
			"Landroid/graphics/Bitmap;->createBitmap([IIILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;",
			"Landroid/graphics/Bitmap;->createBitmap(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;",
			"Landroid/graphics/Bitmap;->createBitmap(Landroid/graphics/Bitmap;IIII)Landroid/graphics/Bitmap;",
			"Landroid/graphics/Bitmap;->createBitmap([IIIIILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;",
			"Landroid/graphics/Bitmap;->getPixel(II)I",
			"Landroid/graphics/Bitmap;->setPixel(III)V",
			"Landroid/graphics/Bitmap;->getPixels([IIIIIII)V",
			"Landroid/graphics/Bitmap;->createScaledBitmap(Landroid/graphics/Bitmap;IIZ)Landroid/graphics/Bitmap;",
			"Landroid/graphics/Bitmap;->copyPixelsToBuffer(Ljava/nio/Buffer;)V",
			"Landroid/graphics/Bitmap;->compress(Landroid/graphics/Bitmap$CompressFormat;ILjava/io/OutputStream;)Z",
			"Landroid/graphics/Bitmap;->setPixels([IIIIIII)V",
			"Landroid/graphics/Bitmap;->copyPixelsFromBuffer(Ljava/nio/Buffer;)V",
	};
	
	public static boolean isGettingPixels(String invokeSig)
	{
		return  invokeSig.equals(bitmapAPIs[7]) || 
				invokeSig.equals(bitmapAPIs[9]); 
	}
	
	public static boolean isSettingPixels(String invokeSig)
	{
		return  invokeSig.equals(bitmapAPIs[1])  || 
				invokeSig.equals(bitmapAPIs[4])  || 
				invokeSig.equals(bitmapAPIs[11]) ||
				invokeSig.equals(bitmapAPIs[12]); 
	}
	
	public static boolean isGettingSinglePixel(String invokeSig)
	{
		return invokeSig.equals(bitmapAPIs[5]);
	}
	public static boolean isSettingSinglePixel(String invokeSig)
	{
		return invokeSig.equals(bitmapAPIs[6]);
	}
	
	public static boolean isBitmapCompress(String invokeSig)
	{
		return invokeSig.equals(bitmapAPIs[10]);
	}
	
	
	public static final String GetPixel = "Landroid/graphics/Bitmap;->getPixel(II)I";
	public static final String SetPixel = "Landroid/graphics/Bitmap;->setPixel(III)V";
}
