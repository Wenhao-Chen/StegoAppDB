package app_analysis.trees.exas.raw_results;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import util.F;
import util.P;

public class App {

	File apk;
	boolean isStego;
	String label;
	
	App(File apk) {
		this.apk = apk;
		isStego = getStegoAppNames().contains(apk.getName());
		label = getStegoAppLabels().computeIfAbsent(apk.getName(), k->"N/A");
		P.p(apk.getName()+" "+isStego+" "+label);
	}
	
	
	static final File StegoAppNamesF = new File("C:\\workspace\\app_analysis\\notes\\ConfirmedStegoApps.txt");
	private static Set<String> stegoAppNames;
	public static Set<String> getStegoAppNames() {
		if (stegoAppNames == null)
			stegoAppNames = new HashSet<>(F.readLinesWithoutEmptyLines(StegoAppNamesF));
		return stegoAppNames;
	}
	
	public static boolean isStego(String apkName) {
		return getStegoAppNames().contains(apkName);
	}
	
	static final File StegoAppLabelsF = new File("C:\\workspace\\app_analysis\\notes\\StegoApp_Labels.txt");
	private static Map<String, String> stegoAppLabels;
	public static Map<String, String> getStegoAppLabels() {
		if (stegoAppLabels == null) {
			stegoAppLabels = new HashMap<>();
			for (String line : F.readLinesWithoutEmptyLines(StegoAppLabelsF)) {
				String[] parts = line.split(" , ");
				stegoAppLabels.put(parts[0], parts[1]);
			}
		}
		return stegoAppLabels;
	}
	
	
}
