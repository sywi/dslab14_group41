package controller;

public class User {
	
	private String _name;
	private String _password;
	private int _credits;
	private boolean _isActive;
	private int _initialCredits;
	
	protected User(String name, String password, int credits) {
		_name = name;
		_password = password;
		_initialCredits = credits;
		_credits = credits;
		_isActive = false;
	}
	
	public String getName() {
		return _name;
	}
	
	public String getPassword() {
		return _password;
	}
	
	public int getCredits() {
		return _credits;
	}
	
	public void setCredits(int c) {
		_credits = c;
	}
	
	public boolean login(String password) {
		return (_isActive = (password.equals(_password)));
	}
	
	public boolean isActive() {
		return _isActive;
	}
	
	public void logout() {
		_credits = _initialCredits;
		_isActive = false;
	}

}
