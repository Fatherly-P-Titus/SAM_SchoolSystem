package SchoolManager;

import java.util.*;
import java.io.*;
import java.nio.file.*;
import java.util.stream.Collectors;

public class SubjectRepositoryManager implements RepositoryManager<Subject> {
    private static final String SUBJECTS_FILE = "data/subjects.txt";
    private static final String COURSES_FILE = "data/courses.txt";
    private static final String STUDENT_COURSES_FILE = "data/student_courses.txt";
    
    private final Crypter crypter;
    private final Logger logger;
    private final Set<Subject> subjects;
    private final Map<String, List<Subject>> gradeSubjectsMap;
    private final Map<String, List<Subject>> disciplineCoursesMap;
    private boolean repositoryModified;
    
    public SubjectRepositoryManager(Crypter crypter, Logger logger) {
        this.crypter = Objects.requireNonNull(crypter, "Crypter cannot be null");
        this.logger = Objects.requireNonNull(logger, "Logger cannot be null");
        
        this.subjects = new LinkedHashSet<>(); // Maintain insertion order
        this.gradeSubjectsMap = new HashMap<>();
        this.disciplineCoursesMap = new HashMap<>();
        this.repositoryModified = false;
        
        initializeRepository();
    }
    
    private void initializeRepository() {
        loadSubjects();
        loadDisciplineCourses();
        logger.logINFO("SubjectRepositoryManager initialized successfully");
    }
    
    // RepositoryManager interface implementation
    @Override
    public Subject createEntry(Subject subject) {
        Objects.requireNonNull(subject, "Subject cannot be null");
        
        if (!subject.isValid()) {
            throw new IllegalArgumentException("Invalid subject data");
        }
        
        if (subjects.contains(subject)) {
            throw new IllegalStateException("Subject already exists: " + subject.getCode());
        }
        
        subjects.add(subject);
        repositoryModified = true;
        updateGradeSubjectsMap(subject);
        
        logger.logINFO("Created new subject: " + subject.getCode());
        return subject;
    }
    
    @Override
    public Subject updateEntry(Subject subject, String attribute, String value) {
        Objects.requireNonNull(subject, "Subject cannot be null");
        
        if (!subjects.contains(subject)) {
            throw new IllegalArgumentException("Subject not found in repository");
        }
        
        Subject updatedSubject = subject.getObject(); // Get a copy
        updateSubjectAttribute(updatedSubject, attribute, value);
        
        // Replace the subject in the repository
        subjects.remove(subject);
        subjects.add(updatedSubject);
        repositoryModified = true;
        rebuildGradeSubjectsMap();
        
        logger.logINFO("Updated subject: " + subject.getCode() + " - " + attribute + " = " + value);
        return updatedSubject;
    }
    
    @Override
    public List<Subject> queryEntry(String attribute, String value) {
        return subjects.stream()
                .filter(subject -> matchesAttribute(subject, attribute, value))
                .collect(Collectors.toList());
    }
    
    @Override
    public boolean deleteEntry(Subject subject) {
        boolean removed = subjects.remove(subject);
        if (removed) {
            repositoryModified = true;
            rebuildGradeSubjectsMap();
            logger.logINFO("Deleted subject: " + subject.getCode());
        }
        return removed;
    }
    
    @Override
    public void viewEntries() {
        System.out.println("\n=== SUBJECT REPOSITORY ===");
        System.out.println("Total subjects: " + subjects.size());
        System.out.println("---------------------------");
        
        if (subjects.isEmpty()) {
            System.out.println("No subjects found in repository.");
            return;
        }
        
        int index = 1;
        for (Subject subject : subjects) {
            System.out.printf("%d. %s%n", index++, subject.infoString());
        }
    }
    
    @Override
    public List<Subject> getRepository() {
        return new ArrayList<>(subjects);
    }
    
    @Override
    public void saveRepository() {
        if (repositoryModified) {
            saveSubjects();
            repositoryModified = false;
            logger.logINFO("Subject repository saved successfully");
        }
    }
    
    // Enhanced functionality
    public List<Subject> getSubjectsByGrade(String gradeLevel) {
        return gradeSubjectsMap.getOrDefault(gradeLevel, Collections.emptyList());
    }
    
    public List<Subject> getSubjectsByDiscipline(String discipline) {
        return disciplineCoursesMap.getOrDefault(discipline, Collections.emptyList());
    }
    
    public boolean subjectExists(String code) {
        return subjects.stream().anyMatch(s -> s.getCode().equalsIgnoreCase(code));
    }
    
    public Subject getSubjectByCode(String code) {
        return subjects.stream()
                .filter(s -> s.getCode().equalsIgnoreCase(code))
                .findFirst()
                .orElse(null);
    }
    
    public void assignSubjectsToStudent(Student student, List<Subject> subjects) {
        Objects.requireNonNull(student, "Student cannot be null");
        Objects.requireNonNull(subjects, "Subjects list cannot be null");
        
        if (subjects.isEmpty()) {
            throw new IllegalArgumentException("Subjects list cannot be empty");
        }
        
        saveStudentCourses(student, subjects);
        logger.logINFO("Assigned " + subjects.size() + " subjects to student: " + student.getID());
    }
    
    public Map<String, Integer> getSubjectStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("total_subjects", subjects.size());
        
        // Count subjects by grade level
        Map<String, Long> gradeCounts = subjects.stream()
                .collect(Collectors.groupingBy(Subject::getGradeLevel, Collectors.counting()));
        
        gradeCounts.forEach((grade, count) -> stats.put("grade_" + grade, count.intValue()));
        
        return stats;
    }
    
    // Private helper methods
    private void updateSubjectAttribute(Subject subject, String attribute, String value) {
        switch (attribute.toUpperCase()) {
            case "TITLE" -> subject.setTitle(value);
            case "CODE" -> subject.setCode(value);
            case "GRADE" -> subject.setGradeLevel(value);
            case "CREDITS" -> subject.setCredits(Double.parseDouble(value));
            default -> throw new IllegalArgumentException("Unknown attribute: " + attribute);
        }
    }
    
    private boolean matchesAttribute(Subject subject, String attribute, String value) {
        return switch (attribute.toUpperCase()) {
            case "TITLE" -> subject.getTitle().equalsIgnoreCase(value);
            case "CODE" -> subject.getCode().equalsIgnoreCase(value);
            case "GRADE" -> subject.getGradeLevel().equalsIgnoreCase(value);
            case "CREDITS" -> String.valueOf(subject.getCredits()).equals(value);
            case "ANY" -> subject.getTitle().equalsIgnoreCase(value) ||
                         subject.getCode().equalsIgnoreCase(value) ||
                         subject.getGradeLevel().equalsIgnoreCase(value);
            default -> false;
        };
    }
    
    private void updateGradeSubjectsMap(Subject subject) {
        gradeSubjectsMap.computeIfAbsent(subject.getGradeLevel(), k -> new ArrayList<>())
                       .add(subject);
    }
    
    private void rebuildGradeSubjectsMap() {
        gradeSubjectsMap.clear();
        subjects.forEach(this::updateGradeSubjectsMap);
    }
    
    // File operations
    private void loadSubjects() {
        File file = new File(SUBJECTS_FILE);
        if (!file.exists()) {
            // Fallback to courses file if subjects file doesn't exist
            loadFromCoursesFile();
            return;
        }
        
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(SUBJECTS_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    try {
                        // Assuming encrypted data needs decryption
                        String decryptedLine = crypter.decodeDecrypt(line.trim());
                        Subject subject = new Subject(decryptedLine);
                        if (subject.isValid()) {
                            subjects.add(subject);
                        }
                    } catch (Exception e) {
                        logger.logINFO("Error parsing subject line: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            logger.logINFO("Error loading subjects: " + e.getMessage());
        }
    }
    
    private void loadFromCoursesFile() {
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(COURSES_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    try {
                        Subject subject = new Subject(line);
                        if (subject.isValid()) {
                            subjects.add(subject);
                        }
                    } catch (Exception e) {
                        logger.logINFO("Error parsing course line: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            logger.logINFO("Error loading courses: " + e.getMessage());
        }
    }
    
    private void loadDisciplineCourses() {
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(COURSES_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    String[] data = line.split(",", -1);
                    if (data.length >= 5) {
                        String discipline = data[4];
                        Subject subject = new Subject(data[3], data[0], data[1], 
                                                    Double.parseDouble(data[2]));
                        
                        disciplineCoursesMap.computeIfAbsent(discipline, k -> new ArrayList<>())
                                          .add(subject);
                    }
                }
            }
        } catch (IOException e) {
            logger.logINFO("Error loading discipline courses: " + e.getMessage());
        }
    }
    
    private void saveSubjects() {
        ensureDataDirectory();
        
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(SUBJECTS_FILE))) {
            for (Subject subject : subjects) {
                String encryptedData = crypter.encryptEncode(subject.toString());
                writer.write(encryptedData);
                writer.newLine();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to save subjects: " + e.getMessage(), e);
        }
    }
    
    private void saveStudentCourses(Student student, List<Subject> courses) {
        ensureDataDirectory();
        
        try (BufferedWriter writer = Files.newBufferedWriter(
                Paths.get(STUDENT_COURSES_FILE), StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            
            String courseCodes = courses.stream()
                    .map(Subject::getCode)
                    .collect(Collectors.joining(","));
            
            String record = String.format("%s,%s,%s,%s", 
                    student.getName(), student.getID(), student.getDiscipline(), courseCodes);
            
            writer.write(record);
            writer.newLine();
        } catch (IOException e) {
            throw new RuntimeException("Failed to save student courses: " + e.getMessage(), e);
        }
    }
    
    private void ensureDataDirectory() {
        File dataDir = new File("data");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
    }
    
    // Utility methods
    public int getRepositorySize() {
        return subjects.size();
    }
    
    public boolean isRepositoryModified() {
        return repositoryModified;
    }
    
    public String getRepositoryInfo() {
        return String.format("Subject Repository: %d subjects, %d grade levels, modified: %s",
                subjects.size(), gradeSubjectsMap.size(), repositoryModified);
    }
}