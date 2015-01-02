//package node;
//
//import java.io.IOException;
//import java.net.ServerSocket;
//import java.net.Socket;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//
//public class NodeRequestWaiter implements Runnable {
//	
////	private int _tcpPort;
//	private ServerSocket _socket;
//	private boolean _isAlive;
//	private ExecutorService _nodePool;
//	
//	public NodeRequestWaiter(ServerSocket socket) {
//		_isAlive = true;
//		_socket = socket;
//	}
//
//	@Override
//	public void run() {	
//		System.out.println("node request waiter");
//		// new thread pool, for client requests, so that this thread can wait
//		// for more clients
//		_nodePool = Executors.newCachedThreadPool();
//
//		while (_isAlive) {
//			try {
//				// socket waiting (accept() is a blocking method)
//				System.out.println("accept");
//				Socket socket = _socket.accept();
//				// client request has arrived, new thread will be generated
//				NodeRequestWorker thread = new NodeRequestWorker(socket);
//				// generated thread is given to the Thread Pool
//				_nodePool.execute(thread);
//			} catch (IOException e) {
//				// TODO
//			}
//		}		
//	}
//
//	public void terminate() throws IOException {
//		_nodePool.shutdown();
//		_isAlive = false;
//	}
//}
