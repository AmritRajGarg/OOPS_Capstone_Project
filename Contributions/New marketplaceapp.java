// new code for the main scr 

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MarketplaceApp extends JFrame {
    CardLayout cardLayout;
    JPanel mainPanel;
    ArrayList<Product> cart = new ArrayList<>();
    HashMap<String, List<Product>> categoryProducts = new HashMap<>();
    Connection connection; // Database connection
    String userRole = "buyer"; // Default user role

    public MarketplaceApp() {
        setTitle("Marketplace");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Initialize database connection
        try {
            connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/marketplace", "root", ""); // Replace with your credentials
            System.out.println("Connected to the database!");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database connection failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }

        populateProducts();

        JPanel topPanel = createTopPanel();
        add(topPanel, BorderLayout.NORTH);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        mainPanel.add(homePage(), "Home");
        mainPanel.add(cartPage(), "Cart");
        mainPanel.add(auctionPage(), "Auction");

        add(mainPanel, BorderLayout.CENTER);
        setVisible(true);
    }

    private void populateProducts() {
        categoryProducts.clear();

        categoryProducts.put("Electronics", new ArrayList<>());
        categoryProducts.put("Clothing", new ArrayList<>());
        categoryProducts.put("Home & Garden", new ArrayList<>());
        categoryProducts.put("Sports", new ArrayList<>());

        try {
            if (connection != null) {
                String query = "SELECT * FROM products";
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(query);

                while (resultSet.next()) {
                    String category = resultSet.getString("category");
                    String name = resultSet.getString("name");
                    String price = resultSet.getString("price");
                    int productId = resultSet.getInt("id");
                    boolean isAuction = resultSet.getInt("is_auction") == 1;
                    Product product = new Product(name, price, isAuction, productId);
                    categoryProducts.get(category).add(product);
                }
                resultSet.close();
                statement.close();
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error fetching products: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private JPanel createTopPanel() {
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(Color.WHITE);
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        JLabel logoLabel = new JLabel("Marketplace");
        logoLabel.setFont(new Font("Arial", Font.BOLD, 38));
        logoLabel.setForeground(new Color(0, 100, 210));

        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 30, 10));
        navPanel.setBackground(Color.WHITE);
        String[] navItems = {"Buy", "Sell", "Auction"};
        for (String item : navItems) {
            JButton navButton = new JButton(item);
            navButton.setFocusPainted(false);
            navButton.setBackground(Color.WHITE);
            navButton.setForeground(Color.BLACK);
            navButton.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
            navButton.setFont(new Font("Arial", Font.PLAIN, 28));
            navButton.addActionListener(e -> {
                if (item.equals("Sell")) {
                    userRole = "seller";
                    cardLayout.show(mainPanel, "Sell");
                } else if (item.equals("Buy")) {
                    userRole = "buyer";
                    cardLayout.show(mainPanel, "Home");
                } else if (item.equals("Auction")) {
                    userRole = "buyer";
                    cardLayout.show(mainPanel, "Auction");
                }
            });
            navPanel.add(navButton);
        }

        topPanel.add(logoLabel, BorderLayout.WEST);
        topPanel.add(navPanel, BorderLayout.EAST);
        return topPanel;
    }

    private JPanel homePage() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 20));
        searchPanel.setBackground(Color.WHITE);
        JTextField searchField = new JTextField(30);
        searchField.setPreferredSize(new Dimension(300, 30));
        JButton searchButton = new JButton("Search");
        searchButton.setPreferredSize(new Dimension(50, 30));
        searchButton.setFocusPainted(false);
        searchButton.setBackground(Color.LIGHT_GRAY);
        searchButton.addActionListener(e -> {
            String searchText = searchField.getText().toLowerCase();
            JPanel searchResultsPanel = new JPanel(new BorderLayout());
            searchResultsPanel.setBackground(Color.WHITE);
            JLabel titleLabel = new JLabel("Search Results for \"" + searchText + "\"", SwingConstants.CENTER);
            titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
            searchResultsPanel.add(titleLabel, BorderLayout.NORTH);

            DefaultTableModel searchTableModel = new DefaultTableModel(new String[]{"Product", "Price", "Add to Cart"}, 0);
            JTable searchTable = new JTable(searchTableModel);
            searchTable.setRowHeight(40);
            searchTable.setFont(new Font("Arial", Font.PLAIN, 18));
            searchTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 18));
            searchTable.getColumn("Add to Cart").setCellRenderer(new ButtonRenderer());
            searchTable.getColumn("Add to Cart").setCellEditor(new ButtonEditor(searchTable)); // Pass searchTable
            JScrollPane searchScrollPane = new JScrollPane(searchTable);
            searchResultsPanel.add(searchScrollPane, BorderLayout.CENTER);

            for (List<Product> productList : categoryProducts.values()) {
                for (Product product : productList) {
                    if (product.name.toLowerCase().contains(searchText)) {
                        searchTableModel.addRow(new Object[]{product.name, product.price, "Add"});
                    }
                }
            }
            if (searchTableModel.getRowCount() == 0) {
                JOptionPane.showMessageDialog(this, "No products found matching your search.", "No Results", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            mainPanel.add(searchResultsPanel, "Search Results");
            cardLayout.show(mainPanel, "Search Results");
        });

        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        panel.add(searchPanel, BorderLayout.NORTH);

        JPanel centerTitlePanel = new JPanel();
        centerTitlePanel.setBackground(Color.WHITE);
        centerTitlePanel.setLayout(new BoxLayout(centerTitlePanel, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel("Shop for Anything", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 32));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("Buy and sell items from a wide range of categories", JLabel.CENTER);
        subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 18));
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        centerTitlePanel.add(titleLabel);
        centerTitlePanel.add(Box.createRigidArea(new Dimension(0, 10)));
        centerTitlePanel.add(subtitleLabel);
        centerTitlePanel.add(Box.createRigidArea(new Dimension(0, 30)));

        panel.add(centerTitlePanel, BorderLayout.CENTER);

        JPanel categoryPanel = new JPanel(new GridLayout(2, 4, 20, 20));
        categoryPanel.setBackground(Color.WHITE);
        String[] categories = {"Electronics", "Clothing", "Home & Garden", "Sports", "Toys", "Motors", "Collectibles", "Deals"};

        for (String cat : categories) {
            JButton catButton = new JButton(cat);
            catButton.setFocusPainted(false);
            catButton.setBackground(Color.WHITE);
            catButton.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 2));
            catButton.setFont(new Font("Arial", Font.PLAIN, 16));
            catButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            catButton.addActionListener(e -> showProductsForCategory(cat));
            categoryPanel.add(catButton);
        }

        panel.add(categoryPanel, BorderLayout.SOUTH);
        mainPanel.add(sellPage(), "Sell");
        return panel;
    }

    private JPanel auctionPage() {
        JPanel auctionPanel = new JPanel(new BorderLayout());
        auctionPanel.setBackground(Color.WHITE);

        JLabel title = new JLabel("Auction Items", JLabel.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 30));
        title.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        auctionPanel.add(title, BorderLayout.NORTH);

        DefaultTableModel auctionTableModel = new DefaultTableModel(new String[]{"Product", "Current Bid", "Place Bid"}, 0);
        JTable auctionTable = new JTable(auctionTableModel);
        auctionTable.setRowHeight(40);
        auctionTable.setFont(new Font("Arial", Font.PLAIN, 18));
        auctionTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 18));
        auctionTable.getColumn("Place Bid").setCellRenderer(new ButtonRenderer());
        auctionTable.getColumn("Place Bid").setCellEditor(new ButtonEditor(auctionTable, auctionTableModel)); // Pass auctionTable and model
        JScrollPane auctionScrollPane = new JScrollPane(auctionTable);
        auctionPanel.add(auctionScrollPane, BorderLayout.CENTER);

        for (List<Product> productList : categoryProducts.values()) {
            for (Product product : productList) {
                if (product.isAuction) {
                    auctionTableModel.addRow(new Object[]{product.name, product.price, "Bid"});
                }
            }
        }

        return auctionPanel;
    }

    private JPanel sellPage() {
        JPanel sellPanel = new JPanel(new BorderLayout());
        sellPanel.setBackground(Color.WHITE);

        JLabel titleLabel = new JLabel("Sell Your Product", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 32));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        sellPanel.add(titleLabel, BorderLayout.NORTH);

        JPanel formPanel = new JPanel(new GridLayout(5, 2, 10, 10));
        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 50, 20, 50));

        JLabel nameLabel = new JLabel("Product Name:");
        nameLabel.setFont(new Font("Arial", Font.PLAIN, 18));
        JTextField nameField = new JTextField(20);
        nameField.setFont(new Font("Arial", Font.PLAIN, 18));

        JLabel categoryLabel = new JLabel("Category:");
        categoryLabel.setFont(new Font("Arial", Font.PLAIN, 18));
        String[] categories = {"Electronics", "Clothing", "Home & Garden", "Sports"};
        JComboBox<String> categoryComboBox = new JComboBox<>(categories);
        categoryComboBox.setFont(new Font("Arial", Font.PLAIN, 18));

        JLabel priceLabel = new JLabel("Price:");
        priceLabel.setFont(new Font("Arial", Font.PLAIN, 18));
        JTextField priceField = new JTextField(20);
        priceField.setFont(new Font("Arial", Font.PLAIN, 18));

        JLabel descriptionLabel = new JLabel("Description:");
        descriptionLabel.setFont(new Font("Arial", Font.PLAIN, 18));
        JTextArea descriptionArea = new JTextArea(5, 20);
        descriptionArea.setFont(new Font("Arial", Font.PLAIN, 18));
        JScrollPane descriptionScrollPane = new JScrollPane(descriptionArea);

        JLabel isAuctionLabel = new JLabel("Auction Item:");
        isAuctionLabel.setFont(new Font("Arial", Font.PLAIN, 18));
        JCheckBox isAuctionCheckBox = new JCheckBox();
        isAuctionCheckBox.setBackground(Color.WHITE);

        JButton submitButton = new JButton("Submit");
        submitButton.setFont(new Font("Arial", Font.BOLD, 18));
        submitButton.setBackground(new Color(0, 100, 210));
        submitButton.setForeground(Color.WHITE);
        submitButton.setFocusPainted(false);
        submitButton.addActionListener(e -> {
            String name = nameField.getText();
            String category = (String) categoryComboBox.getSelectedItem();
            String price = priceField.getText();
            String description = descriptionArea.getText();
            boolean isAuction = isAuctionCheckBox.isSelected();

            try {
                if (connection != null) {
                    String query = "INSERT INTO products (name, category, price, description, is_auction) VALUES (?, ?, ?, ?, ?)";
                    PreparedStatement preparedStatement = connection.prepareStatement(query);
                    preparedStatement.setString(1, name);
                    preparedStatement.setString(2, category);
                    preparedStatement.setString(3, price);
                    preparedStatement.setString(4, description);
                    preparedStatement.setBoolean(5, isAuction);
                    preparedStatement.executeUpdate();
                    preparedStatement.close();

                    // Add the product to the appropriate section
                    Product newProduct = new Product(name, price, isAuction, -1); // -1 for ID since it's not fetched here
                    List<Product> categoryList = categoryProducts.get(category);
                    if (categoryList == null) {
                        categoryList = new ArrayList<>();
                        categoryProducts.put(category, categoryList);
                    }
                    categoryList.add(newProduct);

                    if (isAuction) {
                        JOptionPane.showMessageDialog(this, "Product added to Auction successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(this, "Product added to Buy section successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    }

                    populateProducts(); // Refresh the product lists
                    nameField.setText("");
                    priceField.setText("");
                    descriptionArea.setText("");
                    isAuctionCheckBox.setSelected(false);

                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error adding product: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        });

        formPanel.add(nameLabel);
        formPanel.add(nameField);
        formPanel.add(categoryLabel);
        formPanel.add(categoryComboBox);
        formPanel.add(priceLabel);
        formPanel.add(priceField);
        formPanel.add(descriptionLabel);
        formPanel.add(descriptionScrollPane);
        formPanel.add(isAuctionLabel);
        formPanel.add(isAuctionCheckBox);

        sellPanel.add(formPanel, BorderLayout.CENTER);
        sellPanel.add(submitButton, BorderLayout.SOUTH);

        return sellPanel;
    }
    private JPanel cartPage() {
        JPanel cartPanel = new JPanel(new BorderLayout());
        cartPanel.setBackground(Color.WHITE);

        JLabel title = new JLabel("Your Cart", JLabel.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 30));
        title.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        cartPanel.add(title, BorderLayout.NORTH);

        DefaultTableModel cartTableModel = new DefaultTableModel(new String[]{"Product", "Price", "Remove"}, 0);
        JTable cartTable = new JTable(cartTableModel);
        cartTable.setRowHeight(40);
        cartTable.setFont(new Font("Arial", Font.PLAIN, 18));
        cartTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 18));
        cartTable.getColumn("Remove").setCellRenderer(new ButtonRenderer());
        cartTable.getColumn("Remove").setCellEditor(new ButtonEditor(cartTable)); // Pass cartTable
        JScrollPane cartScrollPane = new JScrollPane(cartTable);
        cartPanel.add(cartScrollPane, BorderLayout.CENTER);

        for (Product product : cart) {
            cartTableModel.addRow(new Object[]{product.name, product.price, "Remove"});
        }

        return cartPanel;
    }

    private void showProductsForCategory(String category) {
        JPanel categoryPanel = new JPanel(new BorderLayout());
        categoryPanel.setBackground(Color.WHITE);

        JLabel title = new JLabel(category + " Products", JLabel.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 30));
        title.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        categoryPanel.add(title, BorderLayout.NORTH);

        DefaultTableModel categoryTableModel = new DefaultTableModel(new String[]{"Product", "Price", "Add to Cart"}, 0);
        JTable categoryTable = new JTable(categoryTableModel);
        categoryTable.setRowHeight(40);
        categoryTable.setFont(new Font("Arial", Font.PLAIN, 18));
        categoryTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 18));
        categoryTable.getColumn("Add to Cart").setCellRenderer(new ButtonRenderer());
        categoryTable.getColumn("Add to Cart").setCellEditor(new ButtonEditor(categoryTable)); // Pass categoryTable
        JScrollPane categoryScrollPane = new JScrollPane(categoryTable);
        categoryPanel.add(categoryScrollPane, BorderLayout.CENTER);

        List<Product> products = categoryProducts.get(category);
        if (products != null) { //check if the category exists
            for (Product product : products) {
                categoryTableModel.addRow(new Object[]{product.name, product.price, "Add"});
            }
        }

        mainPanel.add(categoryPanel, category);
        cardLayout.show(mainPanel, category);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MarketplaceApp::new);
    }
}

class Product {
    String name;
    String price;
    boolean isAuction;
    int id;

    public Product(String name, String price, boolean isAuction, int id) {
        this.name = name;
        this.price = price;
        this.isAuction = isAuction;
        this.id = id;
    }
     public Product(String name, String price) {
        this.name = name;
        this.price = price;

    }
}

class ButtonRenderer extends JButton implements TableCellRenderer {
    public ButtonRenderer() {
        setOpaque(true);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        setText((value == null) ? "" : value.toString());
        return this;
    }
}

class ButtonEditor extends DefaultCellEditor {
    private JButton button;
    private String label;
    private boolean isPushed;
    private JTable table;
    private MarketplaceApp parent; //reference to the main class
    private DefaultTableModel tableModel; //reference to the table model

    public ButtonEditor(JTable table) {
        super(new JTextField());
        this.table = table;
        button = new JButton();
        button.setOpaque(true);
        this.parent = getAppInstance(); //get the instance
        button.addActionListener(e -> {
            fireEditingStopped();
            if (isPushed) { //make sure the button is clicked
                handleAction();
            }

        });
    }

     public ButtonEditor(JTable table, DefaultTableModel tableModel) { //constructor for auction
        super(new JTextField());
        this.table = table;
        this.tableModel = tableModel;
        button = new JButton();
        button.setOpaque(true);
        this.parent = getAppInstance();
        button.addActionListener(e -> {
            fireEditingStopped();
            if (isPushed) {
                handleAction();
            }
        });
    }

    //method to get the instance of the main class
     private MarketplaceApp getAppInstance() {
        Frame[] frames = JFrame.getFrames();
        for (Frame frame : frames) {
            if (frame instanceof MarketplaceApp) {
                return (MarketplaceApp) frame;
            }
        }
        return null;
    }


    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        label = (value == null) ? "" : value.toString();
        button.setText(label);
        isPushed = true;
        return button;
    }

    @Override
    public Object getCellEditorValue() {
        isPushed = false;
        return label;
    }

    @Override
    public boolean stopCellEditing() {
        isPushed = false;
        return super.stopCellEditing();
    }

    protected void fireEditingStopped() {
        super.fireEditingStopped();
    }

    private void handleAction() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow != -1) {
            String productName = (String) table.getValueAt(selectedRow, 0);
            String price = (String) table.getValueAt(selectedRow, 1);

            if (table.getColumnName(table.getSelectedColumn()).equals("Remove")) {
                // Handle remove from cart
                if (parent != null) {
                    for (int i = 0; i < parent.cart.size(); i++) { // Use parent to access the cart
                        if (parent.cart.get(i).name.equals(productName)) {
                            parent.cart.remove(i);
                            ((DefaultTableModel) table.getModel()).removeRow(selectedRow);
                            JOptionPane.showMessageDialog(null, productName + " removed from cart!");
                            return; // Important: Exit after removing the item
                        }
                    }
                }
            } else if (table.getColumnName(table.getSelectedColumn()).equals("Place Bid")) {
                 // Handle bid action
                String currentBid = price;
                String newBid = JOptionPane.showInputDialog(null,
                        "Enter your bid for " + productName + " (Current Bid: " + currentBid + "):",
                        "Place Bid",
                        JOptionPane.PLAIN_MESSAGE);

                if (newBid != null && !newBid.trim().isEmpty()) {
                    try {
                        double newBidValue = Double.parseDouble(newBid);
                        double currentBidValue = Double.parseDouble(currentBid);

                        if (newBidValue > currentBidValue) {
                            table.setValueAt(String.valueOf(newBidValue), selectedRow, 1); //update the table
                            //update the product price
                            for (List<Product> productList : parent.categoryProducts.values()) {
                                for (Product product : productList) {
                                    if (product.name.equals(productName)) {
                                        product.price = String.valueOf(newBidValue);
                                        break;
                                    }
                                }
                            }
                            JOptionPane.showMessageDialog(null,
                                    "Your bid of " + newBid + " has been placed successfully!",
                                    "Bid Successful",
                                    JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(null,
                                    "Your bid must be higher than the current bid!",
                                    "Invalid Bid",
                                    JOptionPane.WARNING_MESSAGE);
                        }
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(null,
                                "Invalid bid amount. Please enter a valid number.",
                                "Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }

            } else if (table.getColumnName(table.getSelectedColumn()).equals("Add to Cart")) {
                // Handle add to cart
                 if (parent != null) {
                     parent.cart.add(new Product(productName, price));
                     JOptionPane.showMessageDialog(null, productName + " added to cart!");
                 }
            }
        }
    }
}

