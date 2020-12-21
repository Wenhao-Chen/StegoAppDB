package app_analysis.oakland;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import apex.bytecode_wrappers.APEXMethod;
import apex.bytecode_wrappers.APEXStatement;
import apex.symbolic.APEXArray;
import apex.symbolic.APEXArray.AputHistory;
import apex.symbolic.listeners.ExecutionListener;
import apex.symbolic.APEXObject;
import apex.symbolic.Expression;
import apex.symbolic.MethodContext;
import apex.symbolic.VM;
import app_analysis.common.Dirs;
import app_analysis.old.FindingEntryPoints;
import app_analysis.trees.exas.Reference;
import app_analysis.trees.exas.TreeFeature.Options;
import ui.ProgressUI;
import util.P;

public class SpatialEmbedding extends DetectionFramework {

	
	String appSet = "all_apps";
	
	public static void main(String[] args) {
		boolean redo = false;
		List<File> apks = Dirs.getAllFiles();
		ProgressUI ui_app = ProgressUI.create("overall app");
		int appCount = 0, total = apks.size();
		for (File apk : apks) {
			ui_app.newLine(String.format("%d/%d %s", ++appCount, total, apk.getName()));
			new SpatialEmbedding(apk, redo).detect();
		}
	}

	SpatialEmbedding(File apk, boolean redo) { super(apk, redo); }

	@Override
	public Set<APEXMethod> scopeMethods() {
		return FindingEntryPoints.find(app, null);
	}

	@Override
	public void initReferenceTrees() {
		Map<String, List<Expression>> allRefTrees = Reference.getReferenceTrees();
		allRefTrees.remove("parse_int");
		ref_trees = new ArrayList<>();
		for (String group : allRefTrees.keySet()) {
			List<Expression> list = allRefTrees.get(group);
			for (int i=0; i<list.size(); i++)
				ref_trees.add(new RefTree(group+"_"+(i+1), list.get(i)));
		}
	}

	@Override
	public void initExecListener() {
		this.execListener = new ExecutionListener() {

			@Override
			public void preStatementExecution(VM vm, MethodContext mc, APEXStatement s) {
				
			}

			@Override
			public void postStatementExecution(VM vm, MethodContext mc, APEXStatement s) {
				if (s.isInvokeStmt() && !vm.crashed) {
					String sig = s.getInvokeSignature();
					String[] paramRegs = s.getParamRegisters();
					if (sig.equals("Landroid/graphics/Bitmap;->setPixel(III)V")) {
						Expression exp = mc.read(paramRegs[3]);
						P.pause("set pixel detected:\n"+exp.toStringRaw());
					}
					else if (sig.equals("Landroid/graphics/Bitmap;->setPixels([IIIIIII)V")) {
						APEXArray arr = (APEXArray) vm.heap.get(mc.read(paramRegs[1]).getObjID());
						for (AputHistory aput : arr.aputHistory) {
							Expression exp = aput.val;
							P.pause("set pixels detected:\n"+exp.toStringRaw());
						}
					}
					else if (sig.equals("Landroid/graphics/Bitmap;->copyPixelsFromBuffer(Ljava/nio/Buffer;)V")) {
						APEXObject buffer = vm.heap.get(mc.read(paramRegs[1]).getObjID());
						if (buffer.arrayReference != null) {
							APEXArray arr = (APEXArray) vm.heap.get(buffer.arrayReference.getObjID());
							for (AputHistory aput : arr.aputHistory) {
								Expression exp = aput.val;
								P.pause("pixel buffer detected:\n"+exp.toStringRaw());
							}
						}
					}
				}
			}
		};
	}

	@Override
	public void initTreeMatchingOptions() {
		int N = 4;
		boolean horizontal = true;
		boolean PQ = true;
		boolean redo = false;
		this.treeMatchingOptions = new Options(appSet, N, horizontal, PQ, redo);
	}

}
