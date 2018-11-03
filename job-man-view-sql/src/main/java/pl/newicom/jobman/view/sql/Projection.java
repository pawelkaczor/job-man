package pl.newicom.jobman.view.sql;

import java.util.function.Consumer;

public interface Projection<E> extends Consumer<E> {

	ProjectionBuilder<E> logic();

	boolean isDefinedAt(E event);

	boolean isApplicableFor(Class<?> eventClass);

}
