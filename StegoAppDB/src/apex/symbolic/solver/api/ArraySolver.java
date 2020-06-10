package apex.symbolic.solver.api;

import java.util.List;

import apex.bytecode_wrappers.APEXStatement;
import apex.symbolic.APEXArray;
import apex.symbolic.APEXObject;
import apex.symbolic.APEXObject.BitmapAccess;
import apex.symbolic.Expression;
import apex.symbolic.MethodContext;
import apex.symbolic.VM;
import apex.symbolic.solver.Arithmetic;
import util.P;

public class ArraySolver extends SolverInterface{

	@Override
	public void solve(String invokeSig, List<Expression> params, APEXStatement s, MethodContext mc, String[] paramRegs, VM vm)
	{
		if (invokeSig.equals("Ljava/lang/reflect/Array;->newInstance(Ljava/lang/Class;[I)Ljava/lang/Object;"))
		{
			if (params.get(1).getObjID()==null) {
				vm.crashed = vm.shouldStop = true;
				return;
			}
			String eleType = "";
			if (params.get(0).isLiteral())
			{
				eleType = params.get(0).getLiteralValue();
			}
			else
			{
				// first parse the array type
				APEXObject typeClass = vm.heap.get(params.get(0).getObjID());
				String birthSmali = typeClass.birth.substring(typeClass.birth.indexOf(" ")+1);
				if (birthSmali.startsWith("sget-object"))
				{
					String fieldSig = birthSmali.substring(birthSmali.lastIndexOf(" ")+1);
					if (fieldSig.endsWith("->TYPE:Ljava/lang/Class;"))
					{
						String className = fieldSig.substring(0, fieldSig.indexOf("->"));
						eleType = PrimitiveWrapperSolver.getPrimitiveName(className);
					}
				}
				if (eleType.isEmpty())
				{
					P.p("Can't solve the element type of Array.newInstance(Class, int...) call");
					P.p(s.getUniqueID()+" "+s.smali);
					typeClass.print();
					P.pause();
				}
			}
			APEXObject dimenObj = vm.heap.get(params.get(1).getObjID());
			if (!(dimenObj instanceof APEXArray)) {
				vm.shouldStop = vm.crashed = true;
				return;
			}
			// modify the array element type if we know the number of dimensions of the array
			// we should know the number most of the time
			String basicEleType = eleType;
			boolean canInitialize = false;
			APEXArray dimen = (APEXArray)dimenObj;
			if (P.isInteger(dimen.length.toString())) {
				int layers = Arithmetic.parseInt(dimen.length.toString());
				for (int i=0; i<layers-1; i++)
					eleType = "["+eleType;
				//dimen.aget(index, vm, s)
				// if all dimensions are literal numbers, populate the
				// array now
			}
			APEXArray arr = vm.createNewArray("["+eleType, "Array.newInstance", params.get(1), s.getUniqueID()+" "+s.smali);
			vm.recentResult = arr.reference;
			//P.pause();
		}
		else if (invokeSig.equals("Ljava/nio/ByteBuffer;->allocate(I)Ljava/nio/ByteBuffer;"))
		{
			APEXObject buffer = vm.createNewObject("Ljava/nio/ByteBuffer;", "ByteBuffer.allocate", s.getUniqueID(), false);
			buffer.bufferLength = params.get(0).clone();
			vm.recentResult = buffer.reference;
		}
		else if (invokeSig.equals("Ljava/nio/ByteBuffer;->array()[B"))
		{
			if (params.get(0).getObjID() == null)
			{
				vm.crashed = vm.shouldStop = true;
				return;
			}
			APEXObject buffer = vm.heap.get(params.get(0).getObjID());
			APEXArray arr = vm.createNewArray("[B", "ByteBuffer.array()", buffer.bufferLength, s.getUniqueID());
			if (buffer.isFromBitmap)
			{
				arr.isFromBitmap = true;
				arr.bitmapReference = buffer.bitmapReference.clone();
				buffer.arrayReference = arr.reference.clone();
			}
			vm.recentResult = arr.reference;
		}
		else if (invokeSig.equals("Ljava/nio/ByteBuffer;->wrap([B)Ljava/nio/ByteBuffer;"))
		{
			if (params.get(0).getObjID()==null)
			{
				vm.crashed = vm.shouldStop = true;
				return;
			}
			APEXObject obj = vm.heap.get(params.get(0).getObjID());
			if (!(obj instanceof APEXArray)) {
				vm.crashed = vm.shouldStop = true;
				return;
			}
			APEXArray arr = (APEXArray) obj;
			APEXObject buffer = vm.createNewObject("Ljava/nio/ByteBuffer;", "ByteBuffer.wrap([B)", s.getUniqueID(), false);
			buffer.bufferLength = arr.length.clone();
			buffer.arrayReference = arr.reference.clone();
			if (arr.isFromBitmap) {
				buffer.isFromBitmap = true;
			}
			vm.recentResult = buffer.reference;
		}
		else if (invokeSig.contentEquals("Ljava/nio/ByteBuffer;->asIntBuffer()Ljava/nio/IntBuffer;")) {
			if (params.get(0).getObjID()==null)
			{
				vm.crashed = vm.shouldStop = true;
				return;
			}
			APEXObject buffer = vm.heap.get(params.get(0).getObjID());
			vm.recentResult = buffer.reference.clone();
		}
		else if (invokeSig.contentEquals("Ljava/nio/IntBuffer;->array()[I"))
		{
			APEXObject buffer = vm.heap.get(params.get(0).getObjID());
			vm.createSymbolicMethodReturn("[I", invokeSig, params, s);
			if (buffer.isFromBitmap)
			{
				APEXArray arr = (APEXArray) vm.heap.get(vm.recentResult.getObjID());
				buffer.arrayReference = arr.reference.clone();
				arr.isFromBitmap = true;
				arr.bitmapReference = buffer.bitmapReference.clone();
			}
		}
		else if (invokeSig.contentEquals("Ljava/nio/IntBuffer;->get([I)Ljava/nio/IntBuffer;")) {
			if (params.get(0).getObjID()==null || params.get(1).getObjID()==null)
			{
				vm.crashed = vm.shouldStop = true;
				return;
			}
			// transfer data from buffer into array
			APEXObject buffer = vm.heap.get(params.get(0).getObjID());
			APEXArray array = (APEXArray) vm.heap.get(params.get(1).getObjID());
			if (buffer.isFromBitmap) {
				array.isFromBitmap = true;
				APEXArray arr = (APEXArray) vm.heap.get(buffer.arrayReference.getObjID());
				array.aputHistory = arr.aputHistory;
			}
			vm.recentResult = buffer.reference.clone();
		}
		else if (invokeSig.contentEquals("Ljava/nio/IntBuffer;->put([I)Ljava/nio/IntBuffer;")) {
			APEXObject buffer = vm.heap.get(params.get(0).getObjID());
			if (params.get(1).getObjID()==null) {
				vm.crashed = vm.shouldStop = true;
				return;
			}
			APEXArray array = (APEXArray) vm.heap.get(params.get(1).getObjID());
			buffer.arrayReference = array.reference.clone();
			if (array.isFromBitmap) {
				buffer.isFromBitmap = true;
				buffer.bitmapReference = array.bitmapReference.clone();
			}
			vm.recentResult = buffer.reference.clone();
		}
		else if (invokeSig.contentEquals("Ljava/nio/IntBuffer;->rewind()Ljava/nio/Buffer;") || 
				invokeSig.contentEquals("Ljava/nio/ByteBuffer;->rewind()Ljava/nio/Buffer;"))
		{
			vm.recentResult = params.get(0).clone();
		}
		else if (invokeSig.equals("Ljava/lang/System;->arraycopy(Ljava/lang/Object;ILjava/lang/Object;II)V"))
		{
			if (params.get(0).getObjID()==null || params.get(2).getObjID()==null) {
				vm.crashed = vm.shouldStop = true;
			}
			APEXObject srcObj = vm.heap.get(params.get(0).getObjID());
			APEXObject dstObj = vm.heap.get(params.get(2).getObjID());
			if (srcObj == null || !(srcObj instanceof APEXArray) || dstObj == null ||
					!(dstObj instanceof APEXArray))
			{
				vm.crashed = vm.shouldStop = true;
				return;
			}
			APEXArray src = (APEXArray) srcObj;
			APEXArray dst = (APEXArray) dstObj;
			Expression srcPos = params.get(1);
			Expression dstPos = params.get(3);
			Expression length = params.get(4);
			
/*			P.p("--- src ---");
			src.print();
			P.p("--- dst ---");
			dst.print();
			P.p("srcPos = "+srcPos.toString());
			P.p("dstPos = "+dstPos.toString());
			P.p("length = "+length.toString());*/
			
			if (length.isLiteral() && !length.isSymbolic)
			{
				int l = Arithmetic.parseInt(length.toString());
				for (int i = 0; i < l; i++)
				{
					Expression delta = Expression.newLiteral("I", i+"");
					Expression readIndex = Arithmetic.add(srcPos, delta, "I");
					Expression writeIndex = Arithmetic.add(dstPos, delta, "I");
					Expression get = src.aget(readIndex, vm, s);
					dst.aput(s, writeIndex, get, vm);
				}
			}
		}
		else if (invokeSig.contentEquals("Ljava/lang/String;->toCharArray()[C")) {
			vm.createSymbolicMethodReturn("[C", invokeSig, params, s);
			APEXArray arr = (APEXArray) vm.heap.get(vm.recentResult.getObjID());
			arr.isCharArrayOfString = true;
			arr.strExp = params.get(0).clone();
		}
		else if (invokeSig.equals("Ljava/lang/String;->valueOf(C)Ljava/lang/String;")) {
			vm.recentResult = params.get(0).clone();
			vm.recentResult.type = "Ljava/lang/String;";
			vm.recentResult.isSymbolic = true;
		}
		else if (invokeSig.contentEquals("Ljava/lang/String;->substring(II)Ljava/lang/String;")) {
			if (params.get(0).isReference()) {
				APEXObject str = vm.heap.get(params.get(0).getObjID());
				if (str.imageDataExp != null) {
					Expression from = params.get(1), to = params.get(2);
					Expression bitsToShift = null;
					if (from.toString().contentEquals("0") && to.root.contentEquals("-")) {
						// Color.red().toBinaryString().substring(0, x-a) ->  red >> a << a
						// gonna risk it and not check the x in x-a
						bitsToShift = to.children.get(1).clone();
					}
					else if (from.toString().contentEquals("0") && to.toString().contentEquals("7") &&
							str.imageDataExp.isReturnValue() && 
							str.imageDataExp.getInvokeSig().startsWith("Landroid/graphics/Color;->")) {
						bitsToShift = Expression.newLiteral("I", "0x1");
					}
					
					if (bitsToShift != null) {
						Expression newImageData = new Expression("<<");
						newImageData.type = str.imageDataExp.type;
						Expression shr = new Expression(">>");
						shr.add(str.imageDataExp).add(bitsToShift.clone());
						newImageData.add(shr).add(bitsToShift.clone());
						str.imageDataExp = newImageData;
						vm.recentResult = str.reference.clone();
						return;
					}
				}
			}
			vm.createSymbolicMethodReturn("Ljava/lang/String;", invokeSig, params, s);
		}
		else if (invokeSig.contentEquals("Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;")) {
			if (params.get(0).getObjID()==null) {
				vm.crashed = vm.shouldStop = true;
				return;
			}
			APEXObject sb = vm.heap.get(params.get(0).getObjID());
			Expression strExp = params.get(1);
			
			// first try adding to most recent
			boolean appended = false;
			List<Expression> history = sb.stringBuilderAppendHistory;
			if (!history.isEmpty() && strExp.isLiteral()) {
				Expression prevStr = history.get(history.size()-1);
				if (prevStr.isLiteral()) {
					prevStr.children.get(0).root += strExp.toString();
					appended = true;
				} else if (prevStr.getObjID()!=null) {
					APEXObject prevS = vm.heap.get(prevStr.getObjID());
					if (prevS.imageDataExp != null && P.isInteger(strExp.toString())) { // prev string is the bit string of an image data
						int pl = Arithmetic.parseInt(strExp.toString());
						Expression newImageData = new Expression("|");
						newImageData.type = prevS.imageDataExp.type;
						newImageData.add(prevS.imageDataExp).add(Expression.newLiteral("I", pl+""));
						prevS.imageDataExp = newImageData;
						appended = true;
					}
				}
			}
			if (!appended)
				history.add(strExp.clone());
//			if (s.index==105 || s.index==106) {
//				P.p("-- appended:");
//				P.p(strExp.root);
//				P.p(s.getUniqueID());
//				P.p("prev size was "+(history.size()-1));
//				P.pause();
//			}
			
			vm.recentResult = sb.reference.clone();
		}
		else if (invokeSig.contentEquals("Ljava/lang/StringBuilder;->toString()Ljava/lang/String;")) {
			if (params.get(0).getObjID()==null) {
				vm.crashed = vm.shouldStop = true;
				return;
			}
			APEXObject sb = vm.heap.get(params.get(0).getObjID());
			if (!sb.stringBuilderAppendHistory.isEmpty()) {
				vm.recentResult = sb.stringBuilderAppendHistory.get(sb.stringBuilderAppendHistory.size()-1).clone();
			}
			else
				vm.createSymbolicMethodReturn("Ljava/lang/String;", invokeSig, params, s);
		}
		else if (invokeSig.contentEquals("Ljava/lang/String;->concat(Ljava/lang/String;)Ljava/lang/String;")) {
			Expression s1 = params.get(0), s2 = params.get(1);
			
			boolean transformed = false;
			if (s1.isLiteral() && s2.isReference()) {
				//NOTE: this is for the special case of appending "0" 
				// 		at the beginning of pixel binary strings
				APEXObject str = vm.heap.get(s2.getObjID());
				if (str.imageDataExp != null) {
					vm.recentResult = str.reference.clone();
					transformed = true;
				}
			}
			else if (s1.isReference()) {
				APEXObject str = vm.heap.get(s1.getObjID());
				if (str.imageDataExp != null) {
					//NOTE: this is for the case where payload is being
					// appended to image data
					Expression payload = new Expression("I", "return")
							.add("Ljava/lang/Integer;->parseInt(Ljava/lang/String;I)I")
							.add(s2.clone()).add(Expression.newLiteral("I", "0x2"));
					Expression newImageData = new Expression(str.imageDataExp.type, "|")
							.add(str.imageDataExp.clone()).add(payload);
					str.imageDataExp = newImageData;
					vm.recentResult = str.reference.clone();
					// public BitmapAccess(String stmtID, String action, Expression x, Expression y, Expression c)
					Expression fakeX = Expression.newLiteral("I", "0x0");
					str.bitmapHistory.add(new BitmapAccess(
							s.getUniqueID(), "setPixel", fakeX, fakeX, str.imageDataExp.clone()));
					//P.pause(str.imageDataExp.toString());
					transformed = true;
				}
			}
			if (!transformed)
				vm.createSymbolicMethodReturn("Ljava/lang/String;", invokeSig, params, s);
		}
		else if (invokeSig.equals("Ljava/lang/String;->hashCode()I")) {
			if (params.get(0).isLiteral())
				vm.recentResult = Expression.newLiteral("I", params.get(0).toString().hashCode()+"");
			else
				vm.createSymbolicMethodReturn("I", invokeSig, params, s);
		}
		else
		{
			P.p("Forgot about this API?? " + invokeSig);
			P.pause();
		}
	}
}
