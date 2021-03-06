package src.menu.components;

import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.FlowLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;

import src.BasicSeamsCarver;

@SuppressWarnings("serial")
public class CarvingSchemeSelector extends JPanel {
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

	public BasicSeamsCarver.CarvingScheme carvingScheme() {
		if(verticalFirst.getState())
			return BasicSeamsCarver.CarvingScheme.VERTICAL_HORIZONTAL;
		else if(horizontalFirst.getState())
			return BasicSeamsCarver.CarvingScheme.HORIZONTAL_VERTICAL;
		else
			return BasicSeamsCarver.CarvingScheme.INTERMITTENT;
	}
}
