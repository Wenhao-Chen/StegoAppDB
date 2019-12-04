package stego.apps.passlok;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import util.Images;

public class PasslokJPEGEncoder {

	static final int defaultQuality = 90;
	static int[] ZigZag = {
			 0, 1, 5, 6,14,15,27,28,
			 2, 4, 7,13,16,26,29,42,
			 3, 8,12,17,25,30,41,43,
			 9,11,18,24,31,40,44,53,
			10,19,23,32,39,45,52,54,
			20,22,33,38,46,51,55,60,
			21,34,37,47,50,56,59,61,
			35,36,48,49,57,58,62,63
	};
	static int[] std_dc_luminance_nrcodes = {0,0,1,5,1,1,1,1,1,1,0,0,0,0,0,0,0};
	static int[] std_dc_luminance_values = {0,1,2,3,4,5,6,7,8,9,10,11};
	static int[] std_ac_luminance_nrcodes = {0,0,2,1,3,3,2,4,3,5,5,4,4,0,0,1,0x7d};
	static int[] std_ac_luminance_values = {
			0x01,0x02,0x03,0x00,0x04,0x11,0x05,0x12,
			0x21,0x31,0x41,0x06,0x13,0x51,0x61,0x07,
			0x22,0x71,0x14,0x32,0x81,0x91,0xa1,0x08,
			0x23,0x42,0xb1,0xc1,0x15,0x52,0xd1,0xf0,
			0x24,0x33,0x62,0x72,0x82,0x09,0x0a,0x16,
			0x17,0x18,0x19,0x1a,0x25,0x26,0x27,0x28,
			0x29,0x2a,0x34,0x35,0x36,0x37,0x38,0x39,
			0x3a,0x43,0x44,0x45,0x46,0x47,0x48,0x49,
			0x4a,0x53,0x54,0x55,0x56,0x57,0x58,0x59,
			0x5a,0x63,0x64,0x65,0x66,0x67,0x68,0x69,
			0x6a,0x73,0x74,0x75,0x76,0x77,0x78,0x79,
			0x7a,0x83,0x84,0x85,0x86,0x87,0x88,0x89,
			0x8a,0x92,0x93,0x94,0x95,0x96,0x97,0x98,
			0x99,0x9a,0xa2,0xa3,0xa4,0xa5,0xa6,0xa7,
			0xa8,0xa9,0xaa,0xb2,0xb3,0xb4,0xb5,0xb6,
			0xb7,0xb8,0xb9,0xba,0xc2,0xc3,0xc4,0xc5,
			0xc6,0xc7,0xc8,0xc9,0xca,0xd2,0xd3,0xd4,
			0xd5,0xd6,0xd7,0xd8,0xd9,0xda,0xe1,0xe2,
			0xe3,0xe4,0xe5,0xe6,0xe7,0xe8,0xe9,0xea,
			0xf1,0xf2,0xf3,0xf4,0xf5,0xf6,0xf7,0xf8,
			0xf9,0xfa
	};
	static int[] std_dc_chrominance_nrcodes = {0,0,3,1,1,1,1,1,1,1,1,1,0,0,0,0,0};
	static int[] std_dc_chrominance_values = {0,1,2,3,4,5,6,7,8,9,10,11};
	static int[] std_ac_chrominance_nrcodes = {0,0,2,1,2,4,4,3,4,7,5,4,4,0,1,2,0x77};
	static int[] std_ac_chrominance_values = {
			0x00,0x01,0x02,0x03,0x11,0x04,0x05,0x21,
			0x31,0x06,0x12,0x41,0x51,0x07,0x61,0x71,
			0x13,0x22,0x32,0x81,0x08,0x14,0x42,0x91,
			0xa1,0xb1,0xc1,0x09,0x23,0x33,0x52,0xf0,
			0x15,0x62,0x72,0xd1,0x0a,0x16,0x24,0x34,
			0xe1,0x25,0xf1,0x17,0x18,0x19,0x1a,0x26,
			0x27,0x28,0x29,0x2a,0x35,0x36,0x37,0x38,
			0x39,0x3a,0x43,0x44,0x45,0x46,0x47,0x48,
			0x49,0x4a,0x53,0x54,0x55,0x56,0x57,0x58,
			0x59,0x5a,0x63,0x64,0x65,0x66,0x67,0x68,
			0x69,0x6a,0x73,0x74,0x75,0x76,0x77,0x78,
			0x79,0x7a,0x82,0x83,0x84,0x85,0x86,0x87,
			0x88,0x89,0x8a,0x92,0x93,0x94,0x95,0x96,
			0x97,0x98,0x99,0x9a,0xa2,0xa3,0xa4,0xa5,
			0xa6,0xa7,0xa8,0xa9,0xaa,0xb2,0xb3,0xb4,
			0xb5,0xb6,0xb7,0xb8,0xb9,0xba,0xc2,0xc3,
			0xc4,0xc5,0xc6,0xc7,0xc8,0xc9,0xca,0xd2,
			0xd3,0xd4,0xd5,0xd6,0xd7,0xd8,0xd9,0xda,
			0xe2,0xe3,0xe4,0xe5,0xe6,0xe7,0xe8,0xe9,
			0xea,0xf2,0xf3,0xf4,0xf5,0xf6,0xf7,0xf8,
			0xf9,0xfa
	};
	static int[] YQT = {
			16, 11, 10, 16, 24, 40, 51, 61,
			12, 12, 14, 19, 26, 58, 60, 55,
			14, 13, 16, 24, 40, 57, 69, 56,
			14, 17, 22, 29, 51, 87, 80, 62,
			18, 22, 37, 56, 68,109,103, 77,
			24, 35, 55, 64, 81,104,113, 92,
			49, 64, 78, 87,103,121,120,101,
			72, 92, 95, 98,112,100,103, 99
		};
	static int[] UVQT = {
			17, 18, 24, 47, 99, 99, 99, 99,
			18, 21, 26, 66, 99, 99, 99, 99,
			24, 26, 56, 99, 99, 99, 99, 99,
			47, 66, 99, 99, 99, 99, 99, 99,
			99, 99, 99, 99, 99, 99, 99, 99,
			99, 99, 99, 99, 99, 99, 99, 99,
			99, 99, 99, 99, 99, 99, 99, 99,
			99, 99, 99, 99, 99, 99, 99, 99
		};
	static double[] aasf = {
			1.0, 1.387039845, 1.306562965, 1.175875602,
			1.0, 0.785694958, 0.541196100, 0.275899379
		};
	
	private int width, height;
	private int[] YTable = new int[64];
	private int[] UVTable = new int[64];
	private double[] fdtbl_Y = new double[64];
	private double[] fdtbl_UV = new double[64];
	private int[][] YDC_HT, UVDC_HT, YAC_HT, UVAC_HT;
	private int[] category = new int[65535];
	private int[][] bitcode = new int[65535][2];
	private int[] DU = new int[64];
	private int[] RGB_YUV_TABLE = new int[2048];
	private int bytenew, bytepos;
	
	protected int[][][] original_coeffs;
	
	public PasslokJPEGEncoder(File f)
	{
		this(Images.loadImage(f));
	}
	
	PasslokJPEGEncoder(BufferedImage image)
	{
		this(image, defaultQuality);
	}
	PasslokJPEGEncoder(BufferedImage image, int quality)
	{
		//long time = System.currentTimeMillis();
		width = image.getWidth();
		height = image.getHeight();
		initHuffmanTbl();
		initCategoryNumber();
		initRGBYUVTable();
		setQuality(quality);
		initCoeffs(getImageData(image));
		//P.p("    JPEG coefficients initialization time: " + (System.currentTimeMillis()-time)+" ms");
	}
	
	public void writeImage(int[] allCoeffs, File outF)
	{
		int length = original_coeffs[0].length;
		int[][][] coeffs = new int[3][length][64];
		for(int index = 0; index < 3; index++)
		{
			for (int i = 0; i < length; i++)
			{
				for (int j = 0; j < 64; j++)
				{
					coeffs[index][i][j] = allCoeffs[index*length*64 + i*64 + j];
				}
			}
		}
		writeImage(coeffs, outF);
	}
	
	public void writeImage(int[][][] coeffs, File outF)
	{
		try
		{
			BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(outF));
			writeHeaders(out);
			writeCompressedData(coeffs, out);
			writeEOI(out);
			out.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private void writeHeaders(BufferedOutputStream out)
	{
        // SOI
		writeWord(0xffd8, out);
        
        // APP0
        final byte[] JFIF = {
        		(byte)0xff, (byte)0xe0, // APP0 marker
        		(byte)0, (byte)0x10,	// length: 16
        		(byte)0x4a,(byte)0x46,(byte)0x49,(byte)0x46, // JFIF
        		(byte)0,
        		(byte)1, (byte)1, // version numbers
        		(byte)0, // xyunits??
        		(byte)0, (byte)1, // x density
        		(byte)0, (byte)1, // y density
        		(byte)0, (byte)0 // thumbs width, height
        };
        writeArray(JFIF, out);
        
        // DQT
        final byte DQT[] = new byte[134];
        DQT[0] = (byte)0xff; DQT[1] = (byte)0xdb; // DQT marker
        DQT[2] = (byte)0; DQT[3] = (byte)0x84; // length: 132
        DQT[4] = (byte)0;
        for (int i = 5; i < 69; i++)
        	DQT[i] = (byte)YTable[i-5];
        DQT[69] = (byte)1;
        for (int i = 70; i < 134; i++)
        	DQT[i] = (byte)UVTable[i-70];
        writeArray(DQT, out);
        
        // SOF0
        final byte SOF[] = {
        	(byte)0xff, (byte)0xc0, // SOF marker
        	(byte)0, (byte)0x11, // length: 17
        	(byte)8, // precision
        	(byte)(height>>8 & 0xff), (byte)(height & 0xff), // image rows
        	(byte)(width>>8 & 0xff), (byte)(width & 0xff),   // image cols
        	(byte)3, 	// number of components
        	(byte)1, 	// component id (Y)
        	(byte)0x11, 	// sample factor
        	(byte)0, 		// q table number
        	(byte)2, 	// component id (U)
        	(byte)0x11, 	// sample factor
        	(byte)1, 		// q table number
        	(byte)3, 	// component id (V)
        	(byte)0x11, 	// sample factor
        	(byte)1, 		// q table number
        };
        writeArray(SOF, out);
        
        // DHT
        writeWord(0xFFC4, out);  // DHT marker
        writeWord(0x01A2, out);  // length
        writeByte((byte)0, out); // HTYDCinfo
		for (int i=0; i<16; i++) 
			writeByte((byte)std_dc_luminance_nrcodes[i+1], out);
		for (int j=0; j<=11; j++) 
			writeByte((byte)std_dc_luminance_values[j], out);
		writeByte((byte)0x10, out); // HTYACinfo
		for (int k=0; k<16; k++) 
			writeByte((byte)std_ac_luminance_nrcodes[k+1], out);
		for (int l=0; l<=161; l++) 
			writeByte((byte)std_ac_luminance_values[l], out);
		writeByte((byte)1, out); // HTUDCinfo
		for (int m=0; m<16; m++) 
			writeByte((byte)std_dc_chrominance_nrcodes[m+1], out);
		for (int n=0; n<=11; n++) 
			writeByte((byte)std_dc_chrominance_values[n], out);
		writeByte((byte)0x11, out); // HTUACinfo
		for (int o=0; o<16; o++)
			writeByte((byte)std_ac_chrominance_nrcodes[o+1], out);
		for (int p=0; p<=161; p++) 
			writeByte((byte)std_ac_chrominance_values[p], out);
		
		// SOS
		writeWord(0xFFDA, out); // SOS marker
		writeWord(12, out); // length: 12
		writeByte((byte)3, out); 	// number of components
		writeByte((byte)1, out); 	// component ID (Y)
		writeByte((byte)0, out); 		// HTY
		writeByte((byte)2, out); 	// component ID (U)
		writeByte((byte)0x11, out); 	// HTU
		writeByte((byte)3, out); 	// component ID (V)
		writeByte((byte)0x11, out); 	// HTV
		writeByte((byte)0, out); 	// Ss
		writeByte((byte)0x3f, out); // Se
		writeByte((byte)0, out); 	// Bf
	}
	
	private void writeCompressedData(int[][][] coeffs, BufferedOutputStream out)
	{
		int DCY=0, DCU=0, DCV=0;
		bytenew=0;
		bytepos=7;
		
		for (int i = 0; i < coeffs[0].length; i++){
			DCY = processDU(coeffs[0][i], DCY, YDC_HT, YAC_HT, out);
			DCU = processDU(coeffs[1][i], DCU, UVDC_HT, UVAC_HT, out);
			DCV = processDU(coeffs[2][i], DCV, UVDC_HT, UVAC_HT, out);
		}
		
		if (bytepos >= 0) {
			int[] fillbits = new int[2];
			fillbits[1] = bytepos+1;
			fillbits[0] = (1<<(bytepos+1))-1;
			writeBits(fillbits, out);
		}
	}
	
	private void writeEOI(BufferedOutputStream out)
	{
		writeWord(0xffd9, out);
	}
	
	void writeArray(final byte[] data, final BufferedOutputStream out) {
        try {
            out.write(data);
        } catch (final IOException e) {
            System.out.println("IO Error: " + e.getMessage());
        }
    }
	
	void writeWord(int num, final BufferedOutputStream out)
	{
		writeByte((byte)(num>>8 & 0xff), out);
		writeByte((byte)(num & 0xff), out);
	}
	
	void writeByte(byte b, final BufferedOutputStream out)
	{
		try {
			out.write(b);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	void writeBits(int[] bs, final BufferedOutputStream out)
	{
		int value = bs[0];
		int posval = bs[1]-1;
		while ( posval >= 0 ) {
			if ((value & (1 << posval)) != 0) {
				bytenew |= (1 << bytepos);
			}
			posval--;
			bytepos--;
			if (bytepos < 0) {
				if (bytenew == 0xFF) {
					writeByte((byte)0xFF, out);
					writeByte((byte)0, out);
				}
				else {
					writeByte((byte)bytenew, out);
				}
				bytepos=7;
				bytenew=0;
			}
		}
	}
	
	int processDU(int[] DU_DCT, int DC, int[][] HTDC, int[][] HTAC, final BufferedOutputStream out)
	{
		int[] EOB = HTAC[0x00];
		int[] M16zeroes = HTAC[0xF0];
		int pos;
		int I16 = 16;
		int I63 = 63;
		int I64 = 64;

		//ZigZag reorder
		for (int j=0;j<I64;++j) {
			DU[ZigZag[j]]=DU_DCT[j];
		}
		int Diff = DU[0] - DC; DC = DU[0];
		//Encode DC
		if (Diff==0) {
			writeBits(HTDC[0], out); // Diff might be 0
		} else {
			pos = 32767+Diff;
			writeBits(HTDC[category[pos]], out);
			writeBits(bitcode[pos], out);
		}
		//Encode ACs
		int end0pos = 63; // was const... which is crazy
		for (; (end0pos>0)&&(DU[end0pos]==0); end0pos--) {};
		//end0pos = first element in reverse order !=0
		if ( end0pos == 0) {
			writeBits(EOB, out);
			return DC;
		}
		int i = 1;
		int lng;
		while ( i <= end0pos ) {
			int startpos = i;
			for (; (DU[i]==0) && (i<=end0pos); ++i) {}
			int nrzeroes = i-startpos;
			if ( nrzeroes >= I16 ) {
				lng = nrzeroes>>4;
				for (int nrmarker=1; nrmarker <= lng; ++nrmarker)
					writeBits(M16zeroes, out);
				nrzeroes = nrzeroes&0xF;
			}
			pos = 32767+DU[i];
			writeBits(HTAC[(nrzeroes<<4)+category[pos]], out);
			writeBits(bitcode[pos], out);
			i++;
		}
		if ( end0pos != I63 ) {
			writeBits(EOB, out);
		}
		return DC;
	}
	
	private void initCoeffs(int[] imageData)
	{
		original_coeffs = new int[3][(int)(Math.ceil(width/8.0))*(int)(Math.ceil(height/8.0))][64];
		//P.pf("image w/h = %d/%d, w*h/64 = %d", width, height, original_coeffs[0].length);

		int x = 0, y = 0;
		int j = 0;
		int start, p, col, row, pos;
		int quadWidth = width*4;
		while (y < height)
		{
			x = 0;
			while (x < quadWidth)
			{
				start = quadWidth*y+x;
				p = start;
				col = -1;
				row = 0;
				double[] YDU = new double[64];
				double[] UDU = new double[64];
				double[] VDU = new double[64];
				for (pos = 0; pos < 64; pos++)
				{
					row = pos >> 3;// /8
					col = ( pos & 7 ) * 4; // %8
					p = start + ( row * quadWidth ) + col;
					
					if(y+row >= height) // padding bottom
						p-= (quadWidth*(y+1+row-height));
					
					if(x+col >= quadWidth) // padding right
						p-= ((x+col) - quadWidth +4);
					
					int r = imageData[ p++ ];
					int g = imageData[ p++ ];
					int b = imageData[ p++ ];
					YDU[pos] = ((RGB_YUV_TABLE[r]             + RGB_YUV_TABLE[(g +  256)>>0] + RGB_YUV_TABLE[(b +  512)>>0]) >> 16)-128;
					UDU[pos] = ((RGB_YUV_TABLE[(r +  768)>>0] + RGB_YUV_TABLE[(g + 1024)>>0] + RGB_YUV_TABLE[(b + 1280)>>0]) >> 16)-128;
					VDU[pos] = ((RGB_YUV_TABLE[(r + 1280)>>0] + RGB_YUV_TABLE[(g + 1536)>>0] + RGB_YUV_TABLE[(b + 1792)>>0]) >> 16)-128;
				}
				original_coeffs[0][j] = fDCTQuant(YDU, fdtbl_Y);
				original_coeffs[1][j] = fDCTQuant(UDU, fdtbl_UV);
				original_coeffs[2][j] = fDCTQuant(VDU, fdtbl_UV);
				
				//P.pf(" j/length = %d/%d", j, original_coeffs[0].length);
				x+= 32;
				j++;
			}
			y += 8;
		}
	}
	
	private int[] getImageData(BufferedImage image)
	{
		int[] data = new int[image.getWidth()*image.getHeight()*4];
		int i = 0;
		for (int y = 0; y < image.getHeight(); y++)
		{
			for (int x = 0; x < image.getWidth(); x++)
			{
				int pixel = image.getRGB(x, y);
				Color c = new Color(pixel);
				data[i++] = c.getRed();
				data[i++] = c.getGreen();
				data[i++] = c.getBlue();
				data[i++] = c.getAlpha();
			}
		}
		return data;
	}
	
	int[] fDCTQuant(double[] data, double[] fdtbl)
	{
		double d0, d1, d2, d3, d4, d5, d6, d7;
		/* Pass 1: process rows. */
		int dataOff=0;
		int i;
		int I8 = 8;
		int I64 = 64;
		for (i=0; i<I8; ++i)
		{
			d0 = data[dataOff];
			d1 = data[dataOff+1];
			d2 = data[dataOff+2];
			d3 = data[dataOff+3];
			d4 = data[dataOff+4];
			d5 = data[dataOff+5];
			d6 = data[dataOff+6];
			d7 = data[dataOff+7];

			double tmp0 = d0 + d7;
			double tmp7 = d0 - d7;
			double tmp1 = d1 + d6;
			double tmp6 = d1 - d6;
			double tmp2 = d2 + d5;
			double tmp5 = d2 - d5;
			double tmp3 = d3 + d4;
			double tmp4 = d3 - d4;

			/* Even part */
			double tmp10 = tmp0 + tmp3;	/* phase 2 */
			double tmp13 = tmp0 - tmp3;
			double tmp11 = tmp1 + tmp2;
			double tmp12 = tmp1 - tmp2;

			data[dataOff] = tmp10 + tmp11; /* phase 3 */
			data[dataOff+4] = tmp10 - tmp11;

			double z1 = (tmp12 + tmp13) * 0.707106781; /* c4 */
			data[dataOff+2] = tmp13 + z1; /* phase 5 */
			data[dataOff+6] = tmp13 - z1;

			/* Odd part */
			tmp10 = tmp4 + tmp5; /* phase 2 */
			tmp11 = tmp5 + tmp6;
			tmp12 = tmp6 + tmp7;

			/* The rotator is modified from fig 4-8 to avoid extra negations. */
			double z5 = (tmp10 - tmp12) * 0.382683433; /* c6 */
			double z2 = 0.541196100 * tmp10 + z5; /* c2-c6 */
			double z4 = 1.306562965 * tmp12 + z5; /* c2+c6 */
			double z3 = tmp11 * 0.707106781; /* c4 */

			double z11 = tmp7 + z3;	/* phase 5 */
			double z13 = tmp7 - z3;

			data[dataOff+5] = z13 + z2;	/* phase 6 */
			data[dataOff+3] = z13 - z2;
			data[dataOff+1] = z11 + z4;
			data[dataOff+7] = z11 - z4;

			dataOff += 8; /* advance pointer to next row */
		}

		/* Pass 2: process columns. */
		dataOff = 0;
		for (i=0; i<I8; ++i)
		{
			d0 = data[dataOff];
			d1 = data[dataOff + 8];
			d2 = data[dataOff + 16];
			d3 = data[dataOff + 24];
			d4 = data[dataOff + 32];
			d5 = data[dataOff + 40];
			d6 = data[dataOff + 48];
			d7 = data[dataOff + 56];

			double tmp0p2 = d0 + d7;
			double tmp7p2 = d0 - d7;
			double tmp1p2 = d1 + d6;
			double tmp6p2 = d1 - d6;
			double tmp2p2 = d2 + d5;
			double tmp5p2 = d2 - d5;
			double tmp3p2 = d3 + d4;
			double tmp4p2 = d3 - d4;

			/* Even part */
			double tmp10p2 = tmp0p2 + tmp3p2;	/* phase 2 */
			double tmp13p2 = tmp0p2 - tmp3p2;
			double tmp11p2 = tmp1p2 + tmp2p2;
			double tmp12p2 = tmp1p2 - tmp2p2;

			data[dataOff] = tmp10p2 + tmp11p2; /* phase 3 */
			data[dataOff+32] = tmp10p2 - tmp11p2;

			double z1p2 = (tmp12p2 + tmp13p2) * 0.707106781; /* c4 */
			data[dataOff+16] = tmp13p2 + z1p2; /* phase 5 */
			data[dataOff+48] = tmp13p2 - z1p2;

			/* Odd part */
			tmp10p2 = tmp4p2 + tmp5p2; /* phase 2 */
			tmp11p2 = tmp5p2 + tmp6p2;
			tmp12p2 = tmp6p2 + tmp7p2;

			/* The rotator is modified from fig 4-8 to avoid extra negations. */
			double z5p2 = (tmp10p2 - tmp12p2) * 0.382683433; /* c6 */
			double z2p2 = 0.541196100 * tmp10p2 + z5p2; /* c2-c6 */
			double z4p2 = 1.306562965 * tmp12p2 + z5p2; /* c2+c6 */
			double z3p2 = tmp11p2 * 0.707106781; /* c4 */

			double z11p2 = tmp7p2 + z3p2;	/* phase 5 */
			double z13p2 = tmp7p2 - z3p2;

			data[dataOff+40] = z13p2 + z2p2; /* phase 6 */
			data[dataOff+24] = z13p2 - z2p2;
			data[dataOff+ 8] = z11p2 + z4p2;
			data[dataOff+56] = z11p2 - z4p2;

			dataOff++; /* advance pointer to next column */
		}

		// Quantize/descale the coefficients
		double fDCTQuantVar;
		int[] outputfDCTQuant = new int[64];
		for (i=0; i<I64; ++i)
		{
			// Apply the quantization and scaling factor & Round to nearest integer
			fDCTQuantVar = data[i]*fdtbl[i];
			outputfDCTQuant[i] = (fDCTQuantVar>0) ? (int)(fDCTQuantVar+0.5) : (int)(fDCTQuantVar-0.5);
		}

		return outputfDCTQuant;
	}

	private void setQuality(int quality){
		if (quality <= 0) {
			quality = 1;
		}
		if (quality > 100) {
			quality = 100;
		}


		double sf = 0;
		if (quality < 50) {
			sf = Math.floor(5000 / quality);
		} else {
			sf = Math.floor(200 - quality*2);
		}

		initQuantTables(sf);
	}
	
	private void initQuantTables(double sf)
	{
		for (int i = 0; i < 64; i++)
		{
			int t = (int)Math.floor((YQT[i]*sf+50)/100);
			if (t < 1)
				t = 1;
			else if (t > 255)
				t = 255;
			YTable[ZigZag[i]] = t;
		}
		
		for (int j = 0; j < 64; j++)
		{
			int u = (int)Math.floor((UVQT[j]*sf+50)/100);
			if (u < 1)
				u = 1;
			else if (u > 255)
				u = 255;
			UVTable[ZigZag[j]] = u;
		}
		
		int k = 0;
		for (int row = 0; row < 8; row++)
		{
			for (int col = 0; col < 8; col++)
			{
				fdtbl_Y[k]  = (1.0 / (YTable [ZigZag[k]] * aasf[row] * aasf[col] * 8.0));
				fdtbl_UV[k] = (1.0 / (UVTable[ZigZag[k]] * aasf[row] * aasf[col] * 8.0));
				k++;
			}
		}
	}

	private void initHuffmanTbl()
	{
		YDC_HT  = computeHuffmanTbl(std_dc_luminance_nrcodes,   std_dc_luminance_values);
		UVDC_HT = computeHuffmanTbl(std_dc_chrominance_nrcodes, std_dc_chrominance_values);
		YAC_HT  = computeHuffmanTbl(std_ac_luminance_nrcodes,   std_ac_luminance_values);
		UVAC_HT = computeHuffmanTbl(std_ac_chrominance_nrcodes, std_ac_chrominance_values);
	}
	
	int[][] computeHuffmanTbl(int[] nrcodes, int[] std_table)
	{
		int codevalue = 0;
		int pos_in_table = 0;
		int[][] HT = new int[maxOf(std_table)+1][2];
		for (int k = 1; k <= 16; k++)
		{
			for (int j = 1; j <= nrcodes[k]; j++)
			{
				HT[std_table[pos_in_table]] = new int[] {codevalue, k};
				pos_in_table++;
				codevalue++;
			}
			codevalue*=2;
		}
		return HT;
	}
	
	private int maxOf(int[] arr)
	{
		int max = Integer.MIN_VALUE;
		for (int i : arr)
			if (i > max)
				max = i;
		return max;
	}

	private void initCategoryNumber()
	{
		int nrlower = 1;
		int nrupper = 2;
		for (int cat = 1; cat <= 15; cat++) {
			//Positive numbers
			for (int nr = nrlower; nr<nrupper; nr++) {
				category[32767+nr] = cat;
				bitcode[32767+nr] = new int[] {nr, cat};
				//bitcode[32767+nr][1] = cat;
				//bitcode[32767+nr][0] = nr;
			}
			//Negative numbers
			for (int nrneg =-(nrupper-1); nrneg<=-nrlower; nrneg++) {
				category[32767+nrneg] = cat;
				bitcode[32767+nrneg] = new int[] {nrupper-1+nrneg, cat};
				//bitcode[32767+nrneg][1] = cat;
				//bitcode[32767+nrneg][0] = nrupper-1+nrneg;
			}
			nrlower <<= 1;
			nrupper <<= 1;
		}
	}
	
	private void initRGBYUVTable() {
		for(int i = 0; i < 256;i++) {
			RGB_YUV_TABLE[i]      		=  19595 * i;
			RGB_YUV_TABLE[(i+ 256)>>0] 	=  38470 * i;
			RGB_YUV_TABLE[(i+ 512)>>0] 	=   7471 * i + 0x8000;
			RGB_YUV_TABLE[(i+ 768)>>0] 	= -11059 * i;
			RGB_YUV_TABLE[(i+1024)>>0] 	= -21709 * i;
			RGB_YUV_TABLE[(i+1280)>>0] 	=  32768 * i + 0x807FFF;
			RGB_YUV_TABLE[(i+1536)>>0] 	= -27439 * i;
			RGB_YUV_TABLE[(i+1792)>>0] 	= - 5329 * i;
		}
	}
	
}
