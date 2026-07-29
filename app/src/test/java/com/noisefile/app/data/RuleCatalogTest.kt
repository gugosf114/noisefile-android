package com.noisefile.app.data

import com.noisefile.app.model.NoiseType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.json.JSONObject
import java.io.File

class RuleCatalogTest {
    private fun catalogJson(): String {
        val candidates = listOf(
            File("app/src/main/assets/${RuleCatalog.ASSET_PATH}"),
            File("src/main/assets/${RuleCatalog.ASSET_PATH}"),
        )
        val catalogFile = candidates.firstOrNull { it.isFile }
            ?: error("Could not find ${RuleCatalog.ASSET_PATH}")
        return catalogFile.readText()
    }

    private fun catalog(): RuleCatalog = RuleCatalog.fromJson(catalogJson())

    @Test
    fun sanJoseBarkingWorkflowRequiresFiveIncidents() {
        val rule = catalog().retrieve(RuleCatalog.SAN_JOSE_ID, NoiseType.BARKING_DOG)
            ?: error("Missing San Jose barking-dog rule")

        assertEquals(5, rule.requiredIncidentCount)
        assertEquals("tel:4087947297", rule.actionUri)
        assertTrue(rule.secondaryActionUri?.startsWith("https://www.sanjoseca.gov/") == true)
        assertTrue(rule.officialSourceLabel.contains("7.40.010"))
    }

    @Test
    fun everyRuleHasAnOfficialSourceAndVerificationDate() {
        catalog().rules.forEach { rule ->
            assertTrue(rule.officialSourceUrl.startsWith("https://"))
            assertFalse(rule.officialSourceUrl.contains("local-government-website"))
            assertFalse(rule.officialSourceLabel.endsWith("/ Guidelines"))
            assertTrue(rule.verifiedDate.isNotBlank())
        }
    }

    @Test
    fun everyRuleHasSpecificResidentGuidance() {
        catalog().rules.forEach { rule ->
            assertFalse(rule.title == "Follow city procedures")
            assertFalse(rule.summary == "Follow the local municipal code guidelines.")
        }
    }

    @Test
    fun everyAvailableCityHasAllThreeCategoriesAndSafeComplaintActions() {
        val catalog = catalog()

        assertEquals(15, catalog.jurisdictions.count { it.isAvailable })
        assertEquals(45, catalog.rules.size)
        catalog.jurisdictions.filter { it.isAvailable }.forEach { jurisdiction ->
            assertEquals(
                jurisdiction.id,
                NoiseType.entries.toSet(),
                catalog.forJurisdiction(jurisdiction.id).map { it.noiseType }.toSet(),
            )
        }

        catalog.rules.forEach { rule ->
            assertNull("${rule.id} must not use a phone reading as a legal cutoff", rule.meterLimit)
            if (rule.noiseType == NoiseType.BARKING_DOG) {
                assertTrue(
                    "${rule.id} must state whether a dB threshold exists",
                    rule.summary.contains("dB"),
                )
            }
            assertTrue(
                "${rule.id} has an unsupported primary action",
                rule.actionUri.startsWith("https://") ||
                    rule.actionUri.startsWith("tel:") ||
                    rule.actionUri.startsWith("mailto:"),
            )
            assertEquals(
                "${rule.id} has an incomplete secondary action",
                rule.secondaryActionLabel == null,
                rule.secondaryActionUri == null,
            )
        }

        assertEquals(
            listOf("san-jose-barking-dog" to 5),
            catalog.rules
                .filter { it.requiredIncidentCount != null }
                .map { it.id to it.requiredIncidentCount },
        )
    }

    @Test
    fun sanMateoRulesContainTheActualLocalRequirements() {
        val catalog = catalog()
        val barking = catalog.retrieve("san-mateo", NoiseType.BARKING_DOG)
            ?: error("Missing San Mateo barking rule")
        val music = catalog.retrieve("san-mateo", NoiseType.PARTY_MUSIC)
            ?: error("Missing San Mateo music rule")
        val construction = catalog.retrieve("san-mateo", NoiseType.CONSTRUCTION)
            ?: error("Missing San Mateo construction rule")

        assertTrue(barking.summary.contains("at any hour"))
        assertTrue(barking.summary.contains("eight incidents during one month"))
        assertTrue(music.summary.contains("single-family Zone 1"))
        assertTrue(music.summary.contains("six-minute ambient"))
        assertTrue(construction.summary.contains("Sundays and holidays noon\u20134:00 p.m."))
        assertTrue(construction.summary.contains("90 dB"))
        assertNull(construction.meterLimit)
    }

    @Test
    fun sanJoseAndSanMateoSeparateLegalThresholdsFromComplaintProcedures() {
        val catalog = catalog()
        val sanJoseAnimal = catalog.retrieve("san-jose", NoiseType.BARKING_DOG)
            ?: error("Missing San Jose barking rule")
        val sanJoseNoise = catalog.retrieve("san-jose", NoiseType.PARTY_MUSIC)
            ?: error("Missing San Jose general-noise rule")
        val sanJoseConstruction = catalog.retrieve("san-jose", NoiseType.CONSTRUCTION)
            ?: error("Missing San Jose construction rule")
        val sanMateoAnimal = catalog.retrieve("san-mateo", NoiseType.BARKING_DOG)
            ?: error("Missing San Mateo barking rule")
        val sanMateoNoise = catalog.retrieve("san-mateo", NoiseType.PARTY_MUSIC)
            ?: error("Missing San Mateo general-noise rule")
        val sanMateoConstruction = catalog.retrieve("san-mateo", NoiseType.CONSTRUCTION)
            ?: error("Missing San Mateo construction rule")

        assertTrue(sanJoseAnimal.summary.contains("no fixed duration or dB threshold"))
        assertTrue(sanJoseAnimal.summary.contains("complaint process"))
        assertEquals(5, sanJoseAnimal.requiredIncidentCount)
        assertEquals("tel:4087947297", sanJoseAnimal.actionUri)
        assertTrue(sanJoseAnimal.secondaryActionUri?.contains("showpublisheddocument") == true)
        assertTrue(complaintDestination(sanJoseAnimal).isDocumentPacket)
        assertTrue(sanJoseNoise.summary.contains("no fixed citywide dB threshold"))
        assertEquals("tel:4082778900", sanJoseNoise.actionUri)
        assertTrue(sanJoseConstruction.summary.contains("may expressly allow different hours"))
        assertTrue(sanJoseConstruction.actionUri.endsWith("/code-service-request-form"))

        assertTrue(sanMateoAnimal.summary.contains("Neither rule sets a dB threshold"))
        assertTrue(sanMateoAnimal.summary.contains("currently hosted 2020 prosecution packet"))
        assertTrue(sanMateoAnimal.actionUri.contains("Barking-Dog-Information-Packet"))
        assertTrue(complaintDestination(sanMateoAnimal).isDocumentPacket)
        assertTrue(sanMateoNoise.summary.contains("six-minute ambient"))
        assertTrue(sanMateoNoise.captureInstruction.contains("Type 1 precision meter"))
        assertEquals("tel:6505227700", sanMateoNoise.actionUri)
        assertTrue(sanMateoConstruction.summary.contains("requires at least one of two 90 dB conditions"))
        assertTrue(sanMateoConstruction.summary.contains("approved exception permits"))
        assertEquals("tel:6505227700", sanMateoConstruction.actionUri)
    }

    @Test
    fun fremontSunnyvaleAndSantaClaraExposeAllThreeCurrentRoutesAndTests() {
        val catalog = catalog()
        val fremont = NoiseType.entries.associateWith {
            catalog.retrieve("fremont", it) ?: error("Missing Fremont $it rule")
        }
        val sunnyvale = NoiseType.entries.associateWith {
            catalog.retrieve("sunnyvale", it) ?: error("Missing Sunnyvale $it rule")
        }
        val santaClara = NoiseType.entries.associateWith {
            catalog.retrieve("santa-clara", it) ?: error("Missing Santa Clara $it rule")
        }
        val mySunnyvale =
            "https://www.sunnyvale.ca.gov/city-services/online-services/mysunnyvale"
        val mySantaClara =
            "https://www.santaclaraca.gov/services/make-a-service-request/submit-a-request-online"

        assertTrue(fremont.getValue(NoiseType.BARKING_DOG).summary.contains("no fixed duration or dB threshold"))
        assertTrue(fremont.getValue(NoiseType.BARKING_DOG).summary.contains("two other people"))
        assertTrue(fremont.getValue(NoiseType.BARKING_DOG).actionUri.endsWith("/report/barking"))
        assertTrue(fremont.getValue(NoiseType.PARTY_MUSIC).summary.contains("distance, not 50 dB"))
        assertEquals("tel:5107906800", fremont.getValue(NoiseType.PARTY_MUSIC).actionUri)
        assertTrue(fremont.getValue(NoiseType.CONSTRUCTION).summary.contains("farther than 500 feet"))
        assertEquals("https://fremontapp.com/", fremont.getValue(NoiseType.CONSTRUCTION).actionUri)

        assertTrue(sunnyvale.getValue(NoiseType.BARKING_DOG).summary.contains("There is no dB threshold"))
        assertEquals("tel:4087307110", sunnyvale.getValue(NoiseType.BARKING_DOG).actionUri)
        assertTrue(sunnyvale.getValue(NoiseType.PARTY_MUSIC).summary.contains("Multifamily primary usable open space"))
        assertEquals(mySunnyvale, sunnyvale.getValue(NoiseType.PARTY_MUSIC).secondaryActionUri)
        assertTrue(sunnyvale.getValue(NoiseType.CONSTRUCTION).summary.contains("two unpaid helpers"))
        assertEquals(mySunnyvale, sunnyvale.getValue(NoiseType.CONSTRUCTION).actionUri)

        assertTrue(santaClara.getValue(NoiseType.BARKING_DOG).summary.contains("no fixed minute or dB threshold"))
        assertEquals("tel:4086155580", santaClara.getValue(NoiseType.BARKING_DOG).actionUri)
        assertTrue(santaClara.getValue(NoiseType.PARTY_MUSIC).summary.contains("only to fixed sources"))
        assertEquals(mySantaClara, santaClara.getValue(NoiseType.PARTY_MUSIC).secondaryActionUri)
        assertTrue(santaClara.getValue(NoiseType.CONSTRUCTION).summary.contains("within 300 feet"))
        assertEquals(mySantaClara, santaClara.getValue(NoiseType.CONSTRUCTION).actionUri)
    }

    @Test
    fun antiochBerkeleyAndConcordUseCurrentRequirementsAndComplaintRoutes() {
        val catalog = catalog()

        val antiochAnimal = catalog.retrieve("antioch", NoiseType.BARKING_DOG)
            ?: error("Missing Antioch barking rule")
        val antiochNoise = catalog.retrieve("antioch", NoiseType.PARTY_MUSIC)
            ?: error("Missing Antioch general-noise rule")
        val antiochConstruction = catalog.retrieve("antioch", NoiseType.CONSTRUCTION)
            ?: error("Missing Antioch construction rule")
        assertTrue(antiochAnimal.summary.contains("30 continuous minutes"))
        assertTrue(antiochAnimal.summary.contains("60 intermittent minutes"))
        assertEquals("tel:9257796989", antiochAnimal.actionUri)
        assertTrue(antiochNoise.summary.contains("no fixed citywide dB"))
        assertTrue(antiochConstruction.summary.contains("Weekend and holiday hours are 9:00 a.m.\u20135:00 p.m."))
        assertTrue(antiochConstruction.summary.contains("project-specific waiver"))
        assertTrue(antiochConstruction.actionUri.contains("/report/category/112204"))

        val berkeleyAnimal = catalog.retrieve("berkeley", NoiseType.BARKING_DOG)
            ?: error("Missing Berkeley barking rule")
        val berkeleyNoise = catalog.retrieve("berkeley", NoiseType.PARTY_MUSIC)
            ?: error("Missing Berkeley general-noise rule")
        val berkeleyConstruction = catalog.retrieve("berkeley", NoiseType.CONSTRUCTION)
            ?: error("Missing Berkeley construction rule")
        assertTrue(berkeleyAnimal.summary.contains("separate residences"))
        assertTrue(berkeleyAnimal.summary.contains("recurs within eight hours"))
        assertEquals("tel:5109816600", berkeleyAnimal.actionUri)
        assertTrue(berkeleyNoise.summary.contains("55 dBA"))
        assertTrue(berkeleyNoise.summary.contains("Inside neighboring multifamily units"))
        assertTrue(berkeleyConstruction.summary.contains("short-term mobile equipment"))
        assertTrue(berkeleyConstruction.summary.contains("stationary equipment"))
        assertEquals(
            "https://berkeleyca.qscend.com/311/request/add?typeId=399",
            berkeleyConstruction.actionUri,
        )

        val concordAnimal = catalog.retrieve("concord", NoiseType.BARKING_DOG)
            ?: error("Missing Concord barking rule")
        val concordNoise = catalog.retrieve("concord", NoiseType.PARTY_MUSIC)
            ?: error("Missing Concord general-noise rule")
        val concordConstruction = catalog.retrieve("concord", NoiseType.CONSTRUCTION)
            ?: error("Missing Concord construction rule")
        assertTrue(concordAnimal.summary.contains("15-day compliance period"))
        assertTrue(concordAnimal.summary.contains("no later than 60 days"))
        assertTrue(concordAnimal.actionUri.contains("Noisy-Animal-Complaint-Form-218"))
        assertTrue(concordNoise.summary.contains("no fixed dB cutoff"))
        assertNull(concordNoise.secondaryActionUri)
        assertTrue(concordConstruction.summary.contains("Sunday 8:00 a.m.\u20135:00 p.m. applies only"))
        assertTrue(concordConstruction.actionUri.contains("/report/category/116356"))
    }

    @Test
    fun oaklandUsesCurrentWarningsLimitsAndExactComplaintRoutes() {
        val catalog = catalog()
        val animal = catalog.retrieve("oakland", NoiseType.BARKING_DOG)
            ?: error("Missing Oakland barking rule")
        val noise = catalog.retrieve("oakland", NoiseType.PARTY_MUSIC)
            ?: error("Missing Oakland general-noise rule")
        val construction = catalog.retrieve("oakland", NoiseType.CONSTRUCTION)
            ?: error("Missing Oakland construction rule")

        assertTrue(animal.summary.contains("no fixed minute or dB threshold"))
        assertTrue(animal.summary.contains("persists for more than 15 minutes"))
        assertEquals("tel:5105354884", animal.actionUri)

        assertTrue(noise.summary.contains("two or more separate housing or commercial units"))
        assertTrue(noise.summary.contains("recurs within one week"))
        assertTrue(noise.summary.contains("reduce each listed standard by 5 dBA"))
        assertNull(noise.meterLimit)
        assertEquals("tel:5107773333", noise.actionUri)
        assertTrue(noise.secondaryActionUri?.contains("TabName=Enforcement") == true)

        assertTrue(construction.summary.contains("no blanket hours ban"))
        assertTrue(construction.summary.contains("less than 10 days"))
        assertTrue(construction.summary.contains("Grading has separate hours"))
        assertTrue(construction.summary.contains("unnecessary idling is prohibited"))
        assertTrue(construction.actionUri.contains("TabName=Enforcement"))
        assertEquals("tel:5102383381", construction.secondaryActionUri)
    }

    @Test
    fun haywardUsesCurrentWarningsMeasurementsAndExactComplaintRoutes() {
        val catalog = catalog()
        val animal = catalog.retrieve("hayward", NoiseType.BARKING_DOG)
            ?: error("Missing Hayward barking rule")
        val noise = catalog.retrieve("hayward", NoiseType.PARTY_MUSIC)
            ?: error("Missing Hayward general-noise rule")
        val construction = catalog.retrieve("hayward", NoiseType.CONSTRUCTION)
            ?: error("Missing Hayward construction rule")

        assertTrue(animal.summary.contains("habitually, unnecessarily, and unreasonably"))
        assertTrue(animal.summary.contains("no fixed minute or dB threshold"))
        assertEquals("tel:5102937200", animal.actionUri)
        assertTrue(animal.secondaryActionUri?.endsWith("classificationId=68676") == true)

        assertTrue(noise.summary.contains("70 dBA"))
        assertTrue(noise.summary.contains("60 dBA"))
        assertTrue(noise.summary.contains("17 factors"))
        assertTrue(noise.summary.contains("within 72 hours"))
        assertNull(noise.meterLimit)
        assertEquals("tel:5102937000", noise.actionUri)
        assertTrue(noise.secondaryActionUri?.endsWith("classificationId=12610") == true)

        assertTrue(construction.summary.contains("83 dBA at 25 feet"))
        assertTrue(construction.summary.contains("86 dBA anywhere outside"))
        assertTrue(construction.summary.contains("not an automatic violation"))
        assertTrue(construction.summary.contains("conditional noise permit"))
        assertTrue(construction.secondaryActionUri?.endsWith("classificationId=12611") == true)
    }

    @Test
    fun vallejoUsesCurrentWarningsTablesAndComplaintRoutes() {
        val catalog = catalog()
        val animal = catalog.retrieve("vallejo", NoiseType.BARKING_DOG)
            ?: error("Missing Vallejo barking rule")
        val noise = catalog.retrieve("vallejo", NoiseType.PARTY_MUSIC)
            ?: error("Missing Vallejo general-noise rule")
        val construction = catalog.retrieve("vallejo", NoiseType.CONSTRUCTION)
            ?: error("Missing Vallejo construction rule")

        assertTrue(animal.summary.contains("no fixed minute or dB threshold"))
        assertTrue(animal.summary.contains("two-week barking journal"))
        assertEquals("tel:7077844733", animal.actionUri)
        assertEquals("tel:7074217090", animal.secondaryActionUri)

        assertTrue(noise.summary.contains("more than 30 minutes per hour"))
        assertTrue(noise.summary.contains("50 dBA or ambient"))
        assertTrue(noise.summary.contains("recurrence within one week"))
        assertNull(noise.meterLimit)
        assertTrue(noise.actionUri.endsWith("/report/category/3616"))
        assertEquals("tel:7076484321", noise.secondaryActionUri)

        assertTrue(construction.summary.contains("one-quarter mile"))
        assertTrue(construction.summary.contains("less than 15 days"))
        assertTrue(construction.summary.contains("no Sunday or legal-holiday table allowance"))
        assertTrue(construction.actionUri.endsWith("/report/category/3616"))
        assertEquals("tel:7076484469", construction.secondaryActionUri)
    }

    @Test
    fun dalyCityUsesCurrentAnimalRulesNewConstructionChapterAndExactRoutes() {
        val catalog = catalog()
        val animal = catalog.retrieve("daly-city", NoiseType.BARKING_DOG)
            ?: error("Missing Daly City barking rule")
        val noise = catalog.retrieve("daly-city", NoiseType.PARTY_MUSIC)
            ?: error("Missing Daly City general-noise rule")
        val construction = catalog.retrieve("daly-city", NoiseType.CONSTRUCTION)
            ?: error("Missing Daly City construction rule")

        assertTrue(animal.summary.contains("Neither rule sets a fixed minute or dB threshold"))
        assertTrue(animal.summary.contains("five or more people"))
        assertTrue(animal.summary.contains("five-person requirement"))
        assertTrue(animal.summary.contains("does not require a warning before every citation"))
        assertTrue(animal.summary.contains("not contracted to handle barking complaints"))
        assertEquals("tel:6509918119", animal.actionUri)
        assertTrue(animal.officialSourceUrl.contains("nodeId=TIT6ANCO"))
        assertNull(animal.meterLimit)

        assertTrue(noise.summary.contains("disturbs the public peace at any time"))
        assertTrue(noise.summary.contains("10:00 p.m.–6:00 a.m."))
        assertTrue(noise.summary.contains("No minute or dB threshold"))
        assertTrue(noise.summary.contains("Police Chief permit"))
        assertTrue(noise.summary.contains("does not require a preliminary warning"))
        assertEquals("tel:6509921225", noise.actionUri)
        assertTrue(noise.secondaryActionUri?.endsWith("/439/Daly-City-iHelp") == true)
        assertNull(noise.meterLimit)

        assertTrue(construction.summary.contains("outdoor work on private property"))
        assertTrue(construction.summary.contains("8:00 a.m.–6:30 p.m."))
        assertTrue(construction.summary.contains("8:30 p.m."))
        assertTrue(construction.summary.contains("9:00 a.m.–6:30 p.m."))
        assertTrue(construction.summary.contains("federal holidays"))
        assertTrue(construction.summary.contains("up to one hour before"))
        assertTrue(construction.summary.contains("70–95 dBA at 50 feet"))
        assertTrue(construction.summary.contains("80–105 dBA at 50 feet"))
        assertTrue(construction.summary.contains("not a mandatory prerequisite"))
        assertNull(construction.meterLimit)
        assertTrue(construction.actionUri.endsWith("/439/Daly-City-iHelp"))
        assertEquals("mailto:codeenforcement@dalycity.org", construction.secondaryActionUri)
    }

    @Test
    fun santaRosaUsesCurrentReasonablePersonTablesWarningsAndExactRoutes() {
        val catalog = catalog()
        val animal = catalog.retrieve("santa-rosa", NoiseType.BARKING_DOG)
            ?: error("Missing Santa Rosa barking rule")
        val noise = catalog.retrieve("santa-rosa", NoiseType.PARTY_MUSIC)
            ?: error("Missing Santa Rosa general-noise rule")
        val construction = catalog.retrieve("santa-rosa", NoiseType.CONSTRUCTION)
            ?: error("Missing Santa Rosa construction rule")

        assertTrue(animal.summary.contains("no fixed minute or dB threshold"))
        assertTrue(animal.summary.contains("does not make a preliminary warning"))
        assertTrue(animal.summary.contains("sends the owner a letter"))
        assertTrue(animal.summary.contains("not the legal definition"))
        assertEquals("tel:7075657100", animal.actionUri)
        assertTrue(animal.secondaryActionUri?.contains("report-an-issue-to-animal-services") == true)
        assertEquals("tel:7075657100", complaintDestination(animal).uri)
        assertFalse(complaintDestination(animal).isOnlineForm)
        assertNull(animal.meterLimit)

        assertTrue(noise.summary.contains("reasonable person of normal sensitiveness"))
        assertTrue(noise.summary.contains("50 dBA"))
        assertTrue(noise.summary.contains("55 dBA"))
        assertTrue(noise.summary.contains("60 dBA"))
        assertTrue(noise.summary.contains("does not prove compliance"))
        assertTrue(noise.summary.contains("A weighting and slow response"))
        assertTrue(noise.summary.contains("15 dB"))
        assertTrue(noise.summary.contains("within 200 feet"))
        assertTrue(noise.summary.contains("three days beforehand"))
        assertTrue(noise.summary.contains("does not require a preliminary warning"))
        assertEquals("tel:7075285222", noise.actionUri)
        assertTrue(noise.secondaryActionUri?.contains("Code-Investigation-Request-Form-74") == true)
        assertNull(noise.meterLimit)

        assertTrue(construction.summary.contains("does not establish one universal construction-hours schedule"))
        assertTrue(construction.summary.contains("project-specific hours or limits"))
        assertTrue(construction.summary.contains("50 dBA"))
        assertTrue(construction.summary.contains("65 dBA"))
        assertTrue(construction.summary.contains("70 dBA"))
        assertTrue(construction.summary.contains("75 dBA"))
        assertTrue(construction.summary.contains("does not by itself prove compliance"))
        assertTrue(construction.summary.contains("does not require a preliminary warning"))
        assertTrue(construction.summary.contains("generally sent within two weeks"))
        assertNull(construction.meterLimit)
        assertTrue(construction.actionUri.contains("Code-Investigation-Request-Form-74"))
        assertEquals("tel:7075285222", construction.secondaryActionUri)
    }

    @Test
    fun richmondSeparatesCityAndCountyAnimalRulesAndUsesCurrentComplaintRoutes() {
        val catalog = catalog()
        val animal = catalog.retrieve("richmond", NoiseType.BARKING_DOG)
            ?: error("Missing Richmond barking rule")
        val noise = catalog.retrieve("richmond", NoiseType.PARTY_MUSIC)
            ?: error("Missing Richmond general-noise rule")
        val construction = catalog.retrieve("richmond", NoiseType.CONSTRUCTION)
            ?: error("Missing Richmond construction rule")

        assertTrue(animal.summary.contains("no fixed minute or dB threshold"))
        assertTrue(animal.summary.contains("30 continuous minutes or 60 intermittent minutes"))
        assertTrue(animal.summary.contains("15-day compliance period"))
        assertTrue(animal.summary.contains("no later than 60 days"))
        assertTrue(animal.summary.contains("does not establish compliance"))
        assertEquals(
            "https://www.contracosta.ca.gov/FormCenter/Animal-Services-17/Noisy-Animal-Complaint-Form-218",
            animal.actionUri,
        )
        assertEquals("tel:5102331214", animal.secondaryActionUri)

        assertTrue(noise.summary.contains("at least 10 minutes"))
        assertTrue(noise.summary.contains("two or more separate housing or commercial units"))
        assertTrue(noise.summary.contains("more than five minutes afterward"))
        assertTrue(noise.summary.contains("recurs within 90 days"))
        assertTrue(noise.summary.contains("sole test for that incident"))
        assertTrue(noise.summary.contains("50 dBA or ambient"))
        assertNull(noise.meterLimit)
        assertEquals("tel:5102331214", noise.actionUri)
        assertEquals("https://ims.ci.richmond.ca.us/ims/Account/Login", noise.secondaryActionUri)

        assertTrue(construction.summary.contains("weekdays 7:00 a.m.–6:00 p.m."))
        assertTrue(construction.summary.contains("one to five dwelling units"))
        assertTrue(construction.summary.contains("temporary Building Official waiver"))
        assertTrue(construction.summary.contains("75/80/85 dBA"))
        assertTrue(construction.summary.contains("60/65/70"))
        assertNull(construction.meterLimit)
        assertEquals("https://ims.ci.richmond.ca.us/ims/Account/Login", construction.actionUri)
        assertEquals("tel:5102331214", construction.secondaryActionUri)
    }

    @Test
    fun sanFranciscoUsesCurrentWarningMeasurementExceptionsAndExactRoutes() {
        val catalog = catalog()
        val animal = catalog.retrieve("san-francisco", NoiseType.BARKING_DOG)
            ?: error("Missing San Francisco barking rule")
        val noise = catalog.retrieve("san-francisco", NoiseType.PARTY_MUSIC)
            ?: error("Missing San Francisco general-noise rule")
        val construction = catalog.retrieve("san-francisco", NoiseType.CONSTRUCTION)
            ?: error("Missing San Francisco construction rule")

        assertTrue(animal.summary.contains("continuously and incessantly for 10 minutes"))
        assertTrue(animal.summary.contains("There is no dB threshold"))
        assertTrue(animal.summary.contains("two unrelated people"))
        assertTrue(animal.summary.contains("within 300 feet"))
        assertTrue(animal.summary.contains("warning letters for the first two complaints"))
        assertEquals("tel:311", animal.actionUri)
        assertNull(animal.meterLimit)

        assertTrue(noise.summary.contains("Police Code § 49"))
        assertTrue(noise.summary.contains("50 feet from the source property's boundary"))
        assertTrue(noise.summary.contains("5 dBA above ambient"))
        assertTrue(noise.summary.contains("8 dBC"))
        assertTrue(noise.summary.contains("45 dBA"))
        assertTrue(noise.summary.contains("Type 1 precision meter"))
        assertTrue(noise.summary.contains("Neither § 49 nor Article 29 requires a preliminary warning"))
        assertNull(noise.meterLimit)
        assertEquals("tel:4155530123", noise.actionUri)
        assertTrue(noise.secondaryActionUri?.endsWith("/form/auto/cs_noise") == true)

        assertTrue(construction.summary.contains("80 dBA at 100 feet"))
        assertTrue(construction.summary.contains("impact tools"))
        assertTrue(construction.summary.contains("8:00 p.m.–7:00 a.m."))
        assertTrue(construction.summary.contains("special permit"))
        assertTrue(construction.summary.contains("emergency work is exempt"))
        assertTrue(construction.summary.contains("does not require a preliminary warning"))
        assertNull(construction.meterLimit)
        assertTrue(construction.actionUri.endsWith("/form/auto/cs_noise"))
        assertEquals("tel:4155530123", construction.secondaryActionUri)
    }

    @Test
    fun sanJoseIncludesConstructionWorkflow() {
        val construction = catalog().retrieve(RuleCatalog.SAN_JOSE_ID, NoiseType.CONSTRUCTION)
            ?: error("Missing San Jose construction rule")

        assertTrue(construction.summary.contains("7:00 a.m."))
        assertTrue(construction.summary.contains("prohibits it on weekends"))
        assertTrue(construction.officialSourceLabel.contains("20.100.450"))
    }

    @Test
    fun sunnyvaleAndSantaClaraUseTheirCurrentOnlineRequestPages() {
        val catalog = catalog()
        val mySunnyvale =
            "https://www.sunnyvale.ca.gov/city-services/online-services/mysunnyvale"
        val mySantaClara =
            "https://www.santaclaraca.gov/services/make-a-service-request/submit-a-request-online"

        assertEquals(
            mySunnyvale,
            catalog.retrieve("sunnyvale", NoiseType.PARTY_MUSIC)?.secondaryActionUri,
        )
        assertEquals(
            mySunnyvale,
            catalog.retrieve("sunnyvale", NoiseType.CONSTRUCTION)?.actionUri,
        )
        assertEquals(
            mySantaClara,
            catalog.retrieve("santa-clara", NoiseType.PARTY_MUSIC)?.secondaryActionUri,
        )
        assertEquals(
            mySantaClara,
            catalog.retrieve("santa-clara", NoiseType.CONSTRUCTION)?.actionUri,
        )
    }

    @Test
    fun unavailableCityNeverFallsBackToAnotherCityRule() {
        val catalog = catalog()

        assertTrue(catalog.forJurisdiction("not-a-city").isEmpty())
        assertNull(catalog.retrieve("not-a-city", NoiseType.CONSTRUCTION))
        assertNull(catalog.jurisdictionById("not-a-city"))
        assertNull(catalog.byId("not-a-rule"))
    }

    @Test
    fun catalogIsVersionedAndUsesSupportedSchema() {
        val catalog = catalog()

        assertEquals(1, catalog.schemaVersion)
        assertTrue(catalog.catalogVersion.isNotBlank())
    }

    @Test
    fun duplicateCityAndCategoryIsRejected() {
        val root = JSONObject(catalogJson())
        val rules = root.getJSONArray("rules")
        val duplicate = JSONObject(rules.getJSONObject(0).toString())
            .put("id", "duplicate-rule-id")
        rules.put(duplicate)

        assertThrows(IllegalArgumentException::class.java) {
            RuleCatalog.fromJson(root.toString())
        }
    }

    @Test
    fun missingCityCategoryIsRejected() {
        val root = JSONObject(catalogJson())
        val rules = root.getJSONArray("rules")
        rules.remove(rules.length() - 1)

        assertThrows(IllegalArgumentException::class.java) {
            RuleCatalog.fromJson(root.toString())
        }
    }

    @Test
    fun insecureOfficialSourceIsRejected() {
        val root = JSONObject(catalogJson())
        root.getJSONArray("rules")
            .getJSONObject(0)
            .put("officialSourceUrl", "http://example.com/not-official")

        assertThrows(IllegalArgumentException::class.java) {
            RuleCatalog.fromJson(root.toString())
        }
    }

    @Test
    fun placeholderOfficialSourceIsRejected() {
        val root = JSONObject(catalogJson())
        root.getJSONArray("rules")
            .getJSONObject(0)
            .put("officialSourceUrl", "https://local-government-website.gov")

        assertThrows(IllegalArgumentException::class.java) {
            RuleCatalog.fromJson(root.toString())
        }
    }

    @Test
    fun incompleteMeterLimitIsRejected() {
        val root = JSONObject(catalogJson())
        root.getJSONArray("rules")
            .getJSONObject(0)
            .put(
                "meterLimit",
                JSONObject()
                    .put("daytimeMaximumDb", 60)
                    .put("comparisonContext", "Residential property"),
            )

        assertThrows(IllegalArgumentException::class.java) {
            RuleCatalog.fromJson(root.toString())
        }
    }
}
