package org.sitemesh.webapp.remote;

import org.eclipse.jetty.client.HttpClient;
import org.sitemesh.DecoratorSelector;
import org.sitemesh.content.ContentProcessor;
import org.sitemesh.webapp.SiteMeshFilter;
import org.sitemesh.webapp.WebAppContext;
import org.sitemesh.webapp.contentfilter.ResponseMetaData;
import org.sitemesh.webapp.contentfilter.Selector;

import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Renaud Bruyeron
 */
public class RemoteSiteMeshFilter extends SiteMeshFilter {

    protected final HttpClient client;

    public RemoteSiteMeshFilter(Selector selector, ContentProcessor contentProcessor, DecoratorSelector<WebAppContext> decoratorSelector, HttpClient client) {
        super(selector, contentProcessor, decoratorSelector);
        this.client = client;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        super.init(filterConfig);
        try {
            client.start();
        } catch(Exception e){
            throw new ServletException("failed to start httpclient", e);
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        try {
            client.stop();
        } catch(Exception e){
            throw new RuntimeException("failed to stop httpclient", e);
        }
    }

    /**
     * Create a context for the current request. This method can be overriden to allow for other
     * types of context.
     */
    @Override
    protected WebAppContext createContext(String contentType, HttpServletRequest request, HttpServletResponse response, ResponseMetaData metaData) {
        return new RemoteWebAppContext(contentType, request, response,
                getFilterConfig().getServletContext(), contentProcessor, metaData, client);
    }
}
