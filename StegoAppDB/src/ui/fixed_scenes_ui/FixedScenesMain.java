package ui.fixed_scenes_ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextArea;
import javax.swing.plaf.basic.BasicArrowButton;

import ui.UI;
import util.F;
import util.Images;
import util.P;

@SuppressWarnings("serial")
public class FixedScenesMain extends JPanel implements ActionListener{
	
	public static final File rootDir = new File("H:\\StegoAppDB_20Devices_FixedScenes_Dec2020");
	private static final String[] DEVICE_NAMES = rootDir.list();
	private static final int NUM_DEVICES = DEVICE_NAMES.length;
	public static final int NUM_VISIBLE_SCENES = 19;

	private RowControlPane[] rowControlPanes = new RowControlPane[NUM_VISIBLE_SCENES];
	private Cell[][] cells = new Cell[NUM_VISIBLE_SCENES][NUM_DEVICES];
	static List<Scene> scenes = new ArrayList<>();
	
	FixedScenesMain() {
		GridLayout l = new GridLayout(0, NUM_DEVICES+1);
		l.setHgap(5);
		l.setVgap(5);
		setLayout(l);
		setBackground(Color.black);
		
		//collect images
		initCamerawScenes();
		
		// top left: controls
		add(new ControlPane());
		// first row is the device labels
		for (int i=0; i<NUM_DEVICES; i++) {
			add(new DeviceLabelPane(DEVICE_NAMES[i]));
		}
		// then NUM_VISIBLE_SCENES more rows
		for (int i=0; i<NUM_VISIBLE_SCENES; i++) {
			add(rowControlPanes[i] = new RowControlPane(i));
			for (int j=0; j<NUM_DEVICES; j++)
				add(cells[i][j] = new Cell(DEVICE_NAMES[j], j, i));
		}
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		switch (e.getActionCommand()) {
			case "up":
				break;
			case "down":
				break;
		}
	}
	
	class Scene { // auto exposure images from all phones for a single scene
		Map<String, File> jpgs = new HashMap<>(); // key: device name
		Map<String, File> thumbs = new HashMap<>(); // key: device name
		String sceneLabel; // use Pixel1-1's scene label as reference
		int index;
		
		Scene(int index) {
			this.index = index;
		}
		
		List<File> getAllImagesFromDevice(String deviceName) {
			List<File> files = new ArrayList<>();
			File autoJPG = jpgs.get(deviceName);
			String sceneName = autoJPG.getName().split("_")[1];
			for (File jpg : autoJPG.getParentFile().listFiles())
				if (jpg.getName().startsWith(deviceName+"_"+sceneName))
					files.add(jpg);
			return files;
		}

		public void swapImages(Scene aboveScene, String deviceName)
		{
			List<File> files1 = getAllImagesFromDevice(deviceName);
			List<File> files2 = aboveScene.getAllImagesFromDevice(deviceName);
			
			P.p("- this: ");
			files1.forEach(f->P.p('\t'+f.getName()));
			P.p("- that: ");
			files2.forEach(f->P.p('\t'+f.getName()));
		}
	}
	
	int countImages(int deviceIndex, int sceneIndex) {
		String dName = DEVICE_NAMES[deviceIndex];
		List<Set<File>> list = allFiles2.getOrDefault(dName, new ArrayList<>());
		return sceneIndex<list.size()?list.get(sceneIndex).size():0;
	}
	
	class ControlPane extends JPanel{
		ControlPane() {
			setBackground(Color.yellow);
			GridLayout l = new GridLayout();
			l.setHgap(5);
			l.setVgap(5);
			setLayout(new GridLayout(0, 1));
			JButton up = new BasicArrowButton(BasicArrowButton.NORTH);
			up.setActionCommand("up");
			up.addActionListener(FixedScenesMain.this);
			JButton down = new BasicArrowButton(BasicArrowButton.SOUTH);
			down.setActionCommand("down");
			down.addActionListener(FixedScenesMain.this);
			JPanel p = new JPanel(new GridLayout(1,0));
			p.add(up);
			p.add(down);
			add(p);
		}
	}
	
	class DeviceLabelPane extends JPanel {
		public DeviceLabelPane(String label) {
			setBackground(Color.yellow);
			add(new JLabel(label));
		}
	}
	
	class RowControlPane extends JPanel {
		private JTextArea label = new JTextArea();
		public RowControlPane(int sceneIndex)
		{
			setBackground(Color.yellow);
			add(label);
			label.setBackground(this.getBackground());
			label.setEditable(false);
			label.setMinimumSize(new Dimension(this.getWidth(), label.getHeight()));
			if (sceneIndex < scenes.size()) {
				Scene s = scenes.get(sceneIndex);
				if (s.sceneLabel==null || s.sceneLabel.equals("null"))
					return;
				//P.p("label: "+s.sceneLabel);
				String timeInfo[] = s.sceneLabel.replace("Scene-", "").split("-");
				String date = timeInfo[0].substring(0,4)+'-'+timeInfo[0].substring(4,6)+'-'+timeInfo[0].substring(6);
				String time = timeInfo[1].substring(0,2)+':'+timeInfo[1].substring(2,4)+':'+timeInfo[1].substring(4);
				label.setText(date+'\n'+time);
			}
		}
	}
	
	
	ExecutorService imageLoader = Executors.newFixedThreadPool(4);
	class Cell extends JPanel implements MouseListener{
		String deviceName;
		BufferedImage image;
		int sceneIndex = -1;
		int deviceIndex = -1;
		CellPopupMenu menu;
		public Cell(String deviceName, int deviceIndex, int sceneIndex) {
			this.deviceName = deviceName;
			this.deviceIndex = deviceIndex;
			loadScene(sceneIndex);
			addMouseListener(this);
			menu = new CellPopupMenu(this);
		}
		
		public void loadScene(int index) {
			if (this.sceneIndex == index)
				return;
			this.sceneIndex = index;
			Scene s = scenes.get(index);
			imageLoader.execute(()->{
				image = loadThumbnail(s.jpgs.get(deviceName), s.thumbs.get(deviceName));
				repaint();
			});
		}
		
		@Override
		protected void paintComponent(Graphics g)
		{
			super.paintComponent(g);
			if (image!=null)
				g.drawImage(image, 
						0, 0, getWidth(), getHeight(), 
						0, 0, image.getWidth(null), image.getHeight(null), null);
			int count = countImages(deviceIndex, sceneIndex);
			if (count != 20) {
				g.setColor(Color.white);
				g.fillRect(0, 3, 30, 30);
				g.setColor(Color.red);
				g.setFont(new Font("Arial", Font.BOLD, 25));
				g.drawString(""+count, 1, 25);
			}
				
				
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			if (e.getButton() == MouseEvent.BUTTON3)
				menu.show(e.getComponent(), e.getX(), e.getY());
		}
		@Override public void mouseEntered(MouseEvent arg0) {}
		@Override public void mouseExited(MouseEvent arg0) {}
		@Override public void mousePressed(MouseEvent arg0) {}
		@Override public void mouseReleased(MouseEvent arg0) {}
	}
	
	class CellPopupMenu extends JPopupMenu {
		Cell cell;
	    public CellPopupMenu(Cell cell) {

	    	JMenuItem swap = new JMenuItem("Swap scene with above");
	    	swap.addActionListener(e->{
				if (cell.sceneIndex==0)
					return;
				Cell above = cells[cell.sceneIndex-1][cell.deviceIndex];
				Scene thisScene = scenes.get(cell.sceneIndex);
				Scene aboveScene = scenes.get(above.sceneIndex);
				thisScene.swapImages(aboveScene, cell.deviceName);
			});
	    	add(swap);
	    	
	    	JMenuItem remove = new JMenuItem("Move scene out of folder");
	    	remove.addActionListener(e->{
	    		Scene s = scenes.get(cell.sceneIndex);
	    		File autoJPG = s.jpgs.get(DEVICE_NAMES[cell.deviceIndex]);
	    		
	    		List<File> all20Images = new ArrayList<>();
	    		String sceneName = autoJPG.getName().split("_")[1];
	    		for (File f : autoJPG.getParentFile().listFiles()) {
	    			if (f.getName().endsWith(".jpg") || f.getName().endsWith(".dng")) {
	    				if (f.getName().split("_")[1].equals(sceneName))
	    					all20Images.add(f);
	    			}
	    		}
	    		P.p("-- all 20 images -- ");
	    		all20Images.forEach(f->P.p('\t'+f.getName()));
	    		
	    		File dir = new File("H:\\StegoAppDB_20Devices_discarded");
	    		all20Images.forEach(f->f.renameTo(new File(dir, f.getName())));
	    	});
	    	add(remove);
	    }
	}
	
	Map<String, Map<String, Set<File>>> allFiles = new TreeMap<>();
	Map<String, List<Set<File>>> allFiles2 = new TreeMap<>();
	
	void initCamerawScenes() {
		// from each device folder, collect the auto-exposure original JPEG images, and make thumbnails for them
		for (String deviceName : DEVICE_NAMES) {
			File deviceDir = new File(rootDir, deviceName);
			File originalDir = new File(deviceDir, "originals");
			if (!originalDir.exists()) {
				P.e("Folder doesn't exist: "+originalDir.getAbsolutePath());
				continue;
			}
			P.p(deviceName+" "+originalDir.list().length);
			int sceneIndex = 0;
			for (File f : originalDir.listFiles()) {
				String sceneLabel = f.getName().split("_")[1];
				allFiles.computeIfAbsent(deviceName, d->new TreeMap<>())
						.computeIfAbsent(sceneLabel, s->new TreeSet<>((f1,f2)->f1.getName().compareTo(f2.getName())))
						.add(f);
				if (F.getFileExt(f).equalsIgnoreCase("jpg") && f.getName().contains("_JPG-00_")) {
					if (scenes.size()<= sceneIndex)
						scenes.add(new Scene(sceneIndex));
					Scene scene = scenes.get(sceneIndex++);
					scene.jpgs.put(deviceName, f);
					scene.thumbs.put(deviceName, new File(thumbDir, f.getName().replace(".jpg", ".png")));
					if (deviceName.equals("Pixel1-1"))
						scene.sceneLabel = f.getName().split("_")[1];
				}
			}
		}
		allFiles.forEach((d,scenes)->{
			scenes.forEach((s,files)->{
				allFiles2.computeIfAbsent(d, k->new ArrayList<>())
						.add(files);
			});
		});
	}

	static final File thumbDir = new File("H:\\thumbnails");
	static BufferedImage loadThumbnail(File jpg, File thumb) {
		if (jpg==null || !jpg.exists())
			return Images.loadImage(new File(thumbDir, "missing.png"));
		thumbDir.mkdirs();
		if (!thumb.exists())
			Images.scale(jpg, thumb);
		return Images.loadImage(thumb);
	}
	
	public static void main(String[] args)
	{
		JFrame frame = UI.createAndShow(new FixedScenesMain(), "Fixed Scenes Helper");
		frame.setExtendedState(frame.getExtendedState() | JFrame.MAXIMIZED_BOTH);
	}
	
	
	
}
