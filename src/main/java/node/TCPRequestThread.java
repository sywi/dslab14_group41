package node;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.StringTokenizer;

import model.ComputationRequestInfo;

import javax.crypto.Mac;

import org.bouncycastle.util.encoders.Base64;

import util.Keys;

public class TCPRequestThread implements Runnable {
	private Socket _socket;
	private String _logDir;
	private String[] _operators;
	private String componentName;
	private PrintStream userResponseStream;
	private int _rmin;
	private Node _ctrl;
	
	private static final ThreadLocal<DateFormat> DATE_FORMAT = new ThreadLocal<DateFormat>() {
		@Override
		protected DateFormat initialValue() {
			return new SimpleDateFormat("yyyyMMdd_HHmmss.SSS");
		}
	};
	
	public TCPRequestThread(Socket s, String logDir, String[] operators, String componentN, PrintStream userResponseS, int rmin) {
		_socket = s;
		_logDir = logDir;
		_operators = operators;
		componentName = componentN;
		userResponseStream = userResponseS;
		_rmin = rmin;
	}
	
	public TCPRequestThread(Socket s, Node ctrl) {
		_socket = s;
		_ctrl = ctrl;
		_logDir = _ctrl.getLogDir();
		_operators = _ctrl.getOperatorList();
		componentName = _ctrl.getComponentName();
		userResponseStream = _ctrl.getPrintStream();
		_rmin = _ctrl.getRmin();
	}

	@Override
	public void run() {
		try {
			boolean response = false;
			BufferedReader reader = new BufferedReader(new InputStreamReader(_socket.getInputStream()));
			
			String input = "";
			String result = "";

			input = reader.readLine();
			
			// identify what kind of message has arrived and handle it
			if(input.startsWith("!share")) {
				result = handleNodeRequest(input);
				response = true;
			} else if(input.startsWith("!commit")) {
				// no need to answer on a !commit
				response = false;
				_ctrl.setCurrentResourceLevel(_ctrl.getPossibleResourceLevel());
			} else if(input.startsWith("!rollback")) {
				// no need to answer on a !rollback
				response = false;
			} else {
				result = handleClientRequest(input);
				response = true;
			}
			
			if(response) {
				PrintWriter writer = new PrintWriter(_socket.getOutputStream(), true);
				writer.println(result);
				writer.close();
			}
			
			_socket.close();
			reader.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	private String handleNodeRequest(String input) {
		String in = "";
		StringTokenizer token = new StringTokenizer(input);
		
		while(token.hasMoreTokens()) {	
				
			in = token.nextToken();
			
			if(isNumber(in.toCharArray())) {
				int i = Integer.parseInt(in);
				_ctrl.setPossibleResourceLevel(i);
				
				if(i >= _rmin)
					return "!ok";
				else 
					return "!nok";
			}
		}
		
		
		return null;
	}
	
	private String handleClientRequest(String input) {
		LinkedList<Integer> operands = new LinkedList<>();
		LinkedList<Character> operators = new LinkedList<>();
				
		//filter for operators and operands (they are seperated by a space)
		String in = "";
		StringTokenizer token = new StringTokenizer(input);
				
		while(token.hasMoreTokens()) {	
				
			in = token.nextToken();
			
			if(isOperator(in)) 
				operators.add(in.toCharArray()[0]);
			else if(isNumber(in.toCharArray()) || isNegativeNumber(in))
				operands.add(Integer.parseInt(in));
		}
		
		// filter for the HMAC part of the message (message structure: <hmac> !compute <term>)
		CharSequence hmac = input.subSequence(0, input.indexOf(" !compute"));
		// generate HMAC with the rest of the message (!compute <term>)
		byte[] generatedHmac = generateHMAC(input.substring(input.indexOf("!compute")));

		// decode the filtered HMAC 
		byte[] base64Message = Base64.decode(hmac.toString());

		String decodedHMAC = byteToString(base64Message);
		
		String result = null;
		
		if(byteToString(generatedHmac).equals(decodedHMAC)) {
			// operator and enough operands have arrived
			if(hasOperator(operators.getFirst()) && operands.size() >= 2) 
				result = calculate(operators.getFirst(), operands.get(0), operands.get(1));				
							
			writeLogFile(input.substring(input.indexOf(" !compute ") + 10), result);
			
			// add HMAC to the result string
			result = byteToString(encryptBase64(generateHMAC(result))) + " " + result;
			
		} else {
			result = generatedHmac + " !tempered " + input.substring(input.indexOf(" !compute ") + 10);
			userResponseStream.println("Message from CloudController got tempered!");
		}
	
		return result;
	}

	private String calculate(char operator, int operand1, int operand2) {
		String result;
		
		switch (operator) {
		case '+':
			result = Integer.toString(operand1 + operand2);
			break;
		case '-':
			result = Integer.toString(operand1 - operand2);
			break;
		case '/':
			if(operand2 != 0) {
				float op1 = operand1;
				float op2 = operand2;
				result = Integer.toString(Math.round(op1 / op2));
			} else
				result = "Division with 0!";
			break;
		case '*':
			result = Integer.toString(operand1 * operand2);
			break;
		default:
			result = "No valid operator!";
			break;
		}
		
		return result;
	}
	
	// checks if the given string is a valid operator
	private boolean isOperator(String s) {
		return (s.equals("*") || s.equals("+") || s.equals("-") || s.equals("/"));
	}

	// checks if this node has the given operator
	public boolean hasOperator(Character o) {
		for(String s : _operators) {
			if(s.equals(o.toString()))
				return true;
		}
		return false;
	}
	
	private boolean isNumber(char[] c) {
		
		for(char ch : c) {
			if(!(ch >= '0' && ch <= '9')) {
				return false;
			}
		}
		
		return true;
	}
	
	private void writeLogFile(String input, String result) {
		File logDir = new File("." + File.separator + _logDir);
		File logFile = new File("." + File.separator + _logDir + File.separator + DATE_FORMAT.get().format(new Date()) + "_" + componentName + ".log");
		
		logDir.getParentFile().mkdir();
		logFile.getParentFile().mkdir();
		
		PrintWriter writer;
		try {
			writer = new PrintWriter("." + File.separator + _logDir + File.separator + DATE_FORMAT.get().format(new Date()) + "_" + componentName + ".log", "UTF-8");
			writer.println(input);
			writer.println(result);
			writer.close();
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			userResponseStream.println("Problems while writing to log file: " + e.getMessage());
		}

	}
	
	public ComputationRequestInfo getLogs() throws IOException{
		ComputationRequestInfo zruck = new ComputationRequestInfo();
		zruck.setComponentName(componentName);
		HashMap<String, String> zruckMap = new HashMap<String, String>();
		File[] logs = new File(_logDir).listFiles();
		for(int i = 0;i<logs.length;i++){
			String key = logs[i].getName();
			BufferedReader bufReader = new BufferedReader(new FileReader(logs[i]));
			String operation = "";
			operation += bufReader.readLine();
			operation += " = ";
			operation += bufReader.readLine();
			zruckMap.put(key, operation);
			bufReader.close();
		}
		zruck.setDateResult(zruckMap);
		
		return zruck;
	}
	
	private byte[] encryptBase64(byte[] msg) {
		byte[] base64Msg = Base64.encode(msg);
		
		return base64Msg;
	}
	
	private byte[] generateHMAC(String msg) {
		Key secretKey;
		byte[] hash = null;
		try {
			secretKey = Keys.readSecretKey(_ctrl.getHMACKeyFile());

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
	
	private boolean isNegativeNumber(String s) {
		for(char c : s.toCharArray()) {
			if(!(c >= '0' && c <= '9' || c == '-')) 
				return false;
		}
		
		return true;
	}
}


