package org.example;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.JSONArray;
import java.util.stream.Collectors;

public class NameScoreCalculator {
    //URL of the web service that provides the names
    private static final String SOURCE_URL = "";

    //URL of the web service where the calculated result is sent
    private static final String TARGET_URL = "";

    //Bearer token for authenticating the GET request to fetch names
    private static final String SOURCE_AUTH = "";

    //Bearer token for authenticating the POST request to submit the result.
    private static final String TARGET_AUTH = "";

    public static void main(String[] args) {
        try {
            // Fetch a list of names from a data source
            List<String> names = fetchNamesFromSource();
            
            // Preprocess the list of names 
            names = preprocessNames(names);
    
            // Calculate the total score based on the processed names
            long totalScore = calculateTotalNameScore(names);
    
            // Print the total score to the console for debugging purposes
            System.out.println("Total Score: " + totalScore);  
    
            // Send the calculated score to a target destination
            sendResultToTarget(totalScore, "Your Name", false);
        } catch (Exception e) {
            // Print an error message if an exception occurs
            System.err.println("An error occurred: " + e.getMessage());
            
            // Print the stack trace for debugging purposes
            e.printStackTrace();
        }
    }

    private static List<String> fetchNamesFromSource() throws Exception {
        // Construct the URL string with the required parameters
        String urlString = SOURCE_URL + "?archivo=first_names&extension=txt";
    
         // Execute the GET request and process the response
        return executeGetRequest(urlString, SOURCE_AUTH)
                // Parse the JSON response to extract names
                .map(NameScoreCalculator::parseNamesFromJson)
                // Throw an exception if the names cannot be fetched
                .orElseThrow(() -> new Exception("Failed to fetch names from source"));
    }

    private static Optional<String> executeGetRequest(String urlString, String auth) throws Exception {
        // Create a URL object from the provided URL string
        URL url = new URL(urlString);
        
        // Open an HTTP connection to the URL
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        
        // Set the Authorization header for the request
        con.setRequestProperty("Authorization", auth);
    
        // Try-with-resources to ensure the BufferedReader is closed after use
        try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
            // Read the response lines and join them into a single String, then wrap it in an Optional
            return Optional.of(in.lines().collect(Collectors.joining()));
        } catch (Exception e) {
            // Print an error message if an exception occurs and return an empty Optional
            System.err.println("Error executing GET request: " + e.getMessage());
            return Optional.empty();
        } finally {
            // Disconnect the HTTP connection
            con.disconnect();
        }
    }

    private static List<String> parseNamesFromJson(String jsonString) {
        // Convert the JSON string into a JSONArray
        JSONArray jsonArray = new JSONArray(jsonString);
        
        // Convert the JSONArray to a List, then stream through the list
        return jsonArray.toList().stream()
                // Map each object in the list to its "NAME" field and convert it to a string
                .map(obj -> ((Map<?, ?>) obj).get("NAME").toString())
                // Collect the results into a List of Strings
                .collect(Collectors.toList());
    }

    private static List<String> preprocessNames(List<String> names) {
        // Convert the list of names to a stream for processing
        return names.stream()
                // Remove leading and trailing whitespace from each name
                .map(String::trim)
                // Remove all non-alphabetic characters from each name
                .map(name -> name.replaceAll("[^a-zA-Z]", ""))
                // Convert each name to uppercase for consistent processing
                .map(String::toUpperCase)
                // Sort the names in alphabetical order
                .sorted()
                // Collect the processed names back into a list
                .collect(Collectors.toList());
    }

    private static long calculateTotalNameScore(List<String> names) {
        long totalScore = 0;  // Initialize total score to 0

        // Iterate through each name in the list
        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);  // Get the name at the current position
            long nameValue = getAlphabeticalValue(name);  // Calculate the alphabetical value of the name

            // Calculate the score for the current name and add it to the total score
            totalScore += nameValue * (i + 1);

            // Print debug information for the current name
            System.out.println("Name: " + name + ", Value: " + nameValue + ", Position: " + (i + 1) + ", Score: " + (nameValue * (i + 1)));  // Debug print
        }

        return totalScore;  // Return the total score of all names
    }

    private static int getAlphabeticalValue(String name) {
        // Convert the name to a stream of characters
        return name.chars()
                // Map each character to its alphabetical value
                .map(ch -> ch - 'A' + 1)
                // Sum the values
                .sum();
    }

    private static void sendResultToTarget(long totalScore, String name, boolean isTest) throws Exception {
        // Construct the URL with query parameters
        String urlString = TARGET_URL + "?archivo=first_names&extension=txt&nombre=" + name + "&prueba=" + (isTest ? 1 : 0);
        
        // Create the JSON input string
        String jsonInputString = "{ \"ResultadoObtenido\": " + totalScore + " }";
    
        // Create a URL object
        URL url = new URL(urlString);
        
        // Open a connection to the URL
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        
        // Set the request method to POST
        con.setRequestMethod("POST");
        
        // Set the request properties
        con.setRequestProperty("Authorization", TARGET_AUTH);
        con.setRequestProperty("Content-Type", "application/json; utf-8");
        
        // Enable output for the connection
        con.setDoOutput(true);
    
        // Write the JSON input string to the output stream
        try (OutputStream os = con.getOutputStream()) {
            byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
    
        // Get the response code from the server
        int responseCode = con.getResponseCode();
        System.out.println("Response Code: " + responseCode);  // Debug print
    
        // Read the response from the server
        try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            System.out.println("Response Body: " + response.toString());  // Debug print
        }
    
        // Disconnect the connection
        con.disconnect();
    }
} 