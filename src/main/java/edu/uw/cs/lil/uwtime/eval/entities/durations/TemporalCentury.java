package edu.uw.cs.lil.uwtime.eval.entities.durations;

import org.joda.time.LocalDateTime;
import org.joda.time.Period;

import edu.uw.cs.lil.uwtime.eval.TemporalModifier;
import edu.uw.cs.lil.uwtime.eval.TemporalQuantifier;


public class TemporalCentury extends TemporalDuration {
	public TemporalCentury(int n) {
		super(n);
	}
	
	public TemporalCentury(int n, TemporalDuration child) {
		super(n, child);
	}

	public TemporalCentury(int n, TemporalDuration child, TemporalQuantifier quantifier, TemporalModifier modifier) {
		super(n, child, quantifier, modifier);
	}

	@Override
	public String getName() {
		return "century";
	}

	@Override
	public String getLocalSequenceValue() {
		return isFixed() ? String.format("%02d", getN() - 1) : "XX";
	}

	@Override
	public int getGranularity() {
		return -2;
	}

	@Override
	public TemporalDuration getDefaultParent() {
		return null;
	}

	@Override
	public TemporalDuration clone() {
		return new TemporalCentury(getN(), child == null ? null : child.clone(), quantifier, modifier);
	}

	@Override
	public int getMaximumN() {
		return 30;
	}
	
	@Override
	public LocalDateTime getStartJodaTime(LocalDateTime jodaDate) {
		jodaDate = jodaDate.plusYears((getN() - 1) * 100);
		return child == null ? jodaDate :child.getStartJodaTime(jodaDate);
	}
	
	@Override
	public Period getJodaUnitPeriod() {
		return new Period().withYears(100);
	}
	
	/*
	@Override
	public String getDurationValue() {
		return isFixed() ? String.format("P%dY", getN() * 100) : "PXC";
	}
	*/
	
	@Override
	public String getDurationValue() {
		return String.format("P%sC", isFixed() ? getN() + "": "X");
	}

	@Override
	public int atSameGranularity(LocalDateTime jodaTime) {
		return jodaTime.getCenturyOfEra();
	}
}
