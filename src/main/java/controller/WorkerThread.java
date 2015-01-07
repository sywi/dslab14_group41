package controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.rmi.RemoteException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import javax.crypto.Mac;

import model.ComputationRequestInfo;

import org.bouncycastle.util.encoders.Base64;

import admin.INotificationCallback;
import util.Keys;

public class WorkerThread implements Runnable {
	
	private Socket _socket;
	private User _user;
	private CloudController _ctrl;
	private HashMap<Character, Integer> _operators;
	private HashMap<String, Integer> _userWatchList;
	private INotificationCallback callback;
	
	protected WorkerThread(Socket socket, CloudController ctrl) {
		_socket = socket;
		_ctrl = ctrl;
		HashMap<Character, Integer> _operators = new HashMap<>();
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
		String cmd;

		while (alive) {
			try {
				cmd = reader.readLine();
				if (cmd == null)
					alive = false;

				if (cmd != null) {
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
	
	private String compute(String cmd) throws RemoteException {

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
						
						if(_operators.containsKey(operator)){
							int value = (int) _operators.get(operator);
							value++;
							_operators.put(operator, value);
						} else {
							_operators.put(operator, 1);
						}

						int operand1;

						if (tempResult != null)
							operand1 = tempResult;
						else
							operand1 = operands.get(i++);

						int operand2 = operands.get(i++);

						// build message
						String msg = "!compute " + Integer.toString(operand1) + " " + operator + " " + Integer.toString(operand2);

						// add HMAC to message
					//	msg = byteToString(encryptBase64(generateHMAC(msg))) + " " + msg;

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

//						if(tempResult == null && hmacCheck) {
//							hmacCheck = checkHMAC(hmac, warmingMsg);
//						} else if(hmacCheck)
//							hmacCheck = checkHMAC(hmac, Integer.toString(tempResult));

						
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
			// TODO rückgabe fnalisieren
			if(_userWatchList.containsKey(_user.getName())){
				if(_userWatchList.get(_user.getName())<_user.getCredits()){
					callback.notify(_user.getName(), _userWatchList.get(_user.getName()));
				}
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
	
	public HashMap getOperators(){
		return _operators;
	}
	public void setUserWatchList(String client, int threshold, INotificationCallback callback){
		_userWatchList.put(client, threshold);
		this.callback=callback;
	}

	public List<ComputationRequestInfo> getLogs() throws IOException, ClassNotFoundException {
		List<ComputationRequestInfo> logs = new LinkedList<ComputationRequestInfo>();
		LinkedList<Node> nodes = new LinkedList<>();
		for (Integer key : _ctrl.getNodes().keySet()) {
			nodes.add(_ctrl.getNodes().get(key));
		}
		for (Node n : nodes) {
		Socket socket = new Socket(n.getAddress(), n.getTcpPort());
		PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
		BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		ObjectInputStream is = new ObjectInputStream( socket.getInputStream());
		
		writer.println("!logs");
		logs.add((ComputationRequestInfo) is.readObject());
		socket.close();
		reader.close();
		
//		OutputStream os = socket.getOutputStream();
//		ObjectOutputStream oos = new ObjectOutputStream(os);
//		PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
//		BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		}
		return logs;
	}
}