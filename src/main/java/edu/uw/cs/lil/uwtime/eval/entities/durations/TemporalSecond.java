package edu.uw.cs.lil.uwtime.eval.entities.durations;

import org.joda.time.LocalDateTime;
import org.joda.time.Period;

import edu.uw.cs.lil.uwtime.eval.TemporalModifier;
import edu.uw.cs.lil.uwtime.eval.TemporalQuantifier;


public class TemporalSecond extends TemporalDuration {
	public TemporalSecond(int n) {
		super(n);
	}
	
	public TemporalSecond(int n, TemporalDuration child) {
		super(n, child);
	}
	
	public TemporalSecond(int n, TemporalDuration child, TemporalQuantifier quantifier, TemporalModifier modifier) {
		super(n, child, quantifier, modifier);
	}

	@Override
	public String getName() {
		return "second";
	}

	@Override
	public String getLocalSequenceValue() {
		return ":" + (isFixed() ? String.format("%02d", getN() - 1) : "XX");
	}

	@Override
	public int getGranularity() {
		return 10;
	}

	@Override
	public TemporalDuration getDefaultParent() {
		return new TemporalMinute(0, this);
	}

	@Override
	public TemporalDuration clone() {
		return new TemporalSecond(getN(), child == null ? null : child.clone(), quantifier, modifier);
	}

	@Override
	public int getMaximumN() {
		return 60;
	}

	@Override
	public LocalDateTime getStartJodaTime(LocalDateTime jodaDate) {
		jodaDate = jodaDate.withSecondOfMinute(getN() - 1);
		return child == null ? jodaDate : child.getStartJodaTime(jodaDate);
	}

	@Override
	public Period getJodaUnitPeriod() {
		return new Period().withSeconds(1);
	}
	
	@Override
	public String getDurationValue() {
		return String.format("PT%sS",(isFixed() ? getN() : "X"));
	}

	@Override
	public int atSameGranularity(LocalDateTime jodaTime) {
		return jodaTime.getSecondOfMinute() + 1;
	}
}
