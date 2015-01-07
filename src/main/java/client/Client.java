package client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.Key;
import java.security.Security;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Base64;

import util.Config;
import util.EncryptionUtils;
import util.Keys;

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
	private String _controllerKeyPath;
	private String _keysDir;
	
	private IvParameterSpec iv = null;
	private SecretKey secretKey = null;

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
				} else if (cmd.startsWith("!authenticate")) {
					response = authenticate(splittedCmd[1]);
					userResponseStream.println(response);
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
			authenticate(componentName);

		} catch (IOException e) {
			userResponseStream.println("Connection to server couldn't be established! Host: " + _host + " TCP Port: " + _tcpPort);
			System.exit(1);
		}
	}

	@Override
	public String login(String username, String password) throws IOException {
		String encryptedMsg = EncryptionUtils.cryptAES(1, secretKey, iv, "!login " + username + " " + password);
		String encryptedMsg64 = new String(Base64.encode(encryptedMsg.getBytes()));
		_writer.println(encryptedMsg64);

		String responseAES64 = _reader.readLine();
		String responseAES = new String(Base64.decode(responseAES64.getBytes()));

		return EncryptionUtils.cryptAES(2, secretKey, iv, responseAES);
	}

	/**
	 * all connections to the server will stay.
	 */
	@Override
	public String logout() throws IOException {
		String encryptedMsg = util.EncryptionUtils.cryptAES(1, secretKey, iv, "!logout");
		String encryptedMsg64 = new String(Base64.encode(encryptedMsg.getBytes()));
		_writer.println(encryptedMsg64);

		String responseAES64 = _reader.readLine();
		String responseAES = new String(Base64.decode(responseAES64.getBytes()));

		return util.EncryptionUtils.cryptAES(2, secretKey, iv, responseAES);
	}

	@Override
	public String credits() throws IOException {
		String encryptedMsg = util.EncryptionUtils.cryptAES(1, secretKey, iv, "!credits");
		String encryptedMsg64 = new String(Base64.encode(encryptedMsg.getBytes()));
		_writer.println(encryptedMsg64);

		String responseAES64 = _reader.readLine();
		String responseAES = new String(Base64.decode(responseAES64.getBytes()));

		return util.EncryptionUtils.cryptAES(2, secretKey, iv, responseAES);
	}

	@Override
	public String buy(long credits) throws IOException {
		String encryptedMsg = util.EncryptionUtils.cryptAES(1, secretKey, iv,"!buy " + credits);
		String encryptedMsg64 = new String(Base64.encode(encryptedMsg.getBytes()));
		_writer.println(encryptedMsg64);

		String responseAES64 = _reader.readLine();
		String responseAES = new String(Base64.decode(responseAES64.getBytes()));

		return util.EncryptionUtils.cryptAES(2, secretKey, iv, responseAES);
	}

	@Override
	public String list() throws IOException {
		String encryptedMsg = util.EncryptionUtils.cryptAES(1, secretKey, iv,"!list");
		String encryptedMsg64 = new String(Base64.encode(encryptedMsg.getBytes()));
		_writer.println(encryptedMsg64);

		String responseAES64 = _reader.readLine();
		String responseAES = new String(Base64.decode(responseAES64.getBytes()));

		return util.EncryptionUtils.cryptAES(2, secretKey, iv, responseAES);
	}

	@Override
	public String compute(String term) throws IOException {
		String encryptedMsg = util.EncryptionUtils.cryptAES(1, secretKey, iv,term);
		String encryptedMsg64 = new String(Base64.encode(encryptedMsg.getBytes()));
		_writer.println(encryptedMsg64);

		String responseAES64 = _reader.readLine();
		String responseAES = new String(Base64.decode(responseAES64.getBytes()));

		return util.EncryptionUtils.cryptAES(2, secretKey, iv, responseAES);
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

	//TODO: go through...
	// --- Commands needed for Lab 2. Please note that you do not have to
	// implement them for the first submission. ---
	@Override
	public String authenticate(String username) throws IOException {
		final byte[] clientChallenge = util.EncryptionUtils.createSecureRandom();
		String clientChallenge64 = new String(Base64.encode(clientChallenge));

		File pemFile = new File(_controllerKeyPath);

		Key key = Keys.readPublicPEM(pemFile);
		String cipheredMsg = EncryptionUtils.encryptRSA(key, "!authenticate " + username + " " + clientChallenge64);

		String cipheredMsg64 = new String(Base64.encode(cipheredMsg.getBytes()));

		// send first message: !authenticate <user> <client-challenge>
		_writer.println(cipheredMsg64);

		// receive second message: !ok <client-challenge> <controller-challenge>
		// <secret-key> <iv-parameter>
		String encryptedControllerResponse64 = _reader.readLine();
		String encryptedControllerResponse = new String(Base64.decode(encryptedControllerResponse64.getBytes()));
		String controllerResponse = util.EncryptionUtils.decryptRSA(_keysDir + "/" + username + ".pem", encryptedControllerResponse);

		if (controllerResponse.contains("!ok")) {
			String[] splitted = controllerResponse.split(" ");
			byte[] returnedClientChallenge = Base64.decode(splitted[1].getBytes());
			
			if (returnedClientChallenge == clientChallenge) {
				byte[] secKey = Base64.decode(splitted[3]);
				secretKey = new SecretKeySpec(secKey, 0, secKey.length, "AES");
				byte[] ivArr = Base64.decode(splitted[4]);
				iv = new IvParameterSpec(ivArr);
				String encryptedMsg = util.EncryptionUtils.cryptAES(1,secretKey, iv, splitted[2]);
				String encryptedMsg64 = new String(Base64.encode(encryptedMsg.getBytes()));
				// send third message: <controller-challenge>
				_writer.println(encryptedMsg64);
			} else {
				System.out.println("Client challenges don't match!");
			}
		}

		return "Successfully authenticated.";

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
			_controllerKeyPath = prop.getProperty("controller.key");
			_keysDir = prop.getProperty("keys.dir");
			input.close();
		} catch (IOException e) {
			userResponseStream.println("Couldn't read client properties.");
			e.printStackTrace();
		}
	}

}
