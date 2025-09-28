package SchoolManager.UI;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import java.util.ResourceBundle;

public class StudentDashboardController extends DashboardController {
    
    // Student-specific navigation
    @FXML private JFXButton coursesBtn;
    @FXML private JFXButton gradesBtn;
    @FXML private JFXButton scheduleBtn;
    @FXML private JFXButton assignmentsBtn;
    @FXML private JFXButton resourcesBtn;
    
    // Student data components
    private StudentRepoManager studentRepo;
    private Student studentData;
    
    @Override
    protected void initializeRoleSpecificUI() {
        setupStudentNavigation();
        loadStudentData();
    }
    
    private void setupStudentNavigation() {
        coursesBtn.setOnAction(e -> showCoursesView());
        gradesBtn.setOnAction(e -> showGradesView());
        scheduleBtn.setOnAction(e -> showScheduleView());
        assignmentsBtn.setOnAction(e -> showAssignmentsView());
        resourcesBtn.setOnAction(e -> showResourcesView());
    }
    
    private void loadStudentData() {
        if (currentUser != null) {
            studentRepo = adminSystem.getStudentRepoManager();
            studentData = studentRepo.findById(currentUser.getID()).orElse(null);
            updateStudentDashboard();
        }
    }
    
    @Override
    protected void loadDashboardView() {
        VBox dashboard = createStudentDashboard();
        contentArea.getChildren().setAll(dashboard);
    }
    
    private VBox createStudentDashboard() {
        VBox dashboard = new VBox(20);
        dashboard.setPadding(new Insets(20));
        dashboard.setStyle("-fx-background-color: #f8f9fa;");
        
        // Quick Stats
        HBox statsPanel = createStudentStatsPanel();
        
        // Recent Activities
        VBox activitiesPanel = createRecentActivitiesPanel();
        
        // Course Overview
        GridPane coursesPanel = createCoursesOverviewPanel();
        
        // Upcoming Deadlines
        VBox deadlinesPanel = createDeadlinesPanel();
        
        dashboard.getChildren().addAll(statsPanel, activitiesPanel, coursesPanel, deadlinesPanel);
        return dashboard;
    }
    
    private HBox createStudentStatsPanel() {
        HBox statsPanel = new HBox(15);
        statsPanel.setPadding(new Insets(15));
        statsPanel.setStyle("-fx-background-color: white; -fx-background-radius: 10;");
        
        String[][] statsData = {
            {"📚", "Current Courses", "5", "Enrolled courses this semester"},
            {"⭐", "Overall GPA", "3.75", "Cumulative grade point average"},
            {"📅", "Pending Assignments", "3", "Assignments due soon"},
            {"✅", "Completed Courses", "12", "Total courses completed"}
        };
        
        for (String[] stat : statsData) {
            VBox statCard = createStatCard(stat[0], stat[1], stat[2], stat[3]);
            statsPanel.getChildren().add(statCard);
        }
        
        return statsPanel;
    }
    
    private VBox createStatCard(String icon, String title, String value, String description) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: #e3f2fd; -fx-background-radius: 8;");
        card.setPrefSize(200, 100);
        
        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 24;");
        
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12;");
        
        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 24; -fx-font-weight: bold; -fx-text-fill: #1976d2;");
        
        Label descLabel = new Label(description);
        descLabel.setStyle("-fx-font-size: 10; -fx-text-fill: #666;");
        
        card.getChildren().addAll(iconLabel, titleLabel, valueLabel, descLabel);
        return card;
    }
    
    private VBox createRecentActivitiesPanel() {
        VBox activitiesPanel = new VBox(10);
        activitiesPanel.setPadding(new Insets(15));
        activitiesPanel.setStyle("-fx-background-color: white; -fx-background-radius: 10;");
        
        Label title = new Label("Recent Activities");
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");
        
        String[][] activities = {
            {"Grade posted for Calculus Midterm", "2 hours ago", "✅"},
            {"New assignment: Physics Lab Report", "1 day ago", "📝"},
            {"Course material updated: Data Structures", "2 days ago", "📚"},
            {"Attendance recorded: 95% this week", "3 days ago", "📊"}
        };
        
        VBox activitiesList = new VBox(8);
        for (String[] activity : activities) {
            HBox activityItem = createActivityItem(activity[0], activity[1], activity[2]);
            activitiesList.getChildren().add(activityItem);
        }
        
        activitiesPanel.getChildren().addAll(title, activitiesList);
        return activitiesPanel;
    }
    
    private HBox createActivityItem(String text, String time, String icon) {
        HBox item = new HBox(10);
        item.setPadding(new Insets(8));
        item.setStyle("-fx-background-color: #f5f5f5; -fx-background-radius: 5;");
        
        Label iconLabel = new Label(icon);
        Label textLabel = new Label(text);
        textLabel.setStyle("-fx-font-size: 12;");
        
        Label timeLabel = new Label(time);
        timeLabel.setStyle("-fx-font-size: 10; -fx-text-fill: #666;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        item.getChildren().addAll(iconLabel, textLabel, spacer, timeLabel);
        return item;
    }
    
    @Override
    protected void loadProfileView() {
        VBox profileView = createStudentProfileView();
        contentArea.getChildren().setAll(profileView);
    }
    
    private VBox createStudentProfileView() {
        VBox profile = new VBox(20);
        profile.setPadding(new Insets(20));
        profile.setStyle("-fx-background-color: white;");
        
        Label title = new Label("Student Profile");
        title.setStyle("-fx-font-size: 24; -fx-font-weight: bold;");
        
        if (studentData != null) {
            GridPane profileGrid = new GridPane();
            profileGrid.setHgap(20);
            profileGrid.setVgap(15);
            profileGrid.setPadding(new Insets(20));
            
            String[][] profileData = {
                {"Student ID:", studentData.getID()},
                {"Full Name:", studentData.getName()},
                {"Grade Level:", studentData.getGrade()},
                {"Discipline:", studentData.getDiscipline()},
                {"CGPA:", String.valueOf(studentData.getCgpa())},
                {"Contact Email:", currentUser.getUsername() + "@school.edu"}
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
    
    // Student-specific view methods
    private void showCoursesView() {
        VBox coursesView = createCoursesView();
        contentArea.getChildren().setAll(coursesView);
    }
    
    private VBox createCoursesView() {
        VBox coursesView = new VBox(20);
        coursesView.setPadding(new Insets(20));
        
        Label title = new Label("My Courses");
        title.setStyle("-fx-font-size: 24; -fx-font-weight: bold;");
        
        // Course cards implementation
        GridPane coursesGrid = new GridPane();
        coursesGrid.setHgap(20);
        coursesGrid.setVgap(20);
        
        String[][] courses = {
            {"Calculus I", "MATH101", "Dr. Smith", "Mon/Wed 9:00-10:30", "A-"},
            {"Physics Fundamentals", "PHYS201", "Dr. Johnson", "Tue/Thu 11:00-12:30", "B+"},
            {"Data Structures", "CS301", "Prof. Davis", "Mon/Wed/Fri 14:00-15:00", "A"},
            {"English Literature", "ENG202", "Dr. Wilson", "Tue/Thu 13:00-14:30", "A-"}
        };
        
        for (int i = 0; i < courses.length; i++) {
            VBox courseCard = createCourseCard(courses[i]);
            coursesGrid.add(courseCard, i % 2, i / 2);
        }
        
        coursesView.getChildren().addAll(title, coursesGrid);
        return coursesView;
    }
    
    private VBox createCourseCard(String[] courseData) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 0);");
        card.setPrefSize(300, 150);
        
        Label courseName = new Label(courseData[0]);
        courseName.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");
        
        Label courseCode = new Label(courseData[1]);
        courseCode.setStyle("-fx-text-fill: #666;");
        
        Label instructor = new Label("Instructor: " + courseData[2]);
        Label schedule = new Label("Schedule: " + courseData[3]);
        Label grade = new Label("Current Grade: " + courseData[4]);
        grade.setStyle("-fx-font-weight: bold; -fx-text-fill: #1976d2;");
        
        card.getChildren().addAll(courseName, courseCode, instructor, schedule, grade);
        return card;
    }
    
    private void showGradesView() {
        // Grades view implementation
    }
    
    private void showScheduleView() {
        // Schedule view implementation
    }
    
    private void showAssignmentsView() {
        // Assignments view implementation
    }
    
    private void showResourcesView() {
        // Resources view implementation
    }
    
    // Other student-specific methods...
    private GridPane createCoursesOverviewPanel() {
        // Implementation for courses overview
        return new GridPane();
    }
    
    private VBox createDeadlinesPanel() {
        // Implementation for deadlines panel
        return new VBox();
    }
}