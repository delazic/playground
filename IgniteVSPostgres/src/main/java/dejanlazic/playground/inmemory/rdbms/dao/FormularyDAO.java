package dejanlazic.playground.inmemory.rdbms.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import dejanlazic.playground.inmemory.rdbms.DatabaseConnector;
import dejanlazic.playground.inmemory.rdbms.model.Formulary;

/**
 * Data Access Object for Formulary entity
 * Provides CRUD operations for formularies with performance metrics
 */
public class FormularyDAO implements BaseDAO<Formulary, UUID> {
    
    private static final Logger LOGGER = Logger.getLogger(FormularyDAO.class.getName());
    
    private final DatabaseConnector connector;
    private static final int BATCH_SIZE = 1000;
    
    // SQL Statements
    private static final String INSERT_SQL = """
        INSERT INTO formulary (
            plan_id, formulary_name, effective_date, termination_date, is_active
        )
        SELECT
            p.plan_id,
            ?, ?, ?, ?
        FROM plan p
        WHERE p.plan_code = ?
        RETURNING formulary_id
        """;
    
    private static final String FIND_BY_ID_SQL = """
        SELECT formulary_id, plan_id, formulary_name, effective_date, termination_date,
               is_active, created_at, updated_at
        FROM formulary WHERE formulary_id = ?
        """;
    
    private static final String FIND_ALL_SQL = """
        SELECT formulary_id, plan_id, formulary_name, effective_date, termination_date,
               is_active, created_at, updated_at
        FROM formulary ORDER BY formulary_name
        """;
    
    private static final String UPDATE_SQL = """
        UPDATE formulary SET
            plan_id = ?, formulary_name = ?, effective_date = ?, termination_date = ?, is_active = ?
        WHERE formulary_id = ?
        """;
    
    private static final String DELETE_SQL = "DELETE FROM formulary WHERE formulary_id = ?";
    
    private static final String COUNT_SQL = "SELECT COUNT(*) FROM formulary";
    
    private static final String EXISTS_SQL = "SELECT EXISTS(SELECT 1 FROM formulary WHERE formulary_id = ?)";
    
    private static final String FIND_BY_PLAN_ID_SQL = """
        SELECT formulary_id, plan_id, formulary_name, effective_date, termination_date,
               is_active, created_at, updated_at
        FROM formulary WHERE plan_id = ?
        ORDER BY effective_date DESC
        """;
    
    private static final String FIND_ACTIVE_SQL = """
        SELECT formulary_id, plan_id, formulary_name, effective_date, termination_date,
               is_active, created_at, updated_at
        FROM formulary 
        WHERE is_active = true 
          AND effective_date <= CURRENT_DATE 
          AND (termination_date IS NULL OR termination_date > CURRENT_DATE)
        ORDER BY formulary_name
        """;
    
    private static final String FIND_BY_NAME_SQL = """
        SELECT formulary_id, plan_id, formulary_name, effective_date, termination_date,
               is_active, created_at, updated_at
        FROM formulary WHERE formulary_name = ?
        """;
    
    public FormularyDAO(DatabaseConnector connector) {
        this.connector = connector;
    }
    
    @Override
    public Formulary insert(Formulary formulary) throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics("Formulary", "INSERT");
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_SQL)) {
            
            setFormularyParameters(ps, formulary);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    formulary.setFormularyId((UUID) rs.getObject(1));
                    metrics.setRecordCount(1);
                    metrics.setRecordSizeBytes(estimateFormularySize(formulary));
                    LOGGER.log(Level.INFO, "Inserted formulary: {0}", formulary.getFormularyName());
                }
            }
            return formulary;
        } finally {
            metrics.complete();
        }
    }
    
    @Override
    public int insertBatch(List<Formulary> formularies) throws SQLException {
        if (formularies == null || formularies.isEmpty()) {
            LOGGER.log(Level.WARNING, "No formularies to insert");
            return 0;
        }
        
        PerformanceMetrics metrics = new PerformanceMetrics("Formulary", "INSERT_BATCH");
        int insertedCount = 0;
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_SQL)) {
            
            conn.setAutoCommit(false);
            
            try {
                long totalSize = 0;
                for (int i = 0; i < formularies.size(); i++) {
                    setFormularyParameters(ps, formularies.get(i));
                    ps.addBatch();
                    totalSize += estimateFormularySize(formularies.get(i));
                    
                    if ((i + 1) % BATCH_SIZE == 0 || i == formularies.size() - 1) {
                        int[] results = ps.executeBatch();
                        insertedCount += results.length;
                        ps.clearBatch();
                        
                        // Log progress for large batches
                        if ((i + 1) % 5000 == 0) {
                            LOGGER.log(Level.INFO, "Inserted {0} of {1} formularies", 
                                new Object[]{i + 1, formularies.size()});
                        }
                    }
                }
                
                conn.commit();
                metrics.setRecordCount(insertedCount);
                metrics.setRecordSizeBytes(totalSize);
                LOGGER.log(Level.INFO, "Successfully inserted {0} formularies", insertedCount);
                
            } catch (SQLException e) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Failed to insert formularies batch", e);
                throw new SQLException(
                    String.format("Failed to insert formularies. Successfully inserted: %d of %d",
                        insertedCount, formularies.size()), e);
            } finally {
                conn.setAutoCommit(true);
                metrics.complete();
            }
        }
        
        return insertedCount;
    }
    
    @Override
    public Optional<Formulary> findById(UUID id) throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics("Formulary", "SELECT_BY_ID");
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_BY_ID_SQL)) {
            
            ps.setObject(1, id);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Formulary formulary = mapResultSetToFormulary(rs);
                    metrics.setRecordCount(1);
                    metrics.setRecordSizeBytes(estimateFormularySize(formulary));
                    return Optional.of(formulary);
                }
            }
            
            return Optional.empty();
        } finally {
            metrics.complete();
        }
    }
    
    /**
     * Find formularies by plan ID
     * @param planId Plan ID to search for
     * @return List of formularies for the plan
     * @throws SQLException if database error occurs
     */
    public List<Formulary> findByPlanId(UUID planId) throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics("Formulary", "SELECT_BY_PLAN_ID");
        List<Formulary> formularies = new ArrayList<>();
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_BY_PLAN_ID_SQL)) {
            
            ps.setObject(1, planId);
            
            try (ResultSet rs = ps.executeQuery()) {
                long totalSize = 0;
                while (rs.next()) {
                    Formulary formulary = mapResultSetToFormulary(rs);
                    formularies.add(formulary);
                    totalSize += estimateFormularySize(formulary);
                }
                
                metrics.setRecordCount(formularies.size());
                metrics.setRecordSizeBytes(totalSize);
                return formularies;
            }
        } finally {
            metrics.complete();
        }
    }
    
    /**
     * Find all currently active formularies
     * @return List of active formularies
     * @throws SQLException if database error occurs
     */
    public List<Formulary> findActiveFormularies() throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics("Formulary", "SELECT_ACTIVE");
        List<Formulary> formularies = new ArrayList<>();
        
        try (Connection conn = connector.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(FIND_ACTIVE_SQL)) {
            
            long totalSize = 0;
            while (rs.next()) {
                Formulary formulary = mapResultSetToFormulary(rs);
                formularies.add(formulary);
                totalSize += estimateFormularySize(formulary);
            }
            
            metrics.setRecordCount(formularies.size());
            metrics.setRecordSizeBytes(totalSize);
            LOGGER.log(Level.INFO, "Retrieved {0} active formularies", formularies.size());
            return formularies;
        } finally {
            metrics.complete();
        }
    }
    
    /**
     * Find a formulary by name
     * @param formularyName Formulary name to search for
     * @return Optional containing the formulary if found
     * @throws SQLException if database error occurs
     */
    public Optional<Formulary> findByName(String formularyName) throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics("Formulary", "SELECT_BY_NAME");
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_BY_NAME_SQL)) {
            
            ps.setString(1, formularyName);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Formulary formulary = mapResultSetToFormulary(rs);
                    metrics.setRecordCount(1);
                    metrics.setRecordSizeBytes(estimateFormularySize(formulary));
                    return Optional.of(formulary);
                }
            }
            
            return Optional.empty();
        } finally {
            metrics.complete();
        }
    }
    
    @Override
    public List<Formulary> findAll() throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics("Formulary", "SELECT_ALL");
        List<Formulary> formularies = new ArrayList<>();
        
        try (Connection conn = connector.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(FIND_ALL_SQL)) {
            
            long totalSize = 0;
            while (rs.next()) {
                Formulary formulary = mapResultSetToFormulary(rs);
                formularies.add(formulary);
                totalSize += estimateFormularySize(formulary);
            }
            
            metrics.setRecordCount(formularies.size());
            metrics.setRecordSizeBytes(totalSize);
            LOGGER.log(Level.INFO, "Retrieved {0} formularies", formularies.size());
            return formularies;
        } finally {
            metrics.complete();
        }
    }
    
    @Override
    public boolean update(Formulary formulary) throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics("Formulary", "UPDATE");
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE_SQL)) {
            
            int idx = 1;
            ps.setObject(idx++, formulary.getPlanId());
            ps.setString(idx++, formulary.getFormularyName());
            ps.setDate(idx++, formulary.getEffectiveDate() != null ? Date.valueOf(formulary.getEffectiveDate()) : null);
            ps.setDate(idx++, formulary.getTerminationDate() != null ? Date.valueOf(formulary.getTerminationDate()) : null);
            ps.setBoolean(idx++, formulary.isActive());
            ps.setObject(idx++, formulary.getFormularyId());
            
            int rowsAffected = ps.executeUpdate();
            boolean updated = rowsAffected > 0;
            
            if (updated) {
                metrics.setRecordCount(1);
                metrics.setRecordSizeBytes(estimateFormularySize(formulary));
                LOGGER.log(Level.INFO, "Updated formulary: {0}", formulary.getFormularyName());
            }
            
            return updated;
        } finally {
            metrics.complete();
        }
    }
    
    @Override
    public boolean delete(UUID id) throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics("Formulary", "DELETE");
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(DELETE_SQL)) {
            
            ps.setObject(1, id);
            int rowsAffected = ps.executeUpdate();
            boolean deleted = rowsAffected > 0;
            
            if (deleted) {
                metrics.setRecordCount(1);
                LOGGER.log(Level.INFO, "Deleted formulary with ID: {0}", id);
            }
            
            return deleted;
        } finally {
            metrics.complete();
        }
    }
    
    @Override
    public long count() throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics("Formulary", "COUNT");
        
        try (Connection conn = connector.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(COUNT_SQL)) {
            
            if (rs.next()) {
                long count = rs.getLong(1);
                metrics.setRecordCount(1); // COUNT operation returns 1 result
                return count;
            }
            
            return 0;
        } finally {
            metrics.complete();
        }
    }
    
    @Override
    public boolean exists(UUID id) throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics("Formulary", "EXISTS");
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(EXISTS_SQL)) {
            
            ps.setObject(1, id);
            
            try (ResultSet rs = ps.executeQuery()) {
                boolean exists = rs.next() && rs.getBoolean(1);
                metrics.setRecordCount(1); // EXISTS operation returns 1 result
                return exists;
            }
        } finally {
            metrics.complete();
        }
    }
    
    /**
     * Set PreparedStatement parameters from a Formulary object
     * Uses planCode to lookup plan_id via JOIN in SQL
     */
    private void setFormularyParameters(PreparedStatement ps, Formulary formulary) throws SQLException {
        int idx = 1;
        ps.setString(idx++, formulary.getFormularyName());
        ps.setDate(idx++, formulary.getEffectiveDate() != null ? Date.valueOf(formulary.getEffectiveDate()) : null);
        ps.setDate(idx++, formulary.getTerminationDate() != null ? Date.valueOf(formulary.getTerminationDate()) : null);
        ps.setBoolean(idx++, formulary.isActive());
        ps.setString(idx++, formulary.getPlanCode());
    }
    
    /**
     * Estimates the size of a Formulary record in bytes.
     * This is an approximation based on the data types and string lengths.
     * 
     * @param formulary The Formulary to estimate
     * @return Estimated size in bytes
     */
    private long estimateFormularySize(Formulary formulary) {
        long size = 0;
        
        // UUIDs (16 bytes each)
        size += 16; // formulary_id
        if (formulary.getPlanId() != null) size += 16;
        
        // Strings (2 bytes per character for UTF-16)
        if (formulary.getFormularyName() != null) {
            size += formulary.getFormularyName().length() * 2L;
        }
        
        // Dates (8 bytes each)
        if (formulary.getEffectiveDate() != null) size += 8;
        if (formulary.getTerminationDate() != null) size += 8;
        
        // Boolean (1 byte)
        size += 1;
        
        // Timestamps (8 bytes each)
        if (formulary.getCreatedAt() != null) size += 8;
        if (formulary.getUpdatedAt() != null) size += 8;
        
        return size;
    }
    
    /**
     * Map ResultSet row to Formulary object
     */
    private Formulary mapResultSetToFormulary(ResultSet rs) throws SQLException {
        Formulary formulary = new Formulary();
        
        formulary.setFormularyId((UUID) rs.getObject("formulary_id"));
        formulary.setPlanId((UUID) rs.getObject("plan_id"));
        formulary.setFormularyName(rs.getString("formulary_name"));
        
        Date effectiveDate = rs.getDate("effective_date");
        if (effectiveDate != null) {
            formulary.setEffectiveDate(effectiveDate.toLocalDate());
        }
        
        Date terminationDate = rs.getDate("termination_date");
        if (terminationDate != null) {
            formulary.setTerminationDate(terminationDate.toLocalDate());
        }
        
        formulary.setActive(rs.getBoolean("is_active"));
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            formulary.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            formulary.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        
        return formulary;
    }
}