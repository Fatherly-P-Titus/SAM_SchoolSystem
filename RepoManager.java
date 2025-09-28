package SchoolManager;

import java.util.List;
import java.util.Optional;

/**
 * Generic repository interface for CRUD operations on all entity types
 * @param <T> The entity type this repository manages
 * @param <ID> The type of the entity's identifier
 */
public interface RepoManager<T, ID> {
    
    // ==================== CRUD OPERATIONS ====================
    
    /**
     * Save a new entity to the repository
     */
    T create(T entity);
    
    /**
     * Find entity by ID
     */
    Optional<T> read(ID id);
    
    /**
     * Update an existing entity
     */
    T update(T entity);
    
    /**
     * Delete an entity by ID
     */
    boolean delete(ID id);
    
    /**
     * Check if entity exists by ID
     */
    boolean exists(ID id);
    
    
    // ==================== BULK OPERATIONS ====================
    
    /**
     * Get all entities
     */
    List<T> findAll();
    
    /**
     * Get total count of entities
     */
    long count();
    
    /**
     * Delete all entities
     */
    void deleteAll();
    
    
    // ==================== REPOSITORY MANAGEMENT ====================
    
    /**
     * Save repository state to persistent storage
     */
    void saveToStorage();
    
    /**
     * Load repository state from persistent storage
     */
    void loadFromStorage();
    
    /**
     * Check if repository has unsaved changes
     */
    boolean hasUnsavedChanges();
    
    
    // ==================== QUERY OPERATIONS ====================
    
    /**
     * Find entities by attribute value
     */
    List<T> findByAttribute(String attribute, Object value);
    
    /**
     * Find entities matching search term across multiple fields
     */
    List<T> search(String searchTerm);
}