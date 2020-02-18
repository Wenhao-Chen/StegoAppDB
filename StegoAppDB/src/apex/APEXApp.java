package apex;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import apex.bytecode_wrappers.APEXClass;
import apex.bytecode_wrappers.APEXMethod;
import util.Apktool;
import util.Dalvik;
import util.F;
import util.P;

public class APEXApp {

	public static File defaultDecodedDir = new File("C:/workspace/app_analysis/decoded");
	
	public File apk, outDir, manifestF, resDir, smaliDir, backupDir;
	public List<File> otherSmaliDirs;
	public Map<String, APEXClass> classes;
	public String packageName;
	public Document manifest;
	public static boolean justDecode = false;
	public static boolean backup = false;
	public static boolean verbose = false;
	
	public APEXApp(File f)
	{
		this(f, null);
	}
	
	public APEXApp(File f, File dir)
	{
		this(f, dir, false);
	}
	
	public APEXApp(File f, boolean forceDecode)
	{
		this(f, null, forceDecode);
	}
	
	public APEXApp(File f, File dir, boolean forceDecode)
	{
		this.apk = f;
		this.outDir = dir;
		
		// set up apktool output directory
		if (outDir == null)
		{
			outDir = new File(defaultDecodedDir, f.getName());
		}
		
		boolean shouldBackup = false;
		// run apktool if necessary
		if (forceDecode || !outDir.exists())
		{
			Apktool.decode(apk, outDir);
		}
		else if (verbose)
		{
			P.p("Skipping apktool since out dir already exists: "+outDir.getAbsolutePath());
		}
		
		// initiate the dirs
		manifestF = new File(outDir, "AndroidManifest.xml");
		resDir = new File(outDir, "res");
		smaliDir = new File(outDir, "smali");
		findOtherSmaliDirs();

		
		if (!justDecode)
		{
			// save backup smali
			backupDir = new File(outDir, "backup");
			backupDir.mkdirs();
			if (backup)
				backupSmali();
			
			// parse smali and XMLs
			parseSmali();
			parseXML();
		}
		loadInstancOfAnswers();
	}
	
	public List<APEXClass> getNonLibraryClasses()
	{
		List<APEXClass> result = new ArrayList<APEXClass>();
		if (packageName.equals("jubatus.android.davinci"))
		{
			for (APEXClass c : classes.values())
			{
				if (c.dexName.startsWith("Ljubatus/android/davinci"))
					result.add(c);
			}
			return result;
		}
		
		for (APEXClass c : classes.values())
		{
			if (!c.isLibraryClass())
				result.add(c);
		}
		return result;
	}
	
	public APEXMethod getNonLibraryMethod(String methodSig)
	{
		String classDexName = methodSig.substring(0, methodSig.indexOf("->"));
		APEXClass c = classes.get(classDexName);
		if (c == null || c.isLibraryClass())
			return null;
		APEXMethod m = c.methods.get(methodSig);
		if (m != null && !m.modifiers.contains("abstract") && !m.modifiers.contains("native"))
			return m;
		return null;
	}
	
	public APEXClass[] getNonLibraryClassesAsArray()
	{
		List<APEXClass> list = getNonLibraryClasses();
		APEXClass[] arr = list.toArray(new APEXClass[list.size()]);
		Arrays.sort(arr, (c1,c2)->(c1.getJavaName().compareTo(c2.getJavaName())));
		return arr;
	}
	
	public APEXMethod getMethod(String methodSig)
	{
		String classDexName = methodSig.substring(0, methodSig.indexOf("->"));
		APEXClass c = classes.get(classDexName);
		if (c == null)
			return null;
		APEXMethod m = c.methods.get(methodSig);
		if (m != null && !m.modifiers.contains("abstract") && !m.modifiers.contains("native"))
			return m;
		return null;
	}
	
	public Set<APEXMethod> getMethods(List<String> sigs)
	{
		Set<APEXMethod> res = new HashSet<>();
		sigs.forEach(sig -> {
			APEXMethod m = getMethod(sig);
			if (m != null)
			res.add(m);
		});
		return res;
	}
	
	private void parseSmali()
	{
		classes = new HashMap<>();
		parseSmali(smaliDir);
		for (File dir : otherSmaliDirs)
			parseSmali(dir);		
	}
	
	private void parseSmali(File f)
	{
		if (f.isDirectory())
		{
			for (File ff : f.listFiles())
				parseSmali(ff);
			return;
		}
		if (f.getName().endsWith(".smali"))
		{
			APEXClass c = new APEXClass(f, this);
			classes.put(c.dexName, c);
		}
		else
			P.e("Parsing smali files but found this: " + f.getAbsolutePath());

		
	}
	
	
	private void parseXML()
	{
		parseManifest();
		parseOtherXMLs();
	}
	
	private void parseManifest()
	{
		if (manifestF == null || !manifestF.exists() || manifestF.length()==0)
			return;
		try
		{
			manifest = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(manifestF);
			manifest.getDocumentElement().normalize();
			packageName = manifest.getDocumentElement().getAttributes().getNamedItem("package").getNodeValue();
			NodeList activities = manifest.getElementsByTagName("activity");
			boolean mainActFound = false;
			for (int i = 0; i < activities.getLength(); i++)
			{
				Node activity = activities.item(i);
				String className = activity.getAttributes().getNamedItem("android:name").getNodeValue();
				if (className.startsWith("."))
					className = packageName + className;
				if (!className.contains("."))
					className = packageName + "." + className;
				APEXClass c = classes.get(Dalvik.JavaToDexName(className));
				if (c != null)
				{
					c.isActivity = true;
					Element e = (Element) activity;
					NodeList actions = e.getElementsByTagName("action");
					for (int j = 0; j < actions.getLength(); j++)
					{
						Node action = actions.item(j);
						if (action.getAttributes().getNamedItem("android:name")
								.getNodeValue().equals("android.intent.action.MAIN"))
						{
							c.isMainActivity = true;
							mainActFound = true;
							break;
						}
					}
				}
			}

			// sometimes main activity declaration is in the <activity-alias> tag
			NodeList aliasList = manifest.getElementsByTagName("activity-alias");
			int index = 0;
			while (!mainActFound && index < aliasList.getLength())
			{
				Node aliasNode = aliasList.item(index);
				String aName = aliasNode.getAttributes().getNamedItem("android:targetActivity").getNodeValue();
				if (aName.startsWith("."))
					aName = aName.substring(1, aName.length());
				if (!aName.contains("."))
					aName = packageName + "." + aName;
				Element e = (Element) aliasNode;
				NodeList actions = e.getElementsByTagName("action");
				for (int j = 0, len2 = actions.getLength(); j < len2; j++)
				{
					Node action = actions.item(j);
					if (action.getAttributes().getNamedItem("android:name")
							.getNodeValue().equals("android.intent.action.MAIN"))
					{
						APEXClass c = classes.get(Dalvik.JavaToDexName(aName));
						c.isMainActivity = true;
						mainActFound = true;
						break;
					}
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private void parseOtherXMLs()
	{
		//TODO
	}
	
	private void backupSmali()
	{
		P.p("Backing up smali files...");
		File backupSmaliDir = new File(backupDir, smaliDir.getName());
		F.copy(smaliDir, backupSmaliDir);
		for (File dir : otherSmaliDirs)
		{
			File to = new File(backupDir, dir.getName());
			F.copy(dir, to);
		}
	}
	
	private void findOtherSmaliDirs()
	{
		otherSmaliDirs = new ArrayList<>();
		for (File f : outDir.listFiles())
		{
			if (f.isDirectory() && f.getName().startsWith("smali_classes"))
				otherSmaliDirs.add(f);
		}
	}
	
	
	private static class InstanceOfAnswer {
		String dexName;
		boolean answer;
		InstanceOfAnswer(String s, boolean b) {dexName = s; answer = b;}
	}
	
	private File answerF;
	private Map<String, InstanceOfAnswer> instanceOfAnswers;
	private void loadInstancOfAnswers()
	{
		answerF = new File("InstanceOfAnswers/"+this.packageName+".csv");
		instanceOfAnswers = new HashMap<>();
		answerF.getParentFile().mkdirs();
		List<String> lines = F.readLines(answerF);
		for (String l : lines)
		{
			String[] parts = l.split(",");
			if (parts.length==3)
			{
				instanceOfAnswers.put(parts[0], new InstanceOfAnswer(parts[1], parts[2].equals("true")));
			}
		}
	}
	
	public boolean instanceOf(String classA, String classB)
	{
		if (classA.equals(classB))
			return true;
		if (Dalvik.isPrimitiveType(classA) || Dalvik.isPrimitiveType(classB))
			return classA.equals(classB);
		
		if (Dalvik.isArrayType(classA) && !Dalvik.isArrayType(classB))
			return false;
		if (Dalvik.isArrayType(classB) && !Dalvik.isArrayType(classA))
			return false;
		if (Dalvik.isArrayType(classA) && Dalvik.isArrayType(classB))
			return instanceOf(classA.substring(1), classB.substring(1));
		
		if (!classes.containsKey(classA) || !classes.containsKey(classB))
		{
			return true;
/*			if (instanceOfAnswers.containsKey(classA))
				return instanceOfAnswers.get(classA).answer;
			
			while (true)
			{
				P.p("[PAUSED] need human input on instanceOf(), classA = "+classA+", classB = "+classB+". Type 'y' or 'n'...");
				String response = P.read();
				P.p("response: " + response);
				if (response.charAt(0)=='y')
				{
					P.p("response: true");
					instanceOfAnswers.put(classA, new InstanceOfAnswer(classB, true));
					F.writeLine(classA+","+classB+",true", this.answerF, true);
					return true;
				}
				else if (response.charAt(0)=='n')
				{
					P.p("response: false");
					instanceOfAnswers.put(classA, new InstanceOfAnswer(classB, false));
					F.writeLine(classA+","+classB+",false", this.answerF, true);
					return false;
				}
			}*/
		}
		else
		{
			APEXClass cA = classes.get(classA);
			APEXClass cB = classes.get(classB);
			return cA.instanceOf(cB, this);
		}
	}
}
