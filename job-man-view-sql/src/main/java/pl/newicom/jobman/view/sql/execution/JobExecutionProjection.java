package pl.newicom.jobman.view.sql.execution;

import static pl.newicom.jobman.view.sql.execution.JobExecutionStatus.*;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.function.Consumer;

import pl.newicom.jobman.execution.event.*;
import pl.newicom.jobman.view.sql.AbstractProjection;
import pl.newicom.jobman.view.sql.ProjectionBuilder;

public class JobExecutionProjection extends AbstractProjection<JobExecutionEvent> {
	//@non-java-start

	private final JobExecutionEntryDao dao;

	public JobExecutionProjection(JobExecutionEntryDao dao) {
		super(JobExecutionEvent.class);
		this.dao = dao;
	}

	@Override
	public ProjectionBuilder<JobExecutionEvent> logic() {
		return builder()
				.onEvent(JobStarted.class, event -> {
					JobExecutionEntry entry = new JobExecutionEntry();
					entry.setJobId(event.jobId());
					entry.setJobStartDate(toDateTime(event.dateTime()));
					entry.setJobType(event.jobType());
					entry.setQueueId(event.queueId());
					entry.setJobExecutionStatus(STARTED);
					dao.persist(entry);
				})
				.onEvent(JobCompleted.class, event -> onJobEnded(event.jobId(), endDateUpdate(event).andThen(statusUpdate(COMPLETED))))
				.onEvent(JobFailed.class, event -> onJobEnded(event.jobId(), endDateUpdate(event).andThen(statusUpdate(FAILED))))
				.onEvent(JobExpired.class, event -> onJobEnded(event.jobId(), endDateUpdate(event).andThen(statusUpdate(EXPIRED))))
				.onEvent(JobTerminated.class, event -> onJobEnded(event.jobId(), endDateUpdate(event).andThen(statusUpdate(TERMINATED))));

	}

	private Consumer<JobExecutionEntry> statusUpdate(JobExecutionStatus status) {
		return entry -> entry.setJobExecutionStatus(status);
	}

	private Consumer<JobExecutionEntry> endDateUpdate(JobExecutionTerminalEvent event) {
		return entry -> entry.setJobEndDate(toDateTime(event.dateTime()));
	}

	private void onJobEnded(String jobId, Consumer<JobExecutionEntry> update) {
		update.accept(dao.findRecent(jobId));
	}

	private static Date toDateTime(ZonedDateTime input) {
		return Date.from(input.toInstant());
	}

	//@non-java-end

}
