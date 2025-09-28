package SchoolManager;

import java.io.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class StudentFeeRepoManager implements RepoManager<StudentFee, String> {
    private static final String STUDENT_FEE_FILE = "students_fee_records.txt";
    private static final String GRADE_FEE_FILE = "school_grade_fees.txt";
    private static final String PAYMENT_HISTORY_FILE = "fees_payment_history.txt";
    
    private final Crypter crypter;
    private final Logger logger;
    private final Map<String, StudentFee> feeRepository;
    private final Map<String, BigDecimal> gradeFeeMapper;
    private final List<PaymentRecord> paymentHistory;
    private volatile boolean hasUnsavedChanges;
    
    public StudentFeeRepoManager(Crypter crypter, Logger logger) {
        this.crypter = Objects.requireNonNull(crypter, "Crypter cannot be null");
        this.logger = Objects.requireNonNull(logger, "Logger cannot be null");
        this.feeRepository = new HashMap<>();
        this.gradeFeeMapper = new HashMap<>();
        this.paymentHistory = new CopyOnWriteArrayList<>();
        this.hasUnsavedChanges = false;
        
        initializeRepository();
    }
    
    // ==================== CRUD OPERATIONS ====================
    
    @Override
    public StudentFee create(StudentFee studentFee) {
        Objects.requireNonNull(studentFee, "StudentFee cannot be null");
        
        if (feeRepository.containsKey(studentFee.getStudentId())) {
            throw new IllegalArgumentException("Fee record for student " + studentFee.getStudentId() + " already exists");
        }
        
        feeRepository.put(studentFee.getStudentId(), studentFee);
        markAsModified();
        
        logger.logINFO("Student fee record created for: " + studentFee.getStudentId());
        return studentFee;
    }
    
    @Override
    public Optional<StudentFee> read(String studentId) {
        Objects.requireNonNull(studentId, "Student ID cannot be null");
        return Optional.ofNullable(feeRepository.get(studentId));
    }
    
    @Override
    public StudentFee update(StudentFee studentFee) {
        Objects.requireNonNull(studentFee, "StudentFee cannot be null");
        
        String studentId = studentFee.getStudentId();
        if (!feeRepository.containsKey(studentId)) {
            throw new IllegalArgumentException("Fee record for student " + studentId + " not found");
        }
        
        feeRepository.put(studentId, studentFee);
        markAsModified();
        
        logger.logINFO("Student fee record updated for: " + studentId);
        return studentFee;
    }
    
    @Override
    public boolean delete(String studentId) {
        Objects.requireNonNull(studentId, "Student ID cannot be null");
        
        StudentFee removed = feeRepository.remove(studentId);
        if (removed != null) {
            markAsModified();
            logger.logINFO("Student fee record deleted for: " + studentId);
            return true;
        }
        return false;
    }
    
    @Override
    public boolean exists(String studentId) {
        return feeRepository.containsKey(studentId);
    }
    
    // ==================== BULK OPERATIONS ====================
    
    @Override
    public List<StudentFee> findAll() {
        return Collections.unmodifiableList(new ArrayList<>(feeRepository.values()));
    }
    
    @Override
    public long count() {
        return feeRepository.size();
    }
    
    @Override
    public void deleteAll() {
        feeRepository.clear();
        markAsModified();
        logger.logINFO("All student fee records deleted");
    }
    
    // ==================== REPOSITORY MANAGEMENT ====================
    
    @Override
    public void saveToStorage() {
        if (!hasUnsavedChanges) {
            logger.logINFO("No fee changes to save");
            return;
        }
        
        try {
            persistFeeData();
            persistPaymentHistory();
            hasUnsavedChanges = false;
            logger.logINFO("Student fee repository saved successfully");
        } catch (IOException e) {
            logger.logERROR("Failed to save fee repository: " + e.getMessage());
            throw new RepositoryException("Failed to save fee data", e);
        }
    }
    
    @Override
    public void loadFromStorage() {
        loadGradeFees();
        loadFeeData();
        loadPaymentHistory();
        hasUnsavedChanges = false;
    }
    
    @Override
    public boolean hasUnsavedChanges() {
        return hasUnsavedChanges;
    }
    
    // ==================== QUERY OPERATIONS ====================
    
    @Override
    public List<StudentFee> findByAttribute(String attribute, Object value) {
        Objects.requireNonNull(attribute, "Attribute cannot be null");
        Objects.requireNonNull(value, "Value cannot be null");
        
        Predicate<StudentFee> predicate = createAttributePredicate(attribute, value);
        List<StudentFee> results = feeRepository.values().stream()
                .filter(predicate)
                .collect(Collectors.toList());
        
        logger.logINFO(String.format("Fee query [%s = '%s'] found %d matches", attribute, value, results.size()));
        return Collections.unmodifiableList(results);
    }
    
    @Override
    public List<StudentFee> search(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        String term = searchTerm.toLowerCase();
        Predicate<StudentFee> searchPredicate = fee ->
            fee.getStudentName().toLowerCase().contains(term) ||
            fee.getStudentId().toLowerCase().contains(term) ||
            fee.getPaymentReference() != null && fee.getPaymentReference().toLowerCase().contains(term);
        
        List<StudentFee> results = feeRepository.values().stream()
                .filter(searchPredicate)
                .collect(Collectors.toList());
        
        logger.logINFO("Fee search found " + results.size() + " matches for: " + searchTerm);
        return Collections.unmodifiableList(results);
    }
    
    // ==================== BUSINESS-SPECIFIC OPERATIONS ====================
    
    public StudentFee createFeeRecord(Student student, BigDecimal initialPayment) {
        Objects.requireNonNull(student, "Student cannot be null");
        
        String grade = student.getGrade();
        BigDecimal gradeFee = gradeFeeMapper.getOrDefault(grade, BigDecimal.ZERO);
        
        StudentFee feeRecord = new StudentFee.Builder()
            .withStudent(student)
            .withTotalFee(gradeFee)
            .withAmountPaid(initialPayment != null ? initialPayment : BigDecimal.ZERO)
            .withLastPaymentDate(initialPayment != null && initialPayment.compareTo(BigDecimal.ZERO) > 0 ? 
                                LocalDate.now() : null)
            .build();
        
        return create(feeRecord);
    }
    
    public StudentFee processPayment(String studentId, BigDecimal amount, String reference) {
        Objects.requireNonNull(studentId, "Student ID cannot be null");
        Objects.requireNonNull(amount, "Amount cannot be null");
        
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be positive");
        }
        
        StudentFee existingFee = feeRepository.get(studentId);
        if (existingFee == null) {
            throw new IllegalArgumentException("No fee record found for student: " + studentId);
        }
        
        StudentFee updatedFee = existingFee.makePayment(amount, reference);
        feeRepository.put(studentId, updatedFee);
        
        // Record payment history
        PaymentRecord paymentRecord = new PaymentRecord(
            studentId, amount, updatedFee.getAmountOwed(), 
            LocalDate.now(), reference, updatedFee.isFullyPaid()
        );
        paymentHistory.add(paymentRecord);
        
        markAsModified();
        logger.logINFO(String.format("Payment processed for student %s: $%.2f", studentId, amount));
        
        return updatedFee;
    }
    
    public List<StudentFee> findOverdueFees() {
        return feeRepository.values().stream()
                .filter(fee -> !fee.isFullyPaid())
                .sorted(Comparator.comparing(StudentFee::getAmountOwed).reversed())
                .collect(Collectors.toList());
    }
    
    public List<StudentFee> findFullyPaidFees() {
        return feeRepository.values().stream()
                .filter(StudentFee::isFullyPaid)
                .collect(Collectors.toList());
    }
    
    public Map<StudentFee.PaymentStatus, Long> getFeeStatistics() {
        return feeRepository.values().stream()
                .collect(Collectors.groupingBy(StudentFee::getPaymentStatus, Collectors.counting()));
    }
    
    public BigDecimal getTotalFeesOutstanding() {
        return feeRepository.values().stream()
                .map(StudentFee::getAmountOwed)
                .filter(amount -> amount.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    public BigDecimal getTotalFeesCollected() {
        return feeRepository.values().stream()
                .map(StudentFee::getAmountPaid)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    public List<PaymentRecord> getPaymentHistory(String studentId) {
        return paymentHistory.stream()
                .filter(record -> record.studentId().equals(studentId))
                .sorted(Comparator.comparing(PaymentRecord::paymentDate).reversed())
                .collect(Collectors.toList());
    }
    
    public List<PaymentRecord> getPaymentHistory(LocalDate fromDate, LocalDate toDate) {
        return paymentHistory.stream()
                .filter(record -> !record.paymentDate().isBefore(fromDate) && 
                                 !record.paymentDate().isAfter(toDate))
                .sorted(Comparator.comparing(PaymentRecord::paymentDate).reversed())
                .collect(Collectors.toList());
    }
    
    public void updateGradeFee(String grade, BigDecimal fee) {
        Objects.requireNonNull(grade, "Grade cannot be null");
        Objects.requireNonNull(fee, "Fee cannot be null");
        
        if (fee.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Grade fee cannot be negative");
        }
        
        gradeFeeMapper.put(grade, fee);
        saveGradeFees();
        logger.logINFO("Grade fee updated: " + grade + " = $" + fee);
    }
    
    public BigDecimal getGradeFee(String grade) {
        return gradeFeeMapper.getOrDefault(grade, BigDecimal.ZERO);
    }
    
    public Map<String, BigDecimal> getAllGradeFees() {
        return Collections.unmodifiableMap(gradeFeeMapper);
    }
    
    // ==================== PRIVATE METHODS ====================
    
    private void initializeRepository() {
        loadGradeFees();
        loadFeeData();
        loadPaymentHistory();
    }
    
    private void loadFeeData() {
        feeRepository.clear();
        
        File file = new File(STUDENT_FEE_FILE);
        if (!file.exists()) {
            logger.logINFO("Student fee file not found, starting with empty repository");
            return;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int loadedCount = 0;
            
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    try {
                        String decryptedData = crypter.decodeDecrypt(line.trim());
                        StudentFee fee = StudentFee.fromCsv(decryptedData);
                        feeRepository.put(fee.getStudentId(), fee);
                        loadedCount++;
                    } catch (Exception e) {
                        logger.logERROR("Failed to parse fee data: " + e.getMessage());
                    }
                }
            }
            
            logger.logINFO("Loaded " + loadedCount + " student fee records");
        } catch (IOException e) {
            logger.logERROR("Failed to load fee data: " + e.getMessage());
            throw new RepositoryException("Failed to load fee data", e);
        }
    }
    
    private void persistFeeData() throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(STUDENT_FEE_FILE, false))) {
            for (StudentFee fee : feeRepository.values()) {
                try {
                    String csvData = fee.toString();
                    String encryptedData = crypter.encryptEncode(csvData);
                    writer.println(encryptedData);
                } catch (Exception e) {
                    logger.logERROR("Failed to encrypt fee data for student: " + fee.getStudentId());
                    throw new IOException("Encryption failed for student: " + fee.getStudentId(), e);
                }
            }
        }
    }
    
    private void loadGradeFees() {
        gradeFeeMapper.clear();
        
        File file = new File(GRADE_FEE_FILE);
        if (!file.exists()) {
            logger.logINFO("Grade fee file not found, using default fees");
            initializeDefaultGradeFees();
            return;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    String[] data = line.split(",");
                    if (data.length >= 2) {
                        gradeFeeMapper.put(data[0], new BigDecimal(data[1]));
                    }
                }
            }
            logger.logINFO("Loaded grade fees for " + gradeFeeMapper.size() + " grades");
        } catch (IOException e) {
            logger.logERROR("Failed to load grade fees: " + e.getMessage());
            initializeDefaultGradeFees();
        }
    }
    
    private void saveGradeFees() throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(GRADE_FEE_FILE, false))) {
            for (Map.Entry<String, BigDecimal> entry : gradeFeeMapper.entrySet()) {
                writer.println(entry.getKey() + "," + entry.getValue());
            }
        }
    }
    
    private void initializeDefaultGradeFees() {
        // Set some reasonable default fees
        gradeFeeMapper.put("FRESHMAN", new BigDecimal("1000.00"));
        gradeFeeMapper.put("SOPHOMORE", new BigDecimal("1100.00"));
        gradeFeeMapper.put("JUNIOR", new BigDecimal("1200.00"));
        gradeFeeMapper.put("SENIOR", new BigDecimal("1300.00"));
    }
    
    private void loadPaymentHistory() {
        paymentHistory.clear();
        
        File file = new File(PAYMENT_HISTORY_FILE);
        if (!file.exists()) {
            logger.logINFO("Payment history file not found, starting with empty history");
            return;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    try {
                        String decryptedData = crypter.decodeDecrypt(line.trim());
                        paymentHistory.add(PaymentRecord.fromCsv(decryptedData));
                    } catch (Exception e) {
                        logger.logERROR("Failed to parse payment history: " + e.getMessage());
                    }
                }
            }
            logger.logINFO("Loaded " + paymentHistory.size() + " payment records");
        } catch (IOException e) {
            logger.logERROR("Failed to load payment history: " + e.getMessage());
        }
    }
    
    private void persistPaymentHistory() throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(PAYMENT_HISTORY_FILE, false))) {
            for (PaymentRecord record : paymentHistory) {
                try {
                    String csvData = record.toCsv();
                    String encryptedData = crypter.encryptEncode(csvData);
                    writer.println(encryptedData);
                } catch (Exception e) {
                    logger.logERROR("Failed to encrypt payment history record");
                    throw new IOException("Encryption failed for payment record", e);
                }
            }
        }
    }
    
    private Predicate<StudentFee> createAttributePredicate(String attribute, Object value) {
        switch (attribute.toUpperCase()) {
            case "STUDENT_ID":
                return fee -> fee.getStudentId().equals(value);
            case "STUDENT_NAME":
                return fee -> fee.getStudentName().equalsIgnoreCase(value.toString());
            case "PAYMENT_STATUS":
                return fee -> fee.getPaymentStatus() == StudentFee.PaymentStatus.valueOf(value.toString());
            case "FULLY_PAID":
                return fee -> fee.isFullyPaid() == Boolean.parseBoolean(value.toString());
            default:
                throw new IllegalArgumentException("Unsupported fee attribute: " + attribute);
        }
    }
    
    private void markAsModified() {
        this.hasUnsavedChanges = true;
    }
    
    // ==================== PAYMENT RECORD INNER CLASS ====================
    
    public record PaymentRecord(
        String studentId,
        BigDecimal amountPaid,
        BigDecimal balanceAfter,
        LocalDate paymentDate,
        String reference,
        boolean fullyPaid
    ) {
        public static PaymentRecord fromCsv(String csvLine) {
            String[] data = csvLine.split(",");
            if (data.length < 6) {
                throw new IllegalArgumentException("Invalid CSV format for PaymentRecord");
            }
            
            return new PaymentRecord(
                data[0],
                new BigDecimal(data[1]),
                new BigDecimal(data[2]),
                LocalDate.parse(data[3]),
                data[4],
                Boolean.parseBoolean(data[5])
            );
        }
        
        public String toCsv() {
            return String.format("%s,%s,%s,%s,%s,%s",
                studentId, amountPaid, balanceAfter, paymentDate, reference, fullyPaid);
        }
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





