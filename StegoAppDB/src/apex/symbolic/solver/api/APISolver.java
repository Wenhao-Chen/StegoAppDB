package apex.symbolic.solver.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import apex.bytecode_wrappers.APEXStatement;
import apex.symbolic.APEXObject;
import apex.symbolic.Expression;
import apex.symbolic.MethodContext;
import apex.symbolic.VM;
import util.P;

public class APISolver {
	
	public static Map<String, SolverInterface> solvers = new HashMap<>();
	private static StringSolver stringSolver;
	private static BitmapSolver bitmapSolver;
	private static CanvasSolver canvasSolver;
	private static PrimitiveWrapperSolver wrapperSolver;
	private static ArraySolver arraySolver;
	private static ThreadSolver threadSolver;
	
	static {
		stringSolver = new StringSolver();
		bitmapSolver = new BitmapSolver();
		canvasSolver = new CanvasSolver();
		wrapperSolver = new PrimitiveWrapperSolver();
		arraySolver = new ArraySolver();
		threadSolver = new ThreadSolver();
		solvers.put("Landroid/graphics/Bitmap;->getPixels([IIIIIII)V", bitmapSolver);
		solvers.put("Landroid/graphics/Bitmap;->getPixel(II)I", bitmapSolver);
		solvers.put("Landroid/graphics/Bitmap;->setPixel(III)V", bitmapSolver);
		solvers.put("Landroid/graphics/Bitmap;->setPixels([IIIIIII)V", bitmapSolver);
		solvers.put("Landroid/graphics/Bitmap;->getWidth()I", bitmapSolver);
		solvers.put("Landroid/graphics/Bitmap;->getHeight()I", bitmapSolver);
		solvers.put("Landroid/graphics/Bitmap;->copyPixelsToBuffer(Ljava/nio/Buffer;)V", bitmapSolver);
		solvers.put("Landroid/graphics/Bitmap;->copyPixelsFromBuffer(Ljava/nio/Buffer;)V", bitmapSolver);
		solvers.put("Landroid/graphics/Bitmap;->compress(Landroid/graphics/Bitmap$CompressFormat;ILjava/io/OutputStream;)Z", bitmapSolver);
		solvers.put("Landroid/graphics/Bitmap;->copy(Landroid/graphics/Bitmap$Config;Z)Landroid/graphics/Bitmap;", bitmapSolver);
		solvers.put("Landroid/graphics/Bitmap;->createBitmap(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;", bitmapSolver);
		solvers.put("Landroid/graphics/Bitmap;->createBitmap(Landroid/graphics/Bitmap;)Landroid/graphics/Bitmap;", bitmapSolver);
		solvers.put("Landroid/graphics/Bitmap;->createBitmap(Landroid/graphics/Bitmap;IIII)Landroid/graphics/Bitmap;", bitmapSolver);
		solvers.put("Landroid/graphics/Bitmap;->createBitmap(Landroid/graphics/Bitmap;IIIILandroid/graphics/Matrix;Z)Landroid/graphics/Bitmap;", bitmapSolver);
		solvers.put("Landroid/graphics/Bitmap;->createBitmap([IIIIILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;", bitmapSolver);
		solvers.put("Landroid/graphics/Bitmap;->createBitmap([IIILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;", bitmapSolver);
		solvers.put("Landroid/graphics/Bitmap;->createScaledBitmap(Landroid/graphics/Bitmap;IIZ)Landroid/graphics/Bitmap;", bitmapSolver);
		solvers.put("Landroid/graphics/Bitmap;->recycle()V", bitmapSolver);
		solvers.put("Landroid/graphics/Matrix;->postScale(FF)Z", bitmapSolver);
		
		solvers.put("Landroid/graphics/Color;->argb(IIII)I", bitmapSolver);
		solvers.put("Landroid/graphics/Color;->red(I)I", bitmapSolver);
		solvers.put("Landroid/graphics/Color;->green(I)I", bitmapSolver);
		solvers.put("Landroid/graphics/Color;->blue(I)I", bitmapSolver);
		solvers.put("Landroid/graphics/Color;->colorToHSV(I[F)V", bitmapSolver);
		solvers.put("Landroid/graphics/Color;->HSVToColor([F)I", bitmapSolver);
		
		solvers.put("Landroid/graphics/Canvas;-><init>(Landroid/graphics/Bitmap;)V", canvasSolver);
		solvers.put("Landroid/graphics/Canvas;->setBitmap(Landroid/graphics/Bitmap;)V", canvasSolver);
		solvers.put("Landroid/graphics/Rect;-><init>(IIII)V", canvasSolver);
		
		//solvers.put("Ljava/lang/String;->equals(Ljava/lang/Object;)Z", stringSolver);
		//solvers.put("Ljava/lang/String;->split(Ljava/lang/String;)[Ljava/lang/String;", stringSolver);
		//solvers.put("Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V", stringSolver);
		//solvers.put("Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;", stringSolver);
		//solvers.put("Ljava/lang/StringBuilder;->toString()Ljava/lang/String;", stringSolver);
		//solvers.put("Ljava/lang/String;->isEmpty()Z", stringSolver);
		//solvers.put("Ljava/lang/String;->charAt(I)C", stringSolver);
		//solvers.put("Ljava/lang/String;->length()I", stringSolver);
		//solvers.put("Ljava/lang/String;->getBytes()[B", stringSolver);
		
		solvers.put("Ljava/lang/Integer;->parseInt(Ljava/lang/String;)I", wrapperSolver);
		solvers.put("Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;", wrapperSolver);
		solvers.put("Ljava/lang/Integer;->intValue()I", wrapperSolver);
		solvers.put("Ljava/lang/Boolean;->valueOf(Z)Ljava/lang/Boolean;", wrapperSolver);
		solvers.put("Ljava/lang/Boolean;->booleanValue()Z", wrapperSolver);
		solvers.put("Ljava/lang/Long;->valueOf(J)Ljava/lang/Long;", wrapperSolver);
		solvers.put("Ljava/lang/Long;->intValue()I", wrapperSolver);
		solvers.put("Ljava/lang/Math;->ceil(D)D", wrapperSolver);
		solvers.put("Ljava/lang/Math;->floor(D)D", wrapperSolver);
		solvers.put("Ljava/lang/Math;->round(D)J", wrapperSolver);
		solvers.put("Ljava/lang/Math;->cos(D)D", wrapperSolver);

		solvers.put("Ljava/lang/reflect/Array;->newInstance(Ljava/lang/Class;[I)Ljava/lang/Object;", arraySolver);
		solvers.put("Ljava/nio/ByteBuffer;->allocate(I)Ljava/nio/ByteBuffer;", arraySolver);
		solvers.put("Ljava/nio/ByteBuffer;->array()[B", arraySolver);
		solvers.put("Ljava/nio/ByteBuffer;->array()[B", arraySolver);
		solvers.put("Ljava/nio/ByteBuffer;->rewind()Ljava/nio/Buffer;", arraySolver);
		solvers.put("Ljava/nio/ByteBuffer;->wrap([B)Ljava/nio/ByteBuffer;", arraySolver);
		solvers.put("Ljava/nio/ByteBuffer;->asIntBuffer()Ljava/nio/IntBuffer;", arraySolver);
		
		solvers.put("Ljava/nio/IntBuffer;->rewind()Ljava/nio/Buffer;", arraySolver);
		solvers.put("Ljava/nio/IntBuffer;->array()[I", arraySolver);
		solvers.put("Ljava/nio/IntBuffer;->put([I)Ljava/nio/IntBuffer;", arraySolver);
		solvers.put("Ljava/nio/IntBuffer;->get([I)Ljava/nio/IntBuffer;", arraySolver);
		solvers.put("Ljava/lang/System;->arraycopy(Ljava/lang/Object;ILjava/lang/Object;II)V", arraySolver);
		
		solvers.put("Ljava/lang/Thread;-><init>(Ljava/lang/Runnable;)V", threadSolver);
		solvers.put("Ljava/lang/Thread;->start()V", threadSolver);
		solvers.put("Ljava/lang/Thread;->run()V", threadSolver);
	}
	
	public static boolean canSolve(String invokeSig)
	{
		//NOTE: some methods don't have a fix full signature, for example, Activity;->runOnUiThread()...
/*		if (invokeSig.startsWith("Landroid/graphics/Canvas;"))
		{
			P.p("need to handle " + invokeSig);
			P.pause();
		}*/
		return invokeSig.endsWith("->runOnUiThread(Ljava/lang/Runnable;)V") 
				|| invokeSig.startsWith("Landroid/graphics/Canvas;->draw")
				|| solvers.containsKey(invokeSig);
	}
	
	public static void solve(String invokeSig, List<Expression> params, APEXStatement s, MethodContext mc, String[] paramRegs, VM vm)
	{
		if (invokeSig.endsWith("->runOnUiThread(Ljava/lang/Runnable;)V"))
			threadSolver.solve(invokeSig, params, s, mc, paramRegs, vm);
		else if (invokeSig.startsWith("Landroid/graphics/Canvas;->draw"))
			canvasSolver.solve(invokeSig, params, s, mc, paramRegs, vm);
		else
			solvers.get(invokeSig).solve(invokeSig, params, s, mc, paramRegs, vm);
	}
	

	public static void printStringInfo(Expression s, VM vm)
	{
		if (s.isLiteral())
			P.p("Literal string: " + s.toString());
		if (s.isReference())
		{
			APEXObject obj = vm.heap.get(s.getObjID());
			obj.print();
		}
	}
}
