package uwsimresgen.view;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import DB.Database;

import uwsimresgen.model.ResultsModel;

public class HomeView extends JPanel implements IView  {

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
	
	
	/*
	private JLabel balanceLabel = new JLabel("Balance (dollars)");
	private JLabel balanceUnlimitedLabel = new JLabel("Unlimited?");
	private JLabel numLinesLabel = new JLabel("Lines");
	private JLabel lineBetLabel = new JLabel("LineBet");
	private JLabel numSpinsLabel = new JLabel("Spins");
	private JLabel denominationLabel = new JLabel("Denomination");
	
	private JCheckBox balanceUnlimitedCB = new JCheckBox("Unlimited?");
	private JTextField balanceTF = new JTextField(20);
	private JTextField numLinesTF = new JTextField(5);
	private JTextField lineBetTF = new JTextField(5);
	private JTextField numSpinsTF = new JTextField(20);
	private JTextField denominationTF = new JTextField(10);
	*/
	
	private JCheckBox genAllStopsCheckBox = new JCheckBox("Generate All Reel Stops?");
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
		this.add(selectFileTF, gbc);
		this.gbc.gridx = 2;
		this.gbc.gridy = 1;
		this.add(selectFile, gbc);
		
		this.gbc.gridx = 0;
		this.gbc.gridy = 2;
		this.gbc.anchor = GridBagConstraints.EAST;
		this.add(blocksFileLabel, gbc);
		this.gbc.gridx = 1;
		this.gbc.gridy = 2;
		this.gbc.anchor = GridBagConstraints.WEST;
		this.add(blocksFileTF, gbc);
		this.gbc.gridx = 2;
		this.gbc.gridy = 2;
		this.add(blocksFile, gbc);
		
		this.gbc.gridx = 0;
		this.gbc.gridy = 3;
		this.gbc.anchor = GridBagConstraints.EAST;
		this.add(prefixLabel, gbc);
		this.gbc.gridx = 1;
		this.gbc.gridy = 3;
		this.gbc.anchor = GridBagConstraints.WEST;
		this.add(prefixTF, gbc);
		this.gbc.gridx = 2;
		this.gbc.gridy = 3;
		this.gbc.anchor = GridBagConstraints.WEST;
		this.add(prefixButton, gbc);
		
		this.gbc.gridx = 0;
		this.gbc.gridy = 4;
		this.gbc.anchor = GridBagConstraints.EAST;
		this.add(dbnameLabel, gbc);
		this.gbc.gridx = 1;
		this.gbc.gridy = 4;
		this.gbc.anchor = GridBagConstraints.WEST;
		this.add(dbnameTF, gbc);
		this.gbc.gridx = 2;
		this.gbc.gridy = 4;
		this.gbc.anchor = GridBagConstraints.WEST;
		this.add(dbnameButton, gbc);
		
		
		
			
		JPanel jp = new JPanel();
		jp.setLayout(new GridBagLayout());
		
		this.gbc.insets.set(0, 10, 0, 0);
		
		this.gbc.gridx = 0;
		this.gbc.gridy = 0;
		this.gbc.gridwidth = 1;
		this.gbc.anchor = GridBagConstraints.EAST;
		jp.add(numlinesLabel, gbc);
		this.gbc.gridx = 1;
		this.gbc.gridy = 0;
		this.gbc.gridwidth = 1;
		this.gbc.anchor = GridBagConstraints.WEST;
		jp.add(numlinesTF, gbc);		
		this.gbc.gridx = 2;
		this.gbc.gridy = 0;
		this.gbc.gridwidth = 1;
		this.gbc.anchor = GridBagConstraints.EAST;
		
		jp.add(genAllStopsCheckBox, gbc);			
		
		this.gbc.gridx = 0;
		this.gbc.gridy = 5;
		this.gbc.gridwidth = 3;
		this.gbc.anchor = GridBagConstraints.EAST;
		this.gbc.insets.set(10, 0, 0, 0);
		this.add(jp, gbc);			
		
		
		
		this.gbc.gridx = 0;
		this.gbc.gridy = 6;
		this.gbc.gridwidth = 3;
		this.gbc.fill = GridBagConstraints.HORIZONTAL;
		this.gbc.anchor = GridBagConstraints.CENTER;
		this.gbc.insets.set(40, 0, 0, 0);
		this.add(runButton, gbc);			
	}
	
	private void registerControllers() {
		
		this.selectFile.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				
				File f = new File("config/");				
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
				
				File f = new File("config/");				
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
				this.selectFileTF.setText( "" );
			}
			
			File blocksFile = this.model.getBlocksFile();
			if( blocksFile != null ) {
				this.blocksFileTF.setText( blocksFile.getName() );
			} else {
				this.blocksFileTF.setText( "" );
			}
			
			this.prefixTF.setText(this.model.getTablePrefix());	
			this.dbnameTF.setText(this.model.getDBName());
			this.numlinesTF.setText(Short.toString(this.model.getGenAllNumLines()));
		}
		
		
		if( this.model.isRunning() ) {
			this.runButton.setEnabled(false);
			this.selectFile.setEnabled(false);
			this.blocksFile.setEnabled(false);
			this.genAllStopsCheckBox.setEnabled(false);
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
			this.genAllStopsCheckBox.setEnabled(true);
			this.prefixButton.setEnabled(true);
			this.dbnameButton.setEnabled(true);
			this.blocksFileTF.setEnabled(true);
			this.selectFileTF.setEnabled(true);
			this.prefixTF.setEnabled(true);
			this.dbnameTF.setEnabled(true);
			
			
			if( this.model.getGenAllStops() ) {
				this.blocksFile.setEnabled(false);
				this.blocksFileLabel.setEnabled(false);
				this.blocksFileTF.setEnabled(false);
				this.numlinesTF.setEnabled(true);
				this.numlinesLabel.setEnabled(true);
				
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
