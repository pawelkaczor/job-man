package pl.newicom.jobman.view.sql.execution;

import static pl.newicom.jobman.view.sql.execution.JobExecutionEntry.FIND_RECENT_BY_JOB_ID;

import javax.persistence.*;
import java.util.Date;

import pl.newicom.jobman.view.sql.AbstractEntity;

@Entity
@Table(name = "EXECUTION_VIEW")
@NamedQueries( {
		@NamedQuery(name = FIND_RECENT_BY_JOB_ID, query = "SELECT e FROM JobExecutionEntry e WHERE e.jobId = :jobId order by e.jobStartDate desc")
})
public class JobExecutionEntry extends AbstractEntity {

	static final String FIND_RECENT_BY_JOB_ID = "JobExecutionEntry.findRecentByJobId";

	@Column(name = "QUEUE_ID")
	private Integer queueId;

	@Column(name = "JOB_ID")
	private String jobId;

	@Column(name = "JOB_TYPE")
	private String jobType;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "JOB_START_DATE")
	private Date jobStartDate;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "JOB_END_DATE")
	private Date jobEndDate;

	@Column(name = "JOB_RESULT")
	private Boolean jobResult;

	@Column(name = "JOB_EXECUTION_STATUS")
	@Enumerated(value = EnumType.STRING)
	private JobExecutionStatus jobExecutionStatus;

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

	public Date getJobStartDate() {
		return jobStartDate;
	}

	public void setJobStartDate(Date jobStartDate) {
		this.jobStartDate = jobStartDate;
	}

	public Date getJobEndDate() {
		return jobEndDate;
	}

	public void setJobEndDate(Date jobEndDate) {
		this.jobEndDate = jobEndDate;
	}

	public Boolean getJobResult() {
		return jobResult;
	}

	public void setJobResult(Boolean jobResult) {
		this.jobResult = jobResult;
	}

	public JobExecutionStatus getJobExecutionStatus() {
		return jobExecutionStatus;
	}

	public void setJobExecutionStatus(JobExecutionStatus jobExecutionStatus) {
		this.jobExecutionStatus = jobExecutionStatus;
	}
}
