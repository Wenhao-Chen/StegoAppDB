package ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsConfiguration;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;

public class UI {

	
	public static void showImage(File f)
	{
		try
		{
			BufferedImage img = ImageIO.read(f);
			ImageIcon icon=new ImageIcon(img);
	        JFrame frame=new JFrame();
	        frame.setLayout(new FlowLayout());
	        frame.setSize(600,600);
	        JLabel lbl=new JLabel();
	        lbl.setIcon(icon);
	        frame.add(lbl);
	        frame.setVisible(true);
	        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public static void showImage(File... fs)
	{
        JFrame frame=new JFrame();
        frame.setLayout(new FlowLayout());
        frame.setSize(1400,600);
		try
		{
			for (File f : fs)
			{
				BufferedImage img = ImageIO.read(f);
				ImageIcon icon=new ImageIcon(img);
		        JLabel lbl=new JLabel();
		        lbl.setIcon(icon);
		        frame.add(lbl);
			}
			
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
	
	
	public static Color randomColor()
	{
		Random rng = new Random();
		return new Color(rng.nextInt(256), rng.nextInt(256), rng.nextInt(256));
	}
	
	public static void setFont(Component c, int fontSize)
	{
		c.setFont(new Font(c.getFont().getName(), Font.PLAIN, fontSize));
	}
	
	
	public static JFrame createAndShow(JPanel panel, String title)
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
		return frame;
	}
}
