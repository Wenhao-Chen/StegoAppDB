package database.stego.apps.passlok;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import database.objects.DBStego;
import database.stego.MessageDictionary;
import database.stego.StegoStats;
import ui.ProgressUI;
import util.F;
import util.P;

public class Passlok {

	public static void main(String[] arg)
	{
		String dir = "E:/stegodb_March2019/OnePlus5-1/stegos/Passlok";
		
		String[] stegos = {
				"OnePlus5-1_Scene-20190321-101425_JPG-00_I800_E33_o.jpg_s_PL_rate-05",
				"OnePlus5-1_Scene-20190321-102328_JPG-03_I500_E17_o.jpg_s_PL_rate-10",
				"OnePlus5-1_Scene-20190413-140618_JPG-09_I3000_E17_o.jpg_s_PL_rate-15"
				};
		
		long time = System.currentTimeMillis();
		for (int i = 0; i < stegos.length; i++)
		{
			File stegoF = new File(dir, stegos[i]+".jpg");
			File statsF = new File(dir, stegos[i]+".csv");
			boolean v = validate(stegoF, statsF);
			P.p(v+"");
		}
		P.p("time: " + (System.currentTimeMillis()-time)/1000);
	}
	
	public static final String fullName = "Passlok";
	public static final String abbrName = "PL";
	
	public static final int MinPasswordLength = 8;
	public static final int MaxPasswordLength = 16;
	
	public static ProgressUI ui;
	
	public static void makeStegos(File input, List<DBStego> appStegos)
	{
		if (ui != null)
			ui.newLine("Passlok embedding 5-25: " + input.getAbsolutePath());
		Passlok pl = new Passlok(input);
		if (ui != null)
			ui.newLine("  saving cover " + appStegos.get(0).stegoImage.getAbsolutePath());
		pl.jpeg.writeImage(pl.jpeg.original_coeffs, appStegos.get(0).stegoImage);
		
		String deviceName = input.getName().substring(0, input.getName().indexOf("_"));
        String inputPath = input.getAbsolutePath();
        inputPath = inputPath.substring(inputPath.indexOf(deviceName));
        String coverPath = appStegos.get(0).stegoImage.getAbsolutePath();
        coverPath = coverPath.substring(coverPath.indexOf(deviceName));
        
        for (int i = 1; i < appStegos.size(); i++)
        {
        	DBStego stego = appStegos.get(i);
        	
    		int messageLength = pl.getMessageLengthBytes(stego.embeddingRate);
    		MessageDictionary.InputMessage messageInfo = MessageDictionary.randomMessage(messageLength);
    		String password = randomBase64String(MessageDictionary.randomInt(MinPasswordLength, MaxPasswordLength));
    		
    		long time = System.currentTimeMillis();
            pl.embed(messageInfo.message, password, stego.stegoImage);
            time = System.currentTimeMillis()-time;
            
            StegoStats stats = new StegoStats();
            stats.inputImageName = inputPath;
            stats.coverImageName = coverPath;
            stats.stegoApp = fullName;
            stats.capacity = pl.capacity;
            stats.embedded = pl.embedded;
            stats.embeddingRate = (float)stats.embedded/(float)stats.capacity;
            stats.changed = pl.changed;
            stats.dictionary = messageInfo.dictName;
            stats.dictStartLine = messageInfo.lineIndex;
            stats.messageLength = messageLength;
            stats.password = password;
            stats.time = time;
            stats.saveToFile(stego.statsFile.getAbsolutePath());
        }
	}
	
	
	public static boolean validate(File stegoF, File statsF)
	{
		// 1. collect the coefficients files from the Passlok javascript
		File coeffDir = new File("F:/TEMP_Passlok_Coefficients");
		File[] coeffFiles = new File[3];
		boolean ready = true;
		for (int i = 1; i<=3; i++)
		{
			coeffFiles[i-1] = new File(coeffDir, stegoF.getName()+"_"+i+".txt");
			if (!coeffFiles[i-1].exists())
				ready = false;
		}
		if (!ready)
		{
			return false;
		}
		
		Map<String, String> info = StegoStats.load(statsF);
		File dict = new File("E:/message_dictionary/"+info.get("Input Dictionary"));
		int startLine = Integer.parseInt(info.get("Dictionary Starting Line"));
		int inputlength = Integer.parseInt(info.get("Input Message Length"));
		String password = info.get("Password");
		String recordMessage = MessageDictionary.getMessage(dict, startLine, inputlength);
		
		Passlok pl = new Passlok(coeffFiles);
		String extracted = pl.extract(password);
		
		boolean equals = recordMessage.equals(extracted);
		if (ui != null)
		{
			ui.newLine("extraction "+equals+" for " + stegoF.getAbsolutePath());
		}
		if (!equals)
		{
			P.p("Passlok validation failed for " +stegoF.getAbsolutePath());
			P.pause();
		}
		//P.p("extracted {\n"+extracted+"\n}");
		return equals;
	}
	
	
	private PasslokJPEGEncoder jpeg;
	int[] allCoeffs;		// allCoeffs is the 'rawCoefficients' from Passlok
	int[] nonZeroCoeffs;	// nonZeroCoeffs is the 'allCoefficients' variable from Passlok source
	int[] permutation, payload; 
	int count2, count3;
	private Isaac isaac;
	
	private int k, changed, embedded, capacity;
	
	Passlok(File[] coeffFiles)
	{
		allCoeffs = loadCoeffsFromFiles(coeffFiles);
		nonZeroCoeffs = removeZeros(allCoeffs);
	}
	
	Passlok(File input)
	{
		this.jpeg = new PasslokJPEGEncoder(input);
		capacity = -222;
		int length = jpeg.original_coeffs[0].length;
		for (int i = 0; i < 3; i++)
		{
			for (int j = 0; j < length; j++)
			{
				for (int k = 0; k < 64; k++)
				{
					int coeff = jpeg.original_coeffs[i][j][k];
					if (coeff != 0)
						capacity++;
				}
			}
		}
	}
	
	public int getMessageLengthBytes(float rate)
	{
		if (rate <= 0 || rate >= 1)
			return 0;
		
		int messageBits = (int)(capacity*rate-48-4); // 48 bits for appended EOF, 4 bits for K
		messageBits = (int)(Math.ceil(messageBits/24.0))*24; // nearest bit count that can be divided by 24
		
		return messageBits/8;
	}
	
	public String extract(String password)
	{
		String seed = password + nonZeroCoeffs.length+"jpeg";
		isaac = new Isaac(seed);
		
		shuffleNonZeroCoefficients();
		
		int length = nonZeroCoeffs.length-222;
		int[] kCode = new int[4];
		
		for (int i = 0; i < 4; i++)
			kCode[i] = stegParity(nonZeroCoeffs[i]);
		
		int k = binArray2decimal(kCode)+1;
		int n = (int)Math.pow(2, k)-1;
		int blocks = (int)Math.floor(length/n);
		P.p("passwrod = " + password);
		P.pf("k/n/blocks = %d/%d/%d", k, n, blocks);
		if (blocks==0)
			return "";
		
		int[] parityBlock = new int[n];
		int[] coverBlock = new int[n];
		int[] outputBin = new int[k*blocks];
		int hash;
		
		for (int i = 0; i < blocks; i++)
		{
			coverBlock = Arrays.copyOfRange(nonZeroCoeffs, 4+i*n, 4+(i*n)+n);
			for (int j = 0; j < n; j++)
				parityBlock[j] = stegParity(coverBlock[j]);
			hash = 0;
			for (int j = 1; j <=n; j++)
				hash ^= parityBlock[j-1]*j;
			for (int j = 0; j < k; j++)
				outputBin[i*k+k-1-j] = (hash>>j)&1;
		}
		
		addNoise(outputBin);
		
		boolean found = false;
		int fromEnd = 0;
		int outLength = outputBin.length;
		for (int j = 0; j < outLength-47; j++)
		{
			found = true;
			for (int l = 0; l < 48; l++)
				found = found && outLength-l-j<outLength && (imgEOF[47-l]==outputBin[outLength-l-j]);
			if (found)
			{
				fromEnd = j+47;
				break;
			}
		}
		if (!found)
			return "";
		
		outputBin = Arrays.copyOfRange(outputBin, 0, outputBin.length-fromEnd);
		
		return fromBin(outputBin);
	}
	
	
	public void embed(String message, String pw, File stegoF)
	{
		if (ui != null)
			ui.newLine("  embedding: " + stegoF.getAbsolutePath());
		
		String encodedMessage = b64EncodeUnicode(message).replaceAll("[=+$]", "");
		int[] messageBytes = toBin(encodedMessage);
		payload = concat(messageBytes, imgEOF);
		
		resetArraysAndTrimZeros();
		
		String seed = pw + nonZeroCoeffs.length+"jpeg";
		isaac = new Isaac(seed);
		
		shuffleNonZeroCoefficients();
		addNoise(payload);
		
		embed();
		unShuffleNonZeroCoefficients();
		addZerosBack();
		
		jpeg.writeImage(allCoeffs, stegoF);
	}
	
	private int embed()
	{
		return embed(0);
	}
	
	private int embed(int startIndex)
	{
		int length = nonZeroCoeffs.length-222;
		double rate = (double)payload.length/(double)length;
		k =2;
		if (payload.length>length)
		{
			P.e("Passlok payload length more than capacity!");
			return 0;
		}
		// calculate K
		while (k/(Math.pow(2,k)-1) > rate)
			k++;
		k--;
		if(k > 16) k = 16;
		int[] kCode = new int[4];
		for(int i = 0; i < 4; i++) 
			kCode[3-i] = (k-1 >> i) & 1;
		
		double y = (double)count3/(double)(count2+count3);
		int ones = 0, minusones=0;
		changed = 0;
		
		// embed kCode
		for(int i = 0; i < 4; i++)
		{
			if(nonZeroCoeffs[startIndex + i] > 0)
			{									//positive same as for png
				if(kCode[i] == 1 && stegParity(nonZeroCoeffs[startIndex + i]) == 0)			//even made odd by going down one
					setCoeff(startIndex+i, nonZeroCoeffs[startIndex + i]-1); //nonZeroCoeffs[startIndex + i]--;
				else if(kCode[i] == 0 && stegParity(nonZeroCoeffs[startIndex + i]) != 0)
				{		//odd made even by going down one, except if the value was 1, which is taken to -1
					if(nonZeroCoeffs[startIndex + i] != 1)
						setCoeff(startIndex+i, nonZeroCoeffs[startIndex + i]-1); //nonZeroCoeffs[startIndex + i]--;
					else
						setCoeff(startIndex+i, -1); //nonZeroCoeffs[startIndex + i]=-1;
				}
			}
			else
			{														//negative coefficients are encoded in reverse
				if(kCode[i] == 0 && stegParity(nonZeroCoeffs[startIndex + i]) != 0)		//"odd" made even by going up one
					setCoeff(startIndex+i, nonZeroCoeffs[startIndex + i]+1);  //nonZeroCoeffs[startIndex + i]++;
				else if(kCode[i] == 1 && stegParity(nonZeroCoeffs[startIndex + i]) == 0)
				{			//"even" made odd by going up one, except if the value was -1, which is taken to 1
					if(nonZeroCoeffs[startIndex + i] != -1)
						setCoeff(startIndex+i, nonZeroCoeffs[startIndex + i]+1); //nonZeroCoeffs[startIndex + i]++;
					else
						setCoeff(startIndex+i, 1); //nonZeroCoeffs[startIndex + i] = 1;
				}
			}
		}
		
		// embed the actual data
		int n = (int)Math.pow(2, k)-1;
		int blocks = (int)Math.ceil((double)payload.length/(double)k);
		int[] parityBlock = new int[n];
		int[] inputBlock = new int[k];
		int[] coverBlock = new int[n];
		int hash, inputNumber, outputNumber;
		
		if (payload.length%k!=0) // pad payload so that its length is a multiple of k
		{
			int[] pad = new int[k-payload.length%k];
			Arrays.fill(pad, 0);
			payload = concat(payload, pad);
		}
		
		//P.pf("k/n/blocks/payload = %d/%d/%d/%d", k, n, blocks, payload.length);
		for (int i = 0; i < blocks; i++)
		{
			inputBlock = Arrays.copyOfRange(payload, i*k, i*k+k);
			inputNumber = binArray2decimal(inputBlock);
			coverBlock = Arrays.copyOfRange(nonZeroCoeffs, 4+i*n, 4+(i*n)+n);
			for (int j = 0; j < n; j++)
				parityBlock[j] = stegParity(coverBlock[j]);
			
			hash = 0;
			for (int j = 1; j <=n; j++)
				hash = hash ^ (parityBlock[j-1]*j);
			outputNumber = inputNumber ^ hash;
			
			//P.pf("input/hash/output = %d/%d/%d", inputNumber, hash, outputNumber);
			
			if (outputNumber!=0)
			{
				int index = startIndex + 3+i*n+outputNumber;
				if(coverBlock[outputNumber-1] > 0)
				{			//positive, so change by going down (normally); if 1 or -1, switch to the other
					if(coverBlock[outputNumber-1] == 1)
					{
						//whether to go up or down determined by whether there are too few or too many 1's and -1's
						if(minusones <= 0)
						{
							setCoeff(index, -1); //nonZeroCoeffs[startIndex + 3+i*n+outputNumber]=-1; 
							ones--; 
							minusones++;
						}
						else
						{
							setCoeff(index, 2); //nonZeroCoeffs[startIndex + 3+i*n+outputNumber]=2; 
							ones--;
						}
					}
					else if(coverBlock[outputNumber-1] == 2)
					{
						if(ones <= 0)
						{	
							setCoeff(index, nonZeroCoeffs[index]-1); //nonZeroCoeffs[startIndex + 3+i*n+outputNumber]--; 
							ones++;
						}
						else
						{
							setCoeff(index, nonZeroCoeffs[index]+1); //nonZeroCoeffs[startIndex + 3+i*n+outputNumber]++;
						}
					}
					else
					{
						if(Math.random() > y)
							setCoeff(index, nonZeroCoeffs[index]-1); //nonZeroCoeffs[startIndex + 3+i*n+outputNumber]--;
						else
							setCoeff(index, nonZeroCoeffs[index]+1); //nonZeroCoeffs[startIndex + 3+i*n+outputNumber]++;
					}
				}
				else if(coverBlock[outputNumber-1] < 0)
				{	//negative, so change by going up
					if(coverBlock[outputNumber-1] == -1)
					{
						if(ones <= 0)
						{
							setCoeff(index, 1); //nonZeroCoeffs[index] = 1; 
							minusones--; 
							ones++;
						}
						else
						{
							setCoeff(index, -2); //nonZeroCoeffs[index] = -2; 
							minusones--;
						}
					}
					else if(coverBlock[outputNumber-1] == -2)
					{
						if(minusones <= 0)
						{
							setCoeff(index, nonZeroCoeffs[index]+1); //nonZeroCoeffs[index]++; 
							minusones++;
						}
						else
						{
							setCoeff(index, nonZeroCoeffs[index]-1); //nonZeroCoeffs[index]--;
						}
					}
					else
					{
						if(Math.random() > y)
							setCoeff(index, nonZeroCoeffs[index]+1); //nonZeroCoeffs[index]++;
						else
							setCoeff(index, nonZeroCoeffs[index]-1); //nonZeroCoeffs[index]--;
					}
				}
			}
		}
		embedded = payload.length;
		return startIndex + blocks*n+3;
	}

	private void setCoeff(int index, int newVal)
	{
		if (nonZeroCoeffs[index] != newVal)
			changed++;
		embedded++;
		nonZeroCoeffs[index] = newVal;
	}
	private void resetArraysAndTrimZeros()
	{
		int length = jpeg.original_coeffs[0].length;
		allCoeffs = new int[length*3*64]; // 3D array into 1D
		List<Integer> nonZeros = new ArrayList<>();	  // trimmed 1D array without 0s
		count2 = count3 = 0;	  // occurrence of 2, -2, 3, -3s
		for (int i = 0; i < 3; i++)
		{
			for (int j = 0; j < length; j++)
			{
				for (int k = 0; k < 64; k++)
				{
					int coeff = jpeg.original_coeffs[i][j][k];
					allCoeffs[i*length*64+j*64+k] = coeff;
					
					if (coeff != 0)
					{
						nonZeros.add(coeff);
						if (nonZeros.size()>4)
						{
							if (coeff==2 || coeff==-2)
								count2++;
							else if (coeff==3 || coeff==-3)
								count3++;
						}
					}
				}
			}
		}
		nonZeroCoeffs = new int[nonZeros.size()];
		for (int i = 0; i < nonZeroCoeffs.length; i++)
			nonZeroCoeffs[i] = nonZeros.get(i);
	}
	private void addZerosBack()
	{
		int j = 0;
		for (int i = 0; i < allCoeffs.length; i++)
		{
			if (allCoeffs[i] != 0)
			{
				allCoeffs[i] = nonZeroCoeffs[j];
				j++;
			}
		}
	}
	private static int[] concat(int[] arr1, int[] arr2)
	{
		int[] res = new int[arr1.length+arr2.length];
		System.arraycopy(arr1, 0, res, 0, arr1.length);
		System.arraycopy(arr2, 0, res, arr1.length, arr2.length);
		return res;
	}
	private void addNoise(int[] array)
	{
		int length = array.length;
		for(int i = 0; i < length; i++){
			array[i] = array[i] ^ ((isaac.rand() >= 0)?1:0);			//here are the call to the integer version of the isaac PRNG, and the XOR operation
		}
	}

	private void shuffleNonZeroCoefficients()
	{
		shuffleCoefficients(0);
	}
	private void shuffleCoefficients(int startIndex)
	{
		int length = nonZeroCoeffs.length;
		int[] permutedCoeffs = new int[length];
		permutation = randPerm(length-startIndex);
		for(int i = 0; i < length - startIndex; i++)
			permutedCoeffs[i] = nonZeroCoeffs[startIndex + permutation[i]];
		
		for(int i = 0; i < length - startIndex; i++)
			nonZeroCoeffs[startIndex + i] = permutedCoeffs[i];
	}
	private void unShuffleNonZeroCoefficients()
	{
		unShuffleCoefficients(0);
	}
	private void unShuffleCoefficients(int startIndex)
	{
		int	length = nonZeroCoeffs.length;
		int[] permutedCoeffs = new int[length];
		int index;
		int[] inversePermutation = new int[length-startIndex];

		for(int i = 0; i < length - startIndex; i++){	
			index = permutation[i];
			inversePermutation[index] = i;
		}		

		for(int i = 0; i < length - startIndex; i++)
			permutedCoeffs[i] = nonZeroCoeffs[startIndex + inversePermutation[i]];
		
		for(int i = 0; i < length - startIndex; i++)
			nonZeroCoeffs[startIndex + i] =  permutedCoeffs[i];
	}
	private int[] randPerm(int n)
	{
		int[] result = new int[n];
		result[0] = 0;
		
		for(int i = 1; i < n; ++i)
		{
			int idx = (int)(isaac.random() * (i + 1)) | 0;			//here is the call to the isaac PRNG library, floating point version
			if(idx < i)
				result[i] = result[idx];
			result[idx] = i;
		}
		return result;
	}
	private static final int[] imgEOF = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1};	
	private static final String base64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
	@SuppressWarnings("serial")
	private static Map<Character, Integer> base64Map = new HashMap<Character, Integer>(){{
		for (int i = 0; i < base64.length(); i++)
			put(base64.charAt(i), i);
	}};
	private static int[] toBin(String input)
	{
		int[] output = new int[input.length() * 6];
		
		int index = 0;
	    for(int i = 0; i < input.length(); i++)
	    {
			int val = base64Map.get(input.charAt(i));
			for (int j = 5; j>=0; j--)
			{
				output[index++] = (val>>>j)&1;
			}
	    }
		return output;
	}
	private static String fromBin(int[] input)
	{
		int length = input.length-(input.length%6);
		byte[] output = new byte[length/6];
		int index = 0;
		
		for (int i = 0; i < length; i+=6)
		{
			index = 0;
			for (int j = 0; j < 6; j++)
			{
				index = 2*index+input[i+j];
			}
			output[i/6] = (byte)base64.charAt(index);
		}
		byte[] bytes = Base64.getDecoder().decode(output);
		return new String(bytes);
	}
	private static int[] removeZeros(int[] arr)
	{
		int length = arr.length, nonZeros = 0;
		 for (int i = 0; i < length; i++)
			 if (arr[i]!=0)
				 nonZeros++;
		int[] res = new int[nonZeros];
		int j = 0;
		for (int i = 0; i < length; i++)
			if (arr[i]!=0)
				res[j++] = arr[i];
		return res;
	}
	private static String randomBase64String(int length)
	{
		String result = "";
		for (int i = 0; i < length; i++)
		{
			int pos = (int)Math.floor(Math.random()*base64.length());
			result += base64.substring(pos, pos+1);
		}
		return result;
	}
	private static int stegParity(int number)
	{
		if(number >= 0)
			return number % 2;
		else
			return -(number-1) % 2;
	}
	private static int binArray2decimal(int[] array)
	{
		int output = 0, mult = 1;
		for (int i = 0; i < array.length; i++)
		{
			output += array[array.length-1-i]*mult;
			mult *= 2;
		}
		return output;
	}
	private static String b64EncodeUnicode(String s)
	{
		return Base64.getEncoder().encodeToString(s.getBytes());
	}
	private static int[] loadCoeffsFromFiles(File[] coeffFiles)
	{
		int[] allCoeffs = null;
		int index = 0;
		for (int x = 0; x < 3; x++)
		{
			File f = coeffFiles[x];
			List<String> lines = F.readLinesWithoutEmptyLines(f);
			P.p(lines.size()+"");
			if (x==0)
			{
				allCoeffs = new int[3*lines.size()*64];
			}
			
			for (String line : lines)
			{
				String[] parts = line.split(",");
				if (parts.length!=64)
				{
					P.p("Coeff file line not 64: " + f.getAbsolutePath());
					P.pause();
				}
				for (int i = 0; i < 64; i++)
				{
					allCoeffs[index++] = Integer.parseInt(parts[i]);
				}
			}
		}
		return allCoeffs;
	}
}
