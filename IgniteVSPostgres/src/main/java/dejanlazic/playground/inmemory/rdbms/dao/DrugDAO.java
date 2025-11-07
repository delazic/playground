package dejanlazic.playground.inmemory.rdbms.dao;

import java.math.BigDecimal;
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
import dejanlazic.playground.inmemory.rdbms.model.Drug;

/**
 * Data Access Object for Drug entity
 * Provides CRUD operations for drugs with performance metrics
 */
public class DrugDAO implements BaseDAO<Drug, UUID> {
    
    private static final Logger LOGGER = Logger.getLogger(DrugDAO.class.getName());
    
    private final DatabaseConnector connector;
    private static final int BATCH_SIZE = 1000;
    
    // SQL Statements
    private static final String INSERT_SQL = """
        INSERT INTO drug (
            ndc_code, drug_name, generic_name, strength, dosage_form,
            manufacturer, drug_class, is_generic, is_brand, awp_price, mac_price
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        RETURNING drug_id
        """;
    
    private static final String FIND_BY_ID_SQL = """
        SELECT drug_id, ndc_code, drug_name, generic_name, strength, dosage_form,
               manufacturer, drug_class, is_generic, is_brand, awp_price, mac_price,
               created_at, updated_at
        FROM drug WHERE drug_id = ?
        """;
    
    private static final String FIND_ALL_SQL = """
        SELECT drug_id, ndc_code, drug_name, generic_name, strength, dosage_form,
               manufacturer, drug_class, is_generic, is_brand, awp_price, mac_price,
               created_at, updated_at
        FROM drug ORDER BY drug_name
        """;
    
    private static final String UPDATE_SQL = """
        UPDATE drug SET
            drug_name = ?, generic_name = ?, strength = ?, dosage_form = ?,
            manufacturer = ?, drug_class = ?, is_generic = ?, is_brand = ?,
            awp_price = ?, mac_price = ?
        WHERE ndc_code = ?
        """;
    
    private static final String DELETE_SQL = "DELETE FROM drug WHERE drug_id = ?";
    
    private static final String COUNT_SQL = "SELECT COUNT(*) FROM drug";
    
    private static final String EXISTS_SQL = "SELECT EXISTS(SELECT 1 FROM drug WHERE drug_id = ?)";
    
    private static final String FIND_BY_NDC_SQL = """
        SELECT drug_id, ndc_code, drug_name, generic_name, strength, dosage_form,
               manufacturer, drug_class, is_generic, is_brand, awp_price, mac_price,
               created_at, updated_at
        FROM drug WHERE ndc_code = ?
        """;
    
    public DrugDAO(DatabaseConnector connector) {
        this.connector = connector;
    }
    
    @Override
    public Drug insert(Drug drug) throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics("Drug", "INSERT");
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_SQL)) {
            
            setDrugParameters(ps, drug);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    drug.setDrugId((UUID) rs.getObject(1));
                    metrics.setRecordCount(1);
                    metrics.setRecordSizeBytes(estimateDrugSize(drug));
                    LOGGER.log(Level.INFO, "Inserted drug: {0}", drug.getNdcCode());
                }
            }
            return drug;
        } finally {
            metrics.complete();
        }
    }
    
    @Override
    public int insertBatch(List<Drug> drugs) throws SQLException {
        if (drugs == null || drugs.isEmpty()) {
            LOGGER.log(Level.WARNING, "No drugs to insert");
            return 0;
        }
        
        PerformanceMetrics metrics = new PerformanceMetrics("Drug", "INSERT_BATCH");
        int insertedCount = 0;
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_SQL)) {
            
            conn.setAutoCommit(false);
            
            try {
                long totalSize = 0;
                for (int i = 0; i < drugs.size(); i++) {
                    setDrugParameters(ps, drugs.get(i));
                    ps.addBatch();
                    totalSize += estimateDrugSize(drugs.get(i));
                    
                    if ((i + 1) % BATCH_SIZE == 0 || i == drugs.size() - 1) {
                        int[] results = ps.executeBatch();
                        insertedCount += results.length;
                        ps.clearBatch();
                        
                        // Log progress for large batches
                        if ((i + 1) % 5000 == 0) {
                            LOGGER.log(Level.INFO, "Inserted {0} of {1} drugs", 
                                new Object[]{i + 1, drugs.size()});
                        }
                    }
                }
                
                conn.commit();
                metrics.setRecordCount(insertedCount);
                metrics.setRecordSizeBytes(totalSize);
                LOGGER.log(Level.INFO, "Successfully inserted {0} drugs", insertedCount);
                
            } catch (SQLException e) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Failed to insert drugs batch", e);
                throw new SQLException(
                    String.format("Failed to insert drugs. Successfully inserted: %d of %d",
                        insertedCount, drugs.size()), e);
            } finally {
                conn.setAutoCommit(true);
                metrics.complete();
            }
        }
        
        return insertedCount;
    }
    
    @Override
    public Optional<Drug> findById(UUID id) throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics("Drug", "SELECT_BY_ID");
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_BY_ID_SQL)) {
            
            ps.setObject(1, id);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Drug drug = mapResultSetToDrug(rs);
                    metrics.setRecordCount(1);
                    metrics.setRecordSizeBytes(estimateDrugSize(drug));
                    return Optional.of(drug);
                }
            }
            
            return Optional.empty();
        } finally {
            metrics.complete();
        }
    }
    
    /**
     * Find a drug by NDC code
     * @param ndcCode NDC code to search for
     * @return Optional containing the drug if found
     * @throws SQLException if database error occurs
     */
    public Optional<Drug> findByNdcCode(String ndcCode) throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics("Drug", "SELECT_BY_NDC");
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_BY_NDC_SQL)) {
            
            ps.setString(1, ndcCode);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Drug drug = mapResultSetToDrug(rs);
                    metrics.setRecordCount(1);
                    metrics.setRecordSizeBytes(estimateDrugSize(drug));
                    return Optional.of(drug);
                }
            }
            
            return Optional.empty();
        } finally {
            metrics.complete();
        }
    }
    
    @Override
    public List<Drug> findAll() throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics("Drug", "SELECT_ALL");
        List<Drug> drugs = new ArrayList<>();
        
        try (Connection conn = connector.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(FIND_ALL_SQL)) {
            
            long totalSize = 0;
            while (rs.next()) {
                Drug drug = mapResultSetToDrug(rs);
                drugs.add(drug);
                totalSize += estimateDrugSize(drug);
            }
            
            metrics.setRecordCount(drugs.size());
            metrics.setRecordSizeBytes(totalSize);
            LOGGER.log(Level.INFO, "Retrieved {0} drugs", drugs.size());
            return drugs;
        } finally {
            metrics.complete();
        }
    }
    
    @Override
    public boolean update(Drug drug) throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics("Drug", "UPDATE");
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE_SQL)) {
            
            int idx = 1;
            ps.setString(idx++, drug.getDrugName());
            ps.setString(idx++, drug.getGenericName());
            ps.setString(idx++, drug.getStrength());
            ps.setString(idx++, drug.getDosageForm());
            ps.setString(idx++, drug.getManufacturer());
            ps.setString(idx++, drug.getDrugClass());
            ps.setBoolean(idx++, drug.isGeneric());
            ps.setBoolean(idx++, drug.isBrand());
            ps.setBigDecimal(idx++, drug.getAwpPrice());
            ps.setBigDecimal(idx++, drug.getMacPrice());
            ps.setString(idx++, drug.getNdcCode());
            
            int rowsAffected = ps.executeUpdate();
            boolean updated = rowsAffected > 0;
            
            if (updated) {
                metrics.setRecordCount(1);
                metrics.setRecordSizeBytes(estimateDrugSize(drug));
                LOGGER.log(Level.INFO, "Updated drug: {0}", drug.getNdcCode());
            }
            
            return updated;
        } finally {
            metrics.complete();
        }
    }
    
    @Override
    public boolean delete(UUID id) throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics("Drug", "DELETE");
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(DELETE_SQL)) {
            
            ps.setObject(1, id);
            int rowsAffected = ps.executeUpdate();
            boolean deleted = rowsAffected > 0;
            
            if (deleted) {
                metrics.setRecordCount(1);
                LOGGER.log(Level.INFO, "Deleted drug with ID: {0}", id);
            }
            
            return deleted;
        } finally {
            metrics.complete();
        }
    }
    
    @Override
    public long count() throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics("Drug", "COUNT");
        
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
        PerformanceMetrics metrics = new PerformanceMetrics("Drug", "EXISTS");
        
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
     * Set PreparedStatement parameters from a Drug object
     */
    private void setDrugParameters(PreparedStatement ps, Drug drug) throws SQLException {
        int idx = 1;
        ps.setString(idx++, drug.getNdcCode());
        ps.setString(idx++, drug.getDrugName());
        ps.setString(idx++, drug.getGenericName());
        ps.setString(idx++, drug.getStrength());
        ps.setString(idx++, drug.getDosageForm());
        ps.setString(idx++, drug.getManufacturer());
        ps.setString(idx++, drug.getDrugClass());
        ps.setBoolean(idx++, drug.isGeneric());
        ps.setBoolean(idx++, drug.isBrand());
        ps.setBigDecimal(idx++, drug.getAwpPrice());
        ps.setBigDecimal(idx++, drug.getMacPrice());
    }
    
    /**
     * Estimates the size of a Drug record in bytes.
     * This is an approximation based on the data types and string lengths.
     * 
     * @param drug The Drug to estimate
     * @return Estimated size in bytes
     */
    private long estimateDrugSize(Drug drug) {
        long size = 0;
        
        // UUID (16 bytes)
        size += 16;
        
        // Strings (2 bytes per character for UTF-16)
        if (drug.getNdcCode() != null) size += drug.getNdcCode().length() * 2L;
        if (drug.getDrugName() != null) size += drug.getDrugName().length() * 2L;
        if (drug.getGenericName() != null) size += drug.getGenericName().length() * 2L;
        if (drug.getStrength() != null) size += drug.getStrength().length() * 2L;
        if (drug.getDosageForm() != null) size += drug.getDosageForm().length() * 2L;
        if (drug.getManufacturer() != null) size += drug.getManufacturer().length() * 2L;
        if (drug.getDrugClass() != null) size += drug.getDrugClass().length() * 2L;
        
        // Booleans (1 byte each)
        size += 2;
        
        // BigDecimal (approximately 16 bytes each)
        if (drug.getAwpPrice() != null) size += 16;
        if (drug.getMacPrice() != null) size += 16;
        
        // Timestamps (8 bytes each)
        if (drug.getCreatedAt() != null) size += 8;
        if (drug.getUpdatedAt() != null) size += 8;
        
        return size;
    }
    
    /**
     * Map ResultSet row to Drug object
     */
    private Drug mapResultSetToDrug(ResultSet rs) throws SQLException {
        Drug drug = new Drug();
        
        drug.setDrugId((UUID) rs.getObject("drug_id"));
        drug.setNdcCode(rs.getString("ndc_code"));
        drug.setDrugName(rs.getString("drug_name"));
        drug.setGenericName(rs.getString("generic_name"));
        drug.setStrength(rs.getString("strength"));
        drug.setDosageForm(rs.getString("dosage_form"));
        drug.setManufacturer(rs.getString("manufacturer"));
        drug.setDrugClass(rs.getString("drug_class"));
        drug.setGeneric(rs.getBoolean("is_generic"));
        drug.setBrand(rs.getBoolean("is_brand"));
        
        BigDecimal awpPrice = rs.getBigDecimal("awp_price");
        if (awpPrice != null) {
            drug.setAwpPrice(awpPrice);
        }
        
        BigDecimal macPrice = rs.getBigDecimal("mac_price");
        if (macPrice != null) {
            drug.setMacPrice(macPrice);
        }
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            drug.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            drug.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        
        return drug;
    }
}
