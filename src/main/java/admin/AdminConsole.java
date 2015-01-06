package admin;

import controller.IAdminConsole;
import model.ComputationRequestInfo;
import util.Config;

import java.io.InputStream;
import java.io.PrintStream;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.Key;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Please note that this class is not needed for Lab 1, but will later be
 * used in Lab 2. Hence, you do not have to implement it for the first
 * submission.
 */
public class AdminConsole implements IAdminConsole, INotificationCallback, Runnable {

	private String componentName;
	private Config config;
	private InputStream userRequestStream;
	private PrintStream userResponseStream;
	private IAdminConsole controller;

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
			InputStream userRequestStream, PrintStream userResponseStream) throws RemoteException, NotBoundException {
		this.componentName = componentName;
		this.config = config;
		this.userRequestStream = userRequestStream;
		this.userResponseStream = userResponseStream;
		Registry registry = LocateRegistry.getRegistry(config.getString("controller.host"), config.getInt("controller.rmi.port"));
		controller = (IAdminConsole) registry.lookup(config.getString("binding.name"));

		// TODO
	}

	@Override
	public void run() {
		// TODO
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
	public static void main(String[] args) throws RemoteException, NotBoundException {
		AdminConsole adminConsole = new AdminConsole(args[0], new Config(
				"admin"), System.in, System.out);
		// TODO: start the admin console
	}

	@Override
	public void notify(String username, int credits) throws RemoteException {
		// TODO Auto-generated method stub
		
	}
}
