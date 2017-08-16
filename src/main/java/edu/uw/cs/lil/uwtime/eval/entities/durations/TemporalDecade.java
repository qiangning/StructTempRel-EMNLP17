package edu.uw.cs.lil.uwtime.eval.entities.durations;

import org.joda.time.LocalDateTime;
import org.joda.time.Period;

import edu.uw.cs.lil.uwtime.eval.TemporalModifier;
import edu.uw.cs.lil.uwtime.eval.TemporalQuantifier;


public class TemporalDecade extends TemporalDuration {
	public TemporalDecade(int n) {
		super(n);
	}
	
	public TemporalDecade(int n, TemporalDuration child) {
		super(n, child);
	}

	public TemporalDecade(int n, TemporalDuration child, TemporalQuantifier quantifier, TemporalModifier modifier) {
		super(n, child, quantifier, modifier);
	}

	@Override
	public String getName() {
		return "decade";
	}

	@Override
	public String getLocalSequenceValue() {
		return isFixed() ? String.format("%03d", getN()) : "XXX";
	}

	@Override
	public int getGranularity() {
		return -1;
	}

	@Override
	public TemporalDuration getDefaultParent() {
		return null;
	}

	@Override
	public TemporalDuration clone() {
		return new TemporalDecade(getN(), child == null ? null : child.clone(), quantifier, modifier);
	}

	@Override
	public int getMaximumN() {
		return 300;
	}
	
	@Override
	public LocalDateTime getStartJodaTime(LocalDateTime jodaDate) {
		jodaDate = jodaDate.plusYears(getN() * 10);
		return child == null ? jodaDate :child.getStartJodaTime(jodaDate);
	}
	
	@Override
	public Period getJodaUnitPeriod() {
		return new Period().withYears(10);
	}
	
	@Override
	public String getDurationValue() {
		return String.format("P%sE", isFixed() ? getN() + "": "X");
	}
	
	/*
	@Override
	public String getDurationValue() {
		return String.format("P%sY", isFixed() ? (getN() * 10) + "": "X");
	}
	*/

	@Override
	public int atSameGranularity(LocalDateTime jodaTime) {
		return jodaTime.getYearOfEra()/10;
	}
}
