package edu.cg.menu.components;

import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.ComponentOrientation;
import java.awt.FlowLayout;

import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;

import edu.cg.BasicSeamsCarver.CarvingScheme;

@SuppressWarnings("serial")
public class CarvingSchemeSelector extends JPanel {
	private JFormattedTextField carvingScheme;
	private JFormattedTextField height;
	
	private Checkbox verticalFirst;
	private Checkbox horizontalFirst;
	private Checkbox intermittent;
	
	public CarvingSchemeSelector() {
		super();
		
		setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
		setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		
		JLabel schemeLabel = new JLabel("Carving scheme     ");
		add(schemeLabel);
		
		
		CheckboxGroup group = new CheckboxGroup();
		verticalFirst = new Checkbox("Vertical first  ", group, true);
		horizontalFirst = new Checkbox("Horizontal first  ", group, false);
		intermittent = new Checkbox("Intermittent", group, false);
		add(verticalFirst);
		add(horizontalFirst);
		add(intermittent);
	}

	public CarvingScheme carvingScheme() {
		if(verticalFirst.getState())
			return CarvingScheme.VERTICAL_HORIZONTAL;
		else if(horizontalFirst.getState())
			return CarvingScheme.HORIZONTAL_VERTICAL;
		else
			return CarvingScheme.INTERMITTENT;
	}
}
