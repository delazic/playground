package dejanlazic.playground.inmemory.rdbms.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Plan Rule entity representing configurable business logic for pharmacy benefit plans.
 * Defines coverage decisions, prior authorization requirements, quantity limits,
 * clinical edits, and other plan-specific rules.
 * 
 * Follows US healthcare PBM standards with flexible JSONB criteria and actions.
 */
public class PlanRule implements Serializable {
    private static final long serialVersionUID = 1L;
    
    // Primary identification
    private Long ruleId;
    private Long planId;
    
    // Rule definition
    private String ruleType;
    private String ruleName;
    private String ruleCriteria;  // JSONB stored as String
    private String ruleAction;    // JSONB stored as String
    
    // Rule configuration
    private Integer priority;
    private Boolean isActive;
    
    // Audit trail
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Constructors
    
    public PlanRule() {
        this.priority = 0;
        this.isActive = true;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public PlanRule(Long planId, String ruleType, String ruleName, 
                    String ruleCriteria, String ruleAction) {
        this();
        this.planId = planId;
        this.ruleType = ruleType;
        this.ruleName = ruleName;
        this.ruleCriteria = ruleCriteria;
        this.ruleAction = ruleAction;
    }
    
    public PlanRule(Long planId, String ruleType, String ruleName, 
                    String ruleCriteria, String ruleAction, Integer priority) {
        this(planId, ruleType, ruleName, ruleCriteria, ruleAction);
        this.priority = priority;
    }
    
    // Getters and Setters
    
    public Long getRuleId() {
        return ruleId;
    }
    
    public void setRuleId(Long ruleId) {
        this.ruleId = ruleId;
    }
    
    public Long getPlanId() {
        return planId;
    }
    
    public void setPlanId(Long planId) {
        this.planId = planId;
    }
    
    public String getRuleType() {
        return ruleType;
    }
    
    public void setRuleType(String ruleType) {
        this.ruleType = ruleType;
    }
    
    public String getRuleName() {
        return ruleName;
    }
    
    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }
    
    public String getRuleCriteria() {
        return ruleCriteria;
    }
    
    public void setRuleCriteria(String ruleCriteria) {
        this.ruleCriteria = ruleCriteria;
    }
    
    public String getRuleAction() {
        return ruleAction;
    }
    
    public void setRuleAction(String ruleAction) {
        this.ruleAction = ruleAction;
    }
    
    public Integer getPriority() {
        return priority;
    }
    
    public void setPriority(Integer priority) {
        this.priority = priority;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    // Utility methods
    
    /**
     * Check if this rule is currently active.
     */
    public boolean isActive() {
        return isActive != null && isActive;
    }
    
    /**
     * Check if this is a high priority rule (priority >= 50).
     */
    public boolean isHighPriority() {
        return priority != null && priority >= 50;
    }
    
    /**
     * Check if this is a low priority rule (priority < 0).
     */
    public boolean isLowPriority() {
        return priority != null && priority < 0;
    }
    
    /**
     * Get priority level description.
     */
    public String getPriorityLevel() {
        if (priority == null) {
            return "NORMAL";
        }
        if (priority >= 50) {
            return "HIGH";
        } else if (priority < 0) {
            return "LOW";
        } else {
            return "NORMAL";
        }
    }
    
    /**
     * Check if this is a coverage rule.
     */
    public boolean isCoverageRule() {
        return "COVERAGE".equals(ruleType);
    }
    
    /**
     * Check if this is a prior authorization rule.
     */
    public boolean isPriorAuthRule() {
        return "PRIOR_AUTH".equals(ruleType);
    }
    
    /**
     * Check if this is a quantity limit rule.
     */
    public boolean isQuantityLimitRule() {
        return "QUANTITY_LIMIT".equals(ruleType);
    }
    
    /**
     * Check if this is a cost share rule.
     */
    public boolean isCostShareRule() {
        return "COST_SHARE".equals(ruleType);
    }
    
    /**
     * Check if this is a clinical edit rule.
     */
    public boolean isClinicalEditRule() {
        return "CLINICAL_EDIT".equals(ruleType);
    }
    
    /**
     * Check if this is a refill restriction rule.
     */
    public boolean isRefillRestrictionRule() {
        return "REFILL_RESTRICTION".equals(ruleType);
    }
    
    /**
     * Check if this is a network restriction rule.
     */
    public boolean isNetworkRestrictionRule() {
        return "NETWORK_RESTRICTION".equals(ruleType);
    }
    
    /**
     * Check if this is a step therapy rule.
     */
    public boolean isStepTherapyRule() {
        return "STEP_THERAPY".equals(ruleType);
    }
    
    /**
     * Get rule type category for grouping.
     */
    public String getRuleCategory() {
        if (isCoverageRule() || isCostShareRule()) {
            return "FINANCIAL";
        } else if (isPriorAuthRule() || isStepTherapyRule()) {
            return "AUTHORIZATION";
        } else if (isQuantityLimitRule() || isRefillRestrictionRule()) {
            return "UTILIZATION";
        } else if (isClinicalEditRule()) {
            return "CLINICAL";
        } else if (isNetworkRestrictionRule()) {
            return "NETWORK";
        } else {
            return "OTHER";
        }
    }
    
    /**
     * Validate that required fields are present.
     */
    public boolean isValid() {
        return planId != null 
            && ruleType != null && !ruleType.trim().isEmpty()
            && ruleName != null && !ruleName.trim().isEmpty()
            && ruleCriteria != null && !ruleCriteria.trim().isEmpty()
            && ruleAction != null && !ruleAction.trim().isEmpty();
    }
    
    /**
     * Get a summary description of this rule.
     */
    public String getSummary() {
        return String.format("Rule #%d: %s [%s] - Priority: %d (%s) - Active: %s",
            ruleId != null ? ruleId : 0,
            ruleName,
            ruleType,
            priority != null ? priority : 0,
            getPriorityLevel(),
            isActive ? "Yes" : "No");
    }
    
    // Object methods
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlanRule planRule = (PlanRule) o;
        return Objects.equals(ruleId, planRule.ruleId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(ruleId);
    }
    
    @Override
    public String toString() {
        return "PlanRule{" +
                "ruleId=" + ruleId +
                ", planId=" + planId +
                ", ruleType='" + ruleType + '\'' +
                ", ruleName='" + ruleName + '\'' +
                ", priority=" + priority +
                ", isActive=" + isActive +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
    
    /**
     * Get detailed string representation including JSONB fields.
     */
    public String toDetailedString() {
        return "PlanRule{" +
                "ruleId=" + ruleId +
                ", planId=" + planId +
                ", ruleType='" + ruleType + '\'' +
                ", ruleName='" + ruleName + '\'' +
                ", ruleCriteria='" + (ruleCriteria != null && ruleCriteria.length() > 100 
                    ? ruleCriteria.substring(0, 100) + "..." 
                    : ruleCriteria) + '\'' +
                ", ruleAction='" + (ruleAction != null && ruleAction.length() > 100 
                    ? ruleAction.substring(0, 100) + "..." 
                    : ruleAction) + '\'' +
                ", priority=" + priority +
                ", isActive=" + isActive +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}

// Made with Bob
