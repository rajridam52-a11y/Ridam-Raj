package com.example

import com.example.data.prefs.DesignSettings
import com.example.data.prefs.ThemePreset
import com.example.data.remote.IndiaRegion
import com.example.data.remote.IndiaWeatherDirectory
import com.example.data.remote.WeatherRepository
import com.example.security.SecurityManager
import com.example.system.ScientificCalculatorEngine
import com.example.system.SystemSchedulers
import com.example.system.UnitConverterEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class RrReminderFunctionTest {

    @Test
    fun indiaWeatherDirectory_coversAllRegionsWithValidIndiaCoordinates() {
        val cities = IndiaWeatherDirectory.cities
        assertTrue("Expected at least 36 Indian cities/UTs", cities.size >= 36)

        val regionsPresent = cities.map { it.region }.toSet()
        val expectedRegions = IndiaRegion.entries.filter { it != IndiaRegion.ALL }.toSet()
        assertEquals(expectedRegions, regionsPresent)

        val names = cities.map { it.name }.toSet()
        assertTrue(names.contains("New Delhi"))
        assertTrue(names.contains("Mumbai"))
        assertTrue(names.contains("Bengaluru"))
        assertTrue(names.contains("Chennai"))
        assertTrue(names.contains("Kolkata"))
        assertTrue(names.contains("Hyderabad"))
        assertTrue(names.contains("Guwahati"))
        assertTrue(names.contains("Srinagar"))
        assertTrue(names.contains("Port Blair"))

        cities.forEach { city ->
            assertTrue("${city.name} lat out of India bounds", city.latitude in 6.0..38.0)
            assertTrue("${city.name} lon out of India bounds", city.longitude in 68.0..98.0)
            assertTrue(city.stateOrUt.isNotBlank())
        }
    }

    @Test
    fun weatherMappings_returnExpectedLabels() {
        assertEquals("Clear Sky", WeatherRepository.wmoCodeToCondition(0))
        assertEquals("Rain", WeatherRepository.wmoCodeToCondition(63))
        assertEquals("Thunderstorm", WeatherRepository.wmoCodeToCondition(95))
        assertEquals("Good", WeatherRepository.aqiToLabel(35))
        assertEquals("Moderate", WeatherRepository.aqiToLabel(80))
        assertEquals("Unhealthy", WeatherRepository.aqiToLabel(180))
    }

    @Test
    fun scientificCalculator_evaluatesArithmeticAndFunctions() {
        assertEquals(17.0, ScientificCalculatorEngine.evaluate("sqrt(144)+sin(30)*10").getOrThrow(), 1e-6)
        assertEquals(125.0, ScientificCalculatorEngine.evaluate("5^3").getOrThrow(), 1e-6)
        assertEquals(2.0, ScientificCalculatorEngine.evaluate("log(100)").getOrThrow(), 1e-6)
        assertTrue(ScientificCalculatorEngine.evaluate("10/0").isFailure)
        assertTrue(ScientificCalculatorEngine.evaluate("").isFailure)
    }

    @Test
    fun unitConverter_convertsAcrossAllCategories() {
        assertEquals(1000.0, UnitConverterEngine.convert(UnitConverterEngine.Category.LENGTH, 1.0, "Kilometer", "Meter"), 1e-4)
        assertEquals(212.0, UnitConverterEngine.convert(UnitConverterEngine.Category.TEMPERATURE, 100.0, "Celsius", "Fahrenheit"), 1e-4)
        assertEquals(1000.0, UnitConverterEngine.convert(UnitConverterEngine.Category.WEIGHT, 1.0, "Kilogram", "Gram"), 1e-4)
        assertEquals(36.0, UnitConverterEngine.convert(UnitConverterEngine.Category.SPEED, 10.0, "m/s", "km/h"), 1e-3)
        assertEquals(3600.0, UnitConverterEngine.convert(UnitConverterEngine.Category.TIME, 1.0, "Hour", "Second"), 1e-4)
    }

    @Test
    fun securityManager_hashesAndVerifiesPinSafely() {
        val salt = SecurityManager.generateSalt()
        val hash = SecurityManager.hashPin("2580", salt)
        assertNotEquals("2580", hash)
        assertTrue(SecurityManager.verifyPin("2580", salt, hash))
        assertFalse(SecurityManager.verifyPin("1234", salt, hash))
        assertFalse(SecurityManager.verifyPin("2580", "", hash))
    }

    @Test
    fun alarmScheduler_computesFutureTriggerMillis() {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply { add(Calendar.MINUTE, 5) }
        val trigger = SystemSchedulers.nextAlarmTriggerMillis(
            hour = cal.get(Calendar.HOUR_OF_DAY),
            minute = cal.get(Calendar.MINUTE),
            repeatDaysMask = 0
        )
        assertTrue(trigger > now)
        assertTrue(trigger - now <= 86_400_000L)
    }

    @Test
    fun designSettings_supportsAllThemesAndDefaults() {
        val defaults = DesignSettings()
        assertTrue(defaults.isDarkMode)
        assertEquals(10, ThemePreset.entries.size)
        assertTrue(defaults.dashboardOrder.isNotEmpty())
    }
}
