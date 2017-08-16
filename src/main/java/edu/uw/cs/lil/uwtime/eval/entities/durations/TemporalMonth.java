package edu.uw.cs.lil.uwtime.eval.entities.durations;

import org.joda.time.LocalDateTime;
import org.joda.time.Period;

import edu.uw.cs.lil.uwtime.eval.TemporalModifier;
import edu.uw.cs.lil.uwtime.eval.TemporalQuantifier;


public class TemporalMonth extends TemporalDuration {
	public TemporalMonth(int n) {
		super(n);
	}
	
	public TemporalMonth(int n, TemporalDuration child) {
		super(n, child);
	}

	public TemporalMonth(int n, TemporalDuration child, TemporalQuantifier quantifier, TemporalModifier modifier) {
		super(n, child, quantifier, modifier);
	}

	@Override
	public String getName() {
		return "month";
	}

	@Override
	public String getLocalSequenceValue() {
		return isFixed() ? String.format("-%02d", getN()) : "-XX";
	}

	@Override
	public int getGranularity() {
		return 3;
	}

	@Override
	public TemporalDuration getDefaultParent() {
		return new TemporalYear(0, this);
	}

	@Override
	public TemporalDuration clone() {
		return new TemporalMonth(getN(), child == null ? null : child.clone(), quantifier, modifier);
	}

	@Override
	public int getMaximumN() {
		return 12;
	}

	@Override
	public LocalDateTime getStartJodaTime(LocalDateTime jodaDate) {
		jodaDate = jodaDate.withMonthOfYear(getN());
		return child == null ? jodaDate : child.getStartJodaTime(jodaDate);
	}

	@Override
	public Period getJodaUnitPeriod() {
		return new Period().withMonths(1);
	}
	@Override
	public String getDurationValue() {
		return String.format("P%sM", isFixed() ? getN() + "": "X");
	}

	@Override
	public int atSameGranularity(LocalDateTime jodaTime) {
		return jodaTime.getMonthOfYear();
	}
}
