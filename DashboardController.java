package SchoolManager.UI;

import com.jfoenix.controls.*;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import java.net.URL;
import java.util.ResourceBundle;

public abstract class DashboardController implements Initializable {
    
    @Fprotected ADMIN adminSystem;
    protected User currentUser;
    
    // Common UI Components
    @FXML protected BorderPane rootPane;
    @FXML protected Label welcomeLabel;
    @FXML protected Label roleLabel;
    @FXML protected Label timeLabel;
    @FXML protected StackPane contentArea;
    @FXML protected VBox sidebar;
    
    // Navigation items common to all roles
    @FXML protected JFXButton dashboardBtn;
    @FXML protected JFXButton profileBtn;
    @FXML protected JFXButton logoutBtn;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupCommonUI();
        initializeRoleSpecificUI();
        loadDashboardView();
    }
    
    public void setAdminSystem(ADMIN adminSystem) {
        this.adminSystem = adminSystem;
    }
    
    public void setCurrentUser(User user) {
        this.currentUser = user;
        updateUserInfo();
    }
    
    private void setupCommonUI() {
        // Setup common styling and event handlers
        setupNavigationHandlers();
        setupTimeUpdates();
    }
    
    private void setupNavigationHandlers() {
        dashboardBtn.setOnAction(e -> loadDashboardView());
        profileBtn.setOnAction(e -> loadProfileView());
        logoutBtn.setOnAction(e -> handleLogout());
    }
    
    private void updateUserInfo() {
        if (currentUser != null) {
            welcomeLabel.setText("Welcome, " + currentUser.getUsername());
            roleLabel.setText("Role: " + currentUser.getRole());
        }
    }
    
    private void setupTimeUpdates() {
        // Real-time clock updates
        // Implementation for time label updates
    }
    
    @FXML
    private void handleLogout() {
        // Secure logout procedure
        adminSystem.shutdown();
        // Return to login screen
        SceneManager.loadLoginScene();
    }
    
    // Abstract methods for role-specific implementation
    protected abstract void initializeRoleSpecificUI();
    protected abstract void loadDashboardView();
    protected abstract void loadProfileView();
}