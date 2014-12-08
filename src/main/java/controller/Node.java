package controller;

import java.net.InetAddress;
import java.util.Date;
import java.util.LinkedList;

public class Node {
	private LinkedList<Character> _operators;
	private int _tcpPort;
	private Date _lastAliveMsg;
	private boolean _isAlive;
	private int _usage;
	private InetAddress _address;
	
	public Node() {
		_operators = new LinkedList<>();
	}
	
	public void setTcpPort(int tcpPort) {
		_tcpPort = tcpPort;
	}
	
	public int getTcpPort() {
		return _tcpPort;
	}
	
	public void setDate(Date lastAliveMsg) {
		_lastAliveMsg = lastAliveMsg;
	}
	
	public Date getDate() {
		return _lastAliveMsg;
	}
	
	public void addOperator(char c) {
		if(isOperator(c))
			_operators.add(c);
	}
	
	public boolean hasOperator(char c) {
		for(char ch : _operators) {
			if(c == ch)
				return true;
		}
		return false;
	}
	
	public String getOperators() {
		String operators = "";
		
		for(Character c : _operators) 
			operators += c;
		
		return operators;
	}
	
	private boolean isOperator(char s) {
		return (s == '*' || s == '+' || s == '-' || s == '/');
	}
	
	public boolean isAlive() {
		return _isAlive;
	}
	
	public void setIsAlive(boolean b) {
		_isAlive = b;
	}
	
	public void setUsage(int i) {
		_usage = i;
	}
	
	public int getUsage() {
		return _usage;
	}
	
	public void setAddress(InetAddress address) {
		_address = address;
	}
	
	public InetAddress getAddress() {
		return _address;
	}
}
