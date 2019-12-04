package apex.instrument;

import java.io.File;
import java.io.PrintWriter;

import util.F;
import util.P;

public class Logger {

	public static final String path = "wenhaoc/apex/APEXLogger.smali";
	public static final String className = "Lwenhaoc/apex/APEXLogger;";
	public static final String CommentTag = "# wenhao instrumentation";
	public static final String LogTag = "wenhaoc_apex_log";
	
	
	public static String getMethodStartStatement(int index)
	{
		return getInvokeStatement("method_start", index);
	}
	
	public static String getMethodEndStatement(int index)
	{
		return getInvokeStatement("method_end", index);
	}
	
	public static String getInvokeStatement(String keyword, int index)
	{
		if (index < 1)
		{
			P.e("Trying to instrumenting "+keyword+" with negative index??");
			new Throwable().printStackTrace();
			P.pause();
		}
		
		return "    invoke-static {}, "+className+"->"+keyword+"_"+index+"()V  "+CommentTag;
	}

	public static void writeSmaliClass(File loggerClassF, int loggingMethodCount)
	{
		loggerClassF.getParentFile().mkdirs();
		PrintWriter out = F.initPrintWriter(loggerClassF);
		
		out.println(".class public "+className);
		out.println(".super Ljava/lang/Object;");
		
		out.println();
		out.println(".method public constructor <init>()V");
		out.println("    .locals 0");
		out.println("    .prologue");
		out.println("    invoke-direct {p0}, Ljava/lang/Object;-><init>()V");
		out.println("    return-void");
		out.println(".end method");
		
		for (int i = 1; i <= loggingMethodCount; i++)
		{
			writeMethod(out, "method_start", i);
			writeMethod(out, "method_end", i);
		}
		out.close();
	}
	
	private static void writeMethod(PrintWriter out, String keyword, int index)
	{
		String methodName = keyword+"_"+index;
		out.println();
		out.println(".method public static "+methodName+"()V");
		out.println("    .locals 7");
		out.println("    new-instance v0, Ljava/lang/Throwable;");
		out.println("    invoke-direct {v0}, Ljava/lang/Throwable;-><init>()V");
		out.println("    invoke-virtual {v0}, Ljava/lang/Throwable;->getStackTrace()[Ljava/lang/StackTraceElement;");
		out.println("    move-result-object v0");
		out.println();
		out.println("    array-length v1, v0");
		out.println("    const/4 v2, 0x1");
		out.println("    if-gt v1, v2, :cond_0");
		out.println();
		out.println("    const-string v1, \""+LogTag+"\"");
		out.println("    new-instance v2, Ljava/lang/StringBuilder;");
		out.println("    invoke-direct {v2}, Ljava/lang/StringBuilder;-><init>()V");
		out.println("    const-string v3, \"APEXLogger.LogI encountering StackTrace size \"");
		out.println("    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;");
		out.println("    array-length v3, v0");
		out.println("    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;");
		out.println("    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;");
		out.println("    move-result-object v2");
		out.println("    invoke-static {v1, v2}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I");
		out.println();
		out.println("    goto :goto_0");
		out.println();
		out.println("    :cond_0");
		out.println("    aget-object v1, v0, v2");
		out.println("    invoke-virtual {v1}, Ljava/lang/StackTraceElement;->getClassName()Ljava/lang/String;");
		out.println("    move-result-object v2");
		out.println("    invoke-virtual {v1}, Ljava/lang/StackTraceElement;->getMethodName()Ljava/lang/String;");
		out.println("    move-result-object v3");
		out.println("    new-instance v4, Ljava/lang/StringBuilder;");
		out.println("    const-string v5, \""+methodName+": \"");
		out.println("    invoke-direct {v4, v5}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V");
		out.println("    invoke-virtual {v4, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;");
		out.println("    const-string v5, \"->\"");
		out.println("    invoke-virtual {v4, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;");
		out.println("    invoke-virtual {v4, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;");
		out.println("    const-string v5, \""+LogTag+"\"");
		out.println("    invoke-virtual {v4}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;");
		out.println("    move-result-object v6");
		out.println("    invoke-static {v5, v6}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I");
		out.println();
		out.println("    :goto_0");
		out.println("    return-void");
		out.println(".end method");
	}
}
