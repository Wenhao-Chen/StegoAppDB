package apex.symbolic;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import apex.bytecode_wrappers.APEXStatement;
import apex.symbolic.solver.Arithmetic;
import app_analysis.common.Dirs;
import util.Dalvik;
import util.P;

public class APEXArray extends APEXObject{

	
	public Expression length;
	
	public List<AputHistory> aputHistory;
	Map<String, Expression> aputMap = new HashMap<>();
	public Map<Integer, Expression> elements;
	public boolean hasFixedLength;
	public boolean isCharArrayOfString;
	public Expression strExp;
	private APEXArray parentArray;
	private Expression indexInParent;
	
	public static class AputHistory {
		public Expression index, val;
		public boolean solved;
		AputHistory(Expression i, Expression v, boolean s)
		{index = i; val = v; solved = s;}
	}
	
	public APEXArray clone()
	{
		APEXArray objA = new APEXArray(objID, type, root, birth, length);
		
		objA.reference = reference.clone();
		objA.fields = new TreeMap<>();
		for (String f : fields.keySet())
			objA.fields.put(f, fields.get(f).clone());
		objA.isSymbolic = isSymbolic;
		
		objA.length = length.clone();
		objA.aputHistory = new ArrayList<>();
		for (AputHistory h : aputHistory)
			objA.aputHistory.add(new AputHistory(h.index.clone(), h.val.clone(), h.solved));
		objA.elements = new HashMap<>();
		for (int i : this.elements.keySet())
			objA.elements.put(i, elements.get(i).clone());
		objA.hasFixedLength = hasFixedLength;
		objA.isFromBitmap = isFromBitmap;
		objA.isCharArrayOfString = isCharArrayOfString;
		if (strExp!=null)
		objA.strExp = strExp.clone();
		if (bitmapReference!=null)	objA.bitmapReference = bitmapReference.clone();
		if (x!=null)				objA.x = x.clone();
		if (y!=null)				objA.y = y.clone();
		if (width!=null)			objA.width = width.clone();
		if (height!=null)			objA.height = height.clone();
		objA.parentArray = parentArray;
		for (String key : aputMap.keySet())
			objA.aputMap.put(key, aputMap.get(key));
		objA.indexInParent = indexInParent!=null?indexInParent.clone():null;
		return objA;
	}
	
	
	public APEXArray(String objID, String type, String root, String birth, Expression length)
	{
		super(objID, type, root, birth);
		if (length != null)
			this.length = length.clone();
		else
		{
			this.length = Expression.newLiteral("I", objID+".array_length");
			this.length.isSymbolic = true;
		}
		aputHistory = new ArrayList<>();
		elements = new HashMap<>();
		hasFixedLength = (this.length.isLiteral() && !this.length.isSymbolic);
	}

	public void aput(APEXStatement s, int index, Expression val, VM vm)
	{
		if (elements.get(index)!=null) {
			Expression oldVal = elements.get(index);
			if (oldVal.bitIndexFromRight>-1 
					&& oldVal.pixelExp != null
					&& !val.related_to_pixel) {
//				P.p("replacing pixel bit???");
//				P.p(oldVal.pixelExp.toString());
//				P.p(""+oldVal.bitIndexFromRight);
//				P.p(val.toString());
				// String stmtID, String action, Expression x, Expression y, Expression c
				Expression x = oldVal.pixelExp.children.get(2);
				Expression y = oldVal.pixelExp.children.get(3);
				Expression c = new Expression("I", "|");
				c.add(oldVal.pixelExp.clone());
				if (oldVal.bitIndexFromRight==0)
					c.add(val.clone());
				else
					c.add(new Expression("I", "<<")
							.add(val.clone())
							.add(Expression.newLiteral("I", ""+oldVal.bitIndexFromRight)));
				bitmapHistory.add(new BitmapAccess(s.getUniqueID(), "setPixel", x, y, c));
				P.pause("added bitmap access: "+c.toString());
			}
		}
		elements.put(index, val.clone());
		aputHistory.add(new AputHistory(Expression.newLiteral("I", ""+index), val.clone(), true));
		this.isFromBitmap = val.related_to_pixel;
//		if (isFromBitmap)
//		{
//			vm.bitmapAccess.add(new BitmapAccess(s.getUniqueID(), "set_aput", "aput", 
//					Arrays.asList(
//							this.reference, 
//							Expression.newLiteral("I", ""+index),
//							val
//							)));
//		}
	}
	
	
	public void aput(APEXStatement s, Expression index, Expression val, VM vm)
	{
		
		if (val.toString().contains("getPixel"))
			this.isFromBitmap = true;
		if (index.isLiteral() && !index.isSymbolic)
			aput(s, Arithmetic.parseInt(index.children.get(0).root), val, vm);
		else {
			aputHistory.add(new AputHistory(index.clone(), val.clone(), false));
			//P.p("[aput history sym] "+index.toString()+" "+val.toString());
			if (isFromBitmap)
			{
				vm.bitmapAccess.add(new BitmapAccess(s.getUniqueID(), "set_aput", "aput", 
						Arrays.asList(this.reference, index, val )));
			}
		}
	}
	
	
	//NOTE: This AGET method might be incorrect sometimes... Example:
	/**		void m(int i, int j)
	 * 		{
	 * 			int[] arr = {0,1,2};
	 * 			arr[i] = 19;
	 * 			arr[1] = 20;
	 * 			int x = arr[i];
	 * 		}
	 * */
	// In this case, the value of x = arr[i] has 2 possibilities: 19 (if i!=1), 20 (if i==1)
	// But our aputHistory has [0,0], [1,1], [2,2], [i,19], [1,20]
	public Expression aget(Expression index, VM vm, APEXStatement s)
	{
		// concrete index and concrete element
		if (index.isLiteral() && !index.isSymbolic)
		{
			int i = Arithmetic.parseInt(index.children.get(0).root);
			if (elements.containsKey(i)) {
				return elements.get(i);
			}
				
		}
		
		// symbolic index but the aput history contains this index
		for (int i = aputHistory.size()-1; i>=0; i--)
		{
			AputHistory h = aputHistory.get(i);
			if (h.index.equals(index)) {
				return h.val;
			}
				
		}
		
		// can't find this element, so return a symbolic value
		String eleType = type.substring(1);
		if (Dalvik.isPrimitiveType(eleType))
		{
			if (isFromBitmap && bitmapReference != null) {
				APEXObject bitmap = vm.heap.get(bitmapReference.getObjID());
				// x = index/width, y = index%width
				if (bitmap.bitmapWidth==null)
				{
					if (vm.allocateConcreteBitmap) {
						bitmap.bitmapWidth = Expression.newLiteral("I", "30");
					}else {
						bitmap.bitmapWidth = new Expression("I", "return");
						bitmap.bitmapWidth.add(new Expression("Landroid/graphics/Bitmap;->getWidth()I"));
						bitmap.bitmapWidth.add(bitmapReference.clone());
					}
				}
				if (eleType.equalsIgnoreCase("B"))	//byte buffer only returns one channel of a pixel
				{
					Expression pixelIndex = Arithmetic.div(index, Expression.newLiteral("I", "4"), "I");
					Expression pixel = null;
					if (bitmap.concreteBitmap!=null && pixelIndex.isLiteral()&&!pixelIndex.isSymbolic)
					{
						int x = Arithmetic.parseInt(pixelIndex.toString())%bitmap.concreteBitmap[0].length;
						int y = Arithmetic.parseInt(pixelIndex.toString())/bitmap.concreteBitmap[0].length;
						pixel = Expression.newLiteral("I", bitmap.concreteBitmap[x][y]+"");
					}
					else
					{
						pixel = new Expression("I", "return");
						pixel.add(new Expression("Landroid/graphics/Bitmap;->getPixel(II)I"));
						pixel.add(bitmap.reference.clone());
						pixel.add(Arithmetic.rem(pixelIndex, bitmap.bitmapWidth, "I"));
						pixel.add(Arithmetic.div(pixelIndex, bitmap.bitmapWidth, "I"));
					}
					String sig = "Landroid/graphics/Color;->alpha(I)I";
					int channelIndex = (index.isLiteral()&&!index.isSymbolic)?
							Arithmetic.parseInt(index.toString())%4:new Random().nextInt(4);
					switch (channelIndex)
					{
					case 0: // red
						sig = "Landroid/graphics/Color;->red(I)I";
						break;
					case 1:	// green
						sig = "Landroid/graphics/Color;->green(I)I";
						break;
					case 2: // blue
						sig = "Landroid/graphics/Color;->blue(I)I";
						break;
					}
					Expression res = new Expression("I","return");
					res.add(new Expression(sig));
					res.add(pixel.clone());
					res.related_to_pixel = true;
					return res;
				}
				else
				{
					Expression res = null;
					if (bitmap.concreteBitmap!=null && index.isLiteral()&&!index.isSymbolic)
					{
						int x = Arithmetic.parseInt(index.toString())%bitmap.concreteBitmap[0].length;
						int y = Arithmetic.parseInt(index.toString())/bitmap.concreteBitmap[0].length;
						if (x>=0 && x < bitmap.concreteBitmap.length &
								y>=0 && y < bitmap.concreteBitmap[0].length &&
								bitmap.concreteBitmap[x][y]!=null)
							return bitmap.concreteBitmap[x][y];
					}
					res = new Expression("I", "return");
					res.add(new Expression("Landroid/graphics/Bitmap;->getPixel(II)I"));
					res.add(bitmap.reference.clone());
					res.add(Arithmetic.rem(index, bitmap.bitmapWidth, "I"));
					res.add(Arithmetic.div(index, bitmap.bitmapWidth, "I"));
					res.related_to_pixel = true;
					return res;
				}
			}
			else if (eleType.contentEquals("C") && isCharArrayOfString && strExp!=null) {
				if (strExp.isLiteral()) {
					Expression res = new Expression("C", "return");
					res.add("Ljava/lang/String->charAt(I)C");
					res.add(strExp.getLiteralValue());
					res.add(index.clone());
					return res;
				} else if (strExp.getObjID()!=null){
					APEXObject str = vm.heap.get(strExp.getObjID());
					if (str.imageDataExp != null) {
						//P.p("-- hehe");
						//P.pause();
						// red.toBinaryString().toCharArray()[x] = (red >> x) & 1
						Expression res = new Expression("C", "&");
						Expression left = new Expression(">>");
						left.add(str.imageDataExp.clone()).add(index.clone());
						res.add(left).add(Expression.newLiteral("I", "0x1"));
						res.isBinaryBitOfImageData = true;
						return res;
					}
				}
			}
			
			Expression res = Expression.newLiteral(eleType, this.objID+"["+index.toString()+"]");
			res.isSymbolic = true;
			return res;
		}
		else if (Dalvik.isArrayType(eleType))
		{
			//NOTE: need to solve multi-dimensional arrays
			APEXArray arr = vm.createNewArray(eleType, this.objID+"["+index.toString()+"]", null, s.getUniqueID()+" "+s.smali);
			arr.isSymbolic = true;
			arr.reference.isSymbolic = true;
			//P.p("  created new array element for array " + reference.getObjID()+"[" + index.toString()+"]");
			aput(s, index, arr.reference, vm);
			arr.parentArray = this;
			arr.indexInParent = index.clone();
			return arr.reference;
		}
		else
		{
			APEXObject obj = vm.createNewObject(eleType, this.objID+"["+index.toString()+"]", s.getUniqueID()+" "+s.smali, true);
			//P.p("  created new object element for array " + reference.getObjID()+"[" + index.toString()+"]");
			this.aput(s, index, obj.reference, vm);
			return obj.reference;
		}
	}
	
	public void print(PrintStream out)
	{
		out.println(" --- "+this.objID+": "+this.root+" "+this.type);
		out.println("\t*** birth: "+this.birth);
		out.println("\t*** array length: " + length.toString());
		out.println("\t*** elements:");
		for (Map.Entry<Integer, Expression> eles: elements.entrySet())
		{
			out.println("\t["+eles.getKey()+"] = " + eles.getValue().toString());
		}
		if (!aputHistory.isEmpty())
		{
			out.println("\t*** aput history");
			for (AputHistory h : aputHistory)
			{
				out.println("\t[ "+h.index.toString()+" ] = " + h.val.toString());
			}
		}
	}
	
	public void writeToFile(PrintWriter out)
	{
		out.println("\t*** array length: " + length.toString());
		out.println("\t*** elements:");
		for (Map.Entry<Integer, Expression> eles: elements.entrySet())
		{
			out.println("\t["+eles.getKey()+"] = " + eles.getValue().toString());
		}
		if (!aputHistory.isEmpty())
		{
			out.println("\t*** aput history");
			for (AputHistory h : aputHistory)
			{
				out.println("\t[ "+h.index.toString()+" ] = " + h.val.toString());
			}
		}
	}
	
	public void print()
	{
		print(System.out);
	}

	
}
