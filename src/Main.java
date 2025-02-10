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
            return String.join(", ", position, String.valueOf(id), name, salary.setScale(2, BigDecimal.ROUND_CEILING).toString());
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
    private static Map<Long, Manager> managerMap = new HashMap<>();
    private static Map<String, DepartmentStats> departmentStats = new HashMap<>();
    private static String outputPath = "console";
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
            if (employeeCount == 0) {
                return "0, 0.00";
            }
            BigDecimal averageSalary = totalSalary.divide(BigDecimal.valueOf(employeeCount), 2, BigDecimal.ROUND_CEILING);
            return employeeCount + ", " + averageSalary.setScale(2, BigDecimal.ROUND_CEILING).toString();
        }
    }

    public static void main(String[] args) {
        String inputFile = "input_file.txt";
        Set<String> validFlags = new HashSet<>(Arrays.asList("--output=", "--path",
                "--sort=", "--order=",
                "-s=", "-o="));

        if (!parseArguments(args, validFlags)) {
            return;
        }

        readDataFromFile(inputFile);
        processPersons();

        if (sortField != null && sortOrder == null) {
            printError("--order parameter is required when --sort is specified.");
            return;
        }

        writeOutput();
    }

    private static boolean parseArguments(String[] args, Set<String> validFlags) {
        for (String arg : args) {
            if (!isValidFlag(arg, validFlags)) {
                printError("Unknown flag: " + arg);
                return false;
            }

            if (arg.startsWith("--output=") || arg.startsWith("-o=")) {
                if (!handleOutputFlag(arg, args)) {
                    return false;
                }
            } else if (arg.startsWith("--sort=") || arg.startsWith("-s=")) {
                if (!handleSortFlag(arg)) {
                    return false;
                }
            } else if (arg.startsWith("--order=")) {
                if (!handleOrderFlag(arg)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isValidFlag(String arg, Set<String> validFlags) {
        return validFlags.stream().anyMatch(arg::startsWith);
    }

    private static void printError(String message) {
        System.err.println("Error: " + message);
    }

    private static boolean handleOutputFlag(String arg, String[] args) {
        String[] parts = arg.split("=");
        if (parts.length != 2 || parts[1].isEmpty()) {
            printError("--output parameter value cannot be empty. Use --output=file for file output or --output=console for console output.");
            return false;
        }
        if (parts[1].equalsIgnoreCase("file")) {
            outputPath = getOutputPath(args);
            if (outputPath == null) {
                printError("Path to output file is missing.");
                return false;
            }
        } else if (parts[1].equalsIgnoreCase("console")) {
            if (Arrays.stream(args).anyMatch(argPath -> argPath.startsWith("--path="))) {
                printError("--path cannot be specified when --output is set to console.");
                return false;
            }
        } else {
            printError("Invalid value for --output. Use --output=file or --output=console.");
            return false;
        }
        return true;
    }

    private static boolean handleSortFlag(String arg) {
        String[] parts = arg.split("=");
        if (parts.length != 2 || parts[1].isEmpty() ||
                (!"name".equalsIgnoreCase(parts[1]) && !"salary".equalsIgnoreCase(parts[1]))) {
            printError("Invalid value for --sort or -s. Only --sort=name or --sort=salary is allowed.");
            return false;
        }
        sortField = parts[1].toLowerCase();
        return true;
    }

    private static boolean handleOrderFlag(String arg) {
        String[] parts = arg.split("=");
        if (parts.length != 2 || parts[1].isEmpty()) {
            printError("--order parameter value cannot be empty. Use --order=asc or --order=desc.");
            return false;
        }
        if (sortField == null) {
            printError("--order parameter cannot be specified without --sort.");
            return false;
        }
        sortOrder = parts[1].toLowerCase();
        if (!"asc".equalsIgnoreCase(sortOrder) && !"desc".equalsIgnoreCase(sortOrder)) {
            printError("Invalid order specified. Use 'asc' or 'desc'.");
            return false;
        }
        return true;
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

                boolean idExists = persons.stream().anyMatch(p -> p.id.equals(id));
                if (idExists) {
                    invalidData.add(line);
                    continue;
                }

                if ("Manager".equalsIgnoreCase(position)) {
                    String departmentName = parts[4].trim();
                    Manager manager = new Manager(position, id, name, salary, departmentName);
                    persons.add(manager);
                    managerMap.put(id, manager);
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
                Manager manager = managerMap.get(employee.managerId);
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
        try (PrintWriter writer = outputPath.equals("console") ?
                new PrintWriter(System.out) : new PrintWriter(new FileWriter(outputPath))) {
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
            departmentEmployees.get(managerMap.get(employee.managerId).id).add(employee);
        }
    }

    private static void writeData(PrintWriter writer) {
        for (Map.Entry<Long, Manager> entry : managerMap.entrySet()) {
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