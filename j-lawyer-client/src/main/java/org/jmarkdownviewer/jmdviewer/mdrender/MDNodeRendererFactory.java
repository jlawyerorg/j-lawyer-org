package org.jmarkdownviewer.jmdviewer.mdrender;

import org.commonmark.renderer.NodeRenderer;

public interface MDNodeRendererFactory {

    /**
     * Create a new node renderer for the specified rendering context.
     *
     * @param context the context for rendering (normally passed on to the node renderer)
     * @return a node renderer
     */
    NodeRenderer create(MDNodeRendererContext context);

}
