package node;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ConsoleInputThread implements Runnable {
	
	private Node _ctrlNode;
	private boolean _isAlive;
	
	public ConsoleInputThread(Node ctrlNode) {
		_ctrlNode = ctrlNode;
		_isAlive = true;
	}

	@Override
	public void run() {
		while (_isAlive) {
		
		// handles console input
			BufferedReader reader = new BufferedReader(new InputStreamReader(_ctrlNode.getInputStream()));
			
			try {
				String cmd = reader.readLine();
				
				switch (cmd) {
				case "!exit":
					_ctrlNode.exit();
					break;
				case "!resources":
					_ctrlNode.getPrintStream().println(_ctrlNode.resources());
					break;

				default:
					_ctrlNode.getPrintStream().println("No vailid command");
				}
				
			} catch (IOException e) {
				_ctrlNode.getPrintStream().println("Problems with user input. " + e.getMessage());
			}
		
		}
	}

	public void terminate() {
		_isAlive = false;
	}
}
