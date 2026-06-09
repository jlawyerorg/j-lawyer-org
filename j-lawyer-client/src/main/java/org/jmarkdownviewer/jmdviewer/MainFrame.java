package org.jmarkdownviewer.jmdviewer;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.print.PrinterException;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.html.HTMLDocument;

import org.jmarkdownviewer.jmdviewer.parser.MarkdownParser;


public class MainFrame extends JFrame implements ActionListener, HyperlinkListener {
	
	HtmlPane htmlpane;
	JLabel mlMsg;

	public MainFrame() {
		super();
		setTitle("jmarkdown viewer");
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		createGui();
	}
	
	private void createGui() {
		setPreferredSize(new Dimension(1280, 960));		
		
		JMenuBar menubar = new JMenuBar();
		JMenu mFile = new JMenu("File");
		mFile.setMnemonic(KeyEvent.VK_F);
		menubar.add(mFile);
		mFile.add(addmenuitem("Open", "OPEN", KeyEvent.VK_O));
		mFile.add(addmenuitem("Reload", "RELOAD", KeyEvent.VK_R));
		mFile.add(addmenuitem("Export", "EXPORT", KeyEvent.VK_E));
		mFile.add(addmenuitem("Print", "PRINT", KeyEvent.VK_P));
		mFile.add(addmenuitem("About", "ABOUT", KeyEvent.VK_A));
		mFile.add(addmenuitem("Exit", "EXIT", KeyEvent.VK_X));

		JMenu mView = new JMenu("View");
		mView.setMnemonic(KeyEvent.VK_F);
		menubar.add(mView);
		mView.add(addmenuitem("Markdown", "MD", KeyEvent.VK_M));
				
		
		setJMenuBar(menubar);
		getContentPane().setLayout(new BorderLayout());
		
		JToolBar toolbar = new JToolBar();
		toolbar.add(makeNavigationButton("Open24.gif", "OPEN", "Open", "Open"));
		toolbar.add(makeNavigationButton("Refresh24.gif", "RELOAD", "Reload", "Reload"));
		add(toolbar, BorderLayout.NORTH);
		
		htmlpane = new HtmlPane();
		JScrollPane scrollpane = new JScrollPane(htmlpane);
		
		htmlpane.addHyperlinkListener(this);
				
		getContentPane().add(scrollpane,BorderLayout.CENTER);
		
		//mlMsg = new JLabel();
		//getContentPane().add(mlMsg, BorderLayout.SOUTH);
		
		//System.out.println(htmlpane.getCSS());
	}

	
	protected JButton makeNavigationButton(String imageName, String actionCommand,
			String toolTipText, String altText) {

		//Create and initialize the button.
		JButton button = new JButton();
		button.setActionCommand(actionCommand);
		button.setToolTipText(toolTipText);
		button.addActionListener(this);

		setIcon(button, imageName, altText);

		return button;
	}
	
	protected void setIcon(JButton button, String imageName, String altText) {
		//Look for the image.
		String imgLocation = "icons/" + imageName;
		URL imageURL = HtmlPane.class.getResource(imgLocation);

		if (imageURL != null) { // image found
			button.setIcon(new ImageIcon(imageURL, altText));
		} else { // no image found
			button.setText(altText);
			System.err.println("Resource not found: " + imgLocation);
		}		
	}

	
	protected JMenuItem addmenuitem(String label, String cmd, int keyevent) {
		JMenuItem item = new JMenuItem(label);
		item.setMnemonic(keyevent);
		item.setActionCommand(cmd);
		item.addActionListener(this);
		return item;		
	}
	

//	private void doopen() {
//		JFileChooser chooser = new JFileChooser(App.getInstance().getLastdir());
//		int ret = chooser.showOpenDialog(this);
//		if (ret == JFileChooser.APPROVE_OPTION) {			
//			htmlpane.load(chooser.getSelectedFile());
//			App.getInstance().setLastdir(chooser.getSelectedFile().getParent());
//		}
//	}
	
//	public void openfile(File file) {
//		htmlpane.load(file);
//	}
	
	String selextension;
	
//	private void doexport() {		 
//		JFileChooser chooser = new JFileChooser(App.getInstance().getLastdir());
//		FileFilter filter = new FileNameExtensionFilter("HTML file", "htm", "html");
//		chooser.addChoosableFileFilter(filter);
//		filter = new FileNameExtensionFilter("Plain text file", "txt");
//		chooser.addChoosableFileFilter(filter);		
//		chooser.addPropertyChangeListener(JFileChooser.FILE_FILTER_CHANGED_PROPERTY, 
//				new PropertyChangeListener()
//		{
//		  public void propertyChange(PropertyChangeEvent evt)
//		  {
//		    FileFilter filter = (FileFilter)evt.getNewValue();
//
//		    selextension = ((FileNameExtensionFilter) filter).getExtensions()[0]; 		    
//
//		  }
//		});
//		
//		
//		int ret = chooser.showSaveDialog(this);
//		if (ret == JFileChooser.APPROVE_OPTION) {			
//			File file = chooser.getSelectedFile();
//			if(file.getName().indexOf(".") == -1 && selextension == null) {
//				String msg = "Select an appropriate extension,"
//						+ " Only HTML or TXT is supported for now";
//				JOptionPane.showMessageDialog(this, msg, "Invalid file type",
//						JOptionPane.WARNING_MESSAGE);
//				return;
//			}
//			if(file.getName().indexOf(".") == -1 && selextension != "") {
//				file = new File(file.getAbsolutePath()
//						.concat(".").concat(selextension));
//			}
//			if (file.exists()) {
//				String msg = "File " + file.getName() + " exists ! Overwrite?";
//				ret = JOptionPane.showConfirmDialog(this, msg, "Overwrite warning", 
//						 JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
//				if(ret != JOptionPane.OK_OPTION)
//					return;
//			}
//			if (file.getName().toLowerCase().endsWith("htm") ||
//				file.getName().toLowerCase().endsWith("html")) {
//				doExpHTML(file);
//			} else if (file.getName().toLowerCase().endsWith("txt")) {
//				doExpText(file);				
//			}
//				
//		}
//		
//	}

	private void doExpHTML(File file) {
		try {
			MarkdownParser parser = new MarkdownParser();
			File mdfile = htmlpane.getFile();
			if(mdfile == null) return;
			
			parser.parse(mdfile);
			String html = parser.getHTML();
			if (html != null && html != "") {
				BufferedWriter writer = new BufferedWriter(new FileWriter(file));
				writer.write("<html>\n<body>\n");				
				writer.write(html);
				writer.write("</body>\n</html>\n");
				writer.flush();
				writer.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void doExpText(File file) {
		String text = htmlpane.getasText();
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(file));
			writer.write(text);
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
		
	private void doviewmd() {
		JTextArea ta = new JTextArea(40, 80);
		ta.setText(htmlpane.getMD());		
		JScrollPane pane = new JScrollPane(ta);
		JOptionPane.showMessageDialog(this, pane);
	}

	private void doprint() {
		try {
			boolean done = htmlpane.print();
            if (done) {
                JOptionPane.showMessageDialog(this, "Printing is done");
            } else {
                JOptionPane.showMessageDialog(this, "Error while printing");
            }
		} catch (PrinterException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(),
            		"Error while printing", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }		
	}

	private void doabout() {		
//		StringBuilder sb = new StringBuilder(1024);
//		try {
//			BufferedReader reader = new BufferedReader(
//					new InputStreamReader(
//						HtmlPane.class.getResourceAsStream("About.md")));
//			String l;
//			while((l = reader.readLine()) != null) {
//				sb.append(l);
//				sb.append("\n");
//			}
//			reader.close();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		MarkdownParser parser = new MarkdownParser();
//		parser.parse(sb.toString());
//		parser.updatejarimages(App.class);
//		String html = parser.getHTML();
//		if(html != null && html != "") {
//			htmlpane.setText(html);
//			htmlpane.setCaretPosition(0);
//		}
	}

	
	@Override
	public void hyperlinkUpdate(HyperlinkEvent e) {
        if(e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
        	if(Desktop.isDesktopSupported()) {
        	    try {
					Desktop.getDesktop().browse(e.getURL().toURI());
				} catch (IOException e1) {
					e1.printStackTrace();
				} catch (URISyntaxException e1) {
					e1.printStackTrace();
				}
        	}
         }		
	}

	@Override
	public void actionPerformed(ActionEvent e) {
//		String cmd = e.getActionCommand(); 
//		if(cmd.equals("OPEN")) {
//			doopen();
//		} else if(cmd.equals("RELOAD")) {
//			htmlpane.reload();
//		} else if(cmd.equals("ABOUT")) {			
//			doabout();
//		} else if(cmd.equals("EXPORT")) {
//			doexport();
//		} else if(cmd.equals("MD")) {
//			doviewmd();
//		} else if(cmd.equals("PRINT")) {
//			doprint();
//		} else if (cmd.equals("EXIT")) {
//			dispose();
//			System.exit(0);
//		}		
	}


	private static final long serialVersionUID = 3354572268511291816L;

}
