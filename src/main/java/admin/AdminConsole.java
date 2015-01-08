package admin;

import controller.IAdminConsole;
import model.ComputationRequestInfo;
import util.Config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.Key;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

/**
 * Please note that this class is not needed for Lab 1, but will later be used
 * in Lab 2. Hence, you do not have to implement it for the first submission.
 */
public class AdminConsole implements IAdminConsole, INotificationCallback,
		Runnable {

	private String componentName;
	private Config config;
	private InputStream userRequestStream;
	private PrintStream userResponseStream;
	private IAdminConsole controller;
	private boolean _run;

	/**
	 * @param componentName
	 *            the name of the component - represented in the prompt
	 * @param config
	 *            the configuration to use
	 * @param userRequestStream
	 *            the input stream to read user input from
	 * @param userResponseStream
	 *            the output stream to write the console output to
	 * @throws RemoteException
	 * @throws NotBoundException
	 */
	public AdminConsole(String componentName, Config config,
			InputStream userRequestStream, PrintStream userResponseStream)
			throws RemoteException, NotBoundException {
		this.componentName = componentName;
		this.config = config;
		this.userRequestStream = userRequestStream;
		this.userResponseStream = userResponseStream;
		Registry registry = LocateRegistry.getRegistry(
				config.getString("controller.host"),
				config.getInt("controller.rmi.port"));
		controller = (IAdminConsole) registry.lookup(config
				.getString("binding.name"));

		// TODO
	}

	@Override
	public void run() {
		_run = true;
		String response;
		BufferedReader consoleReader = new BufferedReader(
				new InputStreamReader(userRequestStream));

		while (_run) {
			response = "";
			// console input
			String cmd = "";

			try {
				cmd = consoleReader.readLine();
				String[] splittedCmd = cmd.split(" ");

				// handle console input
				if (cmd.startsWith("!getLogs")) {
					String[] logs = getLogsSort();
					for (int i = 0; i < logs.length; i++) {
						userResponseStream.println(logs[i]);
					}

				} else if (cmd.startsWith("!statistics")) {
					String[] stats = getStatistics();
					for (int i = 0; i < stats.length; i++) {
						userResponseStream.println(stats[i]);
					}
				} else if (cmd.startsWith("!subscribe")) {
					subscribe(splittedCmd[1], Integer.valueOf(splittedCmd[2]),
							this);
					userResponseStream
							.println("Successfully subscribed for user "
									+ splittedCmd[1]);
				} else if (cmd.startsWith("!exit")) {
					exit();
				} else {
					userResponseStream.println("No valid command.");
				}

			} catch (IOException e) {
				userResponseStream.println("Problems with the connection.");
			}
		}
	}

	private void exit() {
		// TODO Auto-generated method stub

	}

	private String[] getStatistics() throws RemoteException {
		String[] zruck = null;
		LinkedHashMap<Character, Long> stats = statistics();
		Set<Character> operatorsKey=stats.keySet();
		
		//Ugly fix starts:
		String[] operators = new String[operatorsKey.size()];
		int index = 0;
		for (Object c : operatorsKey.toArray()) {
			operators[index++] = ((Character)c).toString();
		}
		//Ugly fix ends.
		
		for (int i = 0; i < operators.length; i++) {
			zruck[i]=operators[i]+" "+stats.get(operators[i]);
		}

		return zruck;
	}

	private String[] getLogsSort() throws RemoteException {
		String[] zruck = null;
		int zruckPos = 0;
		List<ComputationRequestInfo> todo = getLogs();
		for (int i = 0; i < todo.size(); i++) {
			ComputationRequestInfo act = todo.get(i);
			HashMap<String, String> dateResult = act.getDateResult();
			String name = act.getComponentName();
			Set<String> dateTemp = dateResult.keySet();
			String[] date = (String[]) dateTemp.toArray();
			for (int j = 0; j < date.length; j++) {
				String[] splittedDate = date[j].split("_");
				zruck[zruckPos] = splittedDate[0] + "_" + splittedDate[1]
						+ " [" + name + "]: " + dateResult.get(date[j]);
			}
		}
		Arrays.sort(zruck);

		return zruck;
	}

	@Override
	public boolean subscribe(String username, int credits,
			INotificationCallback callback) throws RemoteException {
		return controller.subscribe(username, credits, callback);

	}

	@Override
	public List<ComputationRequestInfo> getLogs() throws RemoteException {
		return controller.getLogs();

	}

	@Override
	public LinkedHashMap<Character, Long> statistics() throws RemoteException {
		return controller.statistics();
	}

	@Override
	public Key getControllerPublicKey() throws RemoteException {
		// TODO Auto-generated method stub
		return controller.getControllerPublicKey();
	}

	@Override
	public void setUserPublicKey(String username, byte[] key)
			throws RemoteException {
		// TODO Auto-generated method stub
	}

	/**
	 * @param args
	 *            the first argument is the name of the {@link AdminConsole}
	 *            component
	 * @throws RemoteException
	 * @throws NotBoundException
	 */
	public static void main(String[] args) throws RemoteException,
			NotBoundException {
		AdminConsole adminConsole = new AdminConsole(args[0], new Config(
				"admin"), System.in, System.out);
		Thread t = new Thread(adminConsole);
		t.start();
	}


	public void notify(String username, int credits) throws RemoteException {
		userResponseStream.println("Notification: "+username+" has less than "+credits+" credits.");

	}
}
