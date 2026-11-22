package com.example.system

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

data class BatteryStatusInfo(
    val levelPercent: Int,
    val isCharging: Boolean,
    val pluggedSource: String,
    val healthLabel: String,
    val temperatureCelsius: Float,
    val voltageMv: Int,
    val technology: String,
    val currentMa: Int = 0,
    val lastUpdatedMillis: Long = System.currentTimeMillis()
)

object BatteryMonitor {
    fun readStatus(context: Context, intentOverride: Intent? = null): BatteryStatusInfo {
        val intent = intentOverride ?: context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager

        val propCapacity = try {
            bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        } catch (_: Exception) {
            -1
        }
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val intentPct = if (level >= 0 && scale > 0) ((level * 100f) / scale).toInt().coerceIn(0, 100) else -1
        val pct = when {
            propCapacity in 0..100 -> propCapacity
            intentPct in 0..100 -> intentPct
            else -> 0
        }

        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        val bmCharging = try {
            bm?.isCharging ?: false
        } catch (_: Exception) {
            false
        }
        val isCharging = bmCharging ||
            plugged != 0 ||
            status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

        val pluggedSource = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC Adapter"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
            else -> if (isCharging) "Charger" else "Battery"
        }

        val health = intent?.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)
            ?: BatteryManager.BATTERY_HEALTH_UNKNOWN
        val healthLabel = when (health) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            else -> "Good"
        }

        val tempTenths = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        val voltage = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
        val tech = intent?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY)?.takeIf { it.isNotBlank() } ?: "Li-ion"
        val currentUa = try {
            bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) ?: 0
        } catch (_: Exception) {
            0
        }
        val currentMa = if (currentUa != Int.MIN_VALUE && currentUa != 0) currentUa / 1000 else 0

        return BatteryStatusInfo(
            levelPercent = pct,
            isCharging = isCharging,
            pluggedSource = pluggedSource,
            healthLabel = healthLabel,
            temperatureCelsius = tempTenths / 10f,
            voltageMv = voltage,
            technology = tech,
            currentMa = currentMa,
            lastUpdatedMillis = System.currentTimeMillis()
        )
    }
}

data class SpeedTestMeasurement(
    val downloadMbps: Double,
    val uploadMbps: Double,
    val latencyMs: Long
)

object SpeedTestRunner {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun runTest(): Result<SpeedTestMeasurement> = withContext(Dispatchers.IO) {
        try {
            // 1. Latency probe
            val latStart = System.nanoTime()
            val headReq = Request.Builder()
                .url("https://speed.cloudflare.com/__down?bytes=1")
                .build()
            client.newCall(headReq).execute().use { resp ->
                if (!resp.isSuccessful) error("Probe HTTP ${resp.code}")
            }
            val latencyMs = ((System.nanoTime() - latStart) / 1_000_000L).coerceAtLeast(1L)

            // 2. Download throughput probe (2 MB)
            val dlStart = System.nanoTime()
            val dlReq = Request.Builder()
                .url("https://speed.cloudflare.com/__down?bytes=2000000")
                .build()
            var bytesRead = 0L
            client.newCall(dlReq).execute().use { resp ->
                if (!resp.isSuccessful) error("Download test HTTP ${resp.code}")
                val stream = resp.body?.byteStream() ?: error("Empty download body")
                val buf = ByteArray(16384)
                while (true) {
                    val read = stream.read(buf)
                    if (read == -1) break
                    bytesRead += read
                }
            }
            val dlSec = ((System.nanoTime() - dlStart) / 1_000_000_000.0).coerceAtLeast(0.05)
            val downloadMbps = (bytesRead * 8.0) / (dlSec * 1_000_000.0)

            // 3. Upload throughput probe (500 KB)
            val payload = ByteArray(500_000) { 0x41 }
            val ulStart = System.nanoTime()
            val ulReq = Request.Builder()
                .url("https://speed.cloudflare.com/__up")
                .post(payload.toRequestBody("application/octet-stream".toMediaType()))
                .build()
            client.newCall(ulReq).execute().use { resp ->
                if (!resp.isSuccessful) error("Upload test HTTP ${resp.code}")
            }
            val ulSec = ((System.nanoTime() - ulStart) / 1_000_000_000.0).coerceAtLeast(0.05)
            val uploadMbps = (payload.size * 8.0) / (ulSec * 1_000_000.0)

            Result.success(
                SpeedTestMeasurement(
                    downloadMbps = downloadMbps,
                    uploadMbps = uploadMbps,
                    latencyMs = latencyMs
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

object ScientificCalculatorEngine {
    fun evaluate(expression: String): Result<Double> {
        return try {
            val cleaned = expression
                .replace("×", "*")
                .replace("÷", "/")
                .replace("−", "-")
                .replace("π", Math.PI.toString())
                .replace("e", Math.E.toString())
                .replace(" ", "")
            require(cleaned.isNotEmpty()) { "Empty expression" }
            val parser = Parser(cleaned)
            val value = parser.parseExpression()
            if (parser.pos < cleaned.length) {
                throw IllegalArgumentException("Unexpected character: ${cleaned[parser.pos]}")
            }
            if (value.isNaN() || value.isInfinite()) {
                throw ArithmeticException("Undefined mathematical result")
            }
            Result.success(value)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private class Parser(private val str: String) {
        var pos = 0

        private fun ch(): Char = if (pos < str.length) str[pos] else '\u0000'
        private fun eat(target: Char): Boolean {
            if (ch() == target) {
                pos++
                return true
            }
            return false
        }

        fun parseExpression(): Double {
            var x = parseTerm()
            while (true) {
                x = when {
                    eat('+') -> x + parseTerm()
                    eat('-') -> x - parseTerm()
                    else -> return x
                }
            }
        }

        private fun parseTerm(): Double {
            var x = parseFactor()
            while (true) {
                x = when {
                    eat('*') -> x * parseFactor()
                    eat('/') -> x / parseFactor()
                    eat('%') -> x % parseFactor()
                    else -> return x
                }
            }
        }

        private fun parseFactor(): Double {
            if (eat('+')) return parseFactor()
            if (eat('-')) return -parseFactor()
            var x: Double
            val startPos = pos
            if (eat('(')) {
                x = parseExpression()
                require(eat(')')) { "Missing ')'" }
            } else if (ch() in '0'..'9' || ch() == '.') {
                while (ch() in '0'..'9' || ch() == '.') pos++
                x = str.substring(startPos, pos).toDouble()
            } else if (ch() in 'a'..'z') {
                while (ch() in 'a'..'z') pos++
                val func = str.substring(startPos, pos)
                x = parseFactor()
                x = when (func) {
                    "sqrt" -> sqrt(x)
                    "sin" -> sin(Math.toRadians(x))
                    "cos" -> cos(Math.toRadians(x))
                    "tan" -> tan(Math.toRadians(x))
                    "log" -> log10(x)
                    "ln" -> ln(x)
                    else -> throw IllegalArgumentException("Unknown function: $func")
                }
            } else {
                throw IllegalArgumentException("Unexpected character: ${ch()}")
            }
            if (eat('^')) x = x.pow(parseFactor())
            return x
        }
    }
}

object UnitConverterEngine {
    enum class Category(val label: String, val units: List<String>) {
        LENGTH("Length", listOf("Meter", "Kilometer", "Centimeter", "Mile", "Yard", "Foot", "Inch")),
        WEIGHT("Weight", listOf("Kilogram", "Gram", "Milligram", "Pound", "Ounce")),
        TEMPERATURE("Temperature", listOf("Celsius", "Fahrenheit", "Kelvin")),
        SPEED("Speed", listOf("km/h", "m/s", "mph", "knots")),
        TIME("Time", listOf("Second", "Minute", "Hour", "Day", "Week")),
        VOLUME("Volume", listOf("Liter", "Milliliter", "Gallon (US)", "Cup")),
        AREA("Area", listOf("Sq Meter", "Sq Kilometer", "Hectare", "Acre", "Sq Foot"))
    }

    fun convert(category: Category, value: Double, from: String, to: String): Double {
        if (from == to) return value
        if (category == Category.TEMPERATURE) {
            val celsius = when (from) {
                "Celsius" -> value
                "Fahrenheit" -> (value - 32.0) * 5.0 / 9.0
                "Kelvin" -> value - 273.15
                else -> value
            }
            return when (to) {
                "Celsius" -> celsius
                "Fahrenheit" -> celsius * 9.0 / 5.0 + 32.0
                "Kelvin" -> celsius + 273.15
                else -> celsius
            }
        }
        val toBase = factorToBase(category, from)
        val fromBase = factorToBase(category, to)
        return (value * toBase) / fromBase
    }

    private fun factorToBase(category: Category, unit: String): Double = when (category) {
        Category.LENGTH -> when (unit) {
            "Meter" -> 1.0
            "Kilometer" -> 1000.0
            "Centimeter" -> 0.01
            "Mile" -> 1609.344
            "Yard" -> 0.9144
            "Foot" -> 0.3048
            "Inch" -> 0.0254
            else -> 1.0
        }
        Category.WEIGHT -> when (unit) {
            "Kilogram" -> 1.0
            "Gram" -> 0.001
            "Milligram" -> 0.000001
            "Pound" -> 0.45359237
            "Ounce" -> 0.0283495231
            else -> 1.0
        }
        Category.SPEED -> when (unit) {
            "m/s" -> 1.0
            "km/h" -> 1.0 / 3.6
            "mph" -> 0.44704
            "knots" -> 0.514444
            else -> 1.0
        }
        Category.TIME -> when (unit) {
            "Second" -> 1.0
            "Minute" -> 60.0
            "Hour" -> 3600.0
            "Day" -> 86400.0
            "Week" -> 604800.0
            else -> 1.0
        }
        Category.VOLUME -> when (unit) {
            "Liter" -> 1.0
            "Milliliter" -> 0.001
            "Gallon (US)" -> 3.78541178
            "Cup" -> 0.236588236
            else -> 1.0
        }
        Category.AREA -> when (unit) {
            "Sq Meter" -> 1.0
            "Sq Kilometer" -> 1_000_000.0
            "Hectare" -> 10_000.0
            "Acre" -> 4046.8564224
            "Sq Foot" -> 0.09290304
            else -> 1.0
        }
        Category.TEMPERATURE -> 1.0
    }
}
