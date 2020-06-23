import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class ReadFromDictionary {

	public static void main(String[] args)
	{
		File dictFolder = new File("E:/message_dictionary");
		String dictName = "shakespeare_coriolanus.txt";
		int startingLine = 2998;
		int numberOfBytes = 3258;
		
		File dictFile = new File(dictFolder, dictName);
		System.out.println(getMessage(dictFile, startingLine, numberOfBytes));
	}
	
	public static String getMessage(File dict, int lineIndex, int bytes)
	{
		try
		{
			BufferedReader in = new BufferedReader(new FileReader(dict));
			String line;
			StringBuilder sb = new StringBuilder();
			
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

}
