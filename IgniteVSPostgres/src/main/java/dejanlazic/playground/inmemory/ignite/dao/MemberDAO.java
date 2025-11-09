package dejanlazic.playground.inmemory.ignite.dao;

import dejanlazic.playground.inmemory.ignite.IgniteConnector;
import dejanlazic.playground.inmemory.ignite.model.Member;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.query.SqlFieldsQuery;
import org.apache.ignite.cache.query.SqlQuery;

import javax.cache.Cache;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Ignite Data Access Object for Member entity
 * Uses Ignite SQL queries for CRUD operations
 */
public class MemberDAO {
    
    private static final Logger LOGGER = Logger.getLogger(MemberDAO.class.getName());
    
    private final IgniteConnector connector;
    private final Ignite ignite;
    private static final String CACHE_NAME = "SQL_PUBLIC_MEMBER";
    
    public MemberDAO(IgniteConnector connector) {
        this.connector = connector;
        this.ignite = connector.getIgnite();
    }
    
    /**
     * Get or create the Member cache
     */
    private IgniteCache<Long, Member> getCache() {
        return ignite.cache(CACHE_NAME);
    }
    
    /**
     * Insert a single member
     */
    public Member insert(Member member) {
        try {
            IgniteCache<Long, Member> cache = getCache();
            
            if (member.getCreatedAt() == null) {
                member.setCreatedAt(new Timestamp(System.currentTimeMillis()));
            }
            member.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
            
            cache.put(member.getMemberId(), member);
            
            LOGGER.log(Level.INFO, "Inserted member: {0}", member.getMemberNumber());
            return member;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to insert member", e);
            throw new RuntimeException("Failed to insert member", e);
        }
    }
    
    /**
     * Insert multiple members in batch
     */
    public int insertBatch(List<Member> members) {
        if (members == null || members.isEmpty()) {
            LOGGER.log(Level.WARNING, "No members to insert");
            return 0;
        }
        
        try {
            IgniteCache<Long, Member> cache = getCache();
            Timestamp now = new Timestamp(System.currentTimeMillis());
            
            int count = 0;
            for (Member member : members) {
                if (member.getCreatedAt() == null) {
                    member.setCreatedAt(now);
                }
                member.setUpdatedAt(now);
                
                cache.put(member.getMemberId(), member);
                count++;
                
                if (count % 10000 == 0) {
                    LOGGER.log(Level.INFO, "Inserted {0} of {1} members", 
                        new Object[]{count, members.size()});
                }
            }
            
            LOGGER.log(Level.INFO, "Successfully inserted {0} members", count);
            return count;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to insert members batch", e);
            throw new RuntimeException("Failed to insert members batch", e);
        }
    }
    
    /**
     * Find member by ID
     */
    public Optional<Member> findById(Long id) {
        try {
            IgniteCache<Long, Member> cache = getCache();
            Member member = cache.get(id);
            return Optional.ofNullable(member);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to find member by ID", e);
            throw new RuntimeException("Failed to find member by ID", e);
        }
    }
    
    /**
     * Find member by member number using SQL query
     */
    public Optional<Member> findByMemberNumber(String memberNumber) {
        try {
            IgniteCache<Long, Member> cache = getCache();
            
            SqlQuery<Long, Member> query = new SqlQuery<>(Member.class, 
                "memberNumber = ?");
            query.setArgs(memberNumber);
            
            List<Cache.Entry<Long, Member>> results = cache.query(query).getAll();
            
            if (!results.isEmpty()) {
                return Optional.of(results.get(0).getValue());
            }
            return Optional.empty();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to find member by member number", e);
            throw new RuntimeException("Failed to find member by member number", e);
        }
    }
    
    /**
     * Find all members
     */
    public List<Member> findAll() {
        try {
            IgniteCache<Long, Member> cache = getCache();
            List<Member> members = new ArrayList<>();
            
            SqlQuery<Long, Member> query = new SqlQuery<>(Member.class, "1=1");
            
            for (Cache.Entry<Long, Member> entry : cache.query(query)) {
                members.add(entry.getValue());
            }
            
            LOGGER.log(Level.INFO, "Retrieved {0} members", members.size());
            return members;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to find all members", e);
            throw new RuntimeException("Failed to find all members", e);
        }
    }
    
    /**
     * Update a member
     */
    public boolean update(Member member) {
        try {
            IgniteCache<Long, Member> cache = getCache();
            
            if (!cache.containsKey(member.getMemberId())) {
                return false;
            }
            
            member.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
            cache.put(member.getMemberId(), member);
            
            LOGGER.log(Level.INFO, "Updated member: {0}", member.getMemberNumber());
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to update member", e);
            throw new RuntimeException("Failed to update member", e);
        }
    }
    
    /**
     * Delete a member by ID
     */
    public boolean delete(Long id) {
        try {
            IgniteCache<Long, Member> cache = getCache();
            boolean removed = cache.remove(id);
            
            if (removed) {
                LOGGER.log(Level.INFO, "Deleted member with ID: {0}", id);
            }
            return removed;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to delete member", e);
            throw new RuntimeException("Failed to delete member", e);
        }
    }
    
    /**
     * Count total members using SQL
     */
    public long count() {
        try {
            IgniteCache<Long, Member> cache = getCache();
            
            SqlFieldsQuery query = new SqlFieldsQuery("SELECT COUNT(*) FROM Member");
            List<List<?>> results = cache.query(query).getAll();
            
            if (!results.isEmpty() && !results.get(0).isEmpty()) {
                return ((Number) results.get(0).get(0)).longValue();
            }
            return 0;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to count members", e);
            throw new RuntimeException("Failed to count members", e);
        }
    }
    
    /**
     * Check if member exists
     */
    public boolean exists(Long id) {
        try {
            IgniteCache<Long, Member> cache = getCache();
            return cache.containsKey(id);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to check member existence", e);
            throw new RuntimeException("Failed to check member existence", e);
        }
    }
    
    /**
     * Find members by state using SQL query
     */
    public List<Member> findByState(String state) {
        try {
            IgniteCache<Long, Member> cache = getCache();
            List<Member> members = new ArrayList<>();
            
            SqlQuery<Long, Member> query = new SqlQuery<>(Member.class, "state = ?");
            query.setArgs(state);
            
            for (Cache.Entry<Long, Member> entry : cache.query(query)) {
                members.add(entry.getValue());
            }
            
            return members;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to find members by state", e);
            throw new RuntimeException("Failed to find members by state", e);
        }
    }
    
    /**
     * Clear all members (for testing)
     */
    public void clearAll() {
        try {
            IgniteCache<Long, Member> cache = getCache();
            cache.clear();
            LOGGER.log(Level.INFO, "Cleared all members from cache");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to clear members", e);
            throw new RuntimeException("Failed to clear members", e);
        }
    }
}

// Made with Bob
