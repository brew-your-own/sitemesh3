package org.sitemesh.webapp.remote;

import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpExchange;
import org.sitemesh.content.Content;
import org.sitemesh.content.ContentProcessor;
import org.sitemesh.webapp.WebAppContext;
import org.sitemesh.webapp.contentfilter.ResponseMetaData;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;
import java.nio.CharBuffer;

/**
 * SiteMesh {@link org.sitemesh.SiteMeshContext} implementation that builds on {@link org.sitemesh.webapp.WebAppContext}
 * to add the ability to fetch decorators from a remote (i.e. HTTP) server.
 * Uses a Jetty Client instance.
 *
 * @todo error handling
 * @author Renaud Bruyeron
 */
public class RemoteWebAppContext extends WebAppContext {

    protected final HttpClient client;

    public RemoteWebAppContext(String contentType, HttpServletRequest request, HttpServletResponse response, ServletContext servletContext, ContentProcessor contentProcessor, ResponseMetaData metaData, HttpClient client) {
        super(contentType, request, response, servletContext, contentProcessor, metaData);
        this.client = client;
    }

    /**
     * Dispatches to another path to render a decorator.
     * <p>The path can be a URL, in which case the dispath is performed by the Jetty Client instance</p>.
     * <p>If not then the standard {@link org.sitemesh.webapp.WebAppContext#dispatch(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, String)} is invoked</p>
     *
     * @see org.sitemesh.webapp.WebAppContext
     */
    @Override
    protected void decorate(String decoratorPath, Content content, Writer out) throws IOException {
        if (decoratorPath.startsWith("/")) {
            // fallback
            super.decorate(decoratorPath, content, out);
        } else {
            metaData.beginNewResponse();
            ContentExchange exchange = new ContentExchange(true);
            exchange.setURL(decoratorPath);

            client.send(exchange);

            // Waits until the exchange is terminated
            int exchangeState = 0;
            try {
                exchangeState = exchange.waitForDone();
                if (exchangeState == HttpExchange.STATUS_COMPLETED) {
                    out.append(CharBuffer.wrap(exchange.getResponseContent()));
                    // update metaData so that caching works as expected...
                    metaData.updateLastModified(exchange.getResponseFields().getDateField("Last-Modified"));
                } else {
                    throw new IOException("problem: " + exchangeState);
                }
            } catch (InterruptedException e) {
                throw new IOException(String.format("Fetching of remote decorator %s interrupted", decoratorPath), e);
            }
        }
    }

}
