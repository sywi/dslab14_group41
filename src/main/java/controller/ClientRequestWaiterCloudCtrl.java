package controller;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientRequestWaiterCloudCtrl implements Runnable {
	
	private ExecutorService _clientPool;
	private int _tcpPort;
	private ServerSocket _socket;
	private CloudController _cloudCtrl;
	private boolean _isAlive;
	private WorkerThread thread;
	
	public ClientRequestWaiterCloudCtrl(int tcpPort, CloudController cloudCtrl) {
		_tcpPort = tcpPort;
		_cloudCtrl = cloudCtrl;
		_isAlive = true;
	}

	@Override
	public void run() {
		try {
			_socket = new ServerSocket(_tcpPort);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		// new thread pool, for client requests, so that this thread can wait
		// for more clients
		_clientPool = Executors.newCachedThreadPool();

		while (_isAlive) {
			try {
				// socket waiting (accept() is a blocking method)
				Socket socket = _socket.accept();
				// client request has arrived, new thread will be generated
				thread = new WorkerThread(socket, _cloudCtrl);
				// generated thread is given to the Thread Pool
				_clientPool.execute(thread);
			} catch (IOException e) {
				// _isAlive == true : client should keep on listening to the socket
				// _isAlive == false : client is on shutdown mode and can stop to
				// listen
				// (so it doesn't throw an exception in case of the
				// _socket.accept() method is interrupted
				if (_isAlive) {
					_cloudCtrl.getPrintStream().println("Problems with client request"	+ e.getMessage());
				}
			}
		}

	}
	
	public HashMap<Character, Integer> getOperators(){
		return thread.getOperators();
	}
	
	public void setUserWatchList(String client, int threshold){
		thread.setUserWatchList(client, threshold);
	}
	
	public void terminate() throws IOException {
		_clientPool.shutdown();
		_isAlive = false;
		_socket.close();
	}

}
