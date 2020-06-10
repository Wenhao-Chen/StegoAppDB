package app_analysis.trees.exas_old;

import java.io.File;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import apex.symbolic.Expression;
import app_analysis.common.Dirs;
import app_analysis.trees.ReferenceTrees;
import app_analysis.trees.TreeRefUtils;
import util.F;
import util.P;

public class TreeExas2 {
	
	public static final File exasRoot = new File(Dirs.NotesRoot, "Exas");
	
	public File expF, recordDir, stegoRecordF;
	public String appName, ID;
	private Boolean isStegoTree;
	private Expression exp_full, exp_normalized, exp_trimmed;
	
	public TreeExas2(File expF, String appName, String ID) {
		this.expF = expF;
		this.appName = appName;
		this.ID = ID;
		File appDir = new File(exasRoot, appName);
		recordDir = new File(appDir, ID);
		recordDir.mkdirs();
		stegoRecordF = new File(recordDir, "is_stego.txt");
	}

	
	
	public Map<String, Integer> getFeatures(Map<String, Integer> dict, String appSet,
			int nPath, boolean doPQNode, boolean doHorizontal, boolean redo) {
		Map<String, Integer> exas = new HashMap<>();
		File featureF = new File(recordDir, 
				"Features_"+appSet+"_"+nPath+"_"+(doPQNode?"Y":"N")+"_"+(doHorizontal?"Y":"N")+".txt");
		if (!redo && featureF.exists()) {
			//NOTE: can read from record file instead
			F.readLinesWithoutEmptyLines(featureF).forEach(l->{
				String[] parts = l.split(" , ");
				exas.put(parts[0], Integer.parseInt(parts[1]));
			});
		} else {
			ExasUtils2.collectFeatures(exas, getExpression_Normalized(), dict, nPath, doPQNode, doHorizontal);
			PrintWriter out = F.initPrintWriter(featureF);
			for (Map.Entry<String, Integer> entry : exas.entrySet())
				out.println(entry.getKey()+" , "+entry.getValue());
			if (exas.isEmpty())
				out.println("");
			out.close();
		}
		return exas;
	}
	
	
	
	public boolean isStego() {
		if (isStegoTree == null) {
			// first try to load record
			if (stegoRecordF.exists())
				isStegoTree = F.readFirstLine(stegoRecordF).equals("true");
			else {
				isStegoTree = ReferenceTrees.isStego(getExpression_Normalized());
				F.writeLine(isStegoTree?"true":"false", stegoRecordF, false);
			}
		}
		return isStegoTree;
	}
	
	public Expression getExpression_Full() {
		if (exp_full == null)
			exp_full = (Expression) F.readObject(expF);
//		if (exp_full == null)
//			P.e("exp still null after ReadObject: "+expF.getAbsolutePath());
		return exp_full;
	}
	
	public Expression getExpression_Normalized() {
		if (exp_normalized == null) {
			exp_normalized = (Expression) F.readObject(expF);
			try {
				TreeRefUtils.normalize(exp_normalized);
				TreeRefUtils.trim(exp_normalized);
			}
			catch (Exception e) {
				P.e("problem with normalization: "+ID);
				P.pause();
			} 
		}
		
		return exp_normalized;
	}
	
	public Expression getExpression_Trimmed() {
		if (exp_trimmed == null) {
			exp_trimmed = (Expression) F.readObject(expF);
			TreeRefUtils.trim(exp_trimmed);
		}
		return exp_trimmed;
	}
	
	public void visualize() {
		if (getExpression_Full() == null)
			return;
		File fullF = getExpression_Full().toDotGraph(ID, expF.getParentFile(), false);
		File trimmedF = getExpression_Full().toDotGraph(ID, expF.getParentFile(), true);
		showPDF(fullF);
		showPDF(trimmedF);
	}
	
	private void showPDF(File pdfF) {
		P.exec("explorer.exe \""+pdfF.getAbsolutePath()+"\"", false);
	}
	
}
