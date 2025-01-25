package trabajomonstrual;

import org.mule.api.MuleEventContext;
import org.mule.api.lifecycle.Callable;
import org.mule.api.MuleMessage;
import org.mule.api.transport.PropertyScope;
import org.mule.transport.http.HttpResponse;
import org.mule.transport.http.HttpRequest;
import org.mule.module.http.internal.ParameterMap;
import org.mule.module.client.MuleClient;
import org.mule.DefaultMuleMessage;
import org.mule.transport.ajax.embedded.AjaxConnector;

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
import java.util.HashMap;
import java.util.Map;

public class consultarPokemons implements Callable {
    private static final Logger logger = Logger.getLogger(consultarPokemons.class);

    @Override
    public Object onCall(MuleEventContext eventContext) throws Exception {
        MuleMessage message = eventContext.getMessage();
        String payloadStr = message.getPayloadAsString();
        
        // Get luchar parameter from query string, default to true if not present
        Object queryParams = message.getInboundProperty("http.query.params");
        boolean shouldBattle = false; // default value
        
        if (queryParams != null) {
            if (queryParams instanceof ParameterMap) {
                ParameterMap paramMap = (ParameterMap) queryParams;
                String lucharParam = paramMap.get("luchar");
                if (lucharParam != null) {
                    shouldBattle = Boolean.parseBoolean(lucharParam);
                }
            }
        }
        
        JSONObject payload = new JSONObject(payloadStr);
        
        if (!shouldBattle) {
            // Existing pokemon data fetch logic
            return handlePokemonDataFetch(payload);
        } else {
            // Battle logic
            String pokemon1Name = payload.getString("pokemon1");
            String pokemon2Name = payload.getString("pokemon2");
            
            JSONObject pokemon1Data = getPokemonData(pokemon1Name);
            JSONObject pokemon2Data = getPokemonData(pokemon2Name);
            
            // Get random moves
            Random random = new Random();
            int move1Index = random.nextInt(4);
            int move2Index = random.nextInt(4);
            
            JSONArray moves1 = pokemon1Data.getJSONArray("moves");
            JSONArray moves2 = pokemon2Data.getJSONArray("moves");
            
            JSONObject move1 = moves1.getJSONObject(move1Index);
            JSONObject move2 = moves2.getJSONObject(move2Index);
            
            // Calculate battle values
            int valor1 = (move1.optInt("power", 0)) * (move1.optInt("accuracy", 1));
            int valor2 = (move2.optInt("power", 0)) * (move2.optInt("accuracy", 1));
            
            // Determine winner
            String winner;
            if (valor1 == valor2) {
                winner = "empate";
            } else {
                winner = valor1 > valor2 ? pokemon1Name + " (1)" : pokemon2Name + " (2)";
            }
            
            // Create battle result
            JSONObject battleResult = new JSONObject();
            battleResult.put("pokemon1", pokemon1Name + " (1)");
            battleResult.put("pokemon2", pokemon2Name + " (2)");
            battleResult.put("movimientoP1", move1.getString("name"));
            battleResult.put("movimientoP2", move2.getString("name"));
            battleResult.put("danyoMovimientoPokemon1", valor1);
            battleResult.put("danyoMovimientoPokemon2", valor2);
            battleResult.put("ganador", winner);
            battleResult.put("selectedMoveIndex1", move1Index);
            battleResult.put("selectedMoveIndex2", move2Index);
            
            // Create battle data for recording
            JSONObject battleData = new JSONObject();
            battleData.put("pokemon1", pokemon1Name + " (1)");
            battleData.put("pokemon2", pokemon2Name + " (2)");
            battleData.put("movimientoP1", move1.getString("name"));
            battleData.put("movimientoP2", move2.getString("name"));
            battleData.put("danyoMovimientoPokemon1", valor1);
            battleData.put("danyoMovimientoPokemon2", valor2);
            battleData.put("ganador", winner);
            
            // Return the battle result
            return battleResult.toString();
        }
    }

    private String handlePokemonDataFetch(JSONObject payload) throws Exception {
        JSONObject response = new JSONObject();
        
        String pokemon1 = payload.getString("pokemon1");
        String pokemon2 = payload.getString("pokemon2");
        
        try {
            JSONObject pokemon1Data = getPokemonData(pokemon1);
            response.put("pokemon1", pokemon1Data);
        } catch (IOException e) {
            response.put("error_pokemon1", "Pokemon not found: " + pokemon1);
        }
        
        try {
            JSONObject pokemon2Data = getPokemonData(pokemon2);
            response.put("pokemon2", pokemon2Data);
        } catch (IOException e) {
            response.put("error_pokemon2", "Pokemon not found: " + pokemon2);
        }
        
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
