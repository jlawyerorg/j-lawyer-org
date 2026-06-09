package org.jmarkdownviewer.jmdviewer;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.swing.JEditorPane;
import javax.swing.text.Document;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import org.commonmark.node.Node;
import org.jmarkdownviewer.jmdviewer.parser.MarkdownParser;

public class HtmlPane extends JEditorPane {

	Node document;
	File file;
	
	public HtmlPane() {
		setEditable(false);
		createPane();
	}

	private void createPane() {
        HTMLEditorKit kit = new HTMLEditorKit();

        // Use a private StyleSheet for this pane to avoid leaking CSS rules
        // into the shared/default HTMLEditorKit stylesheet used elsewhere.
        StyleSheet base = kit.getStyleSheet();
        StyleSheet mdSheet = new StyleSheet();
        mdSheet.addStyleSheet(base);
        mdSheet.importStyleSheet(getClass().getResource("/org/jmarkdownviewer/jmdviewer/resources/github-reduced.css"));
        kit.setStyleSheet(mdSheet);

        setEditorKit(kit);

		//String imgsrc = HtmlPane.class.getResource("/org/jmarkdownviewer/jmdviewer/resources/markdown.png").toString();
		// create some simple html as a string
//		String htmlString = "<html>\n" + "<body>\n" + "<h1>"
//				+ "<img src=\"" + imgsrc + "\">" 
//				+ "&nbsp; Markdown Viewer</h1>\n" 
//				+ "<h2>Select a file</h2>\n"
//				+ "<p>This is some sample text</p>\n"				
//				+ "</body>\n</html>";
		
		// create a document, set it on the JEditorPane, then add the html
		Document doc = kit.createDefaultDocument();
		setDocument(doc);
//		setText(htmlString);

	}

        
        public void setMarkdownText(String mdText) {
            MarkdownParser parser = new MarkdownParser();
            
            parser.parse(mdText);
            document = parser.getDocument();
            //parser.updatefileimages(parent);
            String html = parser.getHTML();
            html=html.replace("�", "");
            if (html != null) {
//                try {
//                    URL url = new URL("file", "", parent);
//                    //System.out.println(url.toString());
//                    HTMLDocument doc = (HTMLDocument) getDocument();
//                    doc.setBase(url);
//                    setDocument(doc);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
                setText(html);
                setCaretPosition(0);
            }
        }
	
//	public void load(File file) {
//		MarkdownParser parser = new MarkdownParser();
//		String parent;
//		try {
//			file = file.getCanonicalFile();
//			parent = file.getParentFile().getCanonicalPath();
//		} catch (IOException e1) {
//			parent = App.getInstance().getLastdir();
//		}
//		parser.parse(file);
//		document = parser.getDocument();
//		this.file = file;
//		parser.updatefileimages(parent);
//		String html = parser.getHTML();
//		if(html != null) {				
//			try {
//				URL url = new URL("file","",parent);
//				//System.out.println(url.toString());
//				HTMLDocument doc = (HTMLDocument) getDocument();					
//				doc.setBase(url);
//				setDocument(doc);					
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//			setText(html);
//			setCaretPosition(0);
//		}
//	}
	
//	public void reload() {
//		if(this.file != null) {
//			load(this.file);
//		}
//	}
	
	public void HTMLLocalImages(String surl, Image image) {
		try {
			Dictionary cache = (Dictionary) getDocument().getProperty("imageCache");
			if (cache == null) {
				cache = new Hashtable();
				getDocument().putProperty("imageCache", cache);
			}

			URL url = new URL(surl);
			cache.put(url, image);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

	}

	public Image createImage() {
		BufferedImage img = new BufferedImage(100, 50, BufferedImage.TYPE_INT_ARGB);
		Graphics g = img.getGraphics();
		((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(Color.BLUE);
		g.fillRect(0, 0, 100, 50);

		g.setColor(Color.YELLOW);
		g.fillOval(5, 5, 90, 40);
		img.flush();

		return img;
	}

	public String getasText() {
		MarkdownParser parser = new MarkdownParser(document);
		return parser.getText();
	}
		
	public String getMD() {
		StringBuilder sb = new StringBuilder(1000);
		if(file == null) 
			return "";
	
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String line;
			while((line = reader.readLine()) != null ) {
				sb.append(line);
				sb.append('\n');
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return sb.toString();
	}

	/*
	public String getMD() {
		MarkdownParser parser = new MarkdownParser(document);
		return parser.getMD();		
	}
	*/
	
	public String getCSS() {
		StringBuilder sb = new StringBuilder(1024);		
		try {
			String line;
			InputStream in = HtmlPane.class.getResourceAsStream("github.css");
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));

			while((line = reader.readLine()) != null) {
				sb.append(line);
				sb.append(System.lineSeparator());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return sb.toString();
	}

	public Node getMDocument() {
		return document;
	}

	public void setMDocument(Node document) {
		this.document = document;
	}

	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}

	
}
