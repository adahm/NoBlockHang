package server.connect;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;
public class HangServer {
    private int port = 20000;
    private Queue<ByteBuffer> sendBuffer = new ArrayDeque<>(); //shot
    private Selector selector;
    private ServerSocketChannel serverSocketChannel;
    public static void main(String[] args){
       HangServer server = new HangServer();
       server.setup();
    }
    public void setup(){
        try{
            //setup server socketchanne and the selector
            selector = Selector.open();
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.bind(new InetSocketAddress(port));
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("setup done");

            //accept connections/write/read to clients
            while (true){
                selector.select(); //stop and wait for key operation
                Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                while (it.hasNext()){
                    SelectionKey key = it.next();
                    it.remove();
                    if (!key.isValid()){
                        continue;
                    }
                    if(key.isAcceptable()){
                        System.out.println("got connection");
                        handler(key);
                    }
                    else if(key.isReadable()){
                        takeinput(key);
                    }
                    else if(key.isWritable()){
                        send(key);
                    }

                }

            }
        }catch (IOException e){
            e.printStackTrace();
        }

    }
    //called from handler get the client and add the uppdated word to be sent to the client and set the keyoperation to write
    public void addToOutBuffer(SelectionKey key ,String OutputString){
        Client client = (Client) key.attachment();
        client.addMsg(OutputString);
        key.interestOps(SelectionKey.OP_WRITE);
        selector.wakeup();

    }
    //get the message from client add the key so we now who to send the reply to
    //if IOeexcetion cancel the key
    private void takeinput(SelectionKey key) throws IOException{
        Client client = (Client) key.attachment();
        try {
            client.handler.getInput(key);
        }catch (IOException e){
            key.cancel();
        }
    }
    //get the client and call send with the uppdated word. Set the key to read the next guess.
    private void send(SelectionKey key) throws IOException{
        Client client = (Client) key.attachment();
        client.send();
        key.interestOps(SelectionKey.OP_READ);
    }
    //set up connection to client and add the client class as atachment.
    public void handler(SelectionKey key)throws IOException{
        ServerSocketChannel servCh = (ServerSocketChannel) key.channel();
        SocketChannel clientCh = servCh.accept(); //set up the conection to the new client
        clientCh.configureBlocking(false);
        clientCh.register(selector,SelectionKey.OP_WRITE,new Client(new HangHandler(this,clientCh))); //save the client which has handler as attachment to slector key
    }

    //class that keeps outbuffer for every client and the handler
    private class Client{
        private final Queue<ByteBuffer> output = new ArrayDeque<>();
        private final HangHandler handler;
        //Constructor for client adds welcome message to buffer to be sent.
        private Client(HangHandler hangHandler){
            handler = hangHandler;
            String welcomeMSG = "Welcome press start to play hangman";
            addMsg(welcomeMSG);
        }
        //adds message to buffer
        private void addMsg(String msg){
            ByteBuffer bytesMsg = ByteBuffer.wrap(msg.getBytes());
            synchronized (output){
                output.add(bytesMsg);
            }
        }
        //sends the msg in the buffer.
        private void send()throws IOException{
            ByteBuffer msg;
            synchronized (output){
                while ((msg = output.peek()) != null){
                    handler.send(msg);
                    output.remove();
                }
            }
        }
    }
}
