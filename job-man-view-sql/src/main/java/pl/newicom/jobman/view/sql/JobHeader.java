package pl.newicom.jobman.view.sql;

import javax.persistence.*;

@Entity
@NamedQueries( {
		@NamedQuery(name = JobHeader.FIND_BY_JOB_ID, query = "SELECT e FROM JobHeader e WHERE e.jobId = :jobId")
})
@Table(name = "JOB_HEADER_VIEW", uniqueConstraints = @UniqueConstraint(name = "JOB_HEADER_VIEW_U1", columnNames = { "JOB_ID"}))
public class JobHeader extends AbstractEntity {

	static final String FIND_BY_JOB_ID = "JobHeader.findByJobId";

	@Column(name = "JOB_ID")
	private String jobId;

	@Column(name = "JOB_TYPE")
	private String jobType;

	@Lob
	@Column(name = "JOB_PARAMS")
	private String jobParameters;

	public JobHeader() {}

	public JobHeader(String jobId, String jobType, String jobParameters) {
		this.jobId = jobId;
		this.jobType = jobType;
		this.jobParameters = jobParameters;
	}

	public String getJobId() {
		return jobId;
	}

	public void setJobId(String jobId) {
		this.jobId = jobId;
	}

	public String getJobParameters() {
		return jobParameters;
	}

	public void setJobParameters(String jobParameters) {
		this.jobParameters = jobParameters;
	}

	public String getJobType() {
		return jobType;
	}

	public void setJobType(String jobType) {
		this.jobType = jobType;
	}
}
