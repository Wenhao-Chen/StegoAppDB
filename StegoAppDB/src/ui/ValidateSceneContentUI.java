package ui;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JPanel;

@SuppressWarnings("serial")
public class ValidateSceneContentUI extends JPanel{

	ValidateSceneContentUI()
	{
		this.setLayout(new BorderLayout());
		
		JPanel top = new JPanel() {
			@Override
			public Dimension getPreferredSize()
			{
				Dimension parentSize = getParent().getSize();
				return new Dimension((int)parentSize.getWidth(), 200);
			}
		};
		top.setBackground(UI.randomColor());
		top.setMinimumSize(new Dimension(1, 200));
		JPanel left = new JPanel();
		left.setBackground(UI.randomColor());
		JPanel right = new JPanel();
		right.setBackground(UI.randomColor());
		JPanel center = new JPanel();
		center.setBackground(UI.randomColor());
		JPanel bottom = new JPanel();
		bottom.setBackground(UI.randomColor());
		
		
		this.add(top, BorderLayout.PAGE_START);
		this.add(left, BorderLayout.LINE_START);
		this.add(right, BorderLayout.LINE_END);
		this.add(bottom, BorderLayout.PAGE_END);
		this.add(center, BorderLayout.CENTER);
	}
	
	
	public static void main(String[] args)
	{
		UI.createAndShow(new ValidateSceneContentUI(), "Validate Scene Content");
	}
	
}
