package org.sitemesh.webapp.remote;

import org.sitemesh.builder.BaseSiteMeshFilterBuilder;

import javax.servlet.Filter;

/**
 * @author Renaud Bruyeron
 * @version $Id$
 */
public class RemoteSiteMeshFilterBuilder extends BaseSiteMeshFilterBuilder<RemoteSiteMeshFilterBuilder> {

    /**
     * Create the SiteMesh Filter.
     */
    @Override
    public Filter create() {
        return new RemoteSiteMeshFilter(getSelector(),
                getContentProcessor(),
                getDecoratorSelector());
    }
}
