package com.carpark.android.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.carpark.android.R
import com.carpark.android.data.local.AppSettingsPreferences
import com.carpark.android.data.local.AuthPreferences
import com.carpark.android.data.model.MyPageRoute
import com.carpark.android.data.model.TabId
import com.carpark.android.ui.components.BottomNavBar
import com.carpark.android.ui.components.HeaderBar
import com.carpark.android.ui.components.NearbyBottomSheet
import com.carpark.android.ui.components.ParkingInfoCard
import com.carpark.android.ui.components.ParkingMapView
import com.carpark.android.ui.components.SearchResultsSheet
import com.carpark.android.ui.components.SavedBottomSheet
import com.carpark.android.ui.detail.DetailScreen
import com.carpark.android.ui.mypage.MyPageScreen
import com.carpark.android.viewmodel.ParkingViewModel

@Composable
fun MainScreen(
    viewModel: ParkingViewModel,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val authPreferences = remember(context) { AuthPreferences(context) }
    val settingsPreferences = remember(context) { AppSettingsPreferences(context) }

    val isDarkMode = isSystemInDarkTheme()
    val hideHeader = state.detailLot != null || state.savedExpanded || state.nearbyExpanded

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (state.activeTab == TabId.MY) {
                MyPageScreen(
                    route = state.myPageRoute,
                    authPreferences = authPreferences,
                    settingsPreferences = settingsPreferences,
                    onNavigate = viewModel::navigateMyPage,
                    onBack = {
                        if (state.myPageRoute == MyPageRoute.ROOT) {
                            viewModel.onTabChange(TabId.HOME)
                        } else {
                            viewModel.navigateMyPage(MyPageRoute.ROOT)
                        }
                    },
                    onLogout = onLogout,
                    modifier = Modifier.weight(1f),
                )
            } else {
                BoxWithConstraints(modifier = Modifier.weight(1f)) {
                    val fabBottom by animateDpAsState(
                        targetValue = when {
                            state.nearbyExpanded && state.sheetOpen -> maxHeight * 0.92f + 16.dp
                            state.isNearbyMode && state.sheetOpen -> maxHeight * 0.55f + 16.dp
                            state.savedExpanded && state.savedOpen -> maxHeight * 0.92f + 16.dp
                            state.savedOpen -> maxHeight * 0.50f + 16.dp
                            state.selectedLot != null -> 240.dp
                            else -> 16.dp
                        },
                        label = "fab_bottom",
                    )

                    ParkingMapView(
                        parkingLots = state.parkingLots,
                        selectedLot = state.selectedLot,
                        dataMode = state.mode,
                        panTo = state.panTo,
                        userLocation = state.userLocation,
                        userBearing = state.userBearing,
                        searchPlaces = state.searchPlaces,
                        onPanToConsumed = viewModel::consumePanTo,
                        onBoundsChange = viewModel::updateBounds,
                        onSelectLot = viewModel::selectLot,
                        onMapClick = {
                            if (state.isNearbyMode) viewModel.minimizeSheet()
                            viewModel.closeSearchResults()
                            viewModel.selectLot(null)
                        },
                        modifier = Modifier.fillMaxSize(),
                    )

                    if (!hideHeader) {
                        Box(modifier = Modifier.align(Alignment.TopCenter)) {
                            HeaderBar(
                                searchQuery = state.searchQuery,
                                onSearchChange = viewModel::updateSearchQuery,
                                onSearch = viewModel::searchPlaces,
                                centerRegion = state.centerRegion,
                                dataMode = state.mode,
                                onModeChange = viewModel::changeMode,
                            )
                        }
                    }

                    FloatingActionButton(
                        onClick = viewModel::fetchMyLocation,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = fabBottom)
                            .size(40.dp),
                        containerColor = if (isDarkMode) Color(0xFF1F2937).copy(alpha = 0.85f)
                        else Color.White.copy(alpha = 0.75f),
                        contentColor = if (isDarkMode) Color.White else Color(0xFF4285F4),
                        elevation = FloatingActionButtonDefaults.elevation(2.dp),
                    ) {
                        if (state.gpsLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = if (isDarkMode) Color.White else Color(0xFF4285F4),
                            )
                        } else {
                            Icon(
                                painter = painterResource(
                                    if (isDarkMode) R.drawable.ic_gps_outlined
                                    else R.drawable.ic_gps_filled
                                ),
                                contentDescription = "my-location",
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }

                    if (state.selectedLot != null) {
                        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                            ParkingInfoCard(
                                lot = state.selectedLot!!,
                                dataMode = state.mode,
                                isSaved = viewModel.isSavedLot(state.selectedLot!!.id),
                                onToggleSave = { viewModel.toggleSavedLot(state.selectedLot!!) },
                                onClose = { viewModel.selectLot(null) },
                                onShowDetail = {
                                    state.selectedLot?.let { viewModel.showDetail(it) }
                                },
                            )
                        }
                    }

                    if (state.isNearbyMode) {
                        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                            NearbyBottomSheet(
                                open = state.sheetOpen,
                                expanded = state.nearbyExpanded,
                                lots = state.nearbyLots,
                                loading = state.loading,
                                onClose = viewModel::closeNearbySheet,
                                onReSearch = viewModel::reSearchNearby,
                                onSelectLot = viewModel::onNearbyLotSelect,
                                onExpandChange = viewModel::setNearbyExpanded,
                            )
                        }
                    }

                    Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                        SavedBottomSheet(
                            open = state.savedOpen,
                            expanded = state.savedExpanded,
                            lots = state.savedLots,
                            hasMore = state.savedHasMore,
                            loading = state.savedLotsLoading,
                            onSelectLot = viewModel::selectSavedLot,
                            onRemoveLot = viewModel::removeSavedLot,
                            onClose = viewModel::closeSavedSheet,
                            onExpandChange = viewModel::setSavedExpanded,
                            onLoadMore = viewModel::loadMoreFavorites,
                        )
                    }

                    Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                        SearchResultsSheet(
                            open = state.searchResultsOpen,
                            results = state.searchPlaces,
                            onSelect = viewModel::selectSearchPlace,
                            onClose = viewModel::closeSearchResults,
                        )
                    }
                }
            }

            BottomNavBar(
                activeTab = state.activeTab,
                onTabChange = viewModel::onTabChange,
            )
        }

        if (state.activeTab != TabId.MY) {
            DetailScreen(
                lot = state.detailLot,
                open = state.detailLot != null,
                isSaved = state.detailLot?.let { viewModel.isSavedLot(it.id) } == true,
                onToggleSave = {
                    state.detailLot?.let(viewModel::toggleSavedLot)
                },
                onClose = viewModel::closeDetail,
            )
        }
    }
}
