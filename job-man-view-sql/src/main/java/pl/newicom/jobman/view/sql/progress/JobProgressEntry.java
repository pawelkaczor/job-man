package pl.newicom.jobman.view.sql.progress;

import static pl.newicom.jobman.view.sql.progress.JobProgressEntry.FIND_BY_JOB_EXECUTION_ID;
import static pl.newicom.jobman.view.sql.progress.JobProgressEntry.FIND_BY_JOB_ID;

import javax.persistence.*;
import java.time.Duration;

import pl.newicom.jobman.view.sql.AbstractEntity;

@Entity
@Table(name = "PROGRESS_VIEW")
@NamedQueries( {
		@NamedQuery(name = FIND_BY_JOB_ID, query = "SELECT e FROM JobProgressEntry e WHERE e.jobId = :jobId"),
		@NamedQuery(name = FIND_BY_JOB_EXECUTION_ID, query = "SELECT e FROM JobProgressEntry e WHERE e.jobExecutionId = :jobExecutionId")
})
public class JobProgressEntry extends AbstractEntity {

	static final String FIND_BY_JOB_ID = "JobProgressEntry.findByJobId";
	static final String FIND_BY_JOB_EXECUTION_ID = "JobProgressEntry.findByJobExecutionId";

	@Column(name = "JOB_ID")
	private String jobId;

	@Column(name = "JOB_EXECUTION_ID", unique = true)
	private String jobExecutionId;

	@Column(name = "TASKS_STARTED")
	private Integer nrOfTasksStarted;

	@Column(name = "TASKS_COMPLETED")
	private Integer nrOfTasksCompleted;

	@Column(name = "REMAINING_TIME")
	private String remainingTime;

	public JobProgressEntry() {}

	public JobProgressEntry(String jobId, String jobExecutionId) {
		this.jobId = jobId;
		this.jobExecutionId = jobExecutionId;
	}

	public String getJobId() {
		return jobId;
	}

	public void setJobId(String jobId) {
		this.jobId = jobId;
	}

	public String getJobExecutionId() {
		return jobExecutionId;
	}

	public void setJobExecutionId(String jobExecutionId) {
		this.jobExecutionId = jobExecutionId;
	}

	public Integer getNrOfTasksStarted() {
		return nrOfTasksStarted;
	}

	public void setNrOfTasksStarted(Integer nrOfTasksStarted) {
		this.nrOfTasksStarted = nrOfTasksStarted;
	}

	public Integer getNrOfTasksCompleted() {
		return nrOfTasksCompleted;
	}

	public void setNrOfTasksCompleted(Integer nrOfTasksCompleted) {
		this.nrOfTasksCompleted = nrOfTasksCompleted;
	}

	public String getRemainingTime() {
		return remainingTime;
	}

	public void setRemainingTime(String remainingTime) {
		this.remainingTime = remainingTime;
	}

	public void updateProgress(Integer nrOfTasksDiff) {
		int started = this.nrOfTasksStarted == null ? 0 : this.nrOfTasksStarted;
		int completed = this.nrOfTasksCompleted == null ? 0 : this.nrOfTasksCompleted;
		if (nrOfTasksDiff > 0) {
			setNrOfTasksStarted(started + nrOfTasksDiff);
		} else if (nrOfTasksDiff < 0) {
			setNrOfTasksCompleted(completed + Math.abs(nrOfTasksDiff));
		}
	}

	public void updateRemainingTime(Duration remainingTime) {
		this.remainingTime = remainingTime.withNanos(0).toString();
	}
}
