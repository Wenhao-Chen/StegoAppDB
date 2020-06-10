package app_analysis.trees.exas;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import apex.symbolic.Expression;
import app_analysis.common.Dirs;
import app_analysis.trees.TreeRefUtils;
import app_analysis.trees.exas.TreeFeature.Options;
import util.F;
import util.P;

public class TreeWrapper {
	
	public File recordDir, expF;
	private Expression exp_full, exp_trimmed;
	public String appName;
	private Map<String, Integer> features;
	private int size = -1;
	private int hash = 0;
	private boolean alreadyTrimmed = false;
	public TreeWrapper(File expF, String appName) {
		this(expF, appName, false);
	}
	
	public TreeWrapper(File expF, String appName, boolean alreadyTrimmed) {
		this.expF = expF;
		this.appName = appName;
		File appDir = new File(Dirs.ExasRoot, appName);
		recordDir = new File(appDir, expF.getName().substring(0, expF.getName().length()-11));
		recordDir.mkdirs();
		this.alreadyTrimmed = alreadyTrimmed;
	}
	
	public int getSize() {
		if (size == -1) {
			size = countNodes(getExpressionTrimmed());
		}
		return size;
	}
	private int countNodes(Expression e) {
		if (e == null)
			return 0;
		int count = 1;
		for (Expression child : e.children)
			count += countNodes(child);
		return count;
	}
	
	public int getHash() {
		if (hash == 0)
			hash = getExpressionTrimmed()==null?0:getExpressionTrimmed().toStringRaw().hashCode();
		return hash;
	}
	
	public Expression getExpressionFull() {
		if (exp_full == null) {
			exp_full = (Expression) F.readObject(expF);
		}
		return exp_full;
	}
	
	public Expression getExpressionTrimmed() {
		if (exp_trimmed == null && getExpressionFull()!=null) {
			exp_trimmed = getExpressionFull().clone();
			if (!alreadyTrimmed)
				TreeRefUtils.trim2(exp_trimmed);
		}
		return exp_trimmed;
	}
	
	public Map<String, Integer> getFeatures(Options opt) {
		File featureF = new File(recordDir, "Features_"+opt.toString()+".txt");
		if (!opt.redo && featureF.exists()) {
			features = new HashMap<>();
			for (String line : F.readLinesWithoutEmptyLines(featureF)) {
				String[] parts = line.split(" , ");
				features.put(parts[0], Integer.parseInt(parts[1]));
			}
		} else {
			features = TreeFeature.collectFeatures(getExpressionTrimmed(), opt);
			F.write(features, " , ", featureF, false);
		}
		return features;
	}
	
	public double getSim1(Map<String, Integer> refFeatures, int refHash, Options opt) {
		File simF = getSimFile(refHash, opt);
		double sim = -1.0;
		if (!opt.redo && simF.exists()) {
			sim = Double.parseDouble(F.readFirstLine(simF));
		} else {
			sim = Experiment1.sim_norm1(getExpressionTrimmed(), refFeatures, opt);
			F.write(sim+"", simF, false);
		}
		return sim;
	}
	
	public void touchSim1(TreeWrapper tw, Options opt) {
		File simFile = getSimFile(tw.getHash(), opt);
		File simFile2 = tw.getSimFile(getHash(), opt);
		if (!opt.redo && simFile.exists() && simFile2.exists())
			return;
		double sim = Experiment1.sim_norm1(getExpressionTrimmed(), tw.getFeatures(opt), opt);
		recordSim1(opt, tw.getHash(), sim);
		tw.recordSim1(opt, getHash(), sim);
	}
	
	public double getSim1(TreeWrapper tw, Options opt) {
		File simFile = getSimFile(tw.getHash(), opt);
		double sim = -1.0;
		if (!opt.redo && simFile.exists()) {
			sim = Double.parseDouble(F.readFirstLine(simFile));
		} else {
			sim = Experiment1.sim_norm1(getExpressionTrimmed(), tw.getFeatures(opt), opt);
			recordSim1(opt, tw.getHash(), sim);
			tw.recordSim1(opt, getHash(), sim);
		}
		return sim;
	}
	
	public void recordSim1(Options opt, int hash, double sim) {
		File f = getSimFile(hash, opt);
		F.write(sim+"", f, false);
	}
	
	private File getSimFile(int hash, Options opt) {
		return new File(recordDir, "Sim1_"+opt.toString()+"_"+hash+".txt");
	}
	
	
	public void visualize() {
		if (getExpressionFull() != null) {
			File f1 = getExpressionFull().toDotGraph("raw", recordDir, false);
			P.exec("explorer.exe \""+f1.getAbsolutePath()+"\"", false);
		}
		if (getExpressionTrimmed() != null) {
			File f2 = getExpressionTrimmed().toDotGraph("trimmed", recordDir, false);
			P.exec("explorer.exe \""+f2.getAbsolutePath()+"\"", false);
		} 
	}

}
