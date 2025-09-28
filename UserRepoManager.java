package SchoolManager;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class UserRepoManager implements RepoManager {
    
    private static final String USERS_DB_FILE = ".databases/.users/.users_db.txt";
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION_MS = 15 * 60 * 1000; // 15 minutes
    
    private final Path usersDbPath;
    private final Crypter crypter;
    private final Logger logger;
    
    private final List<User> userRepo;
    private final List<User> queryMatches;
    private final Map<String, LoginAttempt> loginAttempts;
    
    private boolean repoUpdated;
    
    public UserRepoManager(Crypter crypter, Logger logger) {
        this.crypter = Objects.requireNonNull(crypter, "Crypter cannot be null");
        this.logger = Objects.requireNonNull(logger, "Logger cannot be null");
        
        this.usersDbPath = Paths.get(USERS_DB_FILE);
        this.userRepo = new CopyOnWriteArrayList<>();
        this.queryMatches = new ArrayList<>();
        this.loginAttempts = new HashMap<>();
        this.repoUpdated = false;
        
        initializeRepository();
    }
    
    private void initializeRepository() {
        try {
            loadUsersDataList();
            logger.logINFO("User repository initialized successfully");
        } catch (SecurityException e) {
            logger.logERROR("Security exception during repository initialization: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.logERROR("Failed to initialize user repository: " + e.getMessage());
            // Continue with empty repository rather than failing completely
        }
    }
    
    // Security: Rate limiting for login attempts
    private boolean isAccountLocked(String username) {
        LoginAttempt attempt = loginAttempts.get(username.toLowerCase());
        if (attempt != null && attempt.isLocked()) {
            if (System.currentTimeMillis() - attempt.getLastAttemptTime() < LOCKOUT_DURATION_MS) {
                return true;
            } else {
                loginAttempts.remove(username.toLowerCase()); // Clear expired lockout
            }
        }
        return false;
    }
    
    private void recordLoginAttempt(String username, boolean success) {
        String key = username.toLowerCase();
        LoginAttempt attempt = loginAttempts.getOrDefault(key, new LoginAttempt());
        
        if (success) {
            attempt.reset();
        } else {
            attempt.recordFailure();
        }
        
        loginAttempts.put(key, attempt);
    }
    
    // Enhanced user authentication with security measures
    public AuthenticationResult authenticateUser(String username, char[] password) {
        Objects.requireNonNull(username, "Username cannot be null");
        Objects.requireNonNull(password, "Password cannot be null");
        
        // Security: Check for account lockout
        if (isAccountLocked(username)) {
            logger.logWARN("Account locked due to too many failed attempts: " + username);
            return new AuthenticationResult(null, AuthStatus.ACCOUNT_LOCKED);
        }
        
        // Input validation
        if (!Validator.isValidName(username) || !Validator.isValidPassword(new String(password))) {
            recordLoginAttempt(username, false);
            return new AuthenticationResult(null, AuthStatus.INVALID_CREDENTIALS);
        }
        
        try {
            User authenticatedUser = findAndAuthenticateUser(username, password);
            
            if (authenticatedUser != null) {
                recordLoginAttempt(username, true);
                logger.logINFO("User authentication successful: " + username);
                return new AuthenticationResult(authenticatedUser, AuthStatus.SUCCESS);
            } else {
                recordLoginAttempt(username, false);
                logger.logWARN("User authentication failed: " + username);
                return new AuthenticationResult(null, AuthStatus.INVALID_CREDENTIALS);
            }
            
        } catch (Exception e) {
            logger.logERROR("Authentication error for user " + username + ": " + e.getMessage());
            return new AuthenticationResult(null, AuthStatus.SYSTEM_ERROR);
        } finally {
            // Securely wipe password from memory
            Arrays.fill(password, '\0');
        }
    }
    
    private User findAndAuthenticateUser(String username, char[] password) {
        for (User user : userRepo) {
            if (user.getUsername().equalsIgnoreCase(username)) {
                return verifyUserPassword(user, password);
            }
        }
        return null;
    }
    
    private User verifyUserPassword(User user, char[] password) {
        try {
            // Hash the provided password for comparison
            String providedPasswordHash = crypter.hash(new String(password));
            char[] storedPassword = user.getPassword();
            
            if (storedPassword != null) {
                String storedPasswordHash = new String(storedPassword);
                
                // Use constant-time comparison to prevent timing attacks
                if (constantTimeEquals(providedPasswordHash, storedPasswordHash)) {
                    return user.clone(); // Return clone to protect original
                }
            }
        } catch (Exception e) {
            logger.logERROR("Password verification error for user " + user.getUsername() + ": " + e.getMessage());
        }
        return null;
    }
    
    // Constant-time comparison to prevent timing attacks
    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        
        int result = a.length() ^ b.length();
        for (int i = 0; i < Math.min(a.length(), b.length()); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
    
    // Repository management methods
    @Override
    public SchoolObject createEntry(SchoolObject obj) {
        User user = validateUserObject(obj);
        
        // Check for duplicate ID or username
        if (hasID(user.getID())) {
            throw new IllegalArgumentException("User with ID " + user.getID() + " already exists");
        }
        if (hasUsername(user.getUsername())) {
            throw new IllegalArgumentException("Username " + user.getUsername() + " already exists");
        }
        
        // Hash password before storing
        hashAndSetUserPassword(user);
        userRepo.add(user);
        repoUpdated = true;
        
        logger.logINFO("Created new user: " + user.toSecureString());
        return user;
    }
    
    @Override
    public SchoolObject updateEntry(SchoolObject obj, String attr, String val) {
        User user = validateUserObject(obj);
        User updatedUser = editUser(user, attr, val);
        repoUpdated = true;
        
        logger.logINFO("Updated user " + user.getID() + ": " + attr + " = " + 
                      (attr.equals("password") ? "***" : val));
        return updatedUser;
    }
    
    @Override
    public List<SchoolObject> queryEntry(String attr, String val) {
        List<User> matches = getUserByAttribute(attr, val);
        return new ArrayList<SchoolObject>(matches);
    }
    
    @Override
    public SchoolObject deleteEntry(SchoolObject obj) {
        User user = validateUserObject(obj);
        
        if (userRepo.removeIf(u -> u.getID().equals(user.getID()))) {
            user.secureWipePassword();
            repoUpdated = true;
            logger.logINFO("Deleted user: " + user.toSecureString());
        }
        
        return user;
    }
    
    @Override
    public void viewEntries() {
        listRecords();
    }
    
    @Override
    public List<SchoolObject> getRepository() {
        return new ArrayList<SchoolObject>(userRepo);
    }
    
    // Enhanced repository persistence
    public void storeSaveRepoUpdate() {
        if (repoUpdated) {
            try {
                storeUsersDataList(userRepo);
                repoUpdated = false;
                logger.logINFO("User repository updates stored successfully");
            } catch (SecurityException e) {
                logger.logERROR("Security exception while saving repository: " + e.getMessage());
                throw e;
            } catch (Exception e) {
                logger.logERROR("Failed to save repository updates: " + e.getMessage());
                throw new RuntimeException("Failed to persist user data", e);
            }
        }
    }
    
    private void hashAndSetUserPassword(User user) {
        if (user.getPassword() != null && !user.isPasswordHashed()) {
            String passwordHash = crypter.hash(new String(user.getPassword()));
            user.setPassword(passwordHash);
            user.markPasswordHashed();
        }
    }
    
    private User validateUserObject(SchoolObject obj) {
        if (obj == null) {
            throw new IllegalArgumentException("User object cannot be null");
        }
        if (!(obj instanceof User)) {
            throw new IllegalArgumentException("Object must be of type User");
        }
        return (User) obj;
    }
    
    public boolean hasID(String id) {
        return userRepo.stream().anyMatch(user -> user.getID().equals(id));
    }
    
    public boolean hasUsername(String username) {
        return userRepo.stream().anyMatch(user -> user.getUsername().equalsIgnoreCase(username));
    }
    
    // Secure data persistence
    private void storeUsersDataList(List<User> users) {
        if (users == null) {
            throw new IllegalArgumentException("User list cannot be null");
        }
        
        try {
            // Ensure directory exists
            Files.createDirectories(usersDbPath.getParent());
            
            List<String> encryptedLines = users.stream()
                .map(User::toString)
                .map(crypter::encryptEncode)
                .collect(Collectors.toList());
            
            Files.write(usersDbPath, encryptedLines, StandardCharsets.UTF_8,
                       StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
                       StandardOpenOption.WRITE, StandardOpenOption.SYNC);
            
            // Set secure file permissions (Unix-like systems)
            try {
                Files.setPosixFilePermissions(usersDbPath, 
                    EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
            } catch (UnsupportedOperationException e) {
                // Windows system, ignore permission setting
            }
            
        } catch (IOException e) {
            throw new SecurityException("Failed to securely store user data", e);
        }
    }
    
    private void loadUsersDataList() {
        if (!Files.exists(usersDbPath)) {
            logger.logINFO("User database file does not exist, starting with empty repository");
            return;
        }
        
        if (!Files.isReadable(usersDbPath)) {
            throw new SecurityException("User database file is not readable");
        }
        
        try {
            List<User> loadedUsers = Files.lines(usersDbPath, StandardCharsets.UTF_8)
                .filter(line -> !line.trim().isEmpty())
                .map(crypter::decodeDecrypt)
                .map(User::new)
                .collect(Collectors.toList());
            
            userRepo.clear();
            userRepo.addAll(loadedUsers);
            
            logger.logINFO("Loaded " + loadedUsers.size() + " users from database");
            
        } catch (IOException e) {
            throw new SecurityException("Failed to load user data", e);
        } catch (Exception e) {
            logger.logERROR("Error loading user data: " + e.getMessage());
            throw new RuntimeException("Corrupted or invalid user data", e);
        }
    }
    
    // Additional security methods
    public void secureWipe() {
        userRepo.forEach(User::secureWipePassword);
        userRepo.clear();
        queryMatches.clear();
        loginAttempts.clear();
    }
    
    // Authentication result class
    public static class AuthenticationResult {
        private final User user;
        private final AuthStatus status;
        
        public AuthenticationResult(User user, AuthStatus status) {
            this.user = user;
            this.status = status;
        }
        
        public User getUser() { return user; }
        public AuthStatus getStatus() { return status; }
        public boolean isSuccess() { return status == AuthStatus.SUCCESS; }
    }
    
    public enum AuthStatus {
        SUCCESS, INVALID_CREDENTIALS, ACCOUNT_LOCKED, SYSTEM_ERROR
    }
    
    // Login attempt tracking
    private static class LoginAttempt {
        private int attemptCount;
        private long lastAttemptTime;
        
        public LoginAttempt() {
            this.attemptCount = 0;
            this.lastAttemptTime = 0;
        }
        
        public void recordFailure() {
            attemptCount++;
            lastAttemptTime = System.currentTimeMillis();
        }
        
        public void reset() {
            attemptCount = 0;
            lastAttemptTime = 0;
        }
        
        public boolean isLocked() {
            return attemptCount >= MAX_LOGIN_ATTEMPTS;
        }
        
        public long getLastAttemptTime() {
            return lastAttemptTime;
        }
    }
    
    // ... (other methods like getQueryMatches, clearQueryMatches, etc. remain similar but with security enhancements)
}



