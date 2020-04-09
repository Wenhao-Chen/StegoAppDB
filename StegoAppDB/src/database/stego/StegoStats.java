package database.stego;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import util.F;

public class StegoStats {

	
	// general Information
    public String inputImageName, coverImageName;
    public String stegoApp;
    public float embeddingRate;
    public int capacity, embedded, changed;
    public String dictionary;
    public int dictStartLine, messageLength;
    public String password;
    public long time;

    public Map<String, String> additionalInfo = new HashMap<>();


    public void saveToFile(String outPath)
    {
        try
        {
            PrintWriter out = new PrintWriter(new FileWriter(outPath));
            out.write("Input Image,"+inputImageName+"\n");
            out.write("Stego App,"+stegoApp+"\n");
            out.write("Cover Image,"+coverImageName+"\n");
            out.write("Capacity,"+capacity+"\n");
            out.write("Embedding Rate,"+embeddingRate+"\n");
            out.write("Embedded,"+embedded+"\n");
            out.write("Changed,"+changed+"\n");
            out.write("Input Dictionary,"+dictionary+"\n");
            out.write("Dictionary Starting Line,"+dictStartLine+"\n");
            out.write("Input Message Length (bytes),"+messageLength+"\n");
            out.write("Password,"+password+"\n");
            out.write("Embedding Time (miliseconds),"+time+"\n");
            for (Map.Entry<String, String> entry : additionalInfo.entrySet())
            {
                out.write(entry.getKey()+","+entry.getValue()+"\n");
            }
            out.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    public static Map<String, String> load(File f)
    {
    	if (!f.getName().endsWith(".csv"))
    		return null;
    	Map<String, String> info = new HashMap<>();
    	List<String> lines = F.readLines(f);
    	for (String line : lines)
    	{
    		if (!line.contains(","))
    			continue;
    		String[] parts = line.split(",");
    		int index = parts[0].indexOf(" (");
    		if (index>0)
    			parts[0] = parts[0].substring(0, index);
    		info.put(parts[0], parts[1]);
    	}
    	return info;
    }
}
