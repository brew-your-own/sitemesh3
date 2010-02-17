package org.sitemesh.webapp.remote;

import org.eclipse.jetty.client.HttpClient;
import org.sitemesh.builder.BaseSiteMeshFilterBuilder;

import javax.servlet.Filter;

/**
 * @author Renaud Bruyeron
 */
public class RemoteSiteMeshFilterBuilder extends BaseSiteMeshFilterBuilder<RemoteSiteMeshFilterBuilder> {

    private HttpClient client = new HttpClient();

    /**
     * Create the SiteMesh Filter.
     */
    @Override
    public Filter create() {
        return new RemoteSiteMeshFilter(getSelector(),
                getContentProcessor(),
                getDecoratorSelector(), client);
    }

    /**
     * Set timeout parameter for remote decorators.
     * 
     * @param timeout
     * @return
     */
    public RemoteSiteMeshFilterBuilder setHttpClientTimeout(long timeout){
        this.client.setTimeout(timeout);
        return this;
    }
}
