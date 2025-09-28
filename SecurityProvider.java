package SchoolManager;

import java.util.Objects;

/**
 * Central security component provider using hybrid singleton pattern
 * with dependency injection capabilities for testing
 */
public class SecurityProvider {
    private static SecurityProvider instance;
    private final SecurityComponents components;
    
    // Private constructor for singleton pattern
    private SecurityProvider() {
        this.components = initializeComponents();
    }
    
    /**
     * Get the singleton instance (thread-safe)
     */
    public static synchronized SecurityProvider getInstance() {
        if (instance == null) {
            instance = new SecurityProvider();
        }
        return instance;
    }
    
    /**
     * For testing - allow dependency injection of mock provider
     */
    public static synchronized void setInstance(SecurityProvider provider) {
        instance = provider;
    }
    
    /**
     * Reset to default instance (for cleanup)
     */
    public static synchronized void resetInstance() {
        instance = null;
    }
    
    private SecurityComponents initializeComponents() {
        try {
            // Initialize crypter first (foundation for other components)
            Crypter crypter = new CrypterSymmetricMech();
            
            // Logger depends on crypter for secure logging
            Logger logger = new LoggerMech(crypter, "SAMSecurity");
            
            // AuthGenerator depends on both crypter and logger
            AuthGenerator authGenerator = new AuthGenerator(logger, crypter);
            
            logger.info("Security components initialized successfully");
            
            return new SecurityComponents(crypter, logger, authGenerator);
            
        } catch (Exception e) {
            // Fallback to basic components if specialized initialization fails
            System.err.println("Security initialization failed: " + e.getMessage());
            return createFallbackComponents();
        }
    }
    
    private SecurityComponents createFallbackComponents() {
        // Basic components without complex dependencies
        Crypter crypter = new CrypterSymmetricMech();
        Logger logger = new LoggerMech(crypter, "SAMSecurity-Fallback");
        AuthGenerator authGenerator = new AuthGenerator(logger, crypter);
        
        logger.warn("Using fallback security components");
        return new SecurityComponents(crypter, logger, authGenerator);
    }
    
    public SecurityComponents getComponents() {
        return components;
    }
    
    // Convenience direct getters
    public Crypter getCrypter() {
        return components.crypter;
    }
    
    public Logger getLogger() {
        return components.logger;
    }
    
    public AuthGenerator getAuthGenerator() {
        return components.authGenerator;
    }
    
    /**
     * Immutable container holding all security components
     */
    public static class SecurityComponents {
        public final Crypter crypter;
        public final Logger logger;
        public final AuthGenerator authGenerator;
        
        public SecurityComponents(Crypter crypter, Logger logger, AuthGenerator authGenerator) {
            this.crypter = Objects.requireNonNull(crypter, "Crypter cannot be null");
            this.logger = Objects.requireNonNull(logger, "Logger cannot be null");
            this.authGenerator = Objects.requireNonNull(authGenerator, "AuthGenerator cannot be null");
        }
        
        /**
         * Secure cleanup of all components
         */
        public void secureShutdown() {
            try {
                if (crypter instanceof CrypterSymmetricMech) {
                    ((CrypterSymmetricMech) crypter).secureWipe();
                }
                logger.info("Security components shutdown completed");
                logger.close();
            } catch (Exception e) {
                System.err.println("Error during security shutdown: " + e.getMessage());
            }
        }
    }
    
    /**
     * Proper shutdown procedure
     */
    public void shutdown() {
        if (components != null) {
            components.secureShutdown();
        }
        resetInstance();
    }
}