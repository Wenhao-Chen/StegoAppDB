package database.stego.apps.pixelknot;

import java.io.File;
import java.io.FileInputStream;

import ui.AndroidCommandCenter;
import util.P;

public class F5CoreExtract {


    private static byte[] deZigZag = {
            0, 1, 5, 6, 14, 15, 27, 28, 2, 4, 7, 13, 16, 26, 29, 42, 3, 8, 12, 17, 25, 30, 41, 43, 9, 11, 18, 24, 31,
            40, 44, 53, 10, 19, 23, 32, 39, 45, 52, 54, 20, 22, 33, 38, 46, 51, 55, 60, 21, 34, 37, 47, 50, 56, 59, 61,
            35, 36, 48, 49, 57, 58, 62, 63 };

    private int[] coefficients;
    private int permutation_index;
    private F5Random random;
    private Permutation permutation;

    private int statusWord, f5_k, data_length;

    public F5CoreExtract(String stego_path)
    {
        File stego = new File(stego_path);
        if (!stego.isFile() || !stego.getName().endsWith(".jpg"))
            return;
        try
        {
            byte[] carrier = new byte[(int)stego.length()];
            FileInputStream fis = new FileInputStream(stego);
            fis.read(carrier);
            fis.close();
            HuffmanDecode hufD = new HuffmanDecode(carrier);
            coefficients = hufD.decode();
            statusWord = -1;
            permutation_index = -1;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        	//P.p("bad " + stego.getAbsolutePath());
			//P.exec(AndroidCommandCenter.adbPath+" -s FA6BJ0305399 shell rm /sdcard/Download/StegoDB_March2019/stegos/PixelKnot/"+stego.getName(), true);
        }

    }

    public String extract(String password)
    {
        extractStatusWord(password); // RNG and index are initialized in here
        int code_n = (1 << f5_k) - 1;
        byte[] data = new byte[data_length];
        int index = 0;
        if (code_n > 1)
        {
            int extractedByte = 0, bitsExtracted = 0;
            extractingLoop: do
            {
                // 1. read n places, and calculate k bits
                int hash = 0;
                int code = 1;
                while (code <= code_n)
                {
                    if (permutation_index >= coefficients.length)
                        break extractingLoop;
                    int shuffledIndex = permutation.getShuffled(permutation_index++);
                    if (shuffledIndex % 64 == 0)
                        continue; // skip DC coefficients
                    shuffledIndex = shuffledIndex - shuffledIndex % 64 + deZigZag[shuffledIndex % 64];
                    int coeff = coefficients[shuffledIndex];
                    if (coeff == 0)
                        continue; // skip zeroes
                    int extractedBit = coeff>0? coeff&1 : 1-(coeff&1);
                    if (extractedBit == 1)
                        hash ^= code;
                    code++;
                }
                for (int i = 0; i < f5_k; i++)
                {
                    extractedByte |= (hash >> i & 1) << bitsExtracted++;
                    if (bitsExtracted==8)
                    {
                        extractedByte ^= random.getNextByte();
                        data[index++] = (byte)extractedByte;
                        extractedByte = bitsExtracted = 0;
                        if (index >= data.length)
                            break extractingLoop;
                    }
                }
            }
            while (true);
        }
        else
        {
            while (index < data.length && permutation_index < coefficients.length)
            {
                data[index++] = (byte) (extractBits(8)^random.getNextByte());
            }
        }
        if (index < data_length)
        {
            P.e("Incomplete file: only " + index + " of " + data.length + " bytes extracted.");
        }
        String message = new String(data);
        return message;
    }

    public void extractStatusWord(String password)
    {
        random = new F5Random(password.getBytes());
        permutation = new Permutation(coefficients.length, random);
        // 1. extract status word
        permutation_index = 0;
        statusWord = extractBits(32);
        //P.print("status word: " + Integer.toBinaryString(statusWord));
        statusWord ^= random.getNextByte();
        statusWord ^= random.getNextByte()<<8;
        statusWord ^= random.getNextByte()<<16;
        statusWord ^= random.getNextByte()<<24;
        //f5_k = (statusWord>>24) % 32;
        //NOTE: the value of k in the standard implementation is between 0 and 7 both inclusively,
        //      however it's possible to expand the bound with customizations
        f5_k = statusWord>>24;
        data_length = statusWord & 0x7fffff;
        //NOTE: the reserved bit: [23] is 0 in the standard implementation.
        //reserved_bit = (statusWord >> 23)&1;
    }

    private int extractBits(int length)
    {
        int result = 0;
        int availableBits = 0;
        while (availableBits < length && permutation_index < coefficients.length)
        {
            int shuffledIndex = permutation.getShuffled(permutation_index++);
            if (shuffledIndex%64==0)
                continue;
            shuffledIndex = shuffledIndex - shuffledIndex % 64 + deZigZag[shuffledIndex % 64];
            int coeff = coefficients[shuffledIndex];
            if (coeff==0)
                continue;
            int extractedBit = coeff>0? coeff&1 : 1-(coeff&1);
            result |= extractedBit << availableBits++;
        }
        if (permutation_index >= coefficients.length)
        {
            P.e("[F5CoreExtract] All coefficients have been visited, but payload extraction isn't done yet.");
        }
        return result;
    }
}