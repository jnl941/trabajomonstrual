package trabajomonstrual;

import org.mule.api.MuleEventContext;
import org.mule.api.lifecycle.Callable;
import org.mule.api.MuleMessage;
import org.mule.api.transport.PropertyScope;
import org.mule.transport.http.HttpResponse;
import org.mule.transport.http.HttpRequest;

import org.json.JSONObject;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class consultarPokemons implements Callable {

    @Override
    public Object onCall(MuleEventContext eventContext) throws Exception {
        // PokeAPI endpoint to fetch Pokemons data
        String apiUrl = "https://pokeapi.co/api/v2/pokemon/";

        // Initialize HTTP client to make the request to the PokeAPI
        URL url = new URL(apiUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        // Read the response from the API
        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        // Parse the JSON response
        JSONObject jsonResponse = new JSONObject(response.toString());

        // Optionally, handle specific data such as the "results" array of Pokémon
        return jsonResponse.getJSONArray("results").toString();
    }
}
