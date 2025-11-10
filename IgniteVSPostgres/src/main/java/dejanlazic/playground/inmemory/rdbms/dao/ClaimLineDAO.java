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
import java.util.logging.Level;
import java.util.logging.Logger;

import dejanlazic.playground.inmemory.rdbms.DatabaseConnector;
import dejanlazic.playground.inmemory.rdbms.PerformanceMetrics;
import dejanlazic.playground.inmemory.rdbms.model.ClaimLine;

/**
 * Data Access Object for ClaimLine entity
 * Provides CRUD operations for claim lines with performance metrics
 */
public class ClaimLineDAO implements BaseDAO<ClaimLine, Long> {
    
    private static final Logger LOGGER = Logger.getLogger(ClaimLineDAO.class.getName());
    private static final String LOG_PREFIX = "[ClaimLineDAO]";
    
    private final DatabaseConnector connector;
    private static final int BATCH_SIZE = 1000;
    
    // SQL Statements
    private static final String INSERT_SQL = """
        INSERT INTO claim_line (
            claim_id, claim_number, line_number, service_date, ndc, drug_name,
            quantity_dispensed, days_supply, unit_of_measure, rendering_provider_npi,
            service_facility_npi, place_of_service, billed_amount, allowed_amount,
            paid_amount, patient_responsibility, copay_amount, coinsurance_amount,
            deductible_amount, ingredient_cost, dispensing_fee, sales_tax,
            line_status, denial_code, denial_reason, adjustment_reason, prior_auth_number,
            formulary_status, tier_level, daw_code, generic_indicator, brand_indicator,
            prescription_number, refill_number, date_written, prescriber_npi,
            processing_time_ms
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        RETURNING claim_line_id
        """;
    
    private static final String FIND_BY_ID_SQL = """
        SELECT claim_line_id, claim_id, claim_number, line_number, service_date, ndc, drug_name,
               quantity_dispensed, days_supply, unit_of_measure, rendering_provider_npi,
               service_facility_npi, place_of_service, billed_amount, allowed_amount,
               paid_amount, patient_responsibility, copay_amount, coinsurance_amount,
               deductible_amount, ingredient_cost, dispensing_fee, sales_tax,
               line_status, denial_code, denial_reason, adjustment_reason, prior_auth_number,
               formulary_status, tier_level, daw_code, generic_indicator, brand_indicator,
               prescription_number, refill_number, date_written, prescriber_npi,
               processing_time_ms, created_at, updated_at
        FROM claim_line WHERE claim_line_id = ?
        """;
    
    private static final String FIND_ALL_SQL = """
        SELECT claim_line_id, claim_id, claim_number, line_number, service_date, ndc, drug_name,
               quantity_dispensed, days_supply, unit_of_measure, rendering_provider_npi,
               service_facility_npi, place_of_service, billed_amount, allowed_amount,
               paid_amount, patient_responsibility, copay_amount, coinsurance_amount,
               deductible_amount, ingredient_cost, dispensing_fee, sales_tax,
               line_status, denial_code, denial_reason, adjustment_reason, prior_auth_number,
               formulary_status, tier_level, daw_code, generic_indicator, brand_indicator,
               prescription_number, refill_number, date_written, prescriber_npi,
               processing_time_ms, created_at, updated_at
        FROM claim_line ORDER BY claim_line_id
        """;
    
    private static final String UPDATE_SQL = """
        UPDATE claim_line SET
            line_status = ?, denial_code = ?, denial_reason = ?, adjustment_reason = ?,
            billed_amount = ?, allowed_amount = ?, paid_amount = ?, patient_responsibility = ?,
            copay_amount = ?, coinsurance_amount = ?, deductible_amount = ?
        WHERE claim_line_id = ?
        """;
    
    private static final String DELETE_SQL = "DELETE FROM claim_line WHERE claim_line_id = ?";
    
    private static final String COUNT_SQL = "SELECT COUNT(*) FROM claim_line";
    
    private static final String EXISTS_SQL = "SELECT EXISTS(SELECT 1 FROM claim_line WHERE claim_line_id = ?)";
    
    private static final String FIND_BY_CLAIM_ID_SQL = """
        SELECT claim_line_id, claim_id, claim_number, line_number, service_date, ndc, drug_name,
               quantity_dispensed, days_supply, unit_of_measure, rendering_provider_npi,
               service_facility_npi, place_of_service, billed_amount, allowed_amount,
               paid_amount, patient_responsibility, copay_amount, coinsurance_amount,
               deductible_amount, ingredient_cost, dispensing_fee, sales_tax,
               line_status, denial_code, denial_reason, adjustment_reason, prior_auth_number,
               formulary_status, tier_level, daw_code, generic_indicator, brand_indicator,
               prescription_number, refill_number, date_written, prescriber_npi,
               processing_time_ms, created_at, updated_at
        FROM claim_line WHERE claim_id = ? ORDER BY line_number
        """;
    
    private static final String FIND_BY_CLAIM_NUMBER_SQL = """
        SELECT claim_line_id, claim_id, claim_number, line_number, service_date, ndc, drug_name,
               quantity_dispensed, days_supply, unit_of_measure, rendering_provider_npi,
               service_facility_npi, place_of_service, billed_amount, allowed_amount,
               paid_amount, patient_responsibility, copay_amount, coinsurance_amount,
               deductible_amount, ingredient_cost, dispensing_fee, sales_tax,
               line_status, denial_code, denial_reason, adjustment_reason, prior_auth_number,
               formulary_status, tier_level, daw_code, generic_indicator, brand_indicator,
               prescription_number, refill_number, date_written, prescriber_npi,
               processing_time_ms, created_at, updated_at
        FROM claim_line WHERE claim_number = ? ORDER BY line_number
        """;
    
    private static final String FIND_BY_STATUS_SQL = """
        SELECT claim_line_id, claim_id, claim_number, line_number, service_date, ndc, drug_name,
               quantity_dispensed, days_supply, unit_of_measure, rendering_provider_npi,
               service_facility_npi, place_of_service, billed_amount, allowed_amount,
               paid_amount, patient_responsibility, copay_amount, coinsurance_amount,
               deductible_amount, ingredient_cost, dispensing_fee, sales_tax,
               line_status, denial_code, denial_reason, adjustment_reason, prior_auth_number,
               formulary_status, tier_level, daw_code, generic_indicator, brand_indicator,
               prescription_number, refill_number, date_written, prescriber_npi,
               processing_time_ms, created_at, updated_at
        FROM claim_line WHERE line_status = ? ORDER BY claim_line_id
        """;
    
    public ClaimLineDAO(DatabaseConnector connector) {
        this.connector = connector;
    }
    
    @Override
    public ClaimLine insert(ClaimLine claimLine) throws SQLException {
        System.out.println(LOG_PREFIX + " Creating claim line: " + claimLine);
        PerformanceMetrics metrics = new PerformanceMetrics("ClaimLine", "INSERT");
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_SQL)) {
            
            setClaimLineParameters(ps, claimLine);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    claimLine.setClaimLineId(rs.getLong(1));
                    metrics.setRecordCount(1);
                    metrics.setRecordSizeBytes(estimateClaimLineSize(claimLine));
                    System.out.println(LOG_PREFIX + " Successfully created claim line with ID: " + claimLine.getClaimLineId());
                    LOGGER.log(Level.INFO, "Inserted claim line: {0}", claimLine.getClaimLineId());
                }
            }
            return claimLine;
        } finally {
            metrics.complete();
        }
    }
    
    @Override
    public int insertBatch(List<ClaimLine> claimLines) throws SQLException {
        if (claimLines == null || claimLines.isEmpty()) {
            System.out.println(LOG_PREFIX + " No claim lines to insert");
            LOGGER.log(Level.WARNING, "No claim lines to insert");
            return 0;
        }
        
        System.out.println(LOG_PREFIX + " Batch inserting " + claimLines.size() + " claim lines");
        PerformanceMetrics metrics = new PerformanceMetrics("ClaimLine", "INSERT_BATCH");
        int insertedCount = 0;
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_SQL)) {
            
            conn.setAutoCommit(false);
            
            try {
                long totalSize = 0;
                for (int i = 0; i < claimLines.size(); i++) {
                    setClaimLineParameters(ps, claimLines.get(i));
                    ps.addBatch();
                    totalSize += estimateClaimLineSize(claimLines.get(i));
                    
                    if ((i + 1) % BATCH_SIZE == 0 || i == claimLines.size() - 1) {
                        int[] results = ps.executeBatch();
                        insertedCount += results.length;
                        ps.clearBatch();
                        
                        // Log progress for large batches
                        if ((i + 1) % 5000 == 0) {
                            System.out.println(LOG_PREFIX + " Inserted " + (i + 1) + " of " + claimLines.size() + " claim lines");
                            LOGGER.log(Level.INFO, "Inserted {0} of {1} claim lines", 
                                new Object[]{i + 1, claimLines.size()});
                        }
                    }
                }
                
                conn.commit();
                metrics.setRecordCount(insertedCount);
                metrics.setRecordSizeBytes(totalSize);
                System.out.println(LOG_PREFIX + " Successfully inserted " + insertedCount + " claim lines");
                LOGGER.log(Level.INFO, "Successfully inserted {0} claim lines", insertedCount);
                
            } catch (SQLException e) {
                conn.rollback();
                System.err.println(LOG_PREFIX + " Failed to insert claim lines batch: " + e.getMessage());
                LOGGER.log(Level.SEVERE, "Failed to insert claim lines batch", e);
                throw new SQLException(
                    String.format("Failed to insert claim lines. Successfully inserted: %d of %d",
                        insertedCount, claimLines.size()), e);
            } finally {
                conn.setAutoCommit(true);
                metrics.complete();
            }
        }
        
        return insertedCount;
    }
    
    @Override
    public Optional<ClaimLine> findById(Long id) throws SQLException {
        System.out.println(LOG_PREFIX + " Finding claim line by ID: " + id);
        PerformanceMetrics metrics = new PerformanceMetrics("ClaimLine", "SELECT_BY_ID");
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_BY_ID_SQL)) {
            
            ps.setLong(1, id);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ClaimLine claimLine = mapResultSetToClaimLine(rs);
                    metrics.setRecordCount(1);
                    metrics.setRecordSizeBytes(estimateClaimLineSize(claimLine));
                    System.out.println(LOG_PREFIX + " Found claim line: " + claimLine);
                    return Optional.of(claimLine);
                }
            }
            
            System.out.println(LOG_PREFIX + " Claim line not found with ID: " + id);
            return Optional.empty();
        } finally {
            metrics.complete();
        }
    }
    
    /**
     * Find all claim lines for a specific claim ID
     * @param claimId Claim ID to search for
     * @return List of claim lines for the claim
     * @throws SQLException if database error occurs
     */
    public List<ClaimLine> findByClaimId(Long claimId) throws SQLException {
        System.out.println(LOG_PREFIX + " Finding claim lines for claim ID: " + claimId);
        PerformanceMetrics metrics = new PerformanceMetrics("ClaimLine", "SELECT_BY_CLAIM_ID");
        List<ClaimLine> claimLines = new ArrayList<>();
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_BY_CLAIM_ID_SQL)) {
            
            ps.setLong(1, claimId);
            
            try (ResultSet rs = ps.executeQuery()) {
                long totalSize = 0;
                while (rs.next()) {
                    ClaimLine claimLine = mapResultSetToClaimLine(rs);
                    claimLines.add(claimLine);
                    totalSize += estimateClaimLineSize(claimLine);
                }
                
                metrics.setRecordCount(claimLines.size());
                metrics.setRecordSizeBytes(totalSize);
                System.out.println(LOG_PREFIX + " Found " + claimLines.size() + " claim lines for claim ID: " + claimId);
            }
            
            return claimLines;
        } finally {
            metrics.complete();
        }
    }
    
    /**
     * Find all claim lines for a specific claim number
     * @param claimNumber Claim number to search for
     * @return List of claim lines for the claim
     * @throws SQLException if database error occurs
     */
    public List<ClaimLine> findByClaimNumber(String claimNumber) throws SQLException {
        System.out.println(LOG_PREFIX + " Finding claim lines for claim number: " + claimNumber);
        PerformanceMetrics metrics = new PerformanceMetrics("ClaimLine", "SELECT_BY_CLAIM_NUMBER");
        List<ClaimLine> claimLines = new ArrayList<>();
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_BY_CLAIM_NUMBER_SQL)) {
            
            ps.setString(1, claimNumber);
            
            try (ResultSet rs = ps.executeQuery()) {
                long totalSize = 0;
                while (rs.next()) {
                    ClaimLine claimLine = mapResultSetToClaimLine(rs);
                    claimLines.add(claimLine);
                    totalSize += estimateClaimLineSize(claimLine);
                }
                
                metrics.setRecordCount(claimLines.size());
                metrics.setRecordSizeBytes(totalSize);
                System.out.println(LOG_PREFIX + " Found " + claimLines.size() + " claim lines for claim number: " + claimNumber);
            }
            
            return claimLines;
        } finally {
            metrics.complete();
        }
    }
    
    /**
     * Find all claim lines with a specific status
     * @param status Line status to search for
     * @return List of claim lines with the status
     * @throws SQLException if database error occurs
     */
    public List<ClaimLine> findByStatus(String status) throws SQLException {
        System.out.println(LOG_PREFIX + " Finding claim lines with status: " + status);
        PerformanceMetrics metrics = new PerformanceMetrics("ClaimLine", "SELECT_BY_STATUS");
        List<ClaimLine> claimLines = new ArrayList<>();
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_BY_STATUS_SQL)) {
            
            ps.setString(1, status);
            
            try (ResultSet rs = ps.executeQuery()) {
                long totalSize = 0;
                while (rs.next()) {
                    ClaimLine claimLine = mapResultSetToClaimLine(rs);
                    claimLines.add(claimLine);
                    totalSize += estimateClaimLineSize(claimLine);
                }
                
                metrics.setRecordCount(claimLines.size());
                metrics.setRecordSizeBytes(totalSize);
                System.out.println(LOG_PREFIX + " Found " + claimLines.size() + " claim lines with status: " + status);
            }
            
            return claimLines;
        } finally {
            metrics.complete();
        }
    }
    
    @Override
    public List<ClaimLine> findAll() throws SQLException {
        System.out.println(LOG_PREFIX + " Finding all claim lines");
        PerformanceMetrics metrics = new PerformanceMetrics("ClaimLine", "SELECT_ALL");
        List<ClaimLine> claimLines = new ArrayList<>();
        
        try (Connection conn = connector.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(FIND_ALL_SQL)) {
            
            long totalSize = 0;
            while (rs.next()) {
                ClaimLine claimLine = mapResultSetToClaimLine(rs);
                claimLines.add(claimLine);
                totalSize += estimateClaimLineSize(claimLine);
            }
            
            metrics.setRecordCount(claimLines.size());
            metrics.setRecordSizeBytes(totalSize);
            System.out.println(LOG_PREFIX + " Retrieved " + claimLines.size() + " claim lines");
            LOGGER.log(Level.INFO, "Retrieved {0} claim lines", claimLines.size());
            return claimLines;
        } finally {
            metrics.complete();
        }
    }
    
    @Override
    public boolean update(ClaimLine claimLine) throws SQLException {
        System.out.println(LOG_PREFIX + " Updating claim line: " + claimLine.getClaimLineId());
        PerformanceMetrics metrics = new PerformanceMetrics("ClaimLine", "UPDATE");
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE_SQL)) {
            
            int idx = 1;
            ps.setString(idx++, claimLine.getLineStatus());
            ps.setString(idx++, claimLine.getDenialCode());
            ps.setString(idx++, claimLine.getDenialReason());
            ps.setString(idx++, claimLine.getAdjustmentReason());
            ps.setBigDecimal(idx++, claimLine.getBilledAmount());
            ps.setBigDecimal(idx++, claimLine.getAllowedAmount());
            ps.setBigDecimal(idx++, claimLine.getPaidAmount());
            ps.setBigDecimal(idx++, claimLine.getPatientResponsibility());
            ps.setBigDecimal(idx++, claimLine.getCopayAmount());
            ps.setBigDecimal(idx++, claimLine.getCoinsuranceAmount());
            ps.setBigDecimal(idx++, claimLine.getDeductibleAmount());
            ps.setLong(idx++, claimLine.getClaimLineId());
            
            int rowsAffected = ps.executeUpdate();
            boolean updated = rowsAffected > 0;
            
            if (updated) {
                metrics.setRecordCount(1);
                metrics.setRecordSizeBytes(estimateClaimLineSize(claimLine));
                System.out.println(LOG_PREFIX + " Successfully updated claim line: " + claimLine.getClaimLineId());
                LOGGER.log(Level.INFO, "Updated claim line: {0}", claimLine.getClaimLineId());
            } else {
                System.out.println(LOG_PREFIX + " No claim line found to update with ID: " + claimLine.getClaimLineId());
            }
            
            return updated;
        } finally {
            metrics.complete();
        }
    }
    
    @Override
    public boolean delete(Long id) throws SQLException {
        System.out.println(LOG_PREFIX + " Deleting claim line with ID: " + id);
        PerformanceMetrics metrics = new PerformanceMetrics("ClaimLine", "DELETE");
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(DELETE_SQL)) {
            
            ps.setLong(1, id);
            int rowsAffected = ps.executeUpdate();
            boolean deleted = rowsAffected > 0;
            
            if (deleted) {
                metrics.setRecordCount(1);
                System.out.println(LOG_PREFIX + " Successfully deleted claim line with ID: " + id);
                LOGGER.log(Level.INFO, "Deleted claim line with ID: {0}", id);
            } else {
                System.out.println(LOG_PREFIX + " No claim line found to delete with ID: " + id);
            }
            
            return deleted;
        } finally {
            metrics.complete();
        }
    }
    
    @Override
    public long count() throws SQLException {
        System.out.println(LOG_PREFIX + " Counting claim lines");
        PerformanceMetrics metrics = new PerformanceMetrics("ClaimLine", "COUNT");
        
        try (Connection conn = connector.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(COUNT_SQL)) {
            
            if (rs.next()) {
                long count = rs.getLong(1);
                metrics.setRecordCount(1);
                System.out.println(LOG_PREFIX + " Total claim lines: " + count);
                return count;
            }
            
            return 0;
        } finally {
            metrics.complete();
        }
    }
    
    @Override
    public boolean exists(Long id) throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics("ClaimLine", "EXISTS");
        
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
     * Set PreparedStatement parameters from a ClaimLine object
     */
    private void setClaimLineParameters(PreparedStatement ps, ClaimLine claimLine) throws SQLException {
        int idx = 1;
        ps.setLong(idx++, claimLine.getClaimId());
        ps.setString(idx++, claimLine.getClaimNumber());
        ps.setInt(idx++, claimLine.getLineNumber());
        ps.setDate(idx++, claimLine.getServiceDate() != null ? Date.valueOf(claimLine.getServiceDate()) : null);
        ps.setString(idx++, claimLine.getNdc());
        ps.setString(idx++, claimLine.getDrugName());
        ps.setBigDecimal(idx++, claimLine.getQuantityDispensed());
        ps.setInt(idx++, claimLine.getDaysSupply());
        ps.setString(idx++, claimLine.getUnitOfMeasure());
        ps.setString(idx++, claimLine.getRenderingProviderNpi());
        ps.setString(idx++, claimLine.getServiceFacilityNpi());
        ps.setString(idx++, claimLine.getPlaceOfService());
        ps.setBigDecimal(idx++, claimLine.getBilledAmount());
        ps.setBigDecimal(idx++, claimLine.getAllowedAmount());
        ps.setBigDecimal(idx++, claimLine.getPaidAmount());
        ps.setBigDecimal(idx++, claimLine.getPatientResponsibility());
        ps.setBigDecimal(idx++, claimLine.getCopayAmount());
        ps.setBigDecimal(idx++, claimLine.getCoinsuranceAmount());
        ps.setBigDecimal(idx++, claimLine.getDeductibleAmount());
        ps.setBigDecimal(idx++, claimLine.getIngredientCost());
        ps.setBigDecimal(idx++, claimLine.getDispensingFee());
        ps.setBigDecimal(idx++, claimLine.getSalesTax());
        ps.setString(idx++, claimLine.getLineStatus());
        ps.setString(idx++, claimLine.getDenialCode());
        ps.setString(idx++, claimLine.getDenialReason());
        ps.setString(idx++, claimLine.getAdjustmentReason());
        ps.setString(idx++, claimLine.getPriorAuthNumber());
        ps.setString(idx++, claimLine.getFormularyStatus());
        ps.setObject(idx++, claimLine.getTierLevel());
        ps.setString(idx++, claimLine.getDawCode());
        ps.setObject(idx++, claimLine.getGenericIndicator());
        ps.setObject(idx++, claimLine.getBrandIndicator());
        ps.setString(idx++, claimLine.getPrescriptionNumber());
        ps.setObject(idx++, claimLine.getRefillNumber());
        ps.setDate(idx++, claimLine.getDateWritten() != null ? Date.valueOf(claimLine.getDateWritten()) : null);
        ps.setString(idx++, claimLine.getPrescriberNpi());
        ps.setObject(idx++, claimLine.getProcessingTimeMs());
    }
    
    /**
     * Estimates the size of a ClaimLine record in bytes
     */
    private long estimateClaimLineSize(ClaimLine claimLine) {
        long size = 0;
        
        // Longs and Integers (8 + 4 + 4 + 4 + 4 = 24 bytes)
        size += 24;
        
        // Strings (2 bytes per character for UTF-16)
        if (claimLine.getClaimNumber() != null) size += claimLine.getClaimNumber().length() * 2L;
        if (claimLine.getNdc() != null) size += claimLine.getNdc().length() * 2L;
        if (claimLine.getDrugName() != null) size += claimLine.getDrugName().length() * 2L;
        if (claimLine.getUnitOfMeasure() != null) size += claimLine.getUnitOfMeasure().length() * 2L;
        if (claimLine.getRenderingProviderNpi() != null) size += claimLine.getRenderingProviderNpi().length() * 2L;
        if (claimLine.getServiceFacilityNpi() != null) size += claimLine.getServiceFacilityNpi().length() * 2L;
        if (claimLine.getPlaceOfService() != null) size += claimLine.getPlaceOfService().length() * 2L;
        if (claimLine.getLineStatus() != null) size += claimLine.getLineStatus().length() * 2L;
        if (claimLine.getDenialCode() != null) size += claimLine.getDenialCode().length() * 2L;
        if (claimLine.getDenialReason() != null) size += claimLine.getDenialReason().length() * 2L;
        if (claimLine.getAdjustmentReason() != null) size += claimLine.getAdjustmentReason().length() * 2L;
        if (claimLine.getPriorAuthNumber() != null) size += claimLine.getPriorAuthNumber().length() * 2L;
        if (claimLine.getFormularyStatus() != null) size += claimLine.getFormularyStatus().length() * 2L;
        if (claimLine.getDawCode() != null) size += claimLine.getDawCode().length() * 2L;
        if (claimLine.getPrescriptionNumber() != null) size += claimLine.getPrescriptionNumber().length() * 2L;
        if (claimLine.getPrescriberNpi() != null) size += claimLine.getPrescriberNpi().length() * 2L;
        
        // BigDecimals (approximately 16 bytes each, 11 fields = 176 bytes)
        size += 176;
        
        // Booleans (1 byte each, 2 fields = 2 bytes)
        size += 2;
        
        // Dates (8 bytes each, 2 fields = 16 bytes)
        size += 16;
        
        // Timestamps (8 bytes each, 2 fields = 16 bytes)
        size += 16;
        
        return size;
    }
    
    /**
     * Map ResultSet row to ClaimLine object
     */
    private ClaimLine mapResultSetToClaimLine(ResultSet rs) throws SQLException {
        ClaimLine claimLine = new ClaimLine();
        
        claimLine.setClaimLineId(rs.getLong("claim_line_id"));
        claimLine.setClaimId(rs.getLong("claim_id"));
        claimLine.setClaimNumber(rs.getString("claim_number"));
        
        Integer lineNumber = (Integer) rs.getObject("line_number");
        if (lineNumber != null) {
            claimLine.setLineNumber(lineNumber);
        }
        
        Date serviceDate = rs.getDate("service_date");
        if (serviceDate != null) {
            claimLine.setServiceDate(serviceDate.toLocalDate());
        }
        
        claimLine.setNdc(rs.getString("ndc"));
        claimLine.setDrugName(rs.getString("drug_name"));
        claimLine.setQuantityDispensed(rs.getBigDecimal("quantity_dispensed"));
        
        Integer daysSupply = (Integer) rs.getObject("days_supply");
        if (daysSupply != null) {
            claimLine.setDaysSupply(daysSupply);
        }
        
        claimLine.setUnitOfMeasure(rs.getString("unit_of_measure"));
        claimLine.setRenderingProviderNpi(rs.getString("rendering_provider_npi"));
        claimLine.setServiceFacilityNpi(rs.getString("service_facility_npi"));
        claimLine.setPlaceOfService(rs.getString("place_of_service"));
        claimLine.setBilledAmount(rs.getBigDecimal("billed_amount"));
        claimLine.setAllowedAmount(rs.getBigDecimal("allowed_amount"));
        claimLine.setPaidAmount(rs.getBigDecimal("paid_amount"));
        claimLine.setPatientResponsibility(rs.getBigDecimal("patient_responsibility"));
        claimLine.setCopayAmount(rs.getBigDecimal("copay_amount"));
        claimLine.setCoinsuranceAmount(rs.getBigDecimal("coinsurance_amount"));
        claimLine.setDeductibleAmount(rs.getBigDecimal("deductible_amount"));
        claimLine.setIngredientCost(rs.getBigDecimal("ingredient_cost"));
        claimLine.setDispensingFee(rs.getBigDecimal("dispensing_fee"));
        claimLine.setSalesTax(rs.getBigDecimal("sales_tax"));
        claimLine.setLineStatus(rs.getString("line_status"));
        claimLine.setDenialCode(rs.getString("denial_code"));
        claimLine.setDenialReason(rs.getString("denial_reason"));
        claimLine.setAdjustmentReason(rs.getString("adjustment_reason"));
        claimLine.setPriorAuthNumber(rs.getString("prior_auth_number"));
        claimLine.setFormularyStatus(rs.getString("formulary_status"));
        
        Integer tierLevel = (Integer) rs.getObject("tier_level");
        if (tierLevel != null) {
            claimLine.setTierLevel(tierLevel);
        }
        
        claimLine.setDawCode(rs.getString("daw_code"));
        
        Boolean genericIndicator = (Boolean) rs.getObject("generic_indicator");
        if (genericIndicator != null) {
            claimLine.setGenericIndicator(genericIndicator);
        }
        
        Boolean brandIndicator = (Boolean) rs.getObject("brand_indicator");
        if (brandIndicator != null) {
            claimLine.setBrandIndicator(brandIndicator);
        }
        
        claimLine.setPrescriptionNumber(rs.getString("prescription_number"));
        
        Integer refillNumber = (Integer) rs.getObject("refill_number");
        if (refillNumber != null) {
            claimLine.setRefillNumber(refillNumber);
        }
        
        Date dateWritten = rs.getDate("date_written");
        if (dateWritten != null) {
            claimLine.setDateWritten(dateWritten.toLocalDate());
        }
        
        claimLine.setPrescriberNpi(rs.getString("prescriber_npi"));
        
        Integer processingTimeMs = (Integer) rs.getObject("processing_time_ms");
        if (processingTimeMs != null) {
            claimLine.setProcessingTimeMs(processingTimeMs);
        }
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            claimLine.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            claimLine.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        
        return claimLine;
    }
}

// Made with Bob
