import java.io.*;
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
    private static Set<Long> usedIds = new HashSet<>();
    private static String outputPath = null;
    private static String sortField = null;
    private static String sortOrder = null;

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
        boolean outputParameterFound = false;

        for (String arg : args) {
            if (arg.startsWith("--output=")) {
                outputParameterFound = true;
                String[] parts = arg.split("=");
                if (parts[1].equalsIgnoreCase("file")) {
                    outputPath = getOutputPath(args);
                    if (outputPath == null) {
                        System.err.println("Error: Path to output file is missing.");
                        return;
                    }
                } else if (parts[1].equalsIgnoreCase("console")) {
                    outputPath = null;
                }
            } else if (arg.startsWith("--sort=") || arg.startsWith("-s=")) {
                sortField = arg.split("=")[1];
            } else if (arg.startsWith("--order=") || arg.startsWith("-o=")) {
                sortOrder = arg.split("=")[1];
            } else if (arg.startsWith("--path=")) {
                if (!outputParameterFound) {
                    System.err.println("Error: Incorrect usage. Please specify --output=file or --output=console.");
                    return;
                }
            }
        }

        if (outputPath == null && args.length > 0 && Arrays.stream(args).anyMatch(a -> a.startsWith("--output"))) {
            System.err.println("Error: --output=file parameter is required when specifying output path.");
            return;
        }

        readDataFromFile(inputFile);
        processPersons();

        if (sortField != null && sortOrder == null) {
            System.err.println("Error: --order parameter is required when --sort is specified.");
            return;
        }

        if ("name".equalsIgnoreCase(sortField) || "salary".equalsIgnoreCase(sortField)) {
            if (sortOrder == null) {
                System.err.println("Error: --order parameter is required when --sort is specified.");
                return;
            }
            if (!"asc".equalsIgnoreCase(sortOrder) && !"desc".equalsIgnoreCase(sortOrder)) {
                System.err.println("Error: Invalid order specified. Use 'asc' or 'desc'.");
                return;
            }
        }

        writeOutput();
    }

    private static String getOutputPath(String[] args) {
        for (String arg : args) {
            if (arg.startsWith("--path=")) {
                String[] parts = arg.split("=");
                if (parts.length < 2 || parts[1].isEmpty()) {
                    return null;
                }
                return parts[1];
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

                if (usedIds.contains(id)) {
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
                    usedIds.add(id);
                } else if ("Employee".equalsIgnoreCase(position)) {
                    String managerId = parts[4].trim();
                    try {
                        Long manId = Long.parseLong(managerId);
                        Employee employee = new Employee(position, id, name, salary, manId);
                        persons.add(employee);
                        usedIds.add(id);
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
                    invalidData.add("" + employee);
                }
            }
        }
    }

    private static void writeOutput() {
        try (PrintWriter writer = outputPath != null ? new PrintWriter(new FileWriter(outputPath)) : new PrintWriter(System.out)) {
            if (sortField != null) {
                sortPersons();
            }
            writeData(writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void sortPersons() {
        List<Employee> allEmployees = new ArrayList<>();

        for (Map.Entry<Long, List<Employee>> entry : departmentEmployees.entrySet()) {
            allEmployees.addAll(entry.getValue());
        }

        Comparator<Employee> comparator;
        if ("name".equalsIgnoreCase(sortField)) {
            comparator = Comparator.comparing(e -> e.name);
        } else {
            comparator = Comparator.comparing(e -> e.salary);
        }

        if ("desc".equalsIgnoreCase(sortOrder)) {
            comparator = comparator.reversed();
        }

        allEmployees.sort(comparator);

        for (Map.Entry<Long, List<Employee>> entry : departmentEmployees.entrySet()) {
            entry.setValue(new ArrayList<>());
        }

        for (Employee employee : allEmployees) {
            departmentEmployees.get(managers.get(employee.managerId).id).add(employee);
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