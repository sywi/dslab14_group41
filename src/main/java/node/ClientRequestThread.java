package node;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.StringTokenizer;

public class ClientRequestThread implements Runnable {
	private Socket _socket;
	private String _logDir;
	private String[] _operators;
	private String componentName;
	private PrintStream userResponseStream;
	
	private static final ThreadLocal<DateFormat> DATE_FORMAT = new ThreadLocal<DateFormat>() {
		@Override
		protected DateFormat initialValue() {
			return new SimpleDateFormat("HH:mm:ss.SSS");
		}
	};
	
	public ClientRequestThread(Socket s, String logDir, String[] operators, String componentN, PrintStream userResponseS) {
		_socket = s;
		_logDir = logDir;
		_operators = operators;
		componentName = componentN;
		userResponseStream = userResponseS;
	}

	@Override
	public void run() {
		try {

			BufferedReader reader = new BufferedReader(new InputStreamReader(_socket.getInputStream()));
			
			String input = null;
			LinkedList<Integer> operands = new LinkedList<>();
			LinkedList<Character> operators = new LinkedList<>();
			
			if((input = reader.readLine()) != null) {					
				String in = "";
				StringTokenizer token = new StringTokenizer(input);
				while(token.hasMoreTokens()) {
					
					in = token.nextToken();
					
					if(isOperator(in)) {
						operators.add(in.toCharArray()[0]);
					} else {
						operands.add(Integer.parseInt(in));
					}
				}
			}
			
			String result = null;
							
			// operator and enough operands have arrived
			if(hasOperator(operators.getFirst()) && operands.size() >= 2) 
				result = calculate(operators.getFirst(), operands.get(0), operands.get(1));				
							
			PrintWriter writer = new PrintWriter(_socket.getOutputStream(), true);
			writer.println(result);
			
			writeLogFile(input, result);
			
			_socket.close();
			reader.close();
			writer.close();
		
		} catch (IOException e) {
			e.printStackTrace();
		}
		
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
}


