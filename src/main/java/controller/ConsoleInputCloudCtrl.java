package controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ConsoleInputCloudCtrl implements Runnable {

	private boolean _isAlive;
	private CloudController _ctrl;
	
	public ConsoleInputCloudCtrl(CloudController ctrl) {
		_ctrl = ctrl;
		_isAlive = true;
	}

	@Override
	public void run() {
		while (_isAlive) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(_ctrl.getInputStream()));

			try {
				String cmd = reader.readLine();

				switch (cmd) {
				case "!nodes":
					_ctrl.getPrintStream().println(_ctrl.nodes());
					break;

				case "!users":
					_ctrl.getPrintStream().println(_ctrl.users());
					break;

				case "!exit":
					_ctrl.exit();
					break;

				default:
					_ctrl.getPrintStream().println("No vailid command");
				}

			} catch (IOException e) {
				_ctrl.getPrintStream().println("Problems with user input. "
						+ e.getMessage());
			}
		}
	}
	
	public void terminate() {
		_isAlive = false;
	}
}
