package at.knowcenter.recommender.solrpowered.evaluation.concurent.jobs.marketplace.cb;

import java.util.List;
import java.util.concurrent.Callable;

import at.knowcenter.recommender.solrpowered.engine.filtering.ContentFilter;
import at.knowcenter.recommender.solrpowered.engine.strategy.StrategyType;
import at.knowcenter.recommender.solrpowered.evaluation.metrics.MetricsExporter;
import at.knowcenter.recommender.solrpowered.services.SolrServiceContainer;

public class C_Name_Job extends C_Strategy_Job implements Callable<List<MetricsExporter>>{

	private List<String> users;

	public C_Name_Job(List<String> users) {
		this.users = users;
	}
	
	@Override
	public List<MetricsExporter> call() throws Exception {
		return evaluate();
	}
	
	@Override
	public List<MetricsExporter> evaluate() {
		ContentFilter cf = new ContentFilter();
		List<MetricsExporter> metricsCalcs = initMetricCalcs("7_" + StrategyType.CN_WeightName.name());
		
		System.out.println("Evaluation over " + users.size() + " users");
		
		evaluate_C_Weighted(cf, metricsCalcs, StrategyType.CN_WeightName);
		
		return metricsCalcs;
	}
	
	@Override
	protected List<String> getUsers() {
		return users;
	}
}