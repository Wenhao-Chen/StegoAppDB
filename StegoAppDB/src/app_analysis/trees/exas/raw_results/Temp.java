package app_analysis.trees.exas.raw_results;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import app_analysis.common.Dirs;

public class Temp {

	
	
	public static void main(String[] args) {
		
		// collect data set
		List<App> apps = collectApps();
		
		// do experiment 1: use hand-crafted ref trees
		
		
		
		
		// do experiment 2: use app trees as ref trees
		
		
	}
	
	
	static List<App> collectApps() {
		List<App> apps = new ArrayList<>();
		
		// 120 potential stego apps (77 stegos + 43 non-stegos)
		for (File f : Dirs.getStegoFiles())
			apps.add(new App(f));
		// 1100 image editing apps
		for (File f : Dirs.getFiles(Dirs.ImageApps))
			apps.add(new App(f));
		
		
		return apps;
	}
}
