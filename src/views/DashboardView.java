package views;

import controllers.DashboardController;
import dao.PackingListDAO;
import models.ProfileModel;
import utils.UIConstants;
import views.components.RoundedButton;
import views.components.RoundedTextField;
import views.components.ShadowPanel;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

public class DashboardView extends JFrame {
    private DashboardController controller;
    private JTabbedPane tabbedPane;
    private JPanel topBar;
    private JButton backButton;
    private List<RoundedButton> tabButtons;
    private JPanel myListsPanel;

    public DashboardView() {
        controller = new DashboardController(this);
        initializeUI();
        setTitle("PackPal Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void initializeUI() {
        setPreferredSize(new Dimension(375, 667));
        setMinimumSize(new Dimension(350, 600));
        setResizable(true);

        // Top Bar
        topBar = createTopBar();
        add(topBar, BorderLayout.NORTH);

        // Tabbed Content
        tabbedPane = new JTabbedPane();
        tabbedPane.setTabPlacement(JTabbedPane.TOP);
        tabbedPane.setPreferredSize(new Dimension(375, 600));
        tabbedPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        tabbedPane.addChangeListener(new TabChangeListener());

        // Add My Lists Tab
        myListsPanel = createMyListsPanel();
        tabbedPane.addTab(null, myListsPanel);

        // Add Settings Tab
        JPanel settingsPanel = createSettingsPanel();
        tabbedPane.addTab(null, settingsPanel);

        // Set Initial Tab
        tabbedPane.setSelectedIndex(0);
        updateTabLabels();

        add(tabbedPane, BorderLayout.CENTER);
    }

    private JPanel createTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(UIConstants.PRIMARY_BLUE);
        bar.setPreferredSize(new Dimension(375, 60));

        // Back Button
        backButton = new JButton("← Back");
        backButton.setForeground(Color.WHITE);
        backButton.setContentAreaFilled(false);
        backButton.setBorderPainted(false);
        backButton.setFocusPainted(false);
        backButton.setFont(UIConstants.BODY_FONT.deriveFont(14f));
        backButton.addActionListener(e -> controller.handleBackToWelcome());
        bar.add(backButton, BorderLayout.WEST);

        // Tab Buttons
        JPanel tabsPanel = new JPanel(new GridLayout(1, 2));
        tabsPanel.setOpaque(false);

        RoundedButton myListsTab = new RoundedButton("📋 My Lists", UIConstants.PRIMARY_BLUE);
        myListsTab.setForeground(Color.WHITE);
        myListsTab.setPreferredSize(new Dimension(150, 40));
        myListsTab.setActionCommand("0");
        myListsTab.addActionListener(createTabActionListener(0));
        tabsPanel.add(myListsTab);

        RoundedButton settingsTab = new RoundedButton("⚙️ Settings", Color.WHITE);
        settingsTab.setForeground(UIConstants.PRIMARY_BLUE);
        settingsTab.setPreferredSize(new Dimension(150, 40));
        settingsTab.setActionCommand("1");
        settingsTab.addActionListener(createTabActionListener(1));
        tabsPanel.add(settingsTab);

        bar.add(tabsPanel, BorderLayout.CENTER);

        tabButtons = Arrays.asList(myListsTab, settingsTab);

        return bar;
    }

    private ActionListener createTabActionListener(int tabIndex) {
        return e -> {
            tabbedPane.setSelectedIndex(tabIndex);
            updateTabLabels();
        };
    }

    private void updateTabLabels() {
        int selected = tabbedPane.getSelectedIndex();
        for (int i = 0; i < tabButtons.size(); i++) {
            RoundedButton tabBtn = tabButtons.get(i);
            if (i == selected) {
                tabBtn.setBackground(UIConstants.PRIMARY_BLUE);
                tabBtn.setForeground(Color.WHITE);
                tabBtn.setFont(UIConstants.TITLE_FONT.deriveFont(Font.BOLD));
                tabBtn.setBorder(BorderFactory.createCompoundBorder(
                    new RoundedButton.UnderlineBorder(Color.WHITE, 3),
                    tabBtn.getBorder()
                ));
            } else {
                tabBtn.setBackground(Color.WHITE);
                tabBtn.setForeground(UIConstants.PRIMARY_BLUE);
                tabBtn.setFont(UIConstants.BODY_FONT);
                tabBtn.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
            }
        }
        topBar.repaint();
    }

    private JPanel createMyListsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Create Button (New)
        RoundedButton createBtn = new RoundedButton("Create New List", UIConstants.PRIMARY_BLUE);
        createBtn.setPreferredSize(new Dimension(300, 50));
        createBtn.addActionListener(e -> {
            new CreateNewListView();
            // Optional: dispose() this if modal
        });
        panel.add(createBtn, BorderLayout.NORTH);
        panel.add(Box.createRigidArea(new Dimension(0, 10)), BorderLayout.NORTH);  // Spacing

        // Search Bar
        RoundedTextField searchField = new RoundedTextField(20);
        searchField.setText("🔍 Search lists.");
        searchField.setEditable(true);
        searchField.setPreferredSize(new Dimension(300, 50));
        JPanel searchPanel = new JPanel(new BorderLayout());
        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 20, 0));
        panel.add(searchPanel, BorderLayout.NORTH);

        // Lists (Scrollable) - WITH ERROR HANDLING
        JPanel listsPanel = createListsContent();
        JScrollPane scroll = new JScrollPane(listsPanel);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    // FIXED: Added proper error handling for database operations
    private JPanel createListsContent() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);

        try {
            // Initialize database schema first
            PackingListDAO dao = new PackingListDAO();
            dao.initSchema();  // This should create tables if they don't exist

            // Wait a moment for schema to initialize
            Thread.sleep(100);

            // Load lists from database
            List<PackingListDAO.PackingList> dbLists = dao.getLists();

            if (dbLists.isEmpty()) {
                // Show empty state
                JLabel emptyLabel = new JLabel("No packing lists yet. Create your first list!");
                emptyLabel.setFont(UIConstants.BODY_FONT.deriveFont(14f));
                emptyLabel.setForeground(Color.GRAY);
                emptyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                emptyLabel.setBorder(BorderFactory.createEmptyBorder(50, 0, 0, 0));
                panel.add(emptyLabel);
            } else {
                // Dynamic sections (group by status/type)
                List<PackingListDAO.PackingList> recent = new ArrayList<>();
                List<PackingListDAO.PackingList> templates = new ArrayList<>();
                List<PackingListDAO.PackingList> shared = new ArrayList<>();

                for (PackingListDAO.PackingList list : dbLists) {
                    String subtitle = list.getSubtitle().toLowerCase();
                    if (subtitle.contains("packed") || subtitle.contains("in progress")) {
                        recent.add(list);
                    } else if (subtitle.contains("template")) {
                        templates.add(list);
                    } else if (subtitle.contains("shared")) {
                        shared.add(list);
                    } else {
                        recent.add(list); // Default to recent
                    }
                }

                // Limit recent to 2 items
                if (recent.size() > 2) {
                    recent = recent.subList(0, 2);
                }

                addSection(panel, "Recent", recent);
                addSection(panel, "Templates", templates);
                addSection(panel, "Shared", shared);
            }

        } catch (Exception e) {
            System.err.println("Error loading lists: " + e.getMessage());
            
            // Show error state
            JLabel errorLabel = new JLabel("<html><center>Error loading lists<br><small>Please try again</small></center></html>");
            errorLabel.setFont(UIConstants.BODY_FONT.deriveFont(14f));
            errorLabel.setForeground(Color.RED);
            errorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            errorLabel.setBorder(BorderFactory.createEmptyBorder(50, 0, 0, 0));
            panel.add(errorLabel);

            // Add retry button
            RoundedButton retryBtn = new RoundedButton("Retry", UIConstants.PRIMARY_BLUE);
            retryBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
            retryBtn.addActionListener(e1 -> refreshLists());
            panel.add(retryBtn);
        }

        return panel;
    }

    // FIXED: Updated addSection to use PackingList objects
    private void addSection(JPanel parent, String title, List<PackingListDAO.PackingList> lists) {
        if (lists.isEmpty()) {
            return; // Don't show empty sections
        }

        JLabel sectionTitleLabel = new JLabel(title);
        sectionTitleLabel.setFont(UIConstants.TITLE_FONT.deriveFont(16f).deriveFont(Font.BOLD));
        sectionTitleLabel.setForeground(Color.BLACK);
        sectionTitleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        parent.add(sectionTitleLabel);

        for (PackingListDAO.PackingList list : lists) {
            ShadowPanel card = new ShadowPanel(new BorderLayout());
            card.setPreferredSize(new Dimension(300, 70));
            card.setMaximumSize(new Dimension(300, 70));

            JPanel leftPanel = new JPanel(new GridLayout(2, 1));
            leftPanel.setOpaque(false);
            JLabel itemNameLabel = new JLabel(list.getName());
            itemNameLabel.setFont(UIConstants.BODY_FONT.deriveFont(16f));
            itemNameLabel.setForeground(Color.BLACK);
            JLabel subtitle = new JLabel(list.getSubtitle());
            subtitle.setFont(UIConstants.BODY_FONT.deriveFont(12f));
            subtitle.setForeground(Color.GRAY);
            leftPanel.add(itemNameLabel);
            leftPanel.add(subtitle);
            card.add(leftPanel, BorderLayout.WEST);

            JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            rightPanel.setOpaque(false);

            JLabel blueDot = new JLabel("•");
            blueDot.setFont(new Font("Arial", Font.BOLD, 20));
            blueDot.setForeground(UIConstants.PRIMARY_BLUE);
            rightPanel.add(blueDot);

            JButton editBtn = new JButton("Edit");
            editBtn.setContentAreaFilled(false);
            editBtn.setForeground(UIConstants.PRIMARY_BLUE);
            editBtn.setFont(UIConstants.BODY_FONT.deriveFont(12f));
            editBtn.setBorderPainted(false);
            editBtn.addActionListener(e -> {
                // Use the actual list ID from the database
                int listId = list.getId();
                // Navigate to packing list view
                // new PackingListView(listId);
                JOptionPane.showMessageDialog(this, "Opening list: " + list.getName(), "Edit List", JOptionPane.INFORMATION_MESSAGE);
            });
            rightPanel.add(editBtn);

            card.add(rightPanel, BorderLayout.EAST);

            parent.add(card);
            parent.add(Box.createRigidArea(new Dimension(0, 10)));
        }

        parent.add(Box.createRigidArea(new Dimension(0, 20)));
    }

    // FIXED: Added refresh method
    public void refreshLists() {
        Component[] components = myListsPanel.getComponents();
        for (Component comp : components) {
            if (comp instanceof JScrollPane) {
                JScrollPane scrollPane = (JScrollPane) comp;
                JPanel newListsContent = createListsContent();
                scrollPane.setViewportView(newListsContent);
                break;
            }
        }
        myListsPanel.revalidate();
        myListsPanel.repaint();
    }

    private JPanel createSettingsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Settings content here
        JLabel settingsLabel = new JLabel("Settings", SwingConstants.CENTER);
        settingsLabel.setFont(UIConstants.TITLE_FONT.deriveFont(20f));
        settingsLabel.setForeground(UIConstants.PRIMARY_BLUE);
        panel.add(settingsLabel, BorderLayout.NORTH);

        // Add your settings components here
        JPanel settingsContent = new JPanel();
        settingsContent.setLayout(new BoxLayout(settingsContent, BoxLayout.Y_AXIS));
        settingsContent.setBackground(Color.WHITE);

        // Example settings items
        addSettingsItem(settingsContent, "Profile", "Manage your account settings");
        addSettingsItem(settingsContent, "Notifications", "Configure app notifications");
        addSettingsItem(settingsContent, "Privacy", "Privacy and security settings");

        panel.add(settingsContent, BorderLayout.CENTER);

        return panel;
    }

    private void addSettingsItem(JPanel parent, String title, String description) {
        JPanel itemPanel = new JPanel(new BorderLayout());
        itemPanel.setBackground(Color.WHITE);
        itemPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        itemPanel.setMaximumSize(new Dimension(300, 60));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(UIConstants.BODY_FONT.deriveFont(16f).deriveFont(Font.BOLD));
        titleLabel.setForeground(Color.BLACK);

        JLabel descLabel = new JLabel(description);
        descLabel.setFont(UIConstants.BODY_FONT.deriveFont(12f));
        descLabel.setForeground(Color.GRAY);

        JPanel textPanel = new JPanel(new GridLayout(2, 1));
        textPanel.setBackground(Color.WHITE);
        textPanel.add(titleLabel);
        textPanel.add(descLabel);

        itemPanel.add(textPanel, BorderLayout.WEST);

        JButton arrowBtn = new JButton("›");
        arrowBtn.setContentAreaFilled(false);
        arrowBtn.setForeground(UIConstants.PRIMARY_BLUE);
        arrowBtn.setFont(UIConstants.TITLE_FONT.deriveFont(20f));
        arrowBtn.setBorderPainted(false);
        itemPanel.add(arrowBtn, BorderLayout.EAST);

        parent.add(itemPanel);
        parent.add(Box.createRigidArea(new Dimension(0, 5)));
    }

    // TabChangeListener and getters
    private class TabChangeListener implements ChangeListener {
        @Override
        public void stateChanged(ChangeEvent e) {
            updateTabLabels();
        }
    }

    public JTabbedPane getTabbedPane() { return tabbedPane; }
    
    public void navigateToWelcome() {
        this.setVisible(false);
        WelcomeView welcomeView = new WelcomeView();
        welcomeView.setVisible(true);
        this.dispose();
    }
}
