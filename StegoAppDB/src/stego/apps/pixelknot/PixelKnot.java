package stego.apps.pixelknot;

import java.io.File;
import java.util.Base64;
import java.util.Map;

import stego.MessageDictionary;
import stego.StegoStats;
import ui.AndroidCommandCenter;
import util.P;

public class PixelKnot {

	
	public static final String fullName = "PixelKnot";
	public static final String abbrName = "PK";
	
    public static boolean validate(File stegoF, File infoF)
    {
        Map<String, String> info = StegoStats.load(infoF);

        File dict = new File("E:/message_dictionary/"+info.get("Input Dictionary"));
        int startLine = Integer.parseInt(info.get("Dictionary Starting Line"));
        int inputlength = Integer.parseInt(info.get("Input Message Length"));
        String recordMessage = MessageDictionary.getMessage(dict, startLine, inputlength);
        String password = info.get("Password");
        String aesIV = info.get("AES IV");
        
        return validate(stegoF, recordMessage, password, aesIV);
    }
    
    public static boolean validate(File stego, String recordedMessage, String recordedPassword, String aesIV)
    {
    	String aesPW = recordedPassword.substring(0, recordedPassword.length()/3);
        String aesSalt = recordedPassword.substring(recordedPassword.length()/3, recordedPassword.length()/3*2);
        String f5seed = recordedPassword.substring(recordedPassword.length()/3*2);

        F5CoreExtract f5e = new F5CoreExtract(stego.getAbsolutePath());
        String encrypted = f5e.extract(f5seed);
        
        byte[] message = Base64.getMimeDecoder().decode(encrypted);
        byte[] iv = Base64.getMimeDecoder().decode(aesIV);

        String extracted = Aes.DecryptWithPassword(aesPW, iv, message, aesSalt.getBytes());
        return extracted.equals(recordedMessage);
    }
    
    public static void main(String[] args)
    {
    	File dir = new File("E:/stegodb_March2019/Pixel1-2/stegos/PixelKnot");
    	for (File stegoF : dir.listFiles())
    	{
    		String name = stegoF.getName();
    		if (!name.endsWith(".jpg") || name.endsWith("rate-00.jpg"))
    			continue;
    		File statsF = new File(dir, name.substring(0, name.length()-3)+"csv");
    		
    		P.p(stegoF.getAbsolutePath());
    		validate(stegoF, statsF);
    		
    	}
    }
    
}
