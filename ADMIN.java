package SchoolManager;

import java.io.*;
import java.util.*;

import static java.lang.System.out;

/**
 * Streamlined ADMIN class using SecurityProvider for dependency management
 */
public class ADMIN {
    
    // Configuration files
    private final String securityConfigFile = "security_config";
    private final String designatedAuthsFile = "designated_auths.txt";
    
    // Security credentials
    private String systemPass = "";
    private String adminPass = "";
    private String adminAuth = "";
    
    // Security components from provider
    private final SecurityProvider.SecurityComponents security;
    private final SystemFileOps systemFileOps;
    
    // Security data
    private final List<String> designatedAuths;
    
    public ADMIN() {
        // Get security components from central provider
        this.security = SecurityProvider.getInstance().getComponents();
        
        out.println("\n\t [SECURITY PROVIDER INITIALIZED] \n");
        
        // Initialize remaining components
        this.systemFileOps = new SystemFileMech(security.crypter, security.logger);
        this.designatedAuths = new ArrayList<>();
        
        out.println("\n\t [ADMIN COMPONENTS INITIALIZED] \n");
        
        // Load security data
        loadSecurityData();
        
        security.logger.info("ADMIN system initialized successfully");
    }
    
    ////////////////////////////////////////////////////////////////
    // SECURITY CREDENTIAL MANAGEMENT
    ////////////////////////////////////////////////////////////////
    
    public void setSystemPass(String currentPass, String newPass) {
        if (currentPass.equals(this.adminPass)) {
            this.systemPass = newPass;
            storeSecurityConfig();
            security.logger.info("System password updated");
        } else {
            security.logger.warn("Unauthorized system password change attempt");
            throw new SecurityException("Invalid current password");
        }
    }
    
    public void setAdminPass(String currentSystemPass, String newAdminPass) {
        if (currentSystemPass.equals(systemPass)) {
            this.adminPass = newAdminPass;
            storeSecurityConfig();
            security.logger.info("Admin password updated");
        } else {
            security.logger.warn("Unauthorized admin password change attempt");
            throw new SecurityException("Invalid system password");
        }
    }
    
    ////////////////////////////////////////////////////////////////
    // COMPONENT ACCESSORS (Simplified)
    ////////////////////////////////////////////////////////////////
    
    public Crypter getSystemCrypter() {
        return security.crypter;
    }
    
    public Logger getSystemLogger() {
        return security.logger;
    }
    
    public AuthGenerator getSystemAuthGenerator() {
        return security.authGenerator;
    }
    
    public SystemFileOps getSystemFileMech() {
        return this.systemFileOps;
    }
    
    ////////////////////////////////////////////////////////////////
    // REPOSITORY MANAGER FACTORY METHODS
    ////////////////////////////////////////////////////////////////
    
    public StudentRepoManager getStudentRepoManager() {
        return new StudentRepoManager(security.crypter, security.logger);
    }
    
    public StaffRepoManager getStaffRepoManager() {
        return new StaffRepoManager(security.crypter, security.logger, security.authGenerator);
    }
    
    public UserRepoManager getUserRepoManager() {
        return new UserRepoManager(security.crypter, security.logger);
    }
    
    ////////////////////////////////////////////////////////////////
    // SECURITY VALIDATION
    ////////////////////////////////////////////////////////////////
    
    public boolean validateSystemAccess(String systemPass, String adminPass, String adminAuth) {
        boolean isValid = systemPass.equals(this.systemPass) 
                       && adminPass.equals(this.adminPass) 
                       && this.designatedAuths.contains(adminAuth);
        
        if (isValid) {
            security.logger.info("System access validated successfully");
        } else {
            security.logger.warn("Failed system access validation attempt");
        }
        
        return isValid;
    }
    
    public void addDesignatedAuth(String auth) {
        if (!designatedAuths.contains(auth)) {
            this.designatedAuths.add(auth);
            storeDesignatedAuths();
            security.logger.info("New designated auth added: " + auth);
        }
    }
    
    ////////////////////////////////////////////////////////////////
    // SECURITY DATA PERSISTENCE
    ////////////////////////////////////////////////////////////////
    
    private void loadSecurityData() {
        loadDesignatedAuths();
        loadSecurityConfig();
        security.logger.info("Security data loaded successfully");
    }
    
    private void loadDesignatedAuths() {
        File authFile = new File(designatedAuthsFile);
        if (!authFile.exists()) {
            security.logger.info("Designated auths file not found, starting fresh");
            return;
        }
        
        try (BufferedReader br = new BufferedReader(new FileReader(authFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    String decryptedAuth = security.crypter.decodeDecrypt(line.trim());
                    designatedAuths.add(decryptedAuth);
                }
            }
            security.logger.info("Loaded " + designatedAuths.size() + " designated auths");
        } catch (IOException e) {
            security.logger.error("Failed to load designated auths", e);
            throw new RuntimeException("Security configuration loading failed", e);
        }
    }
    
    private void storeDesignatedAuths() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(designatedAuthsFile, false))) {
            for (String auth : designatedAuths) {
                String encryptedAuth = security.crypter.encryptEncode(auth);
                pw.println(encryptedAuth);
            }
            security.logger.info("Stored " + designatedAuths.size() + " designated auths");
        } catch (IOException e) {
            security.logger.error("Failed to store designated auths", e);
            throw new RuntimeException("Security configuration storage failed", e);
        }
    }
    
    private void loadSecurityConfig() {
        File configFile = new File(securityConfigFile);
        if (!configFile.exists()) {
            security.logger.info("Security config file not found, using defaults");
            return;
        }
        
        try (BufferedReader br = new BufferedReader(new FileReader(configFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    String decryptedData = security.crypter.decodeDecrypt(line.trim());
                    String[] data = decryptedData.split(",");
                    if (data.length >= 3) {
                        this.systemPass = data[0];
                        this.adminPass = data[1];
                        this.adminAuth = data[2];
                    }
                }
            }
            security.logger.info("Security configuration loaded");
        } catch (IOException e) {
            security.logger.error("Failed to load security config", e);
        }
    }
    
    private void storeSecurityConfig() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(securityConfigFile, false))) {
            String configData = String.format("%s,%s,%s", systemPass, adminPass, adminAuth);
            String encryptedData = security.crypter.encryptEncode(configData);
            pw.println(encryptedData);
            security.logger.info("Security configuration stored");
        } catch (IOException e) {
            security.logger.error("Failed to store security config", e);
            throw new RuntimeException("Security configuration storage failed", e);
        }
    }
    
    ////////////////////////////////////////////////////////////////
    // SYSTEM MANAGEMENT
    ////////////////////////////////////////////////////////////////
    
    /**
     * Proper shutdown procedure
     */
    public void shutdown() {
        security.logger.info("ADMIN system shutting down");
        storeSecurityConfig();
        storeDesignatedAuths();
        SecurityProvider.getInstance().shutdown();
    }
    
    /**
     * System status information
     */
    public String getSystemStatus() {
        return String.format(
            "System Status:\n" +
            "- Designated Auths: %d\n" +
            "- Security Provider: Active\n" +
            "- Logger: %s\n" +
            "- Crypter: %s",
            designatedAuths.size(),
            security.logger.getClass().getSimpleName(),
            security.crypter.getClass().getSimpleName()
        );
    }
}