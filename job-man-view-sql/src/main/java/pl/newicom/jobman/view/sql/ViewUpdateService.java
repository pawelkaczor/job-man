package pl.newicom.jobman.view.sql;

import static akka.actor.typed.javadsl.Behaviors.*;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.concurrent.Executors.newSingleThreadExecutor;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import akka.actor.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.persistence.query.javadsl.EventsByPersistenceIdQuery;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Sink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.newicom.jobman.progress.ProgressTopic;
import pl.newicom.jobman.progress.event.JobProgressEvent;
import pl.newicom.jobman.progress.event.JobProgressUpdated;
import pl.newicom.jobman.progress.event.JobRemainingTimeUpdated;
import pl.newicom.jobman.view.sql.command.StartProjection;
import pl.newicom.jobman.view.sql.command.Stop;
import pl.newicom.jobman.view.sql.command.ViewUpdateServiceCommand;
import pl.newicom.jobman.view.sql.execution.JobExecutionEntryDao;
import pl.newicom.jobman.view.sql.execution.JobExecutionProjection;
import pl.newicom.jobman.view.sql.progress.JobProgressEntry;
import pl.newicom.jobman.view.sql.progress.JobProgressEntryDao;
import pl.newicom.jobman.view.sql.schedule.JobScheduleEntryDao;
import pl.newicom.jobman.view.sql.schedule.JobScheduleProjection;

public class ViewUpdateService {
	//@non-java-start
	private final static Logger LOG = LoggerFactory.getLogger(ViewUpdateService.class);

	private static EntityManagerFactory emFactory = Persistence.createEntityManagerFactory("job-man-vu-pu");

	private static Processor getProcessor(String viewId) {
		return new Processor(emFactory, newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat(viewId.charAt(0) + "v-%d").build()));
	}

	private static List<ProjectionInfo> projections() {
		return asList(
				new ProjectionInfo<>(
						pl.newicom.jobman.execution.package$.MODULE$.JobExecutionJournalId(),
						em -> new JobExecutionProjection(new JobExecutionEntryDao(em))
				),
				new ProjectionInfo<>(
						pl.newicom.jobman.schedule.package$.MODULE$.JobSchedulingJournalId(),
						em -> new JobScheduleProjection(new JobScheduleEntryDao(em), new JobHeaderDao(em))
				)
		);
	}

	public static Behavior<ViewUpdateServiceCommand> viewUpdateService(ActorRef pubSubMediator, Function<String, ActorMaterializer> actorMaterializerProvider, EventsByPersistenceIdQuery readJournal) {
		Processor mdProcessor = getProcessor("md");
		return setup(context -> {
			startProgressViewUpdate(new DistributedPubSubFacade(pubSubMediator), context);

			projections().forEach(p -> context.getSelf().tell(new StartProjection(p)));

			return receive(ViewUpdateServiceCommand.class)
					.onMessage(StartProjection.class, (ctx, cmd) -> {
						ProjectionInfo<?> projectionInfo = cmd.getProjectionInfo();
						mdProcessor.mapInTx(em -> getViewMetaData(projectionInfo.getStreamId(), em)).thenAccept(vmd ->
								startProjection(actorMaterializerProvider, readJournal, vmd, projectionInfo, getProcessor(vmd.getViewId()))
						);
						return same();
					})
					.onMessage(Stop.class, (ctx, cmd) -> {
						LOG.info("{} received. Stopping View Update Service", cmd);
						return Behavior.stopped();
					})
					.build();
		});
	}

	private static void startProgressViewUpdate(DistributedPubSubFacade pubSub, ActorContext<ViewUpdateServiceCommand> ctx) {
		pubSub.subscribe(ProgressTopic.Name(), createProgressUpdater(ctx));
	}

	@SuppressWarnings("unchecked")
	private static <E> void startProjection(Function<String, ActorMaterializer> actorMaterializerProvider, EventsByPersistenceIdQuery readJournal, ViewMetaData vmd, ProjectionInfo<E> projectionInfo, Processor processor) {
		readJournal.eventsByPersistenceId(vmd.getStreamId(), vmd.getStreamOffset() + 1, Long.MAX_VALUE)
				.filter(envelope -> {
					Object event = envelope.event();
					Class<?> eventClass = event.getClass();
					Projection<E> projection = projectionInfo.getProvider().apply(null); // :-(
					return projection.isApplicableFor(eventClass) && projection.isDefinedAt((E) event);
				})
				.mapAsync(1, envelope -> {
					E event = (E) envelope.event();
					long offset = envelope.sequenceNr();
					return processor.runInTx(em ->
						runProjection(new ViewMetaData(vmd.getViewId(), vmd.getStreamId(), offset), event, projectionInfo.getProvider().apply(em), em)
					).thenApply(r -> envelope); // to avoid null elements in the stream
				}).runWith(Sink.ignore(), actorMaterializerProvider.apply(format("Projection for %s failed.", vmd.getViewId())));

	}

	private static <E> void runProjection(ViewMetaData vmd, E event, Projection<E> projection, EntityManager em) {
		ViewMetaDataDao viewMetaDataDao = new ViewMetaDataDao(em);
		projection.accept(event);
		Optional<ViewMetaData> vmdOpt = viewMetaDataDao.findByViewIdAndStreamId(vmd.getViewId(), vmd.getStreamId());
		if (vmdOpt.isPresent()) {
			vmdOpt.get().setStreamOffset(vmd.getStreamOffset());
		} else {
			viewMetaDataDao.persist(vmd);
		}
	}

	private static JobProgressEntry findOrCreateProgressEntry(String jobId, String jobExecutionId, EntityManager em) {
		return findProgressEntry(jobExecutionId, em).orElseGet(() -> {
			JobProgressEntryDao dao = new JobProgressEntryDao(em);
			JobProgressEntry newEntry = new JobProgressEntry(jobId, jobExecutionId);
			dao.persist(newEntry);
			return newEntry;
		});
	}

	private static Optional<JobProgressEntry> findProgressEntry(String jobExecutionId, EntityManager em) {
		return new JobProgressEntryDao(em).findByJobExecutionId(jobExecutionId);
	}

	private static akka.actor.typed.ActorRef<JobProgressEvent> createProgressUpdater(ActorContext<ViewUpdateServiceCommand> context) {
		Processor processor = getProcessor("ProgressView");
		return context.asJava().spawn(
			receive(JobProgressEvent.class)
				.onMessage(JobProgressUpdated.class, (ctx, ev) -> {
					processor.runInTx(em ->
							findOrCreateProgressEntry(ev.jobId(), ev.jobExecutionId(), em)
							.updateProgress(ev.nrOfTasksDiff())
					);
					return Behavior.same();
				}).onMessage(JobRemainingTimeUpdated.class, (ctx, ev) -> {
					processor.runInTx(em ->
							findProgressEntry(ev.jobExecutionId(), em)
							.ifPresent(entry -> entry.updateRemainingTime(ev.remainingTime()))
					);
					return Behavior.same();
				}).build(),
				"ProgressUpdater"
		);
	}

	private static ViewMetaData getViewMetaData(String streamId, EntityManager em) {
		String viewId = streamId + "View";
		return new ViewMetaDataDao(em).findByViewIdAndStreamId(viewId, streamId).orElse(new ViewMetaData(viewId, streamId));
	}

	//@non-java-end
}
