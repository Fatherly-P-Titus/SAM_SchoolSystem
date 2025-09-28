package SchoolManager.UI;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public class SceneManager {
    
    public static void loadRoleSpecificDashboard(User user, ADMIN adminSystem) {
        try {
            String fxmlFile = "";
            Object controller = null;
            
            switch (user.getRole().toUpperCase()) {
                case "STUDENT":
                    fxmlFile = "/fxml/student-dashboard.fxml";
                    controller = new StudentDashboardController();
                    break;
                case "STAFF":
                case "TEACHER":
                    fxmlFile = "/fxml/staff-dashboard.fxml";
                    controller = new StaffDashboardController();
                    break;
                case "REGISTRAR":
                    fxmlFile = "/fxml/registrar-dashboard.fxml";
                    controller = new RegistrarDashboardController();
                    break;
                case "ADMIN":
                    fxmlFile = "/fxml/admin-dashboard.fxml";
                    controller = new AdminDashboardController();
                    break;
                default:
                    throw new IllegalArgumentException("Unknown user role: " + user.getRole());
            }
            
            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource(fxmlFile));
            loader.setController(controller);
            Pane root = loader.load();
            
            DashboardController dashboardController = loader.getController();
            dashboardController.setAdminSystem(adminSystem);
            dashboardController.setCurrentUser(user);
            
            Scene scene = new Scene(root);
            scene.getStylesheets().add("/css/dashboard.css");
            
            Stage stage = new Stage();
            stage.setScene(scene);
            stage.setTitle("S.A.M. Systems - " + user.getRole() + " Dashboard");
            stage.setMaximized(true);
            stage.show();
            
        } catch (Exception e) {
            e.printStackTrace();
            // Show error dialog
        }
    }
    
    public static void loadLoginScene() {
        // Implementation to return to login screen
    }
}