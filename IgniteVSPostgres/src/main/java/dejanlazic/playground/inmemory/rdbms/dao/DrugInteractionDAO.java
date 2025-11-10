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
import dejanlazic.playground.inmemory.rdbms.model.DrugInteraction;

/**
 * Data Access Object for DrugInteraction entity
 * Provides CRUD operations for drug interactions with performance metrics
 */
public class DrugInteractionDAO implements BaseDAO<DrugInteraction, Long> {
    
    private static final Logger LOGGER = Logger.getLogger(DrugInteractionDAO.class.getName());
    
    private final DatabaseConnector connector;
    private static final int BATCH_SIZE = 1000;
    
    // SQL Statements
    private static final String INSERT_SQL = """
        INSERT INTO drug_interaction (
            interaction_code, drug_1_name, drug_1_ndc, drug_2_name, drug_2_ndc,
            severity_level, interaction_mechanism, clinical_effects, management_recommendation,
            evidence_level, onset_timing, documentation_source, requires_alert,
            requires_intervention, patient_counseling_required, prescriber_notification_required,
            last_reviewed_date, last_updated_date, active_status, reference_id, notes
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT (interaction_code) DO UPDATE SET
            drug_1_name = EXCLUDED.drug_1_name,
            drug_1_ndc = EXCLUDED.drug_1_ndc,
            drug_2_name = EXCLUDED.drug_2_name,
            drug_2_ndc = EXCLUDED.drug_2_ndc,
            severity_level = EXCLUDED.severity_level,
            interaction_mechanism = EXCLUDED.interaction_mechanism,
            clinical_effects = EXCLUDED.clinical_effects,
            management_recommendation = EXCLUDED.management_recommendation,
            evidence_level = EXCLUDED.evidence_level,
            onset_timing = EXCLUDED.onset_timing,
            documentation_source = EXCLUDED.documentation_source,
            requires_alert = EXCLUDED.requires_alert,
            requires_intervention = EXCLUDED.requires_intervention,
            patient_counseling_required = EXCLUDED.patient_counseling_required,
            prescriber_notification_required = EXCLUDED.prescriber_notification_required,
            last_reviewed_date = EXCLUDED.last_reviewed_date,
            last_updated_date = EXCLUDED.last_updated_date,
            active_status = EXCLUDED.active_status,
            reference_id = EXCLUDED.reference_id,
            notes = EXCLUDED.notes
        RETURNING interaction_id
        """;
    
    private static final String FIND_BY_ID_SQL = """
        SELECT interaction_id, interaction_code, drug_1_name, drug_1_ndc, drug_2_name, drug_2_ndc,
               severity_level, interaction_mechanism, clinical_effects, management_recommendation,
               evidence_level, onset_timing, documentation_source, requires_alert,
               requires_intervention, patient_counseling_required, prescriber_notification_required,
               last_reviewed_date, last_updated_date, active_status, reference_id, notes,
               created_at, updated_at
        FROM drug_interaction WHERE interaction_id = ?
        """;
    
    private static final String FIND_ALL_SQL = """
        SELECT interaction_id, interaction_code, drug_1_name, drug_1_ndc, drug_2_name, drug_2_ndc,
               severity_level, interaction_mechanism, clinical_effects, management_recommendation,
               evidence_level, onset_timing, documentation_source, requires_alert,
               requires_intervention, patient_counseling_required, prescriber_notification_required,
               last_reviewed_date, last_updated_date, active_status, reference_id, notes,
               created_at, updated_at
        FROM drug_interaction ORDER BY severity_level, drug_1_name
        """;
    
    private static final String UPDATE_SQL = """
        UPDATE drug_interaction SET
            drug_1_name = ?, drug_1_ndc = ?, drug_2_name = ?, drug_2_ndc = ?,
            severity_level = ?, interaction_mechanism = ?, clinical_effects = ?,
            management_recommendation = ?, evidence_level = ?, onset_timing = ?,
            documentation_source = ?, requires_alert = ?, requires_intervention = ?,
            patient_counseling_required = ?, prescriber_notification_required = ?,
            last_reviewed_date = ?, last_updated_date = ?, active_status = ?,
            reference_id = ?, notes = ?
        WHERE interaction_code = ?
        """;
    
    private static final String DELETE_SQL = "DELETE FROM drug_interaction WHERE interaction_id = ?";
    
    private static final String COUNT_SQL = "SELECT COUNT(*) FROM drug_interaction";
    
    private static final String EXISTS_SQL = "SELECT EXISTS(SELECT 1 FROM drug_interaction WHERE interaction_id = ?)";
    
    private static final String FIND_BY_INTERACTION_CODE_SQL = """
        SELECT interaction_id, interaction_code, drug_1_name, drug_1_ndc, drug_2_name, drug_2_ndc,
               severity_level, interaction_mechanism, clinical_effects, management_recommendation,
               evidence_level, onset_timing, documentation_source, requires_alert,
               requires_intervention, patient_counseling_required, prescriber_notification_required,
               last_reviewed_date, last_updated_date, active_status, reference_id, notes,
               created_at, updated_at
        FROM drug_interaction WHERE interaction_code = ?
        """;
    
    private static final String FIND_BY_DRUG_NDC_SQL = """
        SELECT interaction_id, interaction_code, drug_1_name, drug_1_ndc, drug_2_name, drug_2_ndc,
               severity_level, interaction_mechanism, clinical_effects, management_recommendation,
               evidence_level, onset_timing, documentation_source, requires_alert,
               requires_intervention, patient_counseling_required, prescriber_notification_required,
               last_reviewed_date, last_updated_date, active_status, reference_id, notes,
               created_at, updated_at
        FROM drug_interaction WHERE drug_1_ndc = ? OR drug_2_ndc = ?
        """;
    
    public DrugInteractionDAO(DatabaseConnector connector) {
        this.connector = connector;
    }
    
    @Override
    public DrugInteraction insert(DrugInteraction interaction) throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics("DrugInteraction", "INSERT");
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_SQL)) {
            
            setDrugInteractionParameters(ps, interaction);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    interaction.setInteractionId(rs.getLong(1));
                    metrics.setRecordCount(1);
                    metrics.setRecordSizeBytes(estimateDrugInteractionSize(interaction));
                    LOGGER.log(Level.INFO, "Inserted drug interaction: {0}", interaction.getInteractionCode());
                }
            }
            return interaction;
        } finally {
            metrics.complete();
        }
    }
    
    @Override
    public int insertBatch(List<DrugInteraction> interactions) throws SQLException {
        if (interactions == null || interactions.isEmpty()) {
            LOGGER.log(Level.WARNING, "No drug interactions to insert");
            return 0;
        }
        
        PerformanceMetrics metrics = new PerformanceMetrics("DrugInteraction", "INSERT_BATCH");
        int insertedCount = 0;
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_SQL)) {
            
            conn.setAutoCommit(false);
            
            try {
                long totalSize = 0;
                for (int i = 0; i < interactions.size(); i++) {
                    setDrugInteractionParameters(ps, interactions.get(i));
                    ps.addBatch();
                    totalSize += estimateDrugInteractionSize(interactions.get(i));
                    
                    if ((i + 1) % BATCH_SIZE == 0 || i == interactions.size() - 1) {
                        int[] results = ps.executeBatch();
                        insertedCount += results.length;
                        ps.clearBatch();
                        
                        // Log progress for large batches
                        if ((i + 1) % 5000 == 0) {
                            LOGGER.log(Level.INFO, "Inserted {0} of {1} drug interactions", 
                                new Object[]{i + 1, interactions.size()});
                        }
                    }
                }
                
                conn.commit();
                metrics.setRecordCount(insertedCount);
                metrics.setRecordSizeBytes(totalSize);
                LOGGER.log(Level.INFO, "Successfully inserted {0} drug interactions", insertedCount);
                
            } catch (SQLException e) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Failed to insert drug interactions batch", e);
                throw new SQLException(
                    String.format("Failed to insert drug interactions. Successfully inserted: %d of %d",
                        insertedCount, interactions.size()), e);
            } finally {
                conn.setAutoCommit(true);
                metrics.complete();
            }
        }
        
        return insertedCount;
    }
    
    @Override
    public Optional<DrugInteraction> findById(Long id) throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics("DrugInteraction", "SELECT_BY_ID");
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_BY_ID_SQL)) {
            
            ps.setLong(1, id);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    DrugInteraction interaction = mapResultSetToDrugInteraction(rs);
                    metrics.setRecordCount(1);
                    metrics.setRecordSizeBytes(estimateDrugInteractionSize(interaction));
                    return Optional.of(interaction);
                }
            }
            
            return Optional.empty();
        } finally {
            metrics.complete();
        }
    }
    
    /**
     * Find a drug interaction by interaction code
     * @param interactionCode Interaction code to search for
     * @return Optional containing the interaction if found
     * @throws SQLException if database error occurs
     */
    public Optional<DrugInteraction> findByInteractionCode(String interactionCode) throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics("DrugInteraction", "SELECT_BY_CODE");
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_BY_INTERACTION_CODE_SQL)) {
            
            ps.setString(1, interactionCode);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    DrugInteraction interaction = mapResultSetToDrugInteraction(rs);
                    metrics.setRecordCount(1);
                    metrics.setRecordSizeBytes(estimateDrugInteractionSize(interaction));
                    return Optional.of(interaction);
                }
            }
            
            return Optional.empty();
        } finally {
            metrics.complete();
        }
    }
    
    /**
     * Find drug interactions involving a specific drug by NDC
     * @param ndcCode NDC code to search for
     * @return List of matching interactions
     * @throws SQLException if database error occurs
     */
    public List<DrugInteraction> findByDrugNdc(String ndcCode) throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics("DrugInteraction", "SELECT_BY_DRUG_NDC");
        List<DrugInteraction> interactions = new ArrayList<>();
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_BY_DRUG_NDC_SQL)) {
            
            ps.setString(1, ndcCode);
            ps.setString(2, ndcCode);
            
            long totalSize = 0;
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DrugInteraction interaction = mapResultSetToDrugInteraction(rs);
                    interactions.add(interaction);
                    totalSize += estimateDrugInteractionSize(interaction);
                }
            }
            
            metrics.setRecordCount(interactions.size());
            metrics.setRecordSizeBytes(totalSize);
            return interactions;
        } finally {
            metrics.complete();
        }
    }
    
    @Override
    public List<DrugInteraction> findAll() throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics("DrugInteraction", "SELECT_ALL");
        List<DrugInteraction> interactions = new ArrayList<>();
        
        try (Connection conn = connector.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(FIND_ALL_SQL)) {
            
            long totalSize = 0;
            while (rs.next()) {
                DrugInteraction interaction = mapResultSetToDrugInteraction(rs);
                interactions.add(interaction);
                totalSize += estimateDrugInteractionSize(interaction);
            }
            
            metrics.setRecordCount(interactions.size());
            metrics.setRecordSizeBytes(totalSize);
            LOGGER.log(Level.INFO, "Retrieved {0} drug interactions", interactions.size());
            return interactions;
        } finally {
            metrics.complete();
        }
    }
    
    @Override
    public boolean update(DrugInteraction interaction) throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics("DrugInteraction", "UPDATE");
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE_SQL)) {
            
            int idx = 1;
            ps.setString(idx++, interaction.getDrug1Name());
            ps.setString(idx++, interaction.getDrug1Ndc());
            ps.setString(idx++, interaction.getDrug2Name());
            ps.setString(idx++, interaction.getDrug2Ndc());
            ps.setString(idx++, interaction.getSeverityLevel());
            ps.setString(idx++, interaction.getInteractionMechanism());
            ps.setString(idx++, interaction.getClinicalEffects());
            ps.setString(idx++, interaction.getManagementRecommendation());
            ps.setString(idx++, interaction.getEvidenceLevel());
            ps.setString(idx++, interaction.getOnsetTiming());
            ps.setString(idx++, interaction.getDocumentationSource());
            ps.setBoolean(idx++, interaction.isRequiresAlert());
            ps.setBoolean(idx++, interaction.isRequiresIntervention());
            ps.setBoolean(idx++, interaction.isPatientCounselingRequired());
            ps.setBoolean(idx++, interaction.isPrescriberNotificationRequired());
            ps.setDate(idx++, interaction.getLastReviewedDate() != null ? 
                Date.valueOf(interaction.getLastReviewedDate()) : null);
            ps.setDate(idx++, interaction.getLastUpdatedDate() != null ? 
                Date.valueOf(interaction.getLastUpdatedDate()) : null);
            ps.setString(idx++, interaction.getActiveStatus());
            ps.setString(idx++, interaction.getReferenceId());
            ps.setString(idx++, interaction.getNotes());
            ps.setString(idx++, interaction.getInteractionCode());
            
            int rowsAffected = ps.executeUpdate();
            boolean updated = rowsAffected > 0;
            
            if (updated) {
                metrics.setRecordCount(1);
                metrics.setRecordSizeBytes(estimateDrugInteractionSize(interaction));
                LOGGER.log(Level.INFO, "Updated drug interaction: {0}", interaction.getInteractionCode());
            }
            
            return updated;
        } finally {
            metrics.complete();
        }
    }
    
    @Override
    public boolean delete(Long id) throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics("DrugInteraction", "DELETE");
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(DELETE_SQL)) {
            
            ps.setLong(1, id);
            int rowsAffected = ps.executeUpdate();
            boolean deleted = rowsAffected > 0;
            
            if (deleted) {
                metrics.setRecordCount(1);
                LOGGER.log(Level.INFO, "Deleted drug interaction with ID: {0}", id);
            }
            
            return deleted;
        } finally {
            metrics.complete();
        }
    }
    
    @Override
    public long count() throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics("DrugInteraction", "COUNT");
        
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
    public boolean exists(Long id) throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics("DrugInteraction", "EXISTS");
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(EXISTS_SQL)) {
            
            ps.setLong(1, id);
            
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
     * Set PreparedStatement parameters from a DrugInteraction object
     */
    private void setDrugInteractionParameters(PreparedStatement ps, DrugInteraction interaction) throws SQLException {
        int idx = 1;
        ps.setString(idx++, interaction.getInteractionCode());
        ps.setString(idx++, interaction.getDrug1Name());
        ps.setString(idx++, interaction.getDrug1Ndc());
        ps.setString(idx++, interaction.getDrug2Name());
        ps.setString(idx++, interaction.getDrug2Ndc());
        ps.setString(idx++, interaction.getSeverityLevel());
        ps.setString(idx++, interaction.getInteractionMechanism());
        ps.setString(idx++, interaction.getClinicalEffects());
        ps.setString(idx++, interaction.getManagementRecommendation());
        ps.setString(idx++, interaction.getEvidenceLevel());
        ps.setString(idx++, interaction.getOnsetTiming());
        ps.setString(idx++, interaction.getDocumentationSource());
        ps.setBoolean(idx++, interaction.isRequiresAlert());
        ps.setBoolean(idx++, interaction.isRequiresIntervention());
        ps.setBoolean(idx++, interaction.isPatientCounselingRequired());
        ps.setBoolean(idx++, interaction.isPrescriberNotificationRequired());
        ps.setDate(idx++, interaction.getLastReviewedDate() != null ? 
            Date.valueOf(interaction.getLastReviewedDate()) : null);
        ps.setDate(idx++, interaction.getLastUpdatedDate() != null ? 
            Date.valueOf(interaction.getLastUpdatedDate()) : null);
        ps.setString(idx++, interaction.getActiveStatus());
        ps.setString(idx++, interaction.getReferenceId());
        ps.setString(idx++, interaction.getNotes());
    }
    
    /**
     * Estimates the size of a DrugInteraction record in bytes.
     * This is an approximation based on the data types and string lengths.
     * 
     * @param interaction The DrugInteraction to estimate
     * @return Estimated size in bytes
     */
    private long estimateDrugInteractionSize(DrugInteraction interaction) {
        long size = 0;
        
        // Long (8 bytes)
        size += 8;
        
        // Strings (2 bytes per character for UTF-16)
        if (interaction.getInteractionCode() != null) size += interaction.getInteractionCode().length() * 2L;
        if (interaction.getDrug1Name() != null) size += interaction.getDrug1Name().length() * 2L;
        if (interaction.getDrug1Ndc() != null) size += interaction.getDrug1Ndc().length() * 2L;
        if (interaction.getDrug2Name() != null) size += interaction.getDrug2Name().length() * 2L;
        if (interaction.getDrug2Ndc() != null) size += interaction.getDrug2Ndc().length() * 2L;
        if (interaction.getSeverityLevel() != null) size += interaction.getSeverityLevel().length() * 2L;
        if (interaction.getInteractionMechanism() != null) size += interaction.getInteractionMechanism().length() * 2L;
        if (interaction.getClinicalEffects() != null) size += interaction.getClinicalEffects().length() * 2L;
        if (interaction.getManagementRecommendation() != null) size += interaction.getManagementRecommendation().length() * 2L;
        if (interaction.getEvidenceLevel() != null) size += interaction.getEvidenceLevel().length() * 2L;
        if (interaction.getOnsetTiming() != null) size += interaction.getOnsetTiming().length() * 2L;
        if (interaction.getDocumentationSource() != null) size += interaction.getDocumentationSource().length() * 2L;
        if (interaction.getActiveStatus() != null) size += interaction.getActiveStatus().length() * 2L;
        if (interaction.getReferenceId() != null) size += interaction.getReferenceId().length() * 2L;
        if (interaction.getNotes() != null) size += interaction.getNotes().length() * 2L;
        
        // Booleans (1 byte each)
        size += 4;
        
        // Dates (8 bytes each)
        if (interaction.getLastReviewedDate() != null) size += 8;
        if (interaction.getLastUpdatedDate() != null) size += 8;
        if (interaction.getCreatedAt() != null) size += 8;
        if (interaction.getUpdatedAt() != null) size += 8;
        
        return size;
    }
    
    /**
     * Map ResultSet row to DrugInteraction object
     */
    private DrugInteraction mapResultSetToDrugInteraction(ResultSet rs) throws SQLException {
        DrugInteraction interaction = new DrugInteraction();
        
        interaction.setInteractionId(rs.getLong("interaction_id"));
        interaction.setInteractionCode(rs.getString("interaction_code"));
        interaction.setDrug1Name(rs.getString("drug_1_name"));
        interaction.setDrug1Ndc(rs.getString("drug_1_ndc"));
        interaction.setDrug2Name(rs.getString("drug_2_name"));
        interaction.setDrug2Ndc(rs.getString("drug_2_ndc"));
        interaction.setSeverityLevel(rs.getString("severity_level"));
        interaction.setInteractionMechanism(rs.getString("interaction_mechanism"));
        interaction.setClinicalEffects(rs.getString("clinical_effects"));
        interaction.setManagementRecommendation(rs.getString("management_recommendation"));
        interaction.setEvidenceLevel(rs.getString("evidence_level"));
        interaction.setOnsetTiming(rs.getString("onset_timing"));
        interaction.setDocumentationSource(rs.getString("documentation_source"));
        interaction.setRequiresAlert(rs.getBoolean("requires_alert"));
        interaction.setRequiresIntervention(rs.getBoolean("requires_intervention"));
        interaction.setPatientCounselingRequired(rs.getBoolean("patient_counseling_required"));
        interaction.setPrescriberNotificationRequired(rs.getBoolean("prescriber_notification_required"));
        
        Date lastReviewedDate = rs.getDate("last_reviewed_date");
        if (lastReviewedDate != null) {
            interaction.setLastReviewedDate(lastReviewedDate.toLocalDate());
        }
        
        Date lastUpdatedDate = rs.getDate("last_updated_date");
        if (lastUpdatedDate != null) {
            interaction.setLastUpdatedDate(lastUpdatedDate.toLocalDate());
        }
        
        interaction.setActiveStatus(rs.getString("active_status"));
        interaction.setReferenceId(rs.getString("reference_id"));
        interaction.setNotes(rs.getString("notes"));
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            interaction.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            interaction.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        
        return interaction;
    }
}


