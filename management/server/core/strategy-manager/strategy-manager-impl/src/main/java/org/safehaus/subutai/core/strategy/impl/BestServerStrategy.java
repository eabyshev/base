package org.safehaus.subutai.core.strategy.impl;

import org.safehaus.subutai.core.strategy.api.Criteria;
import org.safehaus.subutai.core.strategy.api.ServerMetric;
import org.safehaus.subutai.common.protocol.Agent;

import java.util.*;

public class BestServerStrategy extends RoundRobinStrategy {
    private List<Criteria> criteria = new ArrayList<Criteria>();

	@Override
	protected List<Agent> sortServers(Map<Agent, ServerMetric> serverMetrics) {
		// using each strategy criteria, grade servers one by one
		Map<Agent, Integer> grades = new HashMap<Agent, Integer>();
		for (Agent a : serverMetrics.keySet()) grades.put(a, 0);
		for (Criteria sf : criteria) {
			try {
				Agent a = getBestMatch(serverMetrics, MetricComparator.create(sf));
				if (a != null) grades.put(a, grades.get(a) + 1);
			} catch (Exception ex) {
				// comparator not defined for strategy
				// TODO: log
			}
		}

		// sort servers by their grades in decreasing order
		ArrayList<Map.Entry<Agent, Integer>> ls = new ArrayList<Map.Entry<Agent, Integer>>(grades.entrySet());
		Collections.sort(ls, new Comparator<Map.Entry>() {

			@Override
			public int compare(Map.Entry o1, Map.Entry o2) {
				Integer v1 = (Integer) o1.getValue();
				Integer v2 = (Integer) o2.getValue();
				return -1 * v1.compareTo(v2);
			}
		});

		List<Agent> servers = new ArrayList<Agent>();
		for (Map.Entry<Agent, Integer> e : ls) servers.add(e.getKey());
		return servers;
	}

	private Agent getBestMatch(Map<Agent, ServerMetric> serverMetrics, final MetricComparator mc) {

		List<Map.Entry<Agent, ServerMetric>> ls = new ArrayList<Map.Entry<Agent, ServerMetric>>(serverMetrics.entrySet());
		Collections.sort(ls, new Comparator<Map.Entry>() {

			@Override
			public int compare(Map.Entry o1, Map.Entry o2) {
				int v1 = mc.getValue((ServerMetric) o1.getValue());
				int v2 = mc.getValue((ServerMetric) o2.getValue());
				return Integer.compare(v1, v2);
			}
		});

		int ind = mc.isLessBetter() ? 0 : ls.size() - 1;
		return ls.get(ind).getKey();
	}

    @Override
    public String getId() {
        return "BEST_SERVER";
    }

    @Override
    public String getTitle() {
        return "Best server placement strategy";
    }

    @Override
    public boolean hasCriteria() {
        return true;
    }

    @Override
    public List<Criteria> getCriteria() {
        List<Criteria> criteria = new ArrayList<Criteria>();
        Criteria c = new Criteria("MORE_HDD", "More HDD", new Boolean(false));
        criteria.add(c);
        c = new Criteria("MORE_RAM", "More RAM", new Boolean(false));
        criteria.add(c);
        c = new Criteria("MORE_CPU", "More CPU", new Boolean(false));
        criteria.add(c);

        return Collections.unmodifiableList(criteria);
    }
}