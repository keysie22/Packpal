package dao;

import database.DatabaseConfig;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PackingListDAO {
    private static final int USER_ID = 1; // Temporary user

    private Connection getConnection() throws SQLException {
        return DatabaseConfig.getConnection();
    }

    // ✅ Fixed Schema Initialization
    public void initSchema() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {

            // Create users table first
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTO_INCREMENT,
                    email VARCHAR(100) UNIQUE,
                    password VARCHAR(100),
                    name VARCHAR(100)
                )
            """);

            // FIXED: Changed table name from packing_lists to packing_list (consistent naming)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS packing_list (
                    id INTEGER PRIMARY KEY AUTO_INCREMENT,
                    user_id INTEGER,
                    list_name VARCHAR(100),
                    destination VARCHAR(100),
                    dates VARCHAR(100),
                    type VARCHAR(50),
                    description TEXT,
                    created_date DATE DEFAULT CURRENT_DATE,
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                )
            """);

            // FIXED: Corrected foreign key reference to packing_list (not packing_lists)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS list_items (
                    id INTEGER PRIMARY KEY AUTO_INCREMENT,
                    list_id INTEGER,
                    name VARCHAR(100),
                    packed BOOLEAN DEFAULT FALSE,
                    FOREIGN KEY (list_id) REFERENCES packing_list(id) ON DELETE CASCADE
                )
            """);

            // Insert sample data
            stmt.execute("""
                INSERT IGNORE INTO users (id, email, password, name)
                VALUES (1, 'user@example.com', 'pass', 'John Doe')
            """);

            // FIXED: Using packing_list table name
            stmt.execute("""
                INSERT IGNORE INTO packing_list (id, user_id, list_name, destination, dates, type, description)
                VALUES (1, 1, 'Weekend Getaway', 'Beach', '2025-10-15 to 2025-10-17', 'Weekend', 'Sample trip')
            """);

            stmt.execute("""
                INSERT IGNORE INTO list_items (list_id, name)
                VALUES (1, 'Passport'), (1, 'Phone Charger'), (1, 'Sunglasses'), (1, 'Toothbrush')
            """);

            System.out.println("✅ Schema initialized successfully");

        } catch (SQLException e) {
            System.err.println("❌ Schema initialization failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ✅ Create a new list - FIXED: Removed description reference
    public int createPackingList(String name, String destination, String dates, String type) {
        if (name == null || name.isEmpty() || destination == null || destination.isEmpty()) {
            System.err.println("❌ Invalid input");
            return -1;
        }

        String safeName = name == null ? "" : name.trim();
        String safeDest = destination == null ? "" : destination.trim();
        String safeDates = dates == null ? "" : dates.trim();
        String safeType = type == null ? "" : type.trim();
        String safeDescription = ""; // FIXED: Removed problematic description reference

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO packing_list (user_id, list_name, destination, dates, type, description) VALUES (?, ?, ?, ?, ?, ?)",
                     Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, USER_ID);
            stmt.setString(2, safeName);
            stmt.setString(3, safeDest);
            stmt.setString(4, safeDates);
            stmt.setString(5, safeType);
            stmt.setString(6, safeDescription);

            int affected = stmt.executeUpdate();
            if (affected == 0) {
                System.err.println("❌ Creating list failed");
                return -1;
            }

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    addDefaultItems(id, type);
                    return id;
                }
            }

        } catch (SQLException e) {
            System.err.println("❌ SQL Error in createPackingList: " + e.getMessage());
            e.printStackTrace();
        }

        return -1;
    }

    // ✅ Get lists - FIXED: Using correct table name packing_list
    public List<PackingList> getLists() {
        List<PackingList> lists = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("""
                SELECT pl.id, pl.list_name, pl.destination,
                       COUNT(li.id) AS item_count,
                       SUM(CASE WHEN li.packed = 1 THEN 1 ELSE 0 END) AS packed_count,
                       pl.type
                FROM packing_list pl
                LEFT JOIN list_items li ON pl.id = li.list_id
                WHERE pl.user_id = ?
                GROUP BY pl.id
             """)) {

            stmt.setInt(1, USER_ID);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int total = rs.getInt("item_count");
                    int packed = rs.getInt("packed_count");
                    String status = (packed == total && total > 0)
                            ? "Packed"
                            : (packed > 0 ? "In Progress" : "Not Started");
                    String subtitle = total + " items - " + status;

                    lists.add(new PackingList(
                            rs.getInt("id"),
                            rs.getString("list_name"),
                            subtitle
                    ));
                }
            }

        } catch (SQLException e) {
            System.err.println("❌ SQL Error in getLists: " + e.getMessage());
            e.printStackTrace();
        }
        return lists;
    }

    // ✅ Default items
    private void addDefaultItems(int listId, String type) {
        String[] defaults = switch (type) {
            case "Beach" -> new String[]{"Swimsuit", "Towel", "Sunscreen"};
            case "Business" -> new String[]{"Laptop", "Notebook", "Business Cards"};
            case "Camping" -> new String[]{"Tent", "Sleeping Bag", "Flashlight"};
            case "Weekend" -> new String[]{"Casual Clothes", "Snacks", "Toothbrush"};
            default -> new String[]{"Essentials", "Clothes", "Toiletries"};
        };

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO list_items (list_id, name) VALUES (?, ?)")) {
            for (String item : defaults) {
                stmt.setInt(1, listId);
                stmt.setString(2, item);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("❌ Error adding default items: " + e.getMessage());
        }
    }

    // ✅ Item operations
    public void addItem(int listId, String name) {
        if (name == null || name.isEmpty()) return;

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO list_items (list_id, name) VALUES (?, ?)")) {
            stmt.setInt(1, listId);
            stmt.setString(2, name.trim());
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ Error adding item: " + e.getMessage());
        }
    }

    public void deleteItem(int listId, int itemId) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "DELETE FROM list_items WHERE list_id = ? AND id = ?")) {
            stmt.setInt(1, listId);
            stmt.setInt(2, itemId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ Error deleting item: " + e.getMessage());
        }
    }

    public List<ListItem> getItemsByListId(int listId) {
        List<ListItem> items = new ArrayList<>();
        try (Connection conn = getConnection(); 
             PreparedStatement stmt = conn.prepareStatement(
                "SELECT id, name, packed FROM list_items WHERE list_id = ?")) {
            stmt.setInt(1, listId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    items.add(new ListItem(rs.getInt("id"), rs.getString("name"), rs.getBoolean("packed")));
                }
            }
        } catch (SQLException e) {
            System.err.println("GetItemsByListId error: " + e.getMessage());
            e.printStackTrace();
        }
        return items;
    }

    public void setItemPackedStatus(int itemId, boolean packed) {
        try (Connection conn = getConnection(); 
             PreparedStatement stmt = conn.prepareStatement(
                "UPDATE list_items SET packed = ? WHERE id = ?")) {
            stmt.setBoolean(1, packed);
            stmt.setInt(2, itemId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("SetItemPackedStatus error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public int getTotalItemsCount(int listId) {
        try (Connection conn = getConnection(); 
             PreparedStatement stmt = conn.prepareStatement(
                "SELECT COUNT(*) FROM list_items WHERE list_id = ?")) {
            stmt.setInt(1, listId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("GetTotalItemsCount error: " + e.getMessage());
        }
        return 0;
    }

    public int getPackedItemsCount(int listId) {
        try (Connection conn = getConnection(); 
             PreparedStatement stmt = conn.prepareStatement(
                "SELECT COUNT(*) FROM list_items WHERE list_id = ? AND packed = 1")) {
            stmt.setInt(1, listId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("GetPackedItemsCount error: " + e.getMessage());
        }
        return 0;
    }

    // FIXED: Implemented the missing method
    public List<ListItem> getListItems(int listId) {
        return getItemsByListId(listId); // Reuse existing implementation
    }

    // ✅ Helper classes
    public static class PackingList {
        private int id;
        private String name;
        private String subtitle;

        public PackingList(int id, String name, String subtitle) {
            this.id = id;
            this.name = name;
            this.subtitle = subtitle;
        }

        public int getId() { return id; }
        public String getName() { return name; }
        public String getSubtitle() { return subtitle; }
    }

    public static class ListItem {
        private int id;
        private String name;
        private boolean packed;

        public ListItem(int id, String name, boolean packed) {
            this.id = id;
            this.name = name;
            this.packed = packed;
        }

        public int getId() { return id; }
        public String getName() { return name; }
        public boolean isPacked() { return packed; }

        public void setName(String name) { this.name = name; }
        public void setPacked(boolean packed) { this.packed = packed; }
    }
}
