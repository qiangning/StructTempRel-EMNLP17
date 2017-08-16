package edu.uw.cs.lil.uwtime.utils;

public class FormattingUtils {
	private FormattingUtils() {
	}

	public static String formatContents(String indentation, String field, Object value) {
		return String.format("%s%-10s: %s\n", indentation, field, value == null ? "null" : value.toString());
	}
}
