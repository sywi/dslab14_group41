package node;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TCPRequestWaiter implements Runnable {
	
	private boolean _isAlive;
	private ServerSocket _socket;
	private Node _ctrl;
	private ExecutorService _pool;
	
	public TCPRequestWaiter (ServerSocket socket, Node ctrl) {
		_isAlive = true;
		_socket = socket;
		_ctrl = ctrl;
	}

	public void terminate() {
		_isAlive = false;
		_pool.shutdown();
	}
	
	@Override
	public void run() {
		_pool = Executors.newCachedThreadPool();

		while(_isAlive) {
			try {
				// socket waiting (accept() is a blocking method)
				Socket socket = _socket.accept();
								
				// client request has arrived, for each client request a new thread will be generated
//				ClientRequestThread thread = new ClientRequestThread(socket, _ctrl.getLogDir(), _ctrl.getOperatorList(), _ctrl.getComponentName(), _ctrl.getPrintStream(), _ctrl.getRmin());
				TCPRequestThread thread = new TCPRequestThread(socket, _ctrl);
				
				// generated thread is given to the Thread Pool
				_pool.execute(thread);
			} catch (IOException e) {
//				e.printStackTrace();
			}			
		}		
	}

}
