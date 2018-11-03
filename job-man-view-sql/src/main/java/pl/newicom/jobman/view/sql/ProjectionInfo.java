package pl.newicom.jobman.view.sql;

import javax.persistence.EntityManager;
import java.util.function.Function;

public class ProjectionInfo<E> {
	private final String streamId;
	private final Function<EntityManager, Projection<E>> provider;

	public ProjectionInfo(String streamId, Function<EntityManager, Projection<E>> provider) {
		this.streamId = streamId;
		this.provider = provider;
	}

	public String getStreamId() {
		return streamId;
	}

	public Function<EntityManager, Projection<E>> getProvider() {
		return provider;
	}
}
