package database;

import java.io.File;

import util.P;

public class OneHundredIPhones {

	
	public static void main(String[] args)
	{
		File root = new File("E:/100iphone7");
		
		for (File d : root.listFiles())
		{
			int count = d.list().length;
			P.p(d.getName()+ " "+count);
		}
	}
}
