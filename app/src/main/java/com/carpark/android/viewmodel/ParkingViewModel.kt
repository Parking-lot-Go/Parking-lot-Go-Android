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
import com.carpark.android.data.local.SearchHistoryPreferences
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
    val selectedNearbyIndex: Int = -1,
    val detailLot: ParkingLot? = null,
    val activeTab: TabId = TabId.HOME,
    val myPageRoute: MyPageRoute = MyPageRoute.ROOT,
    val searchResultsOpen: Boolean = false,
    val searchPageOpen: Boolean = false,
    val searchQuery: String = "",
    val recentSearches: List<String> = emptyList(),
    val centerRegion: String = "",
    val gpsLoading: Boolean = false,
    val sheetOpen: Boolean = false,
    val savedOpen: Boolean = false,
    val nearbyExpanded: Boolean = false,
    val savedExpanded: Boolean = false,
    val panTo: LatLngPoint? = null,
    val userLocation: LatLngPoint? = null,
    val userBearing: Float? = null,
    val isMapTooZoomedOut: Boolean = false,
)

data class LatLngPoint(val lat: Double, val lng: Double)

class ParkingViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ParkingRepository()
    private val savedPrefs = SavedParkingPreferences(application)
    private val searchHistoryPrefs = SearchHistoryPreferences(application)

    private val _uiState = MutableStateFlow(ParkingUiState())
    val uiState: StateFlow<ParkingUiState> = _uiState.asStateFlow()

    private var refreshJob: Job? = null
    private var boundsUpdateJob: Job? = null
    private var currentBounds: MapBounds? = null
    private var currentZoomLevel: Int = 13
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
        private const val MIN_QUERY_ZOOM_LEVEL = 12
        private const val BOUNDS_UPDATE_DEBOUNCE_MS = 400L
        /** 이전 bounds 대비 이 비율 이상 변해야 재로딩 (0.3 = 30%) */
        private const val BOUNDS_CHANGE_THRESHOLD = 0.3
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
            loadRecentSearches()
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
                } else if (mode == DataMode.REALTIME) {
                    val lots = repository.fetchParkingLots(mode)
                    _uiState.update { it.copy(parkingLots = lots) }
                } else {
                    val lots = repository.fetchParkingLots(mode, bounds, district)
                    _uiState.update { it.copy(parkingLots = lots) }
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
                    val lots = repository.fetchParkingLots(_uiState.value.mode)
                    _uiState.update { it.copy(parkingLots = lots) }
                } catch (_: Exception) { }
            }
        }
    }

    // --- Public Actions ---

    fun updateBounds(bounds: MapBounds, region: String? = null, zoomLevel: Int) {
        val prevBounds = currentBounds
        val prevZoom = currentZoomLevel
        currentBounds = bounds
        currentZoomLevel = zoomLevel
        val isTooFar = zoomLevel <= MIN_QUERY_ZOOM_LEVEL
        if (!region.isNullOrBlank()) {
            val parts = region.split(" ")
            currentDistrict = if (parts.size > 1) parts[1].trim() else null
            _uiState.update { it.copy(centerRegion = region) }
        }
        if (isTooFar) {
            boundsUpdateJob?.cancel()
            refreshJob?.cancel()
            _uiState.update {
                it.copy(
                    isMapTooZoomedOut = true,
                    parkingLots = emptyList(),
                    selectedLot = null,
                    selectedNearbyIndex = -1,
                )
            }
            return
        }
        _uiState.update { it.copy(isMapTooZoomedOut = false) }
        if (_uiState.value.isNearbyMode) return

        // 줌 레벨이 같고 살짝 이동한 경우 재로딩 스킵
        if (prevZoom == zoomLevel && prevBounds != null && !hasBoundsChangedEnough(prevBounds, bounds)) {
            return
        }

        boundsUpdateJob?.cancel()
        boundsUpdateJob = viewModelScope.launch {
            delay(BOUNDS_UPDATE_DEBOUNCE_MS)
            loadParkingLots(_uiState.value.mode, bounds, currentDistrict)
            startAutoRefresh()
        }
    }

    private fun hasBoundsChangedEnough(old: MapBounds, new: MapBounds): Boolean {
        val oldLatSpan = old.neLat - old.swLat
        val oldLngSpan = old.neLng - old.swLng
        if (oldLatSpan == 0.0 || oldLngSpan == 0.0) return true
        val latShift = Math.abs((new.swLat + new.neLat) / 2 - (old.swLat + old.neLat) / 2)
        val lngShift = Math.abs((new.swLng + new.neLng) / 2 - (old.swLng + old.neLng) / 2)
        return latShift / oldLatSpan > BOUNDS_CHANGE_THRESHOLD ||
            lngShift / oldLngSpan > BOUNDS_CHANGE_THRESHOLD
    }

    fun changeMode(newMode: DataMode) {
        _uiState.update { it.copy(mode = newMode, parkingLots = emptyList()) }
        boundsUpdateJob?.cancel()
        refreshJob?.cancel()
        if (!_uiState.value.isNearbyMode && currentZoomLevel <= MIN_QUERY_ZOOM_LEVEL) {
            _uiState.update {
                it.copy(
                    isMapTooZoomedOut = true,
                    parkingLots = emptyList(),
                    selectedLot = null,
                    selectedNearbyIndex = -1,
                )
            }
            return
        }
        loadParkingLots(newMode, currentBounds, currentDistrict)
        if (newMode == DataMode.REALTIME) startAutoRefresh()
    }

    fun selectLot(lot: ParkingLot?) {
        if (lot != null && _uiState.value.selectedLot?.id == lot.id) {
            _uiState.update { it.copy(selectedLot = null, selectedNearbyIndex = -1) }
            return
        }
        if (lot == null) {
            _uiState.update { it.copy(selectedLot = null, selectedNearbyIndex = -1) }
            return
        }
        val nearbyIndex = _uiState.value.nearbyLots.indexOfFirst { it.lot.id == lot.id }
        _uiState.update {
            it.copy(
                selectedLot = if (nearbyIndex >= 0) it.nearbyLots[nearbyIndex].lot else lot,
                selectedNearbyIndex = nearbyIndex,
            )
        }
        // NOT_LINKED 모드에서는 정적 데이터만 있으므로 API로 상세 조회
        if (_uiState.value.mode == DataMode.NOT_LINKED) {
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

    fun openSearchPage() {
        _uiState.update {
            it.copy(
                searchPageOpen = true,
                searchResultsOpen = false,
                recentSearches = searchHistoryPrefs.getRecentSearches(),
            )
        }
    }

    fun closeSearchPage() {
        _uiState.update { it.copy(searchPageOpen = false) }
    }

    fun clearAllRecentSearches() {
        searchHistoryPrefs.clearRecentSearches()
        _uiState.update { it.copy(recentSearches = emptyList()) }
    }

    fun deleteRecentSearch(query: String) {
        searchHistoryPrefs.removeSearches(setOf(query))
        _uiState.update {
            it.copy(recentSearches = searchHistoryPrefs.getRecentSearches())
        }
    }

    fun searchFromHistory(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchPlaces(query)
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
                repository.toggleFavorite(lot.id)
            } catch (e: Exception) {
                // 서버 실패 시 원복
                loadFavorites(refresh = true)
            }
        }
    }

    fun removeSavedLot(lot: ParkingLot) {
        // 낙관적 UI 업데이트 — 즉시 목록에서 제거
        _uiState.update { it.copy(savedLots = it.savedLots.filter { l -> l.id != lot.id }) }
        viewModelScope.launch {
            try {
                repository.toggleFavorite(lot.id)
            } catch (e: Exception) {
                // 서버 실패 시 전체 목록 다시 로드
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
                        searchPageOpen = false,
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
                        searchPageOpen = false,
                        activeTab = tab,
                    )
                }
            }
            TabId.SAVED -> {
                _uiState.update {
                    it.copy(
                        sheetOpen = false,
                        searchPageOpen = false,
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
                        searchPageOpen = false,
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
            _uiState.update {
                it.copy(
                    searchPlaces = emptyList(),
                    searchResultsOpen = false,
                    searchPageOpen = false,
                )
            }
            return
        }

        searchHistoryPrefs.saveSearch(trimmed)
        _uiState.update {
            it.copy(
                searchQuery = trimmed,
                recentSearches = searchHistoryPrefs.getRecentSearches(),
                searchPageOpen = false,
                loading = true,
                error = null,
            )
        }

        viewModelScope.launch {
            try {
                if (com.carpark.android.BuildConfig.KAKAO_REST_API_KEY.isBlank()) {
                    throw IllegalStateException("KAKAO_REST_API_KEY is missing in local.properties")
                }

                val results = repository.searchPlaces(trimmed, currentBounds)
                val first = results.firstOrNull()

                if (first == null) {
                    _uiState.update {
                        it.copy(
                            searchPlaces = emptyList(),
                            searchResultsOpen = false,
                            loading = false,
                            error = "No Kakao map results found",
                        )
                    }
                    return@launch
                }

                _uiState.update {
                    it.copy(
                        searchPlaces = listOf(first),
                        searchResultsOpen = false,
                        loading = false,
                        error = null,
                        activeTab = TabId.HOME,
                        isNearbyMode = false,
                        sheetOpen = false,
                        nearbyExpanded = false,
                        selectedLot = null,
                        selectedNearbyIndex = -1,
                        panTo = LatLngPoint(first.latitude, first.longitude),
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        searchPlaces = emptyList(),
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
                searchPlaces = listOf(place),
                searchResultsOpen = false,
                searchPageOpen = false,
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
            executeNearbySearch(
                lat = lat,
                lng = lng,
                updateUserLocation = true,
                moveCamera = true,
            )
        }
    }

    fun doNearbySearch(lat: Double, lng: Double) {
        executeNearbySearch(
            lat = lat,
            lng = lng,
            updateUserLocation = true,
            moveCamera = true,
        )
    }

    private fun executeNearbySearch(
        lat: Double,
        lng: Double,
        updateUserLocation: Boolean,
        moveCamera: Boolean,
    ) {
        _uiState.update {
            it.copy(
                userLocation = if (updateUserLocation) LatLngPoint(lat, lng) else it.userLocation,
                panTo = if (moveCamera) LatLngPoint(lat, lng) else it.panTo,
                isMapTooZoomedOut = false,
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
                        selectedLot = null,
                        selectedNearbyIndex = -1,
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
        val bounds = currentBounds
        val center = if (bounds != null) {
            LatLngPoint(
                lat = (bounds.swLat + bounds.neLat) / 2,
                lng = (bounds.swLng + bounds.neLng) / 2,
            )
        } else {
            _uiState.value.userLocation
        } ?: return

        executeNearbySearch(
            lat = center.lat,
            lng = center.lng,
            updateUserLocation = false,
            moveCamera = false,
        )
    }

    fun closeNearbySheet() {
        boundsUpdateJob?.cancel()
        _uiState.update {
            it.copy(
                sheetOpen = false,
                isNearbyMode = false,
                nearbyExpanded = false,
                isMapTooZoomedOut = currentZoomLevel <= MIN_QUERY_ZOOM_LEVEL,
                selectedLot = null,
                selectedNearbyIndex = -1,
            )
        }
        if (currentZoomLevel <= MIN_QUERY_ZOOM_LEVEL) {
            _uiState.update { it.copy(parkingLots = emptyList()) }
            return
        }
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
        val index = _uiState.value.nearbyLots.indexOfFirst { it.lot.id == lot.id }
        _uiState.update {
            it.copy(
                sheetOpen = false,
                selectedLot = lot,
                selectedNearbyIndex = index,
                panTo = LatLngPoint(lot.latDouble, lot.lngDouble),
            )
        }
    }

    fun selectNearbyLotByIndex(index: Int) {
        val lot = _uiState.value.nearbyLots.getOrNull(index)?.lot ?: return
        _uiState.update {
            it.copy(
                selectedLot = lot,
                selectedNearbyIndex = index,
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
            state.searchPageOpen -> {
                closeSearchPage(); true
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

    private fun loadRecentSearches() {
        _uiState.update {
            it.copy(recentSearches = searchHistoryPrefs.getRecentSearches())
        }
    }
}
