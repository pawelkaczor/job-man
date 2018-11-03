package pl.newicom.jobman.view.sql;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

public class TransactionManager {
	private final EntityManager em;

	public TransactionManager(EntityManager em) {
		this.em = em;
	}

	public EntityManager getEm() {
		return em;
	}

	public EntityTransaction beginTransaction() {
		EntityTransaction tx = em.getTransaction();
		tx.begin();
		return tx;
	}

	public void commitOrRollback(EntityTransaction tx, boolean success) {
		if (success) {
			commitTransaction(tx);
		} else {
			rollbackTransaction(tx);
		}
		em.close();
	}

	public void commitTransaction(EntityTransaction tx) {
		tx.commit();
	}

	public void rollbackTransaction(EntityTransaction tx) {
		if (tx.isActive()) {
			tx.rollback();
		}
	}

}
