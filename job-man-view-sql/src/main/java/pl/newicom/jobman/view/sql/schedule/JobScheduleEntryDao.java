package pl.newicom.jobman.view.sql.schedule;

import static pl.newicom.jobman.view.sql.schedule.JobScheduleEntry.*;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.Optional;

import pl.newicom.jobman.view.sql.BaseDao;

public class JobScheduleEntryDao extends BaseDao<JobScheduleEntry> {

	public JobScheduleEntryDao(EntityManager em) {
		super(em);
	}

	Optional<JobScheduleEntry> findByJobId(String jobId) {
		final TypedQuery<JobScheduleEntry> query = getEm().createNamedQuery(FIND_BY_JOB_ID, JobScheduleEntry.class);
		query.setParameter("jobId", jobId);
		return findOptional(query);
	}

	List<JobScheduleEntry> getAwaitingList() {
		final TypedQuery<JobScheduleEntry> query = getEm().createNamedQuery(GET_AWAITING_LIST, JobScheduleEntry.class);
		return query.getResultList();
	}

	List<JobScheduleEntry> findByQueueId(Integer queueId) {
		final TypedQuery<JobScheduleEntry> query = getEm().createNamedQuery(FIND_BY_QUEUE_ID, JobScheduleEntry.class);
		query.setParameter("queueId", queueId);
		return query.getResultList();
	}

}
