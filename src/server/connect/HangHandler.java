package server.connect;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ForkJoinPool;

import server.model.Gamestate;



public class HangHandler extends Thread {

    private final Queue<ByteBuffer> output = new ArrayDeque<>(); //Buffer for messages to be sent to client
    private static Object[] words; //array for hangman words
    private Random rand = new Random();
    private SocketChannel socketChannel;
    private ByteBuffer msg = ByteBuffer.allocateDirect(5000); //buffer for input from client
    private Queue<String> messageQueue = new ArrayDeque<>(); //queue for messages from client
    private Gamestate gamestate ;
    private SelectionKey key; //refrence for the key so we can add messages to the clients message queue.
    private HangServer server;


    public HangHandler(HangServer server, SocketChannel socketChannel){
        gamestate = new Gamestate();   //create a new gamestate that keeps the state of the client.

        //Create array for the words from the supplied file.
        try{
            BufferedReader readFile = new BufferedReader(new FileReader("/Users/Andreas/IdeaProjects/hang/src/words.txt"));
            words = readFile.lines().toArray();
        }catch (IOException error) {
            error.printStackTrace();
        }
        this.socketChannel = socketChannel;
        this.server = server;

        //create welcome message and add it to the send buffer
        String welcomeMSG = "Welcome press start to play hangman";
        addMsg(welcomeMSG);
    }

    //add message to the output buffer
    private void addMsg(String msg){
        ByteBuffer bytesMsg = ByteBuffer.wrap(msg.getBytes());
        synchronized (output){
            output.add(bytesMsg);
        }
    }

    //sends the messages in the buffer to the client
    public void send()throws IOException{
        ByteBuffer msg;
        synchronized (output){
            while ((msg = output.peek()) != null){
                //send(msg);
                socketChannel.write(msg);
                output.remove();
            }
        }
    }


    @Override
    public void run(){
        String guess;
        boolean correct = false;
        while(!messageQueue.isEmpty()){ //handle the messages that are in the queue
                guess = messageQueue.poll(); //get the first message
                if((gamestate.getAttempt() == 0 && guess.equals("START"))){
                    int randomnumber = rand.nextInt(words.length);
                    System.out.println((String) words[randomnumber]);
                    gamestate.newgame((String) words[randomnumber]); //create a new game and send a new word
                }
                else if(gamestate.getAttempt()>0){ //make sure that if user doesn´t send start it will not decrese or add the score when round is done.
                    if(guess.length()>1){
                        correct = gamestate.tryWordGuess(guess);  //if the guess has more than one letter it is a asumed to be a word guess.
                    }
                    else {
                        correct = gamestate.tryGuess(guess); //otherwise try the letter
                    }
                    gamestate.decreseAttemps(); //decreset the number of attemps.
                    if(correct){
                        gamestate.addScore();
                        gamestate.setAttemptToZero();
                        //if the word is solved increse score bu one
                    }
                    else if(gamestate.getAttempt() == 0) {
                        gamestate.decreseScore();   //if no atemtps left and the word is not solved lower the score.
                    }


                }
                addMsg(gamestate.getOutputString(correct));
                server.setKeyToWrite(key); //add the outputstring to the buffer to be sent to the client and set the key to write
        }
    }
    public void setKey(SelectionKey key){
        this.key = key;
    }

    //read the buffer in the socketchannel and handle the message
    public void getInput()throws IOException{
        msg.clear(); //set position to 0
        int bytesRead;
        bytesRead = socketChannel.read(msg);
        if(bytesRead == -1){
            throw new IOException("Closed");
        }
        msg.flip();
        byte[] byteGuess = new byte[msg.remaining()];
        msg.get(byteGuess);
        String guess = new String(byteGuess);
        messageQueue.add(guess); //add the message to the queue
        ForkJoinPool.commonPool().execute(this); //assign javathread to handle the message
    }


}
