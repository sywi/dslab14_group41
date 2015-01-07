package controller;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.LinkedList;
import java.util.StringTokenizer;

import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import org.bouncycastle.util.encoders.Base64;

import util.EncryptionUtils;
import util.Keys;

public class WorkerThread implements Runnable {
	
	private Socket _socket;
	private User _user;
	private CloudController _ctrl;
	private boolean authenticated = false;
	private IvParameterSpec iv = null;
	private SecretKey secretKey = null;
	
	protected WorkerThread(Socket socket, CloudController ctrl) {
		_socket = socket;
		_ctrl = ctrl;
	}

	@Override
	public void run() {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(_socket.getInputStream()));
			PrintWriter writer = new PrintWriter(_socket.getOutputStream(), true);
			executeRequest(reader, writer);
		} catch (IOException e) {
			_ctrl.getPrintStream().println("Could not establish conntection: " + e.getMessage());
		}
	}
	
	
	private void executeRequest(BufferedReader reader, PrintWriter writer) {
		boolean alive = true;
		String cmdAES64;

		while (alive) {
			try {
				cmdAES64 = reader.readLine();
				if (cmdAES64 == null)
					alive = false;
				
				if (!authenticated) {
					// receive first message: !authenticate <user>
					// <client-challenge>
					String auth = authenticate(cmdAES64, reader, writer);
					if (auth.equals("success")) {
						authenticated = true;
						continue;
					}
				}

				if (cmdAES64 != null && authenticated) {
					String cmdAES = new String(Base64.decode(cmdAES64.getBytes()));
					String cmd = util.EncryptionUtils.cryptAES(2, secretKey,iv, cmdAES);
					String[] splittedCmd = cmd.split(" ");

					if (_user == null && !splittedCmd[0].equals("!login")) {
						writer.println("Only for logged in users.");
					} else {

						switch (splittedCmd[0]) {
						case "!login":
							if (_user == null) {
								_user = login(splittedCmd[1],splittedCmd[2]);
								
								if (_user != null) 
									writer.println("Successfully logged in.");
								else if (_user == null) 
									writer.println("Wrong username or password.");
								
							} else {
								if (_user.isActive())
									writer.println("You are already logged in!");
								else {
									login(splittedCmd[1], splittedCmd[2]);
									
									if (_user != null) 
										writer.println("Welcome back :)");
									else
										writer.println("Wrong username or password.");
									
								}
							}
							break;

						case "!credits":
							if (_user.isActive())
								writer.println("You have "
										+ _user.getCredits()
										+ " credits left.");
							else
								writer.println("Only for logged in users.");

							break;

						case "!buy":
							if (_user.isActive())
								_user.setCredits(_user.getCredits()
										+ Integer.parseInt(splittedCmd[1]));

							break;

						case "!list":
							writer.println(list());
							break;

						case "!compute":
							if (_user.isActive())
								writer.println(compute(cmd));

							break;

						case "!logout":
							_user.logout();
							writer.println("Goodbye :)");
							break;

						case "!exit":
							_user.logout();
							writer.close();
							reader.close();
							_socket.close();
							alive = false;
							break;

						default:
							writer.println("No vailid command: "
									+ splittedCmd[0]);
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private String authenticate(String encryptedCmd64, BufferedReader reader, PrintWriter writer) {
		String encryptedCmd = new String(Base64.encode(encryptedCmd64.getBytes()));
		String cmd = util.EncryptionUtils.decryptRSA(_ctrl.getKeyFilePath(), encryptedCmd);

		if (cmd.contains("!authenticate")) {
			String[] splittedCmd = cmd.split(" ");
			String clientChallenge64 = splittedCmd[2];
			String controllerChallenge64 = new String(
					Base64.encode(util.EncryptionUtils.createSecureRandom()));

			KeyGenerator generator;
			try {
				generator = KeyGenerator.getInstance("AES");
				generator.init(256);
				secretKey = generator.generateKey();
				String secretKey64 = new String(encryptBase64(secretKey.getEncoded()));

				SecureRandom random = new SecureRandom();
				byte[] ivBytes = new byte[16];
				random.nextBytes(ivBytes);
				iv = new IvParameterSpec(ivBytes);
				String iv64 = new String(
						encryptBase64(iv.toString().getBytes()));

				//wieso client??
				String controllerResponse = EncryptionUtils.encryptRSA(Keys.readPublicPEM(new File("keys/client/" + _user.getName() + ".pub.pem")), "!ok "
								+ clientChallenge64 + " "
								+ controllerChallenge64 + " " + secretKey64
								+ " " + iv64);
				String controllerResponse64 = new String(Base64.encode(controllerResponse.getBytes()));
				// send second message: !ok <client-challenge>
				// <controller-challenge> <secret-key> <iv-parameter>
				writer.println(controllerResponse64);

				// receive third message: <controller-challenge>
				String clientResponse64AES64 = reader.readLine();
				String clientResponse64AES = new String(Base64.encode(clientResponse64AES64.getBytes()));
				String clientResponse64 = util.EncryptionUtils.cryptAES(2,secretKey, iv, clientResponse64AES);

				if (clientResponse64.equals(controllerChallenge64)) {
					return "success";
				} else {
					return "fail";
				}

			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} else {
			return "no !authenticate command found";
		}
		return "fail";
	}

	
	private String compute(String cmd) {

		cmd = cmd.replace("!compute", "");
		LinkedList<Integer> operands = new LinkedList<>();
		LinkedList<Character> operators = new LinkedList<>();
		
		String s = "";
		StringTokenizer token = new StringTokenizer(cmd);
		while(token.hasMoreTokens()) {
			s = token.nextToken();
			if(isOperator(s)) 
				operators.add(s.toCharArray()[0]);
			else 
				operands.add(Integer.parseInt(s));
		}
		
		int i = 0;
		Integer tempResult = null;
		String result = null;
		boolean hmacCheck = true;
		
		if (_user.getCredits() >= operators.size() * 50) {
			// iterating through all opertors
			for (Character currOperator : operators) {
				if(hmacCheck) {

				Node n = getNode(currOperator);

				if (n != null) {
					try {
						Socket socket = new Socket(n.getAddress(), n.getTcpPort());
						PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
						BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

						char operator = currOperator;

						int operand1;

						if (tempResult != null)
							operand1 = tempResult;
						else
							operand1 = operands.get(i++);

						int operand2 = operands.get(i++);

						// build message
						String msg = "!compute " + Integer.toString(operand1) + " " + operator + " " + Integer.toString(operand2);

						// add HMAC to message
						msg = byteToString(encryptBase64(generateHMAC(msg))) + " " + msg;

						writer.println(msg);

						String temp = "";
						String hmac = "";
						String warmingMsg = "";
						String input = reader.readLine();
						StringTokenizer tokenizer = new StringTokenizer(input);
						
						while(tokenizer.hasMoreTokens()) {
							
							temp = tokenizer.nextToken();
							
							if(isNumber(temp)) {
								tempResult = Integer.parseInt(temp);
								int k = n.getUsage() + (String.valueOf(tempResult).length() * 50);
								n.setUsage(k);
							} else if(isNegativeNumber(temp)) {
								tempResult = Integer.parseInt(temp);
								int k = n.getUsage() + (String.valueOf(tempResult).replace("-", "").length() * 50);
								n.setUsage(k);
							} else if(temp.contains("!tempered")) {
								hmacCheck = false;		
							} else if(temp.contains("0!") || temp.contains("operator!")) {
								warmingMsg = input.substring(input.indexOf(" "), input.length()).trim();
								result = warmingMsg;
							} else if(input.startsWith(temp)){
								hmac = temp;
							}
						}

						if(tempResult == null && hmacCheck) {
							hmacCheck = checkHMAC(hmac, warmingMsg);
						} else if(hmacCheck)
							hmacCheck = checkHMAC(hmac, Integer.toString(tempResult));

						
						writer.close();
						reader.close();
						socket.close();
						
					} catch (IOException e) {
						_ctrl.getPrintStream().println("Problems with user request " + e.getMessage());
					}
				}
				} else {
					return "No node for operation " + currOperator + " found.";
				}
				
			}
			
			if(hmacCheck) {
				if(result == null) 
					result = Integer.toString(tempResult);

				_user.setCredits(_user.getCredits()-(operators.size()*50));
			} else {
				_ctrl.getPrintStream().println("Message from Node got tempered!");
				result = "Message got tempered!";
			}

		} else {
			result = "Operation could not be done. Not enough credits.";
		}
		return result;
	}
	
	private boolean isOperator(String s) {
		return (s.equals("*") || s.equals("+") || s.equals("-") || s.equals("/"));
	}
	
	private boolean isNumber(String s) {
		for(char c : s.toCharArray()) {
			if(!(c >= '0' && c <= '9')) 
				return false;
		}
		
		return true;
	}
	
	private boolean isNegativeNumber(String s) {
		for(char c : s.toCharArray()) {
			if(!(c >= '0' && c <= '9' || c == '-')) 
				return false;
		}
		
		return true;
	}
	

	public String list() {
		LinkedList<Character> availableOperators = new LinkedList<>();
		
		for (Integer key : _ctrl.getNodes().keySet()) {
			char[] s = _ctrl.getNodes().get(key).getOperators().toCharArray();
			for(int i = 0; i < s.length; i ++) {
				if(!availableOperators.contains(s[i]))
					availableOperators.add(s[i]);
			}
		}
		
		if(availableOperators.isEmpty())
			return "No operators available.";
		
		return availableOperators.toString();
	}
	
	//method used by WorkerThread
	public User login(String username, String password) {
		for (User u : _ctrl.getClients()) {
			if (username.equals(u.getName())) {
				if (u.login(password)) 
					return u;
			}
		}
		return null;
	}
	
	//method used by WorkerThread
	public Node getNode(Character c) {
		LinkedList<Node> nodes = new LinkedList<>();

		for (Integer key : _ctrl.getNodes().keySet()) {
			if (_ctrl.getNodes().get(key).hasOperator(c)) {
				if (_ctrl.getNodes().get(key).isAlive())
					nodes.add(_ctrl.getNodes().get(key));
			}
		}

		Node minUsage = null;

		if (!nodes.isEmpty()) {
			minUsage = nodes.get(0);

			for (Node n : nodes) {
				if (n.getUsage() < minUsage.getUsage())
					minUsage = n;
			}
		}

		return minUsage;
	}
	
	private byte[] encryptBase64(byte[] msg) {
		byte[] base64Msg = Base64.encode(msg);
		
		return base64Msg;
	}
	
	private byte[] generateHMAC(String msg) {
		Key secretKey;
		byte[] hash = null;
		try {
			secretKey = Keys.readSecretKey(_ctrl.getHmacKeyFile());

			// create HMAC with secret key and message
			Mac hMac = Mac.getInstance(secretKey.getAlgorithm());
			hMac.init(secretKey);
			hMac.update(msg.getBytes());
			hash = hMac.doFinal();

		} catch (IOException | InvalidKeyException | NoSuchAlgorithmException e1) {
			System.out.println("Problems during creating HMAC: " + e1.getMessage());
		}
		
		return hash;
	}
	
	private String byteToString(byte[] byteA) {
		StringBuilder builder = new StringBuilder();
		
		for(byte b : byteA)
			builder.append((char) b);
		
		return builder.toString();
	}
	
	private boolean checkHMAC(String hmac, String msg) {
		byte[] generatedHmac = generateHMAC(msg);

		// decode the filtered HMAC 
		byte[] base64Message = Base64.decode(hmac);

		String decodedHMAC = byteToString(base64Message);
				
		if(byteToString(generatedHmac).equals(decodedHMAC))
			return true;

		return false;
	}
}