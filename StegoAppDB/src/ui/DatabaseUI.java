package ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import database.objects.DBDevice;
import database.objects.DBScene;
import database.objects.MainDB;
import ui.UIWrappers.GBC;
import ui.UIWrappers.LargeLabel;
import util.P;

@SuppressWarnings("serial")
public class DatabaseUI extends JTabbedPane{
	


	private DatabaseUI()
	{
		this.setTabPlacement(JTabbedPane.LEFT);
		this.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		this.setFont(new Font("Arial", Font.PLAIN, 20));
		this.addTab("Overview", new OverviewPanel(this));
	}
	
	class OverviewPanel extends JPanel implements ActionListener{

		private LargeLabel[] logs;
		private JButton b_count, b_validate, b_PNG, b_temp;
		private MainDB db;
		OverviewPanel(DatabaseUI parent)
		{
			this.setLayout(new GridBagLayout());
			this.setBackground(Color.GRAY);
			
			JPanel logArea = new JPanel();
			logArea.setLayout(new BoxLayout(logArea, BoxLayout.PAGE_AXIS));
			logs = new LargeLabel[28];
			for (int i = 0; i < logs.length; i++)
			{
				logs[i] = new LargeLabel("Line "+(i+1));
				logArea.add(logs[i]);
			}
			
			b_count = new JButton("Count");
			b_count.setFont(new Font(b_count.getFont().getName(), Font.PLAIN, 20));
			b_count.addActionListener(this);
			
			b_validate = new JButton("Validate");
			b_validate.setFont(new Font(b_validate.getFont().getName(), Font.PLAIN, 20));
			b_validate.addActionListener(this);

			b_PNG = new JButton("Make PNGs");
			b_PNG.setFont(new Font(b_PNG.getFont().getName(), Font.PLAIN, 20));
			b_PNG.addActionListener(this);
			
			b_temp = new JButton("Temp");
			b_temp.setFont(new Font(b_temp.getFont().getName(), Font.PLAIN, 20));
			b_temp.addActionListener(this);

			JPanel controlArea = new JPanel();
			controlArea.add(b_count);
			controlArea.add(b_validate);
			controlArea.add(b_PNG);
			controlArea.add(b_temp);
			controlArea.setBackground(this.getBackground());
			
			
			GBC c = new GBC();
			c.gridx = c.gridy = 0;
			c.weightx = c.weighty = 0;
			c.gridwidth = c.gridheight = 1;
			c.fill = GBC.BOTH;
			c.anchor = GBC.NORTH;
			add(logArea, c);
			
			c.fill = GBC.NONE;
			c.gridy = 1; c.gridx = 0;
			add(controlArea, c);
			
			new Thread(new Runnable() {
				@Override
				public void run()
				{
					db = new MainDB();
					for (DBDevice device : db.devices)
					{
						parent.addTab(device.name, new DevicePanel(device));
					}
				}
			}).start();
		}
		@Override
		public void actionPerformed(ActionEvent arg0)
		{
			Component src = (Component)arg0.getSource();
			src.setEnabled(false);
			
			new Thread(new Runnable() {
				@Override
				public void run()
				{
					if (db == null)
						return;
					for (int i = 0; i < db.devices.size(); i++)
					{
						logs[i].setText(String.format("%15s", db.devices.get(i).name)+": ");
					}
					
					if (src == b_count)
					{
						for (int i = 0; i < db.devices.size(); i++)
						{
							DBDevice device = db.devices.get(i);
							logs[i].setText(String.format("%15s", device.name)+": "+device.getScenes().size()+" scenes.");
						}
						P.p("Done.");
					}
					else if (src == b_validate)
					{
						for (int i = 0; i < db.devices.size(); i++)
						{
							DBDevice device = db.devices.get(i);
							logs[i].setText(String.format("%15s", device.name)+": validating...");
							device.validateAll();
							logs[i].setText(String.format("%15s", device.name)+": finished validating.");
						}
						P.p("Done.");
					}
					else if (src == b_PNG)
					{
						try
						{
							PrintWriter out = new PrintWriter(new FileWriter("E:/matlab_rgb2gray_jobs_"+P.getTimeString()+".txt"));
							for (int i = 0; i < db.devices.size(); i++)
							{
								DBDevice device = db.devices.get(i);
								logs[i].setText(String.format("%15s", device.name)+": generating color PNGs");
								int matlabJobCount = device.makePNGs(out);
								logs[i].setText(String.format("%15s", device.name)+": matlab job count: "+matlabJobCount);
							}
							out.close();
							P.p("Done.");
						}
						catch (Exception e)
						{
							e.printStackTrace();
						}
						
					}
					else if (src == b_temp)
					{
						for (int i = 0; i < db.devices.size(); i++)
						{
							DBDevice device = db.devices.get(i);
							logs[i].setText(String.format("%15s", device.name)+": deleting ...");
							File recordDir = new File(device.deviceDir, "_records");
							boolean success = recordDir.delete();
							logs[i].setText(String.format("%15s", device.name)+": success? " + success);
						}
						P.p("Done.");
					}
					src.setEnabled(true);
				}
			}).start();
			
		}

	}
	
	class DevicePanel extends JPanel implements ComponentListener, ActionListener{
		private LargeLabel count_scenes, count_originals, count_pngs, count_stegos;
		private JButton b_count, b_validate;
		private DBDevice dbDevice;
		private LargeLabel[] logs;
		private static final int logLineCount = 10;
		private List<String> logMessages;
		DevicePanel(DBDevice device)
		{
			P.p("constructing DevicePanel "+device.name);
			this.dbDevice = device;
			this.addComponentListener(this);
			this.setLayout(new GridBagLayout());
			this.setBackground(Color.GRAY);
			GBC c = new GBC();
			
			c.fill = GBC.BOTH;
			c.gridwidth = c.gridheight = 1;
			c.weightx = c.weighty = 0;
			c.insets = new Insets(0,0,5,5);
			c.anchor = GBC.NORTH;
			
			c.gridy = 0; c.gridx = 0;
			add(new LargeLabel("Device Name:", true), c);
			c.gridx++;
			add(new LargeLabel(device.name), c);
			
			c.gridy++; c.gridx = 0;
			add(new LargeLabel("Directory:", true), c);
			c.gridx++;
			add(new LargeLabel(device.deviceDir.getAbsolutePath()), c);
			
			c.gridy++; c.gridx = 0;
			add(new LargeLabel("# Scenes:", true), c);
			c.gridx++;
			count_scenes = new LargeLabel("");
			add(count_scenes, c);
			
			c.gridy++; c.gridx = 0;
			add(new LargeLabel("# Original Images:", true), c);
			c.gridx++;
			count_originals = new LargeLabel("");
			add(count_originals, c);
			
			c.gridy++; c.gridx = 0;
			add(new LargeLabel("# Cropped PNGs:", true), c);
			c.gridx++;
			count_pngs = new LargeLabel("");
			add(count_pngs, c);
			
			c.gridy++; c.gridx = 0;
			add(new LargeLabel("# Stegos:", true), c);
			c.gridx++;
			count_stegos = new LargeLabel("");
			add(count_stegos, c);
			
			c.fill = GBC.NONE;
			c.gridy++; c.gridx = 0;
			c.gridwidth = 2;
			c.anchor = GBC.NORTHWEST;
			b_count = new JButton("Count");
			b_count.setFont(new Font(b_count.getFont().getName(), Font.PLAIN, 20));
			b_count.addActionListener(this);
			b_validate = new JButton("Validate");
			b_validate.setFont(new Font(b_validate.getFont().getName(), Font.PLAIN, 20));
			b_validate.addActionListener(this);
			JPanel buttonsRow = new JPanel();
			buttonsRow.setBackground(Color.GRAY);
			buttonsRow.add(b_count);
			buttonsRow.add(b_validate);
			add(buttonsRow, c);
			
			c.fill = GBC.BOTH;
			c.gridy++; c.gridx = 0;
			c.gridwidth = 2;
			c.anchor = GBC.NORTHWEST;
			JPanel logArea = new JPanel();
			BoxLayout boxLayout = new BoxLayout(logArea,BoxLayout.PAGE_AXIS);
			logArea.setLayout(boxLayout);
			logArea.setBackground(Color.black);
			logMessages = new LinkedList<String>();
			logs = new LargeLabel[logLineCount];
			for (int i = 0; i < logLineCount; i++)
			{
				logs[i] = new LargeLabel("line "+i);
				logs[i].setBackground(logArea.getBackground());
				logs[i].setForeground(Color.green);
				logArea.add(logs[i]);
			}
			add(logArea, c);
		}
		
		private void countImages()
		{
			dbDevice.init();
			List<DBScene> scenes = dbDevice.getScenes();
			int originals = 0, pngs = 0, stegos = 0;
			for (DBScene scene : scenes)
			{
				printLog("counting " +scene.id);
				originals += scene.getOriginalsCount();
				pngs += scene.getPNGCount();
				stegos += scene.getStegosCount();
			}
			printLog("Done.");
			final int total_originals = originals;
			final int total_pngs = pngs;
			final int total_stegos = stegos;
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run()
				{
					count_scenes.setText(scenes.size()+"");
					count_originals.setText(total_originals+"");
					count_pngs.setText(total_pngs+"");
					count_stegos.setText(total_stegos+"");
				}
			});
		}
		
		private void validateImages()
		{
			dbDevice.init();
			dbDevice.validateAll();
		}
		
		private void printLog(final String message)
		{
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run()
				{
					logMessages.add(message);
					if (logMessages.size()>10)
						logMessages.remove(0);
					int index = 0;
					for (String s : logMessages)
					{
						logs[index++].setText(s);
					}
				}
			});
		}
		
		@Override
		public void componentHidden(ComponentEvent arg0) {}
		@Override
		public void componentMoved(ComponentEvent arg0) {}
		@Override
		public void componentResized(ComponentEvent arg0) {}
		@Override
		public void componentShown(ComponentEvent arg0) {
			countImages();
		}
		@Override
		public void actionPerformed(ActionEvent e)
		{
			Object src = e.getSource();
			new Thread(new Runnable() {
				@Override
				public void run()
				{
					
					if (src == b_count)
					{
						countImages();
					}
					else if (src == b_validate)
					{
						validateImages();
					}
				}
			}).start();
			
		}
	}
	

	
	
	
	public static void main(String[] args)
	{
		createAndShow();
	}
	
	
	public static void createAndShow()
	{
		try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
		JFrame frame = new JFrame("StegoAppDB");
		frame.add(new DatabaseUI());
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setMinimumSize(new Dimension(1200,900));
		frame.setVisible(true);
		frame.pack();
	}
}
