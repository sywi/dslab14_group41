package client;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import util.Config;

public class Client implements IClientCli, Runnable {

	private String componentName;
	private Config config;
	private InputStream userRequestStream;
	private PrintStream userResponseStream;

	private String _host;
	private int _tcpPort;

	private boolean _run;
	private Socket _socket;
	private PrintWriter _writer;
	private BufferedReader _reader;

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
	public Client(String componentName, Config config,
			InputStream userRequestStream, PrintStream userResponseStream) {
		this.componentName = componentName;
		this.config = config;
		this.userRequestStream = userRequestStream;
		this.userResponseStream = userResponseStream;

		readClientProperties();
	}

	@Override
	public void run() {
		buildConnection();
		
		// describes that the client is active, only if exit() is called this boolean is set to false
		_run = true;
		String response;
		BufferedReader consoleReader = new BufferedReader(new InputStreamReader(userRequestStream));


		while (_run) {					
			response = "";
			// console input
			String cmd = "";

			try {
				cmd = consoleReader.readLine();
				String[] splittedCmd = cmd.split(" ");
				
				// handle console input
				if(cmd.startsWith("!login") && splittedCmd.length >= 3) {
					userResponseStream.println(login(splittedCmd[1],splittedCmd[2]));
				} else if(cmd.startsWith("!credits")) {
					userResponseStream.println(credits());
				} else if(cmd.startsWith("!buy")  && splittedCmd.length >= 2) {
					response = buy(Long.parseLong(splittedCmd[1]));
					userResponseStream.println("You now have " + response + " credits.");
				} else if(cmd.startsWith("!compute")) {
					response = compute(cmd);
					userResponseStream.println("Result: " + response);
				} else if(cmd.startsWith("!list")) {
					response = list();
					userResponseStream.println(response);
				} else if(cmd.startsWith("!logout")) {
					response = logout();
					userResponseStream.println(response);
				} else if(cmd.startsWith("!exit")) {
					exit();
				} else {
					userResponseStream.println("No valid command.");
				}
				

			} catch (IOException e) {
				userResponseStream.println("Problems with the connection.");
				buildConnection();
			}
		}
	}
	
	private void buildConnection() {
		try {
			_socket = new Socket(_host, _tcpPort);
			_reader = new BufferedReader(new InputStreamReader(_socket.getInputStream()));
			_writer = new PrintWriter(_socket.getOutputStream(), true);
		} catch (IOException e) {
			userResponseStream.println("Connection to server couldn't be established! Host: " + _host + " TCP Port: " + _tcpPort);
			System.exit(1);
		}
	}

	@Override
	public String login(String username, String password) throws IOException {
		_writer.println("!login " + username + " " + password);

		return _reader.readLine();
	}

	/**
	 * all connections to the server will stay.
	 */
	@Override
	public String logout() throws IOException {
		_writer.println("!logout");

		return _reader.readLine();
	}

	@Override
	public String credits() throws IOException {
		_writer.println("!credits");
		
		return _reader.readLine();
	}

	@Override
	public String buy(long credits) throws IOException {
		_writer.println("!buy " + credits);

		return credits();
	}

	@Override
	public String list() throws IOException {
		_writer.println("!list");

		return _reader.readLine();
	}

	@Override
	public String compute(String term) throws IOException {
		_writer.println(term);
		
		return _reader.readLine();
	}

	/**
	 * close all open connections
	 */
	@Override
	public String exit() throws IOException {
		_run = false;
		_reader.close();
		_writer.close();
		_socket.close();
		userRequestStream.close();
		userResponseStream.close();
		return null;
	}

	/**
	 * @param args
	 *            the first argument is the name of the {@link Client} component
	 */
	public static void main(String[] args) {
		Client client = new Client(args[0], new Config("client"), System.in, System.out);
		ExecutorService e = Executors.newSingleThreadExecutor();
		e.execute(client);
		e.shutdown();
	}

	// --- Commands needed for Lab 2. Please note that you do not have to
	// implement them for the first submission. ---
	@Override
	public String authenticate(String username) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * read client properties from file src/main/resources/client.properties
	 */
	private void readClientProperties() {
		Properties prop = new Properties();
		InputStream input;
		try {
			input = new FileInputStream("src/main/resources/client.properties");
			prop.load(input);
			_host = prop.getProperty("controller.host");
			_tcpPort = Integer.parseInt(prop.getProperty("controller.tcp.port"));
			input.close();
		} catch (IOException e) {
			userResponseStream.println("Couldn't read client properties.");
			e.printStackTrace();
		}
	}

}
