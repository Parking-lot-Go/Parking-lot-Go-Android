package com.carpark.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.carpark.android.data.model.KakaoPlace
import com.carpark.android.ui.theme.Gray100
import com.carpark.android.ui.theme.Gray400
import com.carpark.android.ui.theme.Gray50
import com.carpark.android.ui.theme.Gray700
import com.carpark.android.ui.theme.Gray900
import com.carpark.android.ui.theme.Primary

@Composable
fun SearchResultsSheet(
    open: Boolean,
    results: List<KakaoPlace>,
    onSelect: (KakaoPlace) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!open || results.isEmpty()) return

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Kakao map search",
                    modifier = Modifier.weight(1f),
                    color = Gray900,
                    fontWeight = FontWeight.Bold,
                )
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "close-search-results",
                    modifier = Modifier.clickable(onClick = onClose),
                    tint = Gray400,
                )
            }

            HorizontalDivider(color = Gray100)

            LazyColumn(
                modifier = Modifier.heightIn(max = 320.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                items(results, key = { it.id }) { place ->
                    SearchResultRow(
                        place = place,
                        onClick = { onSelect(place) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(
    place: KakaoPlace,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(Gray50, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = null,
                    tint = Primary,
                )
            }

            Spacer(Modifier.size(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = place.place_name,
                    color = Gray900,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.size(4.dp))
                Text(
                    text = place.category_name,
                    color = Gray400,
                )
            }

            if (!place.distance.isNullOrBlank()) {
                Text(
                    text = "${place.distance}m",
                    color = Gray700,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        Spacer(Modifier.size(10.dp))

        Text(
            text = place.displayAddress,
            color = Gray700,
        )
    }
}
