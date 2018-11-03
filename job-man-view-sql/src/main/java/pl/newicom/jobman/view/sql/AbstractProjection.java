package pl.newicom.jobman.view.sql;

import scala.PartialFunction;

public abstract class AbstractProjection<E> implements Projection<E> {

	private final PartialFunction<E, Void> projection;
	private final Class<E> eventBaseClass;

	public AbstractProjection(Class<E> eventClass) {
		this.eventBaseClass = eventClass;
		projection = logic().build();
	}

	protected ProjectionBuilder<E> builder() {
		return ProjectionBuilder.builder();
	}

	@Override
	public boolean isDefinedAt(E event) {
		return projection.isDefinedAt(event);
	}

	@Override
	public void accept(E e) {
		projection.apply(e);
	}

	@Override
	public boolean isApplicableFor(Class<?> eventClass) {
		return eventBaseClass.isAssignableFrom(eventClass);
	}

}
