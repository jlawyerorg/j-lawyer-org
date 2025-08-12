/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jlawyer.mcpserver;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletSseServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

/**
 *
 * @author jens
 */
public class JlawyerMcpServer {

    /**
     * test: ollmcp installieren MCP-server hinzufügen / nutzen: ollmcp
     * --mcp-server-url http://localhost:45450/sse
     *
     *
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {

            HttpServletSseServerTransportProvider transportProvider
                    = new HttpServletSseServerTransportProvider(
                            new ObjectMapper(), "/", "/sse");

            McpTools mcpTools = new McpTools(new McpService());

            McpSyncServer syncServer = McpServer.sync(transportProvider)
                    .serverInfo("WildFly MCP", "1.0")
                    .capabilities(ServerCapabilities.builder()
                            .resources(false, true)
                            .tools(true)
                            .prompts(true)
                            .logging()
                            .completions()
                            .build())
                    .tools(
                            mcpTools.allInvoices(),
                            mcpTools.invoicesByPattern())
                    .build();

            QueuedThreadPool threadPool = new QueuedThreadPool();
            threadPool.setName("server");

            Server server = new Server(threadPool);

            ServerConnector connector = new ServerConnector(server);
            connector.setPort(45450);
            server.addConnector(connector);

            ServletContextHandler context = new ServletContextHandler();
            context.setContextPath("/");
            context.addServlet(new ServletHolder(transportProvider), "/*");

            server.setHandler(context);
            server.start();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
