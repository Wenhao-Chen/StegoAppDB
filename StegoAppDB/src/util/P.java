package util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class P {
	
	public static String getTimeString()
	{
		return getTimeString("yyyy-MMdd-HH-mm-ss");
	}
	
	public static String getTimeString(String pattern)
	{
		return new SimpleDateFormat(pattern).format(new Date());
	}

	public static String pad(String old, int length, String fillWith)
	{
		while (old.length()<length)
		{
			old = fillWith+old;
		}
		return old;
	}
	
	public static String pad(String old, int length)
	{
		return pad(old, length, "0");
	}
	
	public static void sleep(int millis)
	{
		try
		{
			Thread.sleep(millis);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static Process exec(String cmd, boolean waitFor)
	{
		try
		{
			//P.p("[cmd]"+cmd);
			Process p = Runtime.getRuntime().exec(cmd);
			if (waitFor)
				p.waitFor();
			return p;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}
	
	public static void printProcessStreams(Process p)
	{
		try
		{
			Thread t1 = new Thread(new Runnable(){
				@Override
				public void run()
				{
					printStream(p.getInputStream(), System.out);
				}
			});
			
			Thread t2 = new Thread(new Runnable(){
				@Override
				public void run()
				{
					printStream(p.getErrorStream(), System.err);
				}
			});
			
			t1.start();
			t2.start();
			t1.join();
			t2.join();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static Process exec(String cmd, boolean waitFor, boolean print)
	{
		try
		{
			P.p("[cmd]"+cmd);
			Process p = Runtime.getRuntime().exec(cmd);
			
			if (print)
			{
				Thread t1 = new Thread(new Runnable(){
					@Override
					public void run()
					{
						printStream(p.getInputStream(), System.out);
					}
				});
				
				Thread t2 = new Thread(new Runnable(){
					@Override
					public void run()
					{
						printStream(p.getErrorStream(), System.err);
					}
				});
				
				t1.start();
				t2.start();
				t1.join();
				t2.join();
			}
			return p;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}
	
	public static void printStream(InputStream stream)
	{
		printStream(stream, System.out);
	}
	
	public static void printStream(InputStream stream, PrintStream out)
	{
		try
		{
			BufferedReader in = new BufferedReader(new InputStreamReader(stream));
			String line;
			while ((line=in.readLine())!=null)
			{
				out.println(line);
			}
			in.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	
	public static void waitFor(Process p)
	{
		try
		{
			p.waitFor();
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
	}
	
	public static void e(CharSequence s)
	{
		System.err.println(s);
	}
	
	public static void p(CharSequence s)
	{
		System.out.println(s);
	}
	
	public static void p(String s, File f)
	{
		p(s);
		F.writeLine(s, f, true);
	}
	
	public static void pf(String format, Object... args)
	{
		p(String.format(format, args));
	}
	
	public static void p(Iterable<? extends CharSequence> strings)
	{
		for (CharSequence s: strings)
			p(s);
		
	}

	
	public static void pause()
	{
		byte[] b = new byte[10];
		try {
			System.in.read(b);
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args)
	{
		P.p("read: " + read());
	}
	
	public static String read()
	{
		try
		{
			byte[] b = new byte[255];
			System.in.read(b);
			return new String(b);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}
	
	public static List<String> readStream(InputStream stream)
	{
		BufferedReader in = new BufferedReader(new InputStreamReader(stream));
		return readFromBufferedReader(in);
	}
	
	public static List<String> readFromBufferedReader(BufferedReader in)
	{
		List<String> result = new ArrayList<>();
		String line;
		try
		{
			while ((line = in.readLine())!=null)
			{
				result.add(line);
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		return result;
	}
	
	public static List<String> readInputStream(Process p)
	{
		return readStream(p.getInputStream());
	}
	
	public static List<String> readErrorStream(Process p)
	{
		return readStream(p.getErrorStream());
	}
	

}
