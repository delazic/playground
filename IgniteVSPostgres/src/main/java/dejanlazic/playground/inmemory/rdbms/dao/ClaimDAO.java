package dejanlazic.playground.inmemory.rdbms.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import dejanlazic.playground.inmemory.rdbms.DatabaseConnector;
import dejanlazic.playground.inmemory.rdbms.model.Claim;

/**
 * Data Access Object for Claim entity.
 * Handles all database operations for pharmacy claims.
 */
public class ClaimDAO implements BaseDAO<Claim, Long> {
    
    private final DatabaseConnector connector;
    
    public ClaimDAO(DatabaseConnector connector) {
        this.connector = connector;
    }
    
    @Override
    public Optional<Claim> findById(Long id) throws SQLException {
        String sql = "SELECT * FROM claims WHERE claim_id = ?";
        
        try (Connection conn = connector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToClaim(rs));
                }
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * Find claim by claim number.
     */
    public Optional<Claim> findByClaimNumber(String claimNumber) throws SQLException {
        String sql = "SELECT * FROM claims WHERE claim_number = ?";
        
        try (Connection conn = connector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, claimNumber);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToClaim(rs));
                }
            }
        }
        
        return Optional.empty();
    }
    
    @Override
    public List<Claim> findAll() throws SQLException {
        String sql = "SELECT * FROM claims ORDER BY received_timestamp DESC LIMIT 1000";
        List<Claim> claims = new ArrayList<>();
        
        try (Connection conn = connector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                claims.add(mapResultSetToClaim(rs));
            }
        }
        
        return claims;
    }
    
    @Override
    public Claim insert(Claim claim) throws SQLException {
        String sql = """
            INSERT INTO claims (
                claim_number, transaction_type, received_timestamp, processed_timestamp,
                date_of_service, member_id, person_code, pharmacy_id, pharmacy_npi,
                prescription_number, ndc, drug_id, quantity_dispensed, days_supply,
                refill_number, daw_code, prescriber_npi, prescriber_id,
                ingredient_cost_submitted, dispensing_fee_submitted,
                patient_pay_amount, plan_pay_amount, tax_amount,
                status, response_code, response_message,
                processing_time_ms, deductible_applied, oop_applied
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        try (Connection conn = connector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            int idx = 1;
            stmt.setString(idx++, claim.getClaimNumber());
            stmt.setString(idx++, claim.getTransactionType());
            stmt.setTimestamp(idx++, Timestamp.valueOf(claim.getReceivedTimestamp()));
            stmt.setTimestamp(idx++, claim.getProcessedTimestamp() != null ? 
                Timestamp.valueOf(claim.getProcessedTimestamp()) : null);
            stmt.setDate(idx++, java.sql.Date.valueOf(claim.getDateOfService()));
            stmt.setLong(idx++, claim.getMemberId());
            stmt.setString(idx++, claim.getPersonCode());
            stmt.setLong(idx++, claim.getPharmacyId());
            stmt.setString(idx++, claim.getPharmacyNpi());
            stmt.setString(idx++, claim.getPrescriptionNumber());
            stmt.setString(idx++, claim.getNdc());
            stmt.setObject(idx++, claim.getDrugId());
            stmt.setBigDecimal(idx++, claim.getQuantityDispensed());
            stmt.setInt(idx++, claim.getDaysSupply());
            stmt.setInt(idx++, claim.getRefillNumber());
            stmt.setString(idx++, claim.getDawCode());
            stmt.setString(idx++, claim.getPrescriberNpi());
            stmt.setString(idx++, claim.getPrescriberId());
            stmt.setBigDecimal(idx++, claim.getIngredientCostSubmitted());
            stmt.setBigDecimal(idx++, claim.getDispensingFeeSubmitted());
            stmt.setBigDecimal(idx++, claim.getPatientPayAmount());
            stmt.setBigDecimal(idx++, claim.getPlanPayAmount());
            stmt.setBigDecimal(idx++, claim.getTaxAmount());
            stmt.setString(idx++, claim.getStatus());
            stmt.setString(idx++, claim.getResponseCode());
            stmt.setString(idx++, claim.getResponseMessage());
            stmt.setObject(idx++, claim.getProcessingTimeMs());
            stmt.setBigDecimal(idx++, claim.getDeductibleApplied());
            stmt.setBigDecimal(idx++, claim.getOopApplied());
            
            stmt.executeUpdate();
            return claim;
        }
    }
    
    @Override
    public int insertBatch(List<Claim> claims) throws SQLException {
        String sql = """
            INSERT INTO claims (
                claim_number, transaction_type, received_timestamp, processed_timestamp,
                date_of_service, member_id, person_code, pharmacy_id, pharmacy_npi,
                prescription_number, ndc, drug_id, quantity_dispensed, days_supply,
                refill_number, daw_code, prescriber_npi, prescriber_id,
                ingredient_cost_submitted, dispensing_fee_submitted,
                patient_pay_amount, plan_pay_amount, tax_amount,
                status, response_code, response_message,
                processing_time_ms, deductible_applied, oop_applied
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        try (Connection conn = connector.getConnection()) {
            conn.setAutoCommit(false);
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                int count = 0;
                int batchSize = 1000;
                
                for (Claim claim : claims) {
                    int idx = 1;
                    stmt.setString(idx++, claim.getClaimNumber());
                    stmt.setString(idx++, claim.getTransactionType());
                    stmt.setTimestamp(idx++, Timestamp.valueOf(claim.getReceivedTimestamp()));
                    stmt.setTimestamp(idx++, claim.getProcessedTimestamp() != null ? 
                        Timestamp.valueOf(claim.getProcessedTimestamp()) : null);
                    stmt.setDate(idx++, java.sql.Date.valueOf(claim.getDateOfService()));
                    stmt.setLong(idx++, claim.getMemberId());
                    stmt.setString(idx++, claim.getPersonCode());
                    stmt.setLong(idx++, claim.getPharmacyId());
                    stmt.setString(idx++, claim.getPharmacyNpi());
                    stmt.setString(idx++, claim.getPrescriptionNumber());
                    stmt.setString(idx++, claim.getNdc());
                    stmt.setObject(idx++, claim.getDrugId());
                    stmt.setBigDecimal(idx++, claim.getQuantityDispensed());
                    stmt.setInt(idx++, claim.getDaysSupply());
                    stmt.setInt(idx++, claim.getRefillNumber());
                    stmt.setString(idx++, claim.getDawCode());
                    stmt.setString(idx++, claim.getPrescriberNpi());
                    stmt.setString(idx++, claim.getPrescriberId());
                    stmt.setBigDecimal(idx++, claim.getIngredientCostSubmitted());
                    stmt.setBigDecimal(idx++, claim.getDispensingFeeSubmitted());
                    stmt.setBigDecimal(idx++, claim.getPatientPayAmount());
                    stmt.setBigDecimal(idx++, claim.getPlanPayAmount());
                    stmt.setBigDecimal(idx++, claim.getTaxAmount());
                    stmt.setString(idx++, claim.getStatus());
                    stmt.setString(idx++, claim.getResponseCode());
                    stmt.setString(idx++, claim.getResponseMessage());
                    stmt.setObject(idx++, claim.getProcessingTimeMs());
                    stmt.setBigDecimal(idx++, claim.getDeductibleApplied());
                    stmt.setBigDecimal(idx++, claim.getOopApplied());
                    
                    stmt.addBatch();
                    count++;
                    
                    if (count % batchSize == 0) {
                        stmt.executeBatch();
                        conn.commit();
                        
                        if (count % 10000 == 0) {
                            System.out.println("  Inserted " + count + " claims...");
                        }
                    }
                }
                
                // Execute remaining batch
                stmt.executeBatch();
                conn.commit();
                
                return count;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }
    
    @Override
    public boolean update(Claim claim) throws SQLException {
        String sql = """
            UPDATE claims SET
                status = ?, response_code = ?, response_message = ?,
                processed_timestamp = ?, processing_time_ms = ?,
                patient_pay_amount = ?, plan_pay_amount = ?,
                deductible_applied = ?, oop_applied = ?
            WHERE claim_id = ?
            """;
        
        try (Connection conn = connector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, claim.getStatus());
            stmt.setString(2, claim.getResponseCode());
            stmt.setString(3, claim.getResponseMessage());
            stmt.setTimestamp(4, claim.getProcessedTimestamp() != null ? 
                Timestamp.valueOf(claim.getProcessedTimestamp()) : null);
            stmt.setObject(5, claim.getProcessingTimeMs());
            stmt.setBigDecimal(6, claim.getPatientPayAmount());
            stmt.setBigDecimal(7, claim.getPlanPayAmount());
            stmt.setBigDecimal(8, claim.getDeductibleApplied());
            stmt.setBigDecimal(9, claim.getOopApplied());
            stmt.setLong(10, claim.getClaimId());
            
            return stmt.executeUpdate() > 0;
        }
    }
    
    @Override
    public boolean delete(Long id) throws SQLException {
        String sql = "DELETE FROM claims WHERE claim_id = ?";
        
        try (Connection conn = connector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        }
    }
    
    @Override
    public long count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM claims";
        
        try (Connection conn = connector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getLong(1);
            }
        }
        
        return 0;
    }
    
    @Override
    public boolean exists(Long id) throws SQLException {
        String sql = "SELECT EXISTS(SELECT 1 FROM claims WHERE claim_id = ?)";
        
        try (Connection conn = connector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getBoolean(1);
            }
        }
    }
    
    /**
     * Count claims by status.
     */
    public long countByStatus(String status) throws SQLException {
        String sql = "SELECT COUNT(*) FROM claims WHERE status = ?";
        
        try (Connection conn = connector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, status);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        
        return 0;
    }
    
    /**
     * Get average processing time in milliseconds.
     */
    public double getAverageProcessingTime() throws SQLException {
        String sql = "SELECT AVG(processing_time_ms) FROM claims WHERE processing_time_ms IS NOT NULL";
        
        try (Connection conn = connector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getDouble(1);
            }
        }
        
        return 0.0;
    }
    
    /**
     * Map ResultSet to Claim object.
     */
    private Claim mapResultSetToClaim(ResultSet rs) throws SQLException {
        Claim claim = new Claim();
        
        claim.setClaimId(rs.getLong("claim_id"));
        claim.setClaimNumber(rs.getString("claim_number"));
        claim.setTransactionType(rs.getString("transaction_type"));
        
        Timestamp receivedTs = rs.getTimestamp("received_timestamp");
        if (receivedTs != null) {
            claim.setReceivedTimestamp(receivedTs.toLocalDateTime());
        }
        
        Timestamp processedTs = rs.getTimestamp("processed_timestamp");
        if (processedTs != null) {
            claim.setProcessedTimestamp(processedTs.toLocalDateTime());
        }
        
        java.sql.Date serviceDate = rs.getDate("date_of_service");
        if (serviceDate != null) {
            claim.setDateOfService(serviceDate.toLocalDate());
        }
        
        claim.setMemberId(rs.getLong("member_id"));
        claim.setPersonCode(rs.getString("person_code"));
        claim.setPharmacyId(rs.getLong("pharmacy_id"));
        claim.setPharmacyNpi(rs.getString("pharmacy_npi"));
        claim.setPrescriptionNumber(rs.getString("prescription_number"));
        claim.setNdc(rs.getString("ndc"));
        
        Long drugId = (Long) rs.getObject("drug_id");
        claim.setDrugId(drugId);
        
        claim.setQuantityDispensed(rs.getBigDecimal("quantity_dispensed"));
        claim.setDaysSupply(rs.getInt("days_supply"));
        claim.setRefillNumber(rs.getInt("refill_number"));
        claim.setDawCode(rs.getString("daw_code"));
        claim.setPrescriberNpi(rs.getString("prescriber_npi"));
        claim.setPrescriberId(rs.getString("prescriber_id"));
        claim.setIngredientCostSubmitted(rs.getBigDecimal("ingredient_cost_submitted"));
        claim.setDispensingFeeSubmitted(rs.getBigDecimal("dispensing_fee_submitted"));
        claim.setPatientPayAmount(rs.getBigDecimal("patient_pay_amount"));
        claim.setPlanPayAmount(rs.getBigDecimal("plan_pay_amount"));
        claim.setTaxAmount(rs.getBigDecimal("tax_amount"));
        claim.setStatus(rs.getString("status"));
        claim.setResponseCode(rs.getString("response_code"));
        claim.setResponseMessage(rs.getString("response_message"));
        
        Integer processingTime = (Integer) rs.getObject("processing_time_ms");
        claim.setProcessingTimeMs(processingTime);
        
        claim.setDeductibleApplied(rs.getBigDecimal("deductible_applied"));
        claim.setOopApplied(rs.getBigDecimal("oop_applied"));
        
        return claim;
    }
}

// Made with Bob