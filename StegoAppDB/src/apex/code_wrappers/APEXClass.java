package apex.code_wrappers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import apex.APEXApp;
import util.Dalvik;

public class APEXClass {


	public File smaliF, backupSmaliF;

	public Map<String, APEXField> fields;
	public Map<String, APEXMethod> methods;
	public Map<String, Integer> methodNameIndex;
	public boolean isActivity, isMainActivity;
	
	public String declaration, dexName, superClassDexName, sourceFileName;
	public Set<String> modifiers;
	public List<String> interfaces;
	public List<List<String>> annotations;
	
	
	private BufferedReader in;
	private APEXApp app;
	
	public APEXClass(File smaliF, APEXApp app)
	{
		this.smaliF = smaliF;
		this.app = app;
		
		isActivity = false;
		isMainActivity = false;
		
		parseSmali();
	}
	
	
	public String getJavaName()
	{
		return Dalvik.DexToJavaName(dexName);
	}
	
	
	private void parseSmali()
	{
		fields = new HashMap<>();
		methods = new HashMap<>();
		methodNameIndex = new HashMap<>();
		modifiers = new HashSet<>();
		interfaces = new ArrayList<>();
		annotations = new ArrayList<>();
		
		Map<String, Integer> nameCount = new HashMap<>();
		try
		{
			in = new BufferedReader(new FileReader(smaliF));
			String line;
			while ((line=in.readLine())!=null)
			{
				if (line.isEmpty() || line.charAt(0)!='.')
					continue;
					
				String keyword = line.substring(0, line.indexOf(" "));
				switch (keyword)
				{
				case ".class":
					declaration = line;
					dexName = line.substring(line.lastIndexOf(" ")+1);
					String[] parts = line.split(" ");
					for (int i = 1; i < parts.length-1; i++)
						modifiers.add(parts[i]);
					break;
					
				case ".super":
					superClassDexName = line.substring(line.indexOf(" ")+1);
					break;
					
				case ".source":
					sourceFileName = line.substring(line.indexOf("\"")+1, line.lastIndexOf("\""));
					break;
					
				case ".implements":
					interfaces.add(line.substring(line.indexOf(" ")+1));
					break;
					
				case ".annotation":
					// Multiple lines with last line equals ".end annotation"
					List<String> annotation = new ArrayList<>();
					annotation.add(line);
					readLinesUntil(annotation, ".end annotation");
					annotations.add(annotation);
					break;
				
				case ".field":
					//sometimes fields have annotations
					List<String> fieldBody = new ArrayList<>();
					fieldBody.add(line);
					line = in.readLine();
					if (line!=null && line.trim().startsWith(".annotation"))
					{
						fieldBody.add(line);
						readLinesUntil(fieldBody, ".end field");
					}
					APEXField f = new APEXField(fieldBody, this);
					fields.put(f.subSignature, f);
					break;
					
				case ".method":
					List<String> methodBody = new ArrayList<>();
					methodBody.add(line);
					readLinesUntil(methodBody, ".end method");
					APEXMethod m = new APEXMethod(methodBody, this);
					methods.put(m.signature, m);
					
					String name = m.getName();
					int count = nameCount.getOrDefault(name, 0)+1;
					nameCount.put(name, count);
					methodNameIndex.put(m.signature, count);
					break;
				}
			}
			in.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public APEXMethod getMethodBySubsignature(String subSig)
	{
		return methods.get(dexName+"->"+subSig);
	}
	
	
	public static final List<String> LibraryClasses = new ArrayList<String>(){
		private static final long serialVersionUID = 1L;
	{
			add("Lcom/google/*");
			add("Lgoogle/*");
			add("Lorg/apache/*");	
			add("Landroid/app/*");
			add("Landroid/support/*");
			add("Landroidx/*");
			add("Landroid/bluetooth/*");
			add("Landroid/webkit/*");
			add("Lcom/android/volley/*");
			add("Landroid/arch/*");
			
			add("Lcom/actionbarsherlock/*");
			add("Lcom/flurry/sdk/*");
			
			add("Lcom/facebook/*");
			add("Lcom/startapp/android/*");
			add("Lcom/bumptech/glide/*");
			add("Lcom/mopub/mobileads/VastVideoBlurLastVideoFrameTask;");
			add("Lcom/duapps/*");
			add("Lorg/kobjects/*");
			add("Lorg/ksoap2/*");
			add("Lorg/kxml2/*");
			add("Lorg/xmlpull/*");
			add("Lnet/mandaria/tippytipperlibrary/activities/Total$5;");
			add("Lorg/codehaus/*");
			add("Lorg/slf4j/*");
			add("Lcom/cloudrail/*");
			add("Lcom/github/clans/*");
			add("Lcom/github/junrar/*");
			add("Lcom/github/mikephil/*");
			add("Lcom/squareup/otto/*");
			
			add("Lcom/olav/logolicious/customize/datamodel/ImageExif;");
			add("Lcom/olav/logolicious/customize/widgets/LayersContainerView;");
			add("Lcom/anjlab/android/iab/v3/BillingCache;");
			add("Lcom/nineoldandroids/animation/AnimatorSet;");
			add("Lcom/nitramite/libraries/charts");
			add("Lcom/flask/colorpicker/ColorPickerView;");
			add("Lcom/github/gcacace/signaturepad/*");
			add("Lcom/revmob/ads/*");
			add("Lcom/sweetsugar/watermark/views/*");
			add("Ljp/co/cyberagent/android/gpuimage/OpenGlUtils;");
			add("Lcom/bhima/watermark/views/*");
			add("Lcom/onesignal/*");
			add("Lcom/ufotosoft/challenge/push/im/emoji/*");
			add("Lcom/cam001/gles/*");
			add("Lcom/ufotosoft/advanceditor/shop/*");
			add("Lcom/airbnb/*");
			add("Lcom/ufotosoft/ad/*");
			add("Lcom/mopub/*");
			add("Lcom/cyberlink/youperfect/jniproxy/*");
			add("Lcom/cyberlink/you/database/*");
			add("Lcom/pf/*");
			add("Lcom/cyberlink/youperfect/kernelctrl/gpuimage/*");
			add("Lcom/sina/*");
			add("Lcom/amazon/device/*");
			add("Lf5/*");
			add("Lsse/org/bouncycastle/*");
			add("Lca/repl/camopic/gl/*");
			add("Lcom/caverock/androidsvg/SVGAndroidRenderer;");
	}};
	
	public boolean isLibraryClass()
	{
		if (app.packageName.equals("com.amaze.filemanager"))
		{
			return !dexName.startsWith("Lcom/amaze/filemanager");
		}
		for (String c : LibraryClasses)
		{
			if (c.endsWith("*"))
			{
				if (dexName.startsWith(c.substring(0, c.lastIndexOf("*"))))
					return true;
			}
			else if (c.endsWith(";"))
			{
				if (dexName.equals(c))
					return true;
			}
		}
		return false;
	}

	private void readLinesUntil(List<String> list, String endLine)
	{
		try
		{
			String line;
			while ((line=in.readLine())!=null)
			{
				list.add(line);
				if (line.equals(endLine))
					return;
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public boolean instanceOf(APEXClass c, APEXApp app)
	{
		if (c.dexName.equals(superClassDexName))
			return true;
		if (app.classes.containsKey(superClassDexName) && app.classes.get(superClassDexName).instanceOf(c, app))
			return true;
		
		for (String interfaceName : interfaces)
		{
			if (app.classes.containsKey(interfaceName) && app.classes.get(interfaceName).instanceOf(c, app))
				return true;
		}
		return false;
	}
	
	public ArrayList<String> getInstrumentedBody()
	{
		ArrayList<String> result = new ArrayList<String>();
		result.add(this.declaration);
		result.add(".super " + superClassDexName);
		if (sourceFileName!=null && !sourceFileName.equals(""))
			result.add(".source \"" + this.sourceFileName + "\"");
		result.add("");
		if (interfaces.size()>0)
		{
			for (String s : interfaces)
				result.add(".implements " + s);
			result.add("");
		}
		if (this.annotations.size()>0)
		{
			for (List<String> s : this.annotations)
				result.addAll(s);
		}
		for (APEXField f : fields.values())
		{
			result.addAll(f.declarations);
			result.add("");
		}
/*		if (!addedFields.isEmpty())
		{
			result.add("# added fields");
			for (APEXField f : addedFields)
				result.add(f.getDeclaration());
			result.add("");
		}*/
		for (APEXMethod m : methods.values())
		{
			result.addAll(m.getInstrumentedBody());
			result.add("");
		}
		return result;
	}
}
