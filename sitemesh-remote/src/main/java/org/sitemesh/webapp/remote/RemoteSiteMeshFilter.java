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
 * @version $Id$
 */
public class RemoteSiteMeshFilter extends SiteMeshFilter {

    protected HttpClient client = null;

    public RemoteSiteMeshFilter(Selector selector, ContentProcessor contentProcessor, DecoratorSelector<WebAppContext> decoratorSelector) {
        super(selector, contentProcessor, decoratorSelector);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        super.init(filterConfig);
        client = new HttpClient();
        client.setTimeout(3000);
        try {
            client.start();
        } catch(Exception e){
            throw new RuntimeException("failed to start httpclient", e);
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
        client = null;
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
