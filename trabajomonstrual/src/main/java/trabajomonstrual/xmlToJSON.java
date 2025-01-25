package trabajomonstrual;

import org.mule.api.MuleEventContext;
import org.mule.api.lifecycle.Callable;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.regex.Pattern;
import org.xml.sax.InputSource;

public class xmlToJSON implements Callable {
	
    @Override
    public Object onCall(MuleEventContext eventContext) throws Exception {
        // Get the payload (XML data) from the Mule message
        String xmlData = eventContext.getMessage().getPayloadAsString();

        // Convert the XML to JSON
        String jsonOutput = convertXmlToJson(xmlData);

        // Set the JSON as the payload
        eventContext.getMessage().setPayload(jsonOutput);
        return eventContext.getMessage();
    }

    private String convertXmlToJson(String xmlData) throws Exception {
        // Wrap multiple XML documents in a root element
        xmlData = "<root>" + xmlData.replace("<?xml version='1.0' encoding='windows-1252'?>", "") + "</root>";
        
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputSource inputSource = new InputSource(new StringReader(xmlData));
        Document document = builder.parse(inputSource);

        // Extract root element
        Element root = document.getDocumentElement();

        // Convert to JSON
        JSONArray battlesArray = new JSONArray();
        NodeList allNodes = root.getChildNodes();
        
        for (int i = 0; i < allNodes.getLength(); i++) {
            Node node = allNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                JSONObject battleJson = new JSONObject();
                
                // Handle both direct child elements and nested BattleDetails structure
                if (element.getTagName().equals("PokemonBattle")) {
                    NodeList battleDetails = element.getElementsByTagName("BattleDetails").item(0).getChildNodes();
                    JSONObject detailsJson = new JSONObject();
                    
                    for (int j = 0; j < battleDetails.getLength(); j++) {
                        Node detailNode = battleDetails.item(j);
                        if (detailNode.getNodeType() == Node.ELEMENT_NODE) {
                            detailsJson.put(detailNode.getNodeName(), detailNode.getTextContent().trim());
                        }
                    }
                    battleJson.put("BattleDetails", detailsJson);
                } else {
                    // Handle direct child elements (for winner/loser logs)
                    NodeList childNodes = element.getChildNodes();
                    for (int j = 0; j < childNodes.getLength(); j++) {
                        Node childNode = childNodes.item(j);
                        if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                            battleJson.put(childNode.getNodeName(), childNode.getTextContent().trim());
                        }
                    }
                }
                
                battlesArray.put(battleJson);
            }
        }

        JSONObject result = new JSONObject();
        result.put("PokemonBattles", battlesArray);
        return result.toString(4);
    }
}
