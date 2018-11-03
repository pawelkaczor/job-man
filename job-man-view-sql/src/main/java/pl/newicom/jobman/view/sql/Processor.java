package pl.newicom.jobman.view.sql;

import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.concurrent.CompletableFuture.supplyAsync;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Processor {
	private static final Logger LOG = LoggerFactory.getLogger(Processor.class);

	private final EntityManagerFactory emFactory;
	private final ExecutorService executor;

	public Processor(EntityManagerFactory emFactory, ExecutorService executor) {
		this.emFactory = emFactory;
		this.executor = executor;
	}


	public CompletableFuture<Void> runInTx(Consumer<EntityManager> callable) {
		return runAsync(() -> {
			TransactionManager txMgr = txManager();
			EntityTransaction tx = txMgr.beginTransaction();
			try {
				callable.accept(txMgr.getEm());
				txMgr.commitTransaction(tx);
			} catch (RuntimeException ex) {
				rollback(txMgr, tx, ex);
				throw ex;
			}
		});
	}

	public <R> CompletableFuture<R> mapInTx(Function<EntityManager, R> callable) {
		return supplyAsync(() -> {
			TransactionManager txMgr = txManager();
			EntityTransaction tx = txMgr.beginTransaction();
			boolean success = false;
			R result;
			try {
				result = callable.apply(txMgr.getEm());
				success = true;
			} finally {
				txMgr.commitOrRollback(tx, success);
			}
			return result;
		});
	}

	private static void rollback(TransactionManager txMgr, EntityTransaction tx, RuntimeException ex) {
		LOG.debug("Transaction failed", ex);
		if (tx != null) {
			try {
				txMgr.rollbackTransaction(tx);
			} catch (Exception innerEx) {
				LOG.debug("Transaction rollback failed", innerEx);
			}
		}
	}

	private TransactionManager txManager() {
		return new TransactionManager(emFactory.createEntityManager());
	}

}
