package app_analysis.trees.exas;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import app_analysis.common.Dirs;
import app_analysis.trees.exas.TreeFeature.Options;

public class Experiment_clustering {

	public static void main(String[] args)
	{
		// cluster all the app trees and see if there are groups???
		cluster();
	}

	
	static void cluster() {
		List<File> apks = Dirs.getFiles(Dirs.Stego_Github, Dirs.Stego_Others, Dirs.Stego_PlayStore);
		String appSet = "stegos";
		
		int N = 4;
		boolean horizontal = true;
		boolean PQ = true;
		boolean redo = false;
		Options opt = new Options(appSet, N, horizontal, PQ, redo);
		long t = System.currentTimeMillis();
		Map<String, List<TreeWrapper>> allAppTrees = Experiment1.getTrees(apks);
		
		// K-Medoid
		List<TreeWrapper> allTrees = new ArrayList<TreeWrapper>();
		for (List<TreeWrapper> list : allAppTrees.values())
			allTrees.addAll(list);
		
	}
}
