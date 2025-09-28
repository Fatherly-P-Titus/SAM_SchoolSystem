package SchoolManager;

import java.io.Serializable;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Objects;

/**
 * Secure IV (Initialization Vector) vault for cryptographic operations.
 * Provides secure storage and management of initialization vectors with
 * proper encapsulation and security measures.
 */
public final class IVault implements Serializable {
    private static final long serialVersionUID = 1L;
    
    // Security constants
    private static final int DEFAULT_IV_SIZE = 16;
    private static final int MIN_IV_SIZE = 12; // Minimum for AES-GCM
    private static final int MAX_IV_SIZE = 64;
    
    // Security-sensitive fields marked as transient for custom serialization
    private transient boolean initialized;
    private transient int ivSize;
    private transient byte[] iv1;
    private transient byte[] iv2;
    private transient long lastRotationTime;
    
    // Serialization proxy for secure serialization
    private static class SerializationProxy implements Serializable {
        private static final long serialVersionUID = 1L;
        private final boolean initialized;
        private final int ivSize;
        private final byte[] encryptedIV1;
        private final byte[] encryptedIV2;
        private final long lastRotationTime;
        
        SerializationProxy(IVault vault, byte[] encryptionKey) {
            this.initialized = vault.initialized;
            this.ivSize = vault.ivSize;
            this.lastRotationTime = vault.lastRotationTime;
            
            // Encrypt IVs before serialization
            this.encryptedIV1 = vault.iv1 != null ? 
                encryptIV(vault.iv1, encryptionKey) : null;
            this.encryptedIV2 = vault.iv2 != null ? 
                encryptIV(vault.iv2, encryptionKey) : null;
        }
        
        private Object readResolve() {
            IVault vault = new IVault();
            vault.initialized = this.initialized;
            vault.ivSize = this.ivSize;
            vault.lastRotationTime = this.lastRotationTime;
            // IVs will be decrypted during deserialization by the owning Crypter
            return vault;
        }
        
        private byte[] encryptIV(byte[] iv, byte[] key) {
            // Simple XOR encryption for demonstration - use proper encryption in production
            byte[] encrypted = new byte[iv.length];
            for (int i = 0; i < iv.length; i++) {
                encrypted[i] = (byte) (iv[i] ^ key[i % key.length]);
            }
            return encrypted;
        }
    }
    
    public IVault() {
        this(DEFAULT_IV_SIZE);
    }
    
    public IVault(int ivSize) {
        validateIVSize(ivSize);
        
        this.ivSize = ivSize;
        this.initialized = false;
        this.iv1 = new byte[ivSize];
        this.iv2 = new byte[ivSize];
        this.lastRotationTime = System.currentTimeMillis();
        
        initializeWithSecureRandom();
    }
    
    private void validateIVSize(int size) {
        if (size < MIN_IV_SIZE || size > MAX_IV_SIZE) {
            throw new IllegalArgumentException(
                String.format("IV size must be between %d and %d bytes", MIN_IV_SIZE, MAX_IV_SIZE)
            );
        }
    }
    
    private void initializeWithSecureRandom() {
        SecureRandom secureRandom = new SecureRandom();
        
        secureRandom.nextBytes(iv1);
        secureRandom.nextBytes(iv2);
        this.initialized = true;
        
        secureWipeArrays(iv1, iv2); // Clean up temporary arrays
    }
    
    // Secure IV management methods
    public void setIVault1(byte[] iv) {
        validateIV(iv);
        if (this.iv1 != null) {
            Arrays.fill(this.iv1, (byte) 0); // Securely wipe old IV
        }
        this.iv1 = Arrays.copyOf(iv, iv.length);
        this.initialized = true;
        updateRotationTime();
    }
    
    public void setIVault2(byte[] iv) {
        validateIV(iv);
        if (this.iv2 != null) {
            Arrays.fill(this.iv2, (byte) 0); // Securely wipe old IV
        }
        this.iv2 = Arrays.copyOf(iv, iv.length);
        this.initialized = true;
        updateRotationTime();
    }
    
    public void setBothIVs(byte[] iv1, byte[] iv2) {
        validateIV(iv1);
        validateIV(iv2);
        
        // Secure wipe old IVs
        secureWipeIVs();
        
        this.iv1 = Arrays.copyOf(iv1, iv1.length);
        this.iv2 = Arrays.copyOf(iv2, iv2.length);
        this.initialized = true;
        updateRotationTime();
    }
    
    private void validateIV(byte[] iv) {
        Objects.requireNonNull(iv, "IV cannot be null");
        if (iv.length != ivSize) {
            throw new IllegalArgumentException(
                String.format("IV must be exactly %d bytes", ivSize)
            );
        }
        if (isZeroArray(iv)) {
            throw new IllegalArgumentException("IV cannot be all zeros");
        }
    }
    
    private boolean isZeroArray(byte[] array) {
        for (byte b : array) {
            if (b != 0) {
                return false;
            }
        }
        return true;
    }
    
    // Secure accessors that return copies
    public byte[] getIVault1() {
        return iv1 != null ? Arrays.copyOf(iv1, iv1.length) : null;
    }
    
    public byte[] getIVault2() {
        return iv2 != null ? Arrays.copyOf(iv2, iv2.length) : null;
    }
    
    public byte[] getIVault1Copy() {
        return getIVault1(); // Alias for consistency
    }
    
    public byte[] getIVault2Copy() {
        return getIVault2(); // Alias for consistency
    }
    
    // State management
    public void setStat(boolean stat) {
        this.initialized = stat;
        if (!stat) {
            secureWipeIVs();
        }
    }
    
    public boolean getStat() {
        return this.initialized && 
               iv1 != null && iv2 != null && 
               !isZeroArray(iv1) && !isZeroArray(iv2);
    }
    
    public boolean isInitialized() {
        return getStat();
    }
    
    public int getIVSize() {
        return ivSize;
    }
    
    public long getLastRotationTime() {
        return lastRotationTime;
    }
    
    public long getAgeMillis() {
        return System.currentTimeMillis() - lastRotationTime;
    }
    
    // IV rotation and security methods
    public void rotateIVs() {
        SecureRandom secureRandom = new SecureRandom();
        
        secureWipeIVs();
        
        this.iv1 = new byte[ivSize];
        this.iv2 = new byte[ivSize];
        secureRandom.nextBytes(iv1);
        secureRandom.nextBytes(iv2);
        
        updateRotationTime();
    }
    
    public void rotateIV1() {
        SecureRandom secureRandom = new SecureRandom();
        
        if (iv1 != null) {
            Arrays.fill(iv1, (byte) 0);
        }
        iv1 = new byte[ivSize];
        secureRandom.nextBytes(iv1);
        
        updateRotationTime();
    }
    
    public void rotateIV2() {
        SecureRandom secureRandom = new SecureRandom();
        
        if (iv2 != null) {
            Arrays.fill(iv2, (byte) 0);
        }
        iv2 = new byte[ivSize];
        secureRandom.nextBytes(iv2);
        
        updateRotationTime();
    }
    
    private void updateRotationTime() {
        this.lastRotationTime = System.currentTimeMillis();
    }
    
    // Security: Secure wiping of sensitive data
    public void secureWipeIVs() {
        if (iv1 != null) {
            Arrays.fill(iv1, (byte) 0);
            iv1 = null;
        }
        if (iv2 != null) {
            Arrays.fill(iv2, (byte) 0);
            iv2 = null;
        }
        this.initialized = false;
    }
    
    private void secureWipeArrays(byte[]... arrays) {
        for (byte[] array : arrays) {
            if (array != null) {
                Arrays.fill(array, (byte) 0);
            }
        }
    }
    
    // Validation methods
    public boolean validateIVs() {
        return iv1 != null && iv1.length == ivSize && !isZeroArray(iv1) &&
               iv2 != null && iv2.length == ivSize && !isZeroArray(iv2);
    }
    
    public boolean ivsAreDistinct() {
        return iv1 != null && iv2 != null && !Arrays.equals(iv1, iv2);
    }
    
    // Secure serialization
    private Object writeReplace() {
        return new SerializationProxy(this, generateSerializationKey());
    }
    
    private byte[] generateSerializationKey() {
        // In production, use a proper key derivation function
        // This is a simplified example
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        return key;
    }
    
    private void readObject(java.io.ObjectInputStream in) 
            throws java.io.IOException, ClassNotFoundException {
        throw new java.io.NotSerializableException("IVault requires custom serialization");
    }
    
    // Secure string representation (doesn't expose actual IV values)
    @Override
    public String toString() {
        if (!initialized) {
            return "IVault{status=UNINITIALIZED}";
        }
        
        return String.format(
            "IVault{status=INITIALIZED, ivSize=%d, iv1Hash=%s, iv2Hash=%s, lastRotation=%d}",
            ivSize, 
            iv1 != null ? Integer.toHexString(Arrays.hashCode(iv1)) : "null",
            iv2 != null ? Integer.toHexString(Arrays.hashCode(iv2)) : "null",
            lastRotationTime
        );
    }
    
    // Debug string (use with caution - exposes partial IV information)
    public String toDebugString() {
        if (!initialized || iv1 == null || iv2 == null) {
            return "IVault{UNINITIALIZED}";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("IVault{initialized=").append(initialized)
          .append(", ivSize=").append(ivSize)
          .append(", lastRotation=").append(lastRotationTime)
          .append("}\n");
        
        sb.append("IV1: [");
        appendPartialHex(sb, iv1, 4); // Show only first 4 bytes for security
        sb.append("...] (length: ").append(iv1.length).append(")\n");
        
        sb.append("IV2: [");
        appendPartialHex(sb, iv2, 4); // Show only first 4 bytes for security
        sb.append("...] (length: ").append(iv2.length).append(")");
        
        return sb.toString();
    }
    
    private void appendPartialHex(StringBuilder sb, byte[] data, int maxBytes) {
        int bytesToShow = Math.min(maxBytes, data.length);
        for (int i = 0; i < bytesToShow; i++) {
            if (i > 0) sb.append(",");
            sb.append(String.format("%02X", data[i] & 0xFF));
        }
        if (bytesToShow < data.length) {
            sb.append(",...");
        }
    }
    
    // Secure equals and hashCode
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        IVault other = (IVault) obj;
        return this.initialized == other.initialized &&
               this.ivSize == other.ivSize &&
               Arrays.equals(this.iv1, other.iv1) &&
               Arrays.equals(this.iv2, other.iv2);
    }
    
    @Override
    public int hashCode() {
        int result = Objects.hash(initialized, ivSize);
        result = 31 * result + Arrays.hashCode(iv1);
        result = 31 * result + Arrays.hashCode(iv2);
        return result;
    }
    
    // Finalizer for additional security cleanup
    @Override
    protected void finalize() throws Throwable {
        try {
            secureWipeIVs();
        } finally {
            super.finalize();
        }
    }
    
    // Builder pattern for fluent creation
    public static class Builder {
        private int ivSize = DEFAULT_IV_SIZE;
        private byte[] iv1;
        private byte[] iv2;
        
        public Builder withIVSize(int ivSize) {
            validateIVSize(ivSize);
            this.ivSize = ivSize;
            return this;
        }
        
        public Builder withIV1(byte[] iv1) {
            this.iv1 = iv1 != null ? Arrays.copyOf(iv1, iv1.length) : null;
            return this;
        }
        
        public Builder withIV2(byte[] iv2) {
            this.iv2 = iv2 != null ? Arrays.copyOf(iv2, iv2.length) : null;
            return this;
        }
        
        public IVault build() {
            IVault vault = new IVault(ivSize);
            if (iv1 != null) vault.setIVault1(iv1);
            if (iv2 != null) vault.setIVault2(iv2);
            return vault;
        }
        
        private void validateIVSize(int size) {
            if (size < MIN_IV_SIZE || size > MAX_IV_SIZE) {
                throw new IllegalArgumentException(
                    String.format("IV size must be between %d and %d bytes", MIN_IV_SIZE, MAX_IV_SIZE)
                );
            }
        }
    }
    
    // Static factory methods
    public static IVault createWithSecureIVs() {
        return new IVault();
    }
    
    public static IVault createWithSecureIVs(int ivSize) {
        return new IVault(ivSize);
    }
    
    public static IVault createEmpty() {
        IVault vault = new IVault();
        vault.initialized = false;
        return vault;
    }
}