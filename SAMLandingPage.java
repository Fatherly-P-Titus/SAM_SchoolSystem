package SchoolManager.UI;

import com.jfoenix.controls.*;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.animation.FadeTransition;
import javafx.util.Duration;

public class SAMLandingPage extends Application {
    
    private BorderPane root;
    private StackPane contentPane;
    private JFXDrawer drawer;
    private VBox navDrawer;
    
    @Override
    public void start(Stage primaryStage) {
        initializePrimaryStage(primaryStage);
        setupUIComponents();
        showLandingPage();
    }
    
    private void initializePrimaryStage(Stage stage) {
        stage.setTitle("S.A.M. Systems - Secure Administration & Management");
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/images/logo.png")));
        stage.setMinWidth(1200);
        stage.setMinHeight(800);
    }
    
    private void setupUIComponents() {
        root = new BorderPane();
        root.getStyleClass().add("root-pane");
        
        // Create navigation drawer
        createNavigationDrawer();
        
        // Create content area
        contentPane = new StackPane();
        contentPane.getStyleClass().add("content-pane");
        
        // Setup drawer functionality
        drawer = new JFXDrawer();
        drawer.setSidePane(navDrawer);
        drawer.setDefaultDrawerSize(280);
        drawer.setOverLayVisible(false);
        drawer.setResizableOnDrag(true);
        
        StackPane mainContainer = new StackPane();
        mainContainer.getChildren().addAll(contentPane, drawer);
        
        root.setCenter(mainContainer);
        
        Scene scene = new Scene(root, 1400, 900);
        scene.getStylesheets().add(getClass().getResource("/css/sam-styles.css").toExternalForm());
        stage.setScene(scene);
    }
    
    private void createNavigationDrawer() {
        navDrawer = new VBox(20);
        navDrawer.getStyleClass().add("nav-drawer");
        navDrawer.setPadding(new Insets(40, 20, 40, 20));
        navDrawer.setAlignment(Pos.TOP_LEFT);
        
        // Logo and title
        HBox logoSection = new HBox(15);
        logoSection.setAlignment(Pos.CENTER_LEFT);
        
        ImageView logo = new ImageView(new Image(getClass().getResourceAsStream("/images/logo-small.png")));
        logo.setFitHeight(40);
        logo.setFitWidth(40);
        
        Label title = new Label("S.A.M. Systems");
        title.getStyleClass().add("nav-title");
        title.setFont(Font.font("Roboto", FontWeight.BOLD, 20));
        
        logoSection.getChildren().addAll(logo, title);
        
        // Navigation items
        VBox navItems = new VBox(10);
        
        String[] menuItems = {"Home", "Login", "Sign Up", "About", "Vision", "Mission", "Contact"};
        for (String item : menuItems) {
            JFXButton navButton = new JFXButton(item);
            navButton.getStyleClass().add("nav-button");
            navButton.setPrefWidth(240);
            navButton.setAlignment(Pos.CENTER_LEFT);
            navButton.setOnAction(e -> handleNavigation(item));
            navItems.getChildren().add(navButton);
        }
        
        navDrawer.getChildren().addAll(logoSection, createSeparator(), navItems);
    }
    
    private Node createSeparator() {
        Region separator = new Region();
        separator.setPrefHeight(1);
        separator.setStyle("-fx-background-color: derive(-fx-primary, -20%);");
        return separator;
    }
    
    private void handleNavigation(String item) {
        drawer.close();
        switch (item) {
            case "Home": showLandingPage(); break;
            case "Login": showLoginSection(); break;
            case "Sign Up": showSignupSection(); break;
            case "About": showAboutSection(); break;
            case "Vision": showVisionSection(); break;
            case "Mission": showMissionSection(); break;
            case "Contact": showFooterSection(); break;
        }
    }
    
    private void showLandingPage() {
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        
        VBox landingContent = new VBox();
        landingContent.getStyleClass().add("landing-content");
        
        // Hero Section
        landingContent.getChildren().add(createHeroSection());
        
        // Features Overview
        landingContent.getChildren().add(createFeaturesSection());
        
        // Quick Access Cards
        landingContent.getChildren().add(createQuickAccessSection());
        
        scrollPane.setContent(landingContent);
        applyFadeTransition(scrollPane);
    }
    
    private Node createHeroSection() {
        VBox heroSection = new VBox(30);
        heroSection.getStyleClass().add("hero-section");
        heroSection.setAlignment(Pos.CENTER);
        heroSection.setPadding(new Insets(80, 20, 60, 20));
        
        // Main Title
        Label mainTitle = new Label("Secure Administration & Management System");
        mainTitle.getStyleClass().add("hero-title");
        mainTitle.setWrapText(true);
        mainTitle.setAlignment(Pos.CENTER);
        
        // Subtitle
        Label subtitle = new Label("Enterprise-Grade Educational Management Platform");
        subtitle.getStyleClass().add("hero-subtitle");
        subtitle.setWrapText(true);
        subtitle.setAlignment(Pos.CENTER);
        
        // CTA Buttons
        HBox ctaButtons = new HBox(20);
        ctaButtons.setAlignment(Pos.CENTER);
        
        JFXButton loginBtn = new JFXButton("Get Started");
        loginBtn.getStyleClass().addAll("btn", "btn-primary", "btn-large");
        loginBtn.setOnAction(e -> showLoginSection());
        
        JFXButton learnMoreBtn = new JFXButton("Learn More");
        learnMoreBtn.getStyleClass().addAll("btn", "btn-outline", "btn-large");
        learnMoreBtn.setOnAction(e -> showAboutSection());
        
        ctaButtons.getChildren().addAll(loginBtn, learnMoreBtn);
        
        heroSection.getChildren().addAll(mainTitle, subtitle, ctaButtons);
        return heroSection;
    }
    
    private Node createFeaturesSection() {
        VBox featuresSection = new VBox(40);
        featuresSection.getStyleClass().add("features-section");
        featuresSection.setAlignment(Pos.CENTER);
        featuresSection.setPadding(new Insets(60, 40, 80, 40));
        
        Label sectionTitle = new Label("Why Choose S.A.M. Systems?");
        sectionTitle.getStyleClass().add("section-title");
        
        GridPane featuresGrid = new GridPane();
        featuresGrid.setHgap(30);
        featuresGrid.setVgap(30);
        featuresGrid.setAlignment(Pos.CENTER);
        
        String[][] features = {
            {"🔒", "Military-Grade Security", "AES-256 encryption, FERPA/GDPR compliance"},
            {"📊", "Real-Time Analytics", "Comprehensive dashboards and reporting"},
            {"👥", "Role-Based Access", "Fine-grained permissions for all user types"},
            {"💰", "Financial Management", "Budget tracking and expense approval workflows"},
            {"📚", "Academic Management", "Complete student lifecycle management"},
            {"🛡️", "Audit & Compliance", "Full audit trails and legal compliance"}
        };
        
        for (int i = 0; i < features.length; i++) {
            VBox featureCard = createFeatureCard(features[i][0], features[i][1], features[i][2]);
            featuresGrid.add(featureCard, i % 3, i / 3);
        }
        
        featuresSection.getChildren().addAll(sectionTitle, featuresGrid);
        return featuresSection;
    }
    
    private VBox createFeatureCard(String icon, String title, String description) {
        VBox card = new VBox(15);
        card.getStyleClass().add("feature-card");
        card.setAlignment(Pos.TOP_CENTER);
        card.setPadding(new Insets(30, 20, 30, 20));
        card.setPrefSize(280, 200);
        
        Label iconLabel = new Label(icon);
        iconLabel.getStyleClass().add("feature-icon");
        iconLabel.setFont(Font.font(36));
        
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("feature-title");
        titleLabel.setFont(Font.font("Roboto", FontWeight.BOLD, 16));
        titleLabel.setWrapText(true);
        
        Text descText = new Text(description);
        descText.getStyleClass().add("feature-desc");
        descText.setWrappingWidth(240);
        descText.setTextAlignment(TextAlignment.CENTER);
        
        card.getChildren().addAll(iconLabel, titleLabel, descText);
        return card;
    }
    
    private Node createQuickAccessSection() {
        VBox quickAccessSection = new VBox(30);
        quickAccessSection.getStyleClass().add("quick-access-section");
        quickAccessSection.setAlignment(Pos.CENTER);
        quickAccessSection.setPadding(new Insets(40, 20, 80, 20));
        
        Label sectionTitle = new Label("Quick Access");
        sectionTitle.getStyleClass().add("section-title");
        
        HBox cardsContainer = new HBox(30);
        cardsContainer.setAlignment(Pos.CENTER);
        
        // Login Card
        VBox loginCard = createQuickAccessCard("🚀", "User Login", "Access your secure dashboard", 
            "Login", e -> showLoginSection());
        
        // Signup Card
        VBox signupCard = createQuickAccessCard("👥", "New User", "Create your institution account", 
            "Sign Up", e -> showSignupSection());
        
        // About Card
        VBox aboutCard = createQuickAccessCard("ℹ️", "About System", "Learn about our platform", 
            "Learn More", e -> showAboutSection());
        
        cardsContainer.getChildren().addAll(loginCard, signupCard, aboutCard);
        quickAccessSection.getChildren().addAll(sectionTitle, cardsContainer);
        
        return quickAccessSection;
    }
    
    private VBox createQuickAccessCard(String icon, String title, String desc, String buttonText, 
                                     EventHandler<ActionEvent> handler) {
        VBox card = new VBox(20);
        card.getStyleClass().add("quick-access-card");
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(30, 25, 30, 25));
        card.setPrefSize(280, 220);
        
        Label iconLabel = new Label(icon);
        iconLabel.getStyleClass().add("quick-access-icon");
        iconLabel.setFont(Font.font(42));
        
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("quick-access-title");
        titleLabel.setFont(Font.font("Roboto", FontWeight.BOLD, 18));
        
        Text descText = new Text(desc);
        descText.getStyleClass().add("quick-access-desc");
        descText.setWrappingWidth(220);
        descText.setTextAlignment(TextAlignment.CENTER);
        
        JFXButton actionBtn = new JFXButton(buttonText);
        actionBtn.getStyleClass().addAll("btn", "btn-outline");
        actionBtn.setOnAction(handler);
        
        card.getChildren().addAll(iconLabel, titleLabel, descText, actionBtn);
        return card;
    }
    
    // ==================== LOGIN SECTION ====================
    
    private void showLoginSection() {
        VBox loginSection = new VBox(30);
        loginSection.getStyleClass().add("form-section");
        loginSection.setAlignment(Pos.CENTER);
        loginSection.setPadding(new Insets(60, 40, 80, 40));
        
        Label title = new Label("Secure Login");
        title.getStyleClass().add("form-title");
        
        GridPane loginForm = new GridPane();
        loginForm.setHgap(20);
        loginForm.setVgap(20);
        loginForm.setAlignment(Pos.CENTER);
        
        // Username Field
        Label userLabel = new Label("Username:");
        userLabel.getStyleClass().add("form-label");
        JFXTextField usernameField = new JFXTextField();
        usernameField.setPromptText("Enter your username");
        usernameField.setPrefWidth(300);
        
        // Password Field
        Label passLabel = new Label("Password:");
        passLabel.getStyleClass().add("form-label");
        JFXPasswordField passwordField = new JFXPasswordField();
        passwordField.setPromptText("Enter your password");
        passwordField.setPrefWidth(300);
        
        // Login Button
        JFXButton loginBtn = new JFXButton("Login to System");
        loginBtn.getStyleClass().addAll("btn", "btn-primary", "btn-large");
        loginBtn.setPrefWidth(300);
        
        // Forgot Password
        JFXButton forgotBtn = new JFXButton("Forgot Password?");
        forgotBtn.getStyleClass().add("link-button");
        
        loginForm.add(userLabel, 0, 0);
        loginForm.add(usernameField, 0, 1);
        loginForm.add(passLabel, 0, 2);
        loginForm.add(passwordField, 0, 3);
        loginForm.add(loginBtn, 0, 4);
        loginForm.add(forgotBtn, 0, 5);
        
        GridPane.setHalignment(forgotBtn, HPos.CENTER);
        
        loginSection.getChildren().addAll(title, loginForm);
        applyFadeTransition(loginSection);
    }
    
    // ==================== SIGNUP SECTION ====================
    
    private void showSignupSection() {
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        
        VBox signupSection = new VBox(30);
        signupSection.getStyleClass().add("form-section");
        signupSection.setAlignment(Pos.CENTER);
        signupSection.setPadding(new Insets(40, 20, 60, 20));
        
        Label title = new Label("Create Institution Account");
        title.getStyleClass().add("form-title");
        
        GridPane signupForm = createSignupForm();
        
        signupSection.getChildren().addAll(title, signupForm);
        scrollPane.setContent(signupSection);
        applyFadeTransition(scrollPane);
    }
    
    private GridPane createSignupForm() {
        GridPane form = new GridPane();
        form.setHgap(20);
        form.setVgap(15);
        form.setAlignment(Pos.CENTER);
        form.setPadding(new Insets(20));
        
        String[][] fields = {
            {"Institution Name", "text", "Enter institution name"},
            {"Email Address", "email", "Enter admin email"},
            {"Phone Number", "tel", "Enter contact number"},
            {"Address", "text", "Enter institution address"},
            {"Admin Username", "text", "Choose admin username"},
            {"Admin Password", "password", "Create secure password"},
            {"Confirm Password", "password", "Re-enter password"},
            {"Institution Type", "combo", "Select type"}
        };
        
        for (int i = 0; i < fields.length; i++) {
            Label label = new Label(fields[i][0] + ":");
            label.getStyleClass().add("form-label");
            
            Node field;
            if (fields[i][1].equals("combo")) {
                JFXComboBox<String> combo = new JFXComboBox<>();
                combo.getItems().addAll("K-12 School", "College/University", "Training Center", "Other");
                combo.setPromptText(fields[i][2]);
                combo.setPrefWidth(350);
                field = combo;
            } else {
                JFXTextField textField = fields[i][1].equals("password") ? 
                    new JFXPasswordField() : new JFXTextField();
                textField.setPromptText(fields[i][2]);
                textField.setPrefWidth(350);
                field = textField;
            }
            
            form.add(label, 0, i);
            form.add(field, 1, i);
        }
        
        // Submit Button
        JFXButton submitBtn = new JFXButton("Create Institution Account");
        submitBtn.getStyleClass().addAll("btn", "btn-primary", "btn-large");
        submitBtn.setPrefWidth(350);
        form.add(submitBtn, 1, fields.length);
        
        return form;
    }
    
    // ==================== ABOUT SECTION ====================
    
    private void showAboutSection() {
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        
        VBox aboutSection = new VBox(40);
        aboutSection.getStyleClass().add("content-section");
        aboutSection.setPadding(new Insets(40, 60, 60, 60));
        
        Label title = new Label("About S.A.M. Systems");
        title.getStyleClass().add("content-title");
        
        VBox content = new VBox(25);
        content.getStyleClass().add("about-content");
        
        String[] aboutPoints = {
            "S.A.M. Systems (Secure Administration & Management) is an enterprise-grade educational management platform designed specifically for modern educational institutions.",
            
            "Our platform combines military-grade security with intuitive user experiences, providing comprehensive solutions for student management, staff administration, financial operations, and institutional analytics.",
            
            "Built with compliance at its core, S.A.M. Systems ensures adherence to FERPA, GDPR, and other international data protection standards while delivering powerful features that streamline educational operations.",
            
            "With real-time analytics, role-based access controls, and automated workflow management, institutions can focus on education while we handle the complex administrative challenges."
        };
        
        for (String point : aboutPoints) {
            Text paragraph = new Text(point);
            paragraph.getStyleClass().add("content-paragraph");
            paragraph.setWrappingWidth(800);
            content.getChildren().add(paragraph);
        }
        
        aboutSection.getChildren().addAll(title, content);
        scrollPane.setContent(aboutSection);
        applyFadeTransition(scrollPane);
    }
    
    // ==================== VISION SECTION ====================
    
    private void showVisionSection() {
        VBox visionSection = new VBox(40);
        visionSection.getStyleClass().add("content-section");
        visionSection.setAlignment(Pos.CENTER);
        visionSection.setPadding(new Insets(80, 40, 80, 40));
        
        Label title = new Label("Our Vision");
        title.getStyleClass().add("content-title");
        
        VBox visionContent = new VBox(30);
        visionContent.setAlignment(Pos.CENTER);
        visionContent.setMaxWidth(800);
        
        Label visionStatement = new Label("\"To revolutionize educational administration through secure, intelligent, and accessible technology solutions that empower institutions to focus on what matters most: quality education.\"");
        visionStatement.getStyleClass().add("vision-statement");
        visionStatement.setWrapText(true);
        visionStatement.setTextAlignment(TextAlignment.CENTER);
        
        VBox visionPoints = new VBox(20);
        visionPoints.setAlignment(Pos.CENTER_LEFT);
        
        String[] points = {
            "• Global standard for educational management systems",
            "• Zero-compromise security with maximum usability",
            "• AI-driven insights for institutional improvement",
            "• Seamless integration with emerging educational technologies",
            "• Accessible to institutions of all sizes worldwide"
        };
        
        for (String point : points) {
            Label pointLabel = new Label(point);
            pointLabel.getStyleClass().add("vision-point");
            pointLabel.setWrapText(true);
            visionPoints.getChildren().add(pointLabel);
        }
        
        visionContent.getChildren().addAll(visionStatement, visionPoints);
        visionSection.getChildren().addAll(title, visionContent);
        applyFadeTransition(visionSection);
    }
    
    // ==================== MISSION SECTION ====================
    
    private void showMissionSection() {
        VBox missionSection = new VBox(40);
        missionSection.getStyleClass().add("content-section");
        missionSection.setAlignment(Pos.CENTER);
        missionSection.setPadding(new Insets(80, 40, 80, 40));
        
        Label title = new Label("Our Mission");
        title.getStyleClass().add("content-title");
        
        GridPane missionGrid = new GridPane();
        missionGrid.setHgap(40);
        missionGrid.setVgap(40);
        missionGrid.setAlignment(Pos.CENTER);
        
        String[][] missions = {
            {"🔒", "Security First", "Implement uncompromising security measures to protect sensitive educational data."},
            {"🎯", "User-Centric Design", "Create intuitive interfaces that reduce administrative burden."},
            {"📈", "Data-Driven Insights", "Provide actionable analytics for institutional improvement."},
            {"🌍", "Global Accessibility", "Ensure platform accessibility across diverse educational systems."},
            {"⚡", "Performance Excellence", "Deliver lightning-fast responses and 99.9% uptime."},
            {"🤝", "Partnership Approach", "Work collaboratively with institutions for continuous improvement."}
        };
        
        for (int i = 0; i < missions.length; i++) {
            VBox missionCard = createMissionCard(missions[i][0], missions[i][1], missions[i][2]);
            missionGrid.add(missionCard, i % 2, i / 2);
        }
        
        missionSection.getChildren().addAll(title, missionGrid);
        applyFadeTransition(missionSection);
    }
    
    private VBox createMissionCard(String icon, String title, String description) {
        VBox card = new VBox(15);
        card.getStyleClass().add("mission-card");
        card.setAlignment(Pos.TOP_CENTER);
        card.setPadding(new Insets(30, 25, 30, 25));
        card.setPrefSize(350, 200);
        
        Label iconLabel = new Label(icon);
        iconLabel.getStyleClass().add("mission-icon");
        iconLabel.setFont(Font.font(36));
        
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("mission-title");
        titleLabel.setFont(Font.font("Roboto", FontWeight.BOLD, 18));
        
        Text descText = new Text(description);
        descText.getStyleClass().add("mission-desc");
        descText.setWrappingWidth(300);
        descText.setTextAlignment(TextAlignment.CENTER);
        
        card.getChildren().addAll(iconLabel, titleLabel, descText);
        return card;
    }
    
    // ==================== FOOTER SECTION ====================
    
    private void showFooterSection() {
        VBox footerSection = new VBox();
        footerSection.getStyleClass().add("footer-section");
        
        VBox footerContent = new VBox(30);
        footerContent.setAlignment(Pos.CENTER);
        footerContent.setPadding(new Insets(40, 20, 30, 20));
        
        // Contact Information
        VBox contactInfo = new VBox(15);
        contactInfo.setAlignment(Pos.CENTER);
        
        Label contactTitle = new Label("Contact Information");
        contactTitle.getStyleClass().add("footer-title");
        
        String[] contacts = {
            "📧 support@sam-systems.edu",
            "📞 +1 (555) SAM-HELP (726-4357)",
            "🏢 123 Education Plaza, Tech City, TC 12345",
            "🌐 www.sam-systems.edu"
        };
        
        for (String contact : contacts) {
            Label contactLabel = new Label(contact);
            contactLabel.getStyleClass().add("footer-text");
            contactInfo.getChildren().add(contactLabel);
        }
        
        // Social Links
        HBox socialLinks = new HBox(20);
        socialLinks.setAlignment(Pos.CENTER);
        
        String[] socials = {"LinkedIn", "Twitter", "GitHub", "YouTube"};
        for (String social : socials) {
            JFXButton socialBtn = new JFXButton(social);
            socialBtn.getStyleClass().add("social-button");
            socialLinks.getChildren().add(socialBtn);
        }
        
        // Copyright
        Label copyright = new Label("© 2024 S.A.M. Systems. All rights reserved. | FERPA & GDPR Compliant");
        copyright.getStyleClass().add("copyright-text");
        
        footerContent.getChildren().addAll(contactTitle, contactInfo, socialLinks, copyright);
        footerSection.getChildren().add(footerContent);
        
        applyFadeTransition(footerSection);
    }
    
    private void applyFadeTransition(Node node) {
        FadeTransition ft = new FadeTransition(Duration.millis(500), node);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        contentPane.getChildren().setAll(node);
        ft.play();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}









