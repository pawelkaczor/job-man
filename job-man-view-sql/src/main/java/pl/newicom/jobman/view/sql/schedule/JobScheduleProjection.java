package pl.newicom.jobman.view.sql.schedule;

import static java.util.Optional.ofNullable;

import java.util.List;
import java.util.Optional;

import pl.newicom.jobman.Job;
import pl.newicom.jobman.schedule.event.*;
import pl.newicom.jobman.view.sql.AbstractProjection;
import pl.newicom.jobman.view.sql.JobHeader;
import pl.newicom.jobman.view.sql.JobHeaderDao;
import pl.newicom.jobman.view.sql.ProjectionBuilder;

public class JobScheduleProjection extends AbstractProjection<JobScheduleEvent> {
	//@non-java-start
	private final JobScheduleEntryDao scheduleEntryDao;
	private final JobHeaderDao jobHeaderDao;
	//private final JsonSerializer jsonSerializer;

	public JobScheduleProjection(JobScheduleEntryDao scheduleEntryDao, JobHeaderDao jobHeaderDao) {
		super(JobScheduleEvent.class);
		this.scheduleEntryDao = scheduleEntryDao;
		this.jobHeaderDao = jobHeaderDao;
		//this.jsonSerializer = jsonSerializer;
	}

	@Override
	public ProjectionBuilder<JobScheduleEvent> logic() {
		return builder()
				.onEvent(JobAddedToWaitingList.class, event -> {
					Job job = event.job();
					persistOrUpdateJobHeader(job);
					scheduleEntryDao.persist(jobScheduleEntry(event));
				})
				.onEvent(JobScheduleEntryAdded.class, event -> {
					removeEntryFromWaitingList(event);
					Job job = event.job();
					persistOrUpdateJobHeader(job);
					updatePositionsOnScheduleEntryAdded(event);
					scheduleEntryDao.persist(jobScheduleEntry(event));
				})
				.onEvent(JobCanceled.class, event -> removeEntry(event.jobId()))
				.onEvent(JobEnded.class, event -> removeEntry(event.jobId()));
	}

	private void removeEntryFromWaitingList(JobScheduleEntryAdded event) {
		scheduleEntryDao.findByJobId(event.jobId())
				.filter(JobScheduleEntry::isOnWaitingList)
				.ifPresent(this::removeEntry);
	}

	private void removeEntry(String jobId) {
		scheduleEntryDao.findByJobId(jobId).ifPresent(this::removeEntry);
	}

	private void removeEntry(JobScheduleEntry entry) {
		scheduleEntryDao.delete(entry);
		updatePositionsOnEntryRemoved(entry);
	}

	private void updatePositionsOnEntryRemoved(JobScheduleEntry entry) {
		ofNullable(entry.getQueueId())
				.map(scheduleEntryDao::findByQueueId)
				.orElseGet(scheduleEntryDao::getAwaitingList)
		.stream()
		.filter(e -> e.getJobPosition() > entry.getJobPosition())
		.forEach(e -> e.setJobPosition(e.getJobPosition() - 1));

	}

	private void updatePositionsOnScheduleEntryAdded(JobScheduleEntryAdded event) {
		List<JobScheduleEntry> entries = scheduleEntryDao.findByQueueId(event.queueId());
		entries.stream()
				.filter(e -> e.getJobPosition() >= event.position())
				.forEach(e -> e.setJobPosition(e.getJobPosition() + 1));
	}

	private JobScheduleEntry jobScheduleEntry(JobScheduleEntryAdded event) {
		JobScheduleEntry entry = new JobScheduleEntry();
		entry.setJobId(event.jobId());
		entry.setJobType(event.job().jobType());
		entry.setQueueId(event.queueId());
		entry.setJobPosition(event.position());
		return entry;
	}

	private JobScheduleEntry jobScheduleEntry(JobAddedToWaitingList event) {
		JobScheduleEntry entry = new JobScheduleEntry();
		entry.setJobId(event.jobId());
		entry.setJobType(event.job().jobType());
		entry.setJobPosition(event.position());
		return entry;
	}

	private void persistOrUpdateJobHeader(Job job) {
		Optional<JobHeader> jobHeaderOpt = jobHeaderDao.findByJobId(job.id());
		if (jobHeaderOpt.isPresent()) {
			// TODO
			//jobHeaderOpt.get().setJobParameters(jsonSerializer.toJson(job.getParameters()));
			jobHeaderOpt.get().setJobType(job.jobType());
		} else {
			scheduleEntryDao.getEm().persist(new JobHeader(job.id(), job.jobType(), null/* jsonSerializer.toJson(job.getParameters())*/));
		}
	}

	//@non-java-end
}
