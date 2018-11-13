package pl.newicom.jobman.progress;

import static java.time.Duration.*;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import java.time.Duration;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.CompletionStage;

import akka.Done;
import akka.NotUsed;
import akka.stream.OverflowStrategy;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import pl.newicom.jobman.DistributedPubSubFacade;
import pl.newicom.jobman.JobMan;
import pl.newicom.jobman.progress.event.*;

public class JobExecutionStatisticsPublisher {

	private int tasksStarted;
	private int tasksCompleted;
	private LocalTime recentTimestamp;
	private List<Measurement> measurements = new ArrayList<>();

	private List<JobExecutionStatisticsEvent> onProgressUpdated(JobProgressUpdated event) {
		String jobExecutionId = event.jobExecutionId();
		Integer nrOfTasksDiff = event.nrOfTasksDiff();

		if (nrOfTasksDiff > 0) {
			tasksStarted += nrOfTasksDiff;
			recentTimestamp = event.time();
			measurements = new ArrayList<>();

			return emptyList();
		} else if (recentTimestamp == null) { // out of order event received ?
			return emptyList();
		} else {
			tasksCompleted += -nrOfTasksDiff;
			measurements.add(new Measurement(between(recentTimestamp, event.time()), -nrOfTasksDiff));
			recentTimestamp = event.time();

			if (remainingTasks().equals(0)) {
				return singletonList(new JobExecutionCompleted(jobExecutionId));
			} else {
				return Arrays.asList(
					new JobRemainingTimeUpdated(jobExecutionId, remainingTime()),
					new TaskAvgExecutionTimeUpdated(jobExecutionId, avgTaskDuration())
				);
			}
		}
	}

	private Integer remainingTasks() {
		return tasksStarted - tasksCompleted;
	}

	private Duration remainingTime() {
		return avgTaskDuration().multipliedBy(remainingTasks());
	}

	private Duration avgTaskDuration() {
		return measurements.stream()
				.reduce(Measurement::withMeasurement)
				.map(Measurement::avgTaskDuration)
				.orElse(ZERO);
	}

	private static class Measurement {
		private final Duration duration;
		private final Integer nrOfTasks;

		private Measurement(Duration duration, Integer nrOfTasks) {
			this.duration = duration;
			this.nrOfTasks = nrOfTasks;
		}

		Measurement withMeasurement(Measurement other) {
			return new Measurement(duration.plus(other.duration), nrOfTasks + other.nrOfTasks);
		}

		Duration avgTaskDuration() {
			return duration.dividedBy(nrOfTasks);
		}

	}

	public static void start(JobMan jm, Set<Class<? extends JobExecutionStatisticsEvent>> filter) {
		jobExecutionStatistics(jobProgressUpdates(jm))
				.filter(event -> filter.stream().anyMatch(filterClass -> filterClass.isAssignableFrom(event.getClass())))
				.to(jobProgressPublisher(jm))
				.run(jm.actorMaterializer("JobExecutionStatisticsGenerator failed."));
	}

	private static Source<JobProgressUpdated, NotUsed> jobProgressUpdates(JobMan jm) {
		return Source.actorRef(100, OverflowStrategy.dropHead())
				.mapMaterializedValue(ref -> {
					jm.distributedPubSub().subscribe(ProgressTopic.Name(), ref);
					return NotUsed.getInstance();
				}).filter(e -> e instanceof JobProgressUpdated).map(e -> (JobProgressUpdated)e);

	}

	private static Source<JobExecutionStatisticsEvent, NotUsed> jobExecutionStatistics(Source<JobProgressUpdated, NotUsed> jobProgressUpdates) {
		return jobProgressUpdates
				.groupBy(Integer.MAX_VALUE, JobProgressEvent::jobExecutionId)
				.statefulMapConcat( () -> {
					JobExecutionStatisticsPublisher statsGen = new JobExecutionStatisticsPublisher();
					return statsGen::onProgressUpdated;
				})
				.takeWithin(ofHours(6))
				.takeWhile(e -> !(e instanceof JobExecutionCompleted))
				.async()
				.mergeSubstreams();
	}

	private static Sink<JobExecutionStatisticsEvent, CompletionStage<Done>> jobProgressPublisher(JobMan jobMan) {
		DistributedPubSubFacade pubSub = jobMan.distributedPubSub();
		return Sink.foreach(msg -> pubSub.publish(ProgressTopic.Name(), msg));
	}

}
