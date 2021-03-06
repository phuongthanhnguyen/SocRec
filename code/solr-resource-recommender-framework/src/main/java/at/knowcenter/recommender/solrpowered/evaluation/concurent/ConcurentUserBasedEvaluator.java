package at.knowcenter.recommender.solrpowered.evaluation.concurent;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import at.knowcenter.recommender.solrpowered.engine.strategy.StrategyType;
import at.knowcenter.recommender.solrpowered.evaluation.RecommenderEvaluator;
import at.knowcenter.recommender.solrpowered.evaluation.concurent.jobs.*;
import at.knowcenter.recommender.solrpowered.evaluation.concurent.jobs.marketplace.cb.C_Name_Description_Job;
import at.knowcenter.recommender.solrpowered.evaluation.concurent.jobs.social.UB_CustomerGroups_Job;
import at.knowcenter.recommender.solrpowered.evaluation.concurent.jobs.social.UB_InterestsWithOutMLT_Job;
import at.knowcenter.recommender.solrpowered.evaluation.concurent.jobs.social.UB_Interests_Job;
import at.knowcenter.recommender.solrpowered.evaluation.concurent.jobs.social.cb.UB_InterestsCustomerGroups_Job;
import at.knowcenter.recommender.solrpowered.evaluation.metrics.MetricsExporter;
import at.knowcenter.recommender.solrpowered.services.SolrServiceContainer;
import at.knowcenter.recommender.solrpowered.services.impl.actions.RecommendService;
import at.knowcenter.recommender.solrpowered.services.impl.item.ItemService;
import at.knowcenter.recommender.solrpowered.services.impl.user.UserService;

public class ConcurentUserBasedEvaluator {
	
	public static void main(String... args) {
		ConcurentUserBasedEvaluator evaluator = new ConcurentUserBasedEvaluator();
		evaluator.evaluate();
	}

	private List<String> users;
	
	public void evaluate() {
		long startTime = System.nanoTime();
		
		initEvaluation();
		
		ExecutorService executor = Executors.newFixedThreadPool(4);
		
		List<Callable<List<MetricsExporter>>> jobs = new ArrayList<Callable<List<MetricsExporter>>>();
		
		int fullUserSize = users.size();
		
		int partitionCount = 100;
		
		for (int i = 0; i < partitionCount; i++) {
			
			int partitionSize = fullUserSize / partitionCount;
			int offset = (i + 1) * partitionSize;
			
			if (i == partitionCount - 1) {
				offset = fullUserSize;
			}
			
			List<String> userPartition= users.subList(i * partitionSize, offset);

			jobs.add(new UB_Interests_Job(userPartition, "UB_Int_job_" + i));
			jobs.add(new UB_InterestsCustomerGroups_Job(userPartition, "UB_IntCustGr_job_" + i));
			jobs.add(new UB_CustomerGroups_Job(userPartition, "UB_CustGr_job_" + i));
			jobs.add(new UB_InterestsWithOutMLT_Job(userPartition, "UB_Int_woMLT_job_" + i));
		}
		
		Map<String, List<MetricsExporter>> metricExporterMap = new HashMap<String, List<MetricsExporter>>();
		
		
	    List<Future<List<MetricsExporter>>> list = new ArrayList<Future<List<MetricsExporter>>>();
	    
	    for (Callable<List<MetricsExporter>> job : jobs) {
			Future<List<MetricsExporter>> submit = executor.submit(job);
			list.add(submit);
		}

	    for (Future<List<MetricsExporter>> future : list) {
	    	try {
	    		List<MetricsExporter> metrics = future.get();
	    		
	    		if (metricExporterMap.containsKey(metrics.get(0).getAlgorithmName())) {
	    			List<MetricsExporter> previousMetrics = metricExporterMap.get(metrics.get(0).getAlgorithmName());
	    			for (int i = 0; i < previousMetrics.size(); i++) {
	    				previousMetrics.get(i).appendMetrics(metrics.get(i));
	    			}
	    			
	    			metricExporterMap.put(metrics.get(0).getAlgorithmName(), previousMetrics);
	    		} else {
	    			metricExporterMap.put(metrics.get(0).getAlgorithmName(), metrics);
	    		}
	    	} catch (InterruptedException e) {
	    		e.printStackTrace();
	    	} catch (ExecutionException e) {
	    		e.printStackTrace();
	    	}
	    }
	    executor.shutdown();
	    
	    for (List<MetricsExporter> metrics : metricExporterMap.values()) {
	    	for (int n = 1; n <= 10; n++) {
				MetricsExporter mCalc = metrics.get(n - 1);
				mCalc.exportCalculatedMetricsAverage(users.size());
			}
	    }
	    
	    long totalTime = System.nanoTime() - startTime;
	    System.out.println("Finished all threads in " + (totalTime / 1000000000) + " seconds");
	}

	private void initEvaluation() {
		String url = "192.168.111.32";
		int port = 8984;
		RecommenderEvaluator recommenderEval = new RecommenderEvaluator();
		
		UserService userService = new UserService(url, port, "collection3");
		RecommendService recService = new RecommendService(url, port, "collection2");
		ItemService itemService = new ItemService(url, port, "collection1");
		SolrServiceContainer.getInstance().setUserService(userService);
		SolrServiceContainer.getInstance().setRecommendService(recService);
		SolrServiceContainer.getInstance().setItemService(itemService);
		
		users = recommenderEval.getAllUsers();
		System.out.println("User Size " + users.size());
	}
	
}
