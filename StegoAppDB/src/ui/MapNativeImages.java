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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicArrowButton;

import ui.UIWrappers.LargeLabel;
import util.F;
import util.Images;
import util.P;

@SuppressWarnings("serial")
public class MapNativeImages extends JPanel implements ActionListener{

	static final int NumRows = 12, NumDevices = 18;
	
	
	List<Device> devices;
	LargeLabel[] titleLabels, indexLabels;
	ImageCell[] camerawCells;
	ImageCell[][] nativeCells;
	int startSceneIndex = 0;
	
	JTextField jumpTo;
	BasicArrowButton b_jump, b_up, b_down;
	
	static File root = 
			//new File("E:/stegodb_FixedScenes");
			new File("G:/TEMP_18_Phones_Fixed_Scenes_Data");
	static File recordDir = 
			//new File("E:/TEMP_Cameraw_Native_Matching");
			new File("G:/TEMP_Cameraw_Native_Matching");
	
	MapNativeImages()
	{
		LargeLabel.defaultSize = 15;
		setLayout(new GridLayout(NumRows+1, NumDevices+2, 2, 8));
		setBackground(Color.black);
		
		JPanel firstCell = new JPanel(new GridLayout(0,1));
		firstCell.add(new LargeLabel("Scene Index"));
		JPanel jump = new JPanel(new GridLayout(1,0));
		jumpTo = new JTextField();
		b_jump = new BasicArrowButton(BasicArrowButton.EAST);
		b_jump.addActionListener(this);
		jump.add(jumpTo);
		jump.add(b_jump);
		firstCell.add(jump);
		JPanel up_down = new JPanel(new GridLayout(1,0));
		b_up = new BasicArrowButton(BasicArrowButton.NORTH);
		b_up.addActionListener(this);
		b_down = new BasicArrowButton(BasicArrowButton.SOUTH);
		b_down.addActionListener(this);
		up_down.add(b_up);
		up_down.add(b_down);
		firstCell.add(up_down);
		
		add(firstCell);
		add(new LargeLabel("Cameraw"));
		titleLabels = new LargeLabel[NumDevices];
		for (int i = 0; i < NumDevices; i++)
		{
			titleLabels[i] = new LargeLabel("Device "+i);
			add(titleLabels[i]);
		}
		
		indexLabels = new LargeLabel[NumRows];
		camerawCells = new ImageCell[NumRows];
		nativeCells = new ImageCell[NumRows][NumDevices];
		for (int i = 0; i < NumRows; i++)
		{
			final int k = i;
			JPanel indexCell = new JPanel(new GridLayout(0,1));
			
			indexLabels[i] = new LargeLabel("Scene "+i);
			indexCell.add(indexLabels[i]);
			
			JButton b_toggle = new JButton("toggle");
			b_toggle.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e)
				{
					for (int j = 0; j < NumDevices; j++)
						nativeCells[k][j].toggleButtons();
				}
			});
			indexCell.add(b_toggle);
			JButton b_save = new JButton("save");
			b_save.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e)
				{
					P.p("from 0 to "+(startSceneIndex+k));
					for (int i = 0; i < NumDevices; i++)
					{
						Device d = devices.get(i);
						for (int j = 0; j <= startSceneIndex+k && j < d.scenes.size(); j++)
						{
							File nat = j<d.nativeImages.size()?d.nativeImages.get(j):null;
							String nativeName = nat==null?"null":nat.getName();
							d.map.put(d.scenes.get(j).name, nativeName);
						}
						d.saveRecord();
					}
					for (int i = 0; i <= k; i++)
						indexLabels[i].setBackground(Color.GREEN);
				}
			});
			indexCell.add(b_save);
			add(indexCell);
			
			camerawCells[i] = new ImageCell(false, i, 0);
			add(camerawCells[i]);
			for (int j = 0; j < NumDevices; j++)
			{
				nativeCells[i][j] = new ImageCell(true, i, j);
				add(nativeCells[i][j]);
			}

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
			startSceneIndex = Integer.parseInt(jumpTo.getText().trim());
			updateThumbs();
		}
	}
	
	void updateTitles()
	{
		for (int i = 0; i < NumDevices; i++)
			titleLabels[i].setText(devices.get(i).name);
	}
	
	void updateThumbs()
	{
		new Thread(new Runnable() {
			@Override
			public void run()
			{
				Device phone0 = devices.get(0);
				for (int i = 0; i < NumRows; i++)
				{
					int sceneIndex = i+startSceneIndex;
					indexLabels[i].setText("Scene " + sceneIndex);
					if (sceneIndex>=0 && sceneIndex<phone0.scenes.size())
					{
						Scene s = phone0.scenes.get(sceneIndex);
						camerawCells[i].setScene(s);
						if (phone0.map.containsKey(s.name))
						{
							indexLabels[i].setBackground(Color.green);
						}
						else
						{
							indexLabels[i].setBackground(Color.YELLOW);
						}
					}
					else
						camerawCells[i].setScene(null);
					for (int j = 0; j < NumDevices; j++)
					{
						Device d = devices.get(j);
						if (sceneIndex>=0 && sceneIndex<d.nativeImages.size())
						{
							File nat = d.nativeImages.get(sceneIndex);
							nativeCells[i][j].setImage(nat, d.name);
						}
						else
							nativeCells[i][j].setImage(null, d.name);
					}
				}
			}
		}).start();
	}
	
	class ImageCell extends JPanel implements ActionListener{
		
		boolean isNative;
		private Image image;
		File nativeImageF;
		int rowIndex, deviceIndex;
		
		JButton b_del, b_insert;
		ImageCell(boolean isNative, int rowIndex, int deviceIndex)
		{
			this.isNative = isNative;
			this.rowIndex = rowIndex;
			this.deviceIndex = deviceIndex;
			setBackground(Color.GRAY);
			setLayout(new BorderLayout());
			if (!isNative)
				return;
			
			b_del = new JButton("X");
			adjustButton(b_del, Color.RED);
			b_insert = new JButton("+");
			adjustButton(b_insert, Color.GREEN);
			
			JPanel buttons = new JPanel(new GridLayout(1,0));
			buttons.add(b_del);
			buttons.add(b_insert);
			buttons.setOpaque(false);
			add(buttons, BorderLayout.SOUTH);
			
			this.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e)
				{
					toggleButtons();
				}
			});
		}
		
		void showButtons()
		{
			if (b_del != null) b_del.setVisible(true);
			if (b_insert != null) b_insert.setVisible(true);
		}
		
		void hideButtons()
		{
			if (b_del != null) b_del.setVisible(false);
			if (b_insert != null) b_insert.setVisible(false);
		}
		
		void toggleButtons()
		{
			if (b_del != null) b_del.setVisible(!b_del.isVisible());
			if (b_insert != null) b_insert.setVisible(!b_insert.isVisible());
		}
		
		@Override
		public void actionPerformed(ActionEvent e)
		{
			Object src = e.getSource();
			if (src == b_del)
			{
				File f = devices.get(deviceIndex).nativeImages.remove(rowIndex+startSceneIndex);
				updateThumbs();
				if (f!=null)
				{
					P.p("moving "+nativeImageF.getAbsolutePath()+" to the bad folder");
					File newF = new File(devices.get(deviceIndex).badDir, f.getName());
					f.renameTo(newF);
				}
				hideButtons();
			}
			else if (src == b_insert)
			{
				devices.get(deviceIndex).nativeImages.add(rowIndex+startSceneIndex, null);
				updateThumbs();
				hideButtons();
			}
		}
		
		void adjustButton(JButton b, Color c)
		{
			b.setFont(new Font(b.getFont().getFontName(), Font.PLAIN, 20));
			b.setOpaque(false);
			b.setBorderPainted(false);
			b.setForeground(c);
			b.addActionListener(this);
			b.setVisible(false);
		}
		
		void setImage(File f, String deviceName)
		{
			nativeImageF = f;
			image = null;
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run()
				{
					repaint();
				}
			});
			
			if (f != null)
			{
				File thumb = getThumbFile(f, deviceName);
				if (thumb.exists())
					image = Images.loadImage(thumb);
				else
					image = Images.scale(f, thumb);
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run()
					{
						repaint();
					}
				});
			}
		}
		
		void setScene(Scene s)
		{
			image = null;
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run()
				{
					repaint();
				}
			});
			if (s != null)
			{
				image = s.getAutoJPGThumb();
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


	}
	
	
	private void initDevices()
	{
		devices = new ArrayList<>();
		
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
		File oriDir, dngDir, tifDir, natDir, badDir, mapRecordF;
		List<Scene> scenes;
		List<File> nativeImages;
		Map<String, String> map;
		Device(File root)
		{
			name = root.getName();
			oriDir = new File(root, "originals");
			dngDir = new File(oriDir, "DNG");
			tifDir = new File(dngDir, "TIFF");
			natDir = new File(root, "Native");
			badDir = new File(root, "bad");		badDir.mkdirs();
			mapRecordF = new File(recordDir, name+".csv");
			
			
			Map<String, Scene> ss = new TreeMap<>();
			collectScenes(ss, this, oriDir, dngDir, tifDir);
			scenes = new ArrayList<>(ss.values());
			for (Scene s : scenes) s.validate();
			
			nativeImages = new ArrayList<>();
			map = new TreeMap<String, String>();
			
			List<File> allNatImages = new ArrayList<>();
			for (File f : natDir.listFiles())
				if (f.getName().endsWith(".jpg"))
					allNatImages.add(f);
			
/*			Collections.sort(allNatImages, new Comparator<File>() {
				@Override
				public int compare(File o1, File o2)
				{
					long l1 = o1.lastModified();
					long l2 = o2.lastModified();
					if (l1>l2)
						return 1;
					else if (l1==l2)
						return 0;
					else return -1;
				}
			});*/
			
			int index = 0;
			List<String> record = F.readLinesWithoutEmptyLines(mapRecordF);
			for (String line : record)
			{
				String[] parts = line.split(",");
				map.put(parts[0], parts[1]);
				if (parts[1].equals("null"))
					nativeImages.add(null);
				else
					nativeImages.add(allNatImages.get(index++));
			}
			P.pf("%s jpg = %d, dng = %d, tif = %d, nat = %d, bad = %d, matched = %d,",
					name, oriDir.list().length, dngDir.list().length, tifDir.exists()?tifDir.list().length:0, 
					natDir.list().length, badDir.list().length, index);
			while (index < allNatImages.size())
				nativeImages.add(allNatImages.get(index++));
			
			
		}
		
		void saveRecord()
		{
			mapRecordF.delete();
			for (String name : map.keySet())
				F.writeLine(name+","+map.get(name), mapRecordF, true);
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
		if (tifDir.exists())
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
			//if (tifs.size()!=10) P.p("  "+name+" tif not 10");
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
	
	private static File getThumbFile(File f, String deviceName)
	{
		return new File("E:/temp_thumbs/"+deviceName+"_"+f.getName());
	}
	
	
	public static void main(String[] args)
	{
		UI.createAndShow(new MapNativeImages(), "Mapping Cameraw and Native Images");
	}

}
