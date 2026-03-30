package com.carpark.android.viewmodel

import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.carpark.android.data.local.SavedParkingPreferences
import com.carpark.android.data.model.*
import com.carpark.android.data.repository.ParkingRepository
import com.carpark.android.util.LocationHelper
import android.util.Log
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ParkingUiState(
    val parkingLots: List<ParkingLot> = emptyList(),
    val searchPlaces: List<KakaoPlace> = emptyList(),
    val savedLots: List<ParkingLot> = emptyList(),
    val savedLotsLoading: Boolean = false,
    val savedHasMore: Boolean = false,
    val nearbyLots: List<NearbyParkingLot> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val mode: DataMode = DataMode.NOT_LINKED,
    val isNearbyMode: Boolean = false,
    val selectedLot: ParkingLot? = null,
    val detailLot: ParkingLot? = null,
    val activeTab: TabId = TabId.HOME,
    val myPageRoute: MyPageRoute = MyPageRoute.ROOT,
    val searchResultsOpen: Boolean = false,
    val searchQuery: String = "",
    val centerRegion: String = "",
    val gpsLoading: Boolean = false,
    val sheetOpen: Boolean = false,
    val savedOpen: Boolean = false,
    val nearbyExpanded: Boolean = false,
    val savedExpanded: Boolean = false,
    val panTo: LatLngPoint? = null,
    val userLocation: LatLngPoint? = null,
    val userBearing: Float? = null,
)

data class LatLngPoint(val lat: Double, val lng: Double)

class ParkingViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ParkingRepository()
    private val savedPrefs = SavedParkingPreferences(application)

    private val _uiState = MutableStateFlow(ParkingUiState())
    val uiState: StateFlow<ParkingUiState> = _uiState.asStateFlow()

    private var refreshJob: Job? = null
    private var currentBounds: MapBounds? = null
    private var savedNextCursor: Long? = null
    private var currentDistrict: String? = null

    private val sensorManager =
        application.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationSensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val compassListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return
            val rotMatrix = FloatArray(9)
            SensorManager.getRotationMatrixFromVector(rotMatrix, event.values)
            val orientation = FloatArray(3)
            SensorManager.getOrientation(rotMatrix, orientation)
            val azimuth = ((Math.toDegrees(orientation[0].toDouble()) + 360) % 360).toFloat()
            // 5도 단위로 반올림해서 너무 잦은 리컴포지션 방지
            val rounded = (Math.round(azimuth / 5f) * 5 % 360).toFloat()
            if (rounded != _uiState.value.userBearing) {
                _uiState.update { it.copy(userBearing = rounded) }
            }
        }
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
    }

    companion object {
        private const val REFRESH_INTERVAL = 30_000L
    }

    init {
        rotationSensor?.let {
            sensorManager.registerListener(compassListener, it, SensorManager.SENSOR_DELAY_UI)
        }
        // 앱 시작 시 정적 주차장 데이터를 미리 로드하고 초기 화면 마커를 즉시 표시
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            repository.loadStaticLots(getApplication())
            // 초기 지도 위치(서울, zoom 13)에 해당하는 bounds 계산 후 즉시 마커 세팅
            val zoomLevel = 13.0
            val centerLat = 37.5665
            val centerLng = 126.978
            val latSpan = 180.0 / Math.pow(2.0, zoomLevel) * 2
            val lngSpan = 360.0 / Math.pow(2.0, zoomLevel) * 2
            val initialBounds = MapBounds(
                swLat = centerLat - latSpan / 2,
                swLng = centerLng - lngSpan / 2,
                neLat = centerLat + latSpan / 2,
                neLng = centerLng + lngSpan / 2,
            )
            val lots = repository.getStaticLotsInBounds(getApplication(), initialBounds)
            _uiState.update { it.copy(parkingLots = lots) }
        }
        // 서버에서 즐겨찾기 로드
        viewModelScope.launch {
            loadFavorites(refresh = true)
        }
    }

    // --- Data Loading ---

    private fun loadParkingLots(mode: DataMode, bounds: MapBounds? = null, district: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            try {
                if (mode == DataMode.NOT_LINKED && bounds != null) {
                    val lots = repository.getStaticLotsInBounds(getApplication(), bounds)
                    _uiState.update { it.copy(parkingLots = lots) }
                } else {
                    val lots = repository.fetchParkingLots(mode, bounds, district)
                    if (lots.isNotEmpty()) {
                        _uiState.update { it.copy(parkingLots = lots) }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "데이터를 불러올 수 없습니다") }
            } finally {
                _uiState.update { it.copy(loading = false) }
            }
        }
    }

    private fun startAutoRefresh() {
        refreshJob?.cancel()
        if (_uiState.value.mode != DataMode.REALTIME) return
        refreshJob = viewModelScope.launch {
            while (true) {
                delay(REFRESH_INTERVAL)
                if (_uiState.value.isNearbyMode) continue
                try {
                    val lots = repository.fetchParkingLots(
                        _uiState.value.mode, currentBounds, currentDistrict
                    )
                    if (lots.isNotEmpty()) {
                        _uiState.update { it.copy(parkingLots = lots) }
                    }
                } catch (_: Exception) { }
            }
        }
    }

    // --- Public Actions ---

    fun updateBounds(bounds: MapBounds, region: String? = null) {
        currentBounds = bounds
        if (!region.isNullOrBlank()) {
            val parts = region.split(" ")
            currentDistrict = if (parts.size > 1) parts[1].trim() else null
            _uiState.update { it.copy(centerRegion = region) }
        }
        if (_uiState.value.isNearbyMode) return
        loadParkingLots(_uiState.value.mode, bounds, currentDistrict)
        startAutoRefresh()
    }

    fun changeMode(newMode: DataMode) {
        _uiState.update { it.copy(mode = newMode) }
        refreshJob?.cancel()
        loadParkingLots(newMode, currentBounds, currentDistrict)
        if (newMode == DataMode.REALTIME) startAutoRefresh()
    }

    fun selectLot(lot: ParkingLot?) {
        if (lot != null && _uiState.value.selectedLot?.id == lot.id) {
            _uiState.update { it.copy(selectedLot = null) }
            return
        }
        _uiState.update { it.copy(selectedLot = lot) }
        // NOT_LINKED 모드에서는 정적 데이터만 있으므로 API로 상세 조회
        if (lot != null && _uiState.value.mode == DataMode.NOT_LINKED) {
            viewModelScope.launch {
                try {
                    val full = repository.fetchParkingDetail(lot.id)
                    _uiState.update { it.copy(selectedLot = full) }
                } catch (_: Exception) { /* 실패 시 기본 정보 유지 */ }
            }
        }
    }

    fun showDetail(lot: ParkingLot) {
        viewModelScope.launch {
            try {
                val full = repository.fetchParkingDetail(lot.id)
                _uiState.update { it.copy(detailLot = full) }
            } catch (_: Exception) {
                _uiState.update { it.copy(detailLot = lot) }
            }
        }
    }

    fun closeDetail() {
        _uiState.update { it.copy(detailLot = null) }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun isSavedLot(lotId: Int): Boolean {
        return _uiState.value.savedLots.any { it.id == lotId }
    }

    fun toggleSavedLot(lot: ParkingLot) {
        val isSaved = isSavedLot(lot.id)
        // 낙관적 UI 업데이트
        if (isSaved) {
            _uiState.update { it.copy(savedLots = it.savedLots.filter { l -> l.id != lot.id }) }
        } else {
            _uiState.update { it.copy(savedLots = listOf(lot) + it.savedLots) }
        }
        viewModelScope.launch {
            try {
                if (isSaved) repository.removeFavorite(lot.id)
                else repository.addFavorite(lot.id)
            } catch (e: Exception) {
                // 서버 실패 시 원복
                loadFavorites(refresh = true)
            }
        }
    }

    fun removeSavedLot(lotId: Int) {
        _uiState.update { it.copy(savedLots = it.savedLots.filter { it.id != lotId }) }
        viewModelScope.launch {
            try {
                repository.removeFavorite(lotId)
            } catch (e: Exception) {
                loadFavorites(refresh = true)
            }
        }
    }

    private suspend fun loadFavorites(refresh: Boolean) {
        if (refresh) savedNextCursor = null
        _uiState.update { it.copy(savedLotsLoading = true) }
        try {
            val page = repository.fetchFavorites(
                cursor = if (refresh) null else savedNextCursor,
                size = 10,
            )
            savedNextCursor = page.nextCursor
            _uiState.update {
                it.copy(
                    savedLots = if (refresh) page.content else it.savedLots + page.content,
                    savedHasMore = page.hasNext,
                    savedLotsLoading = false,
                )
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(savedLotsLoading = false) }
            Log.e("ParkingViewModel", "Failed to load favorites", e)
        }
    }

    fun loadMoreFavorites() {
        if (_uiState.value.savedLotsLoading || !_uiState.value.savedHasMore) return
        viewModelScope.launch { loadFavorites(refresh = false) }
    }

    fun refreshFavorites() {
        viewModelScope.launch { loadFavorites(refresh = true) }
    }

    fun selectSavedLot(lot: ParkingLot) {
        _uiState.update {
            it.copy(
                activeTab = TabId.HOME,
                savedOpen = false,
                selectedLot = lot,
                panTo = LatLngPoint(lot.latDouble, lot.lngDouble),
            )
        }
    }

    // --- Tab Navigation ---

    fun onTabChange(tab: TabId) {
        when (tab) {
            TabId.NEARBY -> {
                _uiState.update {
                    it.copy(
                        savedOpen = false,
                        savedExpanded = false,
                        searchResultsOpen = false,
                        activeTab = tab,
                    )
                }
                if (_uiState.value.isNearbyMode) {
                    _uiState.update { it.copy(sheetOpen = !it.sheetOpen) }
                    return
                }
                requestNearbySearch()
            }
            TabId.HOME -> {
                _uiState.update {
                    it.copy(
                        savedOpen = false,
                        savedExpanded = false,
                        sheetOpen = false,
                        activeTab = tab,
                    )
                }
            }
            TabId.SAVED -> {
                _uiState.update {
                    it.copy(
                        sheetOpen = false,
                        searchResultsOpen = false,
                        activeTab = tab,
                        savedOpen = !it.savedOpen,
                    )
                }
            }
            TabId.MY -> {
                _uiState.update {
                    it.copy(
                        sheetOpen = false,
                        searchResultsOpen = false,
                        savedOpen = false,
                        savedExpanded = false,
                        activeTab = tab,
                        myPageRoute = MyPageRoute.ROOT,
                    )
                }
            }
        }
    }

    fun navigateMyPage(route: MyPageRoute) {
        _uiState.update { it.copy(activeTab = TabId.MY, myPageRoute = route) }
    }

    fun searchPlaces(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            _uiState.update { it.copy(searchPlaces = emptyList(), searchResultsOpen = false) }
            return
        }

        _uiState.update { it.copy(searchQuery = trimmed, loading = true, error = null) }

        viewModelScope.launch {
            try {
                if (com.carpark.android.BuildConfig.KAKAO_REST_API_KEY.isBlank()) {
                    throw IllegalStateException("KAKAO_REST_API_KEY is missing in local.properties")
                }

                val results = repository.searchPlaces(trimmed, currentBounds)
                _uiState.update {
                    it.copy(
                        searchPlaces = results,
                        searchResultsOpen = results.isNotEmpty(),
                        loading = false,
                        error = if (results.isEmpty()) "No Kakao map results found" else null,
                    )
                }

                results.firstOrNull()?.let { first ->
                    _uiState.update {
                        it.copy(
                            panTo = LatLngPoint(first.latitude, first.longitude),
                            activeTab = TabId.HOME,
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        loading = false,
                        searchResultsOpen = false,
                        error = e.message ?: "Kakao search failed",
                    )
                }
            }
        }
    }

    fun selectSearchPlace(place: KakaoPlace) {
        _uiState.update {
            it.copy(
                searchResultsOpen = false,
                panTo = LatLngPoint(place.latitude, place.longitude),
                searchQuery = place.place_name,
                activeTab = TabId.HOME,
            )
        }
    }

    fun closeSearchResults() {
        _uiState.update { it.copy(searchResultsOpen = false) }
    }

    // --- Nearby Search ---

    private fun requestNearbySearch() {
        viewModelScope.launch {
            _uiState.update { it.copy(gpsLoading = true) }
            val location = LocationHelper.getCurrentLocation(getApplication())
            val lat = location?.latitude ?: 37.27903037476364
            val lng = location?.longitude ?: 127.46299871026446
            _uiState.update { it.copy(gpsLoading = false) }
            doNearbySearch(lat, lng)
        }
    }

    fun doNearbySearch(lat: Double, lng: Double) {
        _uiState.update {
            it.copy(
                userLocation = LatLngPoint(lat, lng),
                panTo = LatLngPoint(lat, lng),
            )
        }
        viewModelScope.launch {
            refreshJob?.cancel()
            _uiState.update { it.copy(loading = true, isNearbyMode = true, sheetOpen = true) }
            try {
                val nearby = repository.fetchNearbyLots(lat, lng)
                _uiState.update {
                    it.copy(
                        nearbyLots = nearby,
                        parkingLots = nearby.map { n -> n.lot },
                        error = null,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = e.message ?: "주변 주차장을 불러올 수 없습니다",
                        isNearbyMode = false,
                    )
                }
            } finally {
                _uiState.update { it.copy(loading = false) }
            }
        }
    }

    fun reSearchNearby() {
        val center = _uiState.value.userLocation ?: return
        doNearbySearch(center.lat, center.lng)
    }

    fun closeNearbySheet() {
        _uiState.update { it.copy(sheetOpen = false, isNearbyMode = false, nearbyExpanded = false) }
        loadParkingLots(_uiState.value.mode, currentBounds, currentDistrict)
        startAutoRefresh()
    }

    fun minimizeSheet() {
        _uiState.update { it.copy(sheetOpen = false, nearbyExpanded = false) }
    }

    fun setNearbyExpanded(expanded: Boolean) {
        _uiState.update { it.copy(nearbyExpanded = expanded) }
    }

    fun onNearbyLotSelect(nearby: NearbyParkingLot) {
        val lot = nearby.lot
        _uiState.update {
            it.copy(
                sheetOpen = false,
                selectedLot = lot,
                panTo = LatLngPoint(lot.latDouble, lot.lngDouble),
            )
        }
    }

    // --- Saved ---

    fun closeSavedSheet() {
        _uiState.update { it.copy(savedOpen = false, savedExpanded = false) }
    }

    fun setSavedExpanded(expanded: Boolean) {
        _uiState.update { it.copy(savedExpanded = expanded) }
    }

    // --- Back Press ---

    fun handleBackPress(): Boolean {
        val state = _uiState.value
        return when {
            state.activeTab == TabId.MY && state.myPageRoute != MyPageRoute.ROOT -> {
                _uiState.update { it.copy(myPageRoute = MyPageRoute.ROOT) }; true
            }
            state.activeTab == TabId.MY -> {
                _uiState.update { it.copy(activeTab = TabId.HOME, myPageRoute = MyPageRoute.ROOT) }; true
            }
            state.searchResultsOpen -> {
                _uiState.update { it.copy(searchResultsOpen = false) }; true
            }
            state.detailLot != null -> {
                closeDetail(); true
            }
            state.nearbyExpanded -> {
                _uiState.update { it.copy(nearbyExpanded = false) }; true
            }
            state.savedExpanded -> {
                _uiState.update { it.copy(savedExpanded = false) }; true
            }
            state.savedOpen -> {
                _uiState.update { it.copy(savedOpen = false) }; true
            }
            state.sheetOpen -> {
                minimizeSheet(); true
            }
            state.selectedLot != null -> {
                selectLot(null); true
            }
            else -> false
        }
    }

    override fun onCleared() {
        super.onCleared()
        sensorManager.unregisterListener(compassListener)
    }

    fun consumePanTo() {
        _uiState.update { it.copy(panTo = null) }
    }

    fun fetchMyLocation() {
        viewModelScope.launch {
            _uiState.update { it.copy(gpsLoading = true) }
            try {
                val location = LocationHelper.getCurrentLocation(getApplication())
                if (location != null) {
                    val point = LatLngPoint(location.latitude, location.longitude)
                    _uiState.update {
                        it.copy(
                            gpsLoading = false,
                            userLocation = point,
                            panTo = point,
                        )
                    }
                } else {
                    _uiState.update { it.copy(gpsLoading = false) }
                }
            } catch (e: Exception) {
                Log.e("ParkingViewModel", "fetchMyLocation failed", e)
                _uiState.update { it.copy(gpsLoading = false) }
            }
        }
    }
}
