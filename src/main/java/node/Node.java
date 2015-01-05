package node;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Properties;
import java.util.StringTokenizer;
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
	private int _rmin;
	private String[] _operators;
	private ServerSocket _socket;
	private ExecutorService _executorService;
	private IsAliveThread _isAliveThread;
	private TCPRequestWaiter _clientRequestWaiter;
	private ConsoleInputThread _handleConsoleInputThread;
	private int _currentResourceLevel;
	private int _possibleResourceLevel;
	private File _hmacKeyFile;

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

		sendHelloMsg(_datagramSocket);
		if (ctrlHasRecources(waitForResponse())) {

			// wait for incoming resource asking nodes (TCP connection)
			// _nodeRequestWaiter = new NodeRequestWaiter(_socket);
			// _executorService.execute(_nodeRequestWaiter);

			// send isAlive package
			_isAliveThread = new IsAliveThread(_datagramSocket, _operators,
					_tcpPort, _alive, _udpPort);
			_executorService.execute(_isAliveThread);

			// handle client request
			_clientRequestWaiter = new TCPRequestWaiter(_socket, this);
			_executorService.execute(_clientRequestWaiter);

			// handle console input
			_handleConsoleInputThread = new ConsoleInputThread(this);
			_executorService.execute(_handleConsoleInputThread);
		} else {
			userResponseStream.println("Cloud Controller does not have enougth resources!");
			_executorService.shutdownNow();
			_datagramSocket.close();
			try {
				_socket.close();
				userRequestStream.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			userResponseStream.close();
		}
	}

	// closes all connections
	@Override
	public String exit() throws IOException {
		_handleConsoleInputThread.terminate();
		_isAliveThread.terminate();
		_clientRequestWaiter.terminate();

		try {
			Thread.sleep(_alive * 2);
		} catch (InterruptedException e) {
			userResponseStream.println("Problems during shutdown. "
					+ e.getMessage());
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
		for (String s : _operators) {
			if (s.equals(o.toString()))
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
		Node node = new Node(args[0], new Config(args[0]), System.in, System.out);

		ExecutorService executorService = Executors.newSingleThreadExecutor();
		executorService.execute(node);
		executorService.shutdown();
	}

	// --- Commands needed for Lab 2. Please note that you do not have to
	// implement them for the first submission. ---
	@Override
	public String resources() throws IOException {
		return Integer.toString(_currentResourceLevel);
	}

	private void readNodeProperties() {
		Properties prop = new Properties();
		InputStream input;
		try {
			input = new FileInputStream("src/main/resources/" + componentName
					+ ".properties");
			prop.load(input);
			_host = prop.getProperty("controller.host");
			_tcpPort = Integer.parseInt(prop.getProperty("tcp.port"));
			_alive = Integer.parseInt(prop.getProperty("node.alive"));
			_logDir = prop.getProperty("log.dir");
			_udpPort = Integer
					.parseInt(prop.getProperty("controller.udp.port"));
			_operators = prop.getProperty("node.operators").split("(?!^)");
			_rmin = Integer.parseInt(prop.getProperty("node.rmin"));
			_hmacKeyFile = new File(prop.getProperty("hmac.key"));

		} catch (IOException e) {
			userResponseStream.println("Couldn't read node properties. "
					+ e.getMessage());
		}
	}

	/**
	 * sends !hello message
	 * 
	 * @param socket
	 */
	private void sendHelloMsg(DatagramSocket socket) {
		String msg = "!hello";

		byte[] buffer = msg.getBytes();

		InetAddress receiverAddress;
		try {
			receiverAddress = InetAddress.getLocalHost();
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length,
					receiverAddress, _udpPort);
			socket.send(packet);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * wait for response from CloudController
	 * 
	 * @return message from cloud controller
	 */
	private String waitForResponse() {
		byte[] buffer = new byte[200];
		DatagramPacket _packet = new DatagramPacket(buffer, buffer.length);
		try {
			_datagramSocket.receive(_packet);

			String msg = "";

			for (byte b : buffer)
				msg += (char) b;

			return msg.trim();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * checks if CloudController has enough resources
	 * 
	 * @param msg
	 * @return
	 */
	private boolean ctrlHasRecources(String msg) {
		StringTokenizer tokenizer = new StringTokenizer(msg);
		String s;
		LinkedList<Integer> nodePorts = new LinkedList<>();
		int rmax = 0;

		// separates all active nodes from message 
		while (tokenizer.hasMoreTokens()) {
			s = tokenizer.nextToken();

			// nodes starts with "/"
			if (s.startsWith("/")) {
				String[] a = s.split(":");
				nodePorts.add(Integer.parseInt(a[1]));
			} else {
				rmax = Integer.parseInt(s);
			}
		}

		boolean dividedResourcesEnough;

		if (nodePorts.size() > 0) {
			dividedResourcesEnough = (_rmin <= (rmax / (nodePorts.size() + 1)));
			rmax = rmax / (nodePorts.size() + 1);
		} else {
			dividedResourcesEnough = (_rmin <= rmax);
		}

		// if cloud controller resources divided by number of active nodes would be enough for this node
		// ask all active nodes if they also agree
		if (dividedResourcesEnough) {
			return checkNodes(nodePorts, rmax);
		}

		return false;
	}

	/**
	 * sends !share message to all active nodes
	 * depending on their answer sends !commit or !rollback message
	 * 
	 * @param nodePorts = list of node TCP ports
	 * @param rmax = calculated maximum resource level
	 * @return
	 */
	private boolean checkNodes(LinkedList<Integer> nodePorts, int rmax) {

		boolean rollback = sendShareMsg(nodePorts,rmax);

		String message = "";
		
		// at least one node disagreed with the new resource level
		if (rollback) {
			message = "!rollback";
			
		// all nodes are ok with the new resource level
		} else {
			message = "!commit";
			// new resource level is fix
			setCurrentResourceLevel(rmax);
		}
		
		// send either !rollback or !commit message to all active nodes
		for (Integer node : nodePorts) {
			try {

				Socket socket = new Socket("localhost", node);
				PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);

				writer.println(message);

				socket.close();
				writer.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return !rollback;
	}
	
	/**
	 * sends !shareMsg and returns 
	 * true = not all nodes agreed with the new value
	 * false = all nodes agreed with the new value
	 * 
	 * @param nodePorts
	 * @param rmax
	 * @return
	 */
	private boolean sendShareMsg(LinkedList<Integer> nodePorts, int rmax) {
		boolean rollback = false;
		
		// send !share message to all nodes
		for (Integer node : nodePorts) {
			try {
				Socket socket = new Socket("localhost", node);
				BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				PrintWriter writer = new PrintWriter(socket.getOutputStream(),true);

				writer.println("!share " + rmax);
				String response = reader.readLine();

				// if one node disagrees with the new resource level rollback will be true
				if (response.startsWith("!nok")) 
					rollback = true;

				socket.close();
				reader.close();
				writer.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		return rollback;
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

	public int getRmin() {
		return _rmin;
	}
	
	public void setCurrentResourceLevel(int i) {
		_currentResourceLevel = i;
	}
	
	public void setPossibleResourceLevel(int i) {
		_possibleResourceLevel = i;
	}
	
	public int getPossibleResourceLevel() {
		return _possibleResourceLevel;
	}
	
	public File getHMACKeyFile() {
		return _hmacKeyFile;
	}
}
