/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package packpal;

import views.WelcomeView;
import database.DatabaseConfig;
import dao.PackingListDAO;

import javax.swing.*;
import java.sql.SQLException;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Set system look and feel
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                
                // Test database connection before starting the app
                if (testDatabaseSetup()) {
                    // Database is ready, start the application
                    new WelcomeView();
                } else {
                    // Database failed, show error and exit
                    showDatabaseError();
                    System.exit(1);
                }
                
            } catch (Exception e) {
                e.printStackTrace();
                showFatalError(e);
                System.exit(1);
            }
        });
    }

    /**
     * Test database connection and initialize schema
     */
    private static boolean testDatabaseSetup() {
        System.out.println("🚀 Starting PackPal Application...");
        System.out.println("🧪 Testing database connection...");
        
        try {
            // Test basic connection
            if (!DatabaseConfig.testConnection()) {
                System.err.println("❌ Initial database connection test failed");
                return false;
            }
            
            System.out.println("✅ Database connection successful");
            
            // Initialize database schema
            System.out.println("🔄 Initializing database schema...");
            PackingListDAO dao = new PackingListDAO();
            dao.initSchema();
            
            System.out.println("✅ Database setup completed successfully");
            return true;
            
        } catch (Exception e) {
            System.err.println("❌ Database setup failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Show database connection error dialog
     */
    private static void showDatabaseError() {
        JOptionPane.showMessageDialog(null, 
            "<html><div style='text-align: center;'>" +
            "<h2>Database Connection Failed</h2>" +
            "<p>Unable to connect to MySQL database.</p>" +
            "<br>" +
            "<p><b>Please ensure:</b></p>" +
            "<p>• MySQL server is running</p>" +
            "<p>• XAMPP/WAMP is started</p>" +
            "<p>• MySQL service is active</p>" +
            "<p>• Port 3306 is available</p>" +
            "<br>" +
            "<p><small>Check console for detailed error messages</small></p>" +
            "</div></html>", 
            "PackPal - Database Error", 
            JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Show fatal application error
     */
    private static void showFatalError(Exception e) {
        JOptionPane.showMessageDialog(null, 
            "<html><div style='text-align: center;'>" +
            "<h2>Application Error</h2>" +
            "<p>Failed to start PackPal application.</p>" +
            "<br>" +
            "<p><b>Error:</b> " + e.getMessage() + "</p>" +
            "<br>" +
            "<p><small>Please check the console for details</small></p>" +
            "</div></html>", 
            "PackPal - Fatal Error", 
            JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Optional: Pre-load some data for testing
     */
    private static void preloadTestData() {
        try {
            PackingListDAO dao = new PackingListDAO();
            
            // Check if we have any lists, if not create some sample data
            var lists = dao.getLists();
            if (lists.isEmpty()) {
                System.out.println("📝 Creating sample packing lists...");
                
                // Create sample lists for different trip types
                dao.createPackingList("Beach Vacation", "Hawaii", "15/12/2024 - 22/12/2024", "Beach");
                dao.createPackingList("Business Conference", "New York", "10/01/2025 - 12/01/2025", "Business");
                dao.createPackingList("Weekend Camping", "Lake District", "05/07/2024 - 07/07/2024", "Camping");
                
                System.out.println("✅ Sample data created successfully");
            }
            
        } catch (Exception e) {
            System.err.println("❌ Failed to create sample data: " + e.getMessage());
        }
    }
}
