package org.opencivic.signalos.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlaceNamesTest {

    @Test
    void displayKeepsAccentsAndNormalizesSpacingAndCase() {
        assertEquals("Bogotá D.C.", PlaceNames.normalize("  bogotá   d.c. "));
        assertEquals("San José de la Montaña", PlaceNames.normalize("SAN JOSÉ de la MONTAÑA"));
        assertEquals("Villa-Nueva", PlaceNames.normalize("villa-nueva"));
        assertEquals("El Poblado #2", PlaceNames.normalize("el poblado #2"));
    }

    @Test
    void blankBecomesNull() {
        assertNull(PlaceNames.normalize(null));
        assertNull(PlaceNames.normalize("   "));
    }

    @Test
    void rejectsSymbolsAndNamesWithoutLetters() {
        assertThrows(IllegalArgumentException.class, () -> PlaceNames.normalize("<script>alert(1)</script>"));
        assertThrows(IllegalArgumentException.class, () -> PlaceNames.normalize("12345"));
        assertThrows(IllegalArgumentException.class, () -> PlaceNames.normalize("a".repeat(121)));
    }

    @Test
    void keyIgnoresCaseAndAccents() {
        String first = PlaceNames.key("Bogotá", "Chapinero", "El Chicó");
        String second = PlaceNames.key("BOGOTA", "chapinero", "el chico");
        assertEquals(first, second);
        assertEquals("bogota|chapinero|el-chico", first);
    }

    @Test
    void keyIsNullWhenThereIsNoPlace() {
        assertNull(PlaceNames.key(null, null, null));
        assertNull(PlaceNames.key("", " ", null));
    }

    @Test
    void slugFoldsSpanishLetters() {
        assertEquals("nunoa", PlaceNames.slug("Ñuñoa"));
        assertEquals("medellin", PlaceNames.slug("Medellín"));
    }
}
