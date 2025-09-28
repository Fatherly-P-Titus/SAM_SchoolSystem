package SchoolManager;

import java.util.*;
import java.io.*;
import java.nio.file.*;

// Permission Enums for type safety
public enum Permission {
    // Create permissions
    CREATE_STUDENT("can_create_student"),
    CREATE_STAFF("can_create_staff"),
    CREATE_USER("can_create_user"),
    CREATE_EXPENSE("can_create_expense"),
    CREATE_INVENTORY("can_create_inventory"),
    CREATE_SCORES("can_create_scores"),
    CREATE_SCHOOL_FEES("can_create_school_fees"),
    
    // Read permissions
    READ_STUDENT("can_read_student"),
    READ_STAFF("can_read_staff"),
    READ_USER("can_read_user"),
    READ_EXPENSE("can_read_expense"),
    READ_INVENTORY("can_read_inventory"),
    READ_SCORES("can_read_scores"),
    READ_SCHOOL_FEES("can_read_school_fees"),
    VIEW_SYSTEM("can_view_system"),
    
    // Update permissions
    UPDATE_STUDENT("can_update_student"),
    UPDATE_STAFF("can_update_staff"),
    UPDATE_USER("can_update_user"),
    UPDATE_EXPENSE("can_update_expense"),
    UPDATE_INVENTORY("can_update_inventory"),
    UPDATE_SCORES("can_update_scores"),
    UPDATE_SCHOOL_FEES("can_update_school_fees"),
    
    // Delete permissions
    DELETE_STUDENT("can_delete_student"),
    DELETE_STAFF("can_delete_staff"),
    DELETE_USER("can_delete_user"),
    DELETE_EXPENSE("can_delete_expense"),
    DELETE_INVENTORY("can_delete_inventory"),
    DELETE_SCORES("can_delete_scores"),
    DELETE_SCHOOL_FEES("can_delete_school_fees");
    
    private final String permissionString;
    
    Permission(String permissionString) {
        this.permissionString = permissionString;
    }
    
    public String getPermissionString() {
        return permissionString;
    }
    
    public static Permission fromString(String text) {
        for (Permission permission : Permission.values()) {
            if (permission.permissionString.equalsIgnoreCase(text)) {
                return permission;
            }
        }
        throw new IllegalArgumentException("No permission found for: " + text);
    }
}