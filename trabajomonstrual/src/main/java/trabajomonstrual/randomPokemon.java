package trabajomonstrual;

import org.mule.api.MuleEventContext;
import org.mule.api.lifecycle.Callable;
import org.json.JSONObject;
import org.json.JSONArray;
import java.util.Random;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.apache.log4j.Logger;

public class randomPokemon implements Callable {
    private static final Logger logger = Logger.getLogger(randomPokemon.class);
    private Random random = new Random();

    @Override
    public Object onCall(MuleEventContext eventContext) throws Exception {
        // Make direct API call to get Pokemon list
        String apiUrl = "https://pokeapi.co/api/v2/pokemon?limit=1302";
        // logger.info("Calling PokeAPI for Pokemon list: " + apiUrl);
        
        URL url = new URL(apiUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");
        connection.setRequestProperty("Accept", "application/json");
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder responseBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            responseBuilder.append(line);
        }
        reader.close();

        // Parse response and get random Pokemon
        JSONObject pokemonList = new JSONObject(responseBuilder.toString());
        JSONArray results = pokemonList.getJSONArray("results");
        int randomIndex = random.nextInt(results.length());
        JSONObject randomPokemon = results.getJSONObject(randomIndex);
        
        // Create response with just the name
        JSONObject result = new JSONObject();
        result.put("name", randomPokemon.getString("name"));
        
        // logger.info("Selected random Pokemon: " + result.toString());
        return result.toString();
    }
}
