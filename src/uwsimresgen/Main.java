package uwsimresgen;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;

import javax.swing.JFrame;

import uwsimresgen.model.ResultsModel;
import uwsimresgen.view.HomeView;
import uwsimresgen.view.IView;
import uwsimresgen.view.ProcessingView;

public class Main {

	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		Toolkit toolkit = Toolkit.getDefaultToolkit();
		Dimension dim = toolkit.getScreenSize();
		
		JFrame f = new JFrame();
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.setSize(800, 850);
		f.setLocation( (int)(dim.width * 0.5 - f.getSize().width * 0.5), (int)(dim.height * 0.5 - f.getSize().height * 0.5));
		f.setVisible(true);
		f.setTitle("UW Sim Results Generator");
		f.getContentPane().setBackground(Color.WHITE);
		f.getContentPane().setLayout(new BorderLayout());
		
		final ResultsModel model = new ResultsModel();
		HomeView view = new HomeView(model);
		ProcessingView view2 = new ProcessingView(model);
		
		f.getContentPane().add(view, BorderLayout.CENTER);
		f.getContentPane().add(view2, BorderLayout.SOUTH);
		f.getContentPane().validate();
		
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				System.out.println("Shutting Down");
				model.shutdown();
			}
		});
		
	}

}
