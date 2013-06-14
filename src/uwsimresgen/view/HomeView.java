package uwsimresgen.view;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import uwsimresgen.model.ResultsModel;
import uwsimresgen.model.ResultsModel.Mode;

public class HomeView extends JPanel implements IView  {
	private final String CONFIG_PATH = "D:/Linus_Documents/Git Projects/UWSimResGen/config";
	
	private JFileChooser fc = new JFileChooser();	
	private JLabel selectFileLabel = new JLabel("Slot Machine Configuration");
	private JTextField selectFileTF = new JTextField(20);
	private JButton selectFile = new JButton("Browse...");
	
	private JLabel blocksFileLabel = new JLabel("Blocks Configuration");
	private JTextField blocksFileTF = new JTextField(20);
	private JButton blocksFile = new JButton("Browse...");
	
	private JLabel prefixLabel = new JLabel("DB Table Prefix");
	private JTextField prefixTF = new JTextField(20);
	private JButton prefixButton = new JButton("Reset");
	
	private JLabel dbnameLabel = new JLabel("DB Name");
	private JTextField dbnameTF = new JTextField(20);
	private JButton dbnameButton = new JButton("Reset");
		
	private JLabel modeLabel = new JLabel("Game Modes:");
	private JRadioButton msButton = new JRadioButton("Money Storm");
	private JRadioButton dtButton = new JRadioButton("Dolphin Treasure");
	private ButtonGroup modeGroup = new ButtonGroup();
	
	private JLabel optionsLabel = new JLabel("Options:");
	private JCheckBox genAllStopsCheckBox = new JCheckBox("Generate All Reel Stops?");
	private JCheckBox genNoTableCheckBox = new JCheckBox("Do NOT Create Spin Results DB Table");
	private JCheckBox genAllBonusSpinsCheckBox = new JCheckBox("Generate All Bonus Spins");
	private JCheckBox genGamblersRuinCheckBox = new JCheckBox("Simulate Gamblers Ruin Scenario");
	private JCheckBox genPrizeSizeCheckBox = new JCheckBox("Generate Prize Size DB Table");
	private JCheckBox genForcedFreeSpinsCheckBox = new JCheckBox("Simulate Forced Free Spins");
	private JCheckBox genBettingStrategyCheckBox = new JCheckBox("Simulate Betting Strategies");
	
	private JButton runButton = new JButton("Generate Results");

	private JLabel numlinesLabel = new JLabel("Number of Lines:");
	private JTextField numlinesTF = new JTextField(5);
	
	private ResultsModel model;
	
	private GridBagLayout layout;
	private GridBagConstraints gbc;
	
	
	public HomeView( ResultsModel model ) {
		super();
		this.model = model;
		this.layoutView();
		this.registerControllers();
		this.model.AddView(this);
	}
	
	private void layoutView() {
		this.fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
		this.fc.setFileFilter(new XMLFileFilter());
		this.selectFileTF.setEditable(false);
		this.selectFileTF.setBackground(Color.WHITE);
		this.blocksFileTF.setEditable(false);
		this.blocksFileTF.setBackground(Color.WHITE);
		
		// Group up mode radio buttons
		modeGroup.add(msButton);
		modeGroup.add(dtButton);
		
		this.layout = new GridBagLayout();
		this.gbc = new GridBagConstraints();
		
		this.setLayout(layout);
		this.gbc.insets = new Insets(10,10,0,0);
		
		this.gbc.gridx = 0;
		this.gbc.gridy = 1;
		this.gbc.anchor = GridBagConstraints.EAST;
		this.add(selectFileLabel, gbc);
		this.gbc.gridx = 1;
		this.gbc.gridy = 1;
		this.gbc.anchor = GridBagConstraints.WEST;
		this.gbc.fill = GridBagConstraints.HORIZONTAL;
		this.add(selectFileTF, gbc);
		this.gbc.gridx = 2;
		this.gbc.gridy = 1;
		this.gbc.fill = GridBagConstraints.NONE;
		this.add(selectFile, gbc);
		
		this.gbc.gridx = 0;
		this.gbc.gridy = 2;
		this.gbc.anchor = GridBagConstraints.EAST;
		this.add(blocksFileLabel, gbc);
		this.gbc.gridx = 1;
		this.gbc.gridy = 2;
		this.gbc.anchor = GridBagConstraints.WEST;
		this.gbc.fill = GridBagConstraints.HORIZONTAL;
		this.add(blocksFileTF, gbc);
		this.gbc.gridx = 2;
		this.gbc.gridy = 2;
		this.gbc.fill = GridBagConstraints.NONE;
		this.add(blocksFile, gbc);
		
		this.gbc.gridx = 0;
		this.gbc.gridy = 3;
		this.gbc.anchor = GridBagConstraints.EAST;
		this.add(prefixLabel, gbc);
		this.gbc.gridx = 1;
		this.gbc.gridy = 3;
		this.gbc.anchor = GridBagConstraints.WEST;
		this.gbc.fill = GridBagConstraints.HORIZONTAL;
		this.add(prefixTF, gbc);
		this.gbc.gridx = 2;
		this.gbc.gridy = 3;
		this.gbc.anchor = GridBagConstraints.WEST;
		this.gbc.fill = GridBagConstraints.NONE;
		this.add(prefixButton, gbc);
		
		this.gbc.gridx = 0;
		this.gbc.gridy = 4;
		this.gbc.anchor = GridBagConstraints.EAST;
		this.add(dbnameLabel, gbc);
		this.gbc.gridx = 1;
		this.gbc.gridy = 4;
		this.gbc.anchor = GridBagConstraints.WEST;
		this.gbc.fill = GridBagConstraints.HORIZONTAL;
		this.add(dbnameTF, gbc);
		this.gbc.gridx = 2;
		this.gbc.gridy = 4;
		this.gbc.anchor = GridBagConstraints.WEST;
		this.gbc.fill = GridBagConstraints.NONE;
		this.add(dbnameButton, gbc);
		
		// Laying out game mode radio buttons
		msButton.setSelected(true);
		JPanel modePanel = new JPanel();
		modePanel.setLayout(new GridLayout(0, 1));
		modePanel.add(modeLabel);
		modePanel.add(msButton);
		modePanel.add(dtButton);
		
		this.gbc.gridx = 0;
		this.gbc.gridy = 5;
		this.gbc.anchor = GridBagConstraints.FIRST_LINE_START;
		this.gbc.insets.set(10, 0, 0, 50);
		this.add(modePanel, gbc);
		
		// Laying out options 
		JPanel optionsPanel = new JPanel(new GridBagLayout());
		this.gbc.gridx = 0;
		this.gbc.gridy = 0;
		this.gbc.anchor = GridBagConstraints.LINE_START;
		this.gbc.insets.set(0, 20, 0, 0);
		optionsPanel.add(optionsLabel, gbc);
		
		this.gbc.gridx = 0;
		this.gbc.gridy = 1;
		this.gbc.insets.set(0, 30, 0, 0);
		optionsPanel.add(genAllStopsCheckBox, gbc);
		
		this.gbc.gridx = 1;
		this.gbc.gridy = 1;
		optionsPanel.add(numlinesLabel, gbc);
		
		this.gbc.gridx = 2;
		this.gbc.gridy = 1;
		optionsPanel.add(numlinesTF, gbc);				
		
		this.gbc.gridx = 0;
		this.gbc.gridy = 2;
		optionsPanel.add(genAllBonusSpinsCheckBox, gbc);
		optionsPanel.add(genBettingStrategyCheckBox, gbc);
		
		this.gbc.gridx = 0;
		this.gbc.gridy = 3;
		optionsPanel.add(genGamblersRuinCheckBox, gbc);
		
		this.gbc.gridx = 0;
		this.gbc.gridy = 4;
		optionsPanel.add(genPrizeSizeCheckBox, gbc);
		
		this.gbc.gridx = 0;
		this.gbc.gridy = 5;
		optionsPanel.add(genForcedFreeSpinsCheckBox, gbc);
		
		this.gbc.gridx = 0;
		this.gbc.gridy = 6;
		optionsPanel.add(genNoTableCheckBox, gbc);
		
		this.gbc.gridx = 1;
		this.gbc.gridy = 5;
		this.gbc.anchor = GridBagConstraints.FIRST_LINE_START;
		this.gbc.insets.set(4, 10, 0, 10);
		this.add(optionsPanel, gbc);
		
		this.gbc.gridx = 0;
		this.gbc.gridy = 6;
		this.gbc.gridwidth = 3;
		this.gbc.fill = GridBagConstraints.HORIZONTAL;
		this.gbc.anchor = GridBagConstraints.CENTER;
		this.gbc.insets.set(20, 0, 0, 0);
		this.add(runButton, gbc);			
	}
	
	private void registerControllers() {
		
		this.selectFile.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				
				File f = new File(CONFIG_PATH);				
				fc.setCurrentDirectory(f);
							
				int result = fc.showOpenDialog(HomeView.this);
				
				if( result == JFileChooser.APPROVE_OPTION ) {
					File file = fc.getSelectedFile();
					if( file.getName().endsWith(".xml") ) {
						HomeView.this.model.setConfigFile(file);					
					} else {
						JOptionPane.showMessageDialog(
							HomeView.this,
							"You must choose an XML file type",
							"Bad File Type",
							JOptionPane.ERROR_MESSAGE
						);
					}
				}			
			}
			
		});
		
		this.blocksFile.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				
				File f = new File(CONFIG_PATH);				
				fc.setCurrentDirectory(f);				
				
				int result = fc.showOpenDialog(HomeView.this);
				
				if( result == JFileChooser.APPROVE_OPTION ) {
					File file = fc.getSelectedFile();
					if( file.getName().endsWith(".xml") ) {
						HomeView.this.model.setBlocksFile(file);					
					} else {
						JOptionPane.showMessageDialog(
							HomeView.this,
							"You must choose an XML file type",
							"Bad File Type",
							JOptionPane.ERROR_MESSAGE
						);
					}
				} 			
			}
			
		});
		
		
		this.prefixTF.addFocusListener(new FocusListener() {

			@Override
			public void focusGained(FocusEvent arg0) {				
				prefixTF.selectAll();
			}

			@Override
			public void focusLost(FocusEvent arg0) {
				HomeView.this.model.setTablePrefix(prefixTF.getText().trim());
	
			}
						
		});
		
		this.prefixButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				HomeView.this.model.setTablePrefix();			
			}
			
		});
		
		
		
		this.dbnameTF.addFocusListener(new FocusListener() {

			@Override
			public void focusGained(FocusEvent arg0) {				
				dbnameTF.selectAll();
			}

			@Override
			public void focusLost(FocusEvent arg0) {
				HomeView.this.model.setDBName(dbnameTF.getText().trim());
			}
						
		});
		
		this.dbnameButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				HomeView.this.model.setDBName();			
			}
			
		});
		
		
		this.msButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				HomeView.this.model.setMode(uwsimresgen.model.ResultsModel.Mode.MONEY_STORM);
				HomeView.this.model.setDBName("MoneyStormDB");
			}
		});
		
		
		this.dtButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				HomeView.this.model.setMode(uwsimresgen.model.ResultsModel.Mode.DOLPHIN_TREASURE);
				HomeView.this.model.setDBName("DolphinTreasureDB");
			}
		});
		
		this.genAllStopsCheckBox.addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(ItemEvent arg0) {
				if( arg0.getStateChange() == ItemEvent.SELECTED ) {
					HomeView.this.model.setGenAllStops(true);
				} else {
					HomeView.this.model.setGenAllStops(false);
				}
				
			}

			
		});
		
		this.genNoTableCheckBox.addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(ItemEvent arg0) {
				if( arg0.getStateChange() == ItemEvent.SELECTED ) {
					HomeView.this.model.setCreateSpinTable(false);
				} else {
					HomeView.this.model.setCreateSpinTable(true);
				}
				
			}

			
		});
		
		this.genAllBonusSpinsCheckBox.addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(ItemEvent arg0) {
				if( arg0.getStateChange() == ItemEvent.SELECTED ) {
					HomeView.this.model.setGenAllBonusSpin(true);
				} else {
					HomeView.this.model.setGenAllBonusSpin(false);
				}
				
			}

			
		});
		
		this.genGamblersRuinCheckBox.addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(ItemEvent arg0) {
				if( arg0.getStateChange() == ItemEvent.SELECTED ) {
					HomeView.this.model.setGenGamblersRuin(true);
				} else {
					HomeView.this.model.setGenGamblersRuin(false);
				}
				
			}

			
		});
		
		this.genPrizeSizeCheckBox.addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(ItemEvent arg0) {
				if( arg0.getStateChange() == ItemEvent.SELECTED ) {
					HomeView.this.model.setGenPrizeSize(true);
				} else {
					HomeView.this.model.setGenPrizeSize(false);
				}
				
			}

			
		});
		
		this.genForcedFreeSpinsCheckBox.addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(ItemEvent arg0) {
				if( arg0.getStateChange() == ItemEvent.SELECTED ) {
					HomeView.this.model.setGenForcedFreeSpins(true);
				} else {
					HomeView.this.model.setGenForcedFreeSpins(false);
				}
				
			}

			
		});
		
		this.genBettingStrategyCheckBox.addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(ItemEvent arg0) {
				if( arg0.getStateChange() == ItemEvent.SELECTED ) {
					HomeView.this.model.setGenBettingStrategies(true);
				} else {
					HomeView.this.model.setGenBettingStrategies(false);
				}
				
			}

			
		});
		
		this.numlinesTF.addFocusListener(new FocusListener() {

			@Override
			public void focusGained(FocusEvent arg0) {
				
				numlinesTF.selectAll();
			}

			@Override
			public void focusLost(FocusEvent arg0) {
				
				try {
					short value = Short.parseShort(numlinesTF.getText());
					HomeView.this.model.setGenAllNumLines(value);					
				} catch( Exception e ) {
					HomeView.this.model.setGenAllNumLines(HomeView.this.model.getGenAllNumLines());
				}
							
			}
						
		});
		
		this.numlinesTF.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				try {
					short value = Short.parseShort(numlinesTF.getText());
					HomeView.this.model.setGenAllNumLines(value);					
				} catch( Exception e ) {
					HomeView.this.model.setGenAllNumLines(HomeView.this.model.getGenAllNumLines());
				}				
			}
			
		});
		
		
		
		
		this.runButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				if( HomeView.this.model.isSetupValid() ) {
					HomeView.this.model.launch();
				} else {
					JOptionPane.showMessageDialog(
						HomeView.this,
						"Your setup is not valid!",
						"Bad Setup",
						JOptionPane.ERROR_MESSAGE
					);
				}
			}			
		});		
	}
	
	class XMLFileFilter extends javax.swing.filechooser.FileFilter {
		public boolean accept(File f) {
			return f.isDirectory() || f.getName().toLowerCase().endsWith(".xml");
		}
		
		public String getDescription() {
			return ".xml files";
		}
	}
	
	@Override
	public void updateView() {
		
		
		if( !this.model.isRunning() ) {
		
			File configFile = this.model.getConfigFile();
			if( configFile != null ) {
				this.selectFileTF.setText( configFile.getName() );
			} else {
				this.selectFileTF.setText("");
			}
			
			File blocksFile = this.model.getBlocksFile();
			if( blocksFile != null ) {
				this.blocksFileTF.setText( blocksFile.getName() );
			} else {
				this.blocksFileTF.setText("");
			}
			
			this.prefixTF.setText(this.model.getTablePrefix());	
			this.dbnameTF.setText(this.model.getDBName());
			this.numlinesTF.setText(Short.toString(this.model.getGenAllNumLines()));
			
			this.numlinesLabel.setVisible(false);
			this.numlinesTF.setVisible(false);
		}
		
		
		if( this.model.isRunning() ) {
			this.runButton.setEnabled(false);
			this.selectFile.setEnabled(false);
			this.blocksFile.setEnabled(false);
			
			this.msButton.setEnabled(false);
			this.dtButton.setEnabled(false);
			
			this.genAllStopsCheckBox.setEnabled(false);
			this.genNoTableCheckBox.setEnabled(false);
			this.genAllBonusSpinsCheckBox.setEnabled(false);
			this.genGamblersRuinCheckBox.setEnabled(false);
			this.genPrizeSizeCheckBox.setEnabled(false);
			this.genBettingStrategyCheckBox.setEnabled(false);
			
			this.numlinesTF.setEnabled(false);
			this.prefixButton.setEnabled(false);
			this.dbnameButton.setEnabled(false);
			this.blocksFileTF.setEnabled(false);
			this.selectFileTF.setEnabled(false);
			this.prefixTF.setEnabled(false);
			this.dbnameTF.setEnabled(false);	
			
		} else {
			this.runButton.setEnabled(true);
			this.selectFile.setEnabled(true);
			this.prefixButton.setEnabled(true);
			this.dbnameButton.setEnabled(true);
			this.blocksFileTF.setEnabled(true);
			this.selectFileTF.setEnabled(true);
			this.prefixTF.setEnabled(true);
			this.dbnameTF.setEnabled(true);
			
			if (this.model.getMode() == Mode.MONEY_STORM) {
				this.genAllStopsCheckBox.setEnabled(true);
				this.genNoTableCheckBox.setEnabled(true);
				
				this.genAllBonusSpinsCheckBox.setVisible(true);
				this.genGamblersRuinCheckBox.setVisible(true);
				this.genPrizeSizeCheckBox.setVisible(true);
				this.genForcedFreeSpinsCheckBox.setVisible(true);
				this.genAllBonusSpinsCheckBox.setEnabled(true);
				this.genGamblersRuinCheckBox.setEnabled(true);
				this.genPrizeSizeCheckBox.setEnabled(true);
				this.genForcedFreeSpinsCheckBox.setEnabled(true);
				this.genBettingStrategyCheckBox.setVisible(false);
			} else {
				this.genAllStopsCheckBox.setEnabled(true);
				this.genNoTableCheckBox.setEnabled(true);
				this.genBettingStrategyCheckBox.setVisible(true);
				this.genBettingStrategyCheckBox.setEnabled(true);
				
				this.genAllBonusSpinsCheckBox.setVisible(false);
				this.genGamblersRuinCheckBox.setVisible(false);
				this.genPrizeSizeCheckBox.setVisible(false);
				this.genForcedFreeSpinsCheckBox.setVisible(false);
				
				this.prefixTF.setText(this.model.getDTTablePrefix());	
				this.dbnameTF.setText(this.model.getDBName());
			}
			
			this.numlinesLabel.setVisible(false);
			this.numlinesTF.setVisible(false);
			
			this.msButton.setEnabled(true);
			this.dtButton.setEnabled(true);
			
			if( this.model.getGenAllStops() ) {
				this.blocksFile.setEnabled(false);
				this.blocksFileLabel.setEnabled(false);
				this.blocksFileTF.setEnabled(false);
				this.numlinesTF.setEnabled(true);
				this.numlinesLabel.setEnabled(true);
				this.genAllBonusSpinsCheckBox.setEnabled(false);
				this.genGamblersRuinCheckBox.setEnabled(false);
				this.genForcedFreeSpinsCheckBox.setEnabled(false);
				
				this.numlinesLabel.setVisible(true);
				this.numlinesTF.setVisible(true);
				
			}
			
			else if (this.model.getGenGamblersRuin()) {
				this.blocksFile.setEnabled(true);
				this.blocksFileLabel.setEnabled(true);
				this.blocksFileTF.setEnabled(true);
				
				this.genAllStopsCheckBox.setEnabled(false);
				this.genNoTableCheckBox.setEnabled(true);
				this.genAllBonusSpinsCheckBox.setEnabled(false);
				this.genPrizeSizeCheckBox.setEnabled(false);
				this.genForcedFreeSpinsCheckBox.setEnabled(false);
			}
			
			else if (this.model.getGenPrizeSize()) {
				this.blocksFile.setEnabled(true);
				this.blocksFileLabel.setEnabled(true);
				this.blocksFileTF.setEnabled(true);
				this.genGamblersRuinCheckBox.setEnabled(false);
				this.genForcedFreeSpinsCheckBox.setEnabled(false);
				
				if (this.model.getGenAllBonusSpin()) {
					this.genAllStopsCheckBox.setEnabled(false);
					this.numlinesTF.setEnabled(false);
					this.numlinesLabel.setEnabled(false);
				}
			}
			
			else if (this.model.getGenAllBonusSpin()) {
				this.blocksFile.setEnabled(true);
				this.blocksFileLabel.setEnabled(true);
				this.blocksFileTF.setEnabled(true);
				
				this.numlinesTF.setEnabled(false);
				this.numlinesLabel.setEnabled(false);
				
				this.genAllStopsCheckBox.setEnabled(false);
				this.genNoTableCheckBox.setEnabled(true);
				this.genGamblersRuinCheckBox.setEnabled(false);
				this.genForcedFreeSpinsCheckBox.setEnabled(false);
			}
			
			else if (this.model.getGenForcedFreeSpins()){
				this.blocksFile.setEnabled(true);
				this.blocksFileLabel.setEnabled(true);
				this.blocksFileTF.setEnabled(true);
				
				this.genAllStopsCheckBox.setEnabled(false);
				this.genAllBonusSpinsCheckBox.setEnabled(false);
				this.genGamblersRuinCheckBox.setEnabled(false);
				this.genPrizeSizeCheckBox.setEnabled(false);
			
				
			} else {
				this.blocksFile.setEnabled(true);
				this.blocksFileLabel.setEnabled(true);
				this.blocksFileTF.setEnabled(true);
				this.numlinesTF.setEnabled(false);
				this.numlinesLabel.setEnabled(false);
			}
			
		}
		
	}
	
}
