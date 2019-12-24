package ui;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;


@SuppressWarnings("serial")
public class ProgressUI extends JPanel{

	
	public static ProgressUI create(String title)
	{
		return create(title, 20);
	}
	
	public static ProgressUI create(String title, int labelCount)
	{
		ProgressUI ui = new ProgressUI(labelCount);
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run()
			{
				JFrame f = new JFrame("Progress of " + title);
				f.add(ui);
				ui.frame = f;
				f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				
				f.setMinimumSize(new Dimension(500,400));
				f.setVisible(true);
				f.pack();
			}
		});
		return ui;
	}
	
	private List<JLabel> labels;
	private List<String> texts;
	private int labelCount;
	public JFrame frame;
	
	public ProgressUI(int count)
	{
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		setAlignmentX(LEFT_ALIGNMENT);
		this.labelCount = Math.max(count, 1);
		labels = new ArrayList<>();
		for (int i = 0; i < labelCount; i++)
			labels.add(new JLabel("line " + (i+1)));
		for (JLabel l : labels)
			add(l);
		texts = new ArrayList<>();
	}
	
	public void newLine(String text)
	{
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run()
			{	
				texts.add(text);
				if (texts.size()>labelCount)
					texts.remove(0);
				for (int i = 0; i < texts.size(); i++)
				{
					labels.get(i).setText(texts.get(i));
				}
			}
		});
	}
	
	public void appendToLastLine(String text)
	{
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run()
			{	
				if (texts.isEmpty())
				{
					texts.add(text);
					for (int i = 0; i < texts.size(); i++)
					{
						labels.get(i).setText(texts.get(i));
					}
				}
				else
				{
					int index = texts.size()-1;
					texts.add(texts.remove(index)+text);
					labels.get(index).setText(texts.get(index));
				}
			}
		});
	}
	
}