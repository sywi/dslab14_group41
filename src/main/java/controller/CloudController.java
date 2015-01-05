package controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import util.Config;

public class CloudController implements ICloudControllerCli, Runnable {

	private String componentName;
	private Config config;
	private InputStream userRequestStream;
	private PrintStream userResponseStream;

	private int _tcpPort;
	private int _udpPort;
	private int _timeout;
	private int _checkPeriod;
	private int _rmax;
	private LinkedList<User> _clients;
	private ConcurrentHashMap<Integer, Node> _nodes;
	private ExecutorService _executorService;
	private ClientRequestWaiterCloudCtrl _clientRequestWaiter;
	private NodePackageWaiter _nodePackageWaiter;
	private NodeAliveCtrl _nodeAliveCtrl;
	private ConsoleInputCloudCtrl _consoleInput;
	private File _hmacKeyFile;

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
	public CloudController(String componentName, Config config,
			InputStream userRequestStream, PrintStream userResponseStream) {
		this.componentName = componentName;
		this.config = config;
		this.userRequestStream = userRequestStream;
		this.userResponseStream = userResponseStream;
		// read properties
		readCtrlProperties();
		readClientProperties();
	}

	@Override
	public void run() {
		
		// initiating new Thread Pool
		_executorService = Executors.newCachedThreadPool();
		
		
		// handling client requests
		_clientRequestWaiter = new ClientRequestWaiterCloudCtrl(_tcpPort, this);
		_executorService.execute(_clientRequestWaiter);
		
		
		// waiting for packages from nodes
		_nodes = new ConcurrentHashMap<>();
		_nodePackageWaiter = new NodePackageWaiter(this, _udpPort);
		_executorService.execute(_nodePackageWaiter);
				
		// check if every node is alive
		_nodeAliveCtrl = new NodeAliveCtrl(this);
		_executorService.execute(_nodeAliveCtrl);
		
		// handle console input
		_consoleInput = new ConsoleInputCloudCtrl(this);
		_executorService.execute(_consoleInput);

	}

	@Override
	public String nodes() throws IOException {
		StringBuilder builder = new StringBuilder();
		int i = 1;
		Node currNode;
		
		for (Integer key : _nodes.keySet()) {
			currNode = _nodes.get(key);
			builder.append(i++ + ". IP: " + currNode.getAddress() + " Port: " + currNode.getTcpPort());
			if(currNode.isAlive())
				builder.append(" online");
			else
				builder.append(" offline");
			
			builder.append(" Usage: " + currNode.getUsage() + "\n");
		}
		return builder.toString();
	}

	@Override
	public String users() throws IOException {
		StringBuilder builder = new StringBuilder();
		
		for(User u : _clients) {
			builder.append(u.getName() + " ");
			
			if(u.isActive())
				builder.append("online");
			else
				builder.append("offline");
			
			builder.append(" Credits: " + u.getCredits() + "\n");
		}
		
		return builder.toString();
	}

	@Override
	public String exit() throws IOException {
		_clientRequestWaiter.terminate();
		_nodePackageWaiter.terminate();
		_nodeAliveCtrl.terminate();
		_consoleInput.terminate();
		

		try {
			Thread.sleep(_checkPeriod);
		} catch (InterruptedException e) {
			userResponseStream.println("Problems during shutdown. " + e.getMessage());
		}
				
		userRequestStream.close();
		userResponseStream.close();
		_executorService.shutdownNow();


		return null;
	}

	/**
	 * @param args
	 *            the first argument is the name of the {@link CloudController}
	 *            component
	 */
	public static void main(String[] args) {
		CloudController cloudController = new CloudController(args[0],
				new Config("controller"), System.in, System.out);
		
		ExecutorService executorService = Executors.newSingleThreadExecutor();
		executorService.execute(cloudController);
		executorService.shutdown();
	}

	private void readCtrlProperties() {
		Properties prop = new Properties();
		try {
			InputStream input = new FileInputStream("src/main/resources/controller.properties");
			prop.load(input);

			_tcpPort = Integer.parseInt(prop.getProperty("tcp.port"));
			_udpPort = Integer.parseInt(prop.getProperty("udp.port"));
			_timeout = Integer.parseInt(prop.getProperty("node.timeout"));
			_checkPeriod = Integer.parseInt(prop.getProperty("node.checkPeriod"));
			_rmax = Integer.parseInt(prop.getProperty("controller.rmax"));
			_hmacKeyFile = new File(prop.getProperty("hmac.key"));
		} catch (IOException e) {
			userResponseStream.println("Couldn't read cloud controller properties. " + e.getMessage());
		}
	}

	private void readClientProperties() {

		Properties prop = new Properties();
		_clients = new LinkedList<>();

		try {
			InputStream input = new FileInputStream("src/main/resources/user.properties");
			prop.load(input);

			String name = "";
			String password = "";
			int credits;

			// iterating through the property file
			Enumeration<?> e = prop.propertyNames();
			while (e.hasMoreElements()) {
				// get the next element
				String key = (String) e.nextElement();

				// new user found
				if (key.contains(".password")) {
					name = key.replaceAll(".password", "");
					password = prop.getProperty(name + ".password");
					credits = Integer.parseInt(prop.getProperty(name + ".credits"));
					_clients.add(new User(name, password, credits));
				}
			}
		} catch (IOException e) {
			userResponseStream.println("Couldn't read client properties. " + e.getMessage());
		}
	}

	
	public PrintStream getPrintStream() {
		return userResponseStream;
	}
	
	public InputStream getInputStream() {
		return userRequestStream;
	}

	public ConcurrentHashMap<Integer, Node> getNodes() {
		return _nodes;
	}
	
	public int getTimeout() {
		return _timeout;
	}
	
	public int getCheckperiod() {
		return _checkPeriod;
	}
	
	public LinkedList<User> getClients() {
		return _clients;
	}
	
	public int getRmax() {
		return _rmax;
	}
	
	public File getHmacKeyFile() {
		return _hmacKeyFile;
	}
	
}
