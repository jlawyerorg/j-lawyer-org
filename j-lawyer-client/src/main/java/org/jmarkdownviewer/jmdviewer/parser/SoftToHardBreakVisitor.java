package org.jmarkdownviewer.jmdviewer.parser;

import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.HardLineBreak;
import org.commonmark.node.Node;
import org.commonmark.node.SoftLineBreak;

/**
 * Converts SoftLineBreak nodes to HardLineBreak nodes to force <br/> rendering
 * in HTML. This makes line breaks in preview reflect the original Markdown
 * source more closely in Swing's JEditorPane.
 */
public class SoftToHardBreakVisitor extends AbstractVisitor {

    @Override
    public void visit(SoftLineBreak softBreak) {
        Node hard = new HardLineBreak();
        softBreak.insertBefore(hard);
        softBreak.unlink();
    }
}

