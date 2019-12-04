package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicArrowButton;

import ui.UIWrappers.LargeLabel;
import util.Images;
import util.P;

@SuppressWarnings("serial")
public class SyncFixedScenes extends JPanel implements ActionListener{

	static final int NumRows = 12, NumDevices = 18;
	
	List<Device> devices;
	LargeLabel[] deviceLabels, sceneIndexLabels;
	ImageCell[][] imageCells;
	int startSceneIndex = 0;
	BasicArrowButton b_up, b_down;
	JTextField jumpFrom;
	JButton b_jump;
	
	SyncFixedScenes()
	{
		setBackground(Color.black);
		setLayout(new GridLayout(NumRows+1,NumDevices+1, 3, 10));
		
		// first row: Titles
		JPanel title_index = new JPanel(new GridLayout(0,1));
		title_index.add(new LargeLabel("Scene Index", 15));
		JPanel jump = new JPanel(new GridLayout(1,0));
		jumpFrom = new JTextField();
		b_jump = new JButton("go");
		b_jump.addActionListener(this);
		jump.add(jumpFrom);
		jump.add(b_jump);
		title_index.add(jump);
		JPanel up_down = new JPanel(new GridLayout(1,0));
		b_up = new BasicArrowButton(BasicArrowButton.NORTH);
		b_up.addActionListener(this);
		b_down = new BasicArrowButton(BasicArrowButton.SOUTH);
		b_down.addActionListener(this);
		up_down.add(b_up);
		up_down.add(b_down);
		title_index.add(up_down);
		add(title_index);
		
		deviceLabels = new LargeLabel[NumDevices];
		for (int i = 0; i < NumDevices; i++)
		{
			deviceLabels[i] = new LargeLabel("Device "+i, 15);
			add(deviceLabels[i]);
		}
		
		sceneIndexLabels = new LargeLabel[NumRows];
		imageCells = new ImageCell[NumRows][NumDevices];
		for (int i = 0; i < NumRows; i++)
		{
			sceneIndexLabels[i] = new LargeLabel("Scene "+i, 15);
			add(sceneIndexLabels[i]);
			for (int j = 0; j < NumDevices; j++)
			{
				imageCells[i][j] = new ImageCell(j, i);
				add(imageCells[i][j]);
			}
			final int k = i;
			sceneIndexLabels[i].addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e)
				{
					for (int j = 0; j < NumDevices; j++)
					{
						if (imageCells[k][j]!=null)
						{
							JButton b = imageCells[k][j].b_del;
							if (b != null)
							{
								//b.setEnabled(!b.isEnabled());
								b.setVisible(!b.isVisible());
							}
						}
					}
				}
			});
		}
		initDevices();
		startSceneIndex = 0;
		updateTitles();
		updateThumbs();
	}
	
	@Override
	public void actionPerformed(ActionEvent e)
	{
		Object src = e.getSource();
		if (src == b_up)
		{
			startSceneIndex = Math.max(0, startSceneIndex-1);
			updateThumbs();
		}
		else if (src == b_down)
		{
			startSceneIndex++;
			updateThumbs();
		}
		else if (src == b_jump)
		{
			startSceneIndex = Math.max(0, Integer.parseInt(jumpFrom.getText().trim()));
			updateThumbs();
		}
	}
	
	private void updateTitles()
	{
		for (int i = 0; i < devices.size(); i++)
			deviceLabels[i].setText(devices.get(i).name);
	}
	
	private void updateThumbs()
	{
		new Thread(new Runnable() {
			@Override
			public void run()
			{
				for (int i = 0; i < NumRows; i++)
				{
					int sceneIndex = i+startSceneIndex;
					sceneIndexLabels[i].setText("Scene "+sceneIndex);
					for (int j = 0; j < NumDevices; j++)
					{
						Device d = devices.get(j);
						ImageCell cell = imageCells[i][j];
						if (sceneIndex>=0 && sceneIndex < d.scenes.size())
							cell.setScene(d.scenes.get(sceneIndex));
						else
							cell.setScene(null);
					}
				}
			}
		}).start();
		

	}
	
	private void initDevices()
	{
		devices = new ArrayList<>();
		File root = new File("G:/TEMP_18_Phones_Fixed_Scenes_Data");
		for (File dir : root.listFiles())
		{
			if (dir.getName().equals("5dngs"))
				continue;
			Device device = new Device(dir);
			devices.add(device);
		}
	}
	
	static class Device {
		String name;
		File oriDir, dngDir, tifDir, natDir, badDir;
		List<Scene> scenes;
		List<File> nativeImages;
		Device(File root)
		{
			name = root.getName();
			oriDir = new File(root, "originals");
			dngDir = new File(oriDir, "DNG");
			tifDir = new File(dngDir, "TIFF");
			natDir = new File(root, "Native");
			badDir = new File(root, "bad");		badDir.mkdirs();
			P.pf("%s jpg = %d, dng = %d, tif = %d, nat = %d, bad = %d",
					name, oriDir.list().length, dngDir.list().length, tifDir.list().length, 
					natDir.list().length, badDir.list().length);
			
			Map<String, Scene> ss = new TreeMap<>();
			collectScenes(ss, this, oriDir, dngDir, tifDir);
			scenes = new ArrayList<>(ss.values());
			for (Scene s : scenes) s.validate();
			nativeImages = new ArrayList<>();
			for (File nat : natDir.listFiles())
				nativeImages.add(nat);
		}
	}
	
	private static void collectScenes(Map<String, Scene> ss, Device d, File oriDir, File dngDir, File tifDir)
	{
		for (File jpg : oriDir.listFiles())
		{
			if (jpg.getName().endsWith(".jpg"))
			{
				String sceneName = jpg.getName().split("_")[1];
				ss.putIfAbsent(sceneName, new Scene(d, sceneName));
				ss.get(sceneName).jpgs.add(jpg);
			}
		}
		for (File dng : dngDir.listFiles())
		{
			if (dng.getName().endsWith(".dng"))
			{
				String sceneName = dng.getName().split("_")[1];
				ss.putIfAbsent(sceneName, new Scene(d, sceneName));
				ss.get(sceneName).dngs.add(dng);
			}
		}
		for (File tif : tifDir.listFiles())
		{
			if (tif.getName().endsWith(".tif"))
			{
				String sceneName = tif.getName().split("_")[1];
				ss.putIfAbsent(sceneName, new Scene(d, sceneName));
				ss.get(sceneName).tifs.add(tif);
			}
		}
	}
	
	static class Scene {
		Device d;
		String name;
		List<File> jpgs = new ArrayList<>();
		List<File> dngs = new ArrayList<>();
		List<File> tifs = new ArrayList<>();
		Scene(Device d, String s) {this.d = d; name = s;}
		void validate(){
			if (jpgs.size()!=10) P.p("  "+name+" jpg not 10");
			if (dngs.size()!=10) P.p("  "+name+" dng not 10");
			if (tifs.size()!=10) P.p("  "+name+" tif not 10");
		}
		
		BufferedImage getAutoJPGThumb()
		{
			File jpg = jpgs.get(0);
			File thumb = new File("E:/temp_thumbs/"+d.name+"_"+jpg.getName());
			if (!thumb.exists())
				return Images.scale(jpg, thumb);
			return Images.loadImage(thumb);
		}
		
		void moveToBad()
		{
			new Thread(new Runnable(){
				@Override
				public void run()
				{
					for (File jpg : jpgs)
						jpg.renameTo(new File(d.badDir, jpg.getName()));
					for (File dng : dngs)
						dng.renameTo(new File(d.badDir, dng.getName()));
					for (File tif : tifs)
						tif.renameTo(new File(d.badDir, tif.getName()));
				}
			}).start();
			
		}
	}
	
	class ImageCell extends JPanel implements ActionListener{
		Image image;
		Scene s;
		JButton b_del;
		int deviceIndex, sceneIndex;
		
		ImageCell(int deviceIndex, int sceneIndex)
		{
			this.deviceIndex = deviceIndex;
			this.sceneIndex = sceneIndex;
			setBackground(Color.YELLOW);
			setLayout(new BorderLayout());
			JPanel bottom = new JPanel();
			bottom.setOpaque(false);
			b_del = new JButton("X");
			b_del.setFont(new Font(b_del.getFont().getFontName(), Font.PLAIN, 15));
			b_del.setOpaque(false);
			b_del.setBorderPainted(false);
			b_del.setForeground(Color.red);
			b_del.addActionListener(this);
			bottom.add(b_del);
			b_del.setVisible(false);
			add(bottom, BorderLayout.SOUTH);
		}
		public void setScene(Scene s)
		{
			this.s = s;
			this.image = null;
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run()
				{
					repaint();
				}
			});
			if (s!=null)
			{
				this.image = s.getAutoJPGThumb();
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run()
					{
						repaint();
					}
				});
			}
		}
		
		@Override
		protected void paintComponent(Graphics g)
		{
			super.paintComponent(g);
			if (this.image!=null)
				g.drawImage(this.image, 
						0, 0, getWidth(), getHeight(), 
						0, 0, this.image.getWidth(null), this.image.getHeight(null), null);
		}
		@Override
		public void actionPerformed(ActionEvent e)
		{
			Object src = e.getSource();
			if (src == b_del)
			{
				s.moveToBad();
				s.d.scenes.remove(s);
				updateThumbs();
			}
		}
	}
	
	public static void main(String[] args)
	{
		UI.createAndShow(new SyncFixedScenes(), "Sync Fixed Scenes");
	}



}
