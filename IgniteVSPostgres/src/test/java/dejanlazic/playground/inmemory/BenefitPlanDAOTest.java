package dejanlazic.playground.inmemory;

import dejanlazic.playground.inmemory.rdbms.DatabaseConnector;
import dejanlazic.playground.inmemory.rdbms.dao.BenefitPlanDAO;
import dejanlazic.playground.inmemory.rdbms.model.BenefitPlan;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit 5 test class for BenefitPlanDAO
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BenefitPlanDAOTest {
    
    private static DatabaseConnector connector;
    private static BenefitPlanDAO dao;
    
    @BeforeAll
    static void setUp() {
        connector = new DatabaseConnector();
        dao = new BenefitPlanDAO(connector);
        System.out.println("=".repeat(60));
        System.out.println("BenefitPlanDAO Test Suite");
        System.out.println("=".repeat(60));
    }
    
    @AfterAll
    static void tearDown() {
        System.out.println("=".repeat(60));
        System.out.println("All DAO tests completed!");
        System.out.println("=".repeat(60));
    }
    
    @Test
    @Order(1)
    @DisplayName("Test 1: Count existing plans")
    void testCount() {
        System.out.println("\nTest 1: Counting existing plans...");
        assertDoesNotThrow(() -> {
            long count = dao.count();
            System.out.println("✓ Found " + count + " plans in database");
            assertTrue(count >= 0, "Count should be non-negative");
        });
    }
    
    @Test
    @Order(2)
    @DisplayName("Test 2: Find all plans")
    void testFindAll() {
        System.out.println("\nTest 2: Finding all plans...");
        assertDoesNotThrow(() -> {
            List<BenefitPlan> plans = dao.findAll();
            assertNotNull(plans, "Plans list should not be null");
            System.out.println("✓ Retrieved " + plans.size() + " plans");
            
            if (!plans.isEmpty()) {
                BenefitPlan firstPlan = plans.get(0);
                System.out.println("  Sample plan: " + firstPlan.getPlanCode() + " - " + firstPlan.getPlanName());
            }
        });
    }
    
    @Test
    @Order(3)
    @DisplayName("Test 3: Find plan by code")
    void testFindByPlanCode() {
        System.out.println("\nTest 3: Finding plan by code...");
        assertDoesNotThrow(() -> {
            // Try to find an existing plan
            Optional<BenefitPlan> plan = dao.findByPlanCode("COMM_PLAT_001");
            
            if (plan.isPresent()) {
                System.out.println("✓ Found plan: " + plan.get().getPlanName());
                assertEquals("COMM_PLAT_001", plan.get().getPlanCode());
            } else {
                System.out.println("⚠️  Plan COMM_PLAT_001 not found (may not exist yet)");
            }
        });
    }
    
    @Test
    @Order(4)
    @DisplayName("Test 4: Insert a new plan")
    void testInsert() {
        System.out.println("\nTest 4: Inserting a new plan...");
        assertDoesNotThrow(() -> {
            BenefitPlan newPlan = createTestPlan("TEST_PLAN_001", "Test Plan for DAO");
            
            BenefitPlan inserted = dao.insert(newPlan);
            assertNotNull(inserted, "Inserted plan should not be null");
            System.out.println("✓ Successfully inserted plan: " + inserted.getPlanCode());
            
            // Verify it was inserted
            Optional<BenefitPlan> found = dao.findByPlanCode("TEST_PLAN_001");
            assertTrue(found.isPresent(), "Inserted plan should be findable");
        });
    }
    
    @Test
    @Order(5)
    @DisplayName("Test 5: Update an existing plan")
    void testUpdate() {
        System.out.println("\nTest 5: Updating a plan...");
        assertDoesNotThrow(() -> {
            Optional<BenefitPlan> planOpt = dao.findByPlanCode("TEST_PLAN_001");
            
            if (planOpt.isPresent()) {
                BenefitPlan plan = planOpt.get();
                String originalName = plan.getPlanName();
                plan.setPlanName("Updated Test Plan");
                
                boolean updated = dao.update(plan);
                assertTrue(updated, "Update should be successful");
                System.out.println("✓ Updated plan from '" + originalName + "' to '" + plan.getPlanName() + "'");
                
                // Verify the update
                Optional<BenefitPlan> updatedPlan = dao.findByPlanCode("TEST_PLAN_001");
                assertTrue(updatedPlan.isPresent());
                assertEquals("Updated Test Plan", updatedPlan.get().getPlanName());
            } else {
                System.out.println("⚠️  Test plan not found for update test");
            }
        });
    }
    
    @Test
    @Order(6)
    @DisplayName("Test 6: Insert batch of plans")
    void testInsertBatch() {
        System.out.println("\nTest 6: Inserting batch of plans...");
        assertDoesNotThrow(() -> {
            List<BenefitPlan> batchPlans = List.of(
                createTestPlan("TEST_BATCH_001", "Batch Test Plan 1"),
                createTestPlan("TEST_BATCH_002", "Batch Test Plan 2"),
                createTestPlan("TEST_BATCH_003", "Batch Test Plan 3")
            );
            
            int inserted = dao.insertBatch(batchPlans);
            assertEquals(3, inserted, "Should insert 3 plans");
            System.out.println("✓ Successfully inserted " + inserted + " plans in batch");
        });
    }
    
    @Test
    @Order(7)
    @DisplayName("Test 7: Verify final count")
    void testFinalCount() {
        System.out.println("\nTest 7: Verifying final count...");
        assertDoesNotThrow(() -> {
            long count = dao.count();
            System.out.println("✓ Total plans in database: " + count);
            assertTrue(count >= 4, "Should have at least 4 test plans inserted");
        });
    }
    
    /**
     * Helper method to create a test plan
     */
    private BenefitPlan createTestPlan(String code, String name) {
        BenefitPlan plan = new BenefitPlan();
        plan.setPlanCode(code);
        plan.setPlanName(name);
        plan.setPlanType("COMMERCIAL");
        plan.setPlanCategory("TEST");
        plan.setEffectiveDate(LocalDate.now());
        plan.setAnnualDeductible(new BigDecimal("1000.00"));
        plan.setOutOfPocketMax(new BigDecimal("5000.00"));
        plan.setTier1Copay(new BigDecimal("10.00"));
        plan.setTier2Copay(new BigDecimal("25.00"));
        plan.setTier3Copay(new BigDecimal("50.00"));
        plan.setTier4Copay(new BigDecimal("100.00"));
        plan.setTier5Copay(new BigDecimal("200.00"));
        plan.setTier1Coinsurance(BigDecimal.ZERO);
        plan.setTier2Coinsurance(BigDecimal.ZERO);
        plan.setTier3Coinsurance(BigDecimal.ZERO);
        plan.setTier4Coinsurance(BigDecimal.ZERO);
        plan.setTier5Coinsurance(BigDecimal.ZERO);
        plan.setMailOrderAvailable(true);
        plan.setSpecialtyPharmacyRequired(false);
        plan.setDescription("Test plan created by DAO test");
        return plan;
    }
}

// Made with Bob
