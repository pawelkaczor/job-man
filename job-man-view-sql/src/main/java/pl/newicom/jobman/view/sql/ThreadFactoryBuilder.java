package pl.newicom.jobman.view.sql;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

public final class ThreadFactoryBuilder {
	private String nameFormat = null;
	private Boolean daemon = null;
	private Integer priority = null;
	private UncaughtExceptionHandler uncaughtExceptionHandler = null;
	private ThreadFactory backingThreadFactory = null;

	public ThreadFactoryBuilder() {}

	public ThreadFactoryBuilder setNameFormat(String nameFormat) {
		String unused = format(nameFormat, 0); // fail fast if the format is bad or null
		this.nameFormat = nameFormat;
		return this;
	}

	public ThreadFactoryBuilder setDaemon(boolean daemon) {
		this.daemon = daemon;
		return this;
	}

	public ThreadFactoryBuilder setPriority(int priority) {
		this.priority = priority;
		return this;
	}

	public ThreadFactoryBuilder setUncaughtExceptionHandler(
			UncaughtExceptionHandler uncaughtExceptionHandler) {
		this.uncaughtExceptionHandler = uncaughtExceptionHandler;
		return this;
	}

	public ThreadFactoryBuilder setThreadFactory(ThreadFactory backingThreadFactory) {
		this.backingThreadFactory = backingThreadFactory;
		return this;
	}

	public ThreadFactory build() {
		return doBuild(this);
	}

	private static ThreadFactory doBuild(ThreadFactoryBuilder builder) {
		final String nameFormat = builder.nameFormat;
		final Boolean daemon = builder.daemon;
		final Integer priority = builder.priority;
		final UncaughtExceptionHandler uncaughtExceptionHandler = builder.uncaughtExceptionHandler;
		final ThreadFactory backingThreadFactory =
				(builder.backingThreadFactory != null)
						? builder.backingThreadFactory
						: Executors.defaultThreadFactory();
		final AtomicLong count = (nameFormat != null) ? new AtomicLong(0) : null;
		return new ThreadFactory() {
			@Override
			public Thread newThread(Runnable runnable) {
				Thread thread = backingThreadFactory.newThread(runnable);
				if (nameFormat != null) {
					thread.setName(format(nameFormat, count.getAndIncrement()));
				}
				if (daemon != null) {
					thread.setDaemon(daemon);
				}
				if (priority != null) {
					thread.setPriority(priority);
				}
				if (uncaughtExceptionHandler != null) {
					thread.setUncaughtExceptionHandler(uncaughtExceptionHandler);
				}
				return thread;
			}
		};
	}

	private static String format(String format, Object... args) {
		return String.format(Locale.ROOT, format, args);
	}
}