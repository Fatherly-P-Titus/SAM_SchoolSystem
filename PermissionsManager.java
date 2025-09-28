package SchoolManager;

import java.util.*;
import java.io.*;
import java.nio.file.*;

// Main Permissions manager class
public class PermissionsManager {
    private static final String PERMISSIONS_DIR = "system_data/permissions";
    private static final String PERMISSIONS_FILE = "user_permissions.dat";
    
    private final String permissionsFilePath;
    private final Map<String, UserPermissions> userPermissionsMap;
    private final Map<String, UserRole> availableRoles;
    
    public PermissionsManager() {
        this(PERMISSIONS_DIR + File.separator + PERMISSIONS_FILE);
    }
    
    public PermissionsManager(String filePath) {
        this.permissionsFilePath = filePath;
        this.userPermissionsMap = new HashMap<>();
        this.availableRoles = new HashMap<>();
        initializeDefaultRoles();
        ensurePermissionsDirectory();
        loadAllPermissions();
    }
    
    private void initializeDefaultRoles() {
        // Create default roles
        UserRole[] defaultRoles = {
            UserRole.createAdministratorRole(),
            UserRole.createTeacherRole(),
            UserRole.createAccountantRole()
        };
        
        for (UserRole role : defaultRoles) {
            availableRoles.put(role.getRoleName().toLowerCase(), role);
        }
    }
    
    private void ensurePermissionsDirectory() {
        File dir = new File(PERMISSIONS_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }
    
    // UserPermissions management
    public UserPermissions getUserPermissions(String userId) {
        return userPermissionsMap.get(userId);
    }
    
    public UserPermissions createUserPermissions(String userId, UserRole role) {
        UserPermissions permissions = new UserPermissions(userId, role);
        userPermissionsMap.put(userId, permissions);
        saveAllPermissions();
        return permissions;
    }
    
    public void updateUserRole(String userId, UserRole newRole) {
        UserPermissions permissions = userPermissionsMap.get(userId);
        if (permissions != null) {
            permissions.setRole(newRole);
            saveAllPermissions();
        }
    }
    
    public boolean removeUserPermissions(String userId) {
        boolean removed = userPermissionsMap.remove(userId) != null;
        if (removed) {
            saveAllPermissions();
        }
        return removed;
    }
    
    // Role management
    public void addCustomRole(UserRole role) {
        availableRoles.put(role.getRoleName().toLowerCase(), role);
    }
    
    public UserRole getRole(String roleName) {
        return availableRoles.get(roleName.toLowerCase());
    }
    
    public Collection<UserRole> getAvailableRoles() {
        return Collections.unmodifiableCollection(availableRoles.values());
    }
    
    // Permission checking
    public boolean hasPermission(String userId, Permission permission) {
        UserPermissions userPerms = userPermissionsMap.get(userId);
        return userPerms != null && userPerms.hasPermission(permission);
    }
    
    public boolean canAccessView(String userId, String view) {
        UserPermissions userPerms = userPermissionsMap.get(userId);
        return userPerms != null && userPerms.canAccessView(view);
    }
    
    // File operations
    private void loadAllPermissions() {
        File file = new File(permissionsFilePath);
        if (!file.exists()) {
            System.out.println("No existing permissions file found. Starting with empty permissions.");
            return;
        }
        
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            @SuppressWarnings("unchecked")
            Map<String, UserPermissions> loadedMap = (Map<String, UserPermissions>) ois.readObject();
            userPermissionsMap.putAll(loadedMap);
            System.out.println("Loaded permissions for " + userPermissionsMap.size() + " users");
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error loading permissions: " + e.getMessage());
            // Start with empty permissions if loading fails
        }
    }
    
    public void saveAllPermissions() {
        try {
            Path filePath = Paths.get(permissionsFilePath);
            Files.createDirectories(filePath.getParent());
            
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath))) {
                oos.writeObject(userPermissionsMap);
            }
            System.out.println("Permissions saved successfully for " + userPermissionsMap.size() + " users");
        } catch (IOException e) {
            System.err.println("Error saving permissions: " + e.getMessage());
            throw new RuntimeException("Failed to save permissions", e);
        }
    }
    
    // Utility methods
    public int getUserCount() {
        return userPermissionsMap.size();
    }
    
    public Set<String> getAllUserIds() {
        return Collections.unmodifiableSet(userPermissionsMap.keySet());
    }
    
    public String getPermissionsSummary() {
        return String.format("Permissions Manager: %d users, %d available roles", 
                           userPermissionsMap.size(), availableRoles.size());
    }
}


/*
Usage Example

package SchoolManager;

public class PermissionsExample {
    public static void main(String[] args) {
        // Initialize permissions manager
        PermissionsManager permissionsManager = new PermissionsManager();
        
        // Create user permissions
        UserRole teacherRole = permissionsManager.getRole("teacher");
        UserPermissions teacherPerms = permissionsManager.createUserPermissions("user123", teacherRole);
        
        // Check permissions
        boolean canUpdateScores = permissionsManager.hasPermission("user123", Permission.UPDATE_SCORES);
        boolean canAccessStudents = permissionsManager.canAccessView("user123", "student_management");
        
        System.out.println("Can update scores: " + canUpdateScores);
        System.out.println("Can access student management: " + canAccessStudents);
        
        // Print summary
        System.out.println(permissionsManager.getPermissionsSummary());
    }
}
*/