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
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
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
public class JlawyerMcpServerCli {

    /**
     * test: ollmcp installieren MCP-server hinzufügen / nutzen: ollmcp --mcp-server-url http://localhost:45450/sse
     *
     *
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            
            String baseUrl=args[0];
            String user=args[1];
            String password=args[2];
            
            // Transport über stdin/stdout statt HTTP/SSE
            StdioServerTransportProvider transportProvider = new StdioServerTransportProvider(new ObjectMapper());

            McpTools mcpTools = new McpTools(
                new McpService(baseUrl, user, password)
            );

            McpSyncServer syncServer = McpServer.sync(transportProvider)
                .serverInfo("j-lawyer.org MCP Server", "1.0")
                .capabilities(ServerCapabilities.builder()
                        .resources(false, true)
                        .tools(true)
                        .prompts(true)
                        .logging()
                        .completions()
                        .build())
                .tools(
                    mcpTools.allCases(),
                    mcpTools.allActiveCases(),
                    mcpTools.getCase(),
                    mcpTools.searchCases(),
                    mcpTools.getCaseDocuments(),
                    mcpTools.getDocumentContent()
                )
                .build();

            
            //syncServer.run(); // blockiert und bedient stdin/stdout
           

        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

}
