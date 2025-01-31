import java.io.*;

public class Main {
    public static void main(String[] args) {
        String all_text = " ";
        try {
            BufferedReader reader = new BufferedReader(new FileReader("input_file.txt"));
            String line = reader.readLine();
            while (line != null) {
                String[] parts = line.split(",");
                for (String element: parts){
                    all_text = all_text.concat(element);
                }
                all_text = all_text.concat("\n");
                line = reader.readLine();
            }
            reader.close();
            System.out.println(all_text);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try(FileWriter writer = new FileWriter("output.txt", false))
        {
            writer.write(all_text);
            writer.flush();
        }
        catch(IOException ex){
            System.out.println(ex.getMessage());
        }
    }
}