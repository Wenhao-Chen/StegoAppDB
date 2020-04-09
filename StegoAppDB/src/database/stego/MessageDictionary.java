package database.stego;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import ui.ProgressUI;
import util.F;
import util.P;

public class MessageDictionary {

	public static class InputMessage {
		public String message;
		public String dictName;
		public int lineIndex;
	}

	
	public static void main(String[] args)
	{
		//Input Dictionary,shakespeare_richardii.txt
		//Dictionary Starting Line,1273
		//Input Message Length (bytes),52065
		//P.p(getMessage("shakespeare_henryv.txt", 2327, 39498));
		P.p(getMessage("shakespeare_richardii.txt", 1273, 52065));
	}
	
	public static String dict_dir = "E:/message_dictionary";
	
	public static String getMessage(File dict, int lineIndex, int bytes)
	{
		try
		{
			BufferedReader in = new BufferedReader(new FileReader(dict));
			String line;
			StringBuilder sb = new StringBuilder();
			
			// reach target line index
			int currLine = 1;
			while (currLine++<lineIndex)
				in.readLine();
			
			while ((line=in.readLine())!=null && sb.length()<bytes)
			{
				sb.append(line);
				sb.append('\n');
			}
			in.close();
			return sb.substring(0, bytes);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}
	
	public static String getMessage(String dictName, int lineIndex, int bytes)
	{
		return getMessage(new File(dict_dir, dictName), lineIndex, bytes);
	}
	
	public static InputMessage randomMessage(int bytes)
	{
		File f = getRandomDictionary(bytes);
		try
		{

			BufferedReader in = new BufferedReader(new FileReader(f));
			String line;
			
			int lineCount = 0;
			long total = f.length(), len = 0;
			
			while ((line=in.readLine())!=null && len<=total-bytes)
			{
				lineCount++;
				len += line.length()+1; // +1 is for the new line character
			}
			in.close();
			
			InputMessage im = new InputMessage();
			im.dictName = f.getName();
			im.lineIndex = new Random().nextInt(lineCount)+1;
			im.message = getMessage(im.dictName, im.lineIndex, bytes);
			
			return im;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}
	

	public static File getRandomDictionary(int bytes)
	{
		File dictFolder = new File(dict_dir);
        if (!dictFolder.exists() || !dictFolder.isDirectory() || dictFolder.list().length<1)
        {
            System.err.println("can't find any dictionary files at \"" + dict_dir + "\"");
            System.exit(1);
        }

        File[] dicts = dictFolder.listFiles();
        List<File> largeEnough = new ArrayList<>();
        for (File dict : dicts)
            if (dict.length()>bytes)
                largeEnough.add(dict);
        if (largeEnough.isEmpty())
        {
            System.err.println("Can't find a dictionary large enough that provides " + bytes + " bytes.");
            System.exit(1);
        }

        return largeEnough.get(new Random().nextInt(largeEnough.size()));
	}
    
    
    
    public static int randomInt(int lower, int upper)
    {
    	return (int)Math.floor(Math.random()*(upper-lower+1))+lower;
    }
	
    public static void txtToJs(File txt, File js)
    {
    	if (js.exists() && js.length()>txt.length())
    		return;
    	
    	String dictName = txt.getName().substring(0, txt.getName().lastIndexOf("."));
    	long length = txt.length();
    	
    	PrintWriter out = F.initPrintWriter(js);
    	out.println(String.format("var %s = {", dictName));
    	out.println(String.format("\tname:\"%s\",", txt.getName()));
    	out.println(String.format("\tlength:%d,", length));
    	out.println("\tcontent:");
    	
    	List<String> lines = F.readLinesWithoutEmptyLines(txt);
    	for (int i = 0; i < lines.size(); i++)
    	{
    		out.println("\t\t\""+lines.get(i)+"\\n\"" + (i==lines.size()-1?"":"+"));
    	}
    	
    	out.println("};");
    	out.close();
    }
}
