package org.jmarkdownviewer.jmdviewer.parser;

import java.io.File;
import java.net.MalformedURLException;

import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.Image;

public class FileImageVisitor extends AbstractVisitor {

	
	String parent;
	
	public FileImageVisitor(String parent) {
		this.parent = parent;
	}
	
	@Override
	public void visit(Image image) {
		if(!image.getDestination().startsWith("http")) {
			try {
				String name = new File(parent,image.getDestination())
					.toURI().toURL().toExternalForm();
				image.setDestination(name);
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}
		visitChildren(image);
	}

}
