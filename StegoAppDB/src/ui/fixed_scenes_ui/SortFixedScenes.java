package ui.fixed_scenes_ui;

import java.awt.Color;
import java.awt.GridLayout;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import ui.UI;
import util.F;
import util.P;

@SuppressWarnings("serial")
public class SortFixedScenes extends JPanel{
	
	SortFixedScenes() {
		collectFiles();
		initUI();
	}

	public static void main(String[] args)
	{
		JFrame frame = UI.createAndShow(new SortFixedScenes(), "Fixed Scenes Sorting Machine");
		frame.setExtendedState(frame.getExtendedState() | JFrame.MAXIMIZED_BOTH);
	}
	
	
/**  File Operation Section  **/
	class Device {
		String name;
		Map<String, Scene> scenes = new TreeMap<>();
	}
	class Scene {
		String label;
		File[] jpgs = new File[10];
		File[] dngs = new File[10];
		Scene(String l) {label = l;}
		public boolean hasAutoJPG() {
			return jpgs[0]!=null && jpgs[0].exists();
		}
		public int getJPGCount() {
			int count = 0;
			for (File f : jpgs)
				if (f!=null && f.exists())
					count++;
			return count;
		}
		public int getDNGCount() {
			int count = 0;
			for (File f : dngs)
				if (f!=null && f.exists())
					count++;
			return count;
		}
	}
	
	List<Device> devices;
	
	void collectFiles() {
		devices = new ArrayList<>();
		
		File root = new File("H:\\StegoAppDB_20Devices_FixedScenes_Dec2020");
		for (File deviceDir : root.listFiles()) {
			Device d = new Device();
			d.name = deviceDir.getName();
			for (File img : new File(deviceDir, "originals").listFiles()) {
				String ext = F.getFileExt(img);
				if (ext.equalsIgnoreCase("jpg") || ext.equalsIgnoreCase("dng")) {
					String sceneLabel = img.getName().split("_")[1];
					int imageIndex = Integer.parseInt(img.getName().split("_")[2].split("-")[1]);
					Scene s = d.scenes.computeIfAbsent(sceneLabel, k->new Scene(k));
					if (img.getName().contains("_JPG-"))
						s.jpgs[imageIndex] = img;
					else
						s.dngs[imageIndex] = img;
				}
			}
			P.pf("%s has %d scenes:\n", d.name, d.scenes.size());
			d.scenes.values().forEach(s->{
				int numJPG = s.getJPGCount();
				int numDNG = s.getDNGCount();
				if (numJPG+numDNG!=20)
					P.pf("  scene %s has %d JPGs and %d DNGs\n", s.label, numJPG, numDNG);
				if (!s.hasAutoJPG())
					P.pf("  scene %s doesn't have auto JPG\n", s.label);
			});
			NUM_SCENES = Math.max(NUM_SCENES, d.scenes.size());
			devices.add(d);
		}
		P.p("\nMax Number of Scenes: "+NUM_SCENES);
	}
	

/**  User Interface Section **/
	int NUM_SCENES = 0;
	
	void initUI() {
		P.pf("%d %d\n", NUM_SCENES+1, devices.size()+1);
		GridLayout l = new GridLayout(NUM_SCENES+1, devices.size()+1);
		l.setHgap(5);
		l.setVgap(5);
		setLayout(l);
		
		// draw first row
		add(new JPanel());
		for (int i=0; i<devices.size(); i++) {
			JPanel p = new JPanel();
			p.add(new JLabel(devices.get(i).name));
			p.setBackground(Color.yellow);
			add(p);
		}
		
		// draw each row
		
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
