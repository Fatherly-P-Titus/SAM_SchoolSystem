package SchoolManager;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class StudentScoreRepoManager implements RepoManager<StudentScore, String> {
    private static final String STUDENT_SCORES_FILE = "students_scores_records.txt";
    
    private final Crypter crypter;
    private final Logger logger;
    private final Map<String, StudentScore> scoreRepository;
    private volatile boolean hasUnsavedChanges;
    
    public StudentScoreRepoManager(Crypter crypter, Logger logger) {
        this.crypter = Objects.requireNonNull(crypter, "Crypter cannot be null");
        this.logger = Objects.requireNonNull(logger, "Logger cannot be null");
        this.scoreRepository = new ConcurrentHashMap<>();
        this.hasUnsavedChanges = false;
        
        loadFromStorage();
    }
    
    // ==================== CRUD OPERATIONS ====================
    
    @Override
    public StudentScore create(StudentScore studentScore) {
        Objects.requireNonNull(studentScore, "StudentScore cannot be null");
        
        if (scoreRepository.containsKey(studentScore.getScoreId())) {
            throw new IllegalArgumentException("Student score with ID " + studentScore.getScoreId() + " already exists");
        }
        
        scoreRepository.put(studentScore.getScoreId(), studentScore);
        markAsModified();
        
        logger.info("Student score created: " + studentScore.getScoreId() + 
                   " - " + studentScore.getStudentName() + " - " + studentScore.getSubjectName());
        return studentScore;
    }
    
    @Override
    public Optional<StudentScore> read(String scoreId) {
        Objects.requireNonNull(scoreId, "Score ID cannot be null");
        return Optional.ofNullable(scoreRepository.get(scoreId));
    }
    
    @Override
    public StudentScore update(StudentScore studentScore) {
        Objects.requireNonNull(studentScore, "StudentScore cannot be null");
        
        String scoreId = studentScore.getScoreId();
        if (!scoreRepository.containsKey(scoreId)) {
            throw new IllegalArgumentException("Student score with ID " + scoreId + " not found");
        }
        
        scoreRepository.put(scoreId, studentScore);
        markAsModified();
        
        logger.info("Student score updated: " + scoreId);
        return studentScore;
    }
    
    @Override
    public boolean delete(String scoreId) {
        Objects.requireNonNull(scoreId, "Score ID cannot be null");
        
        StudentScore removed = scoreRepository.remove(scoreId);
        if (removed != null) {
            markAsModified();
            logger.info("Student score deleted: " + scoreId);
            return true;
        }
        return false;
    }
    
    @Override
    public boolean exists(String scoreId) {
        return scoreRepository.containsKey(scoreId);
    }
    
    // ==================== BULK OPERATIONS ====================
    
    @Override
    public List<StudentScore> findAll() {
        return Collections.unmodifiableList(new ArrayList<>(scoreRepository.values()));
    }
    
    @Override
    public long count() {
        return scoreRepository.size();
    }
    
    @Override
    public void deleteAll() {
        scoreRepository.clear();
        markAsModified();
        logger.info("All student scores deleted");
    }
    
    // ==================== REPOSITORY MANAGEMENT ====================
    
    @Override
    public void saveToStorage() {
        if (!hasUnsavedChanges) {
            logger.info("No score changes to save");
            return;
        }
        
        try {
            persistScoreData();
            hasUnsavedChanges = false;
            logger.info("Student score repository saved successfully");
        } catch (IOException e) {
            logger.error("Failed to save score repository: " + e.getMessage(), e);
            throw new RepositoryException("Failed to save score data", e);
        }
    }
    
    @Override
    public void loadFromStorage() {
        scoreRepository.clear();
        
        File file = new File(STUDENT_SCORES_FILE);
        if (!file.exists()) {
            logger.info("Student scores file not found, starting with empty repository");
            return;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int loadedCount = 0;
            
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    try {
                        String decryptedData = crypter.decodeDecrypt(line.trim());
                        StudentScore score = StudentScore.fromCsv(decryptedData);
                        scoreRepository.put(score.getScoreId(), score);
                        loadedCount++;
                    } catch (Exception e) {
                        logger.error("Failed to parse score data: " + e.getMessage(), e);
                    }
                }
            }
            
            logger.info("Loaded " + loadedCount + " student scores from repository");
            hasUnsavedChanges = false;
        } catch (IOException e) {
            logger.error("Failed to load score repository: " + e.getMessage(), e);
            throw new RepositoryException("Failed to load score data", e);
        }
    }
    
    @Override
    public boolean hasUnsavedChanges() {
        return hasUnsavedChanges;
    }
    
    // ==================== QUERY OPERATIONS ====================
    
    @Override
    public List<StudentScore> findByAttribute(String attribute, Object value) {
        Objects.requireNonNull(attribute, "Attribute cannot be null");
        Objects.requireNonNull(value, "Value cannot be null");
        
        Predicate<StudentScore> predicate = createAttributePredicate(attribute, value);
        List<StudentScore> results = scoreRepository.values().stream()
                .filter(predicate)
                .collect(Collectors.toList());
        
        logger.info(String.format("Score query [%s = '%s'] found %d matches", attribute, value, results.size()));
        return Collections.unmodifiableList(results);
    }
    
    @Override
    public List<StudentScore> search(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        String term = searchTerm.toLowerCase();
        Predicate<StudentScore> searchPredicate = score ->
            score.getStudentName().toLowerCase().contains(term) ||
            score.getStudentId().toLowerCase().contains(term) ||
            score.getSubjectName().toLowerCase().contains(term) ||
            score.getSubjectCode().toLowerCase().contains(term) ||
            score.getStudentGrade().toLowerCase().contains(term) ||
            score.getTeacherComments() != null && score.getTeacherComments().toLowerCase().contains(term);
        
        List<StudentScore> results = scoreRepository.values().stream()
                .filter(searchPredicate)
                .collect(Collectors.toList());
        
        logger.info("Score search found " + results.size() + " matches for: " + searchTerm);
        return Collections.unmodifiableList(results);
    }
    
    // ==================== BUSINESS-SPECIFIC OPERATIONS ====================
    
    public List<StudentScore> findByStudentId(String studentId) {
        return scoreRepository.values().stream()
                .filter(score -> score.getStudentId().equals(studentId))
                .sorted(Comparator.comparing(StudentScore::getSubjectName))
                .collect(Collectors.toList());
    }
    
    public List<StudentScore> findBySubjectCode(String subjectCode) {
        return scoreRepository.values().stream()
                .filter(score -> score.getSubjectCode().equals(subjectCode))
                .sorted(Comparator.comparing(StudentScore::getScore).reversed())
                .collect(Collectors.toList());
    }
    
    public List<StudentScore> findByGradeLevel(String gradeLevel) {
        return scoreRepository.values().stream()
                .filter(score -> score.getStudentGrade().equals(gradeLevel))
                .collect(Collectors.toList());
    }
    
    public List<StudentScore> findBySemesterAndYear(int semester, int academicYear) {
        return scoreRepository.values().stream()
                .filter(score -> score.getSemester() == semester && score.getAcademicYear() == academicYear)
                .collect(Collectors.toList());
    }
    
    public List<StudentScore> findPassingScores() {
        return scoreRepository.values().stream()
                .filter(StudentScore::isPassingGrade)
                .collect(Collectors.toList());
    }
    
    public List<StudentScore> findFailingScores() {
        return scoreRepository.values().stream()
                .filter(score -> !score.isPassingGrade())
                .collect(Collectors.toList());
    }
    
    public List<StudentScore> findExcellentScores() {
        return scoreRepository.values().stream()
                .filter(StudentScore::isExcellentGrade)
                .collect(Collectors.toList());
    }
    
    public List<StudentScore> findScoresNeedingImprovement() {
        return scoreRepository.values().stream()
                .filter(StudentScore::needsImprovement)
                .collect(Collectors.toList());
    }
    
    public Map<String, Double> calculateStudentGPAs() {
        return scoreRepository.values().stream()
                .collect(Collectors.groupingBy(
                    StudentScore::getStudentId,
                    Collectors.averagingDouble(StudentScore::getGradePoints)
                ));
    }
    
    public Double calculateStudentGPA(String studentId) {
        return scoreRepository.values().stream()
                .filter(score -> score.getStudentId().equals(studentId))
                .mapToDouble(StudentScore::getGradePoints)
                .average()
                .orElse(0.0);
    }
    
    public Map<String, Double> calculateSubjectAverages() {
        return scoreRepository.values().stream()
                .collect(Collectors.groupingBy(
                    StudentScore::getSubjectCode,
                    Collectors.averagingDouble(StudentScore::getScore)
                ));
    }
    
    public Map<String, Long> getGradeDistribution() {
        return scoreRepository.values().stream()
                .collect(Collectors.groupingBy(
                    StudentScore::getLetterGrade,
                    Collectors.counting()
                ));
    }
    
    public Map<String, Double> getClassAveragesBySubject(String gradeLevel) {
        return scoreRepository.values().stream()
                .filter(score -> score.getStudentGrade().equals(gradeLevel))
                .collect(Collectors.groupingBy(
                    StudentScore::getSubjectCode,
                    Collectors.averagingDouble(StudentScore::getScore)
                ));
    }
    
    public StudentScore recordScore(Student student, Subject subject, double score, String comments) {
        Objects.requireNonNull(student, "Student cannot be null");
        Objects.requireNonNull(subject, "Subject cannot be null");
        
        StudentScore newScore = new StudentScore.Builder()
            .withStudent(student)
            .withSubject(subject)
            .withScore(score)
            .withTeacherComments(comments)
            .build();
        
        return create(newScore);
    }
    
    public List<StudentScore> initializeStudentScores(Student student, List<Subject> subjects) {
        Objects.requireNonNull(student, "Student cannot be null");
        Objects.requireNonNull(subjects, "Subjects cannot be null");
        
        List<StudentScore> newScores = subjects.stream()
            .filter(subject -> subject.getGrade().equals(student.getGrade()))
            .map(subject -> new StudentScore.Builder()
                .withStudent(student)
                .withSubject(subject)
                .withScore(0.0)
                .withTeacherComments("Initial entry")
                .build())
            .collect(Collectors.toList());
        
        newScores.forEach(this::create);
        return newScores;
    }
    
    // ==================== PRIVATE METHODS ====================
    
    private void persistScoreData() throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(STUDENT_SCORES_FILE, false))) {
            for (StudentScore score : scoreRepository.values()) {
                try {
                    String csvData = score.toString();
                    String encryptedData = crypter.encryptEncode(csvData);
                    writer.println(encryptedData);
                } catch (Exception e) {
                    logger.error("Failed to encrypt score data for ID: " + score.getScoreId(), e);
                    throw new IOException("Encryption failed for score: " + score.getScoreId(), e);
                }
            }
        }
    }
    
    private Predicate<StudentScore> createAttributePredicate(String attribute, Object value) {
        switch (attribute.toUpperCase()) {
            case "SCOREID":
            case "ID":
                return score -> score.getScoreId().equals(value);
            case "STUDENTID":
                return score -> score.getStudentId().equals(value);
            case "STUDENTNAME":
                return score -> score.getStudentName().equalsIgnoreCase(value.toString());
            case "GRADELEVEL":
            case "STUDENTGRADE":
                return score -> score.getStudentGrade().equalsIgnoreCase(value.toString());
            case "SUBJECTCODE":
                return score -> score.getSubjectCode().equals(value.toString());
            case "SUBJECTNAME":
                return score -> score.getSubjectName().equalsIgnoreCase(value.toString());
            case "SCORE":
                try {
                    double scoreValue = Double.parseDouble(value.toString());
                    return score -> score.getScore() == scoreValue;
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid score value: " + value);
                }
            case "SEMESTER":
                try {
                    int semesterValue = Integer.parseInt(value.toString());
                    return score -> score.getSemester() == semesterValue;
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid semester value: " + value);
                }
            case "ACADEMICYEAR":
                try {
                    int yearValue = Integer.parseInt(value.toString());
                    return score -> score.getAcademicYear() == yearValue;
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid academic year value: " + value);
                }
            case "LETTERGRADE":
                return score -> score.getLetterGrade().equals(value.toString());
            default:
                throw new IllegalArgumentException("Unsupported score attribute: " + attribute);
        }
    }
    
    private void markAsModified() {
        this.hasUnsavedChanges = true;
    }
    
    // ==================== CUSTOM EXCEPTION ====================
    
    public static class RepositoryException extends RuntimeException {
        public RepositoryException(String message) {
            super(message);
        }
        
        public RepositoryException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}