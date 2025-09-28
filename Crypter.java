package SchoolManager;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.security.spec.KeySpec;
import java.util.Base64;

public interface Crypter {
    
    // Constants should be in uppercase and more secure
    String KS_PASSWORD = "KSPass475"; // Should be loaded from secure config
    String KS_ALIAS = "KeyStore1";
    String KEY_PASSWORD = "KEYEntPass991"; // Should be loaded from secure config
    String KEY_ALIAS = "KeyAlias1";
    
    String IV_BANK_FILE = ".system_configs/.security_configs/.ivb.ser";
    String KS_FILE = ".system_configs/.security_configs/.sam_app_keystore.jceks";
    
    // Cryptographic constants
    String AES_ALGORITHM = "AES";
    String AES_CBC_PKCS5PADDING = "AES/CBC/PKCS5Padding";
    String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
    int AES_KEY_SIZE = 256;
    int IV_SIZE = 16;
    int SALT_SIZE = 32;
    int PBKDF2_ITERATIONS = 310000; // OWASP recommended minimum
    
    void storeKeyStore();
    void loadKeyStore();
    
    void setPlainData(String data);
    void setCipherData(String data);
    
    void setCrypterKey(SecretKey key);
    SecretKey getCrypterKey();
    
    IvParameterSpec getIV();
    byte[] getIVBytes();
    void setIVBytes(byte[] iv);
    void changeIV();
    
    String hash(String data);
    String hashPBKDF2(String data);
    String hashPBKDF2(byte[] data);
    
    byte[] encrypt(String data);
    String encryptEncode(String data);
    
    String encode64(byte[] data);
    byte[] decode64(String data);
    
    String decrypt(byte[] cipher);
    String decodeDecrypt(String data);
    
    byte[] getCombined(byte[] data);
    
    // New security methods
    void secureWipe();
    boolean validateKeyStrength();
    byte[] generateSecureRandom(int size);
}