package app_analysis;

public class EntryPoints {
	
	
	public static String getStegoMethodSignature(String packageName)
	{
		switch (packageName)
		{
			case "stega.jj.bldg5.steganography":
				return "Lstega/jj/bldg5/steganography/Encode;->encoded(Landroid/graphics/Bitmap;Ljava/lang/String;)Landroid/graphics/Bitmap;";
			case "ca.repl.free.camopic":   // first bitmap is cover, second bitmap is payload
				return "Lca/repl/free/camopic/ac;->a(Lca/repl/free/camopic/ac;Landroid/graphics/Bitmap;Landroid/graphics/Bitmap;)Ljava/io/File;";
			case "com.steganochipher.stegano":
				return "Lcom/steganocipher/stegano/EncodeActivity;->embedd()Landroid/net/Uri;";
			case "com.akseltorgard.steganography":
				return "Lcom/akseltorgard/steganography/async/EncodeTask;->execute(Lcom/akseltorgard/steganography/async/SteganographyParams;)Lcom/akseltorgard/steganography/async/SteganographyParams;";
			case "com.dinaga.photosecret":
				return "Lcom/dinaga/photosecret/helpers/Steganography;->encode(Landroid/graphics/Bitmap;Ljava/lang/String;Ljava/lang/String;)Landroid/graphics/Bitmap;";
			case "com.fruiz500.passlok": // skipping this one because it uses javascript
				return "";
			case "com.meznik.Steganography":
				return "Lcom/meznik/Steganography/a/d;->a(Landroid/graphics/Bitmap;Ljava/lang/String;Ljava/lang/String;)Landroid/graphics/Bitmap;";
			case "com.romancinkais.stegais": // this one uses native
				return "";
			case "com.talixa.pocketstego":
				return "Lcom/talixa/pocketstego/helpers/StegoEncoder;->createStegoImage(Landroid/graphics/Bitmap;Ljava/lang/String;Ljava/lang/String;Lcom/talixa/pocketstego/helpers/StegoOptions;)V";
			case "com.talpro213.steganographyimage":
				return "LAlgoStego/StegoPVD;->stego(Landroid/graphics/Bitmap;Ljava/lang/String;Ljava/lang/Boolean;)Ljava/lang/Object;";
			case "com.nitramite.cryptography":
				return "Lcom/nitramite/libraries/steganography/BitmapEncoder;->encode(Landroid/graphics/Bitmap;[B)Landroid/graphics/Bitmap;";
			case "dev.nia.niastego":
				return "Ldev/nia/niastego/shield/LeastSignificantBit;->embed([BLandroid/graphics/Bitmap;)Landroid/graphics/Bitmap;";
			case "it.mobistego":
				return "Lit/mobistego/business/LSB2bit;->encodeMessage([IIILit/mobistego/business/LSB2bit$MessageEncodingStatus;Lit/mobistego/business/LSB2bit$ProgressHandler;)[B";
			case "jubatus.android.davinci":
				return "Ljubatus/android/davinci/HideActivity;->run()V";
			case "it.kryfto":
				return "Lit/mobistego/alg/LSB2bit;->encodeMessage([IIILjava/lang/String;Lit/mobistego/handler/ProgressHandler;)[B";
			case "com.nsoeaung.photomessenger":
				return "Lcom/nsoeaung/photomessenger/at;->a([Ljava/lang/String;)Ljava/lang/String;";
			case "info.guardianproject.pixelknot":
				return "Linfo/guardianproject/pixelknot/StegoEncryptionJob$3;->run()V";
			case "com.paranoiaworks.unicus.android.sse":
				return "Lcom/paranoiaworks/unicus/android/sse/utils/Encryptor;->exportTextToSteganogram(Ljava/lang/String;Lcom/paranoiaworks/unicus/android/sse/misc/CryptFileWrapper;Ljava/io/File;ID)V";
			case "bearapps.com.steganography":
				return "Lbearapps/com/steganography/Encode;->encode()V";
			case "hu.appz4.stegoformessenger":
				return "Lhu/appz4/stegoformessenger/MainActivity;->j()V";
			case "com.artbeatte.stegosaurus":
				return "Lcom/artbeatte/stegosaurus/StegoUtils$1;->doInBackground([Ljava/lang/Void;)Ljava/lang/Boolean;";
			case "sk.panacom.stegos": // native
				return "";
		}
		return "";
	}

}
