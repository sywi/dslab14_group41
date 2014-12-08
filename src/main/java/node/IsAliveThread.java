package node;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class IsAliveThread implements Runnable {
	
	private DatagramSocket _datagramSocket;
	private String[] _operators;
	private boolean _isAlive;
	private int _tcpPort;
	private int _alive;
	private int _udpPort;
	
	public IsAliveThread(DatagramSocket datagramSocket, String[] operators, int tcpPort, int alive, int udpPort) {
		_datagramSocket = datagramSocket;
		_operators = operators;
		_isAlive = true;
		_tcpPort = tcpPort;
		_alive = alive;
		_udpPort = udpPort;
	}

	@Override
	public void run() {
		String msg = Integer.toString(_tcpPort);
		
		for(String c : _operators)
			msg += c;
					
		byte[] buffer = msg.getBytes();

		while (_isAlive) {
			try {
				InetAddress receiverAddress = InetAddress.getLocalHost();
				DatagramPacket packet = new DatagramPacket(buffer, buffer.length, receiverAddress, _udpPort);
				_datagramSocket.send(packet);
				Thread.sleep(_alive);
			} catch (InterruptedException | IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void terminate() {
		_isAlive = false;
	}
	
}
