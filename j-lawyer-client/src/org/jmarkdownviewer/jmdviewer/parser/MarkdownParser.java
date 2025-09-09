package org.jmarkdownviewer.jmdviewer.parser;

import java.io.BufferedReader;

import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.List;

import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.Image;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.commonmark.renderer.text.TextContentRenderer;
import org.jmarkdownviewer.jmdviewer.mdrender.MDRenderer;


public class MarkdownParser {

	Node document;
	
	public MarkdownParser() {		
	}
	
	public MarkdownParser(Node document) {
		this.document = document;
	}
	
	public void parse(File file) {
		StringBuilder sb = new StringBuilder(4096);
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			while(true) {
				String l = reader.readLine();
				if(l!= null) {
					sb.append(l);
					sb.append('\n');
				} else
					break;				
			}			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		parse(sb.toString());
	}
	
	public void parse(String mdtext) {
		List<Extension> extensions = Arrays.asList(TablesExtension.create());
		Parser parser = Parser.builder()
				.extensions(extensions)
				.build();				
		document = parser.parse(mdtext);				
	}
	
	public void updatefileimages(String parent) {
		FileImageVisitor iv = new FileImageVisitor(parent);
		document.accept(iv);
	}
	
	public void updatejarimages(Class appclass) {
		JarImageVisitor iv = new JarImageVisitor(appclass);
		document.accept(iv);
	}
	
	public String getHTML() {
		if(document == null)
			return null;
        // Transform soft line breaks into hard breaks to ensure preview keeps line breaks
        SoftToHardBreakVisitor brVisitor = new SoftToHardBreakVisitor();
        document.accept(brVisitor);

        List<Extension> extensions = Arrays.asList(TablesExtension.create());
        HtmlRenderer renderer = HtmlRenderer.builder()
                .extensions(extensions)
                .build();
        String html = renderer.render(document);  
        return html;        
	}

	public String getText() {
		if(document == null)
			return "";

		List<Extension> extensions = Arrays.asList(TablesExtension.create());
		TextContentRenderer renderer = TextContentRenderer.builder()
		        .extensions(extensions)
		        .build();
		String text = renderer.render(document);  
		return text;		

	}
	
	public String getMD() {
		if(document == null)
			return "";

		List<Extension> extensions = Arrays.asList(TablesExtension.create());
		MDRenderer renderer = MDRenderer.builder()
		        .extensions(extensions)
		        .build();
		String text = renderer.render(document);  
		return text;		

	}

	
	public Node getDocument() {
		return document;
	}

	public void setDocument(Node document) {
		this.document = document;
	}
	
}
