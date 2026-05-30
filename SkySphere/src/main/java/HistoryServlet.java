// ================================================================
//  HistoryServlet.java
//  Package: (default package — put in src/main/java)
//  URL mapping: /HistoryServlet   (defined in web.xml)
//
//  How it works:
//    1. Frontend calls GET /SkySphere/HistoryServlet
//    2. Servlet runs SELECT on search_history table via JDBC
//    3. Returns all rows as a JSON array
// ================================================================

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Servlet that fetches all search history records from MySQL.
 *
 * Request:  GET /HistoryServlet
 * Response: JSON array of search history rows
 *
 * Sample response:
 * [
 *   {
 *     "id": 1,
 *     "stateName": "Tamil Nadu",
 *     "temperature": "34°C",
 *     "weatherCondition": "Sunny",
 *     "searchTime": "2025-08-15 10:30:00"
 *   },
 *   ...
 * ]
 */
public class HistoryServlet extends HttpServlet {

    // ── doGet: handle GET request from the frontend ────────────────
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Tell browser we are returning JSON
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Allow cross-origin requests
        response.setHeader("Access-Control-Allow-Origin", "*");

        PrintWriter out = response.getWriter();

        // SQL: select all rows, newest first
        String sql = "SELECT id, state_name, temperature, weather_condition, "
                   + "DATE_FORMAT(searched_at, '%Y-%m-%d %H:%i:%s') AS searched_at "
                   + "FROM search_history "
                   + "ORDER BY searched_at DESC "
                   + "LIMIT 50";   // Cap at 50 rows for performance

        // StringBuilder to accumulate the JSON array string
        StringBuilder json = new StringBuilder();
        json.append("[");  // Start of JSON array

        // Try-with-resources: connection, statement, and resultset auto-close
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {  // Execute SELECT

            boolean first = true;  // Track if we need a comma before each entry

            // Loop through every row returned by MySQL
            while (rs.next()) {
                if (!first) {
                    json.append(",");   // Add comma between JSON objects
                }
                first = false;

                // Read column values from the current row
                int    id        = rs.getInt("id");
                String stateName = rs.getString("state_name");
                String temp      = rs.getString("temperature");
                String condition = rs.getString("weather_condition");
                String time      = rs.getString("searched_at");

                // Build one JSON object for this row
                json.append("{")
                    .append("\"id\":"              ).append(id                        ).append(",")
                    .append("\"stateName\":"        ).append("\"").append(escapeJson(stateName)).append("\",")
                    .append("\"temperature\":"      ).append("\"").append(escapeJson(temp)     ).append("\",")
                    .append("\"weatherCondition\":").append("\"").append(escapeJson(condition)).append("\",")
                    .append("\"searchTime\":"       ).append("\"").append(escapeJson(time)     ).append("\"")
                    .append("}");
            }

            System.out.println("[HistoryServlet] History fetched successfully.");

        } catch (SQLException e) {
            // If DB fails, return an empty array with an error note
            System.err.println("[HistoryServlet] DB error: " + e.getMessage());
            e.printStackTrace();
            // Return empty array instead of crashing
            out.print("[]");
            return;
        }

        json.append("]");   // End of JSON array
        out.print(json.toString());
    }

    // ── JSON helper ────────────────────────────────────────────────
    /**
     * Escapes characters that would break JSON string literals.
     */
    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"",  "\\\"")
                .replace("\n",  "\\n")
                .replace("\r",  "\\r")
                .replace("\t",  "\\t");
    }
}
