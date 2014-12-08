package node;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import util.Config;

public class Node implements INodeCli, Runnable {

	private String componentName;
	private Config config;
	private InputStream userRequestStream;
	private PrintStream userResponseStream;	
	private String _logDir;
	private String _host;
	private int _tcpPort;
	private int _udpPort;
	private int _alive;
	private String[] _operators;
	private ServerSocket _socket;
	private ExecutorService _executorService;
	private IsAliveThread _isAliveThread;
	private ClientRequestWaiter _clientRequestWaiter;
	private ConsoleInputThread _handleConsoleInputThread;
	
	private DatagramSocket _datagramSocket;

	/**
	 * @param componentName
	 *            the name of the component - represented in the prompt
	 * @param config
	 *            the configuration to use
	 * @param userRequestStream
	 *            the input stream to read user input from
	 * @param userResponseStream
	 *            the output stream to write the console output to
	 */
	public Node(String componentName, Config config,
			InputStream userRequestStream, PrintStream userResponseStream) {
		this.componentName = componentName;
		this.config = config;
		this.userRequestStream = userRequestStream;
		this.userResponseStream = userResponseStream;

		readNodeProperties();
	}
	
	
	@Override
	public void run() {
		
		// creating new thread pool
		_executorService = Executors.newCachedThreadPool();
		
		// establish connections
		try {
			// socket for client requests
			_socket = new ServerSocket(_tcpPort);
			// datagram socket to send isAlive package to CloudCtrl
			_datagramSocket = new DatagramSocket();
		} catch (IOException e1) {
			userResponseStream.println("Could not establish connection.");
			e1.printStackTrace();
		}
		
		// send isAlive package
		_isAliveThread = new IsAliveThread(_datagramSocket, _operators, _tcpPort, _alive, _udpPort);
		_executorService.execute(_isAliveThread);
				

		// handle client request
		_clientRequestWaiter = new ClientRequestWaiter(_socket, this);
		_executorService.execute(_clientRequestWaiter);

		
		// handle console input		
		_handleConsoleInputThread = new ConsoleInputThread(this);
		_executorService.execute(_handleConsoleInputThread);

	}

	// closes all connections
	@Override
	public String exit() throws IOException {
		_handleConsoleInputThread.terminate();
		_isAliveThread.terminate();
		_clientRequestWaiter.terminate();
		
		try {
			Thread.sleep(_alive*2);
		} catch (InterruptedException e) {
			userResponseStream.println("Problems during shutdown. " + e.getMessage());
		}
		_executorService.shutdownNow();
		_datagramSocket.close();
		_socket.close();
		userRequestStream.close();
		userResponseStream.close();
		
		return null;
	}

	@Override
	public String history(int numberOfRequests) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}
	
	// checks if this node has the given operator
	public boolean hasOperator(Character o) {
		for(String s : _operators) {
			if(s.equals(o.toString()))
				return true;
		}
		return false;
	}

	/**
	 * @param args
	 *            the first argument is the name of the {@link Node} component,
	 *            which also represents the name of the configuration
	 */
	public static void main(String[] args) {
		Node node = new Node(args[0], new Config(args[0]), System.in,
				System.out);
				
		ExecutorService executorService = Executors.newSingleThreadExecutor();
		executorService.execute(node);
		executorService.shutdown();
	}

	// --- Commands needed for Lab 2. Please note that you do not have to
	// implement them for the first submission. ---
	@Override
	public String resources() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}
	
	private void readNodeProperties() {
		Properties prop = new Properties();
		InputStream input;
		try {
			input = new FileInputStream("src/main/resources/" + componentName + ".properties");
			prop.load(input);
			_host = prop.getProperty("controller.host");
			_tcpPort = Integer.parseInt(prop.getProperty("tcp.port"));
			_alive = Integer.parseInt(prop.getProperty("node.alive"));
			_logDir = prop.getProperty("log.dir");
			_udpPort = Integer.parseInt(prop.getProperty("controller.udp.port"));
			_operators = prop.getProperty("node.operators").split("(?!^)");
		} catch (IOException e) {
			userResponseStream.println("Couldn't read node properties. " + e.getMessage());
		}
	}
	
	public String getLogDir() {
		return _logDir;
	}
	
	public PrintStream getPrintStream() {
		return userResponseStream;
	}
	
	public String[] getOperatorList() {
		return _operators;
	}
	
	public String getComponentName() {
		return componentName;
	}

	public InputStream getInputStream() {
		return userRequestStream;
	}
}
