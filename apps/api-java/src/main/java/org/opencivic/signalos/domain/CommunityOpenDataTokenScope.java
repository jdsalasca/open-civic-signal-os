package org.opencivic.signalos.domain;

public enum CommunityOpenDataTokenScope {
    EXPORT_SIGNALS(CommunityOpenDataExportType.SIGNALS),
    EXPORT_PROPOSALS(CommunityOpenDataExportType.PROPOSALS),
    EXPORT_VOTES(CommunityOpenDataExportType.VOTES),
    EXPORT_DECISIONS(CommunityOpenDataExportType.DECISIONS),
    EXPORT_METRICS(CommunityOpenDataExportType.METRICS);

    private final CommunityOpenDataExportType exportType;

    CommunityOpenDataTokenScope(CommunityOpenDataExportType exportType) {
        this.exportType = exportType;
    }

    public CommunityOpenDataExportType exportType() {
        return exportType;
    }
}
