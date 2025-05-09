import com.mongodb.client.*;
import org.bson.Document;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class MarketplaceApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(LoginFrame::new);
    }
}

// --- LOGIN WINDOW ---
class LoginFrame extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;

    public LoginFrame() {
        setTitle("Login - Marketplace");
        setSize(350, 220);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new GridLayout(5, 1, 10, 10));

        usernameField = new JTextField();
        passwordField = new JPasswordField();
        JButton loginButton = new JButton("Login");
        JButton registerButton = new JButton("Register");

        add(new JLabel("Username:"));
        add(usernameField);
        add(new JLabel("Password:"));
        add(passwordField);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(loginButton);
        buttonPanel.add(registerButton);
        add(buttonPanel);

        loginButton.addActionListener(e -> {
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());

            if (authenticate(username, password)) {
                dispose();
                new MarketplaceWithMongo().setVisible(true);
            } else {
                JOptionPane.showMessageDialog(this, "Invalid credentials!", "Login Failed", JOptionPane.ERROR_MESSAGE);
            }
        });

        registerButton.addActionListener(e -> new RegisterFrame());

        setVisible(true);
    }

    private boolean authenticate(String username, String password) {
        try (MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017")) {
            MongoDatabase db = mongoClient.getDatabase("marketplace");
            MongoCollection<Document> users = db.getCollection("users");

            Document user = users.find(new Document("username", username).append("password", password)).first();
            return user != null;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}

// --- REGISTER WINDOW ---
class RegisterFrame extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;

    public RegisterFrame() {
        setTitle("Register - Marketplace");
        setSize(350, 220);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new GridLayout(5, 1, 10, 10));

        usernameField = new JTextField();
        passwordField = new JPasswordField();
        JButton registerButton = new JButton("Register");

        add(new JLabel("New Username:"));
        add(usernameField);
        add(new JLabel("New Password:"));
        add(passwordField);
        add(registerButton);

        registerButton.addActionListener(e -> {
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());

            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Fields cannot be empty.");
                return;
            }

            try (MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017")) {
                MongoDatabase db = mongoClient.getDatabase("marketplace");
                MongoCollection<Document> users = db.getCollection("users");

                Document existing = users.find(new Document("username", username)).first();
                if (existing != null) {
                    JOptionPane.showMessageDialog(this, "Username already exists.");
                    return;
                }

                users.insertOne(new Document("username", username).append("password", password));
                JOptionPane.showMessageDialog(this, "Registered successfully! Please login.");
                dispose();
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error registering user.");
            }
        });

        setVisible(true);
    }
}

// --- MARKETPLACE GUI ---
class MarketplaceWithMongo extends JFrame {
    private JPanel categoryPanel, productPanel;
    private ArrayList<Product> cart;
    private MongoDatabase database;
    private MongoCollection<Document> productCollection;
    private JTextField searchField;
    private JButton searchButton;

    private final Color PRIMARY = new Color(0x0074D9);
    private final Color BACKGROUND = new Color(0xF0F2F5);
    private final Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 14);
    private final Font FONT_NORMAL = new Font("Segoe UI", Font.PLAIN, 13);

    public MarketplaceWithMongo() {
        setTitle("Marketplace");
        setSize(1100, 750);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        getContentPane().setBackground(BACKGROUND);
        setLayout(new BorderLayout());

        cart = new ArrayList<>();

        MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017");
        database = mongoClient.getDatabase("marketplace");
        productCollection = database.getCollection("products");

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(BACKGROUND);
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        searchField = new JTextField(25);
        searchField.setFont(FONT_NORMAL);
        searchButton = createStyledButton("🔍 Search");
        searchButton.addActionListener(this::performSearch);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.setBackground(BACKGROUND);
        searchPanel.add(searchField);
        searchPanel.add(searchButton);

        categoryPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        categoryPanel.setBackground(BACKGROUND);
        categoryPanel.setBorder(BorderFactory.createTitledBorder("Categories"));

        topPanel.add(searchPanel, BorderLayout.NORTH);
        topPanel.add(categoryPanel, BorderLayout.CENTER);
        add(topPanel, BorderLayout.NORTH);

        productPanel = new JPanel(new GridLayout(0, 3, 20, 20));
        productPanel.setBackground(BACKGROUND);
        JScrollPane scrollPane = new JScrollPane(productPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);

        JButton cartButton = createStyledButton("🛒 View Cart");
        cartButton.setPreferredSize(new Dimension(160, 40));
        cartButton.addActionListener(this::showCart);
        add(cartButton, BorderLayout.SOUTH);

        loadCategories();
    }

    private void loadCategories() {
        Set<String> categories = new HashSet<>();
        for (Document doc : productCollection.find()) {
            String category = doc.getString("category");
            if (category != null) categories.add(category);
        }

        categoryPanel.removeAll();
        for (String category : categories) {
            JButton catButton = createStyledButton(category);
            catButton.setBackground(new Color(0xFFDC00));
            catButton.addActionListener(e -> showProductsByCategory(category));
            categoryPanel.add(catButton);
        }

        revalidate();
        repaint();
    }

    private void showProductsByCategory(String category) {
        productPanel.removeAll();
        for (Document doc : productCollection.find(new Document("category", category))) {
            String name = doc.getString("name");
            String priceStr = getPriceAsString(doc);
            productPanel.add(createProductCard(name, priceStr));
        }
        revalidate();
        repaint();
    }

    private void performSearch(ActionEvent e) {
        String keyword = searchField.getText().trim();
        if (keyword.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a product name to search.");
            return;
        }

        productPanel.removeAll();
        FindIterable<Document> results = productCollection.find(
                new Document("name", new Document("$regex", keyword).append("$options", "i")));

        boolean found = false;
        for (Document doc : results) {
            found = true;
            String name = doc.getString("name");
            String priceStr = getPriceAsString(doc);
            productPanel.add(createProductCard(name, priceStr));
        }

        if (!found) {
            productPanel.add(new JLabel("No products found for: " + keyword));
        }

        revalidate();
        repaint();
    }

    private JPanel createProductCard(String name, String priceStr) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));

        JLabel nameLabel = new JLabel(name, SwingConstants.CENTER);
        nameLabel.setFont(FONT_BOLD);
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel priceLabel = new JLabel("₹" + priceStr);
        priceLabel.setFont(FONT_NORMAL);
        priceLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel imageLabel = new JLabel("[Image]");
        imageLabel.setFont(FONT_NORMAL);
        imageLabel.setForeground(Color.GRAY);
        imageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton addButton = createStyledButton("Add to Cart");
        addButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        addButton.addActionListener(e -> {
            cart.add(new Product(name, priceStr));
            JOptionPane.showMessageDialog(this, name + " added to cart.");
        });

        card.add(imageLabel);
        card.add(Box.createRigidArea(new Dimension(0, 8)));
        card.add(nameLabel);
        card.add(Box.createRigidArea(new Dimension(0, 5)));
        card.add(priceLabel);
        card.add(Box.createRigidArea(new Dimension(0, 8)));
        card.add(addButton);

        return card;
    }

    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setBackground(PRIMARY);
        button.setForeground(Color.WHITE);
        button.setFont(FONT_BOLD);
        button.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                button.setBackground(PRIMARY.darker());
            }

            public void mouseExited(MouseEvent e) {
                button.setBackground(PRIMARY);
            }
        });

        return button;
    }

    private String getPriceAsString(Document doc) {
        Object priceObj = doc.get("price");
        if (priceObj instanceof Number) {
            return String.format("%.2f", ((Number) priceObj).doubleValue());
        } else if (priceObj instanceof String) {
            return (String) priceObj;
        }
        return "0.00";
    }

    private void showCart(ActionEvent e) {
        JFrame cartFrame = new JFrame("🛒 Your Cart");
        cartFrame.setSize(400, 300);
        cartFrame.setLayout(new BorderLayout());

        JTextArea cartArea = new JTextArea();
        cartArea.setEditable(false);
        cartArea.setFont(FONT_NORMAL);
        double total = 0;

        for (Product p : cart) {
            cartArea.append(p.name + " - ₹" + p.price + "\n");
            try {
                total += Double.parseDouble(p.price.replaceAll("[^0-9.]", ""));
            } catch (NumberFormatException ignored) {}
        }

        cartArea.append("\n\nTotal: ₹" + String.format("%.2f", total));

        JPanel buttonPanel = new JPanel();
        JButton placeOrderButton = createStyledButton("✔ Place Order");
        placeOrderButton.addActionListener(e1 -> {
            JOptionPane.showMessageDialog(this, "Order placed successfully!");
            cart.clear();
            cartFrame.dispose();
        });

        buttonPanel.add(placeOrderButton);
        cartFrame.add(new JScrollPane(cartArea), BorderLayout.CENTER);
        cartFrame.add(buttonPanel, BorderLayout.SOUTH);

        cartFrame.setVisible(true);
    }

    static class Product {
        String name, price;

        Product(String name, String price) {
            this.name = name;
            this.price = price;
        }
    }
}
