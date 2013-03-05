package uwsimresgen.view;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import uwsimresgen.model.ResultsModel;

public class ProcessingView extends JPanel implements IView {

	private JProgressBar pb = new JProgressBar();
	
	private JLabel dbTableNameFormatLabel = new JLabel("DB Table Name Format");
	private JLabel dbTableNameFormatValueLabel = new JLabel("Not Available");
	
	private JLabel dbTableNameListLabel = new JLabel("Available Values for <Table Name>");
	private JLabel dbTableNameListValueLabel = new JLabel(
			ResultsModel.BLOCKS_TABLE_NAME
			+ ", " + ResultsModel.RESULTS_TABLE_NAME
			+ ", " + ResultsModel.SYMBOLS_TABLE_NAME
			+ ", " + ResultsModel.PAYLINES_TABLE_NAME
			+ ", " + ResultsModel.BASE_PAYTABLE_TABLE_NAME
			+ ", " + ResultsModel.BONUS_PAYTABLE_TABLE_NAME
//			+ ", " + ResultsModel.BONUS_SPIN_ODDS_TABLE_NAME
			+ ", " + ResultsModel.REELMAPPINGS_TABLE_NAME
		);
	
	private JLabel outputLogLabel = new JLabel("Output Log Location");
	private JLabel outputLogValueLabel = new JLabel("Not Available");
	
	private JLabel processingLabel = new JLabel("Generating Results...");
	private JButton cancelButton = new JButton("Cancel");
	private JButton pauseButton = new JButton("Pause");
	
	private JLabel totalSpinsLabel = new JLabel("0");
	private JLabel currSpinLabel = new JLabel("0");
	//private JLabel currConsumedSpinLabel = new JLabel("0");
	
	private ResultsModel model;
		
	private GridBagLayout layout;
	private GridBagConstraints gbc;
	
	public ProcessingView( ResultsModel model ) {
		super();
		this.model = model;
		this.layoutView();
		this.registerControllers();
		this.model.AddView(this);
	}
	
	private void layoutView() {
		this.layout = new GridBagLayout();
		this.gbc = new GridBagConstraints();
		
		Font f = new Font("Arial", Font.BOLD, 18);
		
		this.pauseButton.setEnabled(false);
		this.cancelButton.setEnabled(false);
		//this.pb.setIndeterminate(true);
		this.processingLabel.setFont(f);
		this.dbTableNameFormatValueLabel.setForeground(Color.GRAY);
		this.dbTableNameListValueLabel.setForeground(Color.GRAY);
		this.outputLogValueLabel.setForeground(Color.GRAY);
		
		
		this.setLayout(layout);
		this.gbc.insets = new Insets(20,10,10,10);
		
		this.gbc.gridx = 0;
		this.gbc.gridy = 0;
		this.gbc.gridwidth = 4;
		this.gbc.anchor = GridBagConstraints.CENTER;
		this.add(processingLabel, gbc);
		
		
		this.gbc.insets = new Insets(5,2,5,2);
		
		JPanel jp = new JPanel();
		jp.setLayout(new GridBagLayout());
				
		this.gbc.gridx = 0;
		this.gbc.gridy = 0;
		this.gbc.gridwidth = 4;
		this.gbc.weightx = 4;
		this.gbc.anchor = GridBagConstraints.CENTER;
		jp.add(this.dbTableNameFormatLabel, gbc);
		this.gbc.gridy = 1;
		jp.add(this.dbTableNameFormatValueLabel, gbc);	
		this.gbc.gridy = 2;
		jp.add(this.dbTableNameListLabel, gbc);
		this.gbc.gridy = 3;
		jp.add(this.dbTableNameListValueLabel, gbc);	
			
				
		this.gbc.gridx = 0;
		this.gbc.gridy = 1;
		this.gbc.gridwidth = 4;
		this.gbc.anchor = GridBagConstraints.CENTER;
		this.add(jp, gbc);
			
		
		this.gbc.insets = new Insets(5,2,5,2);
		
		JPanel jp3 = new JPanel();
		jp3.setLayout(new GridBagLayout());
				
		this.gbc.gridx = 0;
		this.gbc.gridy = 0;
		this.gbc.gridwidth = 4;
		this.gbc.weightx = 4;
		this.gbc.anchor = GridBagConstraints.CENTER;
		jp3.add(outputLogLabel, gbc);
		this.gbc.gridx = 0;
		this.gbc.gridy = 1;
		this.gbc.weightx = 4;
		this.gbc.gridwidth = 4;
		this.gbc.anchor = GridBagConstraints.CENTER;
		jp3.add(outputLogValueLabel, gbc);		
		
		this.gbc.gridx = 0;
		this.gbc.gridy = 2;
		this.gbc.gridwidth = 4;
		this.gbc.anchor = GridBagConstraints.CENTER;
		this.add(jp3, gbc);
			
		
		
		
		this.gbc.insets = new Insets(10,10,10,10);
		
		JPanel jp2 = new JPanel();
		jp2.setLayout(new GridBagLayout());
		
		
		this.gbc.gridx = 1;
		this.gbc.gridy = 0;
		this.gbc.gridwidth = 1;
		this.gbc.weightx = 1;
		this.gbc.anchor = GridBagConstraints.WEST;
		jp2.add(currSpinLabel, gbc);
		this.gbc.gridx = 1;
		this.gbc.gridy = 0;
		this.gbc.gridwidth = 1;
		this.gbc.weightx = 1;
		this.gbc.anchor = GridBagConstraints.WEST;
		//jp2.add(currConsumedSpinLabel, gbc);
		this.gbc.gridx = 2;
		this.gbc.gridy = 0;
		this.gbc.weightx = 2;
		this.gbc.gridwidth = 2;
		this.gbc.anchor = GridBagConstraints.WEST;
		jp2.add(totalSpinsLabel, gbc);		
			
		this.gbc.gridx = 0;
		this.gbc.gridy = 3;
		this.gbc.gridwidth = 4;
		this.gbc.anchor = GridBagConstraints.CENTER;
		this.add(jp2, gbc);
			
		
		
		this.gbc.insets = new Insets(10,10,10,10);
				
		this.gbc.gridx = 0;
		this.gbc.gridy = 4;
		this.gbc.gridwidth = 4;
		this.gbc.fill = GridBagConstraints.HORIZONTAL;
		this.gbc.anchor = GridBagConstraints.CENTER;
		this.add(pb, gbc);		
		
		this.gbc.insets = new Insets(10,10,20,10);
				
		this.gbc.gridx = 0;
		this.gbc.gridy = 5;
		this.gbc.gridwidth = 2;
		this.gbc.weightx = 2;
		this.gbc.anchor = GridBagConstraints.EAST;
		this.gbc.fill = GridBagConstraints.HORIZONTAL;
		this.add(cancelButton, gbc);
		this.gbc.gridx = 2;
		this.gbc.gridy = 5;
		this.gbc.weightx = 2;
		this.gbc.gridwidth = 2;
		this.gbc.fill = GridBagConstraints.HORIZONTAL;
		this.gbc.anchor = GridBagConstraints.WEST;
		this.add(pauseButton, gbc);		
		
	}
	
	private void registerControllers() {
		
		this.cancelButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				ProcessingView.this.model.cancel();
			}
			
		});
		
		this.pauseButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				if( pauseButton.getText().compareTo("Pause") == 0 ) {					
					ProcessingView.this.model.pause();
				} else {					
					ProcessingView.this.model.resume();
				}		
			}
			
		});
		
		
		
	}	
	
	
	@Override
	public void updateView() {
		
		if( this.model.isRunning() ) {
			this.cancelButton.setEnabled(true);
			this.pauseButton.setEnabled(true);
			if( this.model.isPaused() ) {
				this.processingLabel.setText("Generating Results...Paused");
			} else {
				this.processingLabel.setText("Generating Results...Running");
			}
		} else {
			//this.dbTableNameValueLabel.setText(this.model.getDBTableName());
			//this.dbPaytableTableNameValueLabel.setText(this.model.getDBTableName()); // TODO: FIX NAME
			//this.dbReelStopsTableNameValueLabel.setText(this.model.getDBTableName()); // TODO: FIX NAME
			
			this.dbTableNameFormatValueLabel.setText(
					this.model.getTablePrefix()
					+ "_<Table Name>_"
					+ this.model.getTableSuffix()
				);
			
			
			this.outputLogValueLabel.setText(this.model.getOutputLogFilePath());
			this.cancelButton.setEnabled(false);
			this.pauseButton.setEnabled(false);
			
			if( this.model.isError() ) {
				this.processingLabel.setText("Error...Check Log");
			} else {
				this.processingLabel.setText("Ready");
			}			
		}
		
		if( this.model.isPaused() ) {
			this.pauseButton.setText("Resume");
		} else {
			this.pauseButton.setText("Pause");
		}	
		
		this.currSpinLabel.setText("Produced: " + Integer.toString(this.model.getCurrSpin()));
		//this.currConsumedSpinLabel.setText("Consumed: " + Integer.toString(this.model.getCurrConsumedSpin()));
		this.totalSpinsLabel.setText("Spins: " + Integer.toString(this.model.getTotalSpins()));
		this.pb.setMaximum(this.model.getTotalSpins());
		this.pb.setValue(this.model.getCurrSpin()); //getCurrConsumedSpin());	
		this.pb.repaint();
	}

}
