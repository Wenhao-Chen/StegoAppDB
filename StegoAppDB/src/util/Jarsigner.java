package util;

import java.io.File;
import java.io.OutputStream;


public class Jarsigner {

	
	/** NOTE:
	 * For older versions of Android (e.g. Android 4.0.3),
	 * there might an error "INSTALL_PARSE_FAILED_NO_CERTIFICATES"
	 * when installing the already signed app. This is *probably* because
	 * older Android versions require "V1" signature but the jarsigner
	 * signed a "V2" signature.
	 * 
	 * Using an older version (e.g. jdk6) of jarsigner seems to solve this problem.
	 * 
	 * */
	private static final String jarSigner_6 = "C:\\Program Files\\Java\\jdk1.6.0_45\\bin\\jarsigner.exe";
	private static final String jarSigner_7 = "C:\\Program Files\\Java\\jdk1.7.0_80\\bin\\jarsigner.exe";
	private static final String jarSigner_8 = "C:\\Program Files\\Java\\jdk1.8.0_131\\bin\\jarsigner.exe";
	
	private static final String keystorePath = "C:/workspace/android-studio/wenhaoc.keystore";
	private static final String keystoreName = "wenhaoc.keystore";
	private static final String KeyStoreKey = "isu_obad";
	
	public static void signAPK(File unsigned, File signed)
	{
		if (!unsigned.exists())
		{
			//TODO
			P.e("[jarsigner] can't find unsigned app " + unsigned.getAbsolutePath());
			P.pause();
		}
		
		signed.delete();
		
		try
		{
			String inPath = unsigned.getAbsolutePath();
			if (inPath.contains(" "))
				inPath = "\""+inPath+"\"";
			String outPath = signed.getAbsolutePath();
			if (outPath.contains(" "))
				outPath = "\""+outPath+"\"";
			
			String cmd = String.format("%s -keystore %s -signedjar %s %s %s", 
					jarSigner_7, keystorePath, outPath, inPath, keystoreName);
			
			Process pc = P.exec(cmd, false);
			OutputStream out = pc.getOutputStream();
			out.write((KeyStoreKey + "\n").getBytes());
			out.flush();
			pc.waitFor();
			
			if (signed.exists())
			{
				P.p("Signed APK: "+signed.getAbsolutePath());
				unsigned.delete();
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
