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
import java.util.logging.Level;
import java.util.logging.Logger;

import dejanlazic.playground.inmemory.rdbms.DatabaseConnector;
import dejanlazic.playground.inmemory.rdbms.PerformanceMetrics;
import dejanlazic.playground.inmemory.rdbms.model.Enrollment;

/**
 * Data Access Object for Enrollment entity
 * Provides CRUD operations for enrollments
 */
public class EnrollmentDAO implements BaseDAO<Enrollment, Long> {
    
    private static final Logger LOGGER = Logger.getLogger(EnrollmentDAO.class.getName());
    
    private final DatabaseConnector connector;
    private static final int BATCH_SIZE = 1000;
    
    // SQL Statements
    private static final String INSERT_SQL = """
        INSERT INTO enrollment (
            member_id, plan_id, group_number, effective_date, termination_date, relationship, is_active
        ) 
        SELECT 
            m.member_id, 
            p.plan_id, 
            ?, ?, ?, ?, ?
        FROM member m
        CROSS JOIN plan p
        WHERE m.member_number = ? AND p.plan_code = ?
        RETURNING enrollment_id
        """;
    
    private static final String FIND_BY_ID_SQL = """
        SELECT e.enrollment_id, m.member_number, p.plan_code, e.group_number,
               e.effective_date, e.termination_date, e.relationship, e.is_active
        FROM enrollment e
        JOIN member m ON e.member_id = m.member_id
        JOIN plan p ON e.plan_id = p.plan_id
        WHERE e.enrollment_id = ?
        """;
    
    private static final String FIND_ALL_SQL = """
        SELECT e.enrollment_id, m.member_number, p.plan_code, e.group_number,
               e.effective_date, e.termination_date, e.relationship, e.is_active
        FROM enrollment e
        JOIN member m ON e.member_id = m.member_id
        JOIN plan p ON e.plan_id = p.plan_id
        ORDER BY e.effective_date DESC
        LIMIT 1000
        """;
    
    private static final String FIND_BY_MEMBER_SQL = """
        SELECT e.enrollment_id, m.member_number, p.plan_code, e.group_number,
               e.effective_date, e.termination_date, e.relationship, e.is_active
        FROM enrollment e
        JOIN member m ON e.member_id = m.member_id
        JOIN plan p ON e.plan_id = p.plan_id
        WHERE m.member_number = ?
        ORDER BY e.effective_date DESC
        """;
    
    private static final String UPDATE_SQL = """
        UPDATE enrollment SET
            group_number = ?, termination_date = ?, relationship = ?, is_active = ?
        WHERE enrollment_id = ?
        """;
    
    private static final String DELETE_SQL = "DELETE FROM enrollment WHERE enrollment_id = ?";
    
    private static final String COUNT_SQL = "SELECT COUNT(*) FROM enrollment";
    
    private static final String EXISTS_SQL = "SELECT EXISTS(SELECT 1 FROM enrollment WHERE enrollment_id = ?)";
    
    private static final String COUNT_ACTIVE_SQL = "SELECT COUNT(*) FROM enrollment WHERE is_active = true";
    
    public EnrollmentDAO(DatabaseConnector connector) {
        this.connector = connector;
    }
    
    @Override
    public Enrollment insert(Enrollment enrollment) throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics("Enrollment", "INSERT");
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_SQL)) {
            
            setEnrollmentParameters(ps, enrollment);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    enrollment.setEnrollmentId(rs.getLong("enrollment_id"));
                    metrics.setRecordCount(1);
                    metrics.setRecordSizeBytes(estimateEnrollmentSize(enrollment));
                    LOGGER.log(Level.INFO, "Inserted enrollment for member: {0}", enrollment.getMemberNumber());
                }
            }
            return enrollment;
        } finally {
            metrics.complete();
        }
    }
    
    @Override
    public int insertBatch(List<Enrollment> enrollments) throws SQLException {
        if (enrollments == null || enrollments.isEmpty()) {
            LOGGER.log(Level.WARNING, "No enrollments to insert");
            return 0;
        }
        
        PerformanceMetrics metrics = new PerformanceMetrics("Enrollment", "INSERT_BATCH");
        int insertedCount = 0;
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_SQL)) {
            
            conn.setAutoCommit(false);
            
            try {
                long totalSize = 0;
                for (int i = 0; i < enrollments.size(); i++) {
                    setEnrollmentParameters(ps, enrollments.get(i));
                    ps.addBatch();
                    totalSize += estimateEnrollmentSize(enrollments.get(i));
                    
                    if ((i + 1) % BATCH_SIZE == 0 || i == enrollments.size() - 1) {
                        int[] results = ps.executeBatch();
                        insertedCount += results.length;
                        ps.clearBatch();
                        
                        if ((i + 1) % 10000 == 0) {
                            LOGGER.log(Level.INFO, "Inserted {0} enrollments so far...", insertedCount);
                        }
                    }
                }
                
                conn.commit();
                metrics.setRecordCount(insertedCount);
                metrics.setRecordSizeBytes(totalSize);
                LOGGER.log(Level.INFO, "Successfully inserted {0} enrollments", insertedCount);
                
            } catch (SQLException e) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Failed to insert enrollments batch", e);
                throw new SQLException(
                    String.format("Failed to insert enrollments. Successfully inserted: %d of %d",
                        insertedCount, enrollments.size()), e);
            } finally {
                conn.setAutoCommit(true);
                metrics.complete();
            }
        }
        
        return insertedCount;
    }
    
    @Override
    public Optional<Enrollment> findById(Long id) throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics("Enrollment", "SELECT_BY_ID");
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_BY_ID_SQL)) {
            
            ps.setObject(1, id);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Enrollment enrollment = mapResultSetToEnrollment(rs);
                    metrics.setRecordCount(1);
                    metrics.setRecordSizeBytes(estimateEnrollmentSize(enrollment));
                    return Optional.of(enrollment);
                }
            }
            
            return Optional.empty();
        } finally {
            metrics.complete();
        }
    }
    
    /**
     * Find all enrollments for a specific member
     */
    public List<Enrollment> findByMemberNumber(String memberNumber) throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics("Enrollment", "SELECT_BY_MEMBER");
        List<Enrollment> enrollments = new ArrayList<>();
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_BY_MEMBER_SQL)) {
            
            ps.setString(1, memberNumber);
            
            try (ResultSet rs = ps.executeQuery()) {
                long totalSize = 0;
                while (rs.next()) {
                    Enrollment enrollment = mapResultSetToEnrollment(rs);
                    enrollments.add(enrollment);
                    totalSize += estimateEnrollmentSize(enrollment);
                }
                
                metrics.setRecordCount(enrollments.size());
                metrics.setRecordSizeBytes(totalSize);
            }
            
            return enrollments;
        } finally {
            metrics.complete();
        }
    }
    
    @Override
    public List<Enrollment> findAll() throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics("Enrollment", "SELECT_ALL");
        List<Enrollment> enrollments = new ArrayList<>();
        
        try (Connection conn = connector.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(FIND_ALL_SQL)) {
            
            long totalSize = 0;
            while (rs.next()) {
                Enrollment enrollment = mapResultSetToEnrollment(rs);
                enrollments.add(enrollment);
                totalSize += estimateEnrollmentSize(enrollment);
            }
            
            metrics.setRecordCount(enrollments.size());
            metrics.setRecordSizeBytes(totalSize);
            LOGGER.log(Level.INFO, "Retrieved {0} enrollments (limited to 1000)", enrollments.size());
            return enrollments;
        } finally {
            metrics.complete();
        }
    }
    
    @Override
    public boolean update(Enrollment enrollment) throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics("Enrollment", "UPDATE");
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE_SQL)) {
            
            ps.setString(1, enrollment.getGroupNumber());
            ps.setDate(2, enrollment.getTerminationDate() != null ? 
                Date.valueOf(enrollment.getTerminationDate()) : null);
            ps.setString(3, enrollment.getRelationship());
            ps.setBoolean(4, enrollment.isActive());
            ps.setObject(5, enrollment.getEnrollmentId());
            
            int rowsAffected = ps.executeUpdate();
            boolean updated = rowsAffected > 0;
            
            if (updated) {
                metrics.setRecordCount(1);
                metrics.setRecordSizeBytes(estimateEnrollmentSize(enrollment));
                LOGGER.log(Level.INFO, "Updated enrollment: {0}", enrollment.getEnrollmentId());
            }
            
            return updated;
        } finally {
            metrics.complete();
        }
    }
    
    @Override
    public boolean delete(Long id) throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics("Enrollment", "DELETE");
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(DELETE_SQL)) {
            
            ps.setObject(1, id);
            int rowsAffected = ps.executeUpdate();
            boolean deleted = rowsAffected > 0;
            
            if (deleted) {
                metrics.setRecordCount(1);
                LOGGER.log(Level.INFO, "Deleted enrollment with ID: {0}", id);
            }
            
            return deleted;
        } finally {
            metrics.complete();
        }
    }
    
    @Override
    public long count() throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics("Enrollment", "COUNT");
        
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
    
    /**
     * Count active enrollments
     */
    public long countActive() throws SQLException {
        try (Connection conn = connector.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(COUNT_ACTIVE_SQL)) {
            
            if (rs.next()) {
                return rs.getLong(1);
            }
            
            return 0;
        }
    }
    
    @Override
    public boolean exists(Long id) throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics("Enrollment", "EXISTS");
        
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
     * Set PreparedStatement parameters from an Enrollment object
     */
    private void setEnrollmentParameters(PreparedStatement ps, Enrollment enrollment) throws SQLException {
        int idx = 1;
        ps.setString(idx++, enrollment.getGroupNumber());
        ps.setDate(idx++, enrollment.getEffectiveDate() != null ? 
            Date.valueOf(enrollment.getEffectiveDate()) : null);
        ps.setDate(idx++, enrollment.getTerminationDate() != null ? 
            Date.valueOf(enrollment.getTerminationDate()) : null);
        ps.setString(idx++, enrollment.getRelationship());
        ps.setBoolean(idx++, enrollment.isActive());
        ps.setString(idx++, enrollment.getMemberNumber());
        ps.setString(idx++, enrollment.getPlanCode());
    }
    
    /**
     * Map ResultSet row to Enrollment object
     */
    private Enrollment mapResultSetToEnrollment(ResultSet rs) throws SQLException {
        Enrollment enrollment = new Enrollment();
        
        enrollment.setEnrollmentId(rs.getLong("enrollment_id"));
        enrollment.setMemberNumber(rs.getString("member_number"));
        enrollment.setPlanCode(rs.getString("plan_code"));
        enrollment.setGroupNumber(rs.getString("group_number"));
        
        Date effectiveDate = rs.getDate("effective_date");
        if (effectiveDate != null) {
            enrollment.setEffectiveDate(effectiveDate.toLocalDate());
        }
        
        Date terminationDate = rs.getDate("termination_date");
        if (terminationDate != null) {
            enrollment.setTerminationDate(terminationDate.toLocalDate());
        }
        
        enrollment.setRelationship(rs.getString("relationship"));
        enrollment.setActive(rs.getBoolean("is_active"));
        
        return enrollment;
    }
    
    /**
     * Estimate the size of an Enrollment record in bytes
     */
    private long estimateEnrollmentSize(Enrollment enrollment) {
        long size = 0;
        
        // UUID (16 bytes)
        size += 16;
        
        // Strings (2 bytes per character for UTF-16)
        if (enrollment.getMemberNumber() != null) size += enrollment.getMemberNumber().length() * 2L;
        if (enrollment.getPlanCode() != null) size += enrollment.getPlanCode().length() * 2L;
        if (enrollment.getGroupNumber() != null) size += enrollment.getGroupNumber().length() * 2L;
        if (enrollment.getRelationship() != null) size += enrollment.getRelationship().length() * 2L;
        
        // Dates (8 bytes each)
        if (enrollment.getEffectiveDate() != null) size += 8;
        if (enrollment.getTerminationDate() != null) size += 8;
        
        // Boolean (1 byte)
        size += 1;
        
        return size;
    }
}


