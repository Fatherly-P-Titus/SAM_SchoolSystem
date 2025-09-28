package SchoolManager;

import java.io.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ExpenseRepoManager implements RepoManager<Expense, String> {
    private static final String EXPENSE_REPO_FILE = "expenses_records.txt";
    
    private final Crypter crypter;
    private final Logger logger;
    private final List<Expense> expenses;
    private final Map<String, Expense> expenseById;
    private volatile boolean hasUnsavedChanges;
    
    public ExpenseRepoManager(Crypter crypter, Logger logger) {
        this.crypter = Objects.requireNonNull(crypter, "Crypter cannot be null");
        this.logger = Objects.requireNonNull(logger, "Logger cannot be null");
        this.expenses = new CopyOnWriteArrayList<>();
        this.expenseById = new HashMap<>();
        this.hasUnsavedChanges = false;
        
        loadFromStorage();
    }
    
    // ==================== CRUD OPERATIONS ====================
    
    @Override
    public Expense create(Expense expense) {
        Objects.requireNonNull(expense, "Expense cannot be null");
        
        if (expenseById.containsKey(expense.getExpenseId())) {
            throw new IllegalArgumentException("Expense with ID " + expense.getExpenseId() + " already exists");
        }
        
        expenses.add(expense);
        expenseById.put(expense.getExpenseId(), expense);
        markAsModified();
        
        logger.logINFO("Expense created: " + expense.getExpenseId());
        return expense;
    }
    
    @Override
    public Optional<Expense> read(String expenseId) {
        Objects.requireNonNull(expenseId, "Expense ID cannot be null");
        return Optional.ofNullable(expenseById.get(expenseId));
    }
    
    @Override
    public Expense update(Expense expense) {
        Objects.requireNonNull(expense, "Expense cannot be null");
        
        String expenseId = expense.getExpenseId();
        Expense existing = expenseById.get(expenseId);
        if (existing == null) {
            throw new IllegalArgumentException("Expense with ID " + expenseId + " not found");
        }
        
        // Replace in both collections
        expenses.replaceAll(e -> e.getExpenseId().equals(expenseId) ? expense : e);
        expenseById.put(expenseId, expense);
        markAsModified();
        
        logger.logINFO("Expense updated: " + expenseId);
        return expense;
    }
    
    @Override
    public boolean delete(String expenseId) {
        Objects.requireNonNull(expenseId, "Expense ID cannot be null");
        
        Expense removed = expenseById.remove(expenseId);
        if (removed != null) {
            expenses.removeIf(e -> e.getExpenseId().equals(expenseId));
            markAsModified();
            logger.logINFO("Expense deleted: " + expenseId);
            return true;
        }
        return false;
    }
    
    @Override
    public boolean exists(String expenseId) {
        return expenseById.containsKey(expenseId);
    }
    
    // ==================== BULK OPERATIONS ====================
    
    @Override
    public List<Expense> findAll() {
        return Collections.unmodifiableList(expenses);
    }
    
    @Override
    public long count() {
        return expenses.size();
    }
    
    @Override
    public void deleteAll() {
        expenses.clear();
        expenseById.clear();
        markAsModified();
        logger.logINFO("All expenses deleted from repository");
    }
    
    // ==================== REPOSITORY MANAGEMENT ====================
    
    @Override
    public void saveToStorage() {
        if (!hasUnsavedChanges) {
            logger.logINFO("No expense changes to save");
            return;
        }
        
        try {
            persistExpenses();
            hasUnsavedChanges = false;
            logger.logINFO("Expense repository saved successfully");
        } catch (IOException e) {
            logger.logERROR("Failed to save expense repository: " + e.getMessage());
            throw new RepositoryException("Failed to save expenses data", e);
        }
    }
    
    @Override
    public void loadFromStorage() {
        expenses.clear();
        expenseById.clear();
        
        File file = new File(EXPENSE_REPO_FILE);
        if (!file.exists()) {
            logger.logINFO("Expense repository file not found, starting with empty repository");
            return;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int loadedCount = 0;
            
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    try {
                        String decryptedData = crypter.decodeDecrypt(line.trim());
                        Expense expense = Expense.fromCsv(decryptedData);
                        
                        expenses.add(expense);
                        expenseById.put(expense.getExpenseId(), expense);
                        loadedCount++;
                    } catch (Exception e) {
                        logger.logERROR("Failed to parse expense data: " + e.getMessage());
                    }
                }
            }
            
            logger.logINFO("Loaded " + loadedCount + " expenses from repository");
            hasUnsavedChanges = false;
        } catch (IOException e) {
            logger.logERROR("Failed to load expense repository: " + e.getMessage());
            throw new RepositoryException("Failed to load expenses data", e);
        }
    }
    
    @Override
    public boolean hasUnsavedChanges() {
        return hasUnsavedChanges;
    }
    
    // ==================== QUERY OPERATIONS ====================
    
    @Override
    public List<Expense> findByAttribute(String attribute, Object value) {
        Objects.requireNonNull(attribute, "Attribute cannot be null");
        Objects.requireNonNull(value, "Value cannot be null");
        
        Predicate<Expense> predicate = createAttributePredicate(attribute, value);
        List<Expense> results = expenses.stream()
                .filter(predicate)
                .collect(Collectors.toList());
        
        logger.logINFO(String.format("Expense query [%s = '%s'] found %d matches", attribute, value, results.size()));
        return Collections.unmodifiableList(results);
    }
    
    @Override
    public List<Expense> search(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        String term = searchTerm.toLowerCase();
        Predicate<Expense> searchPredicate = expense ->
            expense.getDescription().toLowerCase().contains(term) ||
            expense.getRecipient() != null && expense.getRecipient().toLowerCase().contains(term) ||
            expense.getApprovedBy() != null && expense.getApprovedBy().toLowerCase().contains(term) ||
            expense.getNotes() != null && expense.getNotes().toLowerCase().contains(term) ||
            expense.getCategory().name().toLowerCase().contains(term) ||
            expense.getStatus().name().toLowerCase().contains(term);
        
        List<Expense> results = expenses.stream()
                .filter(searchPredicate)
                .collect(Collectors.toList());
        
        logger.logINFO("Expense search found " + results.size() + " matches for: " + searchTerm);
        return Collections.unmodifiableList(results);
    }
    
    // ==================== BUSINESS-SPECIFIC QUERIES ====================
    
    public List<Expense> findByCategory(Expense.ExpenseCategory category) {
        return expenses.stream()
                .filter(e -> e.getCategory() == category)
                .collect(Collectors.toList());
    }
    
    public List<Expense> findByStatus(Expense.ExpenseStatus status) {
        return expenses.stream()
                .filter(e -> e.getStatus() == status)
                .collect(Collectors.toList());
    }
    
    public List<Expense> findByDateRange(LocalDate startDate, LocalDate endDate) {
        Objects.requireNonNull(startDate, "Start date cannot be null");
        Objects.requireNonNull(endDate, "End date cannot be null");
        
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }
        
        return expenses.stream()
                .filter(e -> !e.getDateIncurred().isBefore(startDate) && !e.getDateIncurred().isAfter(endDate))
                .sorted(Comparator.comparing(Expense::getDateIncurred))
                .collect(Collectors.toList());
    }
    
    public List<Expense> findPendingExpenses() {
        return findByStatus(Expense.ExpenseStatus.PENDING);
    }
    
    public List<Expense> findPaidExpenses() {
        return findByStatus(Expense.ExpenseStatus.PAID);
    }
    
    public BigDecimal getTotalExpenses() {
        return expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    public BigDecimal getTotalExpensesByCategory(Expense.ExpenseCategory category) {
        return expenses.stream()
                .filter(e -> e.getCategory() == category)
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    public Map<Expense.ExpenseCategory, BigDecimal> getExpensesByCategory() {
        return expenses.stream()
                .collect(Collectors.groupingBy(
                    Expense::getCategory,
                    Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)
                ));
    }
    
    public Map<Expense.ExpenseStatus, Long> getExpenseCountByStatus() {
        return expenses.stream()
                .collect(Collectors.groupingBy(Expense::getStatus, Collectors.counting()));
    }
    
    // ==================== PRIVATE METHODS ====================
    
    private void persistExpenses() throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(EXPENSE_REPO_FILE, false))) {
            for (Expense expense : expenses) {
                try {
                    String csvData = expense.toString();
                    String encryptedData = crypter.encryptEncode(csvData);
                    writer.println(encryptedData);
                } catch (Exception e) {
                    logger.logERROR("Failed to encrypt expense data for ID: " + expense.getExpenseId());
                    throw new IOException("Encryption failed for expense: " + expense.getExpenseId(), e);
                }
            }
        }
    }
    
    private Predicate<Expense> createAttributePredicate(String attribute, Object value) {
        switch (attribute.toUpperCase()) {
            case "ID":
                return e -> e.getExpenseId().equals(value);
            case "DESCRIPTION":
                return e -> e.getDescription().equalsIgnoreCase(value.toString());
            case "CATEGORY":
                return e -> e.getCategory() == Expense.ExpenseCategory.valueOf(value.toString());
            case "STATUS":
                return e -> e.getStatus() == Expense.ExpenseStatus.valueOf(value.toString());
            case "RECIPIENT":
                return e -> e.getRecipient() != null && e.getRecipient().equalsIgnoreCase(value.toString());
            case "APPROVEDBY":
                return e -> e.getApprovedBy() != null && e.getApprovedBy().equalsIgnoreCase(value.toString());
            case "AMOUNT":
                try {
                    BigDecimal amountValue = new BigDecimal(value.toString());
                    return e -> e.getAmount().compareTo(amountValue) == 0;
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid amount value: " + value);
                }
            default:
                throw new IllegalArgumentException("Unsupported expense attribute: " + attribute);
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






