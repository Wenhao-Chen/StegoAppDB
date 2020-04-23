package app_analysis.symbolic;

import java.io.File;

import apex.APEXApp;

public class ICFG {

	public static void main(String[] args)
	{
		File apk = new File("C:\\workspace\\app_analysis\\apks\\stego_github\\app.steganosaurus.apk");
		makeICFG(apk);
	}
	
	static void makeICFG(APEXApp app) {
		
		// how to tell if two observation points are connected??
		// if they are in the same method - easy, just use connectivity inspector
		// if they are not in the same method - 
		
		
	}
	
	
	static void makeICFG(File apk) {
		makeICFG(new APEXApp(apk));
	}
}
