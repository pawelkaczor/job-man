package pl.newicom.jobman.view.sql.execution;

import static pl.newicom.jobman.view.sql.execution.JobExecutionEntry.FIND_RECENT_BY_JOB_ID;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import java.util.List;

import pl.newicom.jobman.view.sql.BaseDao;

public class JobExecutionEntryDao extends BaseDao<JobExecutionEntry> {

	public JobExecutionEntryDao(EntityManager em) {
		super(em);
	}

	JobExecutionEntry findRecent(String jobId) {
		final TypedQuery<JobExecutionEntry> query = getEm().createNamedQuery(FIND_RECENT_BY_JOB_ID, JobExecutionEntry.class);
		query.setParameter("jobId", jobId);
		query.setMaxResults(1);
		List<JobExecutionEntry> result = query.getResultList();
		return result.isEmpty() ? null : result.get(0);
	}
}
