package SchoolManager;

import java.io.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class InventoryRepoManager implements RepoManager<Inventory, String> {
    private static final String INVENTORY_FILE = "inventory_repo.txt";
    
    private final Crypter crypter;
    private final Logger logger;
    private final Map<String, Inventory> inventoryRepository;
    private volatile boolean hasUnsavedChanges;
    
    public InventoryRepoManager(Crypter crypter, Logger logger) {
        this.crypter = Objects.requireNonNull(crypter, "Crypter cannot be null");
        this.logger = Objects.requireNonNull(logger, "Logger cannot be null");
        this.inventoryRepository = new ConcurrentHashMap<>();
        this.hasUnsavedChanges = false;
        
        loadFromStorage();
    }
    
    // ==================== CRUD OPERATIONS ====================
    
    @Override
    public Inventory create(Inventory inventory) {
        Objects.requireNonNull(inventory, "Inventory cannot be null");
        
        if (inventoryRepository.containsKey(inventory.getInventoryId())) {
            throw new IllegalArgumentException("Inventory item with ID " + inventory.getInventoryId() + " already exists");
        }
        
        inventoryRepository.put(inventory.getInventoryId(), inventory);
        markAsModified();
        
        logger.info("Inventory item created: " + inventory.getInventoryId() + " - " + inventory.getItemName());
        return inventory;
    }
    
    @Override
    public Optional<Inventory> read(String inventoryId) {
        Objects.requireNonNull(inventoryId, "Inventory ID cannot be null");
        return Optional.ofNullable(inventoryRepository.get(inventoryId));
    }
    
    @Override
    public Inventory update(Inventory inventory) {
        Objects.requireNonNull(inventory, "Inventory cannot be null");
        
        String inventoryId = inventory.getInventoryId();
        if (!inventoryRepository.containsKey(inventoryId)) {
            throw new IllegalArgumentException("Inventory item with ID " + inventoryId + " not found");
        }
        
        inventoryRepository.put(inventoryId, inventory);
        markAsModified();
        
        logger.info("Inventory item updated: " + inventoryId);
        return inventory;
    }
    
    @Override
    public boolean delete(String inventoryId) {
        Objects.requireNonNull(inventoryId, "Inventory ID cannot be null");
        
        Inventory removed = inventoryRepository.remove(inventoryId);
        if (removed != null) {
            markAsModified();
            logger.info("Inventory item deleted: " + inventoryId);
            return true;
        }
        return false;
    }
    
    @Override
    public boolean exists(String inventoryId) {
        return inventoryRepository.containsKey(inventoryId);
    }
    
    // ==================== BULK OPERATIONS ====================
    
    @Override
    public List<Inventory> findAll() {
        return Collections.unmodifiableList(new ArrayList<>(inventoryRepository.values()));
    }
    
    @Override
    public long count() {
        return inventoryRepository.size();
    }
    
    @Override
    public void deleteAll() {
        inventoryRepository.clear();
        markAsModified();
        logger.info("All inventory items deleted");
    }
    
    // ==================== REPOSITORY MANAGEMENT ====================
    
    @Override
    public void saveToStorage() {
        if (!hasUnsavedChanges) {
            logger.info("No inventory changes to save");
            return;
        }
        
        try {
            persistInventoryData();
            hasUnsavedChanges = false;
            logger.info("Inventory repository saved successfully");
        } catch (IOException e) {
            logger.error("Failed to save inventory repository: " + e.getMessage(), e);
            throw new RepositoryException("Failed to save inventory data", e);
        }
    }
    
    @Override
    public void loadFromStorage() {
        inventoryRepository.clear();
        
        File file = new File(INVENTORY_FILE);
        if (!file.exists()) {
            logger.info("Inventory file not found, starting with empty repository");
            return;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int loadedCount = 0;
            
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    try {
                        String decryptedData = crypter.decodeDecrypt(line.trim());
                        Inventory inventory = Inventory.fromCsv(decryptedData);
                        inventoryRepository.put(inventory.getInventoryId(), inventory);
                        loadedCount++;
                    } catch (Exception e) {
                        logger.error("Failed to parse inventory data: " + e.getMessage(), e);
                    }
                }
            }
            
            logger.info("Loaded " + loadedCount + " inventory items from repository");
            hasUnsavedChanges = false;
        } catch (IOException e) {
            logger.error("Failed to load inventory repository: " + e.getMessage(), e);
            throw new RepositoryException("Failed to load inventory data", e);
        }
    }
    
    @Override
    public boolean hasUnsavedChanges() {
        return hasUnsavedChanges;
    }
    
    // ==================== QUERY OPERATIONS ====================
    
    @Override
    public List<Inventory> findByAttribute(String attribute, Object value) {
        Objects.requireNonNull(attribute, "Attribute cannot be null");
        Objects.requireNonNull(value, "Value cannot be null");
        
        Predicate<Inventory> predicate = createAttributePredicate(attribute, value);
        List<Inventory> results = inventoryRepository.values().stream()
                .filter(predicate)
                .collect(Collectors.toList());
        
        logger.info(String.format("Inventory query [%s = '%s'] found %d matches", attribute, value, results.size()));
        return Collections.unmodifiableList(results);
    }
    
    @Override
    public List<Inventory> search(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        String term = searchTerm.toLowerCase();
        Predicate<Inventory> searchPredicate = inventory ->
            inventory.getItemName().toLowerCase().contains(term) ||
            inventory.getDescription().toLowerCase().contains(term) ||
            inventory.getSupplier().toLowerCase().contains(term) ||
            inventory.getLocation().toLowerCase().contains(term) ||
            inventory.getCategory().getDisplayName().toLowerCase().contains(term) ||
            inventory.getCategory().name().toLowerCase().contains(term);
        
        List<Inventory> results = inventoryRepository.values().stream()
                .filter(searchPredicate)
                .collect(Collectors.toList());
        
        logger.info("Inventory search found " + results.size() + " matches for: " + searchTerm);
        return Collections.unmodifiableList(results);
    }
    
    // ==================== BUSINESS-SPECIFIC OPERATIONS ====================
    
    public List<Inventory> findByCategory(Inventory.InventoryCategory category) {
        return inventoryRepository.values().stream()
                .filter(item -> item.getCategory() == category)
                .collect(Collectors.toList());
    }
    
    public List<Inventory> findByStatus(Inventory.InventoryStatus status) {
        return inventoryRepository.values().stream()
                .filter(item -> item.getStatus() == status)
                .collect(Collectors.toList());
    }
    
    public List<Inventory> findLowStockItems() {
        return inventoryRepository.values().stream()
                .filter(Inventory::isLowStock)
                .sorted(Comparator.comparingInt(Inventory::getStockDeficit).reversed())
                .collect(Collectors.toList());
    }
    
    public List<Inventory> findOutOfStockItems() {
        return inventoryRepository.values().stream()
                .filter(Inventory::isOutOfStock)
                .collect(Collectors.toList());
    }
    
    public List<Inventory> findItemsNeedingReorder() {
        return inventoryRepository.values().stream()
                .filter(Inventory::needsReordering)
                .sorted(Comparator.comparingInt(Inventory::getStockDeficit).reversed())
                .collect(Collectors.toList());
    }
    
    public List<Inventory> findItemsBySupplier(String supplier) {
        return inventoryRepository.values().stream()
                .filter(item -> item.getSupplier().equalsIgnoreCase(supplier))
                .collect(Collectors.toList());
    }
    
    public List<Inventory> findItemsByLocation(String location) {
        return inventoryRepository.values().stream()
                .filter(item -> item.getLocation().equalsIgnoreCase(location))
                .collect(Collectors.toList());
    }
    
    public Map<Inventory.InventoryCategory, Long> getInventoryCountByCategory() {
        return inventoryRepository.values().stream()
                .collect(Collectors.groupingBy(Inventory::getCategory, Collectors.counting()));
    }
    
    public Map<Inventory.InventoryStatus, Long> getInventoryCountByStatus() {
        return inventoryRepository.values().stream()
                .collect(Collectors.groupingBy(Inventory::getStatus, Collectors.counting()));
    }
    
    public double getTotalInventoryValue() {
        return inventoryRepository.values().stream()
                .mapToDouble(Inventory::getTotalValue)
                .sum();
    }
    
    public Map<Inventory.InventoryCategory, Double> getInventoryValueByCategory() {
        return inventoryRepository.values().stream()
                .collect(Collectors.groupingBy(
                    Inventory::getCategory,
                    Collectors.summingDouble(Inventory::getTotalValue)
                ));
    }
    
    public Inventory addStock(String inventoryId, int quantityToAdd, String supplier) {
        Objects.requireNonNull(inventoryId, "Inventory ID cannot be null");
        
        Inventory existing = inventoryRepository.get(inventoryId);
        if (existing == null) {
            throw new IllegalArgumentException("Inventory item not found: " + inventoryId);
        }
        
        Inventory updated = existing.addStock(quantityToAdd, supplier);
        inventoryRepository.put(inventoryId, updated);
        markAsModified();
        
        logger.info(String.format("Added %d units to inventory item %s. New quantity: %d", 
            quantityToAdd, inventoryId, updated.getQuantity()));
        
        return updated;
    }
    
    public Inventory removeStock(String inventoryId, int quantityToRemove, String reason) {
        Objects.requireNonNull(inventoryId, "Inventory ID cannot be null");
        
        Inventory existing = inventoryRepository.get(inventoryId);
        if (existing == null) {
            throw new IllegalArgumentException("Inventory item not found: " + inventoryId);
        }
        
        Inventory updated = existing.removeStock(quantityToRemove, reason);
        inventoryRepository.put(inventoryId, updated);
        markAsModified();
        
        logger.info(String.format("Removed %d units from inventory item %s. Reason: %s", 
            quantityToRemove, inventoryId, reason));
        
        return updated;
    }
    
    // ==================== PRIVATE METHODS ====================
    
    private void persistInventoryData() throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(INVENTORY_FILE, false))) {
            for (Inventory inventory : inventoryRepository.values()) {
                try {
                    String csvData = inventory.toString();
                    String encryptedData = crypter.encryptEncode(csvData);
                    writer.println(encryptedData);
                } catch (Exception e) {
                    logger.error("Failed to encrypt inventory data for ID: " + inventory.getInventoryId(), e);
                    throw new IOException("Encryption failed for inventory: " + inventory.getInventoryId(), e);
                }
            }
        }
    }
    
    private Predicate<Inventory> createAttributePredicate(String attribute, Object value) {
        switch (attribute.toUpperCase()) {
            case "ID":
                return item -> item.getInventoryId().equals(value);
            case "NAME":
            case "ITEMNAME":
                return item -> item.getItemName().equalsIgnoreCase(value.toString());
            case "CATEGORY":
                return item -> item.getCategory() == Inventory.InventoryCategory.fromString(value.toString());
            case "STATUS":
                return item -> item.getStatus() == Inventory.InventoryStatus.valueOf(value.toString().toUpperCase());
            case "SUPPLIER":
                return item -> item.getSupplier().equalsIgnoreCase(value.toString());
            case "LOCATION":
                return item -> item.getLocation().equalsIgnoreCase(value.toString());
            case "QUANTITY":
                try {
                    int quantityValue = Integer.parseInt(value.toString());
                    return item -> item.getQuantity() == quantityValue;
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid quantity value: " + value);
                }
            case "MINIMUMSTOCK":
                try {
                    int minStockValue = Integer.parseInt(value.toString());
                    return item -> item.getMinimumStockLevel() == minStockValue;
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid minimum stock value: " + value);
                }
            default:
                throw new IllegalArgumentException("Unsupported inventory attribute: " + attribute);
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