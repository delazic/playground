package dejanlazic.playground.inmemory.rdbms.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import dejanlazic.playground.inmemory.rdbms.DatabaseConnector;
import dejanlazic.playground.inmemory.rdbms.model.BenefitPlan;

/**
 * Data Access Object for BenefitPlan entity
 * Provides CRUD operations for benefit plans
 */
public class BenefitPlanDAO implements BaseDAO<BenefitPlan, UUID> {
    
    private static final Logger LOGGER = Logger.getLogger(BenefitPlanDAO.class.getName());
    
    private final DatabaseConnector connector;
    private static final int BATCH_SIZE = 1000;
    
    // SQL Statements
    private static final String INSERT_SQL = """
        INSERT INTO plan (
            plan_code, plan_name, plan_type, plan_category, effective_date,
            annual_deductible, out_of_pocket_max,
            tier1_copay, tier2_copay, tier3_copay, tier4_copay, tier5_copay,
            tier1_coinsurance, tier2_coinsurance, tier3_coinsurance, tier4_coinsurance, tier5_coinsurance,
            mail_order_available, specialty_pharmacy_required, description
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        RETURNING plan_id
        """;
    
    private static final String FIND_BY_ID_SQL = """
        SELECT plan_id, plan_code, plan_name, plan_type, plan_category, effective_date,
               termination_date, annual_deductible, out_of_pocket_max,
               tier1_copay, tier2_copay, tier3_copay, tier4_copay, tier5_copay,
               tier1_coinsurance, tier2_coinsurance, tier3_coinsurance, tier4_coinsurance, tier5_coinsurance,
               mail_order_available, specialty_pharmacy_required, description, is_active
        FROM plan WHERE plan_id = ?
        """;
    
    private static final String FIND_ALL_SQL = """
        SELECT plan_id, plan_code, plan_name, plan_type, plan_category, effective_date,
               termination_date, annual_deductible, out_of_pocket_max,
               tier1_copay, tier2_copay, tier3_copay, tier4_copay, tier5_copay,
               tier1_coinsurance, tier2_coinsurance, tier3_coinsurance, tier4_coinsurance, tier5_coinsurance,
               mail_order_available, specialty_pharmacy_required, description, is_active
        FROM plan ORDER BY plan_code
        """;
    
    private static final String UPDATE_SQL = """
        UPDATE plan SET
            plan_name = ?, plan_type = ?, plan_category = ?, effective_date = ?,
            termination_date = ?, annual_deductible = ?, out_of_pocket_max = ?,
            tier1_copay = ?, tier2_copay = ?, tier3_copay = ?, tier4_copay = ?, tier5_copay = ?,
            tier1_coinsurance = ?, tier2_coinsurance = ?, tier3_coinsurance = ?, tier4_coinsurance = ?, tier5_coinsurance = ?,
            mail_order_available = ?, specialty_pharmacy_required = ?, description = ?, is_active = ?
        WHERE plan_code = ?
        """;
    
    private static final String DELETE_SQL = "DELETE FROM plan WHERE plan_id = ?";
    
    private static final String COUNT_SQL = "SELECT COUNT(*) FROM plan";
    
    private static final String EXISTS_SQL = "SELECT EXISTS(SELECT 1 FROM plan WHERE plan_id = ?)";
    
    private static final String FIND_BY_CODE_SQL = """
        SELECT plan_id, plan_code, plan_name, plan_type, plan_category, effective_date,
               termination_date, annual_deductible, out_of_pocket_max,
               tier1_copay, tier2_copay, tier3_copay, tier4_copay, tier5_copay,
               tier1_coinsurance, tier2_coinsurance, tier3_coinsurance, tier4_coinsurance, tier5_coinsurance,
               mail_order_available, specialty_pharmacy_required, description, is_active
        FROM plan WHERE plan_code = ?
        """;
    
    public BenefitPlanDAO(DatabaseConnector connector) {
        this.connector = connector;
    }
    
    @Override
    public BenefitPlan insert(BenefitPlan plan) throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics("BenefitPlan", "INSERT");
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_SQL)) {
            
            setPlanParameters(ps, plan);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    metrics.setRecordCount(1);
                    metrics.setRecordSizeBytes(estimatePlanSize(plan));
                    LOGGER.log(Level.INFO, "Inserted plan: {0}", plan.getPlanCode());
                }
            }
            return plan;
        } finally {
            metrics.complete();
        }
    }
    
    @Override
    public int insertBatch(List<BenefitPlan> plans) throws SQLException {
        if (plans == null || plans.isEmpty()) {
            LOGGER.log(Level.WARNING, "No plans to insert");
            return 0;
        }
        
        PerformanceMetrics metrics = new PerformanceMetrics("BenefitPlan", "INSERT_BATCH");
        int insertedCount = 0;
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_SQL)) {
            
            conn.setAutoCommit(false);
            
            try {
                long totalSize = 0;
                for (int i = 0; i < plans.size(); i++) {
                    setPlanParameters(ps, plans.get(i));
                    ps.addBatch();
                    totalSize += estimatePlanSize(plans.get(i));
                    
                    if ((i + 1) % BATCH_SIZE == 0 || i == plans.size() - 1) {
                        int[] results = ps.executeBatch();
                        insertedCount += results.length;
                        ps.clearBatch();
                    }
                }
                
                conn.commit();
                metrics.setRecordCount(insertedCount);
                metrics.setRecordSizeBytes(totalSize);
                LOGGER.log(Level.INFO, "Successfully inserted {0} plans", insertedCount);
                
            } catch (SQLException e) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Failed to insert plans batch", e);
                throw new SQLException(
                    String.format("Failed to insert benefit plans. Successfully inserted: %d of %d",
                        insertedCount, plans.size()), e);
            } finally {
                conn.setAutoCommit(true);
                metrics.complete();
            }
        }
        
        return insertedCount;
    }
    
    @Override
    public Optional<BenefitPlan> findById(UUID id) throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics("BenefitPlan", "SELECT_BY_ID");
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_BY_ID_SQL)) {
            
            ps.setObject(1, id);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    BenefitPlan plan = mapResultSetToPlan(rs);
                    metrics.setRecordCount(1);
                    metrics.setRecordSizeBytes(estimatePlanSize(plan));
                    return Optional.of(plan);
                }
            }
            
            return Optional.empty();
        } finally {
            metrics.complete();
        }
    }
    
    /**
     * Find a plan by its plan code
     * @param planCode Plan code to search for
     * @return Optional containing the plan if found
     * @throws SQLException if database error occurs
     */
    public Optional<BenefitPlan> findByPlanCode(String planCode) throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics("BenefitPlan", "SELECT_BY_PLAN_CODE");
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_BY_CODE_SQL)) {
            
            ps.setString(1, planCode);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    BenefitPlan plan = mapResultSetToPlan(rs);
                    metrics.setRecordCount(1);
                    metrics.setRecordSizeBytes(estimatePlanSize(plan));
                    return Optional.of(plan);
                }
            }
            
            return Optional.empty();
        } finally {
            metrics.complete();
        }
    }
    
    @Override
    public List<BenefitPlan> findAll() throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics("BenefitPlan", "SELECT_ALL");
        List<BenefitPlan> plans = new ArrayList<>();
        
        try (Connection conn = connector.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(FIND_ALL_SQL)) {
            
            long totalSize = 0;
            while (rs.next()) {
                BenefitPlan plan = mapResultSetToPlan(rs);
                plans.add(plan);
                totalSize += estimatePlanSize(plan);
            }
            
            metrics.setRecordCount(plans.size());
            metrics.setRecordSizeBytes(totalSize);
            LOGGER.log(Level.INFO, "Retrieved {0} plans", plans.size());
            return plans;
        } finally {
            metrics.complete();
        }
    }
    
    @Override
    public boolean update(BenefitPlan plan) throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics("BenefitPlan", "UPDATE");
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE_SQL)) {
            
            int idx = 1;
            ps.setString(idx++, plan.getPlanName());
            ps.setString(idx++, plan.getPlanType());
            ps.setString(idx++, plan.getPlanCategory());
            ps.setDate(idx++, plan.getEffectiveDate() != null ? Date.valueOf(plan.getEffectiveDate()) : null);
            ps.setDate(idx++, null); // termination_date
            ps.setBigDecimal(idx++, plan.getAnnualDeductible());
            ps.setBigDecimal(idx++, plan.getOutOfPocketMax());
            ps.setBigDecimal(idx++, plan.getTier1Copay());
            ps.setBigDecimal(idx++, plan.getTier2Copay());
            ps.setBigDecimal(idx++, plan.getTier3Copay());
            ps.setBigDecimal(idx++, plan.getTier4Copay());
            ps.setBigDecimal(idx++, plan.getTier5Copay());
            ps.setBigDecimal(idx++, plan.getTier1Coinsurance());
            ps.setBigDecimal(idx++, plan.getTier2Coinsurance());
            ps.setBigDecimal(idx++, plan.getTier3Coinsurance());
            ps.setBigDecimal(idx++, plan.getTier4Coinsurance());
            ps.setBigDecimal(idx++, plan.getTier5Coinsurance());
            ps.setBoolean(idx++, plan.isMailOrderAvailable());
            ps.setBoolean(idx++, plan.isSpecialtyPharmacyRequired());
            ps.setString(idx++, plan.getDescription());
            ps.setBoolean(idx++, true); // is_active
            ps.setString(idx++, plan.getPlanCode());
            
            int rowsAffected = ps.executeUpdate();
            boolean updated = rowsAffected > 0;
            
            if (updated) {
                metrics.setRecordCount(1);
                metrics.setRecordSizeBytes(estimatePlanSize(plan));
                LOGGER.log(Level.INFO, "Updated plan: {0}", plan.getPlanCode());
            }
            
            return updated;
        } finally {
            metrics.complete();
        }
    }
    
    @Override
    public boolean delete(UUID id) throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics("BenefitPlan", "DELETE");
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(DELETE_SQL)) {
            
            ps.setObject(1, id);
            int rowsAffected = ps.executeUpdate();
            boolean deleted = rowsAffected > 0;
            
            if (deleted) {
                metrics.setRecordCount(1);
                LOGGER.log(Level.INFO, "Deleted plan with ID: {0}", id);
            }
            
            return deleted;
        } finally {
            metrics.complete();
        }
    }
    
    @Override
    public long count() throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics("BenefitPlan", "COUNT");
        
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
        PerformanceMetrics metrics = new PerformanceMetrics("BenefitPlan", "EXISTS");
        
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
     * Set PreparedStatement parameters from a BenefitPlan object
     */
    private void setPlanParameters(PreparedStatement ps, BenefitPlan plan) throws SQLException {
        int idx = 1;
        ps.setString(idx++, plan.getPlanCode());
        ps.setString(idx++, plan.getPlanName());
        ps.setString(idx++, plan.getPlanType());
        ps.setString(idx++, plan.getPlanCategory());
        ps.setDate(idx++, plan.getEffectiveDate() != null ? Date.valueOf(plan.getEffectiveDate()) : null);
        ps.setBigDecimal(idx++, plan.getAnnualDeductible());
        ps.setBigDecimal(idx++, plan.getOutOfPocketMax());
        ps.setBigDecimal(idx++, plan.getTier1Copay());
        ps.setBigDecimal(idx++, plan.getTier2Copay());
        ps.setBigDecimal(idx++, plan.getTier3Copay());
        ps.setBigDecimal(idx++, plan.getTier4Copay());
        ps.setBigDecimal(idx++, plan.getTier5Copay());
        ps.setBigDecimal(idx++, plan.getTier1Coinsurance());
        ps.setBigDecimal(idx++, plan.getTier2Coinsurance());
        ps.setBigDecimal(idx++, plan.getTier3Coinsurance());
        ps.setBigDecimal(idx++, plan.getTier4Coinsurance());
        ps.setBigDecimal(idx++, plan.getTier5Coinsurance());
        ps.setBoolean(idx++, plan.isMailOrderAvailable());
        ps.setBoolean(idx++, plan.isSpecialtyPharmacyRequired());
        ps.setString(idx++, plan.getDescription());
    }
    
    /**
     * Map ResultSet row to BenefitPlan object
     */
    /**
     * Estimates the size of a BenefitPlan record in bytes.
     * This is an approximation based on the data types and string lengths.
     *
     * @param plan The BenefitPlan to estimate
     * @return Estimated size in bytes
     */
    private long estimatePlanSize(BenefitPlan plan) {
        long size = 0;
        
        // UUID (16 bytes)
        size += 16;
        
        // Strings (2 bytes per character for UTF-16)
        if (plan.getPlanCode() != null) size += plan.getPlanCode().length() * 2L;
        if (plan.getPlanName() != null) size += plan.getPlanName().length() * 2L;
        if (plan.getPlanType() != null) size += plan.getPlanType().length() * 2L;
        if (plan.getPlanCategory() != null) size += plan.getPlanCategory().length() * 2L;
        if (plan.getDescription() != null) size += plan.getDescription().length() * 2L;
        
        // Dates (8 bytes each)
        if (plan.getEffectiveDate() != null) size += 8;
        
        // BigDecimal (approximately 16 bytes each for precision)
        size += 12 * 16; // 12 BigDecimal fields
        
        // Booleans (1 byte each)
        size += 3; // 3 boolean fields
        
        return size;
    }
    
    private BenefitPlan mapResultSetToPlan(ResultSet rs) throws SQLException {
        BenefitPlan plan = new BenefitPlan();
        
        plan.setPlanCode(rs.getString("plan_code"));
        plan.setPlanName(rs.getString("plan_name"));
        plan.setPlanType(rs.getString("plan_type"));
        plan.setPlanCategory(rs.getString("plan_category"));
        
        Date effectiveDate = rs.getDate("effective_date");
        if (effectiveDate != null) {
            plan.setEffectiveDate(effectiveDate.toLocalDate());
        }
        
        plan.setAnnualDeductible(rs.getBigDecimal("annual_deductible"));
        plan.setOutOfPocketMax(rs.getBigDecimal("out_of_pocket_max"));
        
        plan.setTier1Copay(rs.getBigDecimal("tier1_copay"));
        plan.setTier2Copay(rs.getBigDecimal("tier2_copay"));
        plan.setTier3Copay(rs.getBigDecimal("tier3_copay"));
        plan.setTier4Copay(rs.getBigDecimal("tier4_copay"));
        plan.setTier5Copay(rs.getBigDecimal("tier5_copay"));
        
        plan.setTier1Coinsurance(rs.getBigDecimal("tier1_coinsurance"));
        plan.setTier2Coinsurance(rs.getBigDecimal("tier2_coinsurance"));
        plan.setTier3Coinsurance(rs.getBigDecimal("tier3_coinsurance"));
        plan.setTier4Coinsurance(rs.getBigDecimal("tier4_coinsurance"));
        plan.setTier5Coinsurance(rs.getBigDecimal("tier5_coinsurance"));
        
        plan.setMailOrderAvailable(rs.getBoolean("mail_order_available"));
        plan.setSpecialtyPharmacyRequired(rs.getBoolean("specialty_pharmacy_required"));
        plan.setDescription(rs.getString("description"));
        
        return plan;
    }
}


