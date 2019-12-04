package stego.apps.pixelknot;

import java.io.UnsupportedEncodingException;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class Aes {
	
	static {
	    Security.addProvider(new BouncyCastleProvider());
	}
	
    public static String DecryptWithPassword(String password, byte[] iv, byte[] message, byte[] salt) {
        String new_message = null;

        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, 256);
            SecretKey tmp = factory.generateSecret(spec);
            SecretKey secret_key = new SecretKeySpec(tmp.getEncoded(), "AES");

            
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "BC");
            cipher.init(Cipher.DECRYPT_MODE, secret_key, new IvParameterSpec(iv));

            new_message = new String(cipher.doFinal(message));

        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
			e.printStackTrace();
		}
        return new_message;
    }


    public static String[] encryptWithPassword(String password, String message, String salt)
    {
        Map.Entry<String, String> entry = EncryptWithPassword(password, message, salt.getBytes()).entrySet().iterator().next();
        return new String[] {entry.getKey(), entry.getValue()};
    }

    public static Map<String, String> EncryptWithPassword(String password, String message, byte[] salt) {
        Map<String, String> pack = null;
        String new_message = null;

        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, 256);
            SecretKey tmp = factory.generateSecret(spec);
            SecretKey secret_key = new SecretKeySpec(tmp.getEncoded(), "AES");

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

            // TODO: follow up (https://android-developers.blogspot.com/2013/08/some-securerandom-thoughts.html)
            cipher.init(Cipher.ENCRYPT_MODE, secret_key);


            //NOTE (Wenhao): AlgorithmParameters.getParameterSpec(IVParameterSpec.class) starts
            // throwing InvalidParameterSpecException on Android 8.1.
            // It works fine with Android 8.0 and below
            byte[] iv_bytes = null;
            try
            {
                AlgorithmParameters params = cipher.getParameters();
                iv_bytes = params.getParameterSpec(IvParameterSpec.class).getIV();
            }
            catch (InvalidParameterSpecException e)
            {
                iv_bytes = cipher.getIV();
            }

            String iv = Base64.getEncoder().encodeToString(iv_bytes);

            new_message = Base64.getEncoder().encodeToString(cipher.doFinal(message.getBytes("UTF-8")));

            pack = new HashMap<String, String>();
            pack.put(iv, new_message);
        } catch (IllegalBlockSizeException e) {

            e.printStackTrace();
        } catch (BadPaddingException e) {

            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {

            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {

            e.printStackTrace();
        } catch (InvalidKeySpecException e) {

            e.printStackTrace();
        } catch (NoSuchPaddingException e) {

            e.printStackTrace();
        } catch (InvalidKeyException e) {

            e.printStackTrace();
        }

        return pack;
    }
}
