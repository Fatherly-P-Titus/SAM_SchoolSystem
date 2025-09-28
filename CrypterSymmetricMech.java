package SchoolManager;

import javax.crypto.*;
import javax.crypto.spec.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;

public class CrypterSymmetricMech implements Crypter {
    
    private final SecureRandom secureRandom = new SecureRandom();
    
    // Sensitive data storage
    private char[] plainData;
    private byte[] cipherData;
    
    private SecretKey key;
    private IvParameterSpec iv;
    private byte[] ivBytes;
    
    private final KeyStore keyStore;
    private IVault vault;
    
    // Security configurations
    private static final int KEY_STRENGTH_MIN_BITS = 128;
    private static final int MAX_DECRYPT_ATTEMPTS = 3;
    private static final long IV_ROTATION_THRESHOLD_MS = 24 * 60 * 60 * 1000; // 24 hours
    
    private int decryptAttempts = 0;
    private long lastKeyRotationTime;
    
    public CrypterSymmetricMech() throws CryptoException {
        try {
            this.keyStore = KeyStore.getInstance("JCEKS");
            initializeSecurityComponents();
        } catch (KeyStoreException e) {
            throw new CryptoException("Failed to initialize KeyStore", e);
        }
    }
    
    public CrypterSymmetricMech(SecretKey key) throws CryptoException {
        this();
        if (validateKey(key)) {
            this.key = key;
            this.lastKeyRotationTime = System.currentTimeMillis();
        } else {
            throw new CryptoException("Invalid or weak key provided");
        }
    }
    
    private void initializeSecurityComponents() throws CryptoException {
        try {
            // Load or create KeyStore
            Path ksPath = Paths.get(KS_FILE);
            if (Files.exists(ksPath)) {
                loadKeyStore();
            } else {
                createNewKeyStore();
            }
            
            // Initialize IV management with enhanced IVault
            initializeIVManagement();
            
            // Validate or generate key
            if (this.key == null || !validateKeyStrength() || shouldRotateKey()) {
                generateAndStoreNewKey();
            }
            
        } catch (Exception e) {
            throw new CryptoException("Failed to initialize security components", e);
        }
    }
    
    private void createNewKeyStore() throws CryptoException {
        try {
            Path ksPath = Paths.get(KS_FILE);
            Files.createDirectories(ksPath.getParent());
            
            keyStore.load(null, KS_PASSWORD.toCharArray());
            storeKeyStore();
        } catch (Exception e) {
            throw new CryptoException("Failed to create new KeyStore", e);
        }
    }
    
    private void initializeIVManagement() throws CryptoException {
        try {
            Path ivPath = Paths.get(IV_BANK_FILE);
            
            if (Files.exists(ivPath)) {
                vault = loadIVault();
                if (vault != null && vault.isInitialized() && vault.validateIVs()) {
                    this.ivBytes = vault.getIVault1(); // Use getter instead of direct access
                    this.iv = new IvParameterSpec(ivBytes);
                    
                    // Check if IV needs rotation
                    if (shouldRotateIVs()) {
                        logger.logINFO("Rotating IVs due to age threshold");
                        rotateIVs();
                    }
                    return;
                }
            }
            
            // Create new secure IV vault using builder pattern
            vault = IVault.createWithSecureIVs(IV_SIZE);
            this.ivBytes = vault.getIVault1();
            this.iv = new IvParameterSpec(ivBytes);
            storeIVault();
            
            logger.logINFO("New IVault initialized with secure IVs");
            
        } catch (Exception e) {
            throw new CryptoException("Failed to initialize IV management", e);
        }
    }
    
    // Enhanced key generation with key derivation
    private SecretKey generateSecureKey() throws CryptoException {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance(AES_ALGORITHM);
            keyGen.init(AES_KEY_SIZE, secureRandom);
            SecretKey newKey = keyGen.generateKey();
            this.lastKeyRotationTime = System.currentTimeMillis();
            return newKey;
        } catch (NoSuchAlgorithmException e) {
            throw new CryptoException("AES algorithm not available", e);
        }
    }
    
    // Generate key from password using PBKDF2
    private SecretKey generateKeyFromPassword(char[] password, byte[] salt) throws CryptoException {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, PBKDF2_ITERATIONS, AES_KEY_SIZE);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
            SecretKey tmpKey = factory.generateSecret(spec);
            return new SecretKeySpec(tmpKey.getEncoded(), AES_ALGORITHM);
        } catch (Exception e) {
            throw new CryptoException("Failed to generate key from password", e);
        }
    }
    
    @Override
    public void changeIV() {
        try {
            if (vault != null) {
                vault.rotateIV1();
                this.ivBytes = vault.getIVault1();
                this.iv = new IvParameterSpec(ivBytes);
                storeIVault();
                logger.logINFO("IV rotated successfully");
            } else {
                generateIV();
            }
        } catch (CryptoException e) {
            throw new SecurityException("Failed to change IV", e);
        }
    }
    
    public void rotateIVs() throws CryptoException {
        if (vault != null) {
            vault.rotateIVs();
            this.ivBytes = vault.getIVault1();
            this.iv = new IvParameterSpec(ivBytes);
            storeIVault();
            logger.logINFO("Both IVs rotated successfully");
        } else {
            throw new CryptoException("IVault not initialized");
        }
    }
    
    // Enhanced encryption with IV validation
    @Override
    public byte[] encrypt(String plaintext) throws CryptoException {
        validateEncryptionInputs(plaintext);
        
        try {
            // Validate IV before use
            if (!validateIVForUse()) {
                logger.logWARN("IV validation failed, rotating IVs");
                rotateIVs();
            }
            
            Cipher cipher = Cipher.getInstance(AES_CBC_PKCS5PADDING);
            cipher.init(Cipher.ENCRYPT_MODE, key, iv);
            
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] combined = getCombined(encrypted);
            
            // Update IV after successful encryption
            if (vault != null) {
                storeIVault(); // Persist IV state
            }
            
            return combined;
            
        } catch (Exception e) {
            throw new CryptoException("Encryption failed", e);
        }
    }
    
    // Batch encryption for multiple records
    public byte[][] encryptBatch(String[] plaintexts) throws CryptoException {
        if (plaintexts == null || plaintexts.length == 0) {
            throw new IllegalArgumentException("Plaintexts array cannot be null or empty");
        }
        
        byte[][] results = new byte[plaintexts.length][];
        for (int i = 0; i < plaintexts.length; i++) {
            results[i] = encrypt(plaintexts[i]);
            
            // Rotate IV for each encryption in batch mode
            if (i < plaintexts.length - 1) {
                changeIV();
            }
        }
        return results;
    }
    
    @Override
    public String decrypt(byte[] combined) throws CryptoException {
        validateDecryptionInputs(combined);
        
        // Rate limiting for decryption attempts
        if (decryptAttempts >= MAX_DECRYPT_ATTEMPTS) {
            throw new CryptoException("Too many decryption attempts - potential brute force attack");
        }
        
        decryptAttempts++;
        
        try {
            // Extract IV and encrypted data
            byte[] ivExtracted = Arrays.copyOfRange(combined, 0, IV_SIZE);
            byte[] encrypted = Arrays.copyOfRange(combined, IV_SIZE, combined.length);
            
            // Validate extracted IV
            if (isZeroArray(ivExtracted)) {
                throw new CryptoException("Invalid IV: zero IV detected");
            }
            
            Cipher cipher = Cipher.getInstance(AES_CBC_PKCS5PADDING);
            cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(ivExtracted));
            
            byte[] decrypted = cipher.doFinal(encrypted);
            decryptAttempts = 0; // Reset on success
            
            return new String(decrypted, StandardCharsets.UTF_8);
            
        } catch (Exception e) {
            logger.logERROR("Decryption attempt failed: " + e.getMessage());
            throw new CryptoException("Decryption failed", e);
        }
    }
    
    // Batch decryption for multiple records
    public String[] decryptBatch(byte[][] encryptedTexts) throws CryptoException {
        if (encryptedTexts == null || encryptedTexts.length == 0) {
            throw new IllegalArgumentException("Encrypted texts array cannot be null or empty");
        }
        
        String[] results = new String[encryptedTexts.length];
        for (int i = 0; i < encryptedTexts.length; i++) {
            results[i] = decrypt(encryptedTexts[i]);
        }
        return results;
    }
    
    // Enhanced validation methods
    private void validateEncryptionInputs(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            throw new IllegalArgumentException("Plaintext cannot be null or empty");
        }
        if (key == null) {
            throw new IllegalStateException("Encryption key not initialized");
        }
        if (iv == null || ivBytes == null) {
            throw new IllegalStateException("IV not initialized");
        }
    }
    
    private void validateDecryptionInputs(byte[] combined) {
        if (combined == null) {
            throw new IllegalArgumentException("Ciphertext cannot be null");
        }
        if (combined.length <= IV_SIZE) {
            throw new IllegalArgumentException("Ciphertext too short");
        }
        if (key == null) {
            throw new IllegalStateException("Decryption key not initialized");
        }
    }
    
    private boolean validateIVForUse() {
        if (vault == null) return false;
        
        return vault.validateIVs() && 
               vault.ivsAreDistinct() && 
               !isZeroArray(ivBytes) &&
               vault.getAgeMillis() < IV_ROTATION_THRESHOLD_MS;
    }
    
    private boolean isZeroArray(byte[] array) {
        if (array == null) return true;
        for (byte b : array) {
            if (b != 0) return false;
        }
        return true;
    }
    
    private boolean shouldRotateIVs() {
        return vault != null && vault.getAgeMillis() > IV_ROTATION_THRESHOLD_MS;
    }
    
    private boolean shouldRotateKey() {
        // Rotate key every 30 days by default
        return System.currentTimeMillis() - lastKeyRotationTime > (30L * 24 * 60 * 60 * 1000);
    }
    
    // Secure hashing implementations
    @Override
    public String hash(String data) {
        if (data == null) return null;
        
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new SecurityException("SHA-256 algorithm not available", e);
        }
    }
    
    @Override
    public String hashPBKDF2(String data) {
        return hashPBKDF2(data != null ? data.getBytes(StandardCharsets.UTF_8) : new byte[0]);
    }
    
    @Override
    public String hashPBKDF2(byte[] data) {
        try {
            byte[] salt = generateSecureRandom(SALT_SIZE);
            
            KeySpec spec = new PBEKeySpec(
                bytesToChars(data),
                salt, 
                PBKDF2_ITERATIONS, 
                256
            );
            
            SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
            byte[] hash = factory.generateSecret(spec).getEncoded();
            
            // Combine salt + hash for storage
            byte[] combined = new byte[salt.length + hash.length];
            System.arraycopy(salt, 0, combined, 0, salt.length);
            System.arraycopy(hash, 0, combined, salt.length, hash.length);
            
            return Base64.getEncoder().encodeToString(combined);
            
        } catch (Exception e) {
            throw new SecurityException("PBKDF2 hashing failed", e);
        } finally {
            // Clean up sensitive data
            if (data != null) Arrays.fill(data, (byte) 0);
        }
    }
    
    private char[] bytesToChars(byte[] bytes) {
        if (bytes == null) return new char[0];
        char[] chars = new char[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            chars[i] = (char) (bytes[i] & 0xFF);
        }
        return chars;
    }
    
    // Enhanced KeyStore management
    @Override
    public void storeKeyStore() throws CryptoException {
        try {
            KeyStore.SecretKeyEntry entry = new KeyStore.SecretKeyEntry(key);
            KeyStore.ProtectionParameter param = new KeyStore.PasswordProtection(
                KEY_PASSWORD.toCharArray()
            );
            
            keyStore.setEntry(KEY_ALIAS, entry, param);
            
            // Set secure file permissions
            Path ksPath = Paths.get(KS_FILE);
            Files.createDirectories(ksPath.getParent());
            
            try (FileOutputStream fos = new FileOutputStream(KS_FILE)) {
                keyStore.store(fos, KS_PASSWORD.toCharArray());
            }
            
            // Set file permissions (Unix systems)
            try {
                Files.setPosixFilePermissions(ksPath, 
                    java.util.Set.of(
                        PosixFilePermission.OWNER_READ,
                        PosixFilePermission.OWNER_WRITE
                    ));
            } catch (UnsupportedOperationException e) {
                // Windows system, ignore
            }
            
        } catch (Exception e) {
            throw new CryptoException("Failed to store KeyStore", e);
        }
    }
    
    @Override
    public void loadKeyStore() throws CryptoException {
        Path ksPath = Paths.get(KS_FILE);
        if (!Files.exists(ksPath)) {
            throw new CryptoException("KeyStore file does not exist: " + KS_FILE);
        }
        
        try (FileInputStream fis = new FileInputStream(KS_FILE)) {
            keyStore.load(fis, KS_PASSWORD.toCharArray());
            
            KeyStore.ProtectionParameter param = new KeyStore.PasswordProtection(
                KEY_PASSWORD.toCharArray()
            );
            
            KeyStore.SecretKeyEntry entry = (KeyStore.SecretKeyEntry) 
                keyStore.getEntry(KEY_ALIAS, param);
            
            if (entry != null) {
                this.key = entry.getSecretKey();
                this.lastKeyRotationTime = System.currentTimeMillis();
            }
            
        } catch (Exception e) {
            throw new CryptoException("Failed to load KeyStore", e);
        }
    }
    
    // IVault management with enhanced security
    private void storeIVault() throws CryptoException {
        if (vault == null) {
            throw new CryptoException("IVault not initialized");
        }
        
        try {
            Path ivPath = Paths.get(IV_BANK_FILE);
            Files.createDirectories(ivPath.getParent());
            
            try (ObjectOutputStream oos = new ObjectOutputStream(
                Files.newOutputStream(ivPath, 
                    StandardOpenOption.CREATE, 
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.SYNC))) {
                oos.writeObject(vault);
            }
            
            // Set secure file permissions
            try {
                Files.setPosixFilePermissions(ivPath, 
                    java.util.Set.of(PosixFilePermission.OWNER_READ));
            } catch (UnsupportedOperationException e) {
                // Windows system, ignore
            }
            
        } catch (IOException e) {
            throw new CryptoException("Failed to store IVault", e);
        }
    }
    
    private IVault loadIVault() throws CryptoException {
        Path ivPath = Paths.get(IV_BANK_FILE);
        if (!Files.exists(ivPath)) {
            return null;
        }
        
        try (ObjectInputStream ois = new ObjectInputStream(
            Files.newInputStream(ivPath))) {
            return (IVault) ois.readObject();
        } catch (Exception e) {
            throw new CryptoException("Failed to load IVault", e);
        }
    }
    
    // Security management methods
    @Override
    public void secureWipe() {
        // Wipe sensitive data from memory
        if (plainData != null) {
            Arrays.fill(plainData, '\0');
            plainData = null;
        }
        if (cipherData != null) {
            Arrays.fill(cipherData, (byte) 0);
            cipherData = null;
        }
        if (ivBytes != null) {
            Arrays.fill(ivBytes, (byte) 0);
            ivBytes = null;
        }
        iv = null;
        
        if (vault != null) {
            vault.secureWipeIVs();
        }
        
        decryptAttempts = 0;
    }
    
    public void performSecurityAudit() {
        logger.logINFO("Performing security audit...");
        
        boolean keyValid = validateKeyStrength();
        boolean ivValid = vault != null && vault.validateIVs();
        boolean ivDistinct = vault != null && vault.ivsAreDistinct();
        boolean ivFresh = vault != null && vault.getAgeMillis() < IV_ROTATION_THRESHOLD_MS;
        
        logger.logINFO("Key strength: " + (keyValid ? "VALID" : "INVALID"));
        logger.logINFO("IV validity: " + (ivValid ? "VALID" : "INVALID"));
        logger.logINFO("IV distinctness: " + (ivDistinct ? "VALID" : "INVALID"));
        logger.logINFO("IV freshness: " + (ivFresh ? "VALID" : "STALE"));
        
        if (!keyValid || !ivValid || !ivDistinct || !ivFresh) {
            logger.logWARN("Security audit detected issues that may require attention");
        }
    }
    
    // Getters and setters with enhanced security
    @Override
    public void setPlainData(String data) {
        // Securely wipe old data
        if (this.plainData != null) {
            Arrays.fill(this.plainData, '\0');
        }
        this.plainData = data != null ? data.toCharArray() : null;
    }
    
    @Override
    public String getPlainData() {
        return plainData != null ? new String(plainData) : null;
    }
    
    @Override
    public void setCipherData(String data) {
        if (this.cipherData != null) {
            Arrays.fill(this.cipherData, (byte) 0);
        }
        this.cipherData = data != null ? data.getBytes(StandardCharsets.UTF_8) : null;
    }
    
    @Override
    public SecretKey getCrypterKey() {
        return this.key;
    }
    
    @Override
    public void setCrypterKey(SecretKey key) {
        if (validateKey(key)) {
            this.key = key;
            this.lastKeyRotationTime = System.currentTimeMillis();
        } else {
            throw new IllegalArgumentException("Invalid or weak key");
        }
    }
    
    @Override
    public IvParameterSpec getIV() {
        return iv;
    }
    
    @Override
    public byte[] getIVBytes() {
        return ivBytes != null ? ivBytes.clone() : null;
    }
    
    @Override
    public void setIVBytes(byte[] ivBytes) {
        if (ivBytes == null || ivBytes.length != IV_SIZE) {
            throw new IllegalArgumentException("IV must be exactly " + IV_SIZE + " bytes");
        }
        if (isZeroArray(ivBytes)) {
            throw new IllegalArgumentException("IV cannot be all zeros");
        }
        
        if (this.ivBytes != null) {
            Arrays.fill(this.ivBytes, (byte) 0);
        }
        this.ivBytes = ivBytes.clone();
        this.iv = new IvParameterSpec(this.ivBytes);
        
        if (vault != null) {
            vault.setIVault1(ivBytes);
        }
    }
    
    @Override
    public byte[] getCombined(byte[] encryptedData) {
        if (ivBytes == null) {
            throw new IllegalStateException("IV not initialized");
        }
        
        byte[] combined = new byte[IV_SIZE + encryptedData.length];
        System.arraycopy(ivBytes, 0, combined, 0, IV_SIZE);
        System.arraycopy(encryptedData, 0, combined, IV_SIZE, encryptedData.length);
        return combined;
    }
    
    // Utility methods
    @Override
    public boolean validateKeyStrength() {
        return validateKey(this.key);
    }
    
    private boolean validateKey(SecretKey key) {
        if (key == null) return false;
        
        try {
            return key.getEncoded().length * 8 >= KEY_STRENGTH_MIN_BITS &&
                   AES_ALGORITHM.equals(key.getAlgorithm());
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public byte[] generateSecureRandom(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("Size must be positive");
        }
        byte[] randomBytes = new byte[size];
        secureRandom.nextBytes(randomBytes);
        return randomBytes;
    }
    
    // Custom exception for cryptographic operations
    public static class CryptoException extends Exception {
        public CryptoException(String message) {
            super(message);
        }
        
        public CryptoException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    
    
    
    // Logger placeholder (should be injected)
    private interface Logger {
        void logINFO(String message);
        void logWARN(String message);
        void logERROR(String message);
    }
    
    private Logger logger = new DefaultLogger();
    
    private static class DefaultLogger implements Logger {
        @Override
        public void logINFO(String message) {
            System.out.println("[INFO] " + message);
        }
        
        @Override
        public void logWARN(String message) {
            System.out.println("[WARN] " + message);
        }
        
        @Override
        public void logERROR(String message) {
            System.out.println("[ERROR] " + message);
        }
    }
    
    public void setLogger(Logger logger) {
        this.logger = Objects.requireNonNull(logger, "Logger cannot be null");
    }
}