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
import java.io.FileNotFoundException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;
import java.io.IOException;
import org.json.JSONArray;
import org.json.JSONException;
import org.apache.log4j.Logger;

public class consultarPokemons implements Callable {
    private static final Logger logger = Logger.getLogger(consultarPokemons.class);

    @Override
    public Object onCall(MuleEventContext eventContext) throws Exception {
        MuleMessage message = eventContext.getMessage();
        String payloadStr = message.getPayloadAsString();
        
        // logger.info("Received payload: " + payloadStr);
        
        JSONObject payload = new JSONObject(payloadStr);
        JSONObject response = new JSONObject();
        
        // Get Pokemon names from the payload
        String pokemon1 = payload.getString("pokemon1");
        String pokemon2 = payload.getString("pokemon2");
        
        // logger.info("Fetching data for Pokemon: " + pokemon1 + " and " + pokemon2);
        
        // Try to get data for both Pokemon
        try {
            JSONObject pokemon1Data = getPokemonData(pokemon1);
            response.put("pokemon1", pokemon1Data);
        } catch (IOException e) {
            // logger.error("Pokemon not found: " + pokemon1);
            response.put("error_pokemon1", "Pokemon not found: " + pokemon1);
        }
        
        try {
            JSONObject pokemon2Data = getPokemonData(pokemon2);
            response.put("pokemon2", pokemon2Data);
        } catch (IOException e) {
            // logger.error("Pokemon not found: " + pokemon2);
            response.put("error_pokemon2", "Pokemon not found: " + pokemon2);
        }
        
        // logger.info("Sending response: " + response.toString());
        return response.toString();
    }

    private JSONObject getPokemonData(String pokemonName) throws Exception {
        String apiUrl = "https://pokeapi.co/api/v2/pokemon/" + pokemonName.toLowerCase();
        // logger.info("Calling PokeAPI: " + apiUrl);
        
        URL url = new URL(apiUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        
        // Add required headers
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");
        connection.setRequestProperty("Accept", "application/json");
        
        // Check response code before reading
        int responseCode = connection.getResponseCode();
        if (responseCode == 404) {
            throw new IOException("Pokemon not found: " + pokemonName);
        }
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder responseBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            responseBuilder.append(line);
        }
        reader.close();

        JSONObject pokemonData = new JSONObject(responseBuilder.toString());
        
        JSONObject simplifiedData = new JSONObject();
        simplifiedData.put("name", pokemonData.getString("name"));
        //simplifiedData.put("base_experience", pokemonData.getInt("base_experience"));
        
        // Get HP stat
        /*JSONArray stats = pokemonData.getJSONArray("stats");
        for (int i = 0; i < stats.length(); i++) {
            JSONObject stat = stats.getJSONObject(i);
            if (stat.getJSONObject("stat").getString("name").equals("hp")) {
                simplifiedData.put("hp", stat.getInt("base_stat"));
                break;
            }
        }*/
        
        // Get animated sprites from showdown
        try{
        	JSONObject sprites = pokemonData.getJSONObject("sprites").getJSONObject("other").getJSONObject("showdown");
            simplifiedData.put("sprite_front", sprites.getString("front_default"));
            simplifiedData.put("sprite_back", sprites.getString("back_default"));
        } catch(JSONException e) {
        	JSONObject sprites = pokemonData.getJSONObject("sprites");
            simplifiedData.put("sprite_front", sprites.getString("front_default"));
            simplifiedData.put("sprite_back", sprites.getString("front_default"));
        }
        
        // Get 4 random moves with details
        JSONArray allMoves = pokemonData.getJSONArray("moves");
        JSONArray selectedMoves = new JSONArray();
        
        Random randMovesGen = new Random();
        
        // Randomly select 4 moves and fetch their details
        for (int i = 0; i < 4 && i < allMoves.length(); i++) {
            JSONObject moveRef = allMoves.getJSONObject(Math.abs(randMovesGen.nextInt())%allMoves.length()).getJSONObject("move");
            String moveUrl = moveRef.getString("url");
            
            // Fetch move details
            URL moveDetailUrl = new URL(moveUrl);
            HttpURLConnection moveConn = (HttpURLConnection) moveDetailUrl.openConnection();
            moveConn.setRequestMethod("GET");
            moveConn.setRequestProperty("User-Agent", "Mozilla/5.0");
            
            BufferedReader moveReader = new BufferedReader(new InputStreamReader(moveConn.getInputStream()));
            StringBuilder moveResponseBuilder = new StringBuilder();
            String moveLine;
            while ((moveLine = moveReader.readLine()) != null) {
                moveResponseBuilder.append(moveLine);
            }
            moveReader.close();
            
            JSONObject moveDetails = new JSONObject(moveResponseBuilder.toString());
            JSONObject move = new JSONObject();
            move.put("name", moveDetails.getString("name"));
            move.put("power", moveDetails.isNull("power") ? 0 : moveDetails.getInt("power"));
            move.put("accuracy", moveDetails.isNull("accuracy") ? 0 : moveDetails.getInt("accuracy"));
            
            selectedMoves.put(move);
        }
        // logger.info("Moves for " + pokemonName + ": " + selectedMoves.toString());
        
        simplifiedData.put("moves", selectedMoves);
        
        return simplifiedData;
    }
}
