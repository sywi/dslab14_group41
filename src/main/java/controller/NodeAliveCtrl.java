package controller;

import java.util.Date;

public class NodeAliveCtrl implements Runnable {
	
	private boolean _isAlive;
	private CloudController _ctrl;
	
	public NodeAliveCtrl(CloudController ctrl) {
		_isAlive = true;
		_ctrl = ctrl;
	}

	@Override
	public void run() {
		while (_isAlive) {
			try {
				Thread.sleep(_ctrl.getCheckperiod());
			} catch (InterruptedException e) {
				_ctrl.getPrintStream().println("Sleep interrupted, Thread for isAlive packet " + e.getMessage());
			}
								
			// iterate through all nodes
			for (Integer key : _ctrl.getNodes().keySet()) {
				// calculate the difference between the last isAlive packet from this node and now
				long diff = (new Date()).getTime() - _ctrl.getNodes().get(key).getDate().getTime();

				if (diff >= _ctrl.getTimeout()) {
					_ctrl.getNodes().get(key).setIsAlive(false);
				} else {
					_ctrl.getNodes().get(key).setIsAlive(true);
				}
			}					
		}
	}
	
	public void terminate() {
		_isAlive = false;
	}

}
