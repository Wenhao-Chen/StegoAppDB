package apex.symbolic.solver.api;

import java.util.List;

import apex.bytecode_wrappers.APEXStatement;
import apex.symbolic.APEXObject;
import apex.symbolic.Expression;
import apex.symbolic.MethodContext;
import apex.symbolic.VM;
import apex.symbolic.APEXObject.BitmapAccess;
import apex.symbolic.solver.Arithmetic;
import util.P;

public class CanvasSolver extends SolverInterface{

	@Override
	public void solve(String invokeSig, List<Expression> params, APEXStatement s, MethodContext mc, String[] paramRegs, VM vm)
	{
		if (invokeSig.equals("Landroid/graphics/Canvas;-><init>(Landroid/graphics/Bitmap;)V")||
				invokeSig.equals("Landroid/graphics/Canvas;->setBitmap(Landroid/graphics/Bitmap;)V"))
		{

			String id = params.get(0).getObjID();
			String bmpID = params.get(1).getObjID();
			if (id != null && bmpID != null)
			{
				APEXObject canvas = vm.heap.get(id);
				APEXObject bmp = vm.heap.get(bmpID);
				canvas.canvas_bitmapRef = bmp.reference;
			}
		}
		else if (invokeSig.startsWith("Landroid/graphics/Canvas;->drawBitmap(Landroid/graphics/Bitmap;"))
		{
			String id = params.get(0).getObjID();
			String bmpID = params.get(1).getObjID();
			if (id != null && bmpID != null)
			{
				APEXObject canvas = vm.heap.get(id);
				APEXObject bmp = vm.heap.get(bmpID);
				if (canvas.canvas_bitmapRef!=null)
				{
					APEXObject dst = vm.heap.get(canvas.canvas_bitmapRef.getObjID());
					BitmapAccess access = new BitmapAccess(s.getUniqueID());
					access.action = "set_canvas_drawBitmap";
					access.canvas_draw_bitmap = bmp.reference;
					for (Expression p : params)
					{
						access.params.add(p.clone());
					}
/*					Expression top = Expression.newLiteral("Ljava/lang/String;", "unknown");
					Expression left = Expression.newLiteral("Ljava/lang/String;", "unknown");
					Expression right = Expression.newLiteral("Ljava/lang/String;", "unknown");
					Expression bottom = Expression.newLiteral("Ljava/lang/String;", "unknown");
					
					if (invokeSig.equals("Landroid/graphics/Canvas;->drawBitmap(Landroid/graphics/Bitmap;FFLandroid/graphics/Paint;)V"))
					{
						left = params.get(2);
						top = params.get(3);
						Expression w = bmp.width;
						if (w==null)
						{
							w = new Expression("return");
							w.add("Landroid/graphics/Bitmap;->getWidth()I");
							w.add(bmp.reference.clone());
						}
						Expression h = bmp.height;
						if (h==null)
						{
							h = new Expression("return");
							h.add("Landroid/graphics/Bitmap;->getHeight()I");
							h.add(bmp.reference.clone());
						}
						right = Arithmetic.add(left, w, left.type);
						bottom = Arithmetic.add(top, h, top.type);
					}
					else if (invokeSig.startsWith("Landroid/graphics/Canvas;->drawBitmap(Landroid/graphics/Bitmap;Landroid/graphics/Rect;"))
					{
						APEXObject dstRect = vm.heap.get(params.get(3).getObjID());
						if (dstRect.rect_left!=null)
							left = dstRect.rect_left;
						if (dstRect.rect_right!=null)
							right = dstRect.rect_right;
						if (dstRect.rect_top!=null)
							top = dstRect.rect_top;
						if (dstRect.rect_bottom!=null)
							bottom = dstRect.rect_bottom;
					}
					
					access.top = top;
					access.left = left;
					access.right = right;
					access.bottom = bottom;*/
					dst.bitmapHistory.add(access);
					vm.bitmapAccess.add(new BitmapAccess(s.getUniqueID(), "canvas_drawBitmap", invokeSig, params));
				}
			}
		}
		else if (invokeSig.startsWith("Landroid/graphics/Canvas;->drawText"))
		{
			String id = params.get(0).getObjID();
			if (id != null)
			{
				APEXObject canvas = vm.heap.get(id);
				if (canvas.bitmapReference!=null)
				{
					APEXObject dst = vm.heap.get(canvas.canvas_bitmapRef.getObjID());
					BitmapAccess a = new BitmapAccess(s.getUniqueID());
					a.action = "set_canvas_drawText";
					for (Expression p : params)
					{
						a.params.add(p.clone());
					}
/*					a.canvas_draw_text = params.get(1).clone();
					Expression[] origin = new Expression[2];
					int index = 0;
					for (int i = 2; i < params.size(); i++)
					{
						if (params.get(i).type.equals("F"))
						{
							origin[index++] = params.get(i).clone();
						}
					}
					if (index!=2)
					{
						P.p("Can't find the origin x,y for Canvas.drawText");
						P.pause();
					}
					
					a.left = origin[0];
					a.top = origin[1];*/
					
					dst.bitmapHistory.add(a);
					vm.bitmapAccess.add(new BitmapAccess(s.getUniqueID(), "canvas_drawText", invokeSig, params));

				}
			}
		}
		else if (invokeSig.startsWith("Landroid/graphics/Canvas;->draw"))
		{
			String id = params.get(0).getObjID();
			if (id != null)
			{
				APEXObject canvas = vm.heap.get(id);
				if (canvas.canvas_bitmapRef!=null)
				{
					APEXObject dst = vm.heap.get(canvas.canvas_bitmapRef.getObjID());
					BitmapAccess a = new BitmapAccess(s.getUniqueID());
					a.action = "set_canvas_"+invokeSig.substring(invokeSig.indexOf("draw"), invokeSig.indexOf("("));
					for (Expression param : params)
						a.params.add(param.clone());
					dst.bitmapHistory.add(a);
					vm.bitmapAccess.add(new BitmapAccess(s.getUniqueID(), a.action, invokeSig, params));

				}
			}
		}
		else if (invokeSig.equals("Landroid/graphics/Rect;-><init>(IIII)V"))
		{
			String id = params.get(0).getObjID();
			if (id != null)
			{
				APEXObject rect = vm.heap.get(id);
				rect.rect_left = params.get(1).clone();
				rect.rect_top = params.get(2).clone();
				rect.rect_right = params.get(3).clone();
				rect.rect_bottom = params.get(4).clone();
			}
		}
		else
		{
			P.p("forgot about this API? "+invokeSig);
			P.pause();
		}
		
	}

	
	
}
