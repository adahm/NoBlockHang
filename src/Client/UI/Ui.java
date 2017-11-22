package Client.UI;

import java.util.Scanner;
import java.util.concurrent.ForkJoinPool;
import Client.controll.ClientControll;
import Client.connect.OutObserver;
public class Ui extends Thread{
    private final Scanner in = new Scanner(System.in);
    private Boolean go = true;
    private String input;
    private ClientControll control = new ClientControll();
    private PrintOut out = new PrintOut();
    @Override
    public void run(){
        //Assume that clienct allways want to connect when starting the program
        //assign thread from java threadpool to keep the conection.
        ForkJoinPool.commonPool().execute(() -> control.connect("localhost",20000,out));

        do {
            System.out.println("AT any time write QUIT to quit the app:" +
            "or Write START to start a new game");
            input = in.next();
            switch(input){
                case "START":
                    control.start();
                    while(go){
                        System.out.println("Guess letter or word:");
                        input = in.next();
                        switch (input){
                            case "QUIT":
                                go = false;
                                control.quit();
                            default:
                                control.sendGuess(input);  //use commonpool to run a seperat thread
                        }
                    }
                case "QUIT":
                    go = false;
                    control.quit();
                    break;


                default:
                    System.out.println("not known command");

            }

        }while(go);
    }
    private class PrintOut implements OutObserver{
        @Override
        public void getServerInput(String input){
            System.out.println(input);
        }
    }

}
