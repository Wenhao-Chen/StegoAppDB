package database.stego.apps.pixelknot;

import java.io.File;
import java.util.Base64;
import java.util.Map;

import database.stego.MessageDictionary;
import database.stego.StegoStats;
import util.P;

public class PixelKnot {

	public final static String PASSWORD_SENTINEL = "----* PK v 1.0 REQUIRES PASSWORD ----*";
	public static final String fullName = "PixelKnot";
	public static final String abbrName = "PK";
	
	
    public static boolean validate(File stegoF, File infoF) {
        Map<String, String> info = StegoStats.load(infoF);

        File dict = new File("E:/message_dictionary/"+info.get("Input Dictionary"));
        int startLine = Integer.parseInt(info.get("Dictionary Starting Line"));
        int inputlength = Integer.parseInt(info.get("Input Message Length"));
        String recordMessage = MessageDictionary.getMessage(dict, startLine, inputlength);
        String password = info.get("Password");
        String aesIV = info.get("AES IV");
        
        return validate(stegoF, recordMessage, password, aesIV);
    }
    
    // check if given jpg is a PixelKnot stego
    // password is needed for f5 seed
    // if we only care about detection, not extraction, then we just need to guess
    // the f5 seed, which is 1/3 of the full password
    public static boolean hasPixelKnotSignature(File stego, String fullPassword) {
    	
    	String f5seed = fullPassword.substring(fullPassword.length()/3*2);
    	F5CoreExtract f5e = new F5CoreExtract(stego.getAbsolutePath());
        String extracted = f5e.extract(f5seed);
    	
    	return extracted.startsWith(PASSWORD_SENTINEL);
    }
    
    // for validating older versions (any stegos generated before Jan 15 2021) of our PixelKnot stegos
    public static boolean validate_old(File stego, String recordedMessage, String recordedPassword, String aesIV)
    {
    	String aesPW = recordedPassword.substring(0, recordedPassword.length()/3);
        String aesSalt = recordedPassword.substring(recordedPassword.length()/3, recordedPassword.length()/3*2);
        String f5seed = recordedPassword.substring(recordedPassword.length()/3*2);

        F5CoreExtract f5e = new F5CoreExtract(stego.getAbsolutePath());
        
        // In older versions of our PixelKnot embedding script, we only embeded the ciphertext.
        String encrypted = f5e.extract(f5seed);
        
        byte[] message = Base64.getMimeDecoder().decode(encrypted);
        byte[] iv = Base64.getMimeDecoder().decode(aesIV);
        
        String extracted = Aes.DecryptWithPassword(aesPW, iv, message, aesSalt.getBytes());
        return extracted.equals(recordedMessage);
    }
    
    // for validating newer versions (Jan 15 2021 onward) of Pixelknot stegos
    public static boolean validate(File stego, String recordedMessage, String recordedPassword, String recordedAESIV)
    {
    	String aesPW = recordedPassword.substring(0, recordedPassword.length()/3);
        String aesSalt = recordedPassword.substring(recordedPassword.length()/3, recordedPassword.length()/3*2);
        String f5seed = recordedPassword.substring(recordedPassword.length()/3*2);

        F5CoreExtract f5e = new F5CoreExtract(stego.getAbsolutePath());
        
        // In newer versions of our PixelKnot embedding script, we embed the same way as the original PixelKnot:
        //    constant_string + IV + ciphertext
        String extracted = f5e.extract(f5seed);
        
        if (!extracted.startsWith(PASSWORD_SENTINEL)) {
        	P.p("No sentinel");
        	return false;
        }
        	
        
        String iv_and_ciphertext = extracted.substring(PASSWORD_SENTINEL.length());

        int idx = iv_and_ciphertext.indexOf("\n");
        
        String iv_raw = iv_and_ciphertext.substring(0, idx);
        if (!recordedAESIV.equals(iv_raw))
        	return false;
        byte[] iv = Base64.getMimeDecoder().decode(iv_raw);
        byte[] message = Base64.getMimeDecoder().decode(iv_and_ciphertext.substring(idx + 1));
        
        String extractedPlaintext = Aes.DecryptWithPassword(aesPW, iv, message, aesSalt.getBytes());
        return extractedPlaintext.equals(recordedMessage);
    }
    
    public static void main(String[] args) {
    	File root = new File("E:\\new_pixelknot_stegos");
    	for (File device : root.listFiles()) {
    		File pkDir = new File(device, "PixelKnot");
    		for (File stegoF : pkDir.listFiles())
        	{
        		String name = stegoF.getName();
        		if (!name.endsWith(".jpg") || name.endsWith("rate-00.jpg"))
        			continue;
        		File statsF = new File(pkDir, name.substring(0, name.length()-3)+"csv");
        		
        		boolean validate = validate(stegoF, statsF);
        		
        		Map<String, String> info = StegoStats.load(statsF);
        		String password = info.get("Password");
        		if (password == null)
        			P.p("PW null for "+stegoF.getAbsolutePath());
        		boolean sig = hasPixelKnotSignature(stegoF, password);
        		
        		if (!validate || !sig)
        			P.p(stegoF.getAbsolutePath() + "  "+validate+"  "+sig);
        	}
    	}
    	
    }
    
}
