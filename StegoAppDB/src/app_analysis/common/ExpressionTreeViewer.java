package app_analysis.common;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import apex.symbolic.Expression;
import app_analysis.trees.TreeRefUtils;
import util.F;
import util.P;

public class ExpressionTreeViewer extends JPanel implements ActionListener{
	private static final long serialVersionUID = 3038403584544261832L;
	
	private JFileChooser fc;
	private JTextField tf_path;
	static final File defaultDir = new File("C:\\workspace\\app_analysis\\notes\\ExpressionTrees");
	
	
	ExpressionTreeViewer() {
		super(new BorderLayout());
		
		JPanel top = new JPanel();
		fc = new JFileChooser();
		fc.setMultiSelectionEnabled(true);
		fc.setFileFilter(new FileNameExtensionFilter("expression files", "expression"));
		fc.setPreferredSize(new Dimension(800,500));
		
		JButton b_loadFile = new JButton("Load Files (*.expression)");
		tf_path = new JTextField(40);
		JButton b_loadPath = new JButton("Load");
		top.add(b_loadFile);
		top.add(Box.createHorizontalStrut(30));
		top.add(tf_path);
		top.add(b_loadPath);
		b_loadFile.addActionListener(this);
		b_loadFile.setActionCommand("load file");
		b_loadPath.addActionListener(this);
		b_loadPath.setActionCommand("load path");
		
		Font oldFont = b_loadFile.getFont();
		Font newFont = new Font(oldFont.getName(), oldFont.getStyle(), 20);
		b_loadFile.setFont(newFont);
		b_loadPath.setFont(newFont);
		add(top, BorderLayout.NORTH);
	}
	
	
	File prevDir = null;
	@Override
	public void actionPerformed(ActionEvent ae) {
		switch (ae.getActionCommand()) {
			case "load file":
				if (prevDir == null)
					prevDir = defaultDir;
				fc.setCurrentDirectory(prevDir);
				int returnVal = fc.showOpenDialog(ExpressionTreeViewer.this);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					File[] f = fc.getSelectedFiles();
					List<File> pdf = new ArrayList<>();
					for (File ff : f) {
						prevDir = ff.getParentFile();
						pdf.add(getPDF(ff));
					}
					for (File ff : pdf)
						showPDF(ff);
				}
				break;
			case "load path":
				String path = tf_path.getText();
				if (path.endsWith(".expression")) {
					File expF = new File(path);
					if (expF.exists()) {
						P.p(expF.getAbsolutePath());
						showPDF(getPDF(expF));
					} else
						P.p("File does not exist: "+path);
				} else
					P.p("Not a *.expression file");
				break;
		}
	}
	
	private File getPDF(File expF) {
		String name = expF.getName();
		name = name.substring(0, name.lastIndexOf("."));
		File fullF = new File(prevDir, name+"_full.pdf");
		Expression exp = null;
		if (!fullF.exists()) {
			exp = (Expression) F.readObject(expF);
			exp.toDotGraph(name, expF.getParentFile(), false);
		}
		File superTrimmedF = new File(prevDir, name+"_normalized_full.pdf");
		if (!superTrimmedF.exists()) {
			if (exp==null)
				exp = (Expression) F.readObject(expF);
			if (exp != null) {
				TreeRefUtils.normalize(exp);
				exp.toDotGraph(name+"_normalized", expF.getParentFile(), false);
			}
		}
		return superTrimmedF;
	}
	
	private void showPDF(File pdfF) {
		if (pdfF.exists())
			P.exec("explorer.exe \""+pdfF.getAbsolutePath()+"\"", false);
	}
	
	public static void main(String[] args) {
		
		SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                //Turn off metal's use of bold fonts
                createAndShowGUI();
            }
        });
	}
	
	private static void createAndShowGUI() {
        //Create and set up the window.
        JFrame frame = new JFrame("Expression Tree Viewer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(400,250));
        frame.setLocation(700, 400);
        //Add content to the window.
        frame.add(new ExpressionTreeViewer());
 
        //Display the window.
        frame.pack();
        frame.setVisible(true);
    }
}
