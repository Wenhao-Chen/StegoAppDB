package app_analysis.old;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import apex.APEXApp;
import apex.bytecode_wrappers.APEXClass;
import apex.bytecode_wrappers.APEXField;
import apex.bytecode_wrappers.APEXMethod;
import apex.graphs.CallGraph;
import ui.UI;
import ui.UIWrappers.LargeLabel;
import util.F;
import util.Graphviz;
import util.P;

public class HelperUI extends JPanel{

	private static final long serialVersionUID = 1L;
	
	static final String root = "C:\\workspace\\app_analysis";
	static final File apkRoot = new File(root, "apks");
	static final File stegoBenmarkAppDir = new File(
			"E:\\crawled_from_github\\android\\StegoBenchmark\\app\\build\\outputs\\apk\\debug");
	static final File decodedRoot = new File(root, "decoded");
	static final File graphsRoot = new File(root, "graphs");
	static final File cfgRoot = new File(graphsRoot, "cfg");
	static final File cgRoot = new File(graphsRoot, "cg");
	static final File notesRoot = new File(root, "notes");
	static final File actualNotesRoot = new File(notesRoot, "Notes");
	static final File entryPointsRoot = new File(notesRoot, "EntryPoints");
	static final File treesRoot = new File(notesRoot, "ExpressionTrees");
	
	//NOTE: Override the removeEldestEntry() if running into memory problems
	Map<String, APEXApp> apps = new LinkedHashMap<String, APEXApp>() {
		private static final long serialVersionUID = 6781776773215826026L;
		@Override
		protected boolean removeEldestEntry(Map.Entry<String, APEXApp> e)
		{
			return size()>10;
		}
	};
	
	Map<String, CallGraph> cgs = new LinkedHashMap<String, CallGraph>() {
		private static final long serialVersionUID = 3354772409104482911L;

		@Override
		protected boolean removeEldestEntry(Map.Entry<String, CallGraph> e)
		{
			return size()>10;
		}
	};
	
	APEXApp app;
	CallGraph cg;
	APEXClass c;
	APEXMethod m;
	APEXField f;
	
	LargeLabel memoryLabel, statusLabel;
	JScrollPane apkTreeView, classTreeView, fieldsAndMethodsView;
	JPanel utilityPanel;
	JButton b_openSmaliFile, b_showCFG;
	JTextArea ta_notes;
	
	BlockingQueue<File> appQueue = new LinkedBlockingQueue<>();
	
	
	HelperUI()
	{
		initUI();
		startMemoryMonitor();
		startAppDecodeThread();
	}
	
	
	
	JPanel initControlPanel()
	{
		utilityPanel = new JPanel(new BorderLayout());
		JButton b = new JButton("Save Notes");
		JLabel l = new JLabel("Notes:");
		ta_notes = new JTextArea(15,0);
		JPanel top = new JPanel();
		
		top.setLayout(new BoxLayout(top, BoxLayout.LINE_AXIS));
		top.add(Box.createRigidArea(new Dimension(20,0)));
		top.add(l);
		top.add(Box.createRigidArea(new Dimension(20,0)));
		top.add(b);
		top.add(Box.createHorizontalGlue());
		
		utilityPanel.add(top, BorderLayout.NORTH);
		utilityPanel.add(ta_notes, BorderLayout.CENTER);
		
		b.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e)
			{
				saveNotes(ta_notes.getText());
			}
		});
		
		ta_notes.setLineWrap(true);
		
		return utilityPanel;
	}
	
	void saveNotes(String text)
	{
		if (app == null)
			return;
		File noteDir = new File(actualNotesRoot, app.apk.getName());
		noteDir.mkdirs();
		File noteF = new File(noteDir, app.apk.getName()+".notes");
		F.write(text, noteF, false);
	}
	
	void loadNotes()
	{
		ta_notes.setText("");
		if (app == null)
		{
			return;
		}
			
		File noteDir = new File(actualNotesRoot, app.apk.getName());
		noteDir.mkdirs();
		File noteF = new File(noteDir, app.apk.getName()+".notes");
		if (noteF.exists())
		{
			ta_notes.setText(String.join("\n", F.readLines(noteF)));
		}
	}

	void initUI()
	{
		setLayout(new BorderLayout());
		
		/// Left Side
		JPanel left = new JPanel(new BorderLayout());
		left.setBorder(BorderFactory.createRaisedBevelBorder());
		left.add(new LargeLabel("App List"), BorderLayout.NORTH);
		left.add(apkTreeView=new JScrollPane(new FileTree(apkRoot)), BorderLayout.CENTER);
		
		
		/// Center Side
		JPanel cen = new JPanel();
		cen.setLayout(new BorderLayout());
		cen.add(classTreeView = new JScrollPane(), BorderLayout.WEST);
		
		JPanel classMemberPane = new JPanel(new BorderLayout());
		//classMemberPane.setLayout(new BoxLayout(classMemberPane, BoxLayout.PAGE_AXIS));
		classMemberPane.add(fieldsAndMethodsView = new JScrollPane(), BorderLayout.CENTER);
		classMemberPane.add(utilityPanel = initControlPanel(), BorderLayout.SOUTH);
		cen.add(classMemberPane, BorderLayout.CENTER); 
		
		/// Bottom Side
		LargeLabel.defaultBackgroundColor = null;
		JPanel bot = new JPanel();
		bot.setLayout(new BoxLayout(bot, BoxLayout.LINE_AXIS));
		bot.setBorder(BorderFactory.createRaisedSoftBevelBorder());
		bot.add(new LargeLabel("Memory Usage: "));
		bot.add(memoryLabel=new LargeLabel(""));
		bot.add(Box.createHorizontalStrut(50));
		bot.add(new LargeLabel("Status: "));
		bot.add(statusLabel = new LargeLabel(""));
		
		add(left, BorderLayout.WEST);
		add(cen, BorderLayout.CENTER);
		add(bot, BorderLayout.SOUTH);
	}

	
	void setStatus(String s)
	{
		setStatus(s, null);
	}
	
	void setStatus(String s, Color c)
	{
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run()
			{
				statusLabel.setText(s);
				if (c != null)
					statusLabel.setBackground(c);
			}
		});
	}
	
	static void openFile(File f, int line)
	{
		String nppp = "\"C:\\Program Files\\Notepad++\\notepad++.exe\"";
		P.exec(nppp+" \""+f.getAbsolutePath()+"\" -n"+line, false);
	}
	
	class ClassMemberTree extends JTree implements TreeSelectionListener, MouseListener {
		private static final long serialVersionUID = -2677760657274777895L;
		MethodPop pop;
		ClassMemberTree(APEXClass c)
		{
			super(new ClassMemberNode(c));
			setFont(new Font(getFont().getName(), getFont().getStyle(), getFont().getSize() + 3));
			for (int i=0; i<this.getRowCount(); i++)
				expandRow(i);
			addTreeSelectionListener(this);
			addMouseListener(this);
			pop = new MethodPop(this);
		}
		
		@Override
		public void valueChanged(TreeSelectionEvent e)
		{
			Object node = getLastSelectedPathComponent();
			if (node instanceof FieldNode)
			{
				FieldNode fn = (FieldNode) node;
				f = fn.f;
			}
			else if (node instanceof MethodNode)
			{
				MethodNode mn = (MethodNode) node;
				m = mn.m;
			}
		}
		
		@Override
		public void mouseClicked(MouseEvent e)
		{
			if (SwingUtilities.isRightMouseButton(e))
			{
				int row = getClosestRowForLocation(e.getX(), e.getY());
				//P.p("right clicking row "+row);
				boolean contains = false;
				for (int i : getSelectionRows())
				if (i==row) {contains = true; break;}
				
				if (!contains)
					setSelectionRow(row);
				//else
				//	P.p("contains");
				Object obj = getPathForRow(row).getLastPathComponent();
				if (obj instanceof MethodNode)
					pop.show(e.getComponent(), e.getX(), e.getY());
			}
		}

		@Override public void mouseEntered(MouseEvent arg0){}
		@Override public void mouseExited(MouseEvent arg0){}
		@Override public void mousePressed(MouseEvent arg0){}
		@Override public void mouseReleased(MouseEvent arg0){}
	}
	
	class MethodPop extends JPopupMenu {
		private static final long serialVersionUID = 7732346239739873829L;
		
		MethodPop(ClassMemberTree tree)
		{
			JMenuItem item_copySig, item_OpenSmali, item_ShowCFG, item_ShowCG, item_remakeCG;
			
			add(item_copySig = new JMenuItem("Copy Signature"));
			item_copySig.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0)
				{
					ta_notes.append("\n");
					ta_notes.append(m.signature);
				}
			});
			
			add(item_OpenSmali = new JMenuItem("Show Smali Code"));
			item_OpenSmali.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0)
				{
					openFile(m.c.smaliF, m.lineNumber);
				}
			});
			
			add(item_ShowCFG = new JMenuItem("Show CFG"));
			item_ShowCFG.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0)
				{
					//HelperUI.this.openFile(m.c.smaliF, m.lineNumber);
					File cfgF = Graphviz.makeCFG(app, m.signature);
					if (cfgF != null)
						P.exec("explorer.exe \""+cfgF.getAbsolutePath()+"\"", false);
				}
			});
			
			add(item_ShowCG = new JMenuItem("Show relevant CG"));
			item_ShowCG.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0)
				{
					//HelperUI.this.openFile(m.c.smaliF, m.lineNumber);
					new Thread(new Runnable() {
						@Override
						public void run()
						{
							File cgDir = new File(cgRoot, app.apk.getName());
							cgDir.mkdirs();
							Set<String> sigs = new HashSet<>();
							for (int row : tree.getSelectionRows())
							{
								Object obj = tree.getPathForRow(row).getLastPathComponent();
								if (obj instanceof MethodNode)
								{
									MethodNode mn = (MethodNode)obj;
									sigs.add(mn.m.signature);
								}
							}
							setStatus("Making Sub CG for "+sigs.size()+" methods");
							File cfgF = Graphviz.makeDotGraph(cg.getDotGraph(sigs), m.c.getJavaName()+"_"+String.join("+", sigs).hashCode(), cgDir, true);
							if (cfgF != null)
							{
								setStatus("Finished making CG");
								P.exec("explorer.exe \""+cfgF.getAbsolutePath()+"\"", false);
							}
							else
								setStatus("Failed making CG");
								
						}
					}).start();

				}
			});
			
			add(item_remakeCG = new JMenuItem("Remake relevant CG"));
			item_remakeCG.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0)
				{
					//HelperUI.this.openFile(m.c.smaliF, m.lineNumber);
					new Thread(new Runnable() {
						@Override
						public void run()
						{
							File cgDir = new File(cgRoot, app.apk.getName());
							cgDir.mkdirs();
							Set<String> sigs = new HashSet<>();
							for (int row : tree.getSelectionRows())
							{
								Object obj = tree.getPathForRow(row).getLastPathComponent();
								if (obj instanceof MethodNode)
								{
									MethodNode mn = (MethodNode)obj;
									sigs.add(mn.m.signature);
								}
							}
							setStatus("Making Sub CG for "+sigs.size()+" methods");
							File cfgF = Graphviz.makeDotGraph(cg.getDotGraph(sigs), m.c.getJavaName()+"_"+String.join("+", sigs).hashCode(), cgDir, true);
							if (cfgF != null)
							{
								setStatus("Finished making CG");
								P.exec("explorer.exe \""+cfgF.getAbsolutePath()+"\"", false);
							}
							else
								setStatus("Failed making CG");
								
						}
					}).start();

				}
			});
		}
	}
	
	class ClassMemberNode extends DefaultMutableTreeNode {
		private static final long serialVersionUID = 3514047380259389683L;
		
		ClassMemberNode(APEXClass c)
		{
			super("Class "+c.getJavaName());
			ClassMemberNode fields = new ClassMemberNode();
			fields.setUserObject("Fields");
			ClassMemberNode methods = new ClassMemberNode();
			methods.setUserObject("Methods");
			c.fields.values().stream().sorted((f1,f2)->(f1.subSignature.compareTo(f2.subSignature))).forEach(f->{
				fields.add(new FieldNode(f));
			});
			c.methods.values().stream().sorted((m1,m2)->(m1.subSignature.compareTo(m2.subSignature))).forEach(m->{
				methods.add(new MethodNode(m));
			});
			add(fields);
			add(methods);
		}
		
		ClassMemberNode() {}
	}

	class FieldNode extends DefaultMutableTreeNode {
		private static final long serialVersionUID = 5623325611074546771L;
		APEXField f;
		FieldNode(APEXField f)
		{
			super(f.subSignature);
			this.f = f;
		}
	}

	class MethodNode extends DefaultMutableTreeNode {
		private static final long serialVersionUID = 244768399248141052L;
		APEXMethod m;
		MethodNode(APEXMethod m)
		{
			super(m.subSignature);
			this.m = m;
		}
	}

	class ClassList extends JList<APEXClass> implements ListSelectionListener, MouseListener{
		private static final long serialVersionUID = -8588620268314822037L;

		ClassPopup pop;
		ClassList(APEXApp app)
		{
			super(app.getNonLibraryClassesAsArray());
			getSelectionModel().addListSelectionListener(this);
			this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			pop = new ClassPopup(this);
			addMouseListener(this);
		}

		@Override
		public void valueChanged(ListSelectionEvent e)
		{
			if (e.getValueIsAdjusting())
				return;
			c = getSelectedValue();
			// Should class members 
			fieldsAndMethodsView.setViewportView(new ClassMemberTree(c));
		}
		
		@Override
		public void mouseClicked(MouseEvent e)
		{
			if (SwingUtilities.isRightMouseButton(e) && !isSelectionEmpty())
			{
				setSelectedIndex(locationToIndex(e.getPoint()));
				pop.show(e.getComponent(), e.getX(), e.getY());
			}
		}

		@Override public void mouseEntered(MouseEvent arg0){}
		@Override public void mouseExited(MouseEvent arg0){}
		@Override public void mousePressed(MouseEvent arg0){}
		@Override public void mouseReleased(MouseEvent arg0){}
	}
	
	class ClassPopup extends JPopupMenu {
		private static final long serialVersionUID = -6843258516249263247L;

		ClassPopup(ClassList cl)
		{
			JMenuItem openSmali;
			add(openSmali = new JMenuItem("Open Smali File"));
			openSmali.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent ae)
				{
					openFile(cl.getSelectedValue().smaliF, 0);
				}
			});
		}
	}
	
	class FileTree extends JTree implements TreeSelectionListener, MouseListener{
		private static final long serialVersionUID = 2006834603254079893L;
		FileNode root;
		FilePopup pop;
		int latestRow;
		FileTree(File rootDir)
		{
			super(new FileNode(rootDir));
			root = (FileNode) getModel().getRoot();
			//root.add(new FileNode(stegoBenmarkAppDir));
			setFont(new Font(getFont().getName(), getFont().getStyle(), getFont().getSize() + 5));
			getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
			addTreeSelectionListener(this);
			
			// Setting initial tree selection
			this.setSelectionPath(new TreePath(root.findFirst("app-debug").getPath()));
			
			this.addMouseListener(this);
			
			pop = new FilePopup(this);
		}
		
		FileNode initRoot(File f)
		{
			return root = new FileNode(f);
		}

		@Override
		public void valueChanged(TreeSelectionEvent e)
		{
			FileNode node = (FileNode) getLastSelectedPathComponent();
			if (node!=null && node.isLeaf())
				appQueue.add(node.f);
		}

		@Override
		public void mouseClicked(MouseEvent e)
		{
			if (SwingUtilities.isRightMouseButton(e))
			{
				latestRow = getClosestRowForLocation(e.getX(), e.getY());
				pop.show(e.getComponent(), e.getX(), e.getY());
			}
		}

		@Override public void mouseEntered(MouseEvent arg0){}
		@Override public void mouseExited(MouseEvent arg0){}
		@Override public void mousePressed(MouseEvent arg0){}
		@Override public void mouseReleased(MouseEvent arg0){}
	}
	
	class FilePopup extends JPopupMenu {
		private static final long serialVersionUID = -4769888463627371482L;

		FilePopup(FileTree tree)
		{
			JMenuItem itemShow, itemNotes, itemOpenDecoded, itemOpenCG, itemOpenTrees;
			add(itemShow = new JMenuItem("Show"));
			itemShow.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent ae)
				{
					tree.setSelectionRow(tree.latestRow);
				}
			});
			
			add(itemNotes = new JMenuItem("Open Notes Dir"));
			itemNotes.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent ae)
				{
					FileNode node = (FileNode) tree.getPathForRow(tree.latestRow).getLastPathComponent();
					File notesDir = new File(actualNotesRoot, node.f.getName());
					P.exec("explorer.exe \""+notesDir.getAbsolutePath()+"\"", false);
				}
			});
			
			add(itemOpenDecoded = new JMenuItem("Open Decoded Dir"));
			itemOpenDecoded.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent ae)
				{
					FileNode node = (FileNode) tree.getPathForRow(tree.latestRow).getLastPathComponent();
					File decodedDir = new File(decodedRoot, node.f.getName());
					P.exec("explorer.exe \""+decodedDir.getAbsolutePath()+"\"", false);
				}
			});
			
			add(itemOpenCG = new JMenuItem("Open CFG Dir"));
			itemOpenCG.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent ae)
				{
					FileNode node = (FileNode) tree.getPathForRow(tree.latestRow).getLastPathComponent();
					File cfgDir = new File(cfgRoot, node.f.getName());
					cfgDir.mkdirs();
					P.exec("explorer.exe \""+cfgDir.getAbsolutePath()+"\"", false);
				}
			});
			
			add(itemOpenCG = new JMenuItem("Open CG Dir"));
			itemOpenCG.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent ae)
				{
					FileNode node = (FileNode) tree.getPathForRow(tree.latestRow).getLastPathComponent();
					File cgDir = new File(cgRoot, node.f.getName());
					cgDir.mkdirs();
					P.exec("explorer.exe \""+cgDir.getAbsolutePath()+"\"", false);
				}
			});
			
			add(itemOpenTrees = new JMenuItem("Open Expression Trees Dir"));
			itemOpenTrees.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent ae)
				{
					FileNode node = (FileNode) tree.getPathForRow(tree.latestRow).getLastPathComponent();
					File treesDir = new File(treesRoot, node.f.getName());
					treesDir.mkdirs();
					P.exec("explorer.exe \""+treesDir.getAbsolutePath()+"\"", false);
				}
			});
		}
	}
	
	class FileNode extends DefaultMutableTreeNode {
		private static final long serialVersionUID = 203765169859598509L;
		
		File f;
		FileNode(File f)
		{
			super(f.getName());
			this.f = f;
			if (f.isDirectory() && !f.getName().contentEquals("instrumented"))
			for (File ff : f.listFiles())
				add(new FileNode(ff));
			
		}
		
		FileNode findFirst(String name)
		{
			if (f.getName().startsWith(name))
				return this;
			
			int count = getChildCount();
			for (int i=0; i<count; i++)
			{
				FileNode s = ((FileNode)getChildAt(i)).findFirst(name);
				if (s != null)
					return s;
			}
			
			return null;
		}
	}
	
	void startAppDecodeThread()
	{
		new Thread(new Runnable() {
			@Override
			public void run()
			{
				while (true) try
				{
					File f = appQueue.take();
					while (!appQueue.isEmpty())
						f = appQueue.take();
					setStatus("Loading app "+f.getAbsolutePath(), Color.yellow);
					app = apps.computeIfAbsent(f.getAbsolutePath(), k->new APEXApp(new File(k)));
					setStatus("Finished loading app "+f.getAbsolutePath());
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run()
						{
							classTreeView.setViewportView(new ClassList(app));
							fieldsAndMethodsView.setViewportView(null);
							loadNotes();
						}
					});
					cg = cgs.computeIfAbsent(f.getAbsolutePath(), k->new CallGraph(app));
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}

			}
		}).start();
	}
	
	void startMemoryMonitor()
	{
		new Thread(new Runnable() {
			@Override
			public void run()
			{
				while (true)
				{
					long bytes = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
					long kbs = bytes/1024;
					long mbs = kbs/1024;
					double gbs = mbs/1024.0;
					String usage = gbs<1?String.format("%d MBs", mbs):String.format("%.2f GBs", gbs);
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run()
						{
							memoryLabel.setText(usage);
						}
					});
					P.sleep(1000);
				}

			}
		}).start();
	}
	
	static void go() { UI.createAndShow(new HelperUI(), "App Analysis Helper"); }
	public static void main(String[] args) { go(); }
}
