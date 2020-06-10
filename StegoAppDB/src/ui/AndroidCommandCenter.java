package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import util.P;

@SuppressWarnings("serial")
public class AndroidCommandCenter extends JPanel{

	public static Map<String, String> deviceIDs;
	
	static {
		deviceIDs = new LinkedHashMap<>();
		deviceIDs.put("Pixel 1-1","FA6BJ0305170"); deviceIDs.put("Pixel 2-1","FA7A21A02515"); deviceIDs.put("OnePlus 5-1","661f65f9");
		deviceIDs.put("Pixel 1-2","FA6BJ0305399"); deviceIDs.put("Pixel 2-2","FA7A21A02530"); deviceIDs.put("OnePlus 5-2","80b69127");
		deviceIDs.put("Pixel 1-3","FA6BT0301400"); deviceIDs.put("Pixel 2-3","HT7AY1A00100"); deviceIDs.put("Samsung S8-1","988964464e5752354c");
		deviceIDs.put("Pixel 1-4","FA6C60303538"); deviceIDs.put("Pixel 2-4","FA81N1A05129"); deviceIDs.put("Samsung S8-2","98897a37575943395a");
	}
	
	static final int lineCount = 10;
	public static final String adbPath = "C:/libs/Android/SDK/platform-tools/adb.exe";
	
	class DevicePanel extends JPanel implements ActionListener{
		
		String name, id;
		JTextField tf_cmd;
		JButton b_exec;
		List<JLabel> output;
		volatile List<String> lines;
		Thread logThread;
		int newLineIndex;
		int cmdIndex;
		List<String> cmdHistory;
		DevicePanel(String s, String i) {name=s; id=i; init();}
		void init()
		{
			setLayout(new BorderLayout());
			
			JPanel outputArea = new JPanel(new GridLayout(0,1));
			outputArea.setBackground(new Color(1,36,86));
			output = new ArrayList<>();
			for (int i = 0; i < lineCount; i++)
			{
				output.add(new JLabel());
				outputArea.add(output.get(i));
				output.get(i).setBackground(Color.blue);
				output.get(i).setForeground(Color.white);
			}
			
			tf_cmd = new JTextField();
			b_exec = new JButton("execute");
			
			JPanel cmdLine = new JPanel(new GridLayout(0,1));
			cmdLine.add(tf_cmd);
			//cmdLine.add(b_exec);
			
			add(new JTextField(name+" adb -s "+id+" shell"), BorderLayout.NORTH);
			add(outputArea, BorderLayout.CENTER);
			add(cmdLine, BorderLayout.SOUTH);
			
			newLineIndex = 0;
			lines = new LinkedList<>();
			cmdIndex = -1;
			cmdHistory = new ArrayList<>();
			tf_cmd.addActionListener(this);
			b_exec.addActionListener(this);
			tf_cmd.addKeyListener(new KeyAdapter() {
				@Override
				public void keyPressed(KeyEvent e)
				{
					int code = e.getKeyCode();
					if (code==38)
						cmdIndex = Math.max(0, --cmdIndex);
					else if (code==40)
						cmdIndex = Math.min(cmdHistory.size(), ++cmdIndex);
					else
						return;
					if (cmdIndex>=0&&cmdIndex<cmdHistory.size())
						tf_cmd.setText(cmdHistory.get(cmdIndex));
				}
			});
		}
		void newLine(String line)
		{
			lines.add(line);
			while (lines.size()>lineCount)
				lines.remove(0);
			int i = 0;
			for (String s : lines)
				output.get(i++).setText(s);
		}
		@Override
		public void actionPerformed(ActionEvent e)
		{
			Object src = e.getSource();
			if (src == tf_cmd || src == b_exec)
			{
				String cmd = tf_cmd.getText().trim();
				exec(cmd);
			}
		}
		void exec(String cmd)
		{
			if (cmdHistory.isEmpty() || !cmdHistory.get(cmdHistory.size()-1).equals(cmd))
			{
				cmdHistory.add(cmd);
				cmdIndex = cmdHistory.size()-1;
			}
			if (logThread==null || !logThread.isAlive())
			{
				if (!cmd.isEmpty())
				{
					String command = adbPath+" -s "+id+" "+cmd;
					P.p("[cmd] "+command);
					for (JLabel l : output)
						l.setText("");
					lines.clear();
					newLine("");
					final Process p = P.exec(command, false);
					logThread = new Thread(new Runnable() {
						@Override
						public void run()
						{
							read(p.getInputStream());
							read(p.getErrorStream());
							newLine("--- Done ---");
						}
					});
					logThread.start();
				}
			}
		}
		void read(InputStream stream)
		{
			SimpleDateFormat formatter = new SimpleDateFormat("[MM/dd HH:mm:ss]");  
		    
			try
			{
				BufferedReader in = new BufferedReader(new InputStreamReader(stream));
				String line;
				while ((line=in.readLine())!=null)
				{
					String time = formatter.format(new Date());
					newLine(time+" "+line);
				}
				in.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	
	Map<String, DevicePanel> panels;
	
	int cmdIndexAll;
	AndroidCommandCenter()
	{
		setBackground(Color.black);
		setLayout(new BorderLayout());
		
		JPanel main = new JPanel(new GridLayout(0,3));
		panels = new HashMap<>();
		for (String name : deviceIDs.keySet())
		{
			panels.put(name, new DevicePanel(name, deviceIDs.get(name)));
			main.add(panels.get(name));
			panels.get(name).setBorder(BorderFactory.createLineBorder(Color.black, 5));
		}
		
		JPanel federalCmd = new JPanel(new GridLayout(1,0));
		JTextField cmdAll = new JTextField("shell ls /sdcard/Download/StegoDB_March2019");
		JButton b_all = new JButton("EXECUTE FOR ALL");
		federalCmd.add(cmdAll);
		federalCmd.add(b_all);
		federalCmd.setBorder(new EmptyBorder(20,20,20,20));
		
		add(main, BorderLayout.CENTER);
		add(federalCmd, BorderLayout.SOUTH);
		
		List<String> cmdHistory = new ArrayList<>();
		cmdIndexAll = 0;
		ActionListener al = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0)
			{
				String cmd = cmdAll.getText().trim();
				if (cmdHistory.isEmpty() || !cmdHistory.get(cmdHistory.size()-1).equals(cmd))
				{
					cmdHistory.add(cmd);
					if (cmdHistory.size()>100)
						cmdHistory.remove(0);
					cmdIndexAll = cmdHistory.size()-1;
				}
				for (DevicePanel p : panels.values())
					p.exec(cmdAll.getText().trim());
			}
		};
		cmdAll.addActionListener(al);
		b_all.addActionListener(al);
		cmdAll.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e)
			{
				int code = e.getKeyCode();
				if (code==38)
					cmdIndexAll = Math.max(0, --cmdIndexAll);
				else if (code==40)
					cmdIndexAll = Math.min(cmdHistory.size()-1, ++cmdIndexAll);
				else
					return;
				P.p("index = "+cmdIndexAll);
				if (cmdIndexAll>=0 && cmdIndexAll<cmdHistory.size())
					cmdAll.setText(cmdHistory.get(cmdIndexAll));
			}
		});
	}
	
	abstract class KeyAdapter implements KeyListener {

		@Override
		public void keyReleased(KeyEvent e) {}

		@Override
		public void keyTyped(KeyEvent e) {}
	}
	
	public static void main(String[] args)
	{
		UI.createAndShow(new AndroidCommandCenter(), "Android Command Center");
	}

}
