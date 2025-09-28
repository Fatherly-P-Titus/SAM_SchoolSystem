package SchoolManager.UI;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import java.util.ResourceBundle;

public class AdminDashboardController extends DashboardController {
    
    // Admin-specific navigation
    @FXML private JFXButton systemHealthBtn;
    @FXML private JFXButton userManagementBtn;
    @FXML private JFXButton securityBtn;
    @FXML private JFXButton backupsBtn;
    @FXML private JFXButton analyticsBtn;
    @FXML private JFXButton complianceBtn;
    
    @Override
    protected void initializeRoleSpecificUI() {
        setupAdminNavigation();
    }
    
    private void setupAdminNavigation() {
        systemHealthBtn.setOnAction(e -> showSystemHealthView());
        userManagementBtn.setOnAction(e -> showUserManagementView());
        securityBtn.setOnAction(e -> showSecurityView());
        backupsBtn.setOnAction(e -> showBackupsView());
        analyticsBtn.setOnAction(e -> showAnalyticsView());
        complianceBtn.setOnAction(e -> showComplianceView());
    }
    
    @Override
    protected void loadDashboardView() {
        VBox dashboard = createAdminDashboard();
        contentArea.getChildren().setAll(dashboard);
    }
    
    private VBox createAdminDashboard() {
        VBox dashboard = new VBox(20);
        dashboard.setPadding(new Insets(20));
        
        // System Overview
        HBox systemStats = createSystemStatsPanel();
        
        // Security Status
        VBox securityStatus = createSecurityStatusPanel();
        
        // Quick Admin Actions
        GridPane adminTools = createAdminToolsPanel();
        
        // System Alerts
        VBox systemAlerts = createSystemAlertsPanel();
        
        dashboard.getChildren().addAll(systemStats, securityStatus, adminTools, systemAlerts);
        return dashboard;
    }
    
    private HBox createSystemStatsPanel() {
        HBox statsPanel = new HBox(15);
        statsPanel.setPadding(new Insets(15));
        statsPanel.setStyle("-fx-background-color: white; -fx-background-radius: 10;");
        
        String[][] statsData = {
            {"🏢", "Total Users", "1,250", "All system users"},
            {"💾", "Database Size", "2.3 GB", "Total data storage"},
            {"🛡️", "Security Score", "98%", "System security rating"},
            {"⏱️", "Uptime", "99.9%", "System availability"}
        };
        
        for (String[] stat : statsData) {
            VBox statCard = createStatCard(stat[0], stat[1], stat[2], stat[3]);
            statsPanel.getChildren().add(statCard);
        }
        
        return statsPanel;
    }
    
    private VBox createSecurityStatusPanel() {
        VBox securityPanel = new VBox(15);
        securityPanel.setPadding(new Insets(15));
        securityPanel.setStyle("-fx-background-color: #fff3e0; -fx-background-radius: 10;");
        
        Label title = new Label("Security Status");
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");
        
        String[][] securityItems = {
            {"✅", "Encryption Active", "All data encrypted at rest"},
            {"✅", "Access Controls", "Role-based permissions enforced"},
            {"⚠️", "Audit Logging", "3 warnings in last 24 hours"},
            {"✅", "Backup System", "Last backup: 2 hours ago"}
        };
        
        VBox securityList = new VBox(8);
        for (String[] item : securityItems) {
            HBox securityItem = createSecurityItem(item[0], item[1], item[2]);
            securityList.getChildren().add(securityItem);
        }
        
        securityPanel.getChildren().addAll(title, securityList);
        return securityPanel;
    }
    
    private HBox createSecurityItem(String status, String text, String details) {
        HBox item = new HBox(10);
        item.setPadding(new Insets(8));
        
        Label statusLabel = new Label(status);
        Label textLabel = new Label(text);
        textLabel.setStyle("-fx-font-weight: bold;");
        
        Label detailsLabel = new Label(details);
        detailsLabel.setStyle("-fx-font-size: 10; -fx-text-fill: #666;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        item.getChildren().addAll(statusLabel, textLabel, spacer, detailsLabel);
        return item;
    }
    
    // Admin-specific view methods
    private void showSystemHealthView() {
        // System monitoring implementation
    }
    
    private void showUserManagementView() {
        // User administration implementation
    }
    
    private void showSecurityView() {
        // Security configuration implementation
    }
    
    private void showBackupsView() {
        // Backup management implementation
    }
    
    private void showAnalyticsView() {
        // System analytics implementation
    }
    
    private void showComplianceView() {
        // Compliance reporting implementation
    }
    
    // Helper methods
    private GridPane createAdminToolsPanel() {
        return new GridPane();
    }
    
    private VBox createSystemAlertsPanel() {
        return new VBox();
    }
    
    @Override
    protected void loadProfileView() {
        VBox profileView = createAdminProfileView();
        contentArea.getChildren().setAll(profileView);
    }
}