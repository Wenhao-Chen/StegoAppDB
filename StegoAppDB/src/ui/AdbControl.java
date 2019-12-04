package ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Event;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import apex.APEXApp;
import util.P;

@SuppressWarnings("serial")
public class AdbControl extends JPanel{
	
	File instrumentedDir = new File("C:/workspace/app_analysis/apks/instrumented");
	File apkDir = new File("C:/workspace/app_analysis/apks");
	JPanel left, top, center, bottom;
	JLabel l_status, l_apk, l_instrumented;
	JList<String> appList;
	Map<String, APEXApp> apps;
	APEXApp app;
	File instrumented;
	
	AdbControl()
	{
		setLayout(new BorderLayout());
		
		left = new JPanel(new GridLayout(1,1));
		left.setBorder(new EmptyBorder(10,10,10,10));
		top = new JPanel(new GridLayout(0,1));
		top.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(10,0,0,0), BorderFactory.createEtchedBorder()));
		
		center = new JPanel(new BorderLayout());
		center.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(10,0,0,0), BorderFactory.createEtchedBorder()));
		bottom = new JPanel();
		
		add(left, BorderLayout.WEST);
		JPanel p = new JPanel(new BorderLayout());
		p.add(top, BorderLayout.NORTH);
		p.add(center, BorderLayout.CENTER);
		p.add(bottom, BorderLayout.SOUTH);
		add(p, BorderLayout.CENTER);
		
/*		left.setBackground(UI.randomColor());
		top.setBackground(UI.randomColor());
		center.setBackground(UI.randomColor());
		bottom.setBackground(UI.randomColor());*/
		
		apps = new HashMap<>();
		initTopPanel();
		initBottomPanel();
		loadAppList();
	}
	
	private void selectApp(String packageName)
	{
		if (app!=null && app.packageName.equals(packageName))
			return;
		appList.setEnabled(false);
		l_status.setText("Status: ["+packageName+"] preparing");
		new Thread(new Runnable() {
			@Override
			public void run()
			{
				apps.putIfAbsent(packageName, new APEXApp(findAPK(packageName)));
				app = apps.get(packageName);
				instrumented = new File(instrumentedDir, app.packageName+"_instrumented.apk");
				
				l_status.setText("Status: ["+packageName+"] ready");
				l_apk.setText("Original APK: "+app.apk.getAbsolutePath());
				l_instrumented.setText("Instrumented APK: "+ instrumented.getAbsolutePath());
				appList.setEnabled(true);
			}
		}).start();
		
	}
	
	private void install_instrumented()
	{
		new Thread(new Runnable() {
			@Override
			public void run()
			{
				l_status.setText("Status: uninstalling old "+app.packageName);
				P.exec(AndroidCommandCenter.adbPath+" uninstall "+app.packageName, true, true);
				l_status.setText("Status: installing instrumented "+app.packageName);
				P.exec(AndroidCommandCenter.adbPath+" install "+instrumented.getAbsolutePath(), true, true);
				l_status.setText("Status: install finished.");
			}
		}).start();
	}
	
	private File findAPK(String packageName)
	{
		for (File dir : apkDir.listFiles())
		{
			if (dir.getName().equals("instrumented"))
				continue;
			for (File f : dir.listFiles())
				if (f.getName().equals(packageName+".apk"))
					return f;
		}
		return null;
	}
	
	private void initTopPanel()
	{
		l_status = new JLabel("Status:");
		UI.setFont(l_status, 30);
		l_apk = new JLabel("Original APK Path: ");
		UI.setFont(l_apk, 20);
		l_instrumented = new JLabel("Instrumented APK Path: ");
		UI.setFont(l_instrumented, 20);
		
		top.add(l_status);
		top.add(l_apk);
		top.add(l_instrumented);
	}
	
	private void initBottomPanel()
	{
		JButton b_install_instrumented = new JButton("Install");
		b_install_instrumented.setActionCommand("install_instrumented");

		ActionListener al = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e)
			{
				String cmd = e.getActionCommand();
				switch (cmd)
				{
				case "install_instrumented":
					install_instrumented();
					break;
				}
			}
		};
		
		
		
		b_install_instrumented.addActionListener(al);
		
		bottom.add(b_install_instrumented);
	}
	
	private void loadAppList()
	{
		DefaultListModel<String> listModel = new DefaultListModel<>();
		for (File f : instrumentedDir.listFiles())
		{
			String name = f.getName();
			name = name.substring(0, name.indexOf("_instrumented"));
			listModel.addElement(name);
		}
		
		DefaultListCellRenderer cellRenderer = new DefaultListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus)
			{
				Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				c.setFont(new Font(c.getFont().getName(), Font.PLAIN, 20));
				return c;
			}
		};
		
		ListSelectionListener listSelector = new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e)
			{
				if (e.getValueIsAdjusting())
					return;
				selectApp(appList.getSelectedValue());
			}
		};
		
		appList = new JList<>(listModel);
		appList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		appList.setCellRenderer(cellRenderer);
		appList.addListSelectionListener(listSelector);
		left.add(new JScrollPane(appList));
	}
	
	

	public static void main(String[] args)
	{
		UI.createAndShow(new AdbControl(), "ADB Control");
	}

}
