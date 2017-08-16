package edu.uw.cs.lil.uwtime.eval;

public enum TemporalModifier {
	NONE, PAST, PRESENT, FUTURE, BC, LESS, MORE, LESS_E, MORE_E, START, MID, END, APPROX ;

	@Override
	public String toString() {
		switch(this) {
		case START:
			return "START";
		case MID:
			return "MID";
		case END:
			return "END";
		case APPROX:
			return "APPROX";
		case LESS:
			return "LESS_THAN";
		case LESS_E:
			return "EQUAL_OR_LESS";
		case MORE:
			return "MORE_THAN";
		case MORE_E:
			return "EQUAL_OR_MORE";
		default:
			return null;
		}
	}
}
