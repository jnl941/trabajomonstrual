package trabajomonstrual;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;

import org.mule.api.MuleEventContext;
import org.mule.api.lifecycle.Callable;

public class consultarLogs implements Callable {

    @Override
    public Object onCall(MuleEventContext eventContext) throws Exception {
        // Get the request path
        String requestPath = eventContext.getMessage().getInboundProperty("http.request.path").toString();
        System.out.println(requestPath);

        // Define the base folder for the logs
        String basePath = "logs/";
        String filePath = null;

        // Determine which file to read based on the request path
        switch (requestPath) {
            case "/logsGanadores":
                filePath = basePath + "logsGanadores.xml";
                break;
            case "/logsPerdedores":
                filePath = basePath + "logsPerdedores.xml";
                break;
            case "/logsEmpates":
                filePath = basePath + "logsEmpates.xml";
                break;
            default:
                throw new IllegalArgumentException("Invalid request path: " + requestPath);
        }

        // Read the file content and return it
        if (filePath != null) {
            try {
                String content = new String(Files.readAllBytes(Paths.get(filePath)), StandardCharsets.UTF_8);
                return content;
            } catch (Exception e) {
                throw new RuntimeException("Error reading the file: " + filePath, e);
            }
        }
        
        return null;
    }
}
