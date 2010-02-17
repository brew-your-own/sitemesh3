package org.sitemesh.webapp.remote;

import junit.framework.TestCase;
import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpExchange;
import org.eclipse.jetty.http.HttpFields;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.HandlerCollection;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.FilterHolder;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.log.Log;
import org.mortbay.resource.FileResource;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URL;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * @author Renaud Bruyeron
 * @version $Id$
 */
public class RemoteTest extends TestCase {

    Server server;
    HandlerCollection hc;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Log.setLog(null);
        server = new Server(8080);
        hc = new HandlerCollection();
        server.setHandler(hc);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        server.stop();
        server = null;
        hc = null;
    }

    public void testCachingRemoteDecorator() throws Exception {

        Context staticContext = new org.mortbay.jetty.webapp.WebAppContext();
        staticContext.setContextPath("/static");
        staticContext.setBaseResource(new FileResource(new URL("file://ignoreTHIS/")));
        staticContext.addServlet(new ServletHolder(new LastModifiedContentServlet("Decorated: <sitemesh:write property='title'/>", "text/html", getLastModifiedMillis(1990))), "/my-decorator");
        hc.addHandler(staticContext);

        Context context = new org.mortbay.jetty.webapp.WebAppContext();
        context.setBaseResource(new FileResource(new URL("file://ignoreTHIS/")));
        context.addServlet(new ServletHolder(new LastModifiedContentServlet("<title>Hello world</title>", "text/html", getLastModifiedMillis(1980))), "/content");

        context.addFilter(new FilterHolder(new RemoteSiteMeshFilterBuilder()
                .addDecoratorPath("/*", "http://localhost:8080/static/my-decorator")
                .create()), "/*", Handler.DEFAULT);
        hc.addHandler(context);

        server.start();

        HttpClient client = new HttpClient();
        client.setTimeout(3000);
        client.start();

        ContentExchange exchange = new ContentExchange(true);
        exchange.setURL("http://localhost:8080/content");
        exchange.setRequestHeader("If-Modified-Since", HttpFields.formatDate(getLastModifiedMillis(1991)));

        client.send(exchange);

        // Waits until the exchange is terminated
        int exchangeState = exchange.waitForDone();
        if (exchangeState == HttpExchange.STATUS_COMPLETED) {
            // response from the server should say 304: not modified!
            assertEquals(304, exchange.getResponseStatus());
        } else {
            fail("invalid exchange code: " + exchangeState);
        }

        // let's do this again, but ask for a if-modified-since where the content is older but the decorator is newer
        // SM3 should return a 200 with a Last-Modified equal to the decorator's
        exchange = new ContentExchange(true);
        exchange.setURL("http://localhost:8080/content");
        exchange.setRequestHeader("If-Modified-Since", HttpFields.formatDate(getLastModifiedMillis(1985)));

        client.send(exchange);

        // Waits until the exchange is terminated
        exchangeState = exchange.waitForDone();
        if (exchangeState == HttpExchange.STATUS_COMPLETED) {
            // response from the server should say 200: modified!
            // the content has not changed, but the decorator has...
            assertEquals(200, exchange.getResponseStatus());
            assertEquals("Decorated: Hello world", exchange.getResponseContent());
        } else {
            fail("invalid exchange code: " + exchangeState);
        }

        client.stop();
    }

    public void testRemoteDecorator() throws Exception {

        Context staticContext = new org.mortbay.jetty.webapp.WebAppContext();
        staticContext.setContextPath("/static");
        staticContext.setBaseResource(new FileResource(new URL("file://ignoreTHIS/")));
        staticContext.addServlet(new ServletHolder(new LastModifiedContentServlet("Decorated: <sitemesh:write property='title'/>", "text/html")), "/my-decorator");
        hc.addHandler(staticContext);

        Context context = new org.mortbay.jetty.webapp.WebAppContext();
        context.setBaseResource(new FileResource(new URL("file://ignoreTHIS/")));
        context.addServlet(new ServletHolder(new LastModifiedContentServlet("<title>Hello world</title>", "text/html")), "/content");

        context.addFilter(new FilterHolder(new RemoteSiteMeshFilterBuilder()
                .addDecoratorPath("/*", "http://localhost:8080/static/my-decorator")
                .create()), "/*", Handler.DEFAULT);
        hc.addHandler(context);

        server.start();

        HttpClient client = new HttpClient();
        client.setTimeout(3000);
        client.start();

        ContentExchange exchange = new ContentExchange(true);
        exchange.setURL("http://localhost:8080/content");

        client.send(exchange);

        // Waits until the exchange is terminated
        int exchangeState = 0;
        try {
            exchangeState = exchange.waitForDone();
            if (exchangeState == HttpExchange.STATUS_COMPLETED) {
                assertEquals("Decorated: Hello world", exchange.getResponseContent());
            } else {
                fail("invalid exchange code: " + exchangeState);
            }
        } finally {
            client.stop();
        }
    }

    private long getLastModifiedMillis(int year) {
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        c.set(Calendar.YEAR, year);
        return c.getTimeInMillis();
    }

    private static class LastModifiedContentServlet extends HttpServlet {
        private final String content;
        private final String contentType;
        private final long lastModified;

        private LastModifiedContentServlet(String content, String contentType, long lastModified) {
            this.content = content;
            this.contentType = contentType;
            this.lastModified = lastModified;
        }


        private LastModifiedContentServlet(String content, String contentType) {
            this(content, contentType, -1);
        }

        @Override
        protected long getLastModified(HttpServletRequest req) {
            return lastModified;
        }

        /**
         * Return content passed in constructor.
         */
        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            response.setContentType(contentType);
            response.getWriter().write(content);
        }
    }
}
