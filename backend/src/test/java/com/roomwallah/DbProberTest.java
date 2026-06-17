package com.roomwallah;

import org.junit.jupiter.api.Test;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DbProberTest {

    @Test
    public void probeDatabase() {
        System.out.println("=== STARTING POSTGRES PROBING ===");
        
        String[] users = {"postgres", "roomwallah_user"};
        String[] passwords = {"", "postgres", "admin", "roomwallah_secure_pass", "password", "root"};
        String[] databases = {"postgres", "roomwallah", "roomwallah_test"};
        
        for (String db : databases) {
            for (String user : users) {
                for (String password : passwords) {
                    String url = "jdbc:postgresql://localhost:5432/" + db;
                    try {
                        // Load Driver
                        Class.forName("org.postgresql.Driver");
                        Connection conn = DriverManager.getConnection(url, user, password);
                        System.out.println(">>> SUCCESS: connected to " + url + " as " + user + " (password: '" + password + "')");
                        
                        // Let's print existing databases or schemas
                        Statement stmt = conn.createStatement();
                        ResultSet rs = stmt.executeQuery("SELECT datname FROM pg_database WHERE datistemplate = false;");
                        List<String> dbs = new ArrayList<>();
                        while (rs.next()) {
                            dbs.add(rs.getString("datname"));
                        }
                        System.out.println("    Available DBs: " + dbs);
                        
                        rs.close();
                        stmt.close();
                        conn.close();
                    } catch (Exception e) {
                        // Suppress, try next
                    }
                }
            }
        }
        System.out.println("=== FINISHED POSTGRES PROBING ===");
    }
}
