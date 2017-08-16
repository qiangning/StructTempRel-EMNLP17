package edu.uw.cs.lil.uwtime.stats;

public class TemporalStatistics extends AbstractStatistics{
	private EndToEndStatistics relaxedStats, strictStats, oracleStats;
	public TemporalStatistics() {
		relaxedStats = new EndToEndStatistics();
		strictStats = new EndToEndStatistics();
		oracleStats = new EndToEndStatistics();
	}

	public EndToEndStatistics getRelaxed() {
		return relaxedStats;
	}

	public EndToEndStatistics getStrict() {
		return strictStats;
	}

	public EndToEndStatistics getOracle() {
		return oracleStats;
	}

	@Override
	public String formatStats() {
		return "Relaxed:\n" + relaxedStats.formatStats() + 
				"\nStrict:\n" + strictStats.formatStats() + 
				"\nOracle:\n" + oracleStats.formatStats();
	}
}
