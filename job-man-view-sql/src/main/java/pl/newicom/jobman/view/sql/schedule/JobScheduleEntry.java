package pl.newicom.jobman.view.sql.schedule;

import static pl.newicom.jobman.view.sql.schedule.JobScheduleEntry.FIND_BY_JOB_ID;
import static pl.newicom.jobman.view.sql.schedule.JobScheduleEntry.FIND_BY_QUEUE_ID;
import static pl.newicom.jobman.view.sql.schedule.JobScheduleEntry.GET_AWAITING_LIST;

import javax.persistence.*;

import pl.newicom.jobman.view.sql.AbstractEntity;

@Entity
@Table(name = "SCHEDULE_VIEW", uniqueConstraints = @UniqueConstraint(name = "SCHEDULE_ENTRY_U1", columnNames = { "JOB_ID"}))
@NamedQueries( {
		@NamedQuery(name = FIND_BY_JOB_ID, query = "SELECT e FROM JobScheduleEntry e WHERE e.jobId = :jobId"),
		@NamedQuery(name = FIND_BY_QUEUE_ID, query = "SELECT e FROM JobScheduleEntry e WHERE e.queueId = :queueId"),
		@NamedQuery(name = GET_AWAITING_LIST, query = "SELECT e FROM JobScheduleEntry e WHERE e.queueId is null")
})
public class JobScheduleEntry extends AbstractEntity {

	static final String FIND_BY_JOB_ID = "JobScheduleEntry.findByJobId";
	static final String FIND_BY_QUEUE_ID = "JobScheduleEntry.findByQueueId";
	static final String GET_AWAITING_LIST = "JobScheduleEntry.getAwaitingList";

	@Column(name = "QUEUE_ID")
	private Integer queueId;

	@Column(name = "JOB_ID")
	private String jobId;

	@Column(name = "JOB_TYPE")
	private String jobType;

	@Column(name = "JOB_POSITION")
	private Integer jobPosition;

	public Integer getQueueId() {
		return queueId;
	}

	public void setQueueId(Integer queueId) {
		this.queueId = queueId;
	}

	public String getJobId() {
		return jobId;
	}

	public void setJobId(String jobId) {
		this.jobId = jobId;
	}

	public String getJobType() {
		return jobType;
	}

	public void setJobType(String jobType) {
		this.jobType = jobType;
	}

	public Integer getJobPosition() {
		return jobPosition;
	}

	public void setJobPosition(Integer jobPosition) {
		this.jobPosition = jobPosition;
	}

	@Transient
	public boolean isOnWaitingList() {
		return queueId == null;
	}
}
