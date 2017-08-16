package edu.uw.cs.lil.uwtime.eval.entities.durations;

import org.joda.time.LocalDateTime;
import org.joda.time.Period;

import edu.uw.cs.lil.uwtime.eval.TemporalModifier;
import edu.uw.cs.lil.uwtime.eval.TemporalQuantifier;


public class TemporalWeekend extends TemporalDuration {
	public TemporalWeekend(int n) {
		super(n);
	}
	
	public TemporalWeekend(int n, TemporalDuration child) {
		super(n, child);
	}

	public TemporalWeekend(int n, TemporalDuration child, TemporalQuantifier quantifier, TemporalModifier modifier) {
		super(n, child, quantifier, modifier);
	}

	@Override
	public String getName() {
		return "weekend";
	}

	@Override
	public String getLocalSequenceValue() {
		return "-WE";
	}

	@Override
	public int getGranularity() {
		return 5;
	}

	@Override
	public TemporalDuration getDefaultParent() {
		return new TemporalWeek(0, this);
	}

	@Override
	public TemporalDuration clone() {
		return new TemporalWeekend(getN(), child == null ? null : child.clone(), quantifier, modifier);
	}

	@Override
	public int getMaximumN() {
		return 1;
	}

	@Override
	public LocalDateTime getStartJodaTime(LocalDateTime jodaTime) {
		// Parent should have set jodaTime to first day of the week
		jodaTime = jodaTime.plusDays(5);
		return child == null ? jodaTime : child.getStartJodaTime(jodaTime);
	}

	@Override
	public Period getJodaUnitPeriod() {
		return new Period().withDays(2);
	}

	@Override
	public String getDurationValue() {
		return "P2D";
	}

	@Override
	public int atSameGranularity(LocalDateTime jodaTime) {
		return (jodaTime.getDayOfWeek() == 6 || jodaTime.getDayOfWeek() == 7 ) ? 1 : 0;
	}
}
