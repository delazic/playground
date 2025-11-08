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
import dejanlazic.playground.inmemory.rdbms.PerformanceMetrics;
import dejanlazic.playground.inmemory.rdbms.model.PharmacyNetwork;
import dejanlazic.playground.inmemory.rdbms.model.PharmacyNetwork.ContractType;
import dejanlazic.playground.inmemory.rdbms.model.PharmacyNetwork.NetworkStatus;
import dejanlazic.playground.inmemory.rdbms.model.PharmacyNetwork.NetworkTier;
import dejanlazic.playground.inmemory.rdbms.model.PharmacyNetwork.NetworkType;

/**
 * Data Access Object for PharmacyNetwork entity
 * Provides CRUD operations for pharmacy network assignments with performance metrics
 */
public class PharmacyNetworkDAO implements BaseDAO<PharmacyNetwork, UUID> {
    
    private static final Logger LOGGER = Logger.getLogger(PharmacyNetworkDAO.class.getName());
    
    private final DatabaseConnector connector;
    private final PharmacyDAO pharmacyDAO;
    private static final int BATCH_SIZE = 1000;
    
    // SQL Statements
    private static final String INSERT_SQL = """
        INSERT INTO pharmacy_network (
            pharmacy_id, network_name, network_type, network_tier, contract_type,
            effective_date, termination_date, status, reimbursement_rate, dispensing_fee,
            is_preferred, is_mail_order, is_specialty
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        RETURNING network_id
        """;
    
    private static final String FIND_BY_ID_SQL = """
        SELECT network_id, pharmacy_id, network_name, network_type, network_tier,
               contract_type, effective_date, termination_date, status,
               reimbursement_rate, dispensing_fee, is_preferred, is_mail_order,
               is_specialty, created_at, updated_at
        FROM pharmacy_network WHERE network_id = ?
        """;
    
    private static final String FIND_ALL_SQL = """
        SELECT network_id, pharmacy_id, network_name, network_type, network_tier,
               contract_type, effective_date, termination_date, status,
               reimbursement_rate, dispensing_fee, is_preferred, is_mail_order,
               is_specialty, created_at, updated_at
        FROM pharmacy_network ORDER BY network_name
        """;
    
    private static final String UPDATE_SQL = """
        UPDATE pharmacy_network SET
            pharmacy_id = ?, network_name = ?, network_type = ?, network_tier = ?,
            contract_type = ?, effective_date = ?, termination_date = ?, status = ?,
            reimbursement_rate = ?, dispensing_fee = ?, is_preferred = ?,
            is_mail_order = ?, is_specialty = ?
        WHERE network_id = ?
        """;
    
    private static final String DELETE_SQL = "DELETE FROM pharmacy_network WHERE network_id = ?";
    
    private static final String COUNT_SQL = "SELECT COUNT(*) FROM pharmacy_network";
    
    private static final String EXISTS_SQL = "SELECT EXISTS(SELECT 1 FROM pharmacy_network WHERE network_id = ?)";
    
    private static final String FIND_BY_PHARMACY_SQL = """
        SELECT network_id, pharmacy_id, network_name, network_type, network_tier,
               contract_type, effective_date, termination_date, status,
               reimbursement_rate, dispensing_fee, is_preferred, is_mail_order,
               is_specialty, created_at, updated_at
        FROM pharmacy_network WHERE pharmacy_id = ? ORDER BY network_name
        """;
    
    private static final String FIND_BY_TYPE_SQL = """
        SELECT network_id, pharmacy_id, network_name, network_type, network_tier,
               contract_type, effective_date, termination_date, status,
               reimbursement_rate, dispensing_fee, is_preferred, is_mail_order,
               is_specialty, created_at, updated_at
        FROM pharmacy_network WHERE network_type = ? ORDER BY network_name
        """;
    
    private static final String FIND_BY_STATUS_SQL = """
        SELECT network_id, pharmacy_id, network_name, network_type, network_tier,
               contract_type, effective_date, termination_date, status,
               reimbursement_rate, dispensing_fee, is_preferred, is_mail_order,
               is_specialty, created_at, updated_at
        FROM pharmacy_network WHERE status = ? ORDER BY network_name
        """;
    
    private static final String FIND_ACTIVE_SQL = """
        SELECT network_id, pharmacy_id, network_name, network_type, network_tier,
               contract_type, effective_date, termination_date, status,
               reimbursement_rate, dispensing_fee, is_preferred, is_mail_order,
               is_specialty, created_at, updated_at
        FROM pharmacy_network 
        WHERE status = 'ACTIVE' 
          AND effective_date <= CURRENT_DATE 
          AND (termination_date IS NULL OR termination_date >= CURRENT_DATE)
        ORDER BY network_name
        """;
    
    public PharmacyNetworkDAO(DatabaseConnector connector) {
        this.connector = connector;
        this.pharmacyDAO = new PharmacyDAO(connector);
    }
    
    @Override
    public PharmacyNetwork insert(PharmacyNetwork network) throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics("PharmacyNetwork", "INSERT");
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_SQL)) {
            
            setPharmacyNetworkParameters(ps, network);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    network.setNetworkId((UUID) rs.getObject(1));
                    metrics.setRecordCount(1);
                    metrics.setRecordSizeBytes(estimatePharmacyNetworkSize(network));
                    LOGGER.log(Level.INFO, "Inserted pharmacy network: {0}", network.getNetworkName());
                }
            }
            return network;
        } finally {
            metrics.complete();
        }
    }
    
    @Override
    public int insertBatch(List<PharmacyNetwork> networks) throws SQLException {
        if (networks == null || networks.isEmpty()) {
            LOGGER.log(Level.WARNING, "No pharmacy networks to insert");
            return 0;
        }
        
        PerformanceMetrics metrics = new PerformanceMetrics("PharmacyNetwork", "INSERT_BATCH");
        int insertedCount = 0;
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_SQL)) {
            
            conn.setAutoCommit(false);
            
            try {
                long totalSize = 0;
                for (int i = 0; i < networks.size(); i++) {
                    setPharmacyNetworkParameters(ps, networks.get(i));
                    ps.addBatch();
                    totalSize += estimatePharmacyNetworkSize(networks.get(i));
                    
                    if ((i + 1) % BATCH_SIZE == 0 || i == networks.size() - 1) {
                        int[] results = ps.executeBatch();
                        insertedCount += results.length;
                        ps.clearBatch();
                        
                        // Log progress for large batches
                        if ((i + 1) % 10000 == 0) {
                            LOGGER.log(Level.INFO, "Inserted {0} of {1} pharmacy networks", 
                                new Object[]{i + 1, networks.size()});
                        }
                    }
                }
                
                conn.commit();
                metrics.setRecordCount(insertedCount);
                metrics.setRecordSizeBytes(totalSize);
                LOGGER.log(Level.INFO, "Successfully inserted {0} pharmacy networks", insertedCount);
                
            } catch (SQLException e) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Failed to insert pharmacy networks batch", e);
                throw new SQLException(
                    String.format("Failed to insert pharmacy networks. Successfully inserted: %d of %d",
                        insertedCount, networks.size()), e);
            } finally {
                conn.setAutoCommit(true);
                metrics.complete();
            }
        }
        
        return insertedCount;
    }
    
    @Override
    public Optional<PharmacyNetwork> findById(UUID id) throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics("PharmacyNetwork", "SELECT_BY_ID");
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_BY_ID_SQL)) {
            
            ps.setObject(1, id);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    PharmacyNetwork network = mapResultSetToPharmacyNetwork(rs);
                    metrics.setRecordCount(1);
                    metrics.setRecordSizeBytes(estimatePharmacyNetworkSize(network));
                    return Optional.of(network);
                }
            }
            
            return Optional.empty();
        } finally {
            metrics.complete();
        }
    }
    
    /**
     * Find pharmacy networks by pharmacy ID
     * @param pharmacyId Pharmacy ID to search for
     * @return List of pharmacy networks for the pharmacy
     * @throws SQLException if database error occurs
     */
    public List<PharmacyNetwork> findByPharmacyId(UUID pharmacyId) throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics("PharmacyNetwork", "SELECT_BY_PHARMACY");
        List<PharmacyNetwork> networks = new ArrayList<>();
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_BY_PHARMACY_SQL)) {
            
            ps.setObject(1, pharmacyId);
            
            try (ResultSet rs = ps.executeQuery()) {
                long totalSize = 0;
                while (rs.next()) {
                    PharmacyNetwork network = mapResultSetToPharmacyNetwork(rs);
                    networks.add(network);
                    totalSize += estimatePharmacyNetworkSize(network);
                }
                
                metrics.setRecordCount(networks.size());
                metrics.setRecordSizeBytes(totalSize);
                return networks;
            }
        } finally {
            metrics.complete();
        }
    }
    
    /**
     * Find pharmacy networks by network type
     * @param type Network type
     * @return List of pharmacy networks of the specified type
     * @throws SQLException if database error occurs
     */
    public List<PharmacyNetwork> findByType(NetworkType type) throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics("PharmacyNetwork", "SELECT_BY_TYPE");
        List<PharmacyNetwork> networks = new ArrayList<>();
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_BY_TYPE_SQL)) {
            
            ps.setString(1, type.name());
            
            try (ResultSet rs = ps.executeQuery()) {
                long totalSize = 0;
                while (rs.next()) {
                    PharmacyNetwork network = mapResultSetToPharmacyNetwork(rs);
                    networks.add(network);
                    totalSize += estimatePharmacyNetworkSize(network);
                }
                
                metrics.setRecordCount(networks.size());
                metrics.setRecordSizeBytes(totalSize);
                return networks;
            }
        } finally {
            metrics.complete();
        }
    }
    
    /**
     * Find pharmacy networks by status
     * @param status Network status
     * @return List of pharmacy networks with the specified status
     * @throws SQLException if database error occurs
     */
    public List<PharmacyNetwork> findByStatus(NetworkStatus status) throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics("PharmacyNetwork", "SELECT_BY_STATUS");
        List<PharmacyNetwork> networks = new ArrayList<>();
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_BY_STATUS_SQL)) {
            
            ps.setString(1, status.name());
            
            try (ResultSet rs = ps.executeQuery()) {
                long totalSize = 0;
                while (rs.next()) {
                    PharmacyNetwork network = mapResultSetToPharmacyNetwork(rs);
                    networks.add(network);
                    totalSize += estimatePharmacyNetworkSize(network);
                }
                
                metrics.setRecordCount(networks.size());
                metrics.setRecordSizeBytes(totalSize);
                return networks;
            }
        } finally {
            metrics.complete();
        }
    }
    
    /**
     * Find all currently active pharmacy networks
     * @return List of active pharmacy networks
     * @throws SQLException if database error occurs
     */
    public List<PharmacyNetwork> findActiveNetworks() throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics("PharmacyNetwork", "SELECT_ACTIVE");
        List<PharmacyNetwork> networks = new ArrayList<>();
        
        try (Connection conn = connector.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(FIND_ACTIVE_SQL)) {
            
            long totalSize = 0;
            while (rs.next()) {
                PharmacyNetwork network = mapResultSetToPharmacyNetwork(rs);
                networks.add(network);
                totalSize += estimatePharmacyNetworkSize(network);
            }
            
            metrics.setRecordCount(networks.size());
            metrics.setRecordSizeBytes(totalSize);
            LOGGER.log(Level.INFO, "Retrieved {0} active pharmacy networks", networks.size());
            return networks;
        } finally {
            metrics.complete();
        }
    }
    
    @Override
    public List<PharmacyNetwork> findAll() throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics("PharmacyNetwork", "SELECT_ALL");
        List<PharmacyNetwork> networks = new ArrayList<>();
        
        try (Connection conn = connector.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(FIND_ALL_SQL)) {
            
            long totalSize = 0;
            while (rs.next()) {
                PharmacyNetwork network = mapResultSetToPharmacyNetwork(rs);
                networks.add(network);
                totalSize += estimatePharmacyNetworkSize(network);
            }
            
            metrics.setRecordCount(networks.size());
            metrics.setRecordSizeBytes(totalSize);
            LOGGER.log(Level.INFO, "Retrieved {0} pharmacy networks", networks.size());
            return networks;
        } finally {
            metrics.complete();
        }
    }
    
    @Override
    public boolean update(PharmacyNetwork network) throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics("PharmacyNetwork", "UPDATE");
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE_SQL)) {
            
            setPharmacyNetworkParameters(ps, network);
            ps.setObject(14, network.getNetworkId());
            
            int rowsAffected = ps.executeUpdate();
            boolean updated = rowsAffected > 0;
            
            if (updated) {
                metrics.setRecordCount(1);
                metrics.setRecordSizeBytes(estimatePharmacyNetworkSize(network));
                LOGGER.log(Level.INFO, "Updated pharmacy network: {0}", network.getNetworkName());
            }
            
            return updated;
        } finally {
            metrics.complete();
        }
    }
    
    @Override
    public boolean delete(UUID id) throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics("PharmacyNetwork", "DELETE");
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(DELETE_SQL)) {
            
            ps.setObject(1, id);
            int rowsAffected = ps.executeUpdate();
            boolean deleted = rowsAffected > 0;
            
            if (deleted) {
                metrics.setRecordCount(1);
                LOGGER.log(Level.INFO, "Deleted pharmacy network with ID: {0}", id);
            }
            
            return deleted;
        } finally {
            metrics.complete();
        }
    }
    
    @Override
    public long count() throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics("PharmacyNetwork", "COUNT");
        
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
        PerformanceMetrics metrics = new PerformanceMetrics("PharmacyNetwork", "EXISTS");
        
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
     * Set PreparedStatement parameters from a PharmacyNetwork object
     */
    private void setPharmacyNetworkParameters(PreparedStatement ps, PharmacyNetwork network) throws SQLException {
        int idx = 1;
        ps.setObject(idx++, network.getPharmacyId());
        ps.setString(idx++, network.getNetworkName());
        ps.setString(idx++, network.getNetworkType() != null ? network.getNetworkType().name() : "RETAIL");
        ps.setString(idx++, network.getNetworkTier() != null ? network.getNetworkTier().name() : "STANDARD");
        ps.setString(idx++, network.getContractType() != null ? network.getContractType().name() : "DIRECT");
        ps.setDate(idx++, network.getEffectiveDate() != null ? Date.valueOf(network.getEffectiveDate()) : null);
        ps.setDate(idx++, network.getTerminationDate() != null ? Date.valueOf(network.getTerminationDate()) : null);
        ps.setString(idx++, network.getStatus() != null ? network.getStatus().name() : "ACTIVE");
        ps.setString(idx++, network.getReimbursementRate());
        ps.setBigDecimal(idx++, network.getDispensingFee());
        ps.setBoolean(idx++, network.isPreferred());
        ps.setBoolean(idx++, network.isMailOrder());
        ps.setBoolean(idx++, network.isSpecialty());
    }
    
    /**
     * Estimates the size of a PharmacyNetwork record in bytes
     */
    private long estimatePharmacyNetworkSize(PharmacyNetwork network) {
        long size = 0;
        
        // UUIDs (16 bytes each)
        size += 32; // network_id + pharmacy_id
        
        // Strings (2 bytes per character for UTF-16)
        if (network.getNetworkName() != null) size += network.getNetworkName().length() * 2L;
        if (network.getReimbursementRate() != null) size += network.getReimbursementRate().length() * 2L;
        
        // Enums (assume 20 bytes each)
        size += 80; // 4 enums
        
        // Dates (8 bytes each)
        if (network.getEffectiveDate() != null) size += 8;
        if (network.getTerminationDate() != null) size += 8;
        
        // BigDecimal (assume 16 bytes)
        if (network.getDispensingFee() != null) size += 16;
        
        // Booleans (1 byte each)
        size += 3;
        
        // Timestamps (8 bytes each)
        if (network.getCreatedAt() != null) size += 8;
        if (network.getUpdatedAt() != null) size += 8;
        
        return size;
    }
    
    /**
     * Map ResultSet row to PharmacyNetwork object
     */
    private PharmacyNetwork mapResultSetToPharmacyNetwork(ResultSet rs) throws SQLException {
        PharmacyNetwork network = new PharmacyNetwork();
        
        network.setNetworkId((UUID) rs.getObject("network_id"));
        network.setPharmacyId((UUID) rs.getObject("pharmacy_id"));
        network.setNetworkName(rs.getString("network_name"));
        
        String typeStr = rs.getString("network_type");
        if (typeStr != null) {
            network.setNetworkType(NetworkType.valueOf(typeStr.toUpperCase().replace("-", "_")));
        }
        
        String tierStr = rs.getString("network_tier");
        if (tierStr != null) {
            network.setNetworkTier(NetworkTier.valueOf(tierStr.toUpperCase()));
        }
        
        String contractStr = rs.getString("contract_type");
        if (contractStr != null) {
            network.setContractType(ContractType.valueOf(contractStr.toUpperCase()));
        }
        
        Date effectiveDate = rs.getDate("effective_date");
        if (effectiveDate != null) {
            network.setEffectiveDate(effectiveDate.toLocalDate());
        }
        
        Date terminationDate = rs.getDate("termination_date");
        if (terminationDate != null) {
            network.setTerminationDate(terminationDate.toLocalDate());
        }
        
        String statusStr = rs.getString("status");
        if (statusStr != null) {
            network.setStatus(NetworkStatus.valueOf(statusStr.toUpperCase()));
        }
        
        network.setReimbursementRate(rs.getString("reimbursement_rate"));
        network.setDispensingFee(rs.getBigDecimal("dispensing_fee"));
        network.setPreferred(rs.getBoolean("is_preferred"));
        network.setMailOrder(rs.getBoolean("is_mail_order"));
        network.setSpecialty(rs.getBoolean("is_specialty"));
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            network.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            network.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        
        return network;
    }
}

// Made with Bob
