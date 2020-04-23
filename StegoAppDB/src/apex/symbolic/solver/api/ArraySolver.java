package apex.symbolic.solver.api;

import java.util.List;

import apex.bytecode_wrappers.APEXStatement;
import apex.symbolic.APEXArray;
import apex.symbolic.APEXObject;
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
					typeClass.print();
					P.pause();
				}
			}
			// NOTE: only assigning the first dimension for now...
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
			APEXArray arr = (APEXArray) vm.heap.get(params.get(0).getObjID());
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
			APEXArray src = (APEXArray) vm.heap.get(params.get(0).getObjID());
			APEXArray dst = (APEXArray) vm.heap.get(params.get(2).getObjID());
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
		else
		{
			P.p("Forgot about this API?? " + invokeSig);
			P.pause();
		}
	}
}
