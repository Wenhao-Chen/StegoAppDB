package apex.symbolic.solver.api;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import apex.bytecode_wrappers.APEXStatement;
import apex.symbolic.APEXArray;
import apex.symbolic.APEXObject;
import apex.symbolic.Expression;
import apex.symbolic.MethodContext;
import apex.symbolic.VM;
import apex.symbolic.APEXArray.AputHistory;
import apex.symbolic.APEXObject.BitmapAccess;
import apex.symbolic.solver.Arithmetic;
import util.P;

public class BitmapSolver extends SolverInterface{
	

	@Override
	public void solve(String invokeSig, List<Expression> params, APEXStatement s, MethodContext mc, String[] paramRegs, VM vm)
	{
		if (invokeSig.equals("Landroid/graphics/Bitmap;->getPixel(II)I"))
		{
			APEXObject bitmap = vm.heap.get(mc.read(paramRegs[0]).getObjID());
			Expression x = mc.read(paramRegs[1]);
			Expression y = mc.read(paramRegs[2]);
			
			vm.bitmapAccess.add(new BitmapAccess(s.getUniqueID(), "getPixel", invokeSig, params));
			bitmap.bitmapHistory.add(new BitmapAccess(s.getUniqueID(), "getPixel", x, y, null));
			if (x.isLiteral()&&!x.isSymbolic&&y.isLiteral()&&!y.isSymbolic&&bitmap.concreteBitmap!=null)
			{
				int xx = Arithmetic.parseInt(x.toString());
				int yy = Arithmetic.parseInt(y.toString());
				if (bitmap.concreteBitmap[xx][yy]!=null)
					vm.recentResult = bitmap.concreteBitmap[xx][yy];
				return;
			}
			vm.createSymbolicMethodReturn("I", invokeSig, params, s);
		}
		else if (invokeSig.equals("Landroid/graphics/Bitmap;->getPixels([IIIIIII)V"))
		{
			APEXObject bitmap = vm.heap.get(mc.read(paramRegs[0]).getObjID());
			APEXArray arr = (APEXArray)vm.heap.get(params.get(1).getObjID());
			arr.isFromBitmap = true;
			arr.bitmapReference = bitmap.reference.clone();
			arr.x = params.get(4).clone();
			arr.y = params.get(5).clone();
			arr.width = params.get(6).clone();
			arr.height = params.get(7).clone();
			
			BitmapAccess acc = new BitmapAccess(s.getUniqueID());
			acc.action = "getPixels";
			for (Expression p : params)
				acc.params.add(p.clone());
			bitmap.bitmapHistory.add(acc);
			vm.bitmapAccess.add(new BitmapAccess(s.getUniqueID(), "getPixels", invokeSig, params));
		}
		else if (invokeSig.equals("Landroid/graphics/Bitmap;->setPixel(III)V"))
		{
			APEXObject bitmap = vm.heap.get(mc.read(paramRegs[0]).getObjID());
			Expression x = mc.read(paramRegs[1]);
			Expression y = mc.read(paramRegs[2]);
			Expression c = mc.read(paramRegs[3]);
			bitmap.bitmapHistory.add(new BitmapAccess(s.getUniqueID(), "setPixel", x, y, c));
			vm.bitmapAccess.add(new BitmapAccess(s.getUniqueID(), "setPixel", invokeSig, params));
			if (x.isLiteral()&&!x.isSymbolic && y.isLiteral()&&!y.isSymbolic && c.isLiteral()&&!c.isSymbolic && bitmap.concreteBitmap!=null)
			{
				bitmap.concreteBitmap[Arithmetic.parseInt(x.toString())][Arithmetic.parseInt(y.toString())] = c.clone();
			}
			//P.p(s.getUniqueID()+" "+s.smali);
			//P.p("set pixel "+x.toString()+" "+y.toString()+" to "+c.toString());
			//P.pause();
		}
		else if (invokeSig.equals("Landroid/graphics/Bitmap;->setPixels([IIIIIII)V"))
		{
			// params in order: bitmap, int[], offset, stride, x, y, width, height
			if (!params.get(0).isReference() || !params.get(1).isReference())
			{
				P.p("wrong branch. exiting..");
				vm.shouldStop = true;
				vm.crashed = true;
				return;
			}
			APEXObject bitmap = vm.heap.get(params.get(0).getObjID());
			APEXArray arr = (APEXArray) vm.heap.get(params.get(1).getObjID());
			Expression width = params.get(6);
			

			BitmapAccess a = new BitmapAccess(s.getUniqueID());
			a.action = "setPixels";
			for (Expression p : params)
				a.params.add(p.clone());
			bitmap.bitmapHistory.add(a);
			vm.bitmapAccess.add(new BitmapAccess(s.getUniqueID(), "setPixels", invokeSig, params));
			for (AputHistory aput : arr.aputHistory)
			{
				Expression x = Arithmetic.rem(aput.index, width, "I");
				Expression y = Arithmetic.div(aput.index, width, "I");
				bitmap.bitmapHistory.add(new BitmapAccess(s.getUniqueID(), "setPixel", x, y, aput.val.clone()));
				P.p("[set pixel]");
				P.p("   "+aput.val.toString());
				List<Expression> pp = new ArrayList<>(Arrays.asList(bitmap.reference, x, y, aput.val));
				vm.bitmapAccess.add(new BitmapAccess(s.getUniqueID(), "setPixel", invokeSig, pp));
			}
		}
		else if (invokeSig.equals("Landroid/graphics/Bitmap;->createBitmap([IIIIILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;"))
		{
			if (!params.get(0).isReference())
			{
				vm.shouldStop = true;
				vm.crashed = true;
				return;
			}
			APEXArray arr = (APEXArray) vm.heap.get(params.get(0).getObjID());
			APEXObject bitmap = vm.createNewObject("Landroid/graphics/Bitmap;", "createBitmap([IIIIIConfig)", s.getUniqueID(), false);
			Expression width = params.get(3);
			BitmapAccess a = new BitmapAccess(s.getUniqueID());
			a.action = "setPixels";
			for (Expression p : params)
				a.params.add(p.clone());
			bitmap.bitmapHistory.add(a);
			vm.bitmapAccess.add(new BitmapAccess(s.getUniqueID(), "setPixels", invokeSig, params));
			for (AputHistory aput : arr.aputHistory)
			{
				Expression x = Arithmetic.rem(aput.index, width, "I");
				Expression y = Arithmetic.div(aput.index, width, "I");
				bitmap.bitmapHistory.add(new BitmapAccess(s.getUniqueID(), "setPixel", x, y, aput.val.clone()));
				List<Expression> pp = new ArrayList<>(Arrays.asList(bitmap.reference, x, y, aput.val));
				vm.bitmapAccess.add(new BitmapAccess(s.getUniqueID(), "setPixel", invokeSig, pp));
			}
			vm.recentResult = bitmap.reference;
		}
		else if (invokeSig.equals("Landroid/graphics/Bitmap;->createBitmap([IIILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;"))
		{
			if (!params.get(0).isReference())
			{
				vm.shouldStop = true;
				vm.crashed = true;
				return;
			}
			APEXArray arr = (APEXArray) vm.heap.get(params.get(0).getObjID());
			APEXObject bitmap = vm.createNewObject("Landroid/graphics/Bitmap;", "createBitmap([IIIIIConfig)", s.getUniqueID(), false);
			Expression width = params.get(1);
			BitmapAccess a = new BitmapAccess(s.getUniqueID());
			a.action = "setPixels";
			for (Expression p : params)
				a.params.add(p.clone());
			bitmap.bitmapHistory.add(a);
			vm.bitmapAccess.add(new BitmapAccess(s.getUniqueID(), "setPixels", invokeSig, params));
			for (AputHistory aput : arr.aputHistory)
			{
				Expression x = Arithmetic.rem(aput.index, width, "I");
				Expression y = Arithmetic.div(aput.index, width, "I");
				bitmap.bitmapHistory.add(new BitmapAccess(s.getUniqueID(), "setPixel", x, y, aput.val.clone()));
				List<Expression> pp = new ArrayList<>(Arrays.asList(bitmap.reference, x, y, aput.val));
				vm.bitmapAccess.add(new BitmapAccess(s.getUniqueID(), "setPixel", invokeSig, pp));
			}
			vm.recentResult = bitmap.reference;
		}
		else if (invokeSig.equals("Landroid/graphics/Bitmap;->createBitmap(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;"))
		{
			Expression w = params.get(0);
			Expression h = params.get(1);
			APEXObject bitmap = vm.createNewObject("Landroid/graphics/Bitmap;", "createBitmap(IIConfig)", s.getUniqueID(), false);
			bitmap.bitmapWidth = w.clone();
			bitmap.bitmapHeight = h.clone();
			if (w.isLiteral()&&!w.isSymbolic && h.isLiteral()&&!h.isSymbolic)
			{
				int width = Arithmetic.parseInt(w.toString());
				int height = Arithmetic.parseInt(h.toString());
				if (width<=0 || height <= 0)
				{
					vm.crashed = true;
					vm.shouldStop = true;
					return;
				}
				bitmap.concreteBitmap = new Expression[height][width];
			}
			vm.recentResult = bitmap.reference;
			
			BitmapAccess a = new BitmapAccess(s.getUniqueID());
			a.action = "createBitmap (empty)";
			for (Expression p : params)
				a.params.add(p.clone());
			bitmap.bitmapHistory.add(a);
			vm.bitmapAccess.add(new BitmapAccess(s.getUniqueID(), "createBitmap", invokeSig, params));
		}
		else if (invokeSig.equals("Landroid/graphics/Bitmap;->createBitmap(Landroid/graphics/Bitmap;)Landroid/graphics/Bitmap;"))
		{
			String srcID = params.get(0).getObjID();
			if (srcID!=null)
			{
				APEXObject bitmap = vm.heap.get(srcID);
				APEXObject res = vm.createNewObject("Landroid/graphics/Bitmap;", "createBitmap(Bitmap)", s.getUniqueID(), bitmap.isSymbolic);
				if (bitmap.bitmapWidth!=null)
					res.bitmapWidth=bitmap.bitmapWidth.clone();
				if (bitmap.bitmapHeight!=null)
					res.bitmapHeight=bitmap.bitmapHeight.clone();
				vm.recentResult = res.reference;
				
				BitmapAccess a = new BitmapAccess(s.getUniqueID());
				a.action = "created_from_bitmap";
				a.copied_from = bitmap.reference;
				res.bitmapHistory.add(a);
				vm.bitmapAccess.add(new BitmapAccess(s.getUniqueID(), "createBitmap", invokeSig, params));
			}
		}
		else if (invokeSig.equals("Landroid/graphics/Bitmap;->createBitmap(Landroid/graphics/Bitmap;IIII)Landroid/graphics/Bitmap;"))
		{
			String srcID = params.get(0).getObjID();
			if (srcID!=null)
			{
				APEXObject bitmap = vm.heap.get(srcID);
				APEXObject res = vm.createNewObject("Landroid/graphics/Bitmap;", "createBitmap(BitmapIIII)", s.getUniqueID(), false);
				res.width = params.get(3).clone();
				res.height = params.get(4).clone();
				vm.recentResult = res.reference;
				
				BitmapAccess a = new BitmapAccess(s.getUniqueID());
				a.action = "created_from_bitmap";
				a.copied_from = bitmap.reference;
				res.bitmapHistory.add(a);
				vm.bitmapAccess.add(new BitmapAccess(s.getUniqueID(), "createBitmap", invokeSig, params));
			}
		}
		else if (invokeSig.equals("Landroid/graphics/Bitmap;->createBitmap(Landroid/graphics/Bitmap;IIIILandroid/graphics/Matrix;Z)Landroid/graphics/Bitmap;"))
		{
			String srcID = params.get(0).getObjID();
			if (srcID!=null)
			{
				APEXObject bitmap = vm.heap.get(srcID);
				APEXObject res = vm.createNewObject("Landroid/graphics/Bitmap;", "createBitmap(BitmapIIIIMatrixZ)", s.getUniqueID(), false);
				res.width = params.get(3).clone();
				res.height = params.get(4).clone();
				vm.recentResult = res.reference;
				
				BitmapAccess a = new BitmapAccess(s.getUniqueID());
				a.action = "created_from_bitmap";
				a.copied_from = bitmap.reference;
				res.bitmapHistory.add(a);
				vm.bitmapAccess.add(new BitmapAccess(s.getUniqueID(), "createBitmap", invokeSig, params));
				
				//APEXObject matrix = vm.heap.get(params.get(5).getObjID());
/*				if (matrix.matrix_scaleX==null || matrix.matrix_scaleY==null)
				{
					P.p("having a scaling matrix with unknown parameter: "+ s.getUniqueID());
					P.pause();
				}
				
				////////// print out all parameters
				P.p("\n\n------------------------------------");
				P.p("-------------------------------------");
				P.p("------------------- Resizing(using scaling matrix) ------------------");
				P.p("*** Source bitmap: "+bitmap.objID);
				P.p("*** X0 = "+params.get(1).toString());
				P.p("*** Y0 = "+params.get(2).toString());
				P.p("*** width = "+res.width.toString());
				P.p("*** height = " + res.height.toString());
				P.p("*** scaling matrix: " + matrix.objID);
				P.p("*** scaling matrix scale_X = " + matrix.matrix_scaleX.toString());
				P.p("*** scaling matrix scale_Y = " + matrix.matrix_scaleY.toString());
				P.p("---------------------------------------------------------------------\n\n\n");
				P.pause();*/
			}
			else
			{
				P.p("creating scaled bitmap from a non-existing bitmap???");
				P.pause();
			}
		}
		else if (invokeSig.equals("Landroid/graphics/Bitmap;->createScaledBitmap(Landroid/graphics/Bitmap;IIZ)Landroid/graphics/Bitmap;"))
		{
			String srcID = params.get(0).getObjID();
			if (srcID!=null)
			{
				APEXObject bitmap = vm.heap.get(srcID);
				APEXObject res = vm.createNewObject("Landroid/graphics/Bitmap;", "createScaledBitmap(BitmapIIZ)", s.getUniqueID(), false);
				res.width = params.get(1).clone();
				res.height = params.get(2).clone();
				vm.recentResult = res.reference;
				
				BitmapAccess a = new BitmapAccess(s.getUniqueID());
				a.action = "created_from_bitmap";
				a.copied_from = bitmap.reference;
				res.bitmapHistory.add(a);
				vm.bitmapAccess.add(new BitmapAccess(s.getUniqueID(), "createBitmap", invokeSig, params));
				
/*				P.p("\n\n--------- create scaled bitmap");
				P.p("*** source bitmap: " + bitmap.objID);
				P.p("*** new width: "+res.width.toString());
				P.p("*** new height: "+res.height.toString());
				P.pause();*/
			}
			else
			{
				P.p("creating scaledbitmap from a null??");
				P.pause();
			}
		}
		else if (invokeSig.equals("Landroid/graphics/Bitmap;->getWidth()I"))
		{
			if (params.get(0).getObjID()==null)
			{
				vm.shouldStop = true;
				vm.crashed = true;
				return;
			}
			APEXObject bitmap = vm.heap.get(params.get(0).getObjID());
			if (bitmap.bitmapWidth != null)
				vm.recentResult = bitmap.bitmapWidth.clone();
			else
			{
				vm.createSymbolicMethodReturn("I", invokeSig, params, s);
			}
		}
		else if (invokeSig.equals("Landroid/graphics/Bitmap;->getHeight()I"))
		{
			if (params.get(0).getObjID()==null)
			{
				vm.shouldStop = true;
				vm.crashed = true;
				return;
			}
			APEXObject bitmap = vm.heap.get(params.get(0).getObjID());
			if (bitmap.bitmapHeight != null)
				vm.recentResult = bitmap.bitmapHeight.clone();
			else
				vm.createSymbolicMethodReturn("I", invokeSig, params, s);
		}
		else if (invokeSig.equals("Landroid/graphics/Bitmap;->copyPixelsToBuffer(Ljava/nio/Buffer;)V"))
		{
			APEXObject buffer = vm.heap.get(params.get(1).getObjID());
			buffer.isFromBitmap = true;
			buffer.bitmapReference = params.get(0).clone();
			
			APEXObject bitmap = vm.heap.get(buffer.bitmapReference.getObjID());
			BitmapAccess a = new BitmapAccess(s.getUniqueID());
			a.action = "getPixelsToBuffer";
			for (Expression p : params)
				a.params.add(p.clone());
			bitmap.bitmapHistory.add(a);
			vm.bitmapAccess.add(new BitmapAccess(s.getUniqueID(), "getPixelsToBuffer", invokeSig, params));
		}
		else if (invokeSig.contentEquals("Ljava/nio/IntBuffer;->array()[I"))
		{
			APEXObject buffer = vm.heap.get(params.get(0).getObjID());
			vm.createSymbolicMethodReturn("[I", invokeSig, params, s);
			if (buffer.isFromBitmap)
			{
				APEXArray arr = (APEXArray) vm.heap.get(vm.recentResult.getObjID());
				buffer.arrayReference = arr.reference;
				arr.isFromBitmap = true;
				arr.bitmapReference = buffer.bitmapReference.clone();
			}
		}
		else if (invokeSig.contentEquals("Ljava/nio/ByteBuffer;->array()[B"))
		{
			APEXObject buffer = vm.heap.get(params.get(0).getObjID());
			vm.createSymbolicMethodReturn("[I", invokeSig, params, s);
			if (buffer.isFromBitmap)
			{
				APEXArray arr = (APEXArray) vm.heap.get(vm.recentResult.getObjID());
				buffer.arrayReference = arr.reference;
				arr.isFromBitmap = true;
				arr.bitmapReference = buffer.bitmapReference.clone();
			}
		}
		else if (invokeSig.contentEquals("Ljava/nio/IntBuffer;->rewind()Ljava/nio/Buffer;") || invokeSig.contentEquals("Ljava/nio/ByteBuffer;->rewind()Ljava/nio/Buffer;"))
		{
			vm.recentResult = params.get(0).clone();
		}
		else if (invokeSig.equals("Landroid/graphics/Bitmap;->copyPixelsFromBuffer(Ljava/nio/Buffer;)V"))
		{
			APEXObject bitmap = vm.heap.get(params.get(0).getObjID());
			APEXObject buffer = vm.heap.get(params.get(1).getObjID());
			
			BitmapAccess a = new BitmapAccess(s.getUniqueID());
			a.action = "setPixelsFromBuffer";
			for (Expression p : params)
				a.params.add(p.clone());
			bitmap.bitmapHistory.add(a);
			vm.bitmapAccess.add(new BitmapAccess(s.getUniqueID(), "setPixelsToBuffer", invokeSig, params));
			if (buffer.arrayReference!=null)
			{
				APEXArray arr = (APEXArray) vm.heap.get(buffer.arrayReference.getObjID());
				//P.p("copying from buffer. array ref: "+arr.reference.toString());
				if (bitmap.width==null)
				{
					bitmap.width = arr.type.equals("[B")?
							Arithmetic.div(arr.length, Expression.newLiteral("I", "4"), "I")
							:arr.length.clone();
				}
				//P.p("aput size: " + arr.aputHistory.size());
				for (AputHistory aput : arr.aputHistory)
				{
					//P.p("  aput " + aput.index.toString()+" "+aput.val);
					if (arr.type.equals("[B")) // each byte element represents one (RGBA) channel of a pixel
					{
						Expression pixelIndex = Arithmetic.div(aput.index, Expression.newLiteral("I", "4"), "I");
						Expression x = Arithmetic.rem(pixelIndex, bitmap.width, "I");
						Expression y = Arithmetic.div(pixelIndex, bitmap.width, "I");
						bitmap.bitmapHistory.add(new BitmapAccess(s.getUniqueID(), "setPixel", x, y, aput.val.clone()));
						vm.bitmapAccess.add(new BitmapAccess(s.getUniqueID(), "setPixel", invokeSig, Arrays.asList(bitmap.reference, x, y, aput.val)));
					}
					else // each int element represents all 4 channels of a pixels
					{
						Expression x = Arithmetic.rem(aput.index, bitmap.width, "I");
						Expression y = Arithmetic.div(aput.index, bitmap.width, "I");
						bitmap.bitmapHistory.add(new BitmapAccess(s.getUniqueID(), "setPixel", x, y, aput.val.clone()));
						//P.p("  new bitmap history: setPixel "+x+" "+y+aput.val);
						vm.bitmapAccess.add(new BitmapAccess(s.getUniqueID(), "setPixel", invokeSig, Arrays.asList(bitmap.reference, x, y, aput.val)));
					}
					
				}
			}
		}
		else if (invokeSig.equals("Landroid/graphics/Bitmap;->compress(Landroid/graphics/Bitmap$CompressFormat;ILjava/io/OutputStream;)Z"))
		{
			String id = params.get(0).getObjID();
			if (id != null)
			{
				APEXObject bitmap = vm.heap.get(id);
				BitmapAccess a = new BitmapAccess(s.getUniqueID());
				a.action = "compress";
				for (Expression p : params)
					a.params.add(p.clone());
				bitmap.bitmapHistory.add(a);
				vm.bitmapAccess.add(new BitmapAccess(s.getUniqueID(), "compress", invokeSig, params));
			}
		}
		else if (invokeSig.equals("Landroid/graphics/Bitmap;->copy(Landroid/graphics/Bitmap$Config;Z)Landroid/graphics/Bitmap;"))
		{
			String id = params.get(0).getObjID();
			if (id != null)
			{
				APEXObject bitmap = vm.heap.get(id);
				APEXObject res = vm.createNewObject("Landroid/graphics/Bitmap;", "Bitmap.copy(ConfigZ)", s.getUniqueID(), bitmap.isSymbolic);
				if (bitmap.bitmapWidth!=null)
					res.bitmapWidth=bitmap.bitmapWidth.clone();
				if (bitmap.bitmapHeight!=null)
					res.bitmapHeight=bitmap.bitmapHeight.clone();
				vm.recentResult = res.reference;
				
				BitmapAccess a = new BitmapAccess(s.getUniqueID());
				a.action = "getPixelsFromBitmap";
				a.copied_from = bitmap.reference;
				res.bitmapHistory.add(a);
				vm.bitmapAccess.add(new BitmapAccess(s.getUniqueID(), "createBitmap", invokeSig, params));

			}
		}
		else if (invokeSig.equals("Landroid/graphics/Bitmap;->recycle()V"))
		{
			String id = params.get(0).getObjID();
			if (id != null)
			{
				APEXObject bitmap = vm.heap.get(id);
				
				BitmapAccess a = new BitmapAccess(s.getUniqueID());
				a.action = "recycled";
				bitmap.bitmapHistory.add(a);
				vm.bitmapAccess.add(new BitmapAccess(s.getUniqueID(), "recycle", invokeSig, params));
			}
		}
		else if (invokeSig.equals("Landroid/graphics/Color;->argb(IIII)I"))
		{
			Expression a = params.get(0), r = params.get(1), g = params.get(2), b = params.get(3);
			if (a.isLiteral()&&!a.isSymbolic && r.isLiteral()&&!r.isSymbolic && g.isLiteral()&&!g.isSymbolic && b.isLiteral()&&!b.isSymbolic)
			{
				int A = Arithmetic.parseInt(a.toString());
				int R = Arithmetic.parseInt(r.toString());
				int G = Arithmetic.parseInt(g.toString());
				int B = Arithmetic.parseInt(b.toString());
				int color = (A & 0xff) << 24 | (R & 0xff) << 16 | (G & 0xff) << 8 | (B & 0xff);
				vm.recentResult = Expression.newLiteral("I", color+"");
			}
			else
			{
				vm.createSymbolicMethodReturn("I", invokeSig, params, s);
			}
		}
		else if (invokeSig.equals("Landroid/graphics/Color;->red(I)I"))
		{
			Expression color = params.get(0);
			if (color.isLiteral()&&!color.isSymbolic)
			{
				Color c = new Color(Arithmetic.parseInt(color.toString()));
				vm.recentResult = Expression.newLiteral("I", c.getRed()+"");
			}
			else
				vm.createSymbolicMethodReturn("I", invokeSig, params, s);
		}
		else if (invokeSig.equals("Landroid/graphics/Color;->green(I)I"))
		{
			Expression color = params.get(0);
			if (color.isLiteral()&&!color.isSymbolic)
			{
				Color c = new Color(Arithmetic.parseInt(color.toString()));
				vm.recentResult = Expression.newLiteral("I", c.getGreen()+"");
			}
			else
				vm.createSymbolicMethodReturn("I", invokeSig, params, s);
		}
		else if (invokeSig.equals("Landroid/graphics/Color;->blue(I)I"))
		{
			Expression color = params.get(0);
			if (color.isLiteral()&&!color.isSymbolic)
			{
				Color c = new Color(Arithmetic.parseInt(color.toString()));
				vm.recentResult = Expression.newLiteral("I", c.getBlue()+"");
			}
			else
				vm.createSymbolicMethodReturn("I", invokeSig, params, s);
		}
		else if (invokeSig.contentEquals("Landroid/graphics/Color;->colorToHSV(I[F)V"))
		{
			// this function turns an ARGB color value (integer) into a 3-element float array which holds the HSV values of this color
			if (params.get(1).getObjID()==null)
			{
				vm.shouldStop = true;
				vm.crashed = true;
				return;
			}
			APEXArray arr = (APEXArray) vm.heap.get(params.get(1).getObjID());
			List<Expression> plist = new ArrayList<>();
			plist.add(params.get(0).clone());
			vm.createSymbolicMethodReturn("F", "Landroid/graphics/Color;->colorToH(I)F", plist, s);
			arr.aput(s, 0, vm.recentResult, vm);
			P.p("[H]");
			P.p(vm.recentResult.toString());
			vm.createSymbolicMethodReturn("F", "Landroid/graphics/Color;->colorToS(I)F", plist, s);
			arr.aput(s, 1, vm.recentResult, vm);
			P.p("[S]");
			P.p(vm.recentResult.toString());
			vm.createSymbolicMethodReturn("F", "Landroid/graphics/Color;->colorToV(I)F", plist, s);
			arr.aput(s, 2, vm.recentResult, vm);
			P.p("[V]");
			P.p(vm.recentResult.toString());
			P.pause();
		}
		else if (invokeSig.contentEquals("Landroid/graphics/Color;->HSVToColor([F)I"))
		{
			if (params.get(0).getObjID()==null)
			{
				vm.shouldStop = true;
				vm.crashed = true;
				return;
			}
			APEXArray arr = (APEXArray) vm.heap.get(params.get(0).getObjID());
			Expression H = arr.aget(Expression.newLiteral("I", "0"), vm, s);
			Expression S = arr.aget(Expression.newLiteral("I", "1"), vm, s);
			Expression V = arr.aget(Expression.newLiteral("I", "2"), vm, s);
			params.remove(0);
			params.add(H);
			params.add(S);
			params.add(V);
			vm.createSymbolicMethodReturn("I", "Landroid/graphics/Color;->HSVToColor(FFF)I", params, s);
			P.p("[Color]");
			P.p(vm.recentResult.toString());
			P.pause();
		}
		else if (invokeSig.equals("Landroid/graphics/Matrix;->postScale(FF)Z"))
		{
			APEXObject matrix = vm.heap.get(params.get(0).getObjID());
			matrix.matrix_scaleX = params.get(1).clone();
			matrix.matrix_scaleY = params.get(2).clone();
		}
		else
		{
			P.p("Forgot about this API?? " + invokeSig);
			P.pause();
		}
	}

	public static void initConcreteBitmap(APEXObject obj)
	{
		int width = 30, height = 20;
		obj.bitmapWidth = Expression.newLiteral("I", ""+width);
		obj.bitmapHeight = Expression.newLiteral("I", ""+height);
		obj.concreteBitmap = new Expression[height][width];
		/*		for (int i = 0; i < height; i++)
		{
			for (int j = 0; j < width; j++)
			{
				obj.concreteBitmap[i][j] = RandomColor();
			}
		}*/
	}
	
	public static int RandomColor()
	{
		Random rng = new Random();
		Color c = new Color(rng.nextInt(256), rng.nextInt(256), rng.nextInt(256));
		return c.getRGB();
	}

}
