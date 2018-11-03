package pl.newicom.jobman.view.sql.progress;

import static pl.newicom.jobman.view.sql.progress.JobProgressEntry.FIND_BY_JOB_EXECUTION_ID;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.Optional;

import pl.newicom.jobman.view.sql.BaseDao;

public class JobProgressEntryDao extends BaseDao<JobProgressEntry> {

	public JobProgressEntryDao(EntityManager em) {
		super(em);
	}

	public Optional<JobProgressEntry> findByJobExecutionId(String jobExecutionId) {
		final TypedQuery<JobProgressEntry> query = getEm().createNamedQuery(FIND_BY_JOB_EXECUTION_ID, JobProgressEntry.class);
		query.setParameter("jobExecutionId", jobExecutionId);
		return findOptional(query);
	}

}
