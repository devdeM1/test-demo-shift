import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class Main {

    static class Employee {
        String position;
        int id;
        String name;
        double salary;
        String managerId;

        public Employee(String position, int id, String name, double salary, String managerId) {
            this.position = position;
            this.id = id;
            this.name = name;
            this.salary = salary;
            this.managerId = managerId;
        }

        @Override
        public String toString() {
            return position + ", " + id + ", " + name + ", " + Math.ceil(salary * 100) / 100;
        }
    }

    private static List<Employee> employees = new ArrayList<>();
    private static List<String> invalidData = new ArrayList<>();
    private static Map<Integer, List<Employee>> departmentEmployees = new HashMap<>();
    private static Map<String, Employee> managers = new HashMap<>();
    private static Map<String, DepartmentStats> departmentStats = new HashMap<>();

    private static String outputPath = null;

    private static class DepartmentStats {
        int employeeCount = 0;
        double totalSalary = 0.0;

        public void addEmployee(double salary) {
            employeeCount++;
            totalSalary += salary;
        }

        public String getStats() {
            double averageSalary = employeeCount > 0 ? Math.ceil(totalSalary / employeeCount * 100) / 100 : 0.0;
            return employeeCount + ", " + averageSalary;
        }
    }

    public static void main(String[] args) {
        String inputFile = "input_file.txt";

        for (String arg : args) {
            if (arg.startsWith("--output=")) {
                String[] parts = arg.split("=");
                if (parts[1].equals("file")) {
                    outputPath = getOutputPath(args);
                    if (outputPath == null) {
                        System.err.println("Error: Path to output file is missing.");
                        return;
                    }
                } else if (parts[1].equals("console")) {
                    outputPath = null;
                }
            }
        }

        readDataFromFile(inputFile);
        processEmployees();
        writeOutput();
    }

    private static String getOutputPath(String[] args) {
        for (String arg : args) {
            if (arg.startsWith("--path=")) {
                return arg.split("=")[1];
            }
        }
        return null;
    }

    private static void readDataFromFile(String filename) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                try {
                    if (parts.length != 5) throw new IllegalArgumentException("Incorrect number of fields");

                    String position = parts[0].trim();
                    int id = Integer.parseInt(parts[1].trim());
                    String name = parts[2].trim();
                    double salary = Double.parseDouble(parts[3].trim());
                    String managerId = parts[4].trim();

                    if (salary <= 0 || (position.equals("Employee") && managerId.isEmpty())) {
                        throw new IllegalArgumentException("Invalid employee data");
                    }

                    Employee employee = new Employee(position, id, name, salary, managerId);
                    employees.add(employee);

                } catch (Exception e) {
                    invalidData.add(line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void processEmployees() {
        for (Employee employee : employees) {
            if ("Manager".equals(employee.position)) {
                managers.put(String.valueOf(employee.id), employee);
                departmentStats.put(employee.managerId, new DepartmentStats());
                departmentStats.get(employee.managerId).addEmployee(employee.salary);
            } else if ("Employee".equals(employee.position)) {
                if (managers.containsKey(employee.managerId)) {
                    departmentEmployees.putIfAbsent(Integer.parseInt(employee.managerId), new ArrayList<>());
                    departmentEmployees.get(Integer.parseInt(employee.managerId)).add(employee);
                    departmentStats.get(managers.get(employee.managerId).managerId).addEmployee(employee.salary);
                } else {
                    invalidData.add(employee.toString());
                }
            }
        }
    }

    private static void writeOutput() {
        try {
            PrintWriter writer;
            if (outputPath != null) {
                writer = new PrintWriter(new FileWriter(outputPath));
            } else {
                writer = new PrintWriter(System.out);
            }
            writeData(writer);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void writeData(PrintWriter writer) {
        // Вывод данных
        for (Map.Entry<String, Employee> entry : managers.entrySet()) {
            Employee manager = entry.getValue();
            String departmentName = manager.managerId; // Название отдела

            writer.println(departmentName);
            writer.println(manager.toString());

            // Вывод сотрудников, подчинённых данному менеджеру
            List<Employee> subordinates = departmentEmployees.get(manager.id);
            if (subordinates != null) {
                for (Employee subordinate : subordinates) {
                    writer.println(subordinate.toString());
                }
            }

            // Вывод статистики для отдела
            writer.println(departmentStats.get(departmentName).getStats());
        }

        // Вывод некорректных данных
        if (!invalidData.isEmpty()) {
            writer.println("Некорректные данные:");
            for (String invalid : invalidData) {
                writer.println(invalid);
            }
        }
    }
}