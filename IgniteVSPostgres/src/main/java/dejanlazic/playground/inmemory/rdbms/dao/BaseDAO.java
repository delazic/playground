package dejanlazic.playground.inmemory.rdbms.dao;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Base Data Access Object interface defining common CRUD operations
 * @param <T> The entity type
 * @param <ID> The primary key type
 */
public interface BaseDAO<T, ID> {
    
    /**
     * Insert a new entity into the database
     * @param entity Entity to insert
     * @return The inserted entity with generated ID (if applicable)
     * @throws SQLException if database error occurs
     */
    T insert(T entity) throws SQLException;
    
    /**
     * Insert multiple entities in a batch operation
     * @param entities List of entities to insert
     * @return Number of entities inserted
     * @throws SQLException if database error occurs
     */
    int insertBatch(List<T> entities) throws SQLException;
    
    /**
     * Find an entity by its primary key
     * @param id Primary key value
     * @return Optional containing the entity if found, empty otherwise
     * @throws SQLException if database error occurs
     */
    Optional<T> findById(ID id) throws SQLException;
    
    /**
     * Find all entities
     * @return List of all entities
     * @throws SQLException if database error occurs
     */
    List<T> findAll() throws SQLException;
    
    /**
     * Update an existing entity
     * @param entity Entity to update
     * @return true if update was successful, false otherwise
     * @throws SQLException if database error occurs
     */
    boolean update(T entity) throws SQLException;
    
    /**
     * Delete an entity by its primary key
     * @param id Primary key value
     * @return true if deletion was successful, false otherwise
     * @throws SQLException if database error occurs
     */
    boolean delete(ID id) throws SQLException;
    
    /**
     * Count total number of entities
     * @return Total count
     * @throws SQLException if database error occurs
     */
    long count() throws SQLException;
    
    /**
     * Check if an entity exists by its primary key
     * @param id Primary key value
     * @return true if entity exists, false otherwise
     * @throws SQLException if database error occurs
     */
    boolean exists(ID id) throws SQLException;
}

// Made with Bob
