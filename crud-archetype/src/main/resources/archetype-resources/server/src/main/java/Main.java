package ${package}.server;

import java.io.IOException;
import java.net.URI;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

/** Starts REST server based on Jersey.
 */
final class Main implements ContainerResponseFilter {
    public static void main(String... args) throws Exception {
        ResourceConfig rc = new ResourceConfig(
            ContactsResource.class, Main.class
        );
        URI u = new URI("http://localhost:8080/");
        HttpServer server = GrizzlyHttpServerFactory.createHttpServer(u, rc);
        System.err.println("Press Enter to shutdown the server");
        System.in.read();
        server.stop();
    }

    @Override
    public void filter(
        ContainerRequestContext requestContext, 
        ContainerResponseContext r
    ) throws IOException {
        r.getHeaders().add("Access-Control-Allow-Origin", "*");
        r.getHeaders().add("Access-Control-Allow-Credentials", "true");
        r.getHeaders().add("Access-Control-Allow-Headers", "Content-Type");
        r.getHeaders().add("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT");
    }    
}
