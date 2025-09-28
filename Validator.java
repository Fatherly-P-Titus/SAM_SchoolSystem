package SchoolManager;

import java.util.*;
import java.io.*;
import java.util.regex.Pattern;


public class Validator {
    
    private final String designations_file = "designations.txt";
    private final String users_login_file = "users.txt";
    
    private Crypter crypter;
    private RepoManager rpm;
    private Map<String,Permission> designations;
    
    
    
    
    
       // Normal text pattern: letters, numbers, spaces, common punctuation
    private static final Pattern NORMAL_TEXT_PATTERN = 
        Pattern.compile("^[a-zA-Z0-9\\s.,!?;:'\"()-]+$");
    
     // Strict alphabet-only text pattern: letters, numbers, spaces, common punctuation
    private static final Pattern ALPHABET_TEXT_PATTERN = 
        Pattern.compile("^[a-zA-Z\\s.()-]+$");
 
    // 7-digit alphanumeric ID pattern
    private static final Pattern ID_PATTERN = 
        Pattern.compile("^[a-zA-Z0-9]{7}$");
    
        // digits numeric number pattern
    private static final Pattern NUMBER_PATTERN = 
        Pattern.compile("^[0-9]$");
    
    // Password pattern: at least 8 chars, must contain letter and number
    private static final Pattern PASSWORD_PATTERN = 
        Pattern.compile("^(?=.*[a-zA-Z])(?=.*[0-9])[a-zA-Z0-9@#$%^&+=!*()_\\-\\[\\]{}|;:,.<>?/~`]{8,}$");
    
       // Name: No special characters, allows spaces, hyphens, and apostrophes for names like O'Connor or Jean-Luc
    private static final Pattern NAME_PATTERN = 
        Pattern.compile("^[a-zA-Z]+(?:[\\s'-][a-zA-Z]+)*$");
    
    // Age: 1-120 years old
    private static final Pattern AGE_PATTERN = 
        Pattern.compile("^(?:1[0-1][0-9]|120|[1-9][0-9]?)$");
    
    // Nigerian Phone Numbers: supports all major formats
    private static final Pattern NIGERIAN_PHONE_PATTERN = 
        Pattern.compile("^(\\+234|0)([7-9][0-1])([0-9]{8})$");
    
    // Address: Allows letters, numbers, spaces, and common address punctuation
    private static final Pattern ADDRESS_PATTERN = 
        Pattern.compile("^[a-zA-Z0-9\\s.,#-]+$");
    
    // Email: Comprehensive email validation
    private static final Pattern EMAIL_PATTERN = 
        Pattern.compile("^(?i)[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}$");
    
    
    
    // Validation methods
    public static boolean isValidName(String name) {
        return name != null && NAME_PATTERN.matcher(name.trim()).matches();
    }
    
    public static boolean isValidAge(String age) {
        if (age == null) return false;
        try {
            int ageValue = Integer.parseInt(age.trim());
            return ageValue >= 1 && ageValue <= 120;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    public static boolean isValidNigerianPhone(String phone) {
        if (phone == null) return false;
        String cleanedPhone = phone.trim().replaceAll("\\s+|-", "");
        return NIGERIAN_PHONE_PATTERN.matcher(cleanedPhone).matches();
    }
    
    public static boolean isValidAddress(String address) {
        return address != null && address.trim().length() >= 5 && 
               ADDRESS_PATTERN.matcher(address.trim()).matches();
    }
    
    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email.trim()).matches();
    }
    
    
    public static boolean isValidText(String input) {
        return input != null && NORMAL_TEXT_PATTERN.matcher(input).matches();
    }
    
        
    public static boolean isValidAlphaText(String input) {
        return input != null && ALPHABET_TEXT_PATTERN.matcher(input).matches();
    }
    
    public static boolean isValidID(String id) {
        return id != null && ID_PATTERN.matcher(id).matches();
    }
    
    public static boolean isValidNumber(String num) {
        return num != null && NUMBER_PATTERN.matcher(num).matches();
    }
    
    public static boolean isValidPassword(String password) {
        return password != null && PASSWORD_PATTERN.matcher(password).matches();
    }
    
    
    public Validator(Crypter crypter) {
        this.crypter = crypter;
        this.designations = new HashMap<>();
        this.loadDesignationList();
        System.out.println("\n\t\t [ACCESS VALIDATOR INITIALIZED] \n");
    }

    public void setRepoManager(RepoManager rpm){
        this.rpm = rpm;
    }
    
    
    
    
    public boolean hasAccess(String id,String scope){
        return this.hasPermission(id,scope,"ACCESS");
    }
    
    public boolean hasReadAccess(String id, String scope){
        return this.hasPermission(id,scope,"READ");
    }
    public boolean hasWriteAccess(String id,String scope){
        return this.hasPermission(id,scope,"WRITE");
    }
    public boolean hasViewAccess(String id,String scope){
        return this.hasPermission(id,scope,"VIEW");
    }
    public boolean hasEditAccess(String id,String scope){
        return this.hasPermission(id,scope,"EDIT");
    }
    public boolean hasDeleteAccess(String id,String scope){
        return this.hasPermission(id,scope,"DELETE");
    }
    
    
    private boolean hasPermission(String id,String scope,String permit){
        /*
        Does the designator with this id exist?;
        Does he have the specified permission scope?;
        Does he have the specified scope pernit(s?);
        */
        boolean stat = false;
        
        if(this.designations.containsKey(id)){
            Permission p = this.designations.get(id);
            if(p.hasScope(scope)){
                List<String> scope_permits = p.getScopePermits(scope);
                if(scope_permits.contains(permit)){
                    stat = true;
                }
            }
        } return stat;
    }
    
    
    
    // Detailed validation with error messages
    public static String validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "Name cannot be empty";
        }
        if (name.trim().length() < 2) {
            return "Name must be at least 2 characters long";
        }
        if (!isValidName(name)) {
            return "Name can only contain letters, spaces, hyphens, and apostrophes";
        }
        return null;
    }
    
    public static String validateAge(String age) {
        if (age == null || age.trim().isEmpty()) {
            return "Age cannot be empty";
        }
        try {
            int ageValue = Integer.parseInt(age.trim());
            if (ageValue < 1) return "Age must be at least 1";
            if (ageValue > 120) return "Age must be 120 or less";
            return null;
        } catch (NumberFormatException e) {
            return "Age must be a valid number";
        }
    }
    
    public static String validateNigerianPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return "Phone number cannot be empty";
        }
        
        String cleanedPhone = phone.trim().replaceAll("\\s+|-", "");
        
        if (cleanedPhone.length() < 11) {
            return "Phone number is too short";
        }
        
        if (!isValidNigerianPhone(cleanedPhone)) {
            return "Invalid Nigerian phone number format. Valid formats: 08012345678, 08123456789, +2348012345678";
        }
        
        return null;
    }
    
    public static String validateAddress(String address) {
        if (address == null || address.trim().isEmpty()) {
            return "Address cannot be empty";
        }
        if (address.trim().length() < 5) {
            return "Address must be at least 5 characters long";
        }
        if (!isValidAddress(address)) {
            return "Address contains invalid characters";
        }
        return null;
    }
    
    public static String validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return "Email cannot be empty";
        }
        if (!isValidEmail(email)) {
            return "Invalid email format";
        }
        return null;
    }
    
    // Utility method to format Nigerian phone numbers
    public static String formatNigerianPhone(String phone) {
        if (!isValidNigerianPhone(phone)) return phone;
        
        String cleanedPhone = phone.trim().replaceAll("\\s+|-", "");
        
        if (cleanedPhone.startsWith("+234")) {
            return cleanedPhone; // Already in international format
        } else if (cleanedPhone.startsWith("0")) {
            // Convert to international format
            return "+234" + cleanedPhone.substring(1);
        }
        
        return cleanedPhone;
    }

    
    // Additional validation methods with custom error messages
    public static String validateText(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "Text cannot be empty";
        }
        if (!isValidText(input)) {
            return "Text contains invalid characters";
        }
        return null; // No error
    }
    
    public static String validateID(String id) {
        if (id == null || id.trim().isEmpty()) {
            return "ID cannot be empty";
        }
        if (!isValidID(id)) {
            return "ID must be exactly 7 alphanumeric characters";
        }
        return null;
    }
    
    public static String validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            return "Password cannot be empty";
        }
        if (password.length() < 8) {
            return "Password must be at least 8 characters long";
        }
        if (!isValidPassword(password)) {
            return "Password must contain at least one letter, one number, and only allowed special characters";
        }
        return null;
    }

    
    
    public boolean saveUserCredential(String id,String pass){
        boolean stat = false;
        
       if((this.isValidID(id)) && (this.isValidPassword(pass))){
           
          try(BufferedWriter bw= new BufferedWriter(new FileWriter(this.users_login_file))){
              String row = String.format("%s,%s",id,pass);
              bw.write(this.crypter.encryptEncode(row));
              bw.write("\n");
              bw.flush(); bw.close();
              stat = true;
          } catch(IOException ioe){
              ioe.printStackTrace();
          };
        } 
        return stat;
    }
    
    private void loadDesignationList(){
        
        try(BufferedReader br = new BufferedReader(new FileReader(this.designations_file))){
            String line = "";
            
            String id, scope;
            String[] permits = null, dta;
            
            Permission p = null;
            
            while((line = br.readLine()) != null){
                if(!line.trim().isEmpty()){
                    dta = line.split(",");
                    id = dta[0];
                    scope = dta[1];
                    permits = new String[dta.length-2];
                    
                    System.arraycopy(dta,2,permits,0,dta.length);
                    
                    if(this.designations.containsKey(id)){
                        p = this.designations.get(id);
                        p.setPermission(scope,permits);
                        this.designations.put(id,p);
                    }else if(!this.designations.containsKey(id)){
                        p = new Permission(id,scope,permits);
                        this.designations.put(id,p);
                    }
                }
            } br.close();
        }catch(IOException ioe){
         ioe.printStackTrace();   
        }
    }
    
    
    public boolean validateAdminUserCredentials(String username, String pass){
        boolean stat = false;
        
        try(BufferedReader br = new BufferedReader(new FileReader(this.users_login_file))){
            String line, row;
            String[] dta;
            String l_id, l_user, l_pass, l_type;
            
            while((line = br.readLine()) != null){
                if(!line.trim().isEmpty()){
                    //decode-decrypt
                    row = this.crypter.decodeDecrypt(line);
                    
                    dta = row.split(",");
                    l_id = dta[0];
                    l_user = dta[1];
                    l_pass = dta[2];
                    l_type = dta[3];
                    
                    if((l_user.equals(username)) && (l_pass.equals(pass))){
                        if(l_type.equals("ADMIN")){
                            stat = true;
                            break;
                        }
                    } l_id = l_pass = "NONE";
                }
            }br.close();
        }catch(IOException ioe){
            ioe.printStackTrace();
        }
        
        return stat;
    }
    
}



/*
public class Main {
    public static void main(String[] args) {
        // Test examples
        String normalText = "Hello, World!";
        String id = "ABC1234";
        String password = "Secure123!";
        
        System.out.println("Normal text valid: " + InputValidator.isValidNormalText(normalText));
        System.out.println("ID valid: " + InputValidator.isValidId(id));
        System.out.println("Password valid: " + InputValidator.isValidPassword(password));
        
        // Get validation messages
        System.out.println(InputValidator.validateNormalText(normalText));
        System.out.println(InputValidator.validateId(id));
        System.out.println(InputValidator.validatePassword(password));
    }
}
*/