package SchoolManager;

import java.io.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class StaffRepoManager implements RepoManager<Staff, String> {
    private static final String STAFF_REPO_FILE = "staff_repo.txt";
    
    private final Crypter crypter;
    private final Logger logger;
    private final AuthGenerator authGenerator;
    private final List<Staff> staffRepository;
    private final Map<String, Staff> staffById;
    private volatile boolean hasUnsavedChanges;
    
    public StaffRepoManager(Crypter crypter, Logger logger, AuthGenerator authGenerator) {
        this.crypter = Objects.requireNonNull(crypter, "Crypter cannot be null");
        this.logger = Objects.requireNonNull(logger, "Logger cannot be null");
        this.authGenerator = Objects.requireNonNull(authGenerator, "AuthGenerator cannot be null");
        this.staffRepository = new CopyOnWriteArrayList<>();
        this.staffById = new HashMap<>();
        this.hasUnsavedChanges = false;
        
        loadFromStorage();
    }
    
    // ==================== CRUD OPERATIONS ====================
    
    @Override
    public Staff create(Staff staff) {
        Objects.requireNonNull(staff, "Staff cannot be null");
        
        if (staffById.containsKey(staff.getStaffId())) {
            throw new IllegalArgumentException("Staff with ID " + staff.getStaffId() + " already exists");
        }
        
        staffRepository.add(staff);
        staffById.put(staff.getStaffId(), staff);
        markAsModified();
        
        logger.logINFO("Staff created: " + staff.getStaffId() + " - " + staff.getName());
        return staff;
    }
    
    @Override
    public Optional<Staff> read(String staffId) {
        Objects.requireNonNull(staffId, "Staff ID cannot be null");
        return Optional.ofNullable(staffById.get(staffId));
    }
    
    @Override
    public Staff update(Staff staff) {
        Objects.requireNonNull(staff, "Staff cannot be null");
        
        String staffId = staff.getStaffId();
        Staff existing = staffById.get(staffId);
        if (existing == null) {
            throw new IllegalArgumentException("Staff with ID " + staffId + " not found");
        }
        
        staffRepository.replaceAll(s -> s.getStaffId().equals(staffId) ? staff : s);
        staffById.put(staffId, staff);
        markAsModified();
        
        logger.logINFO("Staff updated: " + staffId);
        return staff;
    }
    
    @Override
    public boolean delete(String staffId) {
        Objects.requireNonNull(staffId, "Staff ID cannot be null");
        
        Staff removed = staffById.remove(staffId);
        if (removed != null) {
            staffRepository.removeIf(s -> s.getStaffId().equals(staffId));
            markAsModified();
            logger.logINFO("Staff deleted: " + staffId);
            return true;
        }
        return false;
    }
    
    @Override
    public boolean exists(String staffId) {
        return staffById.containsKey(staffId);
    }
    
    // ==================== BULK OPERATIONS ====================
    
    @Override
    public List<Staff> findAll() {
        return Collections.unmodifiableList(staffRepository);
    }
    
    @Override
    public long count() {
        return staffRepository.size();
    }
    
    @Override
    public void deleteAll() {
        staffRepository.clear();
        staffById.clear();
        markAsModified();
        logger.logINFO("All staff records deleted from repository");
    }
    
    // ==================== REPOSITORY MANAGEMENT ====================
    
    @Override
    public void saveToStorage() {
        if (!hasUnsavedChanges) {
            logger.logINFO("No staff changes to save");
            return;
        }
        
        try {
            persistStaffData();
            hasUnsavedChanges = false;
            logger.logINFO("Staff repository saved successfully");
        } catch (IOException e) {
            logger.logERROR("Failed to save staff repository: " + e.getMessage());
            throw new RepositoryException("Failed to save staff data", e);
        }
    }
    
    @Override
    public void loadFromStorage() {
        staffRepository.clear();
        staffById.clear();
        
        File file = new File(STAFF_REPO_FILE);
        if (!file.exists()) {
            logger.logINFO("Staff repository file not found, starting with empty repository");
            return;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int loadedCount = 0;
            
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    try {
                        String decryptedData = crypter.decodeDecrypt(line.trim());
                        Staff staff = Staff.fromCsv(decryptedData);
                        
                        staffRepository.add(staff);
                        staffById.put(staff.getStaffId(), staff);
                        loadedCount++;
                    } catch (Exception e) {
                        logger.logERROR("Failed to parse staff data: " + e.getMessage());
                    }
                }
            }
            
            logger.logINFO("Loaded " + loadedCount + " staff members from repository");
            hasUnsavedChanges = false;
        } catch (IOException e) {
            logger.logERROR("Failed to load staff repository: " + e.getMessage());
            throw new RepositoryException("Failed to load staff data", e);
        }
    }
    
    @Override
    public boolean hasUnsavedChanges() {
        return hasUnsavedChanges;
    }
    
    // ==================== QUERY OPERATIONS ====================
    
    @Override
    public List<Staff> findByAttribute(String attribute, Object value) {
        Objects.requireNonNull(attribute, "Attribute cannot be null");
        Objects.requireNonNull(value, "Value cannot be null");
        
        Predicate<Staff> predicate = createAttributePredicate(attribute, value);
        List<Staff> results = staffRepository.stream()
                .filter(predicate)
                .collect(Collectors.toList());
        
        logger.logINFO(String.format("Staff query [%s = '%s'] found %d matches", attribute, value, results.size()));
        return Collections.unmodifiableList(results);
    }
    
    @Override
    public List<Staff> search(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        String term = searchTerm.toLowerCase();
        Predicate<Staff> searchPredicate = staff ->
            staff.getName().toLowerCase().contains(term) ||
            staff.getDesignation().name().toLowerCase().contains(term) ||
            staff.getDesignation().getDisplayName().toLowerCase().contains(term) ||
            staff.getPhone().contains(term) ||
            staff.getAddress().toLowerCase().contains(term) ||
            staff.getGender().getCode().toLowerCase().contains(term) ||
            String.valueOf(staff.getAge()).contains(term);
        
        List<Staff> results = staffRepository.stream()
                .filter(searchPredicate)
                .collect(Collectors.toList());
        
        logger.logINFO("Staff search found " + results.size() + " matches for: " + searchTerm);
        return Collections.unmodifiableList(results);
    }
    
    // ==================== BUSINESS-SPECIFIC QUERIES ====================
    
    public List<Staff> findByDesignation(Staff.Designation designation) {
        return staffRepository.stream()
                .filter(s -> s.getDesignation() == designation)
                .collect(Collectors.toList());
    }
    
    public List<Staff> findByGender(Staff.Gender gender) {
        return staffRepository.stream()
                .filter(s -> s.getGender() == gender)
                .collect(Collectors.toList());
    }
    
    public List<Staff> findSeniorStaff() {
        return staffRepository.stream()
                .filter(Staff::isSeniorStaff)
                .sorted(Comparator.comparing(Staff::getSalary).reversed())
                .collect(Collectors.toList());
    }
    
    public List<Staff> findBySalaryRange(double minSalary, double maxSalary) {
        if (minSalary < 0 || maxSalary < minSalary) {
            throw new IllegalArgumentException("Invalid salary range");
        }
        
        return staffRepository.stream()
                .filter(s -> s.getSalary() >= minSalary && s.getSalary() <= maxSalary)
                .sorted(Comparator.comparingDouble(Staff::getSalary).reversed())
                .collect(Collectors.toList());
    }
    
    public List<Staff> findStaffWithAuth() {
        return staffRepository.stream()
                .filter(Staff::requiresAuth)
                .collect(Collectors.toList());
    }
    
    public Map<Staff.Designation, Long> getStaffCountByDesignation() {
        return staffRepository.stream()
                .collect(Collectors.groupingBy(Staff::getDesignation, Collectors.counting()));
    }
    
    public Map<Staff.Designation, Double> getTotalSalaryByDesignation() {
        return staffRepository.stream()
                .collect(Collectors.groupingBy(
                    Staff::getDesignation,
                    Collectors.summingDouble(Staff::getSalary)
                ));
    }
    
    public double getTotalSalaryExpenditure() {
        return staffRepository.stream()
                .mapToDouble(Staff::getSalary)
                .sum();
    }
    
    // ==================== AUTH MANAGEMENT ====================
    
    public String generateStaffAuth() {
        String auth = authGenerator.generateAuth();
        logger.logINFO("Generated new staff authentication code");
        return auth;
    }
    
    public boolean grantAuthAccess(String staffId, String password) {
        Optional<Staff> staffOpt = read(staffId);
        if (staffOpt.isPresent()) {
            Staff staff = staffOpt.get();
            Staff updated = staff.copyWithModifications(builder -> 
                builder.withAuthPassword(password != null ? password : generateStaffAuth())
            );
            update(updated);
            logger.logINFO("Auth access granted to staff: " + staffId);
            return true;
        }
        return false;
    }
    
    public boolean revokeAuthAccess(String staffId) {
        Optional<Staff> staffOpt = read(staffId);
        if (staffOpt.isPresent()) {
            Staff staff = staffOpt.get();
            Staff updated = staff.copyWithModifications(builder -> 
                builder.withAuthPassword("NONE")
            );
            update(updated);
            logger.logINFO("Auth access revoked for staff: " + staffId);
            return true;
        }
        return false;
    }
    
    // ==================== PRIVATE METHODS ====================
    
    private void persistStaffData() throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(STAFF_REPO_FILE, false))) {
            for (Staff staff : staffRepository) {
                try {
                    String csvData = staff.toString();
                    String encryptedData = crypter.encryptEncode(csvData);
                    writer.println(encryptedData);
                } catch (Exception e) {
                    logger.logERROR("Failed to encrypt staff data for ID: " + staff.getStaffId());
                    throw new IOException("Encryption failed for staff: " + staff.getStaffId(), e);
                }
            }
        }
    }
    
    private Predicate<Staff> createAttributePredicate(String attribute, Object value) {
        switch (attribute.toUpperCase()) {
            case "ID":
                return s -> s.getStaffId().equals(value);
            case "NAME":
                return s -> s.getName().equalsIgnoreCase(value.toString());
            case "DESIGNATION":
                return s -> s.getDesignation() == Staff.Designation.valueOf(value.toString().toUpperCase());
            case "GENDER":
                return s -> s.getGender() == Staff.Gender.fromCode(value.toString());
            case "AGE":
                try {
                    int ageValue = Integer.parseInt(value.toString());
                    return s -> s.getAge() == ageValue;
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid age value: " + value);
                }
            case "PHONE":
                return s -> s.getPhone().equals(value.toString());
            case "ADDRESS":
                return s -> s.getAddress().equalsIgnoreCase(value.toString());
            case "AUTH":
                return s -> s.getAuthPassword().equals(value.toString());
            case "SALARY":
                try {
                    double salaryValue = Double.parseDouble(value.toString());
                    return s -> s.getSalary() == salaryValue;
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid salary value: " + value);
                }
            default:
                throw new IllegalArgumentException("Unsupported staff attribute: " + attribute);
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


