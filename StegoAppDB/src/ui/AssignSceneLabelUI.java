package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.Iterator;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import database.objects.MainDB;
import ui.UIWrappers.ImageView;
import ui.UIWrappers.LargeLabel;
import util.P;

@SuppressWarnings("serial")
public class AssignSceneLabelUI extends JPanel implements KeyListener, MouseWheelListener{

	ImageView imageView;
	LargeLabel sceneTitle;
	
	Thumbnails thumbs;
	LeftPanel leftPanel;
	RightPanel rightPanel;
	LargeLabel[] devices;
	
	MainDB db;
	
	
	
	AssignSceneLabelUI()
	{
		setLayout(new BorderLayout());
		this.setBackground(Color.DARK_GRAY);
		
		imageView = new ImageView(null);
		sceneTitle = new LargeLabel("Scene Information");
		imageView.setLabel(sceneTitle);
		thumbs = new Thumbnails();
		leftPanel = new LeftPanel();
		rightPanel = new RightPanel();
		
		add(imageView, BorderLayout.CENTER);
		add(thumbs, BorderLayout.PAGE_END);
		add(leftPanel, BorderLayout.LINE_START);
		add(rightPanel, BorderLayout.LINE_END);
		
		addKeyListener(this);
		addMouseWheelListener(this);
		setFocusable(true);
		
		init();
	}
	
	class LeftPanel extends JPanel {
		
		LeftPanel()
		{
			setBackground(UI.randomColor());
			setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		}
		@Override
		public Dimension getPreferredSize()
		{
			return new Dimension(300, getHeight());
		}
	}

	class RightPanel extends JPanel {
		
		RightPanel()
		{
			setBackground(UI.randomColor());
			this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
			add(Box.createRigidArea(new Dimension(1, 20)));
			LargeLabel title = new LargeLabel("Controls");
			add(title);
		}

		@Override
		public Dimension getPreferredSize()
		{
			return new Dimension(300, getHeight());
		}
	}
	
	class Thumbnails extends JPanel {
		private int currentThumbIndex;
		static final int numThumb = 10;
		ImageView[] thumbs;
		
		Thumbnails()
		{
			this.setBackground(Color.BLACK);
			this.setMinimumSize(new Dimension(100, 180));
			currentThumbIndex = 0;
			GridLayout l = new GridLayout(1, numThumb);
			l.setHgap(10);
			l.setVgap(5);
			this.setLayout(l);
			thumbs = new ImageView[numThumb];
			for (int i = 0; i < numThumb; i++)
			{
				thumbs[i] = new ImageView(null);
				thumbs[i].setBackground(UI.randomColor());
				this.add(thumbs[i]);
			}
		}
		
		@Override
		public Dimension getPreferredSize()
		{
			return new Dimension(getWidth(), 150);
		}

		
		
		
		public void nextThumb()
		{
			if (currentThumbIndex >= numThumb)
				return;
			currentThumbIndex++;
			for (ImageView thumb : thumbs)
				thumb.drawBorder = false;
			if (currentThumbIndex >= 0 && currentThumbIndex < thumbs.length)
				thumbs[currentThumbIndex].drawBorder = true;
			this.repaint();
		}
		public void prevThumb()
		{
			if (currentThumbIndex <= 0)
				return;
			currentThumbIndex--;
			for (ImageView thumb : thumbs)
				thumb.drawBorder = false;
			if (currentThumbIndex >= 0 && currentThumbIndex < thumbs.length)
				thumbs[currentThumbIndex].drawBorder = true;
			this.repaint();
		}
	}
	
	
	private void init()
	{
		new Thread(new Runnable() {
			@Override
			public void run()
			{
				db = new MainDB();
				devices = new LargeLabel[db.devicesWithValidatedSceneContent.size()];
				Iterator<String> it = db.devicesWithValidatedSceneContent.iterator();
				
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run()
					{
						for (int i = 0; i < devices.length; i++)
						{
							devices[i] = new LargeLabel(it.next());
							leftPanel.add(devices[i]);
							devices[i].setAlignmentX(0.8f);
						}
						leftPanel.revalidate();
					}
				});
			}
		}).start();
	}
	
	void selectDevice(String name)
	{
		//TODO
	}
	
	
	
	@Override
	public void keyPressed(KeyEvent e) {
		String keyText = KeyEvent.getKeyText(e.getKeyCode());
		P.p(keyText);
		if (keyText.equals("Right"))
		{
			thumbs.nextThumb();
		}
		else if (keyText.equals("Left"))
		{
			thumbs.prevThumb();
		}
		else if (keyText.equals("1"))
		{
			//TODO
		}
		else if (keyText.equals("2"))
		{
			//TODO
		}
	}
	
	@Override
	public void mouseWheelMoved(MouseWheelEvent e)
	{
		int rotation = e.getWheelRotation();
		if (rotation < 0)
			thumbs.prevThumb();
		else if (rotation > 0)
			thumbs.nextThumb();
	}
	
	
	
	public static void main(String[] args)
	{
		UI.createAndShow(new AssignSceneLabelUI(), "Assign Scene Label");
	}
	




	@Override
	public void keyReleased(KeyEvent e) {}

	@Override
	public void keyTyped(KeyEvent e) {}




}
