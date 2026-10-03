package org.opencivic.signalos.web.dto;

/**
 * One column in the municipal ticket export, described so a city can pin an importer to it.
 *
 * <p>Declared as its own DTO rather than reusing the service's internal record: the contract
 * is a published artefact that outlives any implementation, and the two should be able to
 * diverge without either one lying about the other.
 */
public record MunicipalTicketFieldResponse(
    String name,
    String type,
    String description
) {}