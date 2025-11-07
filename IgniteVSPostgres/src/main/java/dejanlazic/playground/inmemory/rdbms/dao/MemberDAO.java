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
import dejanlazic.playground.inmemory.rdbms.model.Member;
import dejanlazic.playground.inmemory.rdbms.model.Member.Gender;

/**
 * Data Access Object for Member entity
 * Provides CRUD operations for members with performance metrics
 */
public class MemberDAO implements BaseDAO<Member, UUID> {
    
    private static final Logger LOGGER = Logger.getLogger(MemberDAO.class.getName());
    
    private final DatabaseConnector connector;
    private static final int BATCH_SIZE = 1000;
    
    // SQL Statements
    private static final String INSERT_SQL = """
        INSERT INTO member (
            member_number, first_name, last_name, date_of_birth, gender,
            address, city, state, zip_code, phone, email
        ) VALUES (?, ?, ?, ?, ?::gender_type, ?, ?, ?, ?, ?, ?)
        RETURNING member_id
        """;
    
    private static final String FIND_BY_ID_SQL = """
        SELECT member_id, member_number, first_name, last_name, date_of_birth, gender,
               address, city, state, zip_code, phone, email, created_at, updated_at
        FROM member WHERE member_id = ?
        """;
    
    private static final String FIND_ALL_SQL = """
        SELECT member_id, member_number, first_name, last_name, date_of_birth, gender,
               address, city, state, zip_code, phone, email, created_at, updated_at
        FROM member ORDER BY member_number
        """;
    
    private static final String UPDATE_SQL = """
        UPDATE member SET
            first_name = ?, last_name = ?, date_of_birth = ?, gender = ?::gender_type,
            address = ?, city = ?, state = ?, zip_code = ?, phone = ?, email = ?
        WHERE member_number = ?
        """;
    
    private static final String DELETE_SQL = "DELETE FROM member WHERE member_id = ?";
    
    private static final String COUNT_SQL = "SELECT COUNT(*) FROM member";
    
    private static final String EXISTS_SQL = "SELECT EXISTS(SELECT 1 FROM member WHERE member_id = ?)";
    
    private static final String FIND_BY_MEMBER_NUMBER_SQL = """
        SELECT member_id, member_number, first_name, last_name, date_of_birth, gender,
               address, city, state, zip_code, phone, email, created_at, updated_at
        FROM member WHERE member_number = ?
        """;
    
    public MemberDAO(DatabaseConnector connector) {
        this.connector = connector;
    }
    
    @Override
    public Member insert(Member member) throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics("Member", "INSERT");
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_SQL)) {
            
            setMemberParameters(ps, member);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    member.setMemberId((UUID) rs.getObject(1));
                    metrics.setRecordCount(1);
                    metrics.setRecordSizeBytes(estimateMemberSize(member));
                    LOGGER.log(Level.INFO, "Inserted member: {0}", member.getMemberNumber());
                }
            }
            return member;
        } finally {
            metrics.complete();
        }
    }
    
    @Override
    public int insertBatch(List<Member> members) throws SQLException {
        if (members == null || members.isEmpty()) {
            LOGGER.log(Level.WARNING, "No members to insert");
            return 0;
        }
        
        PerformanceMetrics metrics = new PerformanceMetrics("Member", "INSERT_BATCH");
        int insertedCount = 0;
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_SQL)) {
            
            conn.setAutoCommit(false);
            
            try {
                long totalSize = 0;
                for (int i = 0; i < members.size(); i++) {
                    setMemberParameters(ps, members.get(i));
                    ps.addBatch();
                    totalSize += estimateMemberSize(members.get(i));
                    
                    if ((i + 1) % BATCH_SIZE == 0 || i == members.size() - 1) {
                        int[] results = ps.executeBatch();
                        insertedCount += results.length;
                        ps.clearBatch();
                        
                        // Log progress for large batches
                        if ((i + 1) % 10000 == 0) {
                            LOGGER.log(Level.INFO, "Inserted {0} of {1} members", 
                                new Object[]{i + 1, members.size()});
                        }
                    }
                }
                
                conn.commit();
                metrics.setRecordCount(insertedCount);
                metrics.setRecordSizeBytes(totalSize);
                LOGGER.log(Level.INFO, "Successfully inserted {0} members", insertedCount);
                
            } catch (SQLException e) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Failed to insert members batch", e);
                throw new SQLException(
                    String.format("Failed to insert members. Successfully inserted: %d of %d",
                        insertedCount, members.size()), e);
            } finally {
                conn.setAutoCommit(true);
                metrics.complete();
            }
        }
        
        return insertedCount;
    }
    
    @Override
    public Optional<Member> findById(UUID id) throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics("Member", "SELECT_BY_ID");
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_BY_ID_SQL)) {
            
            ps.setObject(1, id);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Member member = mapResultSetToMember(rs);
                    metrics.setRecordCount(1);
                    metrics.setRecordSizeBytes(estimateMemberSize(member));
                    return Optional.of(member);
                }
            }
            
            return Optional.empty();
        } finally {
            metrics.complete();
        }
    }
    
    /**
     * Find a member by member number
     * @param memberNumber Member number to search for
     * @return Optional containing the member if found
     * @throws SQLException if database error occurs
     */
    public Optional<Member> findByMemberNumber(String memberNumber) throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics("Member", "SELECT_BY_MEMBER_NUMBER");
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_BY_MEMBER_NUMBER_SQL)) {
            
            ps.setString(1, memberNumber);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Member member = mapResultSetToMember(rs);
                    metrics.setRecordCount(1);
                    metrics.setRecordSizeBytes(estimateMemberSize(member));
                    return Optional.of(member);
                }
            }
            
            return Optional.empty();
        } finally {
            metrics.complete();
        }
    }
    
    @Override
    public List<Member> findAll() throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics("Member", "SELECT_ALL");
        List<Member> members = new ArrayList<>();
        
        try (Connection conn = connector.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(FIND_ALL_SQL)) {
            
            long totalSize = 0;
            while (rs.next()) {
                Member member = mapResultSetToMember(rs);
                members.add(member);
                totalSize += estimateMemberSize(member);
            }
            
            metrics.setRecordCount(members.size());
            metrics.setRecordSizeBytes(totalSize);
            LOGGER.log(Level.INFO, "Retrieved {0} members", members.size());
            return members;
        } finally {
            metrics.complete();
        }
    }
    
    @Override
    public boolean update(Member member) throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics("Member", "UPDATE");
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE_SQL)) {
            
            int idx = 1;
            ps.setString(idx++, member.getFirstName());
            ps.setString(idx++, member.getLastName());
            ps.setDate(idx++, member.getDateOfBirth() != null ? Date.valueOf(member.getDateOfBirth()) : null);
            ps.setString(idx++, member.getGender() != null ? member.getGender().name() : null);
            ps.setString(idx++, member.getAddress());
            ps.setString(idx++, member.getCity());
            ps.setString(idx++, member.getState());
            ps.setString(idx++, member.getZipCode());
            ps.setString(idx++, member.getPhone());
            ps.setString(idx++, member.getEmail());
            ps.setString(idx++, member.getMemberNumber());
            
            int rowsAffected = ps.executeUpdate();
            boolean updated = rowsAffected > 0;
            
            if (updated) {
                metrics.setRecordCount(1);
                metrics.setRecordSizeBytes(estimateMemberSize(member));
                LOGGER.log(Level.INFO, "Updated member: {0}", member.getMemberNumber());
            }
            
            return updated;
        } finally {
            metrics.complete();
        }
    }
    
    @Override
    public boolean delete(UUID id) throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics("Member", "DELETE");
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(DELETE_SQL)) {
            
            ps.setObject(1, id);
            int rowsAffected = ps.executeUpdate();
            boolean deleted = rowsAffected > 0;
            
            if (deleted) {
                metrics.setRecordCount(1);
                LOGGER.log(Level.INFO, "Deleted member with ID: {0}", id);
            }
            
            return deleted;
        } finally {
            metrics.complete();
        }
    }
    
    @Override
    public long count() throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics("Member", "COUNT");
        
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
        PerformanceMetrics metrics = new PerformanceMetrics("Member", "EXISTS");
        
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
     * Set PreparedStatement parameters from a Member object
     */
    private void setMemberParameters(PreparedStatement ps, Member member) throws SQLException {
        int idx = 1;
        ps.setString(idx++, member.getMemberNumber());
        ps.setString(idx++, member.getFirstName());
        ps.setString(idx++, member.getLastName());
        ps.setDate(idx++, member.getDateOfBirth() != null ? Date.valueOf(member.getDateOfBirth()) : null);
        ps.setString(idx++, member.getGender() != null ? member.getGender().name() : null);
        ps.setString(idx++, member.getAddress());
        ps.setString(idx++, member.getCity());
        ps.setString(idx++, member.getState());
        ps.setString(idx++, member.getZipCode());
        ps.setString(idx++, member.getPhone());
        ps.setString(idx++, member.getEmail());
    }
    
    /**
     * Estimates the size of a Member record in bytes.
     * This is an approximation based on the data types and string lengths.
     * 
     * @param member The Member to estimate
     * @return Estimated size in bytes
     */
    private long estimateMemberSize(Member member) {
        long size = 0;
        
        // UUID (16 bytes)
        size += 16;
        
        // Strings (2 bytes per character for UTF-16)
        if (member.getMemberNumber() != null) size += member.getMemberNumber().length() * 2L;
        if (member.getFirstName() != null) size += member.getFirstName().length() * 2L;
        if (member.getLastName() != null) size += member.getLastName().length() * 2L;
        if (member.getAddress() != null) size += member.getAddress().length() * 2L;
        if (member.getCity() != null) size += member.getCity().length() * 2L;
        if (member.getState() != null) size += member.getState().length() * 2L;
        if (member.getZipCode() != null) size += member.getZipCode().length() * 2L;
        if (member.getPhone() != null) size += member.getPhone().length() * 2L;
        if (member.getEmail() != null) size += member.getEmail().length() * 2L;
        
        // Date (8 bytes)
        if (member.getDateOfBirth() != null) size += 8;
        
        // Gender enum (1 byte)
        size += 1;
        
        // Timestamps (8 bytes each)
        if (member.getCreatedAt() != null) size += 8;
        if (member.getUpdatedAt() != null) size += 8;
        
        return size;
    }
    
    /**
     * Map ResultSet row to Member object
     */
    private Member mapResultSetToMember(ResultSet rs) throws SQLException {
        Member member = new Member();
        
        member.setMemberId((UUID) rs.getObject("member_id"));
        member.setMemberNumber(rs.getString("member_number"));
        member.setFirstName(rs.getString("first_name"));
        member.setLastName(rs.getString("last_name"));
        
        Date dateOfBirth = rs.getDate("date_of_birth");
        if (dateOfBirth != null) {
            member.setDateOfBirth(dateOfBirth.toLocalDate());
        }
        
        String genderStr = rs.getString("gender");
        if (genderStr != null) {
            member.setGender(Gender.valueOf(genderStr));
        }
        
        member.setAddress(rs.getString("address"));
        member.setCity(rs.getString("city"));
        member.setState(rs.getString("state"));
        member.setZipCode(rs.getString("zip_code"));
        member.setPhone(rs.getString("phone"));
        member.setEmail(rs.getString("email"));
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            member.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            member.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        
        return member;
    }
}


