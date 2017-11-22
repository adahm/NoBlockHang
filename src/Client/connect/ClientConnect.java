package Client.connect;



import java.io.IOException;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.ForkJoinPool;
//fixa svar från server

public class ClientConnect {
    private SocketChannel socketChannel;
    private ByteBuffer input = ByteBuffer.allocateDirect(500);
    private final Queue<ByteBuffer> output = new ArrayDeque<>();
    private Selector selector;
    private OutObserver out;
    private boolean sendnow = false;


    public void connect(String host, int port, OutObserver out) throws IOException{
        this.out = out; //set the outOberserver to the one recieved
        socketChannel = SocketChannel.open(); //set up the socketchannel and set it to not blocking
        socketChannel.configureBlocking(false);
        socketChannel.connect(new InetSocketAddress(host,port));
        try{
            selector = Selector.open();
        }catch (IOException e){
            e.printStackTrace();
        }
        //we will read the welcome message after connection.
        socketChannel.register(selector, SelectionKey.OP_READ);
        socketChannel.finishConnect();
        while (true){
            if(sendnow){
                socketChannel.keyFor(selector).interestOps(SelectionKey.OP_WRITE);
                sendnow = false;
            }
            //block the thread until key ready for read/write
            selector.select();
            for (SelectionKey key : selector.selectedKeys()){
                selector.selectedKeys().remove(key);
                if(!key.isValid()){
                    continue;
                }
                if(key.isReadable()){
                    readInput(key);
                }
                else if(key.isWritable()){
                    sendToServer(key);
                }
            }
        }
    }

    public void quit() throws IOException{
        socketChannel.close();
        socketChannel.keyFor(selector).cancel();
        //close the channel
    }

    public void sendGuess(String guess){
        send(guess);
    }

    public void start(){
        send("START");
    }

    private void send(String msg){ // add the message to the outputbuffer and wake up the thread blocked a selector.select;
        output.add(ByteBuffer.wrap(msg.getBytes()));
        sendnow = true;
        selector.wakeup();
    }

    //To send a message to the server method atapted from Serverconnection.java class from textprortocollchat from the coursewebb
    private void sendToServer(SelectionKey key)throws IOException{
        ByteBuffer msg;
        synchronized (output){
            while ((msg = output.peek()) != null){ //read the queue
                socketChannel.write(msg); //send the message
                if(msg.hasRemaining()){
                    return;
                }
                output.remove(); //remove the message from the queue

            }
            key.interestOps(SelectionKey.OP_READ);
        }
    }


//using the observer pattern shown on the coursewebb
    public void readInput(SelectionKey key)throws IOException{
        input.clear(); //reset position in buffer
        int Bytes = socketChannel.read(input); //read bytes and place it in input buffer
        if(Bytes == -1){
            throw new IOException();
        }
        input.flip();
        byte[] byteMsg = new byte[input.remaining()];
        input.get(byteMsg); //transfer bytes to byte array
        ForkJoinPool.commonPool().execute(()-> out.getServerInput(new String(byteMsg))); //assign java thread to notify of out.
    }

    //fixa så att out put vi får kanske gör en outputhandler interface så att det blir observer pattern.
}
