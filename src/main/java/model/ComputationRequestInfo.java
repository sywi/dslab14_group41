package model;

import java.util.HashMap;

/**
 * Please note that this class is not needed for Lab 1, but will later be
 * used in Lab 2. Hence, you do not have to implement it for the first
 * submission.
 */
public class ComputationRequestInfo {
	private String componentName;
	private HashMap<String,String> dateResult;

	public String getComponentName() {
		return componentName;
	}

	public void setComponentName(String componentName) {
		this.componentName = componentName;
	}

	public HashMap<String,String> getDateResult() {
		return dateResult;
	}

	public void setDateResult(HashMap<String,String> dateResult) {
		this.dateResult = dateResult;
	}
}
