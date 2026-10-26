package com.example.ui

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.zmanim.ZmanimStore
import com.example.domain.zmanim.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.*
import kotlin.coroutines.resume

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ZmanimViewModel(application: Application) : AndroidViewModel(application) {
    private val store = ZmanimStore(application)
    val preferences = store.state
    private val selectedDate = MutableStateFlow(LocalDate.now())
    private val calculator = ZmanimCalculator()
    private val singavi = com.example.data.zmanim.SingaviZmanimRepository(application)
    private val refresh = MutableStateFlow(0L)
    private data class Calculation(val date: LocalDate, val location: ZmanimLocation?, val minutes: Int?, val israel: Boolean?, val method: ZmanimMethod)
    val day = combine(selectedDate, preferences, refresh) { date, prefs, _ ->
        Calculation(date, prefs.location, prefs.candleLightingMinutes, prefs.holidayInIsrael, prefs.method)
    }.mapLatest { request -> request.location?.let { location ->
        if(request.method == ZmanimMethod.SINGAVI) singavi.day(request.date, location, request.minutes, request.israel)
        else withContext(Dispatchers.Default) { calculator.calculate(request.date, location, request.minutes, request.israel) }
    } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    private val mutableMessage = MutableStateFlow<String?>(null)
    val message = mutableMessage.asStateFlow()
    private val mutableLocating = MutableStateFlow(false)
    val locating = mutableLocating.asStateFlow()
    private var request: Job? = null
    fun selectDate(date: LocalDate) { selectedDate.value = date }
    fun update(value: ZmanimPreferences) { if (value.mode != "gps") request?.cancel(); store.update(value) }
    fun stopLocation() { request?.cancel() }
    fun markScrollHintShown() {
        store.update(preferences.value.let { it.copy(scrollHintsShown = (it.scrollHintsShown + 1).coerceAtMost(2)) })
    }
    fun clearMessage() { mutableMessage.value = null }
    fun permissionDenied() {
        preferences.value.takeIf { it.mode == "gps" }?.let { prefs ->
            store.update(prefs.copy(gpsLocation = prefs.gpsLocation?.copy(capturedAt = 0)))
        }
        mutableMessage.value = "לא ניתנה גישה למיקום. אפשר לבחור יישוב קבוע." }
    fun refreshIfNeeded() {
        refresh.value += 1
        val prefs = preferences.value
        if (prefs.enabled && prefs.mode == "gps" && (prefs.location?.capturedAt ?: 0) < System.currentTimeMillis() - 30 * 60_000)
            if (hasPermission()) locate()
    }
    private fun hasPermission() = ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    fun locate() {
        if (request?.isActive == true) return
        if (!hasPermission()) { permissionDenied(); return }
        request = viewModelScope.launch {
            mutableLocating.value = true
            mutableMessage.value = null
            try {
                val location = withTimeoutOrNull(25_000) { currentLocation() }
                if (location == null) { mutableMessage.value = "המיקום אינו זמין כרגע. אפשר לנסות שוב או לבחור יישוב קבוע."; return@launch }
                val zone = com.example.data.zmanim.OfflineTimezone.at(getApplication(), location.latitude, location.longitude)
                val sample = ZmanimLocation("המיקום שלי", location.latitude, location.longitude, zone,
                    true, if (location.hasAccuracy()) location.accuracy else null, location.time)
                store.update(preferences.value.copy(mode = "gps", gpsLocation = sample))
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { mutableMessage.value = "לא ניתן לעדכן מיקום. המיקום הקודם נשמר; אפשר לבחור יישוב קבוע." }
            finally { mutableLocating.value = false }
        }
    }
    @Suppress("DEPRECATION", "MissingPermission")
    private suspend fun currentLocation(): Location? = suspendCancellableCoroutine { continuation ->
        val manager = getApplication<Application>().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                if (continuation.isActive) { manager.removeUpdates(this); continuation.resume(location) }
            }
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        }
        continuation.invokeOnCancellation { manager.removeUpdates(listener) }
        val providers = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
            .filter { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }
        var requested = false
        providers.forEach { provider ->
            runCatching { manager.requestSingleUpdate(provider, listener, Looper.getMainLooper()); requested = true }
        }
        if (!requested && continuation.isActive) continuation.resume(null)
    }
}
