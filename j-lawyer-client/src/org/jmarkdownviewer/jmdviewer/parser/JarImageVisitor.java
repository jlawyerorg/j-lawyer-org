package org.jmarkdownviewer.jmdviewer.parser;

import java.io.File;
import java.net.MalformedURLException;

import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.Image;

public class JarImageVisitor extends AbstractVisitor {

	Class appclass;
	
	public JarImageVisitor(Class appclass) {
		this.appclass = appclass;
	}
	
	@Override
	public void visit(Image image) {
		if(appclass.getResource(image.getDestination()) == null) return;
		
		String url = appclass.getResource(image.getDestination()).toString();
		image.setDestination(url);
		visitChildren(image);
	}

}
