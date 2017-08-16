package edu.uw.cs.lil.uwtime.eval.entities.durations;

import org.joda.time.LocalDateTime;
import org.joda.time.Period;

import edu.uw.cs.lil.uwtime.eval.TemporalModifier;
import edu.uw.cs.lil.uwtime.eval.TemporalQuantifier;


public class TemporalYear extends TemporalDuration {
	public TemporalYear(int n) {
		super(n);
	}
	
	public TemporalYear(int n, TemporalDuration child) {
		super(n, child);
	}

	public TemporalYear(int n, TemporalDuration child, TemporalQuantifier quantifier, TemporalModifier modifier) {
		super(n, child, quantifier, modifier);
	}

	@Override
	public String getName() {
		return "year";
	}

	@Override
	public String getLocalSequenceValue() {
		return isFixed() ? String.format("%04d", getN()) : "XXXX";
	}

	@Override
	public int getGranularity() {
		return 0;
	}

	@Override
	public TemporalDuration getDefaultParent() {
		return null;
	}

	@Override
	public TemporalDuration clone() {
		return new TemporalYear(getN(), child == null ? null : child.clone(), quantifier, modifier);
	}

	@Override
	public int getMaximumN() {
		return 3000;
	}
	
	@Override
	public LocalDateTime getStartJodaTime(LocalDateTime jodaDate) {
		jodaDate = jodaDate.plusYears(getN());
		return child == null ? jodaDate : child.getStartJodaTime(jodaDate);
	}
	
	@Override
	public Period getJodaUnitPeriod() {
		return new Period().withYears(1);
	}
	
	@Override
	public String getDurationValue() {
		return String.format("P%sY", isFixed() ? getN() + "": "X");
	}

	@Override
	public int atSameGranularity(LocalDateTime jodaTime) {
		return jodaTime.getYearOfEra();
	}
}
