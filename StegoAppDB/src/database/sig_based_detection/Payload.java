package database.sig_based_detection;

import java.util.ArrayList;
import java.util.List;

import util.P;

public class Payload {
	
	public String stegoApp;
	public List<Byte> bytes;
	
	public int bitCount;
	String currentByte;
	int currentBitIndex;
	public Payload()
	{
		bytes = new ArrayList<>();
		currentByte = "";
		currentBitIndex = 0;
		bitCount = 0;
	}
	
	public void addBit(int bit)
	{
		currentByte += ""+bit; // left shift one 
		currentBitIndex++;
		bitCount++;
		if (currentBitIndex >= 8)
		{
			bytes.add((byte)Integer.parseInt(currentByte, 2));
			currentByte = "";
			currentBitIndex = 0;
		}
	}
	
	public byte[] getBytes(int from, int to)
	{
		byte[] bytes = new byte[to-from+1];
		int index = 0;
		for (int i = from ; i <= to; i++)
		{
			bytes[index++] = this.bytes.get(i);
		}
		return bytes;
	}
	
	public static boolean compare(byte[] b1, byte[] b2)
	{
		if (b1.length != b2.length)
			return false;
		for (int i = 0; i < b1.length; i++)
		{
			if (b1[i] != b2[i])
				return false;
		}
		return true;
	}
	
	public static void p(byte[] bytes)
	{
		String msg = "byte array["+bytes.length+"] = {";
		for (byte b : bytes)
			msg += b+",";
		if (bytes.length>0)
			msg = msg.substring(0, msg.length()-1);
		msg += "}";
		P.p(msg);
	}
	
}
