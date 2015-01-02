//package node;
//
//import java.io.BufferedReader;
//import java.io.IOException;
//import java.io.InputStreamReader;
//import java.io.PrintWriter;
//import java.net.Socket;
//
//public class NodeRequestWorker implements Runnable {
//	
//	private Socket _socket;
//	
//	public NodeRequestWorker(Socket socket) {
//		_socket = socket;
//	}
//
//	@Override
//	public void run() {
//		System.out.println("node request worker");
//		try {
//			BufferedReader reader = new BufferedReader(new InputStreamReader(_socket.getInputStream()));
//			PrintWriter writer = new PrintWriter(_socket.getOutputStream(), true);
//			executeRequest(reader, writer);
//		} catch (IOException e) {
//			//TODO
//		}		
//	}
//	
//	private void executeRequest(BufferedReader reader, PrintWriter writer) {
//		System.out.println("executeRequest");
//		String r;
//		String request = "";
//		
//		try {
//			while((r = reader.readLine()) != null) 
//				request += r;
//			
//			System.out.println("-->> " + request);
//			
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}
//
//	
//}
