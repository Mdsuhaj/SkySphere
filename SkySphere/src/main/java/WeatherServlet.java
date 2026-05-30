// ================================================================
//  WeatherServlet.java  — UPGRADED WITH LIVE WEATHER API
//  Uses: OpenWeatherMap free API
//  Replace: YOUR_API_KEY_HERE with your actual key from openweathermap.org
//
//  How it works:
//    1. User selects a state → frontend calls /WeatherServlet?state=Kerala
//    2. Servlet maps state name to a major city in that state
//    3. Servlet calls OpenWeatherMap API using Java's HttpURLConnection
//    4. Parses the JSON response (no external library needed)
//    5. Saves real data to MySQL search_history table
//    6. Returns real weather JSON to the browser
// ================================================================

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class WeatherServlet extends HttpServlet {

    // ── YOUR API KEY — paste it here ──────────────────────────────
    // Get it free from: https://openweathermap.org/api
    // After signing up, go to: API keys tab → copy the default key
    // ⚠️  Wait 10-15 minutes after signing up before the key works
    private static final String API_KEY = "427c8e37b0d779b359a943f039d350e7";

    // ── OpenWeatherMap API base URL ───────────────────────────────
    // q = city name, units = metric (Celsius), appid = your key
    private static final String API_BASE_URL =
        "https://api.openweathermap.org/data/2.5/weather?q=%s&units=metric&appid=%s";

    // ── Map each Indian state → its major representative city ─────
    // OpenWeatherMap works best with city names, not state names.
    // We map each state to its capital or most well-known city.
    private static final Map<String, String> STATE_TO_CITY = new HashMap<>();

    static {
        STATE_TO_CITY.put("Tamil Nadu",     "Chennai");       // Capital: Chennai
        STATE_TO_CITY.put("Kerala",         "Thiruvananthapuram"); // Capital
        STATE_TO_CITY.put("Karnataka",      "Bangalore");     // Capital: Bengaluru
        STATE_TO_CITY.put("Andhra Pradesh", "Visakhapatnam"); // Largest city
        STATE_TO_CITY.put("Telangana",      "Hyderabad");     // Capital
        STATE_TO_CITY.put("Maharashtra",    "Mumbai");        // Capital
        STATE_TO_CITY.put("Gujarat",        "Ahmedabad");     // Largest city
        STATE_TO_CITY.put("Rajasthan",      "Jaipur");        // Capital
        STATE_TO_CITY.put("Punjab",         "Chandigarh");    // Capital
        STATE_TO_CITY.put("West Bengal",    "Kolkata");       // Capital
    }

    // ── doGet: handle incoming request from browser ───────────────
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Access-Control-Allow-Origin", "*");

        PrintWriter out = response.getWriter();

        // 1. Read the 'state' parameter from URL
        String stateName = request.getParameter("state");

        if (stateName == null || stateName.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\": \"State parameter is missing.\"}");
            return;
        }

        stateName = stateName.trim();

        // 2. Find which city represents this state
        String cityName = STATE_TO_CITY.get(stateName);

        if (cityName == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            out.print("{\"error\": \"State not found: " + escapeJson(stateName) + "\"}");
            return;
        }

        // 3. Call OpenWeatherMap API and get real weather
        try {
            // Build the full API URL with city name and API key
            String apiUrl = String.format(API_BASE_URL,
                cityName.replace(" ", "+"), API_KEY);

            System.out.println("[WeatherServlet] Calling API: " + apiUrl);

            // Make the HTTP GET request to OpenWeatherMap
            String rawJson = callWeatherAPI(apiUrl);

            System.out.println("[WeatherServlet] API Response: " + rawJson);

            // 4. Parse the JSON response from OpenWeatherMap
            WeatherResult result = parseWeatherJson(rawJson);

            if (result == null) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.print("{\"error\": \"Could not parse weather data. Check your API key.\"}");
                return;
            }

            // 5. Record the timestamp
            String searchTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

            // 6. Save real weather data to MySQL database
            saveToDatabase(stateName, result.temperature, result.condition, searchTime);

            // 7. Build and send response JSON to frontend
            String json = "{"
                + "\"stateName\":"         + "\"" + escapeJson(stateName)       + "\","
                + "\"cityName\":"          + "\"" + escapeJson(cityName)         + "\","
                + "\"temperature\":"       + "\"" + escapeJson(result.temperature) + "\","
                + "\"weatherCondition\":" + "\"" + escapeJson(result.condition)  + "\","
                + "\"humidity\":"          + "\"" + escapeJson(result.humidity)  + "\","
                + "\"wind\":"              + "\"" + escapeJson(result.wind)      + "\","
                + "\"visibility\":"        + "\"" + escapeJson(result.visibility)+ "\","
                + "\"feelsLike\":"         + "\"" + escapeJson(result.feelsLike) + "\","
                + "\"minTemp\":"           + "\"" + escapeJson(result.minTemp)   + "\","
                + "\"maxTemp\":"           + "\"" + escapeJson(result.maxTemp)   + "\","
                + "\"pressure\":"          + "\"" + escapeJson(result.pressure)  + "\","
                + "\"searchTime\":"        + "\"" + escapeJson(searchTime)       + "\""
                + "}";

            out.print(json);
            System.out.println("[WeatherServlet] Sent live weather for: " + stateName + " (" + cityName + ")");

        } catch (Exception e) {
            System.err.println("[WeatherServlet] API call failed: " + e.getMessage());
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\": \"Weather API call failed: " + escapeJson(e.getMessage()) + "\"}");
        }
    }

    // ── Make HTTP GET request to OpenWeatherMap ───────────────────
    /**
     * Opens a connection to the API URL and reads the response as a String.
     * Uses Java's built-in HttpURLConnection — no extra libraries needed.
     *
     * @param apiUrl  The complete OpenWeatherMap API URL
     * @return        The raw JSON response string
     * @throws IOException if the network request fails
     */
    private String callWeatherAPI(String apiUrl) throws IOException {
        URL url = new URL(apiUrl);

        // Open connection
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(8000);  // 8 seconds to connect
        connection.setReadTimeout(8000);     // 8 seconds to read response
        connection.setRequestProperty("Accept", "application/json");

        int responseCode = connection.getResponseCode();
        System.out.println("[WeatherServlet] HTTP response code: " + responseCode);

        // Check for 401 = bad API key, 404 = city not found
        if (responseCode == 401) {
            throw new IOException("Invalid API key. Check YOUR_API_KEY_HERE in WeatherServlet.java");
        }
        if (responseCode == 404) {
            throw new IOException("City not found in OpenWeatherMap database.");
        }
        if (responseCode != 200) {
            throw new IOException("API returned HTTP " + responseCode);
        }

        // Read the full response body
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(connection.getInputStream(), "UTF-8")
        );
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        connection.disconnect();

        return response.toString();
    }

    // ── Inner class to hold parsed weather values ─────────────────
    static class WeatherResult {
        String temperature;   // e.g. "32°C"
        String condition;     // e.g. "Clear Sky"
        String humidity;      // e.g. "72%"
        String wind;          // e.g. "14 km/h"
        String visibility;    // e.g. "10 km"
        String feelsLike;     // e.g. "35°C"
        String minTemp;       // e.g. "29°C"
        String maxTemp;       // e.g. "36°C"
        String pressure;      // e.g. "1013 hPa"
    }

    // ── Parse OpenWeatherMap JSON response ────────────────────────
    /**
     * Parses the OpenWeatherMap API JSON without using any external library.
     * We extract values using simple string search — beginner-friendly approach.
     *
     * OpenWeatherMap response format (simplified):
     * {
     *   "weather": [{ "description": "clear sky" }],
     *   "main": {
     *     "temp": 32.4,
     *     "feels_like": 35.1,
     *     "temp_min": 29.0,
     *     "temp_max": 36.0,
     *     "pressure": 1013,
     *     "humidity": 72
     *   },
     *   "wind": { "speed": 3.9 },
     *   "visibility": 10000
     * }
     *
     * @param json  The raw JSON string from the API
     * @return      A WeatherResult object with formatted values
     */
    private WeatherResult parseWeatherJson(String json) {
        try {
            WeatherResult result = new WeatherResult();

            // Extract temperature (in Celsius because we set units=metric)
            double temp = extractDouble(json, "\"temp\":");
            result.temperature = Math.round(temp) + "°C";

            // Extract feels like temperature
            double feelsLike = extractDouble(json, "\"feels_like\":");
            result.feelsLike = Math.round(feelsLike) + "°C";

            // Extract min and max temperature
            double tempMin = extractDouble(json, "\"temp_min\":");
            result.minTemp = Math.round(tempMin) + "°C";

            double tempMax = extractDouble(json, "\"temp_max\":");
            result.maxTemp = Math.round(tempMax) + "°C";

            // Extract humidity (percentage)
            double humidity = extractDouble(json, "\"humidity\":");
            result.humidity = (int) humidity + "%";

            // Extract atmospheric pressure
            double pressure = extractDouble(json, "\"pressure\":");
            result.pressure = (int) pressure + " hPa";

            // Extract wind speed (in m/s, convert to km/h)
            double windMs = extractDouble(json, "\"speed\":");
            double windKmh = windMs * 3.6;  // 1 m/s = 3.6 km/h
            result.wind = Math.round(windKmh) + " km/h";

            // Extract visibility (in meters, convert to km)
            double visMeters = extractDouble(json, "\"visibility\":");
            double visKm = visMeters / 1000.0;
            result.visibility = String.format("%.1f km", visKm);

            // Extract weather description
            // The description field looks like: "description":"clear sky"
            result.condition = capitalizeWords(extractString(json, "\"description\":\""));

            return result;

        } catch (Exception e) {
            System.err.println("[WeatherServlet] JSON parse error: " + e.getMessage());
            return null;
        }
    }

    // ── JSON parsing helpers ──────────────────────────────────────

    /**
     * Extracts a numeric (double) value after a given key in a JSON string.
     * Example: extractDouble("{\"temp\":32.4}", "\"temp\":") → 32.4
     */
    private double extractDouble(String json, String key) {
        int idx = json.indexOf(key);
        if (idx == -1) return 0.0;

        // Start reading after the key
        int start = idx + key.length();

        // Read until we hit a comma, closing brace, or space
        StringBuilder num = new StringBuilder();
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (Character.isDigit(c) || c == '.' || c == '-') {
                num.append(c);
            } else {
                break;
            }
        }

        return num.length() > 0 ? Double.parseDouble(num.toString()) : 0.0;
    }

    /**
     * Extracts a string value after a given key in a JSON string.
     * Example: extractString("{\"description\":\"clear sky\"}", "\"description\":\"")
     *          → "clear sky"
     */
    private String extractString(String json, String key) {
        int idx = json.indexOf(key);
        if (idx == -1) return "Unknown";

        int start = idx + key.length();
        int end   = json.indexOf("\"", start);  // Find closing quote

        if (end == -1) return "Unknown";
        return json.substring(start, end);
    }

    /**
     * Capitalizes the first letter of each word.
     * Example: "clear sky" → "Clear Sky"
     */
    private String capitalizeWords(String text) {
        if (text == null || text.isEmpty()) return text;
        String[] words = text.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)));
                sb.append(word.substring(1));
                sb.append(" ");
            }
        }
        return sb.toString().trim();
    }

    // ── Save to MySQL ─────────────────────────────────────────────
    /**
     * Inserts one row into search_history with REAL weather data.
     */
    private void saveToDatabase(String stateName, String temp,
                                String condition, String searchTime) {
        String sql = "INSERT INTO search_history "
                   + "(state_name, temperature, weather_condition, search_time) "
                   + "VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, stateName);
            ps.setString(2, temp);
            ps.setString(3, condition);
            ps.setString(4, searchTime);
            ps.executeUpdate();

            System.out.println("[WeatherServlet] Saved to DB: " + stateName + " " + temp);

        } catch (SQLException e) {
            System.err.println("[WeatherServlet] DB error: " + e.getMessage());
        }
    }

    // ── Escape JSON string ────────────────────────────────────────
    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}