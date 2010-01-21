package org.sitemesh.webapp.remote;

import org.eclipse.jetty.client.HttpClient;
import org.sitemesh.content.Content;
import org.sitemesh.content.ContentProcessor;
import org.sitemesh.webapp.WebAppContext;
import org.sitemesh.webapp.contentfilter.BasicSelector;
import org.sitemesh.webapp.contentfilter.HttpServletRequestFilterable;
import org.sitemesh.webapp.contentfilter.HttpServletResponseBuffer;
import org.sitemesh.webapp.contentfilter.ResponseMetaData;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;
import java.nio.CharBuffer;

/**
 * @author Renaud Bruyeron
 * @version $Id$
 */
public class RemoteWebAppContext extends WebAppContext {

    protected final HttpClient client;

    public RemoteWebAppContext(String contentType, HttpServletRequest request, HttpServletResponse response, ServletContext servletContext, ContentProcessor contentProcessor, ResponseMetaData metaData, HttpClient client) {
        super(contentType, request, response, servletContext, contentProcessor, metaData);
        this.client = client;
    }

    /**
     * Dispatches to another path to render a decorator.
     *
     * <p>This path may anything that handles a standard request (e.g. Servlet,
     * JSP, MVC framework, etc).</p>
     *
     * <p>The end point can access the {@link org.sitemesh.content.ContentProperty} and {@link org.sitemesh.SiteMeshContext} by using
     * looking them up as {@link HttpServletRequest} attributes under the keys
     * {@link #CONTENT_KEY} and
     * {@link #CONTEXT_KEY} respectively.</p>
     */
    @Override
    protected void decorate(String decoratorPath, Content content, Writer out) throws IOException {
        HttpServletRequest filterableRequest = new HttpServletRequestFilterable(getRequest());
        // Wrap response so output gets buffered.
        HttpServletResponseBuffer responseBuffer = new HttpServletResponseBuffer(getResponse(), getMetaData(), new BasicSelector() {
            @Override
            public boolean shouldBufferForContentType(String contentType, String mimeType, String encoding) {
                return true; // We know we should buffer.
            }
        });
        responseBuffer.setContentType(getResponse().getContentType()); // Trigger buffering.

        // It's possible that this is reentrant, so we need to take a copy
        // of additional request attributes so we can restore them afterwards.
        Object oldContent = getRequest().getAttribute(CONTENT_KEY);
        Object oldContext = getRequest().getAttribute(CONTEXT_KEY);

        getRequest().setAttribute(CONTENT_KEY, content);
        getRequest().setAttribute(CONTEXT_KEY, this);

        try {
            // Main dispatch.
            dispatch(filterableRequest, responseBuffer, decoratorPath);

            // Write out the buffered output.
            CharBuffer buffer = responseBuffer.getBuffer();
            out.append(buffer);
        } catch (ServletException e) {
            //noinspection ThrowableInstanceNeverThrown
            throw (IOException) new IOException("Could not dispatch to decorator").initCause(e);
        } finally {
            // Restore previous state.
            getRequest().setAttribute(CONTENT_KEY, oldContent);
            getRequest().setAttribute(CONTEXT_KEY, oldContext);
        }
    }
    
}
