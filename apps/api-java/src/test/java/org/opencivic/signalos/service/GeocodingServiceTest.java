package org.opencivic.signalos.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GeocodingServiceTest {

    @Test
    void mapsNominatimAddressToCityLocalityAndNeighborhood() throws Exception {
        String body = """
            {
              "display_name": "Chapinero, Bogotá, Cundinamarca, Colombia",
              "address": {
                "suburb": "Chapinero",
                "neighbourhood": "El Chicó",
                "city": "Bogotá",
                "state": "Cundinamarca",
                "country": "Colombia"
              }
            }
            """;

        GeocodingService.PlaceSuggestion place = new GeocodingService().parse(body);

        assertEquals("Bogotá", place.city());
        assertEquals("Chapinero", place.locality());
        assertEquals("El Chicó", place.neighborhood());
        assertEquals("Cundinamarca", place.region());
        assertEquals("Colombia", place.country());
    }

    @Test
    void fallsBackToTownAndMunicipalityWhenCityIsMissing() throws Exception {
        String body = """
            {
              "address": {
                "town": "Chía",
                "municipality": "Chía",
                "country": "Colombia"
              }
            }
            """;

        GeocodingService.PlaceSuggestion place = new GeocodingService().parse(body);

        assertEquals("Chía", place.city());
        assertEquals("Colombia", place.country());
    }
}
