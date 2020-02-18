package ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsConfiguration;
import java.awt.Toolkit;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;

public class UI {

	
	public static Color randomColor()
	{
		Random rng = new Random();
		return new Color(rng.nextInt(256), rng.nextInt(256), rng.nextInt(256));
	}
	
	public static void setFont(Component c, int fontSize)
	{
		c.setFont(new Font(c.getFont().getName(), Font.PLAIN, fontSize));
	}
	
	
	public static void createAndShow(JPanel panel, String title)
	{
		try
		{
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
		catch (Exception e)
		{
            e.printStackTrace();
        }
		
		JFrame frame = new JFrame(title);

		final GraphicsConfiguration config = frame.getGraphicsConfiguration();
        final int left = Toolkit.getDefaultToolkit().getScreenInsets(config).left;
        final int right = Toolkit.getDefaultToolkit().getScreenInsets(config).right;
        final int top = Toolkit.getDefaultToolkit().getScreenInsets(config).top;
        final int bottom = Toolkit.getDefaultToolkit().getScreenInsets(config).bottom;
        final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        final int width = screenSize.width - left - right-20;
        final int height = screenSize.height - top - bottom-200;
        frame.setMinimumSize(new Dimension(width, height));
        //frame.setLocation(screenSize.width + 10, 100);
		frame.add(panel);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
		frame.pack();
	}
}
