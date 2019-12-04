package apex.symbolic;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import util.P;

@SuppressWarnings("unused")
public class APEXObject{

	public String objID;
	public String root;
	public String type;
	public String birth;
	public Expression reference;
	public Map<String, Expression> fields;
	public boolean isSymbolic;
	
	///////////////// bitmap properties
	public Expression bitmapWidth, bitmapHeight;
	public Expression[][] concreteBitmap;
	public List<BitmapAccess> bitmapHistory = new ArrayList<>();
	public static class BitmapAccess {
		public String action;
		public Expression copied_from;
		public Expression canvas_draw_bitmap, top, left, bottom, right;
		public Expression canvas_draw_text;
		public String sig;
		public List<Expression> params = new ArrayList<>();
		public Expression x, y, c;
		public BitmapAccess(String action, String sig, List<Expression> p) {
			this.action = action;
			this.sig = sig;
			for (Expression pp : p)
				params.add(pp.clone());
		}
		public BitmapAccess() {}
		public BitmapAccess(String action, Expression x, Expression y, Expression c)
		{
			this.action = action;
			this.x = x.clone();
			this.y = y.clone();
			params.add(x);
			params.add(y);
			if (c != null)
			{
				this.c = c.clone();
				params.add(c);
			}
		}
	}
	////////////////////////////////////
	/////////////// canvas and rect properties
	public Expression canvas_bitmapRef;
	public Expression rect_left, rect_top, rect_right, rect_bottom;
	///////////////////////////////////////
	
	///////////////// Matrix.postScale(float x, float y)///
	public Expression matrix_scaleX, matrix_scaleY;
	
	///////////////// StringBuilder operations
	public List<Expression> stringBuilderAppendHistory = new ArrayList<>();
	
	////////////////////////////////////
	//////////////// Primitive Wrapper properties
	public Expression primitiveExpr;
	
	////////////////////////////////////
	//////////////// Thread properties //
	public Expression RunnableRef;
	////////////////////////////////////////
	
	/////////////// Int[], ByteBuffer, IntBuffer that are connected with Bitmaps ///////
	public boolean isFromBitmap;
	public Expression bitmapReference;
	public Expression x, y, width, height; // these are the params in Bitmap.getPixels() and setPixels()
	public Expression bufferLength;        // this is from Buffer.allocate(I)
	public Expression arrayReference;		// this is from Buffer.wrap([x)
	///////////////////////////////////////////////////////////////////////////////////

	private APEXObject() {}
	
	public APEXObject(String objID, String type, String root, String birth)
	{
		this.objID = objID;
		this.type = type;
		this.root = root;
		this.birth = birth;
		this.fields = new TreeMap<>();
		reference = Expression.newReference(objID, type);
		isSymbolic = false;
		isFromBitmap = false;
	}
	
	public APEXObject clone()
	{
		if (this instanceof APEXArray)
			return ((APEXArray)this).clone();
		
		APEXObject obj = new APEXObject();
		obj.objID = objID;
		obj.root = root;
		obj.type = type;
		obj.birth = birth;
		obj.reference = reference.clone();
		obj.fields = new TreeMap<>();
		for (String f : fields.keySet())
			obj.fields.put(f, fields.get(f).clone());
		obj.isSymbolic = isSymbolic;
		if (this.concreteBitmap!=null)
		{
			obj.concreteBitmap = new Expression[concreteBitmap.length][concreteBitmap[0].length];
			for (int i = 0; i < concreteBitmap.length; i++)
			{
				for (int j = 0; j < concreteBitmap[0].length; j++)
					obj.concreteBitmap[i][j] = this.concreteBitmap[i][j];
			}
		}
		
		return obj;
	}
	
	public void print()
	{
		print(System.out);
	}
	
	public Set<String> print(File f)
	{
		Set<String> res = new HashSet<>();
		P.p(" --- "+this.objID+": "+this.root+" "+this.type, f);
		P.p("    *** birth: "+this.birth, f);
		for (Map.Entry<String, Expression> fieldEntry : this.fields.entrySet())
		{
			P.p("\t"+fieldEntry.getKey()+"\t=\t"+fieldEntry.getValue().toString(), f);
		}
		if (this.bitmapWidth!=null)
		{
			P.p("\twidth = "+bitmapWidth.toString(), f);
		}
		if (this.bitmapHeight!=null)
		{
			P.p("\theight = " + bitmapHeight.toString(), f);
		}
		if (this.rect_left!=null)	P.p("\trect_left = " + rect_left.toString(), f);
		if (this.rect_top!=null)	P.p("\trect_top = " +  rect_top.toString(), f);
		if (this.rect_right!=null)	P.p("\trect_right = " + rect_right.toString(), f);
		if (this.rect_bottom!=null)	P.p("\trect_bottom = " + rect_bottom.toString(), f);
		if (!this.bitmapHistory.isEmpty())
		{
			P.p("    *** Bitmap Access History -----", f);
			for (int i = 0; i < bitmapHistory.size(); i++)
			{
				BitmapAccess access = bitmapHistory.get(i);
				P.p("\t\t"+access.action, f);
				for (Expression p : access.params)
				{
					P.p("\t\t  "+p.toString(), f);
					if (p.isReference())
						res.add(p.getObjID());
				}
			}
		}
		return res;
	}
	
	public void print(PrintStream out)
	{
		out.println(" --- "+this.objID+": "+this.root+" "+this.type);
		out.println("    *** birth: "+this.birth);
		for (Map.Entry<String, Expression> fieldEntry : this.fields.entrySet())
		{
			out.println("\t"+fieldEntry.getKey()+"\t=\t"+fieldEntry.getValue().toString());
		}
		if (!this.bitmapHistory.isEmpty())
		{
			out.println("    *** Bitmap Access History -----");
			for (int i = 0; i < bitmapHistory.size(); i++)
			{
				BitmapAccess access = bitmapHistory.get(i);
				out.print("\t  "+access.action+"(");
				for (Expression p : access.params)
				{
					out.print("  "+p.toString());
				}
				out.println(")");
			}
		}
	}
}
