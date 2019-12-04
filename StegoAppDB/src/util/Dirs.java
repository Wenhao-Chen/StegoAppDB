package util;

import java.io.File;

public class Dirs {

	public static final File Desktop = new File("C:/Users/C03223-Stego2/Desktop");
	public static final File StegoRoot = new File(Desktop, "stego");
	public static final File OriginalAPKDir = new File(StegoRoot, "original_apks");
	public static final File DecodedDir = new File(StegoRoot, "decoded");
	public static final File NotesDir = new File(StegoRoot, "notes");
	
}
