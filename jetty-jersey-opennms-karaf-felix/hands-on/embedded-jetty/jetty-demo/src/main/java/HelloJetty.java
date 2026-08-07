import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;

public class HelloJetty {
    public static void main(String[] args) throws Exception {
        Server server = new Server(8081);
        ServletHandler handler = new ServletHandler();
        server.setHandler(handler);

        handler.addServletWithMapping(HelloServlet.class, "/hello");

        server.start();
        server.join();
    }
}
