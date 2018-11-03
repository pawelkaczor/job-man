package pl.newicom.jobman.view.sql;

import static pl.newicom.jobman.view.sql.ViewMetaData.FIND_BY_VIEW_STREAM_ID;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.Optional;

public class ViewMetaDataDao extends BaseDao<ViewMetaData> {

	public ViewMetaDataDao(EntityManager em) {
		super(em);
	}

	public Optional<ViewMetaData> findByViewIdAndStreamId(String viewId, String streamId) {
		final TypedQuery<ViewMetaData> query = getEm().createNamedQuery(FIND_BY_VIEW_STREAM_ID, ViewMetaData.class);
		query.setParameter("viewId", viewId);
		query.setParameter("streamId", streamId);
		return findOptional(query);
	}
}
