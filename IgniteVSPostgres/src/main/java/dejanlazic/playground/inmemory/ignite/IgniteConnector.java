package dejanlazic.playground.inmemory.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;

import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Apache Ignite Database Connector
 * Manages Ignite cluster connection and lifecycle
 */
public class IgniteConnector {
    private static final Logger LOGGER = Logger.getLogger(IgniteConnector.class.getName());
    
    private Ignite ignite;
    private final String configPath;
    
    /**
     * Create connector with default XML configuration
     */
    public IgniteConnector() {
        this.configPath = "ignite/config/ignite-config.xml";
    }
    
    /**
     * Create connector with custom XML configuration path
     * @param configPath Path to Ignite XML configuration file
     */
    public IgniteConnector(String configPath) {
        this.configPath = configPath;
    }
    
    /**
     * Create connector with programmatic configuration
     */
    public IgniteConnector(boolean useProgrammaticConfig) {
        this.configPath = null;
        if (useProgrammaticConfig) {
            initializeProgrammatically();
        }
    }
    
    /**
     * Initialize Ignite with XML configuration
     */
    public void initialize() {
        if (ignite != null) {
            LOGGER.log(Level.WARNING, "Ignite already initialized");
            return;
        }
        
        try {
            if (configPath != null) {
                LOGGER.log(Level.INFO, "Starting Ignite with configuration: {0}", configPath);
                ignite = Ignition.start(configPath);
            } else {
                LOGGER.log(Level.INFO, "Starting Ignite with default configuration");
                ignite = Ignition.start();
            }
            
            LOGGER.log(Level.INFO, "Ignite cluster started successfully");
            LOGGER.log(Level.INFO, "Cluster name: {0}", ignite.name());
            LOGGER.log(Level.INFO, "Cluster nodes: {0}", ignite.cluster().nodes().size());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to start Ignite cluster", e);
            throw new RuntimeException("Failed to initialize Ignite", e);
        }
    }
    
    /**
     * Initialize Ignite with programmatic configuration
     */
    private void initializeProgrammatically() {
        if (ignite != null) {
            LOGGER.log(Level.WARNING, "Ignite already initialized");
            return;
        }
        
        try {
            IgniteConfiguration cfg = new IgniteConfiguration();
            cfg.setIgniteInstanceName("pbm-ignite-cluster");
            cfg.setPeerClassLoadingEnabled(true);
            
            // Configure discovery
            TcpDiscoverySpi discoverySpi = new TcpDiscoverySpi();
            TcpDiscoveryVmIpFinder ipFinder = new TcpDiscoveryVmIpFinder();
            ipFinder.setAddresses(Collections.singletonList("127.0.0.1:47500..47509"));
            discoverySpi.setIpFinder(ipFinder);
            cfg.setDiscoverySpi(discoverySpi);
            
            LOGGER.log(Level.INFO, "Starting Ignite with programmatic configuration");
            ignite = Ignition.start(cfg);
            
            LOGGER.log(Level.INFO, "Ignite cluster started successfully");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to start Ignite cluster", e);
            throw new RuntimeException("Failed to initialize Ignite", e);
        }
    }
    
    /**
     * Get the Ignite instance
     * @return Ignite instance
     */
    public Ignite getIgnite() {
        if (ignite == null) {
            initialize();
        }
        return ignite;
    }
    
    /**
     * Test Ignite connectivity
     * @return true if connected, false otherwise
     */
    public boolean testConnection() {
        try {
            if (ignite == null) {
                initialize();
            }
            return ignite != null && ignite.cluster().nodes().size() > 0;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Connection test failed", e);
            return false;
        }
    }
    
    /**
     * Get cluster information
     * @return Cluster information string
     */
    public String getClusterInfo() {
        if (ignite == null) {
            return "Ignite not initialized";
        }
        
        return String.format("Ignite Cluster: %s, Nodes: %d, Active: %s",
                ignite.name(),
                ignite.cluster().nodes().size(),
                ignite.cluster().active());
    }
    
    /**
     * Activate cluster (required for persistence)
     */
    public void activateCluster() {
        if (ignite != null && !ignite.cluster().active()) {
            LOGGER.log(Level.INFO, "Activating Ignite cluster");
            ignite.cluster().active(true);
        }
    }
    
    /**
     * Close Ignite connection and cleanup resources
     */
    public void close() {
        if (ignite != null) {
            LOGGER.log(Level.INFO, "Closing Ignite cluster");
            ignite.close();
            ignite = null;
        }
    }
    
    /**
     * Check if Ignite is initialized
     * @return true if initialized, false otherwise
     */
    public boolean isInitialized() {
        return ignite != null;
    }
}

// Made with Bob
