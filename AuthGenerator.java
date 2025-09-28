package SchoolManager;

import java.util.*;
import java.io.*;
import java.util.stream.Collectors;

public class AuthGenerator {
    
    private static final String ID_STORE_FILE = "generated_auths.txt";
    private static final String SECURITY_CONFIG_FILE = "security_config.txt";
    private static final int DEFAULT_ID_LENGTH = 6;
    private static final int AUTH_CODE_LENGTH = 6; // 3 letters + 3 digits
    private static final int LETTERS_COUNT = 3;
    private static final int DIGITS_COUNT = 3;
    
    private static final List<String> ALPHANUMERIC_CHARS = 
        Arrays.asList("A", "B", "C", "J", "Q", "Y", "W", "Z", "R", "X", "S", "H");
    
    private final Crypter crypter;
    private final Set<String> generatedIds;
    private final Set<String> generatedAuths;
    private final Logger logger;
    
    public AuthGenerator(Logger logger, Crypter crypter) {
        this.crypter = crypter;
        this.logger = logger;
        this.generatedIds = new HashSet<>();
        this.generatedAuths = new HashSet<>();
        
        // Uncomment if needed
        // this.loadAllAuths();
    }
    
    // Main ID generation method with full control
    public String generateId(String prefix, int length) {
        if (prefix == null) prefix = "#";
        if (length <= prefix.length()) {
            throw new IllegalArgumentException("Length must be greater than prefix length");
        }
        
        String id;
        do {
            id = prefix + generateRandomDigits(length - prefix.length());
        } while (generatedIds.contains(id));
        
        generatedIds.add(id);
        storeGeneratedIds();
        return id;
    }
    
    // Convenience method with default length
    public String generateId(String prefix) {
        return generateId(prefix, DEFAULT_ID_LENGTH);
    }
    
    // Convenience method with default prefix and length
    public String generateId() {
        return generateId("#", DEFAULT_ID_LENGTH);
    }
    
    public String generateAuth() {
        String auth;
        do {
            auth = generateRandomLetters(LETTERS_COUNT) + generateRandomDigits(DIGITS_COUNT);
        } while (generatedAuths.contains(auth));
        
        generatedAuths.add(auth);
        storeGeneratedAuths();
        return auth;
    }
    
    public void loadAllAuths() {
        String msg = "LOADED ALL GENERATED AUTHS SUCCESSFULLY!";
        try (BufferedReader br = new BufferedReader(new FileReader(ID_STORE_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    processStoredAuthLine(line);
                }
            }
            logger.log(msg);
            System.out.println(msg);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
    
    private void processStoredAuthLine(String encryptedLine) {
        try {
            String data = crypter.decodeDecrypt(encryptedLine);
            String[] parts = data.split(",");
            if (parts.length >= 2) {
                String type = parts[1];
                switch (type) {
                    case "id" -> generatedIds.add(parts[0]);
                    case "auth" -> generatedAuths.add(parts[0]); // Fixed index
                }
            }
        } catch (Exception e) {
            logger.log("Error processing stored auth line: " + e.getMessage());
        }
    }
    
    private void storeGeneratedIds() {
        storeGeneratedData(generatedIds, "id", "GENERATED IDS STORED SUCCESSFULLY");
    }
    
    private void storeGeneratedAuths() {
        storeGeneratedData(generatedAuths, "auth", "GENERATED AUTHS STORED SUCCESSFULLY");
    }
    
    private void storeGeneratedData(Set<String> dataSet, String type, String successMessage) {
        if (dataSet.isEmpty()) return;
        
        try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(ID_STORE_FILE, false)))) {
            for (String item : dataSet) {
                String data = String.format("%s,%s", item, type);
                pw.println(crypter.encryptEncode(data));
            }
            System.out.println(successMessage);
            logger.log(successMessage);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
    
    public String madProtocol(int lines, int mark) {
        System.out.println("ENGAGING *MAD* PROTOCOL SEQUENCE!");
        String finalPassword = "";
        
        try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(SECURITY_CONFIG_FILE, false)))) {
            for (int i = 0; i < lines; i++) {
                String pass = generateMadCode();
                if (i == mark) {
                    finalPassword = pass;
                }
                pw.printf("•ACCESS: %s%n", pass);
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return finalPassword;
    }
    
    private String generateMadCode() {
        return generateRandomLetters(LETTERS_COUNT) + generateRandomDigits(DIGITS_COUNT);
    }
    
    private String generateRandomLetters(int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            int rnd = (int) (Math.random() * ALPHANUMERIC_CHARS.size());
            sb.append(ALPHANUMERIC_CHARS.get(rnd));
        }
        return sb.toString();
    }
    
    private String generateRandomDigits(int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append((int) (Math.random() * 10));
        }
        return sb.toString();
    }
    
    public String infoString() {
        return String.format("•=> Generated IDs: %d%n•=> Generated Auths: %d%n", 
                           generatedIds.size(), generatedAuths.size());
    }
    
    // Utility methods for testing and management
    public Set<String> getGeneratedIds() {
        return Collections.unmodifiableSet(generatedIds);
    }
    
    public Set<String> getGeneratedAuths() {
        return Collections.unmodifiableSet(generatedAuths);
    }
    
    public void clearGeneratedData() {
        generatedIds.clear();
        generatedAuths.clear();
    }
}