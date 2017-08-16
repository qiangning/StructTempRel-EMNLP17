package edu.uw.cs.lil.uwtime.eval.entities.durations;

import org.joda.time.IllegalFieldValueException;
import org.joda.time.LocalDateTime;
import org.joda.time.Period;

import edu.uw.cs.lil.uwtime.eval.TemporalModifier;
import edu.uw.cs.lil.uwtime.eval.TemporalQuantifier;


public class TemporalDay extends TemporalDuration {
	public TemporalDay(int n) {
		super(n);
	}
	
	public TemporalDay(int n, TemporalDuration child) {
		super(n, child);
	}

	public TemporalDay(int n, TemporalDuration child, TemporalQuantifier quantifier, TemporalModifier modifier) {
		super(n, child, quantifier, modifier);
	}

	@Override
	public String getName() {
		return "day";
	}

	@Override
	public String getLocalSequenceValue() {
		if (parent instanceof TemporalMonth)
			return (isFixed() ? String.format("-%02d", getN()) : "-XX");
		else 
			return (isFixed() ? String.format("-%d", getN()) : "-XX");
	}

	@Override
	public int getGranularity() {
		return 6;
	}

	@Override
	public TemporalDuration getDefaultParent() {
		return new TemporalMonth(0, this);
	}

	@Override
	public TemporalDuration clone() {
		return new TemporalDay(getN(), child == null ? null : child.clone(), quantifier, modifier);
	}

	@Override
	public int getMaximumN() {
		if (parent instanceof TemporalWeek)
			return 7;
		else if (parent instanceof TemporalMonth)
			return 31;
		else 
			return 365;
	}

	@Override
	public LocalDateTime getStartJodaTime(LocalDateTime jodaDate) {
		if (parent instanceof TemporalWeek)
			jodaDate = jodaDate.withDayOfWeek(getN());
		else if (parent instanceof TemporalMonth) {
			try {
				jodaDate = jodaDate.withDayOfMonth(getN());
			}
			catch (IllegalFieldValueException e) {
				// Day of the month doesn't exist for the given day
				return null;
			}
		}
		else
			jodaDate = jodaDate.withDayOfYear(getN());
		return child == null ? jodaDate :child.getStartJodaTime(jodaDate);
	}

	@Override
	public Period getJodaUnitPeriod() {
		return new Period().withDays(1);
	}

	@Override
	public String getDurationValue() {
		return String.format("P%sD", isFixed() ? getN() + "": "X");
	}

	@Override
	public int atSameGranularity(LocalDateTime jodaTime) {
		if (parent instanceof TemporalWeek)
			return jodaTime.getDayOfWeek();
		else if (parent instanceof TemporalMonth)
			return jodaTime.getDayOfMonth();
		else
			return jodaTime.getDayOfYear();
	}
}
