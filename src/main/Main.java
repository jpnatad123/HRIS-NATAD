package Main;

import config.dbConnect;
import java.util.*;

public class Main {

    public static dbConnect con = new dbConnect();

    public static void ensureSuperAdmin() {
        String checkQuery = "SELECT * FROM Employee WHERE email = ?";
        List<Map<String, Object>> result = con.fetchRecords(checkQuery, "admin@hris.com");
        String correctHash = con.hashPassword("admin123");

        if (result.isEmpty()) {
            String insert = "INSERT INTO Employee(first_name, last_name, email, password, department_id, position_id, employment_status) VALUES (?, ?, ?, ?, 1, 1, 'Active')";
            con.addRecord(insert, "Super", "Admin", "admin@hris.com", correctHash);
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
            System.out.println("1. View All Employees");
            System.out.println("2. Approve Pending Employee");
            System.out.println("3. Update Employee Status");
            System.out.println("4. Delete Employee");
            System.out.println("5. Logout");
            System.out.print("Enter choice: ");
            choice = sc.nextInt();

            switch (choice) {
                case 1:
                    String viewSql = "SELECT employee_id, first_name, last_name, email, employment_status FROM Employee";
                    String[] headers = {"ID", "First Name", "Last Name", "Email", "Status"};
                    String[] cols = {"employee_id", "first_name", "last_name", "email", "employment_status"};
                    con.viewRecords(viewSql, headers, cols);
                    break;
                case 2:
                    System.out.print("Enter Employee ID to Approve: ");
                    int approveId = sc.nextInt();
                    String approveSql = "UPDATE Employee SET employment_status = 'Approved' WHERE employee_id = ?";
                    con.updateRecord(approveSql, approveId);
                    break;
                case 3:
                    System.out.print("Enter Employee ID to Update: ");
                    int updateId = sc.nextInt();
                    System.out.print("Enter New Status: ");
                    String newStatus = sc.next();
                    String updateSql = "UPDATE Employee SET employment_status = ? WHERE employee_id = ?";
                    con.updateRecord(updateSql, newStatus, updateId);
                    break;
                case 4:
                    System.out.print("Enter Employee ID to Delete: ");
                    int delId = sc.nextInt();
                    String deleteSql = "DELETE FROM Employee WHERE employee_id = ?";
                    con.deleteRecord(deleteSql, delId);
                    break;
                case 5:
                    System.out.println("Logging out...");
                    break;
                default:
                    System.out.println("Invalid choice.");
            }
        } while (choice != 5);
    }

    // ====== EMPLOYEE MENU ======
    public static void employeeMenu(Scanner sc, String email) {
        int choice;
        do {
            System.out.println("\n===== EMPLOYEE MENU =====");
            System.out.println("1. View My Profile");
            System.out.println("2. Apply for Leave");
            System.out.println("3. View My Payroll Record");
            System.out.println("4. Logout");
            System.out.print("Enter choice: ");
            choice = sc.nextInt();

            switch (choice) {
                case 1:
                    String profileSql = "SELECT employee_id, first_name, last_name, email, department_id, position_id, employment_status FROM Employee WHERE email = ?";
                    String[] headers = {"ID", "First Name", "Last Name", "Email", "Dept ID", "Pos ID", "Status"};
                    String[] cols = {"employee_id", "first_name", "last_name", "email", "department_id", "position_id", "employment_status"};
                    con.viewRecords(profileSql.replace("?", "'" + email + "'"), headers, cols);
                    break;
                case 2:
                    System.out.print("Enter Leave Type (e.g. Vacation, Sick): ");
                    String leaveType = sc.next();
                    System.out.print("Enter Start Date (YYYY-MM-DD): ");
                    String startDate = sc.next();
                    System.out.print("Enter End Date (YYYY-MM-DD): ");
                    String endDate = sc.next();
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
                        String paySql = "SELECT payroll_id, basic_salary, deductions, bonuses, net_salary, payroll_date FROM Payroll WHERE employee_id = ?";
                        String[] payHeaders = {"Payroll ID", "Basic", "Deduct", "Bonus", "Net", "Date"};
                        String[] payCols = {"payroll_id", "basic_salary", "deductions", "bonuses", "net_salary", "payroll_date"};
                        con.viewRecords(paySql.replace("?", String.valueOf(empId2)), payHeaders, payCols);
                    } else {
                        System.out.println("No payroll record found.");
                    }
                    break;
                case 4:
                    System.out.println("Logging out...");
                    break;
                default:
                    System.out.println("Invalid choice.");
            }
        } while (choice != 4);
    }

    public static int getEmployeeIdByEmail(String email) {
        String sql = "SELECT employee_id FROM Employee WHERE email = ?";
        List<Map<String, Object>> res = con.fetchRecords(sql, email);
        if (!res.isEmpty()) {
            return Integer.parseInt(res.get(0).get("employee_id").toString());
        }
        return -1;
    }

    public static void main(String[] args) {
        con.connectDB();
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

            switch (choice) {
                case 1:
                    System.out.print("Enter Email: ");
                    String email = sc.next();
                    System.out.print("Enter Password: ");
                    String password = sc.next();
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
                    String fname = sc.next();
                    System.out.print("Enter Last Name: ");
                    String lname = sc.next();
                    System.out.print("Enter Email: ");
                    String newEmail = sc.next();

                    while (true) {
                        String check = "SELECT * FROM Employee WHERE email = ?";
                        List<Map<String, Object>> res = con.fetchRecords(check, newEmail);
                        if (res.isEmpty()) {
                            break;
                        } else {
                            System.out.print("Email already exists. Enter another: ");
                            newEmail = sc.next();
                        }
                    }

                    System.out.print("Enter Password: ");
                    String newPass = sc.next();
                    String hashed = con.hashPassword(newPass);
                    String sql = "INSERT INTO Employee(first_name, last_name, email, password, department_id, position_id, employment_status) VALUES (?, ?, ?, ?, 1, 1, 'Pending')";
                    con.addRecord(sql, fname, lname, newEmail, hashed);
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
            cont = sc.next().charAt(0);
        } while (cont == 'Y' || cont == 'y');

        System.out.println("Thank you. Program ended.");
    }
}
