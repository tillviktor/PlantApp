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
    @Test fun retainsEveryProvidedCommonName() {
        val matches = PlantApi.parseMatches("""{"results":[{"species":{"scientificNameWithoutAuthor":"Bellis perennis","commonNames":["Százszorszép","Common daisy","English daisy"]}}]}""")
        assertEquals(listOf("Százszorszép", "Common daisy", "English daisy"), matches.first().alternativeNames)
    }
    @Test fun resolvesNormalizedAndRedirectedScientificTitles() {
        val body = """{"query":{"normalized":[{"from":"Bellis_perennis","to":"Bellis perennis"}],"redirects":[{"from":"Bellis perennis","to":"Common daisy"}],"pages":{"1":{"title":"Common daisy","pageprops":{"wikibase_item":"Q123"}},"-1":{"title":"Missing","missing":""}}}}"""
        assertEquals(mapOf("Bellis_perennis" to "Q123"), PlantApi.parseWikiIds(body, listOf("Bellis_perennis", "Missing")))
    }
    @Test fun parsesBothLanguagesAndAliasesWithoutUnrelatedFields() {
        val body = """{"entities":{"Q123":{"labels":{"hu":{"value":"százszorszép"},"en":{"value":"common daisy"},"de":{"value":"Gänseblümchen"}},"aliases":{"en":[{"value":"English daisy"},{"value":"common daisy"}],"hu":[{"value":"százszorszép"}]}}}}"""
        assertEquals(listOf("százszorszép", "common daisy", "English daisy"), PlantApi.parseAlternativeNames(body)["Q123"])
        assertTrue(PlantApi.parseAlternativeNames("{}").isEmpty())
    }

}
