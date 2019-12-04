package ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Random;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import util.Images;

@SuppressWarnings("serial")
public class UIWrappers {
	


	static class GBC extends GridBagConstraints {}
	
	public static class LargeLabel extends JLabel{

		public static int defaultSize = 20;
		public static Color defaultBackgroundColor = Color.YELLOW;
		public static int alignment = SwingConstants.CENTER;
		
		public LargeLabel(String text)
		{
			this(text, defaultSize);
		}
		
		public LargeLabel(String text, int fontSize)
		{
			this(text, Font.PLAIN, fontSize);
		}
		
		public LargeLabel(String text, boolean bold)
		{
			this(text, bold?Font.BOLD:Font.PLAIN, defaultSize);
		}
		
		public LargeLabel(String text, int style, int fontSize)
		{
			super(text);
			
			this.setFont(new Font(getFont().getFontName(), style, fontSize));
			this.setBackground(defaultBackgroundColor);
			this.setOpaque(true);
			this.setHorizontalAlignment(alignment);
		}
		
		public void setAlpha(double alpha)
		{
			Color c = this.getBackground();
			Color newC = new Color(c.getRed(), c.getGreen(), c.getBlue(), (int)(255*alpha));
			this.setBackground(newC);
		}
		
		public void setBackground(Color c, double alpha)
		{
			this.setBackground(c);
			this.setAlpha(alpha);
		}
	}
	
	public static class ImageView extends JPanel {
		private BufferedImage image;
		//= Images.loadImage(new File("C:/Users/C03223-Stego2/Downloads/iPhone6sPlus-1_Scene-20180701-132807_JPG-00_I50_E30_o.jpg"));
		public boolean drawBorder = false;
		public boolean maintainAspectRatio = true;
		public int imageMargin = 5;
		public JLabel label;
		
		public ImageView(File f)
		{
			if (f != null)
				image = Images.loadImage(f);
		}
		
		public void setLabel(JLabel label)
		{
			this.removeAll();
			this.add(label);
			this.label = label;
		}
		
		public void setImage(BufferedImage image)
		{
			this.image = image;
		}
		
		@Override
		protected void paintComponent(Graphics gg)
		{
			super.paintComponent(gg);
			Graphics2D g = (Graphics2D)gg;
			int labelX = 0, labelY = 0;
			if (image != null)
			{
				if (maintainAspectRatio)
				{
					double w0 = getWidth();
					double h0 = getHeight();
					double h1 = image.getHeight()-2*imageMargin;
					double w1 = image.getWidth()*(h0/h1);
					g.drawImage(image, (int)(w0-w1)/2, imageMargin, (int)w1, (int)h0, this);
					labelX = (int)(w0-w1)/2;
					labelY = imageMargin;
				}
				else
				{
					g.drawImage(image, imageMargin, imageMargin, getWidth()-imageMargin, getHeight()-imageMargin, this);
					labelX = labelY = imageMargin;
				}
			}
			setLabelLocation(labelX, labelY);
			if (drawBorder)
			{
				g.setStroke(new BasicStroke(6));
				g.setColor(Color.GREEN);
				g.drawRect(0, 0, getWidth(), getHeight());
			}
		}
		
		private void setLabelLocation(int x, int y)
		{
			if (label != null)
			{
				label.setLocation(x, y);
			}
		}
	}
	
}
