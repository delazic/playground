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
import java.util.logging.Level;
import java.util.logging.Logger;

import dejanlazic.playground.inmemory.rdbms.DatabaseConnector;
import dejanlazic.playground.inmemory.rdbms.PerformanceMetrics;
import dejanlazic.playground.inmemory.rdbms.model.Pharmacy;
import dejanlazic.playground.inmemory.rdbms.model.Pharmacy.PharmacyType;

/**
 * Data Access Object for Pharmacy entity
 * Provides CRUD operations for pharmacies with performance metrics
 */
public class PharmacyDAO implements BaseDAO<Pharmacy, Long> {
    
    private static final Logger LOGGER = Logger.getLogger(PharmacyDAO.class.getName());
    
    private final DatabaseConnector connector;
    private static final int BATCH_SIZE = 1000;
    
    // SQL Statements
    private static final String INSERT_SQL = """
        INSERT INTO pharmacy (
            ncpdp_id, pharmacy_name, npi, address, city, state, zip_code, phone,
            pharmacy_type, is_active
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?::pharmacy_type, ?)
        ON CONFLICT (ncpdp_id) DO UPDATE SET
            pharmacy_name = EXCLUDED.pharmacy_name,
            npi = EXCLUDED.npi,
            address = EXCLUDED.address,
            city = EXCLUDED.city,
            state = EXCLUDED.state,
            zip_code = EXCLUDED.zip_code,
            phone = EXCLUDED.phone,
            pharmacy_type = EXCLUDED.pharmacy_type,
            is_active = EXCLUDED.is_active
        RETURNING pharmacy_id
        """;
    
    private static final String FIND_BY_ID_SQL = """
        SELECT pharmacy_id, ncpdp_id, pharmacy_name, npi, address, city, state, 
               zip_code, phone, pharmacy_type, is_active, created_at, updated_at
        FROM pharmacy WHERE pharmacy_id = ?
        """;
    
    private static final String FIND_ALL_SQL = """
        SELECT pharmacy_id, ncpdp_id, pharmacy_name, npi, address, city, state, 
               zip_code, phone, pharmacy_type, is_active, created_at, updated_at
        FROM pharmacy ORDER BY pharmacy_name
        """;
    
    private static final String UPDATE_SQL = """
        UPDATE pharmacy SET
            ncpdp_id = ?, pharmacy_name = ?, npi = ?, address = ?, city = ?, 
            state = ?, zip_code = ?, phone = ?, pharmacy_type = ?::pharmacy_type, 
            is_active = ?
        WHERE pharmacy_id = ?
        """;
    
    private static final String DELETE_SQL = "DELETE FROM pharmacy WHERE pharmacy_id = ?";
    
    private static final String COUNT_SQL = "SELECT COUNT(*) FROM pharmacy";
    
    private static final String EXISTS_SQL = "SELECT EXISTS(SELECT 1 FROM pharmacy WHERE pharmacy_id = ?)";
    
    private static final String FIND_BY_NCPDP_SQL = """
        SELECT pharmacy_id, ncpdp_id, pharmacy_name, npi, address, city, state, 
               zip_code, phone, pharmacy_type, is_active, created_at, updated_at
        FROM pharmacy WHERE ncpdp_id = ?
        """;
    
    private static final String FIND_BY_STATE_SQL = """
        SELECT pharmacy_id, ncpdp_id, pharmacy_name, npi, address, city, state, 
               zip_code, phone, pharmacy_type, is_active, created_at, updated_at
        FROM pharmacy WHERE state = ? ORDER BY city, pharmacy_name
        """;
    
    private static final String FIND_BY_TYPE_SQL = """
        SELECT pharmacy_id, ncpdp_id, pharmacy_name, npi, address, city, state, 
               zip_code, phone, pharmacy_type, is_active, created_at, updated_at
        FROM pharmacy WHERE pharmacy_type = ?::pharmacy_type ORDER BY pharmacy_name
        """;
    
    private static final String FIND_ACTIVE_SQL = """
        SELECT pharmacy_id, ncpdp_id, pharmacy_name, npi, address, city, state, 
               zip_code, phone, pharmacy_type, is_active, created_at, updated_at
        FROM pharmacy WHERE is_active = true ORDER BY pharmacy_name
        """;
    
    public PharmacyDAO(DatabaseConnector connector) {
        this.connector = connector;
    }
    
    @Override
    public Pharmacy insert(Pharmacy pharmacy) throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics("Pharmacy", "INSERT");
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_SQL)) {
            
            setPharmacyParameters(ps, pharmacy);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    pharmacy.setPharmacyId(rs.getLong(1));
                    metrics.setRecordCount(1);
                    metrics.setRecordSizeBytes(estimatePharmacySize(pharmacy));
                    LOGGER.log(Level.INFO, "Inserted pharmacy: {0}", pharmacy.getPharmacyName());
                }
            }
            return pharmacy;
        } finally {
            metrics.complete();
        }
    }
    
    @Override
    public int insertBatch(List<Pharmacy> pharmacies) throws SQLException {
        if (pharmacies == null || pharmacies.isEmpty()) {
            LOGGER.log(Level.WARNING, "No pharmacies to insert");
            return 0;
        }
        
        PerformanceMetrics metrics = new PerformanceMetrics("Pharmacy", "INSERT_BATCH");
        int insertedCount = 0;
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_SQL)) {
            
            conn.setAutoCommit(false);
            
            try {
                long totalSize = 0;
                for (int i = 0; i < pharmacies.size(); i++) {
                    setPharmacyParameters(ps, pharmacies.get(i));
                    ps.addBatch();
                    totalSize += estimatePharmacySize(pharmacies.get(i));
                    
                    if ((i + 1) % BATCH_SIZE == 0 || i == pharmacies.size() - 1) {
                        int[] results = ps.executeBatch();
                        insertedCount += results.length;
                        ps.clearBatch();
                        
                        // Log progress for large batches
                        if ((i + 1) % 10000 == 0) {
                            LOGGER.log(Level.INFO, "Inserted {0} of {1} pharmacies", 
                                new Object[]{i + 1, pharmacies.size()});
                        }
                    }
                }
                
                conn.commit();
                metrics.setRecordCount(insertedCount);
                metrics.setRecordSizeBytes(totalSize);
                LOGGER.log(Level.INFO, "Successfully inserted {0} pharmacies", insertedCount);
                
            } catch (SQLException e) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Failed to insert pharmacies batch", e);
                throw new SQLException(
                    String.format("Failed to insert pharmacies. Successfully inserted: %d of %d",
                        insertedCount, pharmacies.size()), e);
            } finally {
                conn.setAutoCommit(true);
                metrics.complete();
            }
        }
        
        return insertedCount;
    }
    
    @Override
    public Optional<Pharmacy> findById(Long id) throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics("Pharmacy", "SELECT_BY_ID");
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_BY_ID_SQL)) {
            
            ps.setLong(1, id);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Pharmacy pharmacy = mapResultSetToPharmacy(rs);
                    metrics.setRecordCount(1);
                    metrics.setRecordSizeBytes(estimatePharmacySize(pharmacy));
                    return Optional.of(pharmacy);
                }
            }
            
            return Optional.empty();
        } finally {
            metrics.complete();
        }
    }
    
    /**
     * Find pharmacy by NCPDP ID
     * @param ncpdpId NCPDP ID to search for
     * @return Optional containing the pharmacy if found
     * @throws SQLException if database error occurs
     */
    public Optional<Pharmacy> findByNcpdpId(String ncpdpId) throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics("Pharmacy", "SELECT_BY_NCPDP");
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_BY_NCPDP_SQL)) {
            
            ps.setString(1, ncpdpId);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Pharmacy pharmacy = mapResultSetToPharmacy(rs);
                    metrics.setRecordCount(1);
                    metrics.setRecordSizeBytes(estimatePharmacySize(pharmacy));
                    return Optional.of(pharmacy);
                }
            }
            
            return Optional.empty();
        } finally {
            metrics.complete();
        }
    }
    
    /**
     * Find pharmacies by state
     * @param state Two-letter state code
     * @return List of pharmacies in the state
     * @throws SQLException if database error occurs
     */
    public List<Pharmacy> findByState(String state) throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics("Pharmacy", "SELECT_BY_STATE");
        List<Pharmacy> pharmacies = new ArrayList<>();
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_BY_STATE_SQL)) {
            
            ps.setString(1, state);
            
            try (ResultSet rs = ps.executeQuery()) {
                long totalSize = 0;
                while (rs.next()) {
                    Pharmacy pharmacy = mapResultSetToPharmacy(rs);
                    pharmacies.add(pharmacy);
                    totalSize += estimatePharmacySize(pharmacy);
                }
                
                metrics.setRecordCount(pharmacies.size());
                metrics.setRecordSizeBytes(totalSize);
                return pharmacies;
            }
        } finally {
            metrics.complete();
        }
    }
    
    /**
     * Find pharmacies by type
     * @param type Pharmacy type
     * @return List of pharmacies of the specified type
     * @throws SQLException if database error occurs
     */
    public List<Pharmacy> findByType(PharmacyType type) throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics("Pharmacy", "SELECT_BY_TYPE");
        List<Pharmacy> pharmacies = new ArrayList<>();
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_BY_TYPE_SQL)) {
            
            ps.setString(1, type.name());
            
            try (ResultSet rs = ps.executeQuery()) {
                long totalSize = 0;
                while (rs.next()) {
                    Pharmacy pharmacy = mapResultSetToPharmacy(rs);
                    pharmacies.add(pharmacy);
                    totalSize += estimatePharmacySize(pharmacy);
                }
                
                metrics.setRecordCount(pharmacies.size());
                metrics.setRecordSizeBytes(totalSize);
                return pharmacies;
            }
        } finally {
            metrics.complete();
        }
    }
    
    /**
     * Find all active pharmacies
     * @return List of active pharmacies
     * @throws SQLException if database error occurs
     */
    public List<Pharmacy> findActivePharmacies() throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics("Pharmacy", "SELECT_ACTIVE");
        List<Pharmacy> pharmacies = new ArrayList<>();
        
        try (Connection conn = connector.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(FIND_ACTIVE_SQL)) {
            
            long totalSize = 0;
            while (rs.next()) {
                Pharmacy pharmacy = mapResultSetToPharmacy(rs);
                pharmacies.add(pharmacy);
                totalSize += estimatePharmacySize(pharmacy);
            }
            
            metrics.setRecordCount(pharmacies.size());
            metrics.setRecordSizeBytes(totalSize);
            LOGGER.log(Level.INFO, "Retrieved {0} active pharmacies", pharmacies.size());
            return pharmacies;
        } finally {
            metrics.complete();
        }
    }
    
    @Override
    public List<Pharmacy> findAll() throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics("Pharmacy", "SELECT_ALL");
        List<Pharmacy> pharmacies = new ArrayList<>();
        
        try (Connection conn = connector.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(FIND_ALL_SQL)) {
            
            long totalSize = 0;
            while (rs.next()) {
                Pharmacy pharmacy = mapResultSetToPharmacy(rs);
                pharmacies.add(pharmacy);
                totalSize += estimatePharmacySize(pharmacy);
            }
            
            metrics.setRecordCount(pharmacies.size());
            metrics.setRecordSizeBytes(totalSize);
            LOGGER.log(Level.INFO, "Retrieved {0} pharmacies", pharmacies.size());
            return pharmacies;
        } finally {
            metrics.complete();
        }
    }
    
    @Override
    public boolean update(Pharmacy pharmacy) throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics("Pharmacy", "UPDATE");
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE_SQL)) {
            
            setPharmacyParameters(ps, pharmacy);
            ps.setLong(11, pharmacy.getPharmacyId());
            
            int rowsAffected = ps.executeUpdate();
            boolean updated = rowsAffected > 0;
            
            if (updated) {
                metrics.setRecordCount(1);
                metrics.setRecordSizeBytes(estimatePharmacySize(pharmacy));
                LOGGER.log(Level.INFO, "Updated pharmacy: {0}", pharmacy.getPharmacyName());
            }
            
            return updated;
        } finally {
            metrics.complete();
        }
    }
    
    @Override
    public boolean delete(Long id) throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics("Pharmacy", "DELETE");
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(DELETE_SQL)) {
            
            ps.setLong(1, id);
            int rowsAffected = ps.executeUpdate();
            boolean deleted = rowsAffected > 0;
            
            if (deleted) {
                metrics.setRecordCount(1);
                LOGGER.log(Level.INFO, "Deleted pharmacy with ID: {0}", id);
            }
            
            return deleted;
        } finally {
            metrics.complete();
        }
    }
    
    @Override
    public long count() throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics("Pharmacy", "COUNT");
        
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
    public boolean exists(Long id) throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics("Pharmacy", "EXISTS");
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(EXISTS_SQL)) {
            
            ps.setLong(1, id);
            
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
     * Set PreparedStatement parameters from a Pharmacy object
     */
    private void setPharmacyParameters(PreparedStatement ps, Pharmacy pharmacy) throws SQLException {
        int idx = 1;
        ps.setString(idx++, pharmacy.getNcpdpId());
        ps.setString(idx++, pharmacy.getPharmacyName());
        ps.setString(idx++, pharmacy.getNpi());
        ps.setString(idx++, pharmacy.getAddress());
        ps.setString(idx++, pharmacy.getCity());
        ps.setString(idx++, pharmacy.getState());
        ps.setString(idx++, pharmacy.getZipCode());
        ps.setString(idx++, pharmacy.getPhone());
        ps.setString(idx++, pharmacy.getPharmacyType() != null ? pharmacy.getPharmacyType().name() : "RETAIL");
        ps.setBoolean(idx++, pharmacy.isActive());
    }
    
    /**
     * Estimates the size of a Pharmacy record in bytes
     */
    private long estimatePharmacySize(Pharmacy pharmacy) {
        long size = 0;
        
        // Long (8 bytes)
        size += 8;
        
        // Strings (2 bytes per character for UTF-16)
        if (pharmacy.getNcpdpId() != null) size += pharmacy.getNcpdpId().length() * 2L;
        if (pharmacy.getPharmacyName() != null) size += pharmacy.getPharmacyName().length() * 2L;
        if (pharmacy.getNpi() != null) size += pharmacy.getNpi().length() * 2L;
        if (pharmacy.getAddress() != null) size += pharmacy.getAddress().length() * 2L;
        if (pharmacy.getCity() != null) size += pharmacy.getCity().length() * 2L;
        if (pharmacy.getState() != null) size += pharmacy.getState().length() * 2L;
        if (pharmacy.getZipCode() != null) size += pharmacy.getZipCode().length() * 2L;
        if (pharmacy.getPhone() != null) size += pharmacy.getPhone().length() * 2L;
        
        // Enum (assume 20 bytes)
        size += 20;
        
        // Boolean (1 byte)
        size += 1;
        
        // Timestamps (8 bytes each)
        if (pharmacy.getCreatedAt() != null) size += 8;
        if (pharmacy.getUpdatedAt() != null) size += 8;
        
        return size;
    }
    
    /**
     * Map ResultSet row to Pharmacy object
     */
    private Pharmacy mapResultSetToPharmacy(ResultSet rs) throws SQLException {
        Pharmacy pharmacy = new Pharmacy();
        
        pharmacy.setPharmacyId(rs.getLong("pharmacy_id"));
        pharmacy.setNcpdpId(rs.getString("ncpdp_id"));
        pharmacy.setPharmacyName(rs.getString("pharmacy_name"));
        pharmacy.setNpi(rs.getString("npi"));
        pharmacy.setAddress(rs.getString("address"));
        pharmacy.setCity(rs.getString("city"));
        pharmacy.setState(rs.getString("state"));
        pharmacy.setZipCode(rs.getString("zip_code"));
        pharmacy.setPhone(rs.getString("phone"));
        
        String typeStr = rs.getString("pharmacy_type");
        if (typeStr != null) {
            pharmacy.setPharmacyType(PharmacyType.valueOf(typeStr.toUpperCase()));
        }
        
        pharmacy.setActive(rs.getBoolean("is_active"));
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            pharmacy.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            pharmacy.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        
        return pharmacy;
    }
}
