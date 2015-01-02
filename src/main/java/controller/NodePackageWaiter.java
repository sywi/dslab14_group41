package controller;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.Date;

public class NodePackageWaiter implements Runnable {
	
	private boolean _isAlive;
	private CloudController _ctrl;
	private DatagramSocket _datagramSocket;
	private int _udpPort;
	private DatagramPacket _packet;
	
	public NodePackageWaiter(CloudController ctrl, int udpPort) {
		_isAlive = true;
		_ctrl = ctrl;
		_udpPort = udpPort;
	}

	@Override
	public void run() {
		
		try {
			_datagramSocket = new DatagramSocket(_udpPort);
		} catch (SocketException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		while (_isAlive) {
			try {

				// socket waiting (receive(p) is a blocking method)
				byte[] buffer = new byte[30];
				_packet = new DatagramPacket(buffer, buffer.length);
				_datagramSocket.receive(_packet);
				
				// handle the incoming message
				handleMsg(buffer, _packet.getSocketAddress());

			} catch (IOException e) {
				if (_isAlive)
					e.printStackTrace();
			}
		}
	}
	
	public void terminate() {
		_isAlive = false;
		_datagramSocket.close();
	}
	
	private void handleMsg(byte[] buffer, SocketAddress address) {
		String msg = "";
		
		for(byte b : buffer) 
			msg += (char) b;
		
		msg.trim();

		// hello message from Two-phase-commit
		if(msg.contains("!hello")) {
			sendResponseMsg(address);		
		// alive message
		} else {
			String tcpPort = "";
			char[] operators = new char[3];
			int operatorCount = 0;
			
			for(Character c : msg.toCharArray()) {
				if(c >= '0' && c <= '9')
					tcpPort += c;
				else if(isOperator(c))
					operators[operatorCount++] = c;
			}
			updateNodeInformation(tcpPort, operators);
		}
			
	}
	
	private void updateNodeInformation(String tcpPort, char[] operators) {
		if (!_ctrl.getNodes().containsKey(Integer.parseInt(tcpPort))) {
			Node n = new Node();
			// set time stamp
			n.setDate(new Date());
			n.setIsAlive(true);
			n.setTcpPort(Integer.parseInt(tcpPort));
			n.setAddress(_packet.getAddress());

			for(char c : operators) {
				n.addOperator(c);
			}

			// put the new node to the HashMap
			_ctrl.getNodes().put(Integer.parseInt(tcpPort), n);

		} else {
			// this node already exists, only the time stamp has to be
			// renewed
			_ctrl.getNodes().get(Integer.parseInt(tcpPort)).setDate(new Date());
		}
	}

	private boolean isOperator(char s) {
		return (s == '*' || s == '+' || s == '-' || s == '/');
	}
	
	private void sendResponseMsg(SocketAddress address) {
		
		Node currNode;
		StringBuilder builder = new StringBuilder();
		
		// creating message with all active nodes
		for (Integer key : _ctrl.getNodes().keySet()) {
			currNode = _ctrl.getNodes().get(key);
			if(currNode.isAlive())
				builder.append(currNode.getAddress() + ":" + currNode.getTcpPort() + "\n");
		}
		
		builder.append(_ctrl.getRmax());
				
		byte[] buffer = builder.toString().getBytes();
		
		// send datagram packet as a response
		try {
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address);
			_datagramSocket.send(packet);
		} catch (IOException e) {
			_ctrl.getPrintStream().println("Warning: Not possible to send response to !hello message");
			e.printStackTrace();
		}
	}
}
