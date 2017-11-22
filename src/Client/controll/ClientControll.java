package Client.controll;
import Client.connect.ClientConnect;
import Client.connect.OutObserver;
import java.io.IOException;

public class ClientControll {
    private final ClientConnect connection = new ClientConnect();
    public void connect(String host, int port, OutObserver out){
        try {
            connection.connect(host,port, out);
        }catch (IOException e){
            e.printStackTrace();
            System.exit(1);
        }
    }
    public void start(){
        connection.start();
    }

    public void quit(){
        try {
            connection.quit();
        }
        catch (IOException e){
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void sendGuess(String guess){
        connection.sendGuess(guess);
    }
}
