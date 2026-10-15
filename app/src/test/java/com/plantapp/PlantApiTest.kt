package com.plantapp
import org.junit.Assert.*
import org.junit.Test

class PlantApiTest {
    @Test fun parsesSpeciesAndSortsCandidates() {
        val json = """{"results":[{"score":0.2,"species":{"scientificNameWithoutAuthor":"Rosa canina","commonNames":["Dog rose"],"family":{"scientificNameWithoutAuthor":"Rosaceae"}}},{"score":0.95,"species":{"scientificNameWithoutAuthor":"Bellis perennis","commonNames":["Common daisy"],"family":{"scientificNameWithoutAuthor":"Asteraceae"}}}]}"""
        val matches = PlantApi.parseMatches(json)
        assertEquals(2, matches.size)
        assertEquals("Common daisy", matches.first().commonName)
        assertEquals("Asteraceae", matches.first().family)
        assertEquals(0.95, matches.first().confidence, 0.0001)
        assertEquals("https://en.wikipedia.org/wiki/Bellis_perennis", matches.first().wikipediaUrl)
    }
    @Test fun handlesMissingNamesAndEmptyResults() {
        assertTrue(PlantApi.parseMatches("{}" ).isEmpty())
        val matches = PlantApi.parseMatches("""{"results":[{"species":{}},{"species":{"scientificNameWithoutAuthor":"Quercus robur"}}]}""")
        assertEquals(1, matches.size)
        assertEquals("Quercus robur", matches.first().commonName)
        assertEquals(0.0, matches.first().confidence, 0.0)
    }
    @Test fun limitsResultsAndEncodesNames() {
        val item = """{"score":2,"species":{"scientificNameWithoutAuthor":"A & B"}}"""
        val matches = PlantApi.parseMatches("{\"results\":[${List(4) { item }.joinToString(",")}]}")
        assertEquals(3, matches.size)
        assertEquals(1.0, matches.first().confidence, 0.0)
        assertEquals("https://en.wikipedia.org/wiki/A_%26_B", matches.first().wikipediaUrl)
    }
}
