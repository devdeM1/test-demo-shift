import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.*;

public class Main {

    static abstract class Person {
        String position;
        Long id;
        String name;
        BigDecimal salary;

        public Person(String position, Long id, String name, BigDecimal salary) {
            this.position = position;
            this.id = id;
            this.name = name;
            this.salary = salary;
        }

        @Override
        public String toString() {
            return position + ", " + id + ", " + name + ", " + salary;
        }
    }

    static class Manager extends Person {
        String departmentName;

        public Manager(String position, Long id, String name, BigDecimal salary, String departmentName) {
            super(position, id, name, salary);
            this.departmentName = departmentName;
        }
    }

    static class Employee extends Person {
        Long managerId;

        public Employee(String position, Long id, String name, BigDecimal salary, Long managerId) {
            super(position, id, name, salary);
            this.managerId = managerId;
        }
    }

    private static List<Person> persons = new ArrayList<>();
    private static List<String> invalidData = new ArrayList<>();
    private static Map<Long, List<Employee>> departmentEmployees = new HashMap<>();
    private static Map<Long, Manager> managers = new HashMap<>();
    private static Map<String, DepartmentStats> departmentStats = new HashMap<>();
    private static String outputPath = null;

    private static class DepartmentStats {
        int employeeCount = 0;
        BigDecimal totalSalary = BigDecimal.ZERO;

        public void addEmployee(BigDecimal salary) {
            employeeCount++;
            totalSalary = totalSalary.add(salary);
        }

        public String getStats() {
            if (employeeCount == 0) return "0, 0.00";
            BigDecimal averageSalary = totalSalary.divide(BigDecimal.valueOf(employeeCount), 2, BigDecimal.ROUND_HALF_UP);
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
        processPersons();
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
                if (parts.length != 5) {
                    invalidData.add(line);
                    continue;
                }

                String position = parts[0].trim();
                Long id;
                String name = parts[2].trim();
                BigDecimal salary;


                try {
                    id = Long.parseLong(parts[1].trim());
                    salary = new BigDecimal(parts[3].trim());
                    if (salary.compareTo(BigDecimal.ZERO) < 0) {
                        invalidData.add(line);
                        continue;
                    }
                } catch (NumberFormatException e) {
                    invalidData.add(line);
                    continue;
                }

                if ("Manager".equalsIgnoreCase(position)) {
                    String departmentName = parts[4].trim();
                    Manager manager = new Manager(position, id, name, salary, departmentName);
                    persons.add(manager);
                    managers.put(id, manager);
                    departmentStats.put(departmentName, new DepartmentStats());
                    departmentStats.get(departmentName).addEmployee(salary);
                } else if ("Employee".equalsIgnoreCase(position)) {
                    String managerId = parts[4].trim();
                    try {
                        Long manId = Long.parseLong(managerId);
                        Employee employee = new Employee(position, id, name, salary, manId);
                        persons.add(employee);
                    } catch (NumberFormatException e) {
                        invalidData.add(line);
                    }
                } else {
                    invalidData.add(line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void processPersons() {
        for (Person person : persons) {
            if (person instanceof Employee) {
                Employee employee = (Employee) person;
                Manager manager = managers.get(employee.managerId);
                if (manager != null) {
                    departmentEmployees.putIfAbsent(manager.id, new ArrayList<>());
                    departmentEmployees.get(manager.id).add(employee);
                    departmentStats.get(manager.departmentName).addEmployee(employee.salary);
                } else {
                    invalidData.add("Сотрудник без существующего менеджера: " + employee);
                }
            }
        }
    }

    private static void writeOutput() {
        try (PrintWriter writer = outputPath != null ? new PrintWriter(new FileWriter(outputPath)) : new PrintWriter(System.out)) {
            writeData(writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void writeData(PrintWriter writer) {
        for (Map.Entry<Long, Manager> entry : managers.entrySet()) {
            Manager manager = entry.getValue();
            String departmentName = manager.departmentName;

            writer.println(departmentName);
            writer.println(manager.toString());

            List<Employee> subordinates = departmentEmployees.get(manager.id);
            if (subordinates != null) {
                for (Employee subordinate : subordinates) {
                    writer.println(subordinate.toString());
                }
            }

            writer.println(departmentStats.get(departmentName).getStats());
            writer.println();
        }

        if (!invalidData.isEmpty()) {
            writer.println("Некорректные данные:");
            for (String invalid : invalidData) {
                writer.println(invalid);
            }
        }
    }
}
