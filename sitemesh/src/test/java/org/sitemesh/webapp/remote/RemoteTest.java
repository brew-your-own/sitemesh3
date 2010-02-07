package org.sitemesh.webapp.remote;

import junit.framework.TestCase;
import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpExchange;
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

/**
 * @author Renaud Bruyeron
 * @version $Id$
 */
public class RemoteTest extends TestCase {

    public void testRemoteDecorator() throws Exception {
        Log.setLog(null);
        Server server = new Server(8080);
        HandlerCollection hc = new HandlerCollection();

        Context staticContext = new org.mortbay.jetty.webapp.WebAppContext();
        staticContext.setContextPath("/static");
        staticContext.setBaseResource(new FileResource(new URL("file://ignoreTHIS/")));
        staticContext.addServlet(new ServletHolder(new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest request, HttpServletResponse response)
                    throws ServletException, IOException {
                response.setContentType("text/html");
                response.getOutputStream().print("Decorated: <sitemesh:write property='title'/>");
            }
        }), "/my-decorator");
        hc.addHandler(staticContext);

        Context context = new org.mortbay.jetty.webapp.WebAppContext();
        context.setBaseResource(new FileResource(new URL("file://ignoreTHIS/")));
        context.addServlet(new ServletHolder(new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest request, HttpServletResponse response)
                    throws ServletException, IOException {
                response.setContentType("text/html");
                response.getOutputStream().print("<title>Hello world</title>");
            }
        }), "/content");

        context.addFilter(new FilterHolder(new RemoteSiteMeshFilterBuilder()
                .addDecoratorPath("/*", "http://localhost:8080/static/my-decorator")
                .create()), "/*", Handler.DEFAULT);
        hc.addHandler(context);

        server.setHandler(hc);
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
            server.stop();
        }
    }
}
