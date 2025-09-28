package SchoolManager.UI;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import java.util.ResourceBundle;

public class StaffDashboardController extends DashboardController {
    
    // Staff-specific navigation
    @FXML private JFXButton myClassesBtn;
    @FXML private JFXButton attendanceBtn;
    @FXML private JFXButton gradingBtn;
    @FXML private JFXButton studentsBtn;
    @FXML private JFXButton resourcesBtn;
    @FXML private JFXButton reportsBtn;
    
    private StaffRepoManager staffRepo;
    private Staff staffData;
    
    @Override
    protected void initializeRoleSpecificUI() {
        setupStaffNavigation();
        loadStaffData();
    }
    
    private void setupStaffNavigation() {
        myClassesBtn.setOnAction(e -> showMyClassesView());
        attendanceBtn.setOnAction(e -> showAttendanceView());
        gradingBtn.setOnAction(e -> showGradingView());
        studentsBtn.setOnAction(e -> showStudentsView());
        resourcesBtn.setOnAction(e -> showResourcesView());
        reportsBtn.setOnAction(e -> showReportsView());
    }
    
    private void loadStaffData() {
        if (currentUser != null) {
            staffRepo = adminSystem.getStaffRepoManager();
            staffData = staffRepo.findById(currentUser.getID()).orElse(null);
            updateStaffDashboard();
        }
    }
    
    @Override
    protected void loadDashboardView() {
        VBox dashboard = createStaffDashboard();
        contentArea.getChildren().setAll(dashboard);
    }
    
    private VBox createStaffDashboard() {
        VBox dashboard = new VBox(20);
        dashboard.setPadding(new Insets(20));
        
        // Teaching Overview
        HBox teachingStats = createTeachingStatsPanel();
        
        // Quick Actions
        GridPane quickActions = createQuickActionsPanel();
        
        // Recent Activities
        VBox recentActivities = createStaffActivitiesPanel();
        
        // Upcoming Schedule
        VBox schedulePanel = createSchedulePanel();
        
        dashboard.getChildren().addAll(teachingStats, quickActions, recentActivities, schedulePanel);
        return dashboard;
    }
    
    private HBox createTeachingStatsPanel() {
        HBox statsPanel = new HBox(15);
        statsPanel.setPadding(new Insets(15));
        statsPanel.setStyle("-fx-background-color: white; -fx-background-radius: 10;");
        
        String[][] statsData = {
            {"👨‍🏫", "Active Classes", "4", "Courses teaching this semester"},
            {"📊", "Students Total", "120", "Total students across all classes"},
            {"✅", "Grading Pending", "23", "Assignments to be graded"},
            {"📅", "Next Class", "1 hour", "Time until next class"}
        };
        
        for (String[] stat : statsData) {
            VBox statCard = createStatCard(stat[0], stat[1], stat[2], stat[3]);
            statsPanel.getChildren().add(statCard);
        }
        
        return statsPanel;
    }
    
    private GridPane createQuickActionsPanel() {
        GridPane actionsGrid = new GridPane();
        actionsGrid.setHgap(15);
        actionsGrid.setVgap(15);
        actionsGrid.setPadding(new Insets(15));
        actionsGrid.setStyle("-fx-background-color: white; -fx-background-radius: 10;");
        
        String[][] actions = {
            {"📝", "Take Attendance", "Record today's attendance"},
            {"📚", "Post Grades", "Update student grades"},
            {"💬", "Send Announcement", "Notify your students"},
            {"📋", "Create Assignment", "Add new assignment"},
            {"📊", "View Reports", "Class performance analytics"},
            {"👥", "Student Management", "Manage student records"}
        };
        
        for (int i = 0; i < actions.length; i++) {
            VBox actionCard = createActionCard(actions[i][0], actions[i][1], actions[i][2]);
            actionsGrid.add(actionCard, i % 3, i / 3);
        }
        
        return actionsGrid;
    }
    
    private VBox createActionCard(String icon, String title, String description) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: #e8f5e8; -fx-background-radius: 8; -fx-cursor: hand;");
        card.setPrefSize(180, 100);
        
        card.setOnMouseClicked(e -> handleQuickAction(title));
        
        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 20;");
        
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-weight: bold; -fx-text-alignment: center;");
        
        Label descLabel = new Label(description);
        descLabel.setStyle("-fx-font-size: 10; -fx-text-fill: #666; -fx-text-alignment: center;");
        
        card.getChildren().addAll(iconLabel, titleLabel, descLabel);
        return card;
    }
    
    private void handleQuickAction(String action) {
        switch (action) {
            case "Take Attendance": showAttendanceView(); break;
            case "Post Grades": showGradingView(); break;
            case "Send Announcement": showAnnouncementView(); break;
            case "Create Assignment": showAssignmentCreation(); break;
            case "View Reports": showReportsView(); break;
            case "Student Management": showStudentsView(); break;
        }
    }
    
    @Override
    protected void loadProfileView() {
        VBox profileView = createStaffProfileView();
        contentArea.getChildren().setAll(profileView);
    }
    
    private VBox createStaffProfileView() {
        VBox profile = new VBox(20);
        profile.setPadding(new Insets(20));
        profile.setStyle("-fx-background-color: white; -fx-background-radius: 10;");
        
        Label title = new Label("Staff Profile");
        title.setStyle("-fx-font-size: 24; -fx-font-weight: bold;");
        
        if (staffData != null) {
            GridPane profileGrid = new GridPane();
            profileGrid.setHgap(20);
            profileGrid.setVgap(15);
            
            String[][] profileData = {
                {"Staff ID:", staffData.getStaffId()},
                {"Full Name:", staffData.getName()},
                {"Designation:", staffData.getDesignation().getDisplayName()},
                {"Department:", staffData.getDepartment()},
                {"Email:", currentUser.getUsername() + "@school.edu"},
                {"Phone:", staffData.getPhone()}
            };
            
            for (int i = 0; i < profileData.length; i++) {
                Label fieldLabel = new Label(profileData[i][0]);
                fieldLabel.setStyle("-fx-font-weight: bold;");
                
                Label valueLabel = new Label(profileData[i][1]);
                
                profileGrid.add(fieldLabel, 0, i);
                profileGrid.add(valueLabel, 1, i);
            }
            
            profile.getChildren().addAll(title, profileGrid);
        }
        
        return profile;
    }
    
    // Staff-specific view methods
    private void showMyClassesView() {
        VBox classesView = createClassesView();
        contentArea.getChildren().setAll(classesView);
    }
    
    private VBox createClassesView() {
        VBox view = new VBox(20);
        view.setPadding(new Insets(20));
        
        Label title = new Label("My Classes");
        title.setStyle("-fx-font-size: 24; -fx-font-weight: bold;");
        
        // Implementation for classes view
        return view;
    }
    
    private void showAttendanceView() {
        // Attendance management implementation
    }
    
    private void showGradingView() {
        // Grade management implementation
    }
    
    private void showStudentsView() {
        // Student management implementation
    }
    
    private void showReportsView() {
        // Reports generation implementation
    }
    
    private void showAnnouncementView() {
        // Announcement creation implementation
    }
    
    private void showAssignmentCreation() {
        // Assignment creation implementation
    }
    
    // Helper methods for staff dashboard
    private VBox createStaffActivitiesPanel() {
        // Implementation for staff activities
        return new VBox();
    }
    
    private VBox createSchedulePanel() {
        // Implementation for schedule panel
        return new VBox();
    }
    
    private VBox createStatCard(String icon, String title, String value, String description) {
        // Reuse stat card creation from StudentDashboard
        VBox card = new VBox(10);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: #e3f2fd; -fx-background-radius: 8;");
        card.setPrefSize(200, 100);
        
        // Implementation similar to student version
        return card;
    }
}