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
import dejanlazic.playground.inmemory.rdbms.model.PlanRule;

/**
 * Data Access Object for PlanRule entity
 * Provides CRUD operations for plan rules with performance metrics and logging
 */
public class PlanRuleDAO implements BaseDAO<PlanRule, Long> {
    
    private static final Logger LOGGER = Logger.getLogger(PlanRuleDAO.class.getName());
    private static final String LOG_PREFIX = "[PlanRuleDAO]";
    
    private final DatabaseConnector connector;
    private static final int BATCH_SIZE = 1000;
    
    // SQL Statements
    private static final String INSERT_SQL = """
        INSERT INTO plan_rule (
            plan_id, rule_type, rule_name, rule_criteria, rule_action,
            priority, is_active
        ) VALUES (?, ?, ?, ?::jsonb, ?::jsonb, ?, ?)
        RETURNING rule_id
        """;
    
    private static final String FIND_BY_ID_SQL = """
        SELECT rule_id, plan_id, rule_type, rule_name, rule_criteria, rule_action,
               priority, is_active, created_at, updated_at
        FROM plan_rule WHERE rule_id = ?
        """;
    
    private static final String FIND_ALL_SQL = """
        SELECT rule_id, plan_id, rule_type, rule_name, rule_criteria, rule_action,
               priority, is_active, created_at, updated_at
        FROM plan_rule ORDER BY plan_id, priority DESC, rule_id
        """;
    
    private static final String UPDATE_SQL = """
        UPDATE plan_rule SET
            rule_type = ?, rule_name = ?, rule_criteria = ?::jsonb, rule_action = ?::jsonb,
            priority = ?, is_active = ?
        WHERE rule_id = ?
        """;
    
    private static final String DELETE_SQL = "DELETE FROM plan_rule WHERE rule_id = ?";
    
    private static final String COUNT_SQL = "SELECT COUNT(*) FROM plan_rule";
    
    private static final String EXISTS_SQL = "SELECT EXISTS(SELECT 1 FROM plan_rule WHERE rule_id = ?)";
    
    private static final String FIND_BY_PLAN_ID_SQL = """
        SELECT rule_id, plan_id, rule_type, rule_name, rule_criteria, rule_action,
               priority, is_active, created_at, updated_at
        FROM plan_rule WHERE plan_id = ? ORDER BY priority DESC, rule_id
        """;
    
    private static final String FIND_BY_RULE_TYPE_SQL = """
        SELECT rule_id, plan_id, rule_type, rule_name, rule_criteria, rule_action,
               priority, is_active, created_at, updated_at
        FROM plan_rule WHERE rule_type = ? ORDER BY plan_id, priority DESC
        """;
    
    private static final String FIND_ACTIVE_RULES_SQL = """
        SELECT rule_id, plan_id, rule_type, rule_name, rule_criteria, rule_action,
               priority, is_active, created_at, updated_at
        FROM plan_rule WHERE is_active = true ORDER BY plan_id, priority DESC
        """;
    
    private static final String FIND_BY_PLAN_AND_TYPE_SQL = """
        SELECT rule_id, plan_id, rule_type, rule_name, rule_criteria, rule_action,
               priority, is_active, created_at, updated_at
        FROM plan_rule WHERE plan_id = ? AND rule_type = ? ORDER BY priority DESC
        """;
    
    public PlanRuleDAO(DatabaseConnector connector) {
        this.connector = connector;
    }
    
    @Override
    public PlanRule insert(PlanRule planRule) throws SQLException {
        System.out.println(LOG_PREFIX + " Creating plan rule: " + planRule.getRuleName());
        PerformanceMetrics metrics = new PerformanceMetrics("PlanRule", "INSERT");
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_SQL)) {
            
            setPlanRuleParameters(ps, planRule, false);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    planRule.setRuleId(rs.getLong(1));
                    metrics.setRecordCount(1);
                    metrics.setRecordSizeBytes(estimatePlanRuleSize(planRule));
                    System.out.println(LOG_PREFIX + " Successfully created plan rule with ID: " + planRule.getRuleId());
                    LOGGER.log(Level.INFO, "Inserted plan rule: {0}", planRule.getRuleId());
                }
            }
            return planRule;
        } finally {
            metrics.complete();
        }
    }
    
    @Override
    public int insertBatch(List<PlanRule> planRules) throws SQLException {
        if (planRules == null || planRules.isEmpty()) {
            System.out.println(LOG_PREFIX + " No plan rules to insert");
            LOGGER.log(Level.WARNING, "No plan rules to insert");
            return 0;
        }
        
        System.out.println(LOG_PREFIX + " Batch inserting " + planRules.size() + " plan rules");
        PerformanceMetrics metrics = new PerformanceMetrics("PlanRule", "INSERT_BATCH");
        int insertedCount = 0;
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_SQL)) {
            
            conn.setAutoCommit(false);
            
            try {
                long totalSize = 0;
                for (int i = 0; i < planRules.size(); i++) {
                    setPlanRuleParameters(ps, planRules.get(i), false);
                    ps.addBatch();
                    totalSize += estimatePlanRuleSize(planRules.get(i));
                    
                    if ((i + 1) % BATCH_SIZE == 0 || i == planRules.size() - 1) {
                        int[] results = ps.executeBatch();
                        insertedCount += results.length;
                        ps.clearBatch();
                        
                        // Log progress for large batches
                        if ((i + 1) % 5000 == 0) {
                            System.out.println(LOG_PREFIX + " Inserted " + (i + 1) + " of " + planRules.size() + " plan rules");
                            LOGGER.log(Level.INFO, "Inserted {0} of {1} plan rules", 
                                new Object[]{i + 1, planRules.size()});
                        }
                    }
                }
                
                conn.commit();
                metrics.setRecordCount(insertedCount);
                metrics.setRecordSizeBytes(totalSize);
                System.out.println(LOG_PREFIX + " Successfully inserted " + insertedCount + " plan rules");
                LOGGER.log(Level.INFO, "Successfully inserted {0} plan rules", insertedCount);
                
            } catch (SQLException e) {
                conn.rollback();
                System.err.println(LOG_PREFIX + " Failed to insert plan rules batch: " + e.getMessage());
                LOGGER.log(Level.SEVERE, "Failed to insert plan rules batch", e);
                throw new SQLException(
                    String.format("Failed to insert plan rules. Successfully inserted: %d of %d",
                        insertedCount, planRules.size()), e);
            } finally {
                conn.setAutoCommit(true);
                metrics.complete();
            }
        }
        
        return insertedCount;
    }
    
    @Override
    public Optional<PlanRule> findById(Long id) throws SQLException {
        System.out.println(LOG_PREFIX + " Finding plan rule by ID: " + id);
        PerformanceMetrics metrics = new PerformanceMetrics("PlanRule", "SELECT_BY_ID");
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_BY_ID_SQL)) {
            
            ps.setLong(1, id);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    PlanRule planRule = mapResultSetToPlanRule(rs);
                    metrics.setRecordCount(1);
                    metrics.setRecordSizeBytes(estimatePlanRuleSize(planRule));
                    System.out.println(LOG_PREFIX + " Found plan rule: " + planRule.getRuleName());
                    return Optional.of(planRule);
                }
            }
            
            System.out.println(LOG_PREFIX + " Plan rule not found with ID: " + id);
            return Optional.empty();
        } finally {
            metrics.complete();
        }
    }
    
    /**
     * Find all plan rules for a specific plan ID
     * @param planId Plan ID to search for
     * @return List of plan rules for the plan
     * @throws SQLException if database error occurs
     */
    public List<PlanRule> findByPlanId(Long planId) throws SQLException {
        System.out.println(LOG_PREFIX + " Finding plan rules for plan ID: " + planId);
        PerformanceMetrics metrics = new PerformanceMetrics("PlanRule", "SELECT_BY_PLAN_ID");
        List<PlanRule> planRules = new ArrayList<>();
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_BY_PLAN_ID_SQL)) {
            
            ps.setLong(1, planId);
            
            try (ResultSet rs = ps.executeQuery()) {
                long totalSize = 0;
                while (rs.next()) {
                    PlanRule planRule = mapResultSetToPlanRule(rs);
                    planRules.add(planRule);
                    totalSize += estimatePlanRuleSize(planRule);
                }
                
                metrics.setRecordCount(planRules.size());
                metrics.setRecordSizeBytes(totalSize);
                System.out.println(LOG_PREFIX + " Found " + planRules.size() + " plan rules for plan ID: " + planId);
            }
            
            return planRules;
        } finally {
            metrics.complete();
        }
    }
    
    /**
     * Find all plan rules of a specific type
     * @param ruleType Rule type to search for
     * @return List of plan rules with the type
     * @throws SQLException if database error occurs
     */
    public List<PlanRule> findByRuleType(String ruleType) throws SQLException {
        System.out.println(LOG_PREFIX + " Finding plan rules with type: " + ruleType);
        PerformanceMetrics metrics = new PerformanceMetrics("PlanRule", "SELECT_BY_RULE_TYPE");
        List<PlanRule> planRules = new ArrayList<>();
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_BY_RULE_TYPE_SQL)) {
            
            ps.setString(1, ruleType);
            
            try (ResultSet rs = ps.executeQuery()) {
                long totalSize = 0;
                while (rs.next()) {
                    PlanRule planRule = mapResultSetToPlanRule(rs);
                    planRules.add(planRule);
                    totalSize += estimatePlanRuleSize(planRule);
                }
                
                metrics.setRecordCount(planRules.size());
                metrics.setRecordSizeBytes(totalSize);
                System.out.println(LOG_PREFIX + " Found " + planRules.size() + " plan rules with type: " + ruleType);
            }
            
            return planRules;
        } finally {
            metrics.complete();
        }
    }
    
    /**
     * Find all active plan rules
     * @return List of active plan rules
     * @throws SQLException if database error occurs
     */
    public List<PlanRule> findActiveRules() throws SQLException {
        System.out.println(LOG_PREFIX + " Finding active plan rules");
        PerformanceMetrics metrics = new PerformanceMetrics("PlanRule", "SELECT_ACTIVE");
        List<PlanRule> planRules = new ArrayList<>();
        
        try (Connection conn = connector.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(FIND_ACTIVE_RULES_SQL)) {
            
            long totalSize = 0;
            while (rs.next()) {
                PlanRule planRule = mapResultSetToPlanRule(rs);
                planRules.add(planRule);
                totalSize += estimatePlanRuleSize(planRule);
            }
            
            metrics.setRecordCount(planRules.size());
            metrics.setRecordSizeBytes(totalSize);
            System.out.println(LOG_PREFIX + " Found " + planRules.size() + " active plan rules");
            
            return planRules;
        } finally {
            metrics.complete();
        }
    }
    
    /**
     * Find plan rules by plan ID and rule type
     * @param planId Plan ID to search for
     * @param ruleType Rule type to search for
     * @return List of matching plan rules
     * @throws SQLException if database error occurs
     */
    public List<PlanRule> findByPlanIdAndRuleType(Long planId, String ruleType) throws SQLException {
        System.out.println(LOG_PREFIX + " Finding plan rules for plan ID: " + planId + " and type: " + ruleType);
        PerformanceMetrics metrics = new PerformanceMetrics("PlanRule", "SELECT_BY_PLAN_AND_TYPE");
        List<PlanRule> planRules = new ArrayList<>();
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_BY_PLAN_AND_TYPE_SQL)) {
            
            ps.setLong(1, planId);
            ps.setString(2, ruleType);
            
            try (ResultSet rs = ps.executeQuery()) {
                long totalSize = 0;
                while (rs.next()) {
                    PlanRule planRule = mapResultSetToPlanRule(rs);
                    planRules.add(planRule);
                    totalSize += estimatePlanRuleSize(planRule);
                }
                
                metrics.setRecordCount(planRules.size());
                metrics.setRecordSizeBytes(totalSize);
                System.out.println(LOG_PREFIX + " Found " + planRules.size() + " plan rules");
            }
            
            return planRules;
        } finally {
            metrics.complete();
        }
    }
    
    @Override
    public List<PlanRule> findAll() throws SQLException {
        System.out.println(LOG_PREFIX + " Finding all plan rules");
        PerformanceMetrics metrics = new PerformanceMetrics("PlanRule", "SELECT_ALL");
        List<PlanRule> planRules = new ArrayList<>();
        
        try (Connection conn = connector.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(FIND_ALL_SQL)) {
            
            long totalSize = 0;
            while (rs.next()) {
                PlanRule planRule = mapResultSetToPlanRule(rs);
                planRules.add(planRule);
                totalSize += estimatePlanRuleSize(planRule);
            }
            
            metrics.setRecordCount(planRules.size());
            metrics.setRecordSizeBytes(totalSize);
            System.out.println(LOG_PREFIX + " Retrieved " + planRules.size() + " plan rules");
            LOGGER.log(Level.INFO, "Retrieved {0} plan rules", planRules.size());
            return planRules;
        } finally {
            metrics.complete();
        }
    }
    
    @Override
    public boolean update(PlanRule planRule) throws SQLException {
        System.out.println(LOG_PREFIX + " Updating plan rule: " + planRule.getRuleId());
        PerformanceMetrics metrics = new PerformanceMetrics("PlanRule", "UPDATE");
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE_SQL)) {
            
            setPlanRuleParameters(ps, planRule, true);
            
            int rowsAffected = ps.executeUpdate();
            boolean updated = rowsAffected > 0;
            
            if (updated) {
                metrics.setRecordCount(1);
                metrics.setRecordSizeBytes(estimatePlanRuleSize(planRule));
                System.out.println(LOG_PREFIX + " Successfully updated plan rule: " + planRule.getRuleId());
                LOGGER.log(Level.INFO, "Updated plan rule: {0}", planRule.getRuleId());
            } else {
                System.out.println(LOG_PREFIX + " No plan rule found to update with ID: " + planRule.getRuleId());
            }
            
            return updated;
        } finally {
            metrics.complete();
        }
    }
    
    @Override
    public boolean delete(Long id) throws SQLException {
        System.out.println(LOG_PREFIX + " Deleting plan rule with ID: " + id);
        PerformanceMetrics metrics = new PerformanceMetrics("PlanRule", "DELETE");
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(DELETE_SQL)) {
            
            ps.setLong(1, id);
            int rowsAffected = ps.executeUpdate();
            boolean deleted = rowsAffected > 0;
            
            if (deleted) {
                metrics.setRecordCount(1);
                System.out.println(LOG_PREFIX + " Successfully deleted plan rule with ID: " + id);
                LOGGER.log(Level.INFO, "Deleted plan rule with ID: {0}", id);
            } else {
                System.out.println(LOG_PREFIX + " No plan rule found to delete with ID: " + id);
            }
            
            return deleted;
        } finally {
            metrics.complete();
        }
    }
    
    @Override
    public long count() throws SQLException {
        System.out.println(LOG_PREFIX + " Counting plan rules");
        PerformanceMetrics metrics = new PerformanceMetrics("PlanRule", "COUNT");
        
        try (Connection conn = connector.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(COUNT_SQL)) {
            
            if (rs.next()) {
                long count = rs.getLong(1);
                metrics.setRecordCount(1);
                System.out.println(LOG_PREFIX + " Total plan rules: " + count);
                return count;
            }
            
            return 0;
        } finally {
            metrics.complete();
        }
    }
    
    @Override
    public boolean exists(Long id) throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics("PlanRule", "EXISTS");
        
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
     * Set PreparedStatement parameters from a PlanRule object
     * @param isUpdate true if this is for an UPDATE statement, false for INSERT
     */
    private void setPlanRuleParameters(PreparedStatement ps, PlanRule planRule, boolean isUpdate) throws SQLException {
        int idx = 1;
        
        if (!isUpdate) {
            // INSERT: plan_id comes first
            ps.setLong(idx++, planRule.getPlanId());
        }
        
        // Common fields for both INSERT and UPDATE
        ps.setString(idx++, planRule.getRuleType());
        ps.setString(idx++, planRule.getRuleName());
        ps.setString(idx++, planRule.getRuleCriteria());
        ps.setString(idx++, planRule.getRuleAction());
        ps.setObject(idx++, planRule.getPriority());
        ps.setBoolean(idx++, planRule.getIsActive() != null ? planRule.getIsActive() : true);
        
        if (isUpdate) {
            // UPDATE: rule_id comes last
            ps.setLong(idx++, planRule.getRuleId());
        }
    }
    
    /**
     * Estimates the size of a PlanRule record in bytes
     */
    private long estimatePlanRuleSize(PlanRule planRule) {
        long size = 0;
        
        // Longs and Integers (8 + 8 + 4 = 20 bytes)
        size += 20;
        
        // Strings (2 bytes per character for UTF-16)
        if (planRule.getRuleType() != null) size += planRule.getRuleType().length() * 2L;
        if (planRule.getRuleName() != null) size += planRule.getRuleName().length() * 2L;
        
        // JSONB fields (estimate based on string length)
        if (planRule.getRuleCriteria() != null) size += planRule.getRuleCriteria().length() * 2L;
        if (planRule.getRuleAction() != null) size += planRule.getRuleAction().length() * 2L;
        
        // Boolean (1 byte)
        size += 1;
        
        // Timestamps (8 bytes each, 2 fields = 16 bytes)
        size += 16;
        
        return size;
    }
    
    /**
     * Map ResultSet row to PlanRule object
     */
    private PlanRule mapResultSetToPlanRule(ResultSet rs) throws SQLException {
        PlanRule planRule = new PlanRule();
        
        planRule.setRuleId(rs.getLong("rule_id"));
        planRule.setPlanId(rs.getLong("plan_id"));
        planRule.setRuleType(rs.getString("rule_type"));
        planRule.setRuleName(rs.getString("rule_name"));
        planRule.setRuleCriteria(rs.getString("rule_criteria"));
        planRule.setRuleAction(rs.getString("rule_action"));
        
        Integer priority = (Integer) rs.getObject("priority");
        if (priority != null) {
            planRule.setPriority(priority);
        }
        
        Boolean isActive = (Boolean) rs.getObject("is_active");
        if (isActive != null) {
            planRule.setIsActive(isActive);
        }
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            planRule.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            planRule.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        
        return planRule;
    }
}

// Made with Bob
