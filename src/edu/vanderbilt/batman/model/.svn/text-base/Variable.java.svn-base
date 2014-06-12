package edu.vanderbilt.batman.model;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import lombok.Getter;
import lombok.Setter;

public class Variable {
	
	@Getter private String context;
	@Getter private String name;
	@Getter private List<String> values;
	@Setter private int valueType;
	
	// variable value type;
	public static int UNDETERMINED = -1;
	public static int NUMBER = 0;    // +/- 0-9
	public static int NAME = 1;     // no space in string.
	public static int EMAIL = 2;    // pattern: xx@xxx.com/edu/org/net.
	public static int PHONE = 3;    // 10 digit number.
	public static int STRING = 4;   // general string containing spaces, e.g., addr, paper name.
	
	private static Pattern pattern_num = Pattern.compile("^[-+]?[0-9]*\\.?[0-9]+$");
	private static Pattern pattern_email = Pattern.compile("([\\w-]+(?:\\.[\\w-]+)*@(?:[\\w-]+\\.)+\\w{2,7})\\b");
	private static Pattern pattern_phone = Pattern.compile("^\\(?(\\d{3})\\)?[- ]?(\\d{3})[- ]?(\\d{4})$");
	
	public Variable(String con, String n) {
		context = con;
		name = n;
		values = new ArrayList<String>();
	}
	
	public Variable(Variable var) {
		context = var.getContext();
		name = var.getName();
		values = new ArrayList<String>(var.getValues());
	}
	
	public Variable(String var) {
		context = var.substring(0, var.lastIndexOf("|"));
		name = var.substring(var.lastIndexOf("|")+1);
		values = new ArrayList<String>();
	}
	
	public void addValue(String value) {
		values.add(value);
	}
	
	public void addValues(List<String> vs) {
		values.addAll(vs);
	}
	
	public String nameWithContext() {
		return context + "|" + name;
	}
	
	@Override
	public String toString() {
		return nameWithContext();
	}
	
	
	public int getValueType() {
		if (valueType == UNDETERMINED && values.size() != 0) {
			computeValueType();
		}
		return valueType;
	}
	
	private void computeValueType() {
		int type = UNDETERMINED;
		for (String value: values) {
			if (type == UNDETERMINED) {
				type = patternMatch(value);
			} else if (type != patternMatch(value)) {
				type = STRING;
			}
		}
		valueType = type;
	}
	
	private int patternMatch(String value) {
		if (value.contains(" ")){    // general string contain space.
			return STRING;
		} else {
			if (pattern_email.matcher(value).matches()) {       // check if email.
				return EMAIL;
			} else if (pattern_phone.matcher(value).matches()) {    // check if phone number.
				return PHONE;
			} else if (pattern_num.matcher(value).matches()){   // check number: integer/double; positive/both.
				return NUMBER;
			} else {
				return NAME;    // no space string.
			}
		}
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof Variable) {
			return this.nameWithContext().equals(((Variable)o).nameWithContext());
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return 0;
	}

}
