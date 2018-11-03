package pl.newicom.jobman.view.sql;

import static pl.newicom.jobman.view.sql.JobHeader.FIND_BY_JOB_ID;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.Optional;

public class JobHeaderDao extends BaseDao<JobHeader> {

	public JobHeaderDao(EntityManager em) {
		super(em);
	}

	public Optional<JobHeader> findByJobId(String jobId) {
		final TypedQuery<JobHeader> query = getEm().createNamedQuery(FIND_BY_JOB_ID, JobHeader.class);
		query.setParameter("jobId", jobId);
		return findOptional(query);
	}

}
