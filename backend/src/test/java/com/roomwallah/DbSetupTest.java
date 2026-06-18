package com.roomwallah;
import org.junit.jupiter.api.Test;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class DbSetupTest {

    @Test
    public void setupTestDatabase() {
        System.out.println("=== STARTING DATABASE SETUP ===");
        
        String url = "jdbc:postgresql://localhost:5432/roomwallah_test";
        String user = "roomwallah_user";
        String password = "roomwallah_secure_pass";
        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement()) {
            
            // 1. Create roomwallah_user role if it doesn't exist
            boolean userExists = false;
            try (ResultSet rs = stmt.executeQuery("SELECT 1 FROM pg_roles WHERE rolname='roomwallah_user'")) {
                if (rs.next()) {
                    userExists = true;
                }
            }
            
            if (!userExists) {
                System.out.println("Creating role roomwallah_user...");
                stmt.execute("CREATE ROLE roomwallah_user WITH LOGIN PASSWORD 'roomwallah_secure_pass'");
                System.out.println("Role roomwallah_user created successfully.");
            } else {
                System.out.println("Role roomwallah_user already exists. Updating password...");
                stmt.execute("ALTER ROLE roomwallah_user WITH PASSWORD 'roomwallah_secure_pass'");
            }
            
            // Grant superuser/login privileges just in case to avoid permission issues during test ddl execution
            try {
                stmt.execute("ALTER ROLE roomwallah_user CREATEDB");
                System.out.println("Granted CREATEDB privilege to roomwallah_user.");
            } catch (Exception e) {
                System.out.println("Could not grant CREATEDB to roomwallah_user: " + e.getMessage());
            }

            // 2. Create roomwallah_test database if it doesn't exist
            boolean dbExists = false;
            try (ResultSet rs = stmt.executeQuery("SELECT 1 FROM pg_database WHERE datname='roomwallah_test'")) {
                if (rs.next()) {
                    dbExists = true;
                }
            }
            
            if (!dbExists) {
                System.out.println("Creating database roomwallah_test...");
                stmt.execute("CREATE DATABASE roomwallah_test OWNER roomwallah_user");
                System.out.println("Database roomwallah_test created successfully.");
            } else {
                System.out.println("Database roomwallah_test already exists.");
                // Ensure owner is set
                try {
                    stmt.execute("ALTER DATABASE roomwallah_test OWNER TO roomwallah_user");
                } catch (Exception e) {
                    System.out.println("Could not alter owner of roomwallah_test: " + e.getMessage());
                }
            }
            
            // Also create target main database "roomwallah" if it doesn't exist, just in case
            boolean mainDbExists = false;
            try (ResultSet rs = stmt.executeQuery("SELECT 1 FROM pg_database WHERE datname='roomwallah'")) {
                if (rs.next()) {
                    mainDbExists = true;
                }
            }
            if (!mainDbExists) {
                System.out.println("Creating database roomwallah...");
                stmt.execute("CREATE DATABASE roomwallah OWNER roomwallah_user");
                System.out.println("Database roomwallah created successfully.");
            }
            
            // 3. Grant privileges
            System.out.println("Granting privileges...");
            stmt.execute("GRANT ALL PRIVILEGES ON DATABASE roomwallah_test TO roomwallah_user");
            stmt.execute("GRANT ALL PRIVILEGES ON DATABASE roomwallah TO roomwallah_user");
            System.out.println("Privileges granted successfully.");
            
        } catch (Exception e) {
            System.err.println("Database setup failed: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        
        System.out.println("=== DATABASE SETUP COMPLETED ===");
    }
}
