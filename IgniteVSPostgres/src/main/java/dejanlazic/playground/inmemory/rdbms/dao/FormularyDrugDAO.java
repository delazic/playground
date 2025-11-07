package dejanlazic.playground.inmemory.rdbms.dao;

import java.sql.Connection;
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
import dejanlazic.playground.inmemory.rdbms.PerformanceMetrics;
import dejanlazic.playground.inmemory.rdbms.model.FormularyDrug;

/**
 * Data Access Object for FormularyDrug entity
 * Provides CRUD operations for formulary-drug relationships with performance metrics
 * 
 * IMPORTANT: This table has foreign keys to both formulary and drug tables
 * - formulary_id references formulary(formulary_id)
 * - drug_id references drug(drug_id)
 * 
 * The INSERT operation uses JOINs to resolve business keys (formulary_code, ndc_code)
 * to their respective UUIDs, similar to how EnrollmentDAO and FormularyDAO work.
 */
public class FormularyDrugDAO implements BaseDAO<FormularyDrug, UUID> {
    
    private static final Logger LOGGER = Logger.getLogger(FormularyDrugDAO.class.getName());
    
    private final DatabaseConnector connector;
    private static final int BATCH_SIZE = 1000;
    
    // SQL Statements
    private static final String INSERT_SQL = """
        INSERT INTO formulary_drug (
            formulary_id, drug_id, tier, status, requires_prior_auth, 
            requires_step_therapy, quantity_limit, days_supply_limit
        )
        SELECT
            f.formulary_id,
            d.drug_id,
            ?, ?, ?, ?, ?, ?
        FROM formulary f
        CROSS JOIN drug d
        WHERE f.formulary_code = ?
          AND d.ndc_code = ?
        ON CONFLICT (formulary_id, drug_id) DO NOTHING
        RETURNING formulary_drug_id
        """;
    
    private static final String FIND_BY_ID_SQL = """
        SELECT formulary_drug_id, formulary_id, drug_id, tier, status,
               requires_prior_auth, requires_step_therapy, quantity_limit, 
               days_supply_limit, created_at, updated_at
        FROM formulary_drug WHERE formulary_drug_id = ?
        """;
    
    private static final String FIND_ALL_SQL = """
        SELECT formulary_drug_id, formulary_id, drug_id, tier, status,
               requires_prior_auth, requires_step_therapy, quantity_limit, 
               days_supply_limit, created_at, updated_at
        FROM formulary_drug
        ORDER BY formulary_id, tier, drug_id
        LIMIT 10000
        """;
    
    private static final String UPDATE_SQL = """
        UPDATE formulary_drug SET
            formulary_id = ?, drug_id = ?, tier = ?, status = ?,
            requires_prior_auth = ?, requires_step_therapy = ?,
            quantity_limit = ?, days_supply_limit = ?
        WHERE formulary_drug_id = ?
        """;
    
    private static final String DELETE_SQL = "DELETE FROM formulary_drug WHERE formulary_drug_id = ?";
    
    private static final String COUNT_SQL = "SELECT COUNT(*) FROM formulary_drug";
    
    private static final String EXISTS_SQL = "SELECT EXISTS(SELECT 1 FROM formulary_drug WHERE formulary_drug_id = ?)";
    
    private static final String FIND_BY_FORMULARY_ID_SQL = """
        SELECT formulary_drug_id, formulary_id, drug_id, tier, status,
               requires_prior_auth, requires_step_therapy, quantity_limit, 
               days_supply_limit, created_at, updated_at
        FROM formulary_drug
        WHERE formulary_id = ?
        ORDER BY tier, drug_id
        """;
    
    private static final String FIND_BY_DRUG_ID_SQL = """
        SELECT formulary_drug_id, formulary_id, drug_id, tier, status,
               requires_prior_auth, requires_step_therapy, quantity_limit, 
               days_supply_limit, created_at, updated_at
        FROM formulary_drug
        WHERE drug_id = ?
        ORDER BY formulary_id
        """;
    
    private static final String FIND_BY_TIER_SQL = """
        SELECT formulary_drug_id, formulary_id, drug_id, tier, status,
               requires_prior_auth, requires_step_therapy, quantity_limit, 
               days_supply_limit, created_at, updated_at
        FROM formulary_drug
        WHERE tier = ?
        ORDER BY formulary_id, drug_id
        LIMIT 1000
        """;
    
    private static final String COUNT_BY_TIER_SQL = "SELECT COUNT(*) FROM formulary_drug WHERE tier = ?";
    
    private static final String FIND_WITH_PRIOR_AUTH_SQL = """
        SELECT formulary_drug_id, formulary_id, drug_id, tier, status,
               requires_prior_auth, requires_step_therapy, quantity_limit, 
               days_supply_limit, created_at, updated_at
        FROM formulary_drug
        WHERE requires_prior_auth = true
        ORDER BY formulary_id, drug_id
        LIMIT 1000
        """;
    
    public FormularyDrugDAO(DatabaseConnector connector) {
        this.connector = connector;
    }
    
    @Override
    public FormularyDrug insert(FormularyDrug formularyDrug) throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics("FormularyDrug", "INSERT");
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_SQL)) {
            
            setFormularyDrugParameters(ps, formularyDrug);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    formularyDrug.setFormularyDrugId((UUID) rs.getObject(1));
                    metrics.setRecordCount(1);
                    metrics.setRecordSizeBytes(estimateFormularyDrugSize(formularyDrug));
                    LOGGER.log(Level.INFO, "Inserted formulary-drug relationship");
                }
            }
            return formularyDrug;
        } finally {
            metrics.complete();
        }
    }
    
    @Override
    public int insertBatch(List<FormularyDrug> formularyDrugs) throws SQLException {
        if (formularyDrugs == null || formularyDrugs.isEmpty()) {
            LOGGER.log(Level.WARNING, "No formulary-drug relationships to insert");
            return 0;
        }
        
        PerformanceMetrics metrics = new PerformanceMetrics("FormularyDrug", "INSERT_BATCH");
        int insertedCount = 0;
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_SQL)) {
            
            conn.setAutoCommit(false);
            
            try {
                long totalSize = 0;
                for (int i = 0; i < formularyDrugs.size(); i++) {
                    setFormularyDrugParameters(ps, formularyDrugs.get(i));
                    ps.addBatch();
                    totalSize += estimateFormularyDrugSize(formularyDrugs.get(i));
                    
                    if ((i + 1) % BATCH_SIZE == 0 || i == formularyDrugs.size() - 1) {
                        int[] results = ps.executeBatch();
                        insertedCount += results.length;
                        ps.clearBatch();
                        
                        // Log progress for large batches
                        if ((i + 1) % 10000 == 0) {
                            LOGGER.log(Level.INFO, "Inserted {0} of {1} formulary-drug relationships", 
                                new Object[]{i + 1, formularyDrugs.size()});
                        }
                    }
                }
                
                conn.commit();
                metrics.setRecordCount(insertedCount);
                metrics.setRecordSizeBytes(totalSize);
                LOGGER.log(Level.INFO, "Successfully inserted {0} formulary-drug relationships", insertedCount);
                
            } catch (SQLException e) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Failed to insert formulary-drug relationships batch", e);
                throw new SQLException(
                    String.format("Failed to insert formulary-drug relationships. Successfully inserted: %d of %d",
                        insertedCount, formularyDrugs.size()), e);
            } finally {
                conn.setAutoCommit(true);
                metrics.complete();
            }
        }
        
        return insertedCount;
    }
    
    @Override
    public Optional<FormularyDrug> findById(UUID id) throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics("FormularyDrug", "SELECT_BY_ID");
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_BY_ID_SQL)) {
            
            ps.setObject(1, id);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    FormularyDrug formularyDrug = mapResultSetToFormularyDrug(rs);
                    metrics.setRecordCount(1);
                    metrics.setRecordSizeBytes(estimateFormularyDrugSize(formularyDrug));
                    return Optional.of(formularyDrug);
                }
            }
            
            return Optional.empty();
        } finally {
            metrics.complete();
        }
    }
    
    /**
     * Find all formulary-drug relationships for a specific formulary
     * @param formularyId Formulary ID to search for
     * @return List of formulary-drug relationships
     * @throws SQLException if database error occurs
     */
    public List<FormularyDrug> findByFormularyId(UUID formularyId) throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics("FormularyDrug", "SELECT_BY_FORMULARY_ID");
        List<FormularyDrug> formularyDrugs = new ArrayList<>();
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_BY_FORMULARY_ID_SQL)) {
            
            ps.setObject(1, formularyId);
            
            try (ResultSet rs = ps.executeQuery()) {
                long totalSize = 0;
                while (rs.next()) {
                    FormularyDrug formularyDrug = mapResultSetToFormularyDrug(rs);
                    formularyDrugs.add(formularyDrug);
                    totalSize += estimateFormularyDrugSize(formularyDrug);
                }
                
                metrics.setRecordCount(formularyDrugs.size());
                metrics.setRecordSizeBytes(totalSize);
                return formularyDrugs;
            }
        } finally {
            metrics.complete();
        }
    }
    
    /**
     * Find all formulary-drug relationships for a specific drug
     * @param drugId Drug ID to search for
     * @return List of formulary-drug relationships
     * @throws SQLException if database error occurs
     */
    public List<FormularyDrug> findByDrugId(UUID drugId) throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics("FormularyDrug", "SELECT_BY_DRUG_ID");
        List<FormularyDrug> formularyDrugs = new ArrayList<>();
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_BY_DRUG_ID_SQL)) {
            
            ps.setObject(1, drugId);
            
            try (ResultSet rs = ps.executeQuery()) {
                long totalSize = 0;
                while (rs.next()) {
                    FormularyDrug formularyDrug = mapResultSetToFormularyDrug(rs);
                    formularyDrugs.add(formularyDrug);
                    totalSize += estimateFormularyDrugSize(formularyDrug);
                }
                
                metrics.setRecordCount(formularyDrugs.size());
                metrics.setRecordSizeBytes(totalSize);
                return formularyDrugs;
            }
        } finally {
            metrics.complete();
        }
    }
    
    /**
     * Find formulary-drug relationships by tier
     * @param tier Tier to search for (1-5)
     * @return List of formulary-drug relationships
     * @throws SQLException if database error occurs
     */
    public List<FormularyDrug> findByTier(int tier) throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics("FormularyDrug", "SELECT_BY_TIER");
        List<FormularyDrug> formularyDrugs = new ArrayList<>();
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_BY_TIER_SQL)) {
            
            ps.setInt(1, tier);
            
            try (ResultSet rs = ps.executeQuery()) {
                long totalSize = 0;
                while (rs.next()) {
                    FormularyDrug formularyDrug = mapResultSetToFormularyDrug(rs);
                    formularyDrugs.add(formularyDrug);
                    totalSize += estimateFormularyDrugSize(formularyDrug);
                }
                
                metrics.setRecordCount(formularyDrugs.size());
                metrics.setRecordSizeBytes(totalSize);
                return formularyDrugs;
            }
        } finally {
            metrics.complete();
        }
    }
    
    /**
     * Count formulary-drug relationships by tier
     * @param tier Tier to count (1-5)
     * @return Count of relationships
     * @throws SQLException if database error occurs
     */
    public long countByTier(int tier) throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics("FormularyDrug", "COUNT_BY_TIER");
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(COUNT_BY_TIER_SQL)) {
            
            ps.setInt(1, tier);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long count = rs.getLong(1);
                    metrics.setRecordCount(1);
                    return count;
                }
            }
            
            return 0;
        } finally {
            metrics.complete();
        }
    }
    
    /**
     * Find formulary-drug relationships requiring prior authorization
     * @return List of formulary-drug relationships
     * @throws SQLException if database error occurs
     */
    public List<FormularyDrug> findWithPriorAuth() throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics("FormularyDrug", "SELECT_WITH_PRIOR_AUTH");
        List<FormularyDrug> formularyDrugs = new ArrayList<>();
        
        try (Connection conn = connector.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(FIND_WITH_PRIOR_AUTH_SQL)) {
            
            long totalSize = 0;
            while (rs.next()) {
                FormularyDrug formularyDrug = mapResultSetToFormularyDrug(rs);
                formularyDrugs.add(formularyDrug);
                totalSize += estimateFormularyDrugSize(formularyDrug);
            }
            
            metrics.setRecordCount(formularyDrugs.size());
            metrics.setRecordSizeBytes(totalSize);
            LOGGER.log(Level.INFO, "Retrieved {0} formulary-drug relationships with prior auth", formularyDrugs.size());
            return formularyDrugs;
        } finally {
            metrics.complete();
        }
    }
    
    @Override
    public List<FormularyDrug> findAll() throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics("FormularyDrug", "SELECT_ALL");
        List<FormularyDrug> formularyDrugs = new ArrayList<>();
        
        try (Connection conn = connector.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(FIND_ALL_SQL)) {
            
            long totalSize = 0;
            while (rs.next()) {
                FormularyDrug formularyDrug = mapResultSetToFormularyDrug(rs);
                formularyDrugs.add(formularyDrug);
                totalSize += estimateFormularyDrugSize(formularyDrug);
            }
            
            metrics.setRecordCount(formularyDrugs.size());
            metrics.setRecordSizeBytes(totalSize);
            LOGGER.log(Level.INFO, "Retrieved {0} formulary-drug relationships (limited to 10,000)", formularyDrugs.size());
            return formularyDrugs;
        } finally {
            metrics.complete();
        }
    }
    
    @Override
    public boolean update(FormularyDrug formularyDrug) throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics("FormularyDrug", "UPDATE");
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE_SQL)) {
            
            int idx = 1;
            ps.setObject(idx++, formularyDrug.getFormularyId());
            ps.setObject(idx++, formularyDrug.getDrugId());
            ps.setInt(idx++, formularyDrug.getTier());
            ps.setString(idx++, formularyDrug.getStatus());
            ps.setBoolean(idx++, formularyDrug.isRequiresPriorAuth());
            ps.setBoolean(idx++, formularyDrug.isRequiresStepTherapy());
            ps.setObject(idx++, formularyDrug.getQuantityLimit());
            ps.setObject(idx++, formularyDrug.getDaysSupplyLimit());
            ps.setObject(idx++, formularyDrug.getFormularyDrugId());
            
            int rowsAffected = ps.executeUpdate();
            boolean updated = rowsAffected > 0;
            
            if (updated) {
                metrics.setRecordCount(1);
                metrics.setRecordSizeBytes(estimateFormularyDrugSize(formularyDrug));
                LOGGER.log(Level.INFO, "Updated formulary-drug relationship");
            }
            
            return updated;
        } finally {
            metrics.complete();
        }
    }
    
    @Override
    public boolean delete(UUID id) throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics("FormularyDrug", "DELETE");
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(DELETE_SQL)) {
            
            ps.setObject(1, id);
            int rowsAffected = ps.executeUpdate();
            boolean deleted = rowsAffected > 0;
            
            if (deleted) {
                metrics.setRecordCount(1);
                LOGGER.log(Level.INFO, "Deleted formulary-drug relationship with ID: {0}", id);
            }
            
            return deleted;
        } finally {
            metrics.complete();
        }
    }
    
    @Override
    public long count() throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics("FormularyDrug", "COUNT");
        
        try (Connection conn = connector.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(COUNT_SQL)) {
            
            if (rs.next()) {
                long count = rs.getLong(1);
                metrics.setRecordCount(1);
                return count;
            }
            
            return 0;
        } finally {
            metrics.complete();
        }
    }
    
    @Override
    public boolean exists(UUID id) throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics("FormularyDrug", "EXISTS");
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(EXISTS_SQL)) {
            
            ps.setObject(1, id);
            
            try (ResultSet rs = ps.executeQuery()) {
                boolean exists = rs.next() && rs.getBoolean(1);
                metrics.setRecordCount(1);
                return exists;
            }
        } finally {
            metrics.complete();
        }
    }
    
    /**
     * Set PreparedStatement parameters from a FormularyDrug object
     * Uses formularyCode and ndcCode to lookup formulary_id and drug_id via JOINs in SQL
     */
    private void setFormularyDrugParameters(PreparedStatement ps, FormularyDrug formularyDrug) throws SQLException {
        int idx = 1;
        ps.setInt(idx++, formularyDrug.getTier());
        ps.setString(idx++, formularyDrug.getStatus());
        ps.setBoolean(idx++, formularyDrug.isRequiresPriorAuth());
        ps.setBoolean(idx++, formularyDrug.isRequiresStepTherapy());
        ps.setObject(idx++, formularyDrug.getQuantityLimit());
        ps.setObject(idx++, formularyDrug.getDaysSupplyLimit());
        ps.setString(idx++, formularyDrug.getFormularyCode());
        ps.setString(idx++, formularyDrug.getNdcCode());
    }
    
    /**
     * Estimates the size of a FormularyDrug record in bytes
     */
    private long estimateFormularyDrugSize(FormularyDrug formularyDrug) {
        long size = 0;
        
        // UUIDs (16 bytes each)
        size += 16; // formulary_drug_id
        if (formularyDrug.getFormularyId() != null) size += 16;
        if (formularyDrug.getDrugId() != null) size += 16;
        
        // Integers (4 bytes each)
        size += 4; // tier
        if (formularyDrug.getQuantityLimit() != null) size += 4;
        if (formularyDrug.getDaysSupplyLimit() != null) size += 4;
        
        // Strings (2 bytes per character for UTF-16)
        if (formularyDrug.getStatus() != null) {
            size += formularyDrug.getStatus().length() * 2L;
        }
        
        // Booleans (1 byte each)
        size += 2; // requires_prior_auth, requires_step_therapy
        
        // Timestamps (8 bytes each)
        if (formularyDrug.getCreatedAt() != null) size += 8;
        if (formularyDrug.getUpdatedAt() != null) size += 8;
        
        return size;
    }
    
    /**
     * Map ResultSet row to FormularyDrug object
     */
    private FormularyDrug mapResultSetToFormularyDrug(ResultSet rs) throws SQLException {
        FormularyDrug formularyDrug = new FormularyDrug();
        
        formularyDrug.setFormularyDrugId((UUID) rs.getObject("formulary_drug_id"));
        formularyDrug.setFormularyId((UUID) rs.getObject("formulary_id"));
        formularyDrug.setDrugId((UUID) rs.getObject("drug_id"));
        formularyDrug.setTier(rs.getInt("tier"));
        formularyDrug.setStatus(rs.getString("status"));
        formularyDrug.setRequiresPriorAuth(rs.getBoolean("requires_prior_auth"));
        formularyDrug.setRequiresStepTherapy(rs.getBoolean("requires_step_therapy"));
        
        Integer quantityLimit = (Integer) rs.getObject("quantity_limit");
        formularyDrug.setQuantityLimit(quantityLimit);
        
        Integer daysSupplyLimit = (Integer) rs.getObject("days_supply_limit");
        formularyDrug.setDaysSupplyLimit(daysSupplyLimit);
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            formularyDrug.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            formularyDrug.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        
        return formularyDrug;
    }
}
