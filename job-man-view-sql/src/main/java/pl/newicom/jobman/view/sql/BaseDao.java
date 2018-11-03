package pl.newicom.jobman.view.sql;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Root;
import java.util.List;
import java.util.Optional;

public class BaseDao<E extends AbstractEntity> {

	private final EntityManager em;

	public BaseDao(final EntityManager em) {
		this.em = em;
	}

	public void persist(final E entity) {
		this.em.persist(entity);
	}

	public E update(final E entity) {
		return this.em.merge(entity);
	}

	public <T extends AbstractEntity> T save(T entity) {
		if (entity.getId() == null) {
			this.em.persist(entity);
		} else {
			entity = this.em.merge(entity);
		}
		return entity;
	}

	public <T extends AbstractEntity> void saveOnlyNew(T entity) {
		if (entity.getId() == null) {
			this.em.persist(entity);
		}
	}

	public <T extends AbstractEntity> List<T> getAll(final Class<T> entityClass, String... eagerFetch) {
		final CriteriaBuilder builder = this.em.getCriteriaBuilder();
		final CriteriaQuery<T> query = builder.createQuery(entityClass);
		Root<T> from = query.from(entityClass);
		for (String fetch : eagerFetch) {
			from.fetch(fetch, JoinType.LEFT);
		}

		query.select(from);

		List<T> result = this.em.createQuery(query).getResultList();
		return result;
	}

	public <EE extends E> Optional<EE> findById(final Class<EE> entityClass, final Object id) {
		return Optional.ofNullable(id != null ? this.em.find(entityClass, id) : null);
	}

	public EntityManager getEm() {
		return this.em;
	}

	protected <T> Optional<T> findOptional(TypedQuery<T> query) {
		try {
			return ofNullable(query.getSingleResult());
		} catch (NoResultException nrx) {
			return empty();
		} catch (NonUniqueResultException nurx) {
			return ofNullable(query.setMaxResults(1).getSingleResult());
		}
	}

	public Integer delete(E entity) {
		this.em.remove(entity);
		return 1;
	}

}
