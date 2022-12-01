import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Main
{    
    public static void main(String[] args)throws IOException{
        String path = args[0];
        BufferedReader reader = new BufferedReader(new FileReader(path));
        String output;
        while((output = reader.readLine()) != null)
            System.out.println(output);
        reader.close();
    }
}