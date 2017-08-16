package edu.uw.cs.lil.uwtime.eval.entities.durations;

import org.joda.time.LocalDateTime;
import org.joda.time.Period;

import edu.uw.cs.lil.uwtime.eval.TemporalModifier;
import edu.uw.cs.lil.uwtime.eval.TemporalQuantifier;

public class TemporalWeek extends TemporalDuration {
	public TemporalWeek(int n) {
		super(n);
	}
	
	public TemporalWeek(int n, TemporalDuration child) {
		super(n, child);
	}

	public TemporalWeek(int n, TemporalDuration child, TemporalQuantifier quantifier, TemporalModifier modifier) {
		super(n, child, quantifier, modifier);
	}

	@Override
	public String getName() {
		return "week";
	}

	@Override
	public String getLocalSequenceValue() {
		return isFixed() ? String.format("-W%d", getN()) : "-WXX";
	}

	@Override
	public int getGranularity() {
		return 4;
	}

	@Override
	public TemporalDuration getDefaultParent() {
		return new TemporalYear(0, this);
	}

	@Override
	public TemporalDuration clone() {
		return new TemporalWeek(getN(), child == null ? null : child.clone(), quantifier, modifier);
	}

	@Override
	public int getMaximumN() {
		if (parent instanceof TemporalMonth)
			return 4;
		else
			return 52;
	}

	@Override
	public LocalDateTime getStartJodaTime(LocalDateTime jodaTime) {
		// First week contains the first Thursday. ISO-8601 definition
		while (jodaTime.getDayOfWeek() != 4)
			jodaTime = jodaTime.plusDays(1);
		jodaTime = jodaTime.minusDays(3);
		jodaTime = jodaTime.plusWeeks(getN() - 1); // Don't need to add weeks from the first week
		return child == null ? jodaTime : child.getStartJodaTime(jodaTime);
	}

	@Override
	public Period getJodaUnitPeriod() {
		return new Period().withWeeks(1);
	}

	@Override
	public String getDurationValue() {
		return String.format("P%sW", isFixed() ? getN() + "": "X");
	}

	@Override
	public int atSameGranularity(LocalDateTime jodaTime) {
		return jodaTime.getWeekOfWeekyear();
	}
}
