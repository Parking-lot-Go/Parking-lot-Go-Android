package com.carpark.android.ui.mypage

import android.content.Intent
import android.net.Uri

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carpark.android.R
import com.carpark.android.data.api.RetrofitClient
import com.carpark.android.data.local.AppSettingsPreferences
import com.carpark.android.data.local.AuthPreferences
import com.carpark.android.data.model.MyPageRoute
import com.carpark.android.data.model.NavigationProvider
import com.carpark.android.data.model.ThemeMode
import com.carpark.android.ui.theme.Gray100
import com.carpark.android.ui.theme.Gray400
import com.carpark.android.ui.theme.Gray50
import com.carpark.android.ui.theme.Gray700
import com.carpark.android.ui.theme.Gray900
import com.carpark.android.ui.theme.Primary
import kotlinx.coroutines.launch

@Composable
fun MyPageScreen(
    route: MyPageRoute,
    authPreferences: AuthPreferences,
    settingsPreferences: AppSettingsPreferences,
    onNavigate: (MyPageRoute) -> Unit,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (route) {
        MyPageRoute.ROOT -> MyPageHome(
            authPreferences = authPreferences,
            onNavigate = onNavigate,
            onLogout = onLogout,
            modifier = modifier,
        )
        MyPageRoute.NOTICE -> NoticePage(
            onBack = onBack,
            modifier = modifier,
        )
        MyPageRoute.CONTACT -> ContactPage(
            onBack = onBack,
            modifier = modifier,
        )
        MyPageRoute.REQUEST -> RequestPage(
            onBack = onBack,
            modifier = modifier,
        )
        MyPageRoute.SETTINGS -> SettingsPage(
            settingsPreferences = settingsPreferences,
            onBack = onBack,
            modifier = modifier,
        )
        MyPageRoute.TERMS -> TermsPage(
            onBack = onBack,
            modifier = modifier,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MyPageHome(
    authPreferences: AuthPreferences,
    onNavigate: (MyPageRoute) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val nickname = authPreferences.userNickname ?: "사용자"
    val loginType = authPreferences.loginType ?: "UNKNOWN"
    val backgroundColor = MaterialTheme.colorScheme.background

    Scaffold(
        modifier = modifier,
        containerColor = backgroundColor,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "마이페이지",
                        fontWeight = FontWeight.Bold,
                        color = Gray900,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .verticalScroll(rememberScrollState())
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 20.dp),
        ) {
            ProfileCard(
                nickname = nickname,
                loginType = loginType,
            )

            Spacer(Modifier.height(20.dp))

            MenuSection(
                title = "계정",
                items = listOf(
                    MenuItemData(
                        title = "환경설정",
                        subtitle = "기본 내비 설정을 변경할 수 있어요",
                        icon = Icons.Default.Settings,
                        onClick = { onNavigate(MyPageRoute.SETTINGS) },
                    ),
                    MenuItemData(
                        title = "이용 약관",
                        subtitle = "서비스 이용 조건을 확인해 주세요",
                        icon = Icons.Default.Description,
                        onClick = { onNavigate(MyPageRoute.TERMS) },
                    ),
                    MenuItemData(
                        title = "로그아웃",
                        subtitle = "현재 기기에서 안전하게 로그아웃해요",
                        icon = Icons.AutoMirrored.Filled.Logout,
                        onClick = onLogout,
                    ),
                    MenuItemData(
                        title = "정기권 구매하러 가기",
                        subtitle = "성남도시개발공사 정기권 구매 페이지로 이동해요",
                        icon = Icons.Default.DirectionsCar,
                        onClick = {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://www.knpark.com/season.html"),
                                ),
                            )
                        },
                    ),
                ),
            )

            Spacer(Modifier.height(20.dp))

            MenuSection(
                title = "고객 지원",
                items = listOf(
                    MenuItemData(
                        title = "공지사항",
                        subtitle = "최근 업데이트와 서비스 공지를 확인해요",
                        icon = Icons.Default.Info,
                        onClick = { onNavigate(MyPageRoute.NOTICE) },
                    ),
                    MenuItemData(
                        title = "문의하기",
                        subtitle = "버그나 이용 중 불편한 점을 전달해 주세요",
                        icon = Icons.Default.Help,
                        onClick = { onNavigate(MyPageRoute.CONTACT) },
                    ),
                    MenuItemData(
                        title = "개발자에게 요구사항",
                        subtitle = "원하는 기능이나 개선 아이디어를 남겨 주세요",
                        icon = Icons.Default.Edit,
                        onClick = { onNavigate(MyPageRoute.REQUEST) },
                    ),
                ),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoticePage(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = MaterialTheme.colorScheme.background
    Scaffold(
        modifier = modifier,
        containerColor = backgroundColor,
        topBar = {
            PageTopBar(title = "공지사항", onBack = onBack)
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .verticalScroll(rememberScrollState())
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NoticeCard(
                title = "마이페이지 기능이 업데이트되었어요",
                date = "2026.03.31",
                body = "로그인 정보, 환경설정, 이용 약관, 공지사항, 문의하기, 개발자 요구사항 메뉴를 한 곳에서 볼 수 있게 정리했어요.",
            )
            NoticeCard(
                title = "카카오 로그인과 장소 검색을 지원해요",
                date = "2026.03.30",
                body = "카카오 로그인 후 서버 토큰을 발급받고, 카카오맵 기반 장소 검색 결과를 지도와 함께 확인할 수 있어요.",
            )
            NoticeCard(
                title = "로그아웃 보안 흐름이 강화되었어요",
                date = "2026.03.30",
                body = "로그아웃 시 로컬 세션을 즉시 정리하고, 인증이 만료된 경우 다시 로그인 화면으로 이동하도록 개선했어요.",
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContactPage(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = MaterialTheme.colorScheme.background
    Scaffold(
        modifier = modifier,
        containerColor = backgroundColor,
        topBar = {
            PageTopBar(title = "문의하기", onBack = onBack)
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            InfoCard(
                title = "문의 전에 확인해 주세요",
                body = "문제가 발생한 화면, 재현 방법, 로그인 여부, 사용 중인 기기 정보를 함께 정리해 주시면 더 빠르게 확인할 수 있어요.",
            )
            InfoCard(
                title = "이런 내용을 보내 주세요",
                body = "1. 어떤 기능에서 문제가 있었는지\n2. 언제부터 발생했는지\n3. 기대한 동작과 실제 동작이 어떻게 다른지\n4. 가능하다면 스크린샷이나 오류 문구",
            )
            InfoCard(
                title = "접수 채널 안내",
                body = "현재는 앱 내 정식 문의 접수 기능을 준비 중이에요. 우선 이 페이지를 기준으로 문의 내용을 정리해 개발팀 전달 형식으로 사용할 수 있게 구성했어요.",
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RequestPage(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = MaterialTheme.colorScheme.background
    Scaffold(
        modifier = modifier,
        containerColor = backgroundColor,
        topBar = {
            PageTopBar(title = "개발자에게 요구사항", onBack = onBack)
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .verticalScroll(rememberScrollState())
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            InfoCard(
                title = "요구사항은 이렇게 남겨 주세요",
                body = "원하는 기능, 필요한 이유, 자주 사용하는 상황을 함께 적어 주시면 우선순위를 판단하는 데 도움이 돼요.",
            )
            InfoCard(
                title = "예시",
                body = "예: 즐겨찾기한 주차장만 빠르게 필터링하고 싶어요.\n예: 검색 결과를 거리순으로 정렬할 수 있으면 좋아요.\n예: 빈자리 많은 주차장만 보이도록 옵션이 필요해요.",
            )
            InfoCard(
                title = "반영 기준",
                body = "많이 요청되는 기능, 사용 빈도가 높은 시나리오, 안정성과 성능에 영향이 적은 개선부터 우선 검토할 수 있어요.",
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsPage(
    settingsPreferences: AppSettingsPreferences,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedProvider by remember {
        mutableStateOf(settingsPreferences.preferredNavigation)
    }
    var selectedThemeMode by remember {
        mutableStateOf(settingsPreferences.themeMode)
    }
    val scope = rememberCoroutineScope()
    val backgroundColor = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surface
    val titleColor = MaterialTheme.colorScheme.onSurface
    val bodyColor = MaterialTheme.colorScheme.onSurfaceVariant

    Scaffold(
        modifier = modifier,
        containerColor = backgroundColor,
        topBar = {
            PageTopBar(title = "환경설정", onBack = onBack)
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(innerPadding)
                .padding(20.dp),
        ) {
            Text(
                text = "테마 설정",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = titleColor,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "휴대폰 설정을 따르거나, 앱에서 라이트/다크 모드를 직접 고를 수 있어요.",
                style = MaterialTheme.typography.bodyMedium,
                color = bodyColor,
            )

            Spacer(Modifier.height(16.dp))

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = surfaceColor),
            ) {
                ThemeMode.entries.forEachIndexed { index, themeMode ->
                    ThemeModeOptionRow(
                        mode = themeMode,
                        selected = selectedThemeMode == themeMode,
                        onSelect = {
                            selectedThemeMode = themeMode
                            settingsPreferences.themeMode = themeMode
                        },
                    )

                    if (index != ThemeMode.entries.lastIndex) {
                        HorizontalDivider(color = Gray100)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "기본 내비 설정",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = titleColor,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "길안내 버튼을 눌렀을 때 먼저 실행할 내비 앱을 선택해 주세요.",
                style = MaterialTheme.typography.bodyMedium,
                color = bodyColor,
            )

            Spacer(Modifier.height(16.dp))

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = surfaceColor),
            ) {
                NavigationProvider.entries.forEachIndexed { index, provider ->
                    NavigationOptionRow(
                        provider = provider,
                        selected = selectedProvider == provider,
                        onSelect = {
                            selectedProvider = provider
                            settingsPreferences.preferredNavigation = provider
                            scope.launch {
                                runCatching {
                                    RetrofitClient.api.updateNaviType(provider.name)
                                }
                            }
                        },
                    )

                    if (index != NavigationProvider.entries.lastIndex) {
                        HorizontalDivider(color = Gray100)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TermsPage(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = Gray50,
        topBar = {
            PageTopBar(title = "이용 약관", onBack = onBack)
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Gray50)
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(20.dp),
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "서비스 이용 약관",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Gray900,
                    )
                    Spacer(Modifier.height(12.dp))
                    TermsText("1. 본 서비스는 주차장 정보와 길안내 기능을 제공합니다.")
                    TermsText("2. 제공되는 주차 정보는 실제 운영 상황과 다를 수 있습니다.")
                    TermsText("3. 사용자는 관련 법령과 서비스 정책을 준수하며 앱을 이용해야 합니다.")
                    TermsText("4. 로그인 정보는 인증과 서비스 제공 목적 범위 안에서만 처리됩니다.")
                    TermsText("5. 추후 정식 약관이 준비되면 해당 내용을 기준으로 대체될 수 있습니다.")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PageTopBar(
    title: String,
    onBack: () -> Unit,
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    TopAppBar(
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                color = onSurfaceColor,
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "back",
                    tint = onSurfaceColor,
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = surfaceColor,
        ),
    )
}

@Composable
private fun NoticeCard(
    title: String,
    date: String,
    body: String,
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Gray900,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = date,
                style = MaterialTheme.typography.bodySmall,
                color = Gray400,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = Gray700,
                lineHeight = 22.sp,
            )
        }
    }
}

@Composable
private fun InfoCard(
    title: String,
    body: String,
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Gray900,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = Gray700,
                lineHeight = 22.sp,
            )
        }
    }
}

@Composable
private fun TermsText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = Gray700,
        lineHeight = 22.sp,
    )
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun ProfileCard(
    nickname: String,
    loginType: String,
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Gray100),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = Gray700,
                    modifier = Modifier.size(28.dp),
                )
            }

            Spacer(Modifier.size(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = nickname,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Gray900,
                )
                Spacer(Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Gray50,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(loginIconRes(loginType)),
                            contentDescription = loginType,
                            tint = if (loginType.uppercase() == "KAKAO") Color(0xD9000000) else Color.Unspecified,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.size(6.dp))
                        Text(
                            text = loginLabel(loginType),
                            style = MaterialTheme.typography.labelMedium,
                            color = Gray700,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuSection(
    title: String,
    items: List<MenuItemData>,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = Gray900,
    )
    Spacer(Modifier.height(10.dp))

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        items.forEachIndexed { index, item ->
            MenuRow(item = item)
            if (index != items.lastIndex) {
                HorizontalDivider(color = Gray100)
            }
        }
    }
}

@Composable
private fun MenuRow(item: MenuItemData) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = item.onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Gray50),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = Primary,
            )
        }

        Spacer(Modifier.size(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = Gray900,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Gray400,
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Gray400,
        )
    }
}

@Composable
private fun NavigationOptionRow(
    provider: NavigationProvider,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(
                    if (provider == NavigationProvider.NAVER) R.drawable.ic_naver_map
                    else R.drawable.ic_kakao_navi
                ),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.size(12.dp))
            Column {
                Text(
                    text = if (provider == NavigationProvider.NAVER) "네이버 지도" else "카카오내비",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Gray900,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (provider == NavigationProvider.NAVER) {
                        "길안내를 누르면 네이버 지도를 먼저 실행해요"
                    } else {
                        "길안내를 누르면 카카오내비를 먼저 실행해요"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Gray400,
                )
            }
        }

        RadioButton(
            selected = selected,
            onClick = onSelect,
        )
    }
}

@Composable
private fun ThemeModeOptionRow(
    mode: ThemeMode,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val title = when (mode) {
        ThemeMode.SYSTEM -> "시스템 설정 따르기"
        ThemeMode.LIGHT -> "라이트 모드"
        ThemeMode.DARK -> "다크 모드"
    }
    val description = when (mode) {
        ThemeMode.SYSTEM -> "휴대폰의 다크모드 설정에 맞춰 자동으로 적용돼요."
        ThemeMode.LIGHT -> "항상 밝은 화면으로 표시해요."
        ThemeMode.DARK -> "항상 어두운 화면으로 표시해요."
    }
    val titleColor = MaterialTheme.colorScheme.onSurface
    val bodyColor = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = titleColor,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = bodyColor,
            )
        }

        RadioButton(
            selected = selected,
            onClick = onSelect,
        )
    }
}

private data class MenuItemData(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

private fun loginIconRes(loginType: String): Int {
    return when (loginType.uppercase()) {
        "KAKAO" -> R.drawable.ic_kakao
        "GOOGLE" -> R.drawable.ic_google
        else -> R.drawable.ic_my
    }
}

private fun loginLabel(loginType: String): String {
    return when (loginType.uppercase()) {
        "KAKAO" -> "카카오 로그인"
        "GOOGLE" -> "구글 로그인"
        else -> "기타 로그인"
    }
}
