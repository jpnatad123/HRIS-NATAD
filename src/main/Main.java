package Main;

import java.sql.*;
import java.util.*;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

// Simple dbConnect class inside Main package
class dbConnect {
    public static Connection connectDB() {
        Connection con = null;
        try {
            Class.forName("org.sqlite.JDBC");
            con = DriverManager.getConnection("jdbc:sqlite:HRIS.db");
        } catch (Exception e) {
            System.out.println("Connection Failed: " + e);
        }
        return con;
    }

    public void addRecord(String sql, Object... values) {
        try (Connection conn = dbConnect.connectDB();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) {
                pstmt.setObject(i + 1, values[i]);
            }
            pstmt.executeUpdate();
            System.out.println("Record added successfully!");
        } catch (SQLException e) {
            System.out.println("Error adding record: " + e.getMessage());
        }
    }

    public void viewRecords(String sqlQuery, String[] columnHeaders, String[] columnNames) {
        if (columnHeaders.length != columnNames.length) {
            System.out.println("Error: Mismatch between column headers and column names.");
            return;
        }
        try (Connection conn = dbConnect.connectDB();
             PreparedStatement pstmt = conn.prepareStatement(sqlQuery);
             ResultSet rs = pstmt.executeQuery()) {
            StringBuilder headerLine = new StringBuilder();
            headerLine.append("--------------------------------------------------------------------------------\n| ");
            for (String header : columnHeaders) {
                headerLine.append(String.format("%-20s | ", header));
            }
            headerLine.append("\n--------------------------------------------------------------------------------");
            System.out.println(headerLine.toString());
            while (rs.next()) {
                StringBuilder row = new StringBuilder("| ");
                for (String colName : columnNames) {
                    String value = rs.getString(colName);
                    row.append(String.format("%-20s | ", value != null ? value : ""));
                }
                System.out.println(row.toString());
            }
            System.out.println("--------------------------------------------------------------------------------");
        } catch (SQLException e) {
            System.out.println("Error retrieving records: " + e.getMessage());
        }
    }

    public void updateRecord(String sql, Object... values) {
        try (Connection conn = dbConnect.connectDB();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) {
                pstmt.setObject(i + 1, values[i]);
            }
            pstmt.executeUpdate();
            System.out.println("Record updated successfully!");
        } catch (SQLException e) {
            System.out.println("Error updating record: " + e.getMessage());
        }
    }

    public void deleteRecord(String sql, Object... values) {
        try (Connection conn = dbConnect.connectDB();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) {
                if (values[i] instanceof Integer) {
                    pstmt.setInt(i + 1, (Integer) values[i]);
                } else {
                    pstmt.setString(i + 1, values[i].toString());
                }
            }
            pstmt.executeUpdate();
            System.out.println("Record deleted successfully!");
        } catch (SQLException e) {
            System.out.println("Error deleting record: " + e.getMessage());
        }
    }

    public List<Map<String, Object>> fetchRecords(String sqlQuery, Object... values) {
        List<Map<String, Object>> records = new ArrayList<>();
        try (Connection conn = dbConnect.connectDB();
             PreparedStatement pstmt = conn.prepareStatement(sqlQuery)) {
            for (int i = 0; i < values.length; i++) {
                pstmt.setObject(i + 1, values[i]);
            }
            ResultSet rs = pstmt.executeQuery();
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.put(metaData.getColumnName(i), rs.getObject(i));
                }
                records.add(row);
            }
        } catch (SQLException e) {
            System.out.println("Error fetching records: " + e.getMessage());
        }
        return records;
    }

    public String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = md.digest(password.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashedBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            System.out.println("Error hashing password: " + e.getMessage());
            return null;
        }
    }
}

// Your Main class
public class Main {
    public static dbConnect con = new dbConnect();

    // ====== CUSTOM DISPLAY FOR DEPARTMENTS ======
    public static void displayDepartments() {
        String sql = "SELECT department_id, dept_name FROM Department ORDER BY department_id";
        List<Map<String, Object>> departments = con.fetchRecords(sql);
        
        if (departments.isEmpty()) {
            System.out.println("No departments found.");
            return;
        }
        
        System.out.println("\n=== DEPARTMENT LIST ===");
        System.out.println("ID  | Department Name");
        System.out.println("----|-------------------");
        
        for (Map<String, Object> dept : departments) {
            int id = Integer.parseInt(dept.get("department_id").toString());
            String name = dept.get("dept_name").toString();
            System.out.printf("%-3d | %s%n", id, name);
        }
        System.out.println("-------------------");
    }

    // ====== CUSTOM DISPLAY FOR POSITIONS ======
    public static void displayPositions() {
        String sql = "SELECT position_id, position_name, status FROM Position ORDER BY position_id";
        List<Map<String, Object>> positions = con.fetchRecords(sql);
        
        if (positions.isEmpty()) {
            System.out.println("No positions found.");
            return;
        }
        
        System.out.println("\n=== POSITION LIST ===");
        System.out.println("ID  | Position Name   | Status");
        System.out.println("----|-----------------|--------");
        
        for (Map<String, Object> pos : positions) {
            int id = Integer.parseInt(pos.get("position_id").toString());
            String name = pos.get("position_name").toString();
            String status = pos.get("status") != null ? pos.get("status").toString() : "NULL";
            System.out.printf("%-3d | %-15s | %s%n", id, name, status);
        }
        System.out.println("-------------------");
    }

    // ====== ENSURE REQUIRED DEPARTMENTS AND POSITIONS ======
    public static void ensureRequiredDepartmentsAndPositions() {
        // Ensure Admin Department exists
        String checkDept = "SELECT * FROM Department WHERE dept_name = 'Admin Department'";
        List<Map<String, Object>> deptResult = con.fetchRecords(checkDept);
        if (deptResult.isEmpty()) {
            String insertDept = "INSERT INTO Department (dept_name) VALUES ('Admin Department')";
            con.addRecord(insertDept);
            System.out.println("Admin Department created");
        }
        
        // Ensure Administrator position exists with Active status
        String checkPos = "SELECT * FROM Position WHERE position_name = 'Administrator'";
        List<Map<String, Object>> posResult = con.fetchRecords(checkPos);
        if (posResult.isEmpty()) {
            String insertPos = "INSERT INTO Position (position_name, status) VALUES ('Administrator', 'Active')";
            con.addRecord(insertPos);
            System.out.println("Administrator position created");
        }
        
        // Create other common departments and positions if they don't exist
        String[] departments = {"Human Resources", "IT", "Finance", "Marketing", "Operations"};
        String[] positions = {"Manager", "Developer", "Analyst", "Specialist", "Coordinator"};
        
        for (String dept : departments) {
            String check = "SELECT * FROM Department WHERE dept_name = ?";
            List<Map<String, Object>> result = con.fetchRecords(check, dept);
            if (result.isEmpty()) {
                String insert = "INSERT INTO Department (dept_name) VALUES (?)";
                con.addRecord(insert, dept);
            }
        }
        
        for (String pos : positions) {
            String check = "SELECT * FROM Position WHERE position_name = ?";
            List<Map<String, Object>> result = con.fetchRecords(check, pos);
            if (result.isEmpty()) {
                String insert = "INSERT INTO Position (position_name, status) VALUES (?, 'Active')";
                con.addRecord(insert, pos);
            }
        }
    }

    // ====== GET DEPARTMENT ID ======
    public static int getDepartmentId(String deptName) {
        String sql = "SELECT department_id FROM Department WHERE dept_name = ?";
        List<Map<String, Object>> res = con.fetchRecords(sql, deptName);
        if (!res.isEmpty()) {
            return Integer.parseInt(res.get(0).get("department_id").toString());
        }
        return -1;
    }

    // ====== GET POSITION ID ======
    public static int getPositionId(String positionName) {
        String sql = "SELECT position_id FROM Position WHERE position_name = ? AND status = 'Active'";
        List<Map<String, Object>> res = con.fetchRecords(sql, positionName);
        if (!res.isEmpty()) {
            return Integer.parseInt(res.get(0).get("position_id").toString());
        }
        return -1;
    }

    // ====== ENSURE SUPER ADMIN ACCOUNT ======
    public static void ensureSuperAdmin() {
        // First, ensure we have the required department and position
        ensureRequiredDepartmentsAndPositions();
        
        String checkQuery = "SELECT * FROM Employee WHERE email = ?";
        List<Map<String, Object>> result = con.fetchRecords(checkQuery, "admin@hris.com");
        String correctHash = con.hashPassword("admin123");

        if (result.isEmpty()) {
            // Get the actual IDs for Admin Department and Administrator position
            int adminDeptId = getDepartmentId("Admin Department");
            int adminPosId = getPositionId("Administrator");
            
            String insert = "INSERT INTO Employee(first_name, last_name, email, password, department_id, position_id, employment_status) VALUES (?, ?, ?, ?, ?, ?, 'Active')";
            con.addRecord(insert, "Super", "Admin", "admin@hris.com", correctHash, adminDeptId, adminPosId);
            System.out.println("Super Admin account created (admin@hris.com / admin123)");
        } else {
            String update = "UPDATE Employee SET password = ?, employment_status = 'Active' WHERE email = ?";
            con.updateRecord(update, correctHash, "admin@hris.com");
        }
    }

    // ====== ADMIN MENU ======
    public static void adminMenu(Scanner sc) {
        int choice;
        do {
            System.out.println("\n===== ADMIN MENU =====");
            System.out.println("1. View All Employees with Full Payroll & Position");
            System.out.println("2. Approve Pending Employee");
            System.out.println("3. Update Employee Status");
            System.out.println("4. Delete/Deactivate Employee");
            System.out.println("5. Manage Payroll");
            System.out.println("6. Manage Positions");
            System.out.println("7. Manage Departments");
            System.out.println("8. Manage Leave Requests");
            System.out.println("9. Logout");
            System.out.print("Enter choice: ");
            choice = sc.nextInt();
            sc.nextLine(); // consume newline

            switch (choice) {
                case 1:
                    String adminView = "SELECT e.employee_id, e.first_name, e.last_name, e.email, d.dept_name, pos.position_name, e.employment_status, " +
                                       "p.payroll_id, p.basic_salary, p.bonuses, p.net_salary, p.payroll_date " +
                                       "FROM Employee e " +
                                       "LEFT JOIN Department d ON e.department_id = d.department_id " +
                                       "LEFT JOIN Position pos ON e.position_id = pos.position_id " +
                                       "LEFT JOIN Payroll p ON e.employee_id = p.employee_id " +
                                       "ORDER BY e.employee_id, p.payroll_date DESC";
                    String[] headers = {"ID", "First Name", "Last Name", "Email", "Department", "Position", "Status", "Payroll ID", "Basic", "Bonus", "Net", "Date"};
                    String[] cols = {"employee_id", "first_name", "last_name", "email", "dept_name", "position_name", "employment_status", "payroll_id", "basic_salary", "bonuses", "net_salary", "payroll_date"};
                    con.viewRecords(adminView, headers, cols);
                    break;

                case 2:
                    System.out.print("Enter Employee ID to Approve: ");
                    int approveId = sc.nextInt();
                    String approveSql = "UPDATE Employee SET employment_status = 'Approved' WHERE employee_id = ?";
                    con.updateRecord(approveSql, approveId);
                    System.out.println("Employee approved successfully!");
                    break;

                case 3:
                    System.out.print("Enter Employee ID to Update: ");
                    int updateId = sc.nextInt();
                    sc.nextLine();
                    System.out.print("Enter New Status (Approved/Active/Inactive): ");
                    String newStatus = sc.nextLine();
                    String updateSql = "UPDATE Employee SET employment_status = ? WHERE employee_id = ?";
                    con.updateRecord(updateSql, newStatus, updateId);
                    System.out.println("Employee status updated successfully!");
                    break;

                case 4:
                    System.out.println("\n===== EMPLOYEE MANAGEMENT =====");
                    System.out.println("1. Deactivate Employee (Set to Inactive)");
                    System.out.println("2. Permanently Delete Employee");
                    System.out.print("Enter choice: ");
                    int deleteChoice = sc.nextInt();
                    sc.nextLine();
                    
                    if (deleteChoice == 1) {
                        // Deactivate employee (safer approach)
                        System.out.print("Enter Employee ID to Deactivate: ");
                        int deactivateId = sc.nextInt();
                        
                        // Check if employee exists
                        String checkEmp = "SELECT * FROM Employee WHERE employee_id = ?";
                        List<Map<String, Object>> empExists = con.fetchRecords(checkEmp, deactivateId);
                        if (empExists.isEmpty()) {
                            System.out.println("Error: Employee ID " + deactivateId + " does not exist!");
                            break;
                        }
                        
                        String empName = empExists.get(0).get("first_name") + " " + empExists.get(0).get("last_name");
                        
                        String deactivateSql = "UPDATE Employee SET employment_status = 'Inactive' WHERE employee_id = ?";
                        con.updateRecord(deactivateSql, deactivateId);
                        System.out.println("Employee " + empName + " deactivated successfully!");
                        
                    } else if (deleteChoice == 2) {
                        // Permanent deletion
                        System.out.print("Enter Employee ID to Delete: ");
                        int delId = sc.nextInt();
                        sc.nextLine();
                        
                        // Check if employee exists
                        String checkEmp = "SELECT * FROM Employee WHERE employee_id = ?";
                        List<Map<String, Object>> empExists = con.fetchRecords(checkEmp, delId);
                        if (empExists.isEmpty()) {
                            System.out.println("Error: Employee ID " + delId + " does not exist!");
                            break;
                        }
                        
                        String empName = empExists.get(0).get("first_name") + " " + empExists.get(0).get("last_name");
                        
                        System.out.print("WARNING: This will permanently delete " + empName + " and all related records. Continue? (Y/N): ");
                        String confirm = sc.nextLine();
                        
                        if (confirm.equalsIgnoreCase("Y")) {
                            try {
                                // Manual cascade deletion
                                String deletePayroll = "DELETE FROM Payroll WHERE employee_id = ?";
                                String deleteLeave = "DELETE FROM Leave WHERE employee_id = ?";
                                String deleteEmployee = "DELETE FROM Employee WHERE employee_id = ?";
                                
                                // Delete related records first
                                con.deleteRecord(deletePayroll, delId);
                                con.deleteRecord(deleteLeave, delId);
                                // Then delete employee
                                con.deleteRecord(deleteEmployee, delId);
                                
                                System.out.println("Employee " + empName + " and all related records deleted successfully!");
                            } catch (Exception e) {
                                System.out.println("Error deleting employee: " + e.getMessage());
                                System.out.println("Try deactivating the employee instead.");
                            }
                        } else {
                            System.out.println("Deletion cancelled.");
                        }
                    } else {
                        System.out.println("Invalid choice.");
                    }
                    break;

                case 5:
                    managePayroll(sc);
                    break;

                case 6:
                    managePositions(sc);
                    break;

                case 7:
                    manageDepartments(sc);
                    break;

                case 8:
                    manageLeaveRequests(sc);
                    break;

                case 9:
                    System.out.println("Logging out...");
                    break;

                default:
                    System.out.println("Invalid choice.");
            }
        } while (choice != 9);
    }

    // ====== MANAGE DEPARTMENTS ======
    public static void manageDepartments(Scanner sc) {
        int choice;
        do {
            System.out.println("\n===== DEPARTMENT MANAGEMENT =====");
            System.out.println("1. View All Departments");
            System.out.println("2. Add New Department");
            System.out.println("3. Update Department Name");
            System.out.println("4. Delete Department");
            System.out.println("5. Back to Admin Menu");
            System.out.print("Enter choice: ");
            choice = sc.nextInt();
            sc.nextLine();

            switch (choice) {
                case 1:
                    displayDepartments();
                    break;

                case 2:
                    System.out.print("Enter New Department Name: ");
                    String newDept = sc.nextLine();
                    
                    // Check if department already exists
                    String checkSql = "SELECT * FROM Department WHERE dept_name = ?";
                    List<Map<String, Object>> existing = con.fetchRecords(checkSql, newDept);
                    if (!existing.isEmpty()) {
                        System.out.println("Department '" + newDept + "' already exists!");
                        break;
                    }
                    
                    String insertSql = "INSERT INTO Department (dept_name) VALUES (?)";
                    con.addRecord(insertSql, newDept);
                    System.out.println("Department '" + newDept + "' added successfully!");
                    break;

                case 3:
                    System.out.print("Enter Department ID to Update: ");
                    int deptId = sc.nextInt();
                    sc.nextLine();
                    
                    // Check if department exists
                    String checkDept = "SELECT * FROM Department WHERE department_id = ?";
                    List<Map<String, Object>> deptCheck = con.fetchRecords(checkDept, deptId);
                    if (deptCheck.isEmpty()) {
                        System.out.println("Error: Department ID " + deptId + " does not exist!");
                        break;
                    }
                    
                    String currentName = deptCheck.get(0).get("dept_name").toString();
                    System.out.println("Current department name: " + currentName);
                    System.out.print("Enter New Department Name: ");
                    String newDeptName = sc.nextLine();
                    
                    String updateSql = "UPDATE Department SET dept_name = ? WHERE department_id = ?";
                    con.updateRecord(updateSql, newDeptName, deptId);
                    System.out.println("Department name updated successfully!");
                    break;

                case 4:
                    System.out.print("Enter Department ID to Delete: ");
                    int delDeptId = sc.nextInt();
                    sc.nextLine();
                    
                    // Check if department exists
                    String checkDelDept = "SELECT * FROM Department WHERE department_id = ?";
                    List<Map<String, Object>> delDeptCheck = con.fetchRecords(checkDelDept, delDeptId);
                    if (delDeptCheck.isEmpty()) {
                        System.out.println("Error: Department ID " + delDeptId + " does not exist!");
                        break;
                    }
                    
                    String deptName = delDeptCheck.get(0).get("dept_name").toString();
                    
                    // Check if department has employees
                    String checkEmployees = "SELECT COUNT(*) as emp_count FROM Employee WHERE department_id = ?";
                    List<Map<String, Object>> empCount = con.fetchRecords(checkEmployees, delDeptId);
                    int employeeCount = Integer.parseInt(empCount.get(0).get("emp_count").toString());
                    
                    if (employeeCount > 0) {
                        System.out.println("Cannot delete department '" + deptName + "'! It has " + employeeCount + " employees assigned.");
                        System.out.println("Please reassign or remove all employees from this department first.");
                        break;
                    }
                    
                    System.out.print("Are you sure you want to delete department '" + deptName + "'? (Y/N): ");
                    String confirm = sc.nextLine();
                    
                    if (confirm.equalsIgnoreCase("Y")) {
                        String deleteSql = "DELETE FROM Department WHERE department_id = ?";
                        con.deleteRecord(deleteSql, delDeptId);
                        System.out.println("Department '" + deptName + "' deleted successfully!");
                    } else {
                        System.out.println("Deletion cancelled.");
                    }
                    break;

                case 5:
                    System.out.println("Returning to Admin Menu...");
                    break;

                default:
                    System.out.println("Invalid choice.");
            }

        } while (choice != 5);
    }

    // ====== MANAGE LEAVE REQUESTS ======
    public static void manageLeaveRequests(Scanner sc) {
        int choice;
        do {
            System.out.println("\n===== LEAVE REQUEST MANAGEMENT =====");
            System.out.println("1. View All Leave Requests");
            System.out.println("2. Approve Leave Request");
            System.out.println("3. Reject Leave Request");
            System.out.println("4. View Approved Leaves");
            System.out.println("5. View Rejected Leaves");
            System.out.println("6. Back to Admin Menu");
            System.out.print("Enter choice: ");
            choice = sc.nextInt();
            sc.nextLine();

            switch (choice) {
                case 1:
                    // View all pending leave requests with employee names
                    String pendingSql = "SELECT l.leave_id, l.employee_id, e.first_name, e.last_name, l.leave_type, l.start_date, l.end_date, l.status " +
                                       "FROM Leave l " +
                                       "JOIN Employee e ON l.employee_id = e.employee_id " +
                                       "WHERE l.status = 'Pending' " +
                                       "ORDER BY l.leave_id";
                    String[] pendingHeaders = {"Leave ID", "Emp ID", "First Name", "Last Name", "Leave Type", "Start Date", "End Date", "Status"};
                    String[] pendingCols = {"leave_id", "employee_id", "first_name", "last_name", "leave_type", "start_date", "end_date", "status"};
                    System.out.println("\n=== PENDING LEAVE REQUESTS ===");
                    con.viewRecords(pendingSql, pendingHeaders, pendingCols);
                    break;

                case 2:
                    // Approve leave request
                    System.out.print("Enter Leave ID to Approve: ");
                    int approveLeaveId = sc.nextInt();
                    
                    // Check if leave request exists and is pending
                    String checkLeave = "SELECT * FROM Leave WHERE leave_id = ? AND status = 'Pending'";
                    List<Map<String, Object>> leaveCheck = con.fetchRecords(checkLeave, approveLeaveId);
                    if (leaveCheck.isEmpty()) {
                        System.out.println("Error: Leave request not found or already processed!");
                        break;
                    }
                    
                    String approveSql = "UPDATE Leave SET status = 'Approved' WHERE leave_id = ?";
                    con.updateRecord(approveSql, approveLeaveId);
                    
                    // Get employee details for notification
                    String empSql = "SELECT e.first_name, e.last_name, e.email FROM Leave l " +
                                   "JOIN Employee e ON l.employee_id = e.employee_id " +
                                   "WHERE l.leave_id = ?";
                    List<Map<String, Object>> empInfo = con.fetchRecords(empSql, approveLeaveId);
                    if (!empInfo.isEmpty()) {
                        String firstName = empInfo.get(0).get("first_name").toString();
                        String lastName = empInfo.get(0).get("last_name").toString();
                        System.out.println("Leave request approved for " + firstName + " " + lastName + "!");
                    } else {
                        System.out.println("Leave request approved!");
                    }
                    break;

                case 3:
                    // Reject leave request
                    System.out.print("Enter Leave ID to Reject: ");
                    int rejectLeaveId = sc.nextInt();
                    sc.nextLine(); // consume newline
                    
                    // Check if leave request exists and is pending
                    String checkReject = "SELECT * FROM Leave WHERE leave_id = ? AND status = 'Pending'";
                    List<Map<String, Object>> rejectCheck = con.fetchRecords(checkReject, rejectLeaveId);
                    if (rejectCheck.isEmpty()) {
                        System.out.println("Error: Leave request not found or already processed!");
                        break;
                    }
                    
                    System.out.print("Enter rejection reason: ");
                    String rejectionReason = sc.nextLine();
                    
                    String rejectSql = "UPDATE Leave SET status = 'Rejected', rejection_reason = ? WHERE leave_id = ?";
                    con.updateRecord(rejectSql, rejectionReason, rejectLeaveId);
                    
                    // Get employee details for notification
                    String empRejectSql = "SELECT e.first_name, e.last_name FROM Leave l " +
                                         "JOIN Employee e ON l.employee_id = e.employee_id " +
                                         "WHERE l.leave_id = ?";
                    List<Map<String, Object>> empRejectInfo = con.fetchRecords(empRejectSql, rejectLeaveId);
                    if (!empRejectInfo.isEmpty()) {
                        String firstName = empRejectInfo.get(0).get("first_name").toString();
                        String lastName = empRejectInfo.get(0).get("last_name").toString();
                        System.out.println("Leave request rejected for " + firstName + " " + lastName + "!");
                    } else {
                        System.out.println("Leave request rejected!");
                    }
                    break;

                case 4:
                    // View approved leaves
                    String approvedSql = "SELECT l.leave_id, l.employee_id, e.first_name, e.last_name, l.leave_type, l.start_date, l.end_date, l.status " +
                                        "FROM Leave l " +
                                        "JOIN Employee e ON l.employee_id = e.employee_id " +
                                        "WHERE l.status = 'Approved' " +
                                        "ORDER BY l.start_date DESC";
                    String[] approvedHeaders = {"Leave ID", "Emp ID", "First Name", "Last Name", "Leave Type", "Start Date", "End Date", "Status"};
                    String[] approvedCols = {"leave_id", "employee_id", "first_name", "last_name", "leave_type", "start_date", "end_date", "status"};
                    System.out.println("\n=== APPROVED LEAVES ===");
                    con.viewRecords(approvedSql, approvedHeaders, approvedCols);
                    break;

                case 5:
                    // View rejected leaves
                    String rejectedSql = "SELECT l.leave_id, l.employee_id, e.first_name, e.last_name, l.leave_type, l.start_date, l.end_date, l.status, l.rejection_reason " +
                                        "FROM Leave l " +
                                        "JOIN Employee e ON l.employee_id = e.employee_id " +
                                        "WHERE l.status = 'Rejected' " +
                                        "ORDER BY l.start_date DESC";
                    String[] rejectedHeaders = {"Leave ID", "Emp ID", "First Name", "Last Name", "Leave Type", "Start Date", "End Date", "Status", "Reason"};
                    String[] rejectedCols = {"leave_id", "employee_id", "first_name", "last_name", "leave_type", "start_date", "end_date", "status", "rejection_reason"};
                    System.out.println("\n=== REJECTED LEAVES ===");
                    con.viewRecords(rejectedSql, rejectedHeaders, rejectedCols);
                    break;

                case 6:
                    System.out.println("Returning to Admin Menu...");
                    break;

                default:
                    System.out.println("Invalid choice.");
            }

        } while (choice != 6);
    }

    // ====== MANAGE POSITIONS ======
    public static void managePositions(Scanner sc) {
        int choice;
        do {
            System.out.println("\n===== POSITION MANAGEMENT =====");
            System.out.println("1. View All Positions");
            System.out.println("2. Add New Position");
            System.out.println("3. Update Position Status");
            System.out.println("4. Back to Admin Menu");
            System.out.print("Enter choice: ");
            choice = sc.nextInt();
            sc.nextLine();

            switch (choice) {
                case 1:
                    displayPositions();
                    break;

                case 2:
                    System.out.print("Enter New Position Name: ");
                    String newPosition = sc.nextLine();
                    
                    // Check if position already exists
                    String checkSql = "SELECT * FROM Position WHERE position_name = ?";
                    List<Map<String, Object>> existing = con.fetchRecords(checkSql, newPosition);
                    if (!existing.isEmpty()) {
                        System.out.println("Position '" + newPosition + "' already exists!");
                        break;
                    }
                    
                    System.out.print("Enter Status (Active/Inactive): ");
                    String status = sc.nextLine();
                    
                    String insertSql = "INSERT INTO Position (position_name, status) VALUES (?, ?)";
                    con.addRecord(insertSql, newPosition, status);
                    System.out.println("Position '" + newPosition + "' added successfully!");
                    break;

                case 3:
                    System.out.print("Enter Position ID to Update: ");
                    int posId = sc.nextInt();
                    sc.nextLine();
                    
                    // Check if position exists
                    String checkPos = "SELECT * FROM Position WHERE position_id = ?";
                    List<Map<String, Object>> posCheck = con.fetchRecords(checkPos, posId);
                    if (posCheck.isEmpty()) {
                        System.out.println("Error: Position ID " + posId + " does not exist!");
                        break;
                    }
                    
                    System.out.print("Enter New Status (Active/Inactive): ");
                    String newStatus = sc.nextLine();
                    
                    String updateSql = "UPDATE Position SET status = ? WHERE position_id = ?";
                    con.updateRecord(updateSql, newStatus, posId);
                    System.out.println("Position status updated successfully!");
                    break;

                case 4:
                    System.out.println("Returning to Admin Menu...");
                    break;

                default:
                    System.out.println("Invalid choice.");
            }

        } while (choice != 4);
    }

    // ====== PAYROLL MANAGEMENT ======
    public static void managePayroll(Scanner sc) {
        int choice;
        do {
            System.out.println("\n===== PAYROLL MANAGEMENT =====");
            System.out.println("1. Add Payroll Record");
            System.out.println("2. Update Payroll Record");
            System.out.println("3. Delete Payroll Record");
            System.out.println("4. Back to Admin Menu");
            System.out.print("Enter choice: ");
            choice = sc.nextInt();
            sc.nextLine();

            switch (choice) {
                case 1:
                    System.out.print("Enter Employee ID: ");
                    int empId = sc.nextInt();
                    sc.nextLine();
                    
                    // Check if employee exists
                    String checkEmp = "SELECT * FROM Employee WHERE employee_id = ?";
                    List<Map<String, Object>> empCheck = con.fetchRecords(checkEmp, empId);
                    if (empCheck.isEmpty()) {
                        System.out.println("Error: Employee ID " + empId + " does not exist!");
                        break;
                    }
                    
                    System.out.print("Enter Basic Salary: ");
                    String basic = sc.nextLine();
                    System.out.print("Enter Bonuses: ");
                    String bonus = sc.nextLine();
                    
                    // Calculate net salary
                    try {
                        double basicNum = Double.parseDouble(basic);
                        double bonusNum = Double.parseDouble(bonus);
                        double net = basicNum + bonusNum;
                        String netStr = String.valueOf(net);
                        
                        System.out.print("Enter Payroll Date (YYYY-MM-DD): ");
                        String date = sc.nextLine();
                        
                        String addSql = "INSERT INTO Payroll(employee_id, basic_salary, bonuses, net_salary, payroll_date) VALUES (?, ?, ?, ?, ?)";
                        con.addRecord(addSql, empId, basic, bonus, netStr, date);
                        System.out.println("Payroll added successfully!");
                        
                    } catch (NumberFormatException e) {
                        System.out.println("Error: Please enter valid numbers for salary and bonuses!");
                    }
                    break;

                case 2:
                    System.out.print("Enter Payroll ID to Update: ");
                    int payId = sc.nextInt();
                    sc.nextLine();
                    
                    // Check if payroll record exists
                    String checkPay = "SELECT * FROM Payroll WHERE payroll_id = ?";
                    List<Map<String, Object>> payCheck = con.fetchRecords(checkPay, payId);
                    if (payCheck.isEmpty()) {
                        System.out.println("Error: Payroll ID " + payId + " does not exist!");
                        break;
                    }
                    
                    System.out.print("Enter Basic Salary: ");
                    String newBasic = sc.nextLine();
                    System.out.print("Enter Bonuses: ");
                    String newBonus = sc.nextLine();
                    
                    try {
                        double newBasicNum = Double.parseDouble(newBasic);
                        double newBonusNum = Double.parseDouble(newBonus);
                        double newNet = newBasicNum + newBonusNum;
                        String newNetStr = String.valueOf(newNet);
                        
                        String updateSql = "UPDATE Payroll SET basic_salary = ?, bonuses = ?, net_salary = ? WHERE payroll_id = ?";
                        con.updateRecord(updateSql, newBasic, newBonus, newNetStr, payId);
                        System.out.println("Payroll updated successfully!");
                    } catch (NumberFormatException e) {
                        System.out.println("Error: Please enter valid numbers for salary and bonuses!");
                    }
                    break;

                case 3:
                    System.out.print("Enter Payroll ID to Delete: ");
                    int delPay = sc.nextInt();
                    String delSql = "DELETE FROM Payroll WHERE payroll_id = ?";
                    con.deleteRecord(delSql, delPay);
                    System.out.println("Payroll record deleted.");
                    break;

                case 4:
                    System.out.println("Returning to Admin Menu...");
                    break;

                default:
                    System.out.println("Invalid choice.");
            }

        } while (choice != 4);
    }

    // ====== EMPLOYEE MENU ======
    public static void employeeMenu(Scanner sc, String email) {
        int choice;
        do {
            System.out.println("\n===== EMPLOYEE MENU =====");
            System.out.println("1. View My Profile & Position");
            System.out.println("2. Apply for Leave");
            System.out.println("3. View My Payroll Records");
            System.out.println("4. View My Leave Status");
            System.out.println("5. Logout");
            System.out.print("Enter choice: ");
            choice = sc.nextInt();
            sc.nextLine(); // consume newline

            switch (choice) {
                case 1:
                    String profileSql = "SELECT e.employee_id, e.first_name, e.last_name, e.email, d.dept_name, pos.position_name, e.employment_status " +
                                        "FROM Employee e " +
                                        "LEFT JOIN Department d ON e.department_id = d.department_id " +
                                        "LEFT JOIN Position pos ON e.position_id = pos.position_id " +
                                        "WHERE e.email = ?";
                    List<Map<String, Object>> profileData = con.fetchRecords(profileSql, email);
                    if (!profileData.isEmpty()) {
                        String[] headers = {"ID", "First Name", "Last Name", "Email", "Department", "Position", "Status"};
                        // Convert the result to display
                        System.out.println("--------------------------------------------------------------------------------");
                        System.out.println("| " + String.format("%-20s | %-20s | %-20s | %-20s | %-20s | %-20s | %-20s |", 
                            headers[0], headers[1], headers[2], headers[3], headers[4], headers[5], headers[6]));
                        System.out.println("--------------------------------------------------------------------------------");
                        Map<String, Object> row = profileData.get(0);
                        System.out.println("| " + String.format("%-20s | %-20s | %-20s | %-20s | %-20s | %-20s | %-20s |", 
                            row.get("employee_id"), row.get("first_name"), row.get("last_name"), 
                            row.get("email"), row.get("dept_name"), row.get("position_name"), 
                            row.get("employment_status")));
                        System.out.println("--------------------------------------------------------------------------------");
                    } else {
                        System.out.println("No profile data found.");
                    }
                    break;

                case 2:
                    System.out.print("Enter Leave Type (e.g. Vacation, Sick): ");
                    String leaveType = sc.nextLine();
                    System.out.print("Enter Start Date (YYYY-MM-DD): ");
                    String startDate = sc.nextLine();
                    System.out.print("Enter End Date (YYYY-MM-DD): ");
                    String endDate = sc.nextLine();
                    int empId = getEmployeeIdByEmail(email);
                    if (empId != -1) {
                        String leaveSql = "INSERT INTO Leave(employee_id, leave_type, start_date, end_date, status) VALUES (?, ?, ?, ?, 'Pending')";
                        con.addRecord(leaveSql, empId, leaveType, startDate, endDate);
                        System.out.println("Leave request submitted successfully!");
                    } else {
                        System.out.println("Error: Employee not found.");
                    }
                    break;

                case 3:
                    int empId2 = getEmployeeIdByEmail(email);
                    if (empId2 != -1) {
                        String paySql = "SELECT p.payroll_id, pos.position_name, p.basic_salary, p.bonuses, p.net_salary, p.payroll_date " +
                                        "FROM Payroll p " +
                                        "INNER JOIN Employee e ON p.employee_id = e.employee_id " +
                                        "INNER JOIN Position pos ON e.position_id = pos.position_id " +
                                        "WHERE p.employee_id = ? " +
                                        "ORDER BY p.payroll_date DESC";
                        List<Map<String, Object>> payrollData = con.fetchRecords(paySql, empId2);
                        if (!payrollData.isEmpty()) {
                            String[] headers = {"Payroll ID", "Position", "Basic", "Bonus", "Net", "Date"};
                            System.out.println("--------------------------------------------------------------------------------");
                            System.out.println("| " + String.format("%-20s | %-20s | %-20s | %-20s | %-20s | %-20s |", 
                                headers[0], headers[1], headers[2], headers[3], headers[4], headers[5]));
                            System.out.println("--------------------------------------------------------------------------------");
                            for (Map<String, Object> row : payrollData) {
                                System.out.println("| " + String.format("%-20s | %-20s | %-20s | %-20s | %-20s | %-20s |", 
                                    row.get("payroll_id"), row.get("position_name"), row.get("basic_salary"), 
                                    row.get("bonuses"), row.get("net_salary"), row.get("payroll_date")));
                            }
                            System.out.println("--------------------------------------------------------------------------------");
                        } else {
                            System.out.println("No payroll records found.");
                        }
                    } else {
                        System.out.println("Error: Employee not found.");
                    }
                    break;

                case 4:
                    // View leave status for employee
                    int empId3 = getEmployeeIdByEmail(email);
                    if (empId3 != -1) {
                        String leaveStatusSql = "SELECT leave_id, leave_type, start_date, end_date, status, rejection_reason " +
                                              "FROM Leave " +
                                              "WHERE employee_id = ? " +
                                              "ORDER BY start_date DESC";
                        List<Map<String, Object>> leaveData = con.fetchRecords(leaveStatusSql, empId3);
                        if (!leaveData.isEmpty()) {
                            String[] headers = {"Leave ID", "Leave Type", "Start Date", "End Date", "Status", "Reason"};
                            System.out.println("--------------------------------------------------------------------------------");
                            System.out.println("| " + String.format("%-20s | %-20s | %-20s | %-20s | %-20s | %-20s |", 
                                headers[0], headers[1], headers[2], headers[3], headers[4], headers[5]));
                            System.out.println("--------------------------------------------------------------------------------");
                            for (Map<String, Object> row : leaveData) {
                                System.out.println("| " + String.format("%-20s | %-20s | %-20s | %-20s | %-20s | %-20s |", 
                                    row.get("leave_id"), row.get("leave_type"), row.get("start_date"), 
                                    row.get("end_date"), row.get("status"), 
                                    row.get("rejection_reason") != null ? row.get("rejection_reason") : ""));
                            }
                            System.out.println("--------------------------------------------------------------------------------");
                        } else {
                            System.out.println("No leave records found.");
                        }
                    } else {
                        System.out.println("Error: Employee not found.");
                    }
                    break;

                case 5:
                    System.out.println("Logging out...");
                    break;

                default:
                    System.out.println("Invalid choice.");
            }
        } while (choice != 5);
    }

    // ====== GET EMPLOYEE ID ======
    public static int getEmployeeIdByEmail(String email) {
        String sql = "SELECT employee_id FROM Employee WHERE email = ?";
        List<Map<String, Object>> res = con.fetchRecords(sql, email);
        if (!res.isEmpty()) {
            return Integer.parseInt(res.get(0).get("employee_id").toString());
        }
        return -1;
    }

    // ====== MAIN METHOD ======
    public static void main(String[] args) {
        ensureSuperAdmin();
        Scanner sc = new Scanner(System.in);
        char cont;

        do {
            System.out.println("\n===== HRIS MAIN MENU =====");
            System.out.println("1. Login");
            System.out.println("2. Register (New Employee)");
            System.out.println("3. Exit");
            System.out.print("Enter choice: ");
            int choice = sc.nextInt();
            sc.nextLine(); // consume newline

            switch (choice) {
                case 1:
                    System.out.print("Enter Email: ");
                    String email = sc.nextLine();
                    System.out.print("Enter Password: ");
                    String password = sc.nextLine();
                    String hashedPass = con.hashPassword(password);

                    String qry = "SELECT * FROM Employee WHERE email = ? AND password = ?";
                    List<Map<String, Object>> result = con.fetchRecords(qry, email, hashedPass);

                    if (result.isEmpty()) {
                        System.out.println("Invalid email or password!");
                    } else {
                        Map<String, Object> user = result.get(0);
                        String status = user.get("employment_status").toString();
                        String fname = user.get("first_name").toString();
                        String lname = user.get("last_name").toString();

                        if (status.equalsIgnoreCase("Pending")) {
                            System.out.println("Account is pending approval. Contact Admin.");
                        } else if (email.equalsIgnoreCase("admin@hris.com")) {
                            System.out.println("Welcome Super Admin " + fname + " " + lname);
                            adminMenu(sc);
                        } else {
                            System.out.println("Welcome Employee " + fname + " " + lname);
                            employeeMenu(sc, email);
                        }
                    }
                    break;

                case 2:
                    System.out.print("Enter First Name: ");
                    String fname = sc.nextLine();
                    System.out.print("Enter Last Name: ");
                    String lname = sc.nextLine();
                    System.out.print("Enter Email: ");
                    String newEmail = sc.nextLine();

                    while (true) {
                        String check = "SELECT * FROM Employee WHERE email = ?";
                        List<Map<String, Object>> res = con.fetchRecords(check, newEmail);
                        if (res.isEmpty()) break;
                        System.out.print("Email already exists. Enter another: ");
                        newEmail = sc.nextLine();
                    }

                    System.out.print("Enter Password: ");
                    String newPass = sc.nextLine();
                    String hashed = con.hashPassword(newPass);

                    // Show available departments
                    System.out.println("\nAvailable Departments:");
                    String deptSql = "SELECT department_id, dept_name FROM Department ORDER BY department_id";
                    List<Map<String, Object>> departments = con.fetchRecords(deptSql);
                    for (Map<String, Object> dept : departments) {
                        System.out.println(dept.get("department_id") + ". " + dept.get("dept_name"));
                    }
                    System.out.print("Enter Department ID: ");
                    int deptId = sc.nextInt();

                    // Show available ACTIVE positions only
                    System.out.println("\nAvailable Positions:");
                    String posSql = "SELECT position_id, position_name FROM Position WHERE status = 'Active' ORDER BY position_id";
                    List<Map<String, Object>> positions = con.fetchRecords(posSql);
                    for (Map<String, Object> pos : positions) {
                        System.out.println(pos.get("position_id") + ". " + pos.get("position_name"));
                    }
                    System.out.print("Enter Position ID: ");
                    int posId = sc.nextInt();
                    sc.nextLine(); // consume newline

                    String sql = "INSERT INTO Employee(first_name, last_name, email, password, department_id, position_id, employment_status) VALUES (?, ?, ?, ?, ?, ?, 'Pending')";
                    con.addRecord(sql, fname, lname, newEmail, hashed, deptId, posId);
                    System.out.println("Employee registered successfully. Awaiting admin approval.");
                    break;

                case 3:
                    System.out.println("Exiting program...");
                    System.exit(0);
                    break;

                default:
                    System.out.println("Invalid choice.");
                    break;
            }

            System.out.print("\nDo you want to continue? (Y/N): ");
            cont = sc.nextLine().charAt(0);
        } while (cont == 'Y' || cont == 'y');

        System.out.println("Thank you. Program ended.");
        sc.close();
    }
}