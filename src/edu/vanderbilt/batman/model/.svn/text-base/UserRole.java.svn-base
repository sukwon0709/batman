package edu.vanderbilt.batman.model;

import lombok.Getter;

public class UserRole implements Comparable<UserRole> {
	
	@Getter private int role;
	
	public UserRole(int r) {
		role = r;
	}
	
	public UserRole(UserRole r) {
		role = r.getRole();
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof UserRole) {
			return role == ((UserRole)o).getRole();
		}
		return false;
	}
	
	/*
	 * need to be overriden. containsKey() method calls hashCode first, then equals.
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return 0;
	}

	@Override
	public int compareTo(UserRole r) {
		if (role == r.getRole()) {
			return 0;
		} else if (role > r.getRole()) {
			return 1;
		} else {
			return -1;
		}
	}

	public String toString(){
		return Integer.toString(role);
		
	}
}
