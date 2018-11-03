package pl.newicom.jobman.view.sql;

import java.util.function.Consumer;

import akka.japi.JavaPartialFunction;
import scala.PartialFunction;
import scala.PartialFunction$;

public class ProjectionBuilder<E> {

	private final PartialFunction<E, Void> projection;

	static <T> ProjectionBuilder<T> builder() {
		return new ProjectionBuilder<>(PartialFunction$.MODULE$.empty());
	}

	private ProjectionBuilder(PartialFunction<E, Void> projection) {
		this.projection = projection;
	}

	private <T extends E> PartialFunction<E, Void> toPartialFunction(Class<T> eventClass, Consumer<T> handler) {
		return new JavaPartialFunction<E, Void>() {
			@SuppressWarnings("unchecked")
			@Override
			public Void apply(E in, boolean isCheck) {
				if (eventClass.isAssignableFrom(in.getClass())) {
					if (isCheck) {
						return null;
					} else {
						handler.accept((T) in);
					}
				} else {
					throw noMatch();
				}
				return null;
			}
		};
	}

	public <T extends E> ProjectionBuilder<E> onEvent(Class<T> eventClass, Consumer<T> handler) {
		return new ProjectionBuilder<>(projection.orElse(toPartialFunction(eventClass, handler)));
	}

	public PartialFunction<E, Void> build() {
		return projection;
	}
}
