package controller;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Date;

public class NodePackageWaiter implements Runnable {
	
	private boolean _isAlive;
	private CloudController _ctrl;
	private DatagramSocket _datagramSocket;
	private int _udpPort;
	
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
				DatagramPacket packet = new DatagramPacket(buffer,
						buffer.length);
				_datagramSocket.receive(packet);

				// reading the buffer from the packet
				int i = 0;
				String tcpPort = "";

				i = 0;

				// the first 5 bytes are the nodes TCP port
				while (i <= 4) {
					char c = (char) buffer[i++];
					tcpPort += Character.toString(c);
				}

				// getPort() is different from each node, but stays the same for
				// every packet
				// the HashMap doesn't contains a node with this port, it must
				// be a new one
				if (!_ctrl.getNodes().containsKey(Integer.parseInt(tcpPort))) {
					Node n = new Node();
					// set time stamp
					n.setDate(new Date());
					n.setIsAlive(true);
					n.setTcpPort(Integer.parseInt(tcpPort));
					n.setAddress(packet.getAddress());

					// the next bytes are the operators of the node
					while (i < buffer.length) {
						char c = (char) buffer[i++];
						n.addOperator(c);
					}

					// put the new node to the HashMap
					_ctrl.getNodes().put(Integer.parseInt(tcpPort), n);

				} else {
					// this node already exists, only the time stamp has to be
					// renewed
					_ctrl.getNodes().get(Integer.parseInt(tcpPort)).setDate(new Date());
				}
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

}
