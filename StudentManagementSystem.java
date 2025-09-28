package SchoolManager.UI;

import SchoolManager.*;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.util.Optional;

public class StudentManagementSystem extends Application {
    private StudentManager studentManager;
    private Crypter crypter;
    private Logger logger;
    
    // UI Components
    private TabPane mainTabPane;
    private TableView<Student> studentTable;
    private ObservableList<Student> studentData;
    
    @Override
    public void init() throws Exception {
        // Initialize dependencies
        this.crypter = new CrypterSymmetricMech();
        this.logger = new LoggerMech(crypter, "StudentManagementSystem");
        
        // Initialize repositories
        StudentRepoManager studentRepo = new StudentRepoManager(crypter, logger);
        StudentFeeRepoManager feeRepo = new StudentFeeRepoManager(crypter, logger);
        StudentScoreRepoManager scoreRepo = new StudentScoreRepoManager(crypter, logger);
        SubjectRepositoryManager subjectRepo = new SubjectRepositoryManager(crypter, logger);
        
        // Initialize student manager
        this.studentManager = new StudentManager(studentRepo, feeRepo, scoreRepo, subjectRepo, logger);
        
        // Initialize UI data
        this.studentData = FXCollections.observableArrayList(studentRepo.findAll());
    }
    
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("School Manager - Student Management System");
        
        // Create main layout
        BorderPane root = new BorderPane();
        root.setTop(createMenuBar());
        root.setCenter(createMainContent());
        
        Scene scene = new Scene(root, 1200, 800);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    
    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();
        
        // File Menu
        Menu fileMenu = new Menu("File");
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setOnAction(e -> System.exit(0));
        fileMenu.getItems().addAll(exitItem);
        
        // Student Menu
        Menu studentMenu = new Menu("Students");
        MenuItem newStudentItem = new MenuItem("New Student");
        MenuItem refreshItem = new MenuItem("Refresh");
        
        newStudentItem.setOnAction(e -> showNewStudentDialog());
        refreshItem.setOnAction(e -> refreshStudentData());
        
        studentMenu.getItems().addAll(newStudentItem, new SeparatorMenuItem(), refreshItem);
        
        // Reports Menu
        Menu reportsMenu = new Menu("Reports");
        MenuItem statisticsItem = new MenuItem("School Statistics");
        statisticsItem.setOnAction(e -> showStatisticsDialog());
        
        reportsMenu.getItems().addAll(statisticsItem);
        
        menuBar.getMenus().addAll(fileMenu, studentMenu, reportsMenu);
        return menuBar;
    }
    
    private TabPane createMainContent() {
        mainTabPane = new TabPane();
        
        // Student Management Tab
        Tab studentTab = new Tab("Student Management");
        studentTab.setContent(createStudentManagementTab());
        studentTab.setClosable(false);
        
        // Fee Management Tab
        Tab feeTab = new Tab("Fee Management");
        feeTab.setContent(createFeeManagementTab());
        feeTab.setClosable(false);
        
        // Score Management Tab
        Tab scoreTab = new Tab("Score Management");
        scoreTab.setContent(createScoreManagementTab());
        scoreTab.setClosable(false);
        
        mainTabPane.getTabs().addAll(studentTab, feeTab, scoreTab);
        return mainTabPane;
    }
    
    private VBox createStudentManagementTab() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        
        // Toolbar
        HBox toolbar = new HBox(10);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        
        Button addButton = new Button("Add New Student");
        Button editButton = new Button("Edit Student");
        Button deleteButton = new Button("Delete Student");
        Button viewButton = new Button("View Dashboard");
        
        addButton.setOnAction(e -> showNewStudentDialog());
        editButton.setOnAction(e -> editSelectedStudent());
        deleteButton.setOnAction(e -> deleteSelectedStudent());
        viewButton.setOnAction(e -> showStudentDashboard());
        
        toolbar.getChildren().addAll(addButton, editButton, deleteButton, viewButton);
        
        // Student Table
        studentTable = new TableView<>();
        setupStudentTable();
        
        content.getChildren().addAll(toolbar, studentTable);
        return content;
    }
    
    private void setupStudentTable() {
        // Clear existing columns
        studentTable.getColumns().clear();
        
        // Create columns
        TableColumn<Student, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        
        TableColumn<Student, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        
        TableColumn<Student, String> gradeCol = new TableColumn<>("Grade");
        gradeCol.setCellValueFactory(new PropertyValueFactory<>("grade"));
        
        TableColumn<Student, String> disciplineCol = new TableColumn<>("Discipline");
        disciplineCol.setCellValueFactory(new PropertyValueFactory<>("discipline"));
        
        TableColumn<Student, Double> cgpaCol = new TableColumn<>("CGPA");
        cgpaCol.setCellValueFactory(new PropertyValueFactory<>("cgpa"));
        
        studentTable.getColumns().addAll(idCol, nameCol, gradeCol, disciplineCol, cgpaCol);
        studentTable.setItems(studentData);
    }
    
    private VBox createFeeManagementTab() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        
        Label title = new Label("Fee Management");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        // Fee payment section
        VBox paymentSection = new VBox(10);
        paymentSection.setStyle("-fx-border-color: #ccc; -fx-border-width: 1; -fx-padding: 10;");
        
        Label paymentTitle = new Label("Process Payment");
        paymentTitle.setStyle("-fx-font-weight: bold;");
        
        HBox paymentForm = new HBox(10);
        TextField studentIdField = new TextField();
        studentIdField.setPromptText("Student ID");
        
        TextField amountField = new TextField();
        amountField.setPromptText("Amount");
        
        TextField referenceField = new TextField();
        referenceField.setPromptText("Reference");
        
        Button processPaymentButton = new Button("Process Payment");
        processPaymentButton.setOnAction(e -> processPayment(
            studentIdField.getText(), 
            amountField.getText(), 
            referenceField.getText()
        ));
        
        paymentForm.getChildren().addAll(studentIdField, amountField, referenceField, processPaymentButton);
        paymentForm.setAlignment(Pos.CENTER_LEFT);
        
        paymentSection.getChildren().addAll(paymentTitle, paymentForm);
        
        // Overdue accounts section
        VBox overdueSection = new VBox(10);
        overdueSection.setStyle("-fx-border-color: #ccc; -fx-border-width: 1; -fx-padding: 10;");
        
        Label overdueTitle = new Label("Overdue Accounts");
        overdueTitle.setStyle("-fx-font-weight: bold;");
        
        Button viewOverdueButton = new Button("View Overdue Accounts");
        viewOverdueButton.setOnAction(e -> showOverdueAccounts());
        
        overdueSection.getChildren().addAll(overdueTitle, viewOverdueButton);
        
        content.getChildren().addAll(title, paymentSection, overdueSection);
        return content;
    }
    
    private VBox createScoreManagementTab() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        
        Label title = new Label("Score Management");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        // Record score section
        VBox recordScoreSection = new VBox(10);
        recordScoreSection.setStyle("-fx-border-color: #ccc; -fx-border-width: 1; -fx-padding: 10;");
        
        Label recordTitle = new Label("Record Student Score");
        recordTitle.setStyle("-fx-font-weight: bold;");
        
        GridPane scoreForm = new GridPane();
        scoreForm.setHgap(10);
        scoreForm.setVgap(10);
        
        TextField scoreStudentIdField = new TextField();
        TextField scoreSubjectField = new TextField();
        TextField scoreValueField = new TextField();
        TextArea commentsArea = new TextArea();
        
        scoreForm.add(new Label("Student ID:"), 0, 0);
        scoreForm.add(scoreStudentIdField, 1, 0);
        scoreForm.add(new Label("Subject Code:"), 0, 1);
        scoreForm.add(scoreSubjectField, 1, 1);
        scoreForm.add(new Label("Score:"), 0, 2);
        scoreForm.add(scoreValueField, 1, 2);
        scoreForm.add(new Label("Comments:"), 0, 3);
        scoreForm.add(commentsArea, 1, 3);
        
        Button recordScoreButton = new Button("Record Score");
        recordScoreButton.setOnAction(e -> recordScore(
            scoreStudentIdField.getText(),
            scoreSubjectField.getText(),
            scoreValueField.getText(),
            commentsArea.getText()
        ));
        
        scoreForm.add(recordScoreButton, 1, 4);
        
        recordScoreSection.getChildren().addAll(recordTitle, scoreForm);
        content.getChildren().addAll(title, recordScoreSection);
        
        return content;
    }
    
    // ==================== DIALOG METHODS ====================
    
    private void showNewStudentDialog() {
        Dialog<Student> dialog = new Dialog<>();
        dialog.setTitle("Enroll New Student");
        dialog.setHeaderText("Enter student information");
        
        // Set the button types
        ButtonType enrollButtonType = new ButtonType("Enroll", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(enrollButtonType, ButtonType.CANCEL);
        
        // Create the form
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        TextField nameField = new TextField();
        nameField.setPromptText("Full Name");
        TextField genderField = new TextField();
        genderField.setPromptText("Gender");
        TextField ageField = new TextField();
        ageField.setPromptText("Age");
        TextField disciplineField = new TextField();
        disciplineField.setPromptText("Discipline");
        TextField gradeField = new TextField();
        gradeField.setPromptText("Grade");
        TextField phoneField = new TextField();
        phoneField.setPromptText("Phone");
        TextField addressField = new TextField();
        addressField.setPromptText("Address");
        
        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Gender:"), 0, 1);
        grid.add(genderField, 1, 1);
        grid.add(new Label("Age:"), 0, 2);
        grid.add(ageField, 1, 2);
        grid.add(new Label("Discipline:"), 0, 3);
        grid.add(disciplineField, 1, 3);
        grid.add(new Label("Grade:"), 0, 4);
        grid.add(gradeField, 1, 4);
        grid.add(new Label("Phone:"), 0, 5);
        grid.add(phoneField, 1, 5);
        grid.add(new Label("Address:"), 0, 6);
        grid.add(addressField, 1, 6);
        
        dialog.getDialogPane().setContent(grid);
        
        // Convert the result to a Student object when the enroll button is clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == enrollButtonType) {
                try {
                    Student student = new Student.Builder()
                        .withName(nameField.getText())
                        .withGender(genderField.getText())
                        .withAge(Integer.parseInt(ageField.getText()))
                        .withDiscipline(disciplineField.getText())
                        .withGrade(gradeField.getText())
                        .withPhone(phoneField.getText())
                        .withAddress(addressField.getText())
                        .build();
                    
                    return student;
                } catch (Exception e) {
                    showErrorDialog("Invalid student data", e.getMessage());
                    return null;
                }
            }
            return null;
        });
        
        Optional<Student> result = dialog.showAndWait();
        result.ifPresent(student -> {
            try {
                StudentManager.EnrollmentResult enrollmentResult = studentManager.enrollNewStudent(student);
                studentData.add(enrollmentResult.getStudent());
                showSuccessDialog("Student enrolled successfully!", 
                    "Student ID: " + enrollmentResult.getStudent().getID());
            } catch (Exception e) {
                showErrorDialog("Enrollment failed", e.getMessage());
            }
        });
    }
    
    private void showStudentDashboard() {
        Student selected = studentTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showErrorDialog("No Selection", "Please select a student first.");
            return;
        }
        
        try {
            StudentManager.StudentDashboard dashboard = studentManager.getStudentDashboard(selected.getID());
            
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Student Dashboard");
            alert.setHeaderText("Dashboard for: " + dashboard.getStudent().getName());
            
            VBox content = new VBox(10);
            
            // Student info
            Label studentInfo = new Label("Student Information:");
            studentInfo.setStyle("-fx-font-weight: bold;");
            TextArea infoArea = new TextArea(dashboard.getStudent().getInfoString());
            infoArea.setEditable(false);
            
            // Fee info
            Label feeInfo = new Label("Fee Information:");
            feeInfo.setStyle("-fx-font-weight: bold;");
            TextArea feeArea = new TextArea(
                dashboard.getFee() != null ? dashboard.getFee().toDetailedString() : "No fee record found");
            feeArea.setEditable(false);
            
            // Academic info
            Label academicInfo = new Label("Academic Performance:");
            academicInfo.setStyle("-fx-font-weight: bold;");
            Label gpaLabel = new Label(String.format("GPA: %.2f", dashboard.getGPA()));
            Label avgScoreLabel = new Label(String.format("Average Score: %.1f%%", dashboard.getAverageScore()));
            
            content.getChildren().addAll(studentInfo, infoArea, feeInfo, feeArea, 
                                       academicInfo, gpaLabel, avgScoreLabel);
            
            alert.getDialogPane().setContent(content);
            alert.showAndWait();
            
        } catch (Exception e) {
            showErrorDialog("Dashboard Error", e.getMessage());
        }
    }
    
    private void showStatisticsDialog() {
        try {
            StudentManager.SchoolStatistics stats = studentManager.getSchoolStatistics();
            
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("School Statistics");
            alert.setHeaderText("Current School Statistics");
            
            VBox content = new VBox(10);
            
            Label studentsLabel = new Label("Total Students: " + stats.getTotalStudents());
            Label feesOutstandingLabel = new Label("Total Fees Outstanding: $" + stats.getTotalFeesOutstanding());
            Label feesCollectedLabel = new Label("Total Fees Collected: $" + stats.getTotalFeesCollected());
            
            content.getChildren().addAll(studentsLabel, feesOutstandingLabel, feesCollectedLabel);
            alert.getDialogPane().setContent(content);
            alert.showAndWait();
            
        } catch (Exception e) {
            showErrorDialog("Statistics Error", e.getMessage());
        }
    }
    
    // ==================== ACTION METHODS ====================
    
    private void processPayment(String studentId, String amountStr, String reference) {
        if (studentId.isEmpty() || amountStr.isEmpty()) {
            showErrorDialog("Invalid Input", "Please enter both student ID and amount.");
            return;
        }
        
        try {
            BigDecimal amount = new BigDecimal(amountStr);
            studentManager.processStudentPayment(studentId, amount, reference);
            showSuccessDialog("Payment Processed", "Payment of $" + amount + " processed successfully.");
        } catch (Exception e) {
            showErrorDialog("Payment Error", e.getMessage());
        }
    }
    
    private void recordScore(String studentId, String subjectCode, String scoreStr, String comments) {
        if (studentId.isEmpty() || subjectCode.isEmpty() || scoreStr.isEmpty()) {
            showErrorDialog("Invalid Input", "Please fill all required fields.");
            return;
        }
        
        try {
            double score = Double.parseDouble(scoreStr);
            studentManager.recordStudentScore(studentId, subjectCode, score, comments);
            showSuccessDialog("Score Recorded", "Score recorded successfully.");
        } catch (Exception e) {
            showErrorDialog("Score Recording Error", e.getMessage());
        }
    }
    
    private void showOverdueAccounts() {
        try {
            var overdueAccounts = studentManager.getOverdueAccounts();
            
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Overdue Accounts");
            alert.setHeaderText("Students with Overdue Fees");
            
            if (overdueAccounts.isEmpty()) {
                alert.setContentText("No overdue accounts found.");
            } else {
                StringBuilder content = new StringBuilder();
                for (var fee : overdueAccounts) {
                    content.append(String.format("%s - %s: $%.2f owed\n", 
                        fee.getStudentId(), fee.getStudentName(), fee.getAmountOwed()));
                }
                alert.setContentText(content.toString());
            }
            
            alert.showAndWait();
        } catch (Exception e) {
            showErrorDialog("Error", e.getMessage());
        }
    }
    
    private void editSelectedStudent() {
        Student selected = studentTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showErrorDialog("No Selection", "Please select a student to edit.");
            return;
        }
        // Implementation for edit dialog would go here
        showInfoDialog("Edit Feature", "Edit functionality to be implemented.");
    }
    
    private void deleteSelectedStudent() {
        Student selected = studentTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showErrorDialog("No Selection", "Please select a student to delete.");
            return;
        }
        
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirm Deletion");
        confirmation.setHeaderText("Delete Student");
        confirmation.setContentText("Are you sure you want to delete student: " + selected.getName() + "?");
        
        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                if (studentManager.removeStudent(selected.getID())) {
                    studentData.remove(selected);
                    showSuccessDialog("Student Deleted", "Student removed successfully.");
                }
            } catch (Exception e) {
                showErrorDialog("Deletion Error", e.getMessage());
            }
        }
    }
    
    private void refreshStudentData() {
        try {
            studentData.setAll(studentManager.studentRepo.findAll());
            showSuccessDialog("Refresh Complete", "Student data refreshed successfully.");
        } catch (Exception e) {
            showErrorDialog("Refresh Error", e.getMessage());
        }
    }
    
    // ==================== UTILITY METHODS ====================
    
    private void showErrorDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showSuccessDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showInfoDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}