package crawling;

import java.io.File;

import util.F;
import util.P;

public class Github_DownloadRepos {

	
	
	public static void main(String[] args)
	{
		for (String repo : F.readLinesWithoutEmptyLines(new File("E:/repos.txt")))
		{
			String[] parts = repo.split("/");
			String dirName = parts[parts.length-2]+"_"+parts[parts.length-1];
			File dir = new File("E:/github_steg", dirName);
			P.exec("git clone "+repo+".git "+dir.getAbsolutePath(), true, true).destroy();
			if (!dir.exists())
			{
				P.p("??? "+dir.getAbsolutePath());
				P.pause();
			}
			F.writeLine("github page: "+repo, new File(dir, "repo_page.txt"), false);
		}
	}
}
