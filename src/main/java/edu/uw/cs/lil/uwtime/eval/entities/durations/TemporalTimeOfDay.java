package edu.uw.cs.lil.uwtime.eval.entities.durations;

import org.joda.time.LocalDateTime;
import org.joda.time.Period;

import edu.uw.cs.lil.uwtime.eval.TemporalModifier;
import edu.uw.cs.lil.uwtime.eval.TemporalQuantifier;


public class TemporalTimeOfDay extends TemporalDuration {
	final private static String[] timesOfDayNames = {"MO", "AF", "EV", "NI"};
	final private static int[] timesOfDayStartHours = {0, 12, 18, 18};
	final private static int[] timesOfDayNumHours = {12, 6, 6, 6};
	
	public TemporalTimeOfDay(int n) {
		super(n);
	}
	
	public TemporalTimeOfDay(int n, TemporalDuration child) {
		super(n, child);
	}
	
	public TemporalTimeOfDay(int n, TemporalDuration child, TemporalQuantifier quantifier, TemporalModifier modifier) {
		super(n, child, quantifier, modifier);
	}

	@Override
	public String getName() {
		return "time_of_day";
	}

	@Override
	public String getLocalSequenceValue() {
		return "T" + (isFixed() ? timesOfDayNames[getN() - 1] : "XX");
	}

	@Override
	public int getGranularity() {
		return 7;
	}

	@Override
	public TemporalDuration getDefaultParent() {
		return new TemporalDay(0, this);
	}

	@Override
	public TemporalDuration clone() {
		return new TemporalTimeOfDay(getN(), child == null ? null : child.clone(), quantifier, modifier);
	}

	@Override
	public int getMaximumN() {
		return 4;
	}

	@Override
	public LocalDateTime getStartJodaTime(LocalDateTime jodaDate) {
		jodaDate = jodaDate.withHourOfDay(timesOfDayStartHours[getN() - 1]);
		return child == null ? jodaDate : child.getStartJodaTime(jodaDate);
	}

	@Override
	public Period getJodaUnitPeriod() {
		return new Period().withHours(timesOfDayNumHours[getN()-1]);
	}
	
	@Override
	public String getDurationValue() {
		// Not actually seen in data
		return String.format("PT%s",(isFixed() ? timesOfDayNames[getN() - 1] : "X"));
	}

	@Override
	public int atSameGranularity(LocalDateTime jodaTime) {
		int hour = jodaTime.getHourOfDay();
		for (int i = 0 ; i < timesOfDayStartHours.length; i++)
			if (hour >= timesOfDayStartHours[i] && hour < timesOfDayStartHours[i] + timesOfDayNumHours[i])
				return i;
		return 0;
	}
}
