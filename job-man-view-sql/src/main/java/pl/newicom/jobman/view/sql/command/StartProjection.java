package pl.newicom.jobman.view.sql.command;

import pl.newicom.jobman.view.sql.ProjectionInfo;

public class StartProjection implements ViewUpdateServiceCommand {
	private ProjectionInfo projectionInfo;

	public StartProjection(ProjectionInfo projectionInfo) {
		this.projectionInfo = projectionInfo;
	}

	public ProjectionInfo getProjectionInfo() {
		return projectionInfo;
	}

	public void setProjectionInfo(ProjectionInfo projectionInfo) {
		this.projectionInfo = projectionInfo;
	}
}
