package pl.newicom.jobman.view.sql;

import javax.persistence.*;

@Entity
@Table(name = "VIEW_METADATA", uniqueConstraints = @UniqueConstraint(name = "VMD_U1", columnNames = { "VIEW_ID", "STREAM_ID" }))
@NamedQueries( {
		@NamedQuery(name = ViewMetaData.FIND_BY_VIEW_STREAM_ID, query = "SELECT v FROM ViewMetaData v WHERE v.viewId = :viewId and v.streamId = :streamId")
})
public class ViewMetaData extends AbstractEntity {

	static final String FIND_BY_VIEW_STREAM_ID = "ViewMetaData.findByViewIdAndStreamId";

	@Column(name = "VIEW_ID")
	private String viewId;

	@Column(name = "STREAM_ID")
	private String streamId;

	@Column(name = "STREAM_OFFSET")
	private Long streamOffset;

	public ViewMetaData() {}

	ViewMetaData(String viewId, String streamId) {
		this.viewId = viewId;
		this.streamId = streamId;
		this.streamOffset = -1L;
	}

	ViewMetaData(String viewId, String streamId, Long streamOffset) {
		this.viewId = viewId;
		this.streamId = streamId;
		this.streamOffset = streamOffset;
	}

	public String getViewId() {
		return viewId;
	}

	public void setViewId(String viewId) {
		this.viewId = viewId;
	}

	public String getStreamId() {
		return streamId;
	}

	public void setStreamId(String streamId) {
		this.streamId = streamId;
	}

	public Long getStreamOffset() {
		return streamOffset;
	}

	public void setStreamOffset(Long streamOffset) {
		this.streamOffset = streamOffset;
	}
}
