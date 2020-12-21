package util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import ui.ProgressUI;

public class F {

	public static ProgressUI ui;
	
	public static void writeObject(Object obj, String path)
	{
		try
		{
			FileOutputStream fileOut = new FileOutputStream(path);
            ObjectOutputStream objectOut = new ObjectOutputStream(fileOut);
            objectOut.writeObject(obj);
            objectOut.close();
 
        }
		catch (Exception ex)
		{
            ex.printStackTrace();
        }
	}
	
	public static void writeObject(Object obj, File f)
	{
		writeObject(obj, f.getAbsolutePath());
	}
	
	public static Object readObject(File f) {
		return readObject(f.getAbsolutePath());
	}
	
	public static Object readObject(String path)
	{
		try
		{
			FileInputStream fileIn = new FileInputStream(path);
			ObjectInputStream objectIn = new ObjectInputStream(fileIn);
			Object obj = objectIn.readObject();
			objectIn.close();
			return obj;
		}
		catch (ClassNotFoundException e) {
			return null;
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		return null;
	}
	
	public static PrintWriter initPrintWriter(File f)
	{
		return initPrintWriter(f.getAbsolutePath());
	}
	
	public static PrintWriter initPrintWriter(String path)
	{
		return initPrintWriter(path, false);
	}
	
	public static PrintWriter initPrintWriter(File f, boolean append)
	{
		return initPrintWriter(f.getAbsolutePath(), append);
	}
	
	public static PrintWriter initPrintWriter(String path, boolean append)
	{
		PrintWriter writer = null;
		try
		{
			writer = new PrintWriter(new FileWriter(path, append));
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return writer;
	}
	
	public static FileOutputStream initFileOutputStream(File f)
	{
		try
		{
			return new FileOutputStream(f);
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
		return null;
	}
	
	public static BufferedReader initReader(File f)
	{
		try
		{
			return new BufferedReader(new FileReader(f));
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
		return null;
	}
	
	
	public static List<String> readLines(File f)
	{
		List<String> result = new ArrayList<>();
		if (!f.exists())
			return result;
		try
		{
			BufferedReader in = new BufferedReader(new FileReader(f));
			String line;
			while ((line=in.readLine())!=null)
			{
				result.add(line);
			}
			in.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return result;
	}
	
	public static List<String> readLinesWithoutEmptyLines(File f)
	{
		List<String> result = new ArrayList<>();
		if (!f.exists())
			return result;
		try
		{
			BufferedReader in = new BufferedReader(new FileReader(f));
			String line;
			while ((line=in.readLine())!=null)
			{
				if (line.isEmpty())
					continue;
				result.add(line);
			}
			in.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return result;
	}
	
	public static String readFirstLine(File f)
	{
		try
		{
			BufferedReader in = new BufferedReader(new FileReader(f));
			String line = in.readLine();
			in.close();
			return line;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return "";
	}
	
	
	
	public static void writeLine(String s, File f, boolean append)
	{
		write(s.endsWith("\n")?s:s+"\n", f, append);
	}
	
	public static void write(String s, File f, boolean append)
	{
		try
		{
			PrintWriter out = new PrintWriter(new FileWriter(f, append));
			out.write(s);
			out.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static void write(Iterable<? extends Object> list, File f, boolean append)
	{
		try
		{
			PrintWriter out = new PrintWriter(new FileWriter(f, append));
			for (Object obj : list)
				out.write(obj.toString()+"\n");
			out.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static void write(Map<? extends Object, ? extends Object> map, String separator, File f, boolean append) {
		try
		{
			PrintWriter out = new PrintWriter(new FileWriter(f, append));
			for (Map.Entry<? extends Object, ? extends Object> entry : map.entrySet())
				out.println(entry.getKey().toString()+separator+entry.getValue().toString());
			out.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static void write(int[] arr, File f, boolean append) {
		try
		{
			PrintWriter out = new PrintWriter(new FileWriter(f, append));
			for (int i : arr)
				out.println(i);
			out.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static void write(double[] arr, File f, boolean append) {
		try
		{
			PrintWriter out = new PrintWriter(new FileWriter(f, append));
			for (double i : arr)
				out.println(i);
			out.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static void copyToFolder(File from, File toDir) {
		File to = new File(toDir, from.getName());
		copy(from, to);
	}
	
	public static void copy(File from, File to)
	{
		if (from.isDirectory())
		{
			to.mkdirs();
			for (File f1 : from.listFiles()) {
				File f2 = new File(to, f1.getName());
				copy(f1, f2);
			}
		}
		else if (!to.exists())
		{
			try
			{
				Files.copy(from.toPath(), to.toPath(), StandardCopyOption.REPLACE_EXISTING);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	
	public static String getFileExt(File f)
	{
		int index = f.getName().lastIndexOf(".");
		if (index == -1)
			return "";
		return f.getName().substring(index+1);
	}
	
	public static boolean compare(File f1, File f2)
	{
		final int blockSize = 128;
		try
		{
			InputStream in1 = new FileInputStream(f1);
			InputStream in2 = new FileInputStream(f2);
			byte[] aBuffer = new byte[blockSize];
		    byte[] bBuffer = new byte[blockSize];
		    while (true)
		    {
		    	int aByteCount = in1.read(aBuffer, 0, blockSize);
		        in2.read(bBuffer, 0, blockSize);
		        if (aByteCount < 0) {
		        	in1.close();
		        	in2.close();
		            return true;
		        }
		        if (!Arrays.equals(aBuffer, bBuffer)) {
		        	in2.close();
		        	in1.close();
		            return false;
		        }
		    }
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return true;
	}
	
	public static void changeExtToLowerCase(File f)
	{
		if (f.isDirectory())
		{
			for (File ff : f.listFiles())
				changeExtToLowerCase(ff);
		}
		else
		{
			String ext = F.getFileExt(f);
			if (ext.equals("JPG")||ext.equals("DNG"))
			{
				String left = f.getName().substring(0, f.getName().lastIndexOf("."));
				File newF = new File(f.getParentFile(), left+"."+ext.toLowerCase());
				f.renameTo(newF);
			}
		}
	}
	
	public static void delete(File f)
	{
		if (f.isDirectory())
		{
			for (File ff : f.listFiles())
				delete(ff);
		}
		if (ui != null)
			ui.newLine("deleting " + f.getAbsolutePath());
		f.delete();
	}
	
}
