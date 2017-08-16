package edu.uw.cs.lil.uwtime.eval.entities.durations;

import org.joda.time.LocalDateTime;
import org.joda.time.Period;

import edu.uw.cs.lil.uwtime.eval.TemporalModifier;
import edu.uw.cs.lil.uwtime.eval.TemporalQuantifier;


public class TemporalMinute extends TemporalDuration {
	public TemporalMinute(int n) {
		super(n);
	}
	
	public TemporalMinute(int n, TemporalDuration child) {
		super(n, child);
	}
	
	public TemporalMinute(int n, TemporalDuration child, TemporalQuantifier quantifier, TemporalModifier modifier) {
		super(n, child, quantifier, modifier);
	}

	@Override
	public String getName() {
		return "minute";
	}

	@Override
	public String getLocalSequenceValue() {
		return ":" + (isFixed() ? String.format("%02d", getN() - 1) : "XX");
	}

	@Override
	public int getGranularity() {
		return 9;
	}

	@Override
	public TemporalDuration getDefaultParent() {
		return new TemporalHour(0, this);
	}

	@Override
	public TemporalDuration clone() {
		return new TemporalMinute(getN(), child == null ? null : child.clone(), quantifier, modifier);
	}

	@Override
	public int getMaximumN() {
		return 60;
	}

	@Override
	public LocalDateTime getStartJodaTime(LocalDateTime jodaDate) {
		jodaDate = jodaDate.withMinuteOfHour(getN() - 1);
		return child == null ? jodaDate : child.getStartJodaTime(jodaDate);
	}

	@Override
	public Period getJodaUnitPeriod() {
		return new Period().withMinutes(1);
	}
	
	@Override
	public String getDurationValue() {
		return String.format("PT%sM",(isFixed() ? getN() : "X"));
	}

	@Override
	public int atSameGranularity(LocalDateTime jodaTime) {
		return jodaTime.getMinuteOfHour() + 1;
	}
}
