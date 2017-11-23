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
    //called from handler after message has been added to the buffer to set the keys keyoperation to write
    public void setKeyToWrite(SelectionKey key){
        key.interestOps(SelectionKey.OP_WRITE);
        selector.wakeup();
    }
    //get the message from client add the key so we now who to send the reply to
    //if IOeexcetion cancel the key
    private void takeinput(SelectionKey key) throws IOException{
        HangHandler handler = (HangHandler) key.attachment();
        try {
            handler.getInput(key);
        }catch (IOException e){
            key.cancel();
        }
    }
    //get the handler and call send with the uppdated word. Set the key to read the next guess.
    private void send(SelectionKey key) throws IOException{
        HangHandler handler = (HangHandler) key.attachment();
        handler.send();
        key.interestOps(SelectionKey.OP_READ);
    }

    //set up connection to client and add the handler class as atachment.
    public void handler(SelectionKey key)throws IOException{
        ServerSocketChannel servCh = (ServerSocketChannel) key.channel();
        SocketChannel clientCh = servCh.accept(); //set up the conection to the new client
        clientCh.configureBlocking(false);
        clientCh.register(selector,SelectionKey.OP_WRITE,new HangHandler(this,clientCh)); //save the handler as attachment to key
    }
}
