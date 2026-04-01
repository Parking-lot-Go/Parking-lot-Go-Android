package com.carpark.android.ui.mypage

import android.content.Intent
import android.net.Uri
import android.util.Patterns
import android.widget.Toast

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.onGloballyPositioned
import com.carpark.android.R
import com.carpark.android.data.api.RetrofitClient
import com.carpark.android.data.local.AppSettingsPreferences
import com.carpark.android.data.local.AuthPreferences
import com.carpark.android.data.model.FeatureRequestCategory
import com.carpark.android.data.model.InquiryCategory
import com.carpark.android.data.model.MyPageRoute
import com.carpark.android.data.model.NavigationProvider
import com.carpark.android.data.model.SupportTicket
import com.carpark.android.data.model.SupportTicketStatus
import com.carpark.android.data.model.SupportTicketType
import com.carpark.android.data.model.ThemeMode
import com.carpark.android.ui.theme.Amber
import com.carpark.android.ui.theme.Green
import com.carpark.android.ui.theme.Gray100
import com.carpark.android.ui.theme.Gray400
import com.carpark.android.ui.theme.Gray500
import com.carpark.android.ui.theme.Gray900
import com.carpark.android.ui.theme.Primary
import com.carpark.android.ui.theme.Red
import com.carpark.android.ui.theme.isAppInDarkTheme
import kotlinx.coroutines.launch

@Composable
fun MyPageScreen(
    route: MyPageRoute,
    authPreferences: AuthPreferences,
    settingsPreferences: AppSettingsPreferences,
    onLoadSupportTickets: suspend () -> Result<List<SupportTicket>>,
    onSubmitInquiry: suspend (
        category: InquiryCategory,
        title: String,
        content: String,
        contextNote: String,
        replyEmail: String,
    ) -> Result<Unit>,
    onSubmitFeatureRequest: suspend (
        category: FeatureRequestCategory,
        title: String,
        problem: String,
        expectedImprovement: String,
    ) -> Result<Unit>,
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
        MyPageRoute.TICKETS -> TicketsPage(
            onLoadSupportTickets = onLoadSupportTickets,
            onNavigate = onNavigate,
            onBack = onBack,
            modifier = modifier,
        )
        MyPageRoute.CONTACT -> ContactFormPage(
            onSubmitInquiry = onSubmitInquiry,
            onBack = onBack,
            modifier = modifier,
        )
        MyPageRoute.REQUEST -> RequestFormPage(
            onSubmitFeatureRequest = onSubmitFeatureRequest,
            onBack = onBack,
            modifier = modifier,
        )
        MyPageRoute.SETTINGS -> SettingsPage(
            settingsPreferences = settingsPreferences,
            onBack = onBack,
            modifier = modifier,
        )
        MyPageRoute.NOTIFICATIONS -> NotificationSettingsPage(
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
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
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
                title = "시스템 설정",
                items = listOf(
                    MenuItemData(
                        title = "환경설정",
                        subtitle = "테마와 기본 길안내 앱을 설정할 수 있어요",
                        icon = Icons.Default.Settings,
                        onClick = { onNavigate(MyPageRoute.SETTINGS) },
                    ),
                    MenuItemData(
                        title = "알림 설정",
                        subtitle = "공지와 주차 관련 알림 수신 여부를 관리해요",
                        icon = Icons.Default.Notifications,
                        onClick = { onNavigate(MyPageRoute.NOTIFICATIONS) },
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
                        icon = Icons.AutoMirrored.Filled.Help,
                        onClick = { onNavigate(MyPageRoute.CONTACT) },
                    ),
                    MenuItemData(
                        title = "내 문의내역",
                        subtitle = "접수한 문의와 요구사항 진행 상태를 확인해요",
                        icon = Icons.Default.Description,
                        onClick = { onNavigate(MyPageRoute.TICKETS) },
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
private fun ContactFormPage(
    onSubmitInquiry: suspend (
        category: InquiryCategory,
        title: String,
        content: String,
        contextNote: String,
        replyEmail: String,
    ) -> Result<Unit>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var inquiryType by rememberSaveable { mutableStateOf("") }
    var title by rememberSaveable { mutableStateOf("") }
    var details by rememberSaveable { mutableStateOf("") }
    var contextNote by rememberSaveable { mutableStateOf("") }
    var replyEmail by rememberSaveable { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    val isReplyEmailValid = replyEmail.isBlank() || Patterns.EMAIL_ADDRESS.matcher(replyEmail).matches()
    val canSubmit = title.isNotBlank() && details.isNotBlank() && isReplyEmailValid && !isSubmitting
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
                body = "작성한 문의는 앱에서 바로 접수되고, 앱 버전과 OS 버전, 기기 모델 정보가 함께 전달돼요. 재현 방법이나 오류 문구를 적어주시면 더 빠르게 확인할 수 있어요.",
            )

            FormSectionCard(
                title = "문의 작성",
                description = "문제가 발생한 화면과 기대한 동작을 함께 적어주시면 확인이 훨씬 빨라져요.",
            ) {
                SelectFieldBlock(
                    label = "문의 유형",
                    options = listOf("버그/오류", "앱 사용", "계정/로그인", "기타"),
                    selected = inquiryType,
                    onSelect = { inquiryType = it },
                )
                FormTextFieldBlock(
                    label = "제목",
                    placeholder = "예: 저장 탭에서 말풍선 배경색이 어둡게 보여요",
                    value = title,
                    maxLength = 40,
                    onValueChange = { title = it.take(40) },
                )
                FormTextFieldBlock(
                    label = "문의 내용",
                    placeholder = "문제가 발생한 화면, 기대한 동작, 실제로 보인 동작을 순서대로 적어주세요.",
                    value = details,
                    maxLength = 500,
                    minLines = 6,
                    onValueChange = { details = it.take(500) },
                )
                FormTextFieldBlock(
                    label = "추가 상황",
                    placeholder = "재현 방법, 사용 기기, OS 버전 등 도움이 될 내용을 남겨주세요.",
                    value = contextNote,
                    maxLength = 200,
                    minLines = 4,
                    onValueChange = { contextNote = it.take(200) },
                )
                FormTextFieldBlock(
                    label = "회신 받을 이메일",
                    placeholder = "선택 입력",
                    value = replyEmail,
                    maxLength = 60,
                    onValueChange = { replyEmail = it.take(60) },
                )
                if (!isReplyEmailValid) {
                    Text(
                        text = "이메일 형식을 확인해 주세요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                FormSubmitButton(
                    text = if (isSubmitting) "문의 접수 중..." else "문의 접수하기",
                    enabled = canSubmit,
                    onClick = {
                        scope.launch {
                            isSubmitting = true
                            val result = onSubmitInquiry(
                                inquiryCategoryOf(inquiryType),
                                title,
                                details,
                                contextNote,
                                replyEmail,
                            )
                            isSubmitting = false

                            result.onSuccess {
                                inquiryType = "버그/오류"
                                title = ""
                                details = ""
                                contextNote = ""
                                replyEmail = ""
                                Toast.makeText(context, "문의가 접수되었어요.", Toast.LENGTH_SHORT).show()
                            }.onFailure { error ->
                                Toast.makeText(
                                    context,
                                    error.message ?: "문의 접수에 실패했어요.",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        }
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RequestFormPage(
    onSubmitFeatureRequest: suspend (
        category: FeatureRequestCategory,
        title: String,
        problem: String,
        expectedImprovement: String,
    ) -> Result<Unit>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var category by rememberSaveable { mutableStateOf("새 기능") }
    var featureTitle by rememberSaveable { mutableStateOf("") }
    var userProblem by rememberSaveable { mutableStateOf("") }
    var expectedEffect by rememberSaveable { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    val canSubmit = featureTitle.isNotBlank() && userProblem.isNotBlank() && !isSubmitting
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
                .padding(innerPadding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            InfoCard(
                title = "요구사항은 이렇게 남겨 주세요",
                body = "요구사항은 앱에서 바로 접수돼요. 어떤 점이 불편한지와 기대하는 개선 방향을 함께 적어주시면 검토 우선순위를 정하는 데 큰 도움이 돼요.",
            )

            FormSectionCard(
                title = "요구사항 작성",
                description = "사용자 문제와 기대 개선을 함께 남겨주시면 실제 기능 요청 티켓으로 접수돼요.",
            ) {
                SelectFieldBlock(
                    label = "카테고리",
                    options = listOf("새 기능", "UI/UX", "성능", "기타"),
                    selected = category,
                    onSelect = { category = it },
                )
                FormTextFieldBlock(
                    label = "한 줄 요약",
                    placeholder = "예: 저장 탭에서 정렬 옵션을 더 세분화하고 싶어요",
                    value = featureTitle,
                    maxLength = 40,
                    onValueChange = { featureTitle = it.take(40) },
                )
                FormTextFieldBlock(
                    label = "지금 불편한 점",
                    placeholder = "현재 어떤 흐름이 번거로운지, 언제 자주 아쉬운지 적어주세요.",
                    value = userProblem,
                    maxLength = 500,
                    minLines = 6,
                    onValueChange = { userProblem = it.take(500) },
                )
                FormTextFieldBlock(
                    label = "원하는 방향",
                    placeholder = "어떤 방식으로 개선되면 좋을지 자유롭게 적어주세요.",
                    value = expectedEffect,
                    maxLength = 240,
                    minLines = 4,
                    onValueChange = { expectedEffect = it.take(240) },
                )
                FormSubmitButton(
                    text = if (isSubmitting) "요구사항 접수 중..." else "요구사항 접수하기",
                    enabled = canSubmit,
                    onClick = {
                        scope.launch {
                            isSubmitting = true
                            val result = onSubmitFeatureRequest(
                                featureRequestCategoryOf(category),
                                featureTitle,
                                userProblem,
                                expectedEffect,
                            )
                            isSubmitting = false

                            result.onSuccess {
                                category = "새 기능"
                                featureTitle = ""
                                userProblem = ""
                                expectedEffect = ""
                                Toast.makeText(context, "요구사항이 접수되었어요.", Toast.LENGTH_SHORT).show()
                            }.onFailure { error ->
                                Toast.makeText(
                                    context,
                                    error.message ?: "요구사항 접수에 실패했어요.",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        }
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TicketsPage(
    onLoadSupportTickets: suspend () -> Result<List<SupportTicket>>,
    onNavigate: (MyPageRoute) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var tickets by remember { mutableStateOf<List<SupportTicket>>(emptyList()) }
    var selectedTicket by remember { mutableStateOf<SupportTicket?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableStateOf(0) }
    var selectedType by rememberSaveable { mutableStateOf("전체") }
    var selectedStatus by rememberSaveable { mutableStateOf("전체") }
    val backgroundColor = MaterialTheme.colorScheme.background

    BackHandler(enabled = selectedTicket != null) {
        selectedTicket = null
    }

    LaunchedEffect(reloadKey) {
        isLoading = true
        errorMessage = null
        onLoadSupportTickets()
            .onSuccess { tickets = it.sortedByDescending(SupportTicket::createdAt) }
            .onFailure { error ->
                errorMessage = error.message ?: "문의내역을 불러오지 못했어요."
            }
        isLoading = false
    }

    val filteredTickets = remember(tickets, selectedType, selectedStatus) {
        tickets.filter { ticket ->
            val matchesType = when (selectedType) {
                "문의" -> ticket.ticketType == SupportTicketType.INQUIRY
                "요구사항" -> ticket.ticketType == SupportTicketType.FEATURE_REQUEST
                else -> true
            }
            val matchesStatus = when (selectedStatus) {
                "대기" -> ticket.status == SupportTicketStatus.PENDING
                "처리중" -> ticket.status == SupportTicketStatus.IN_PROGRESS
                "완료" -> ticket.status == SupportTicketStatus.RESOLVED
                "반려" -> ticket.status == SupportTicketStatus.REJECTED
                else -> true
            }
            matchesType && matchesStatus
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = backgroundColor,
        topBar = {
            PageTopBar(
                title = if (selectedTicket == null) "내 문의내역" else "문의 상세",
                onBack = {
                    if (selectedTicket != null) {
                        selectedTicket = null
                    } else {
                        onBack()
                    }
                },
            )
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
            when {
                isLoading -> {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(26.dp),
                                strokeWidth = 2.5.dp,
                            )
                            Text(
                                text = "문의내역을 불러오는 중이에요.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                errorMessage != null -> {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                text = "문의내역을 불러오지 못했어요",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = errorMessage.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 22.sp,
                            )
                            FormSubmitButton(
                                text = "다시 불러오기",
                                enabled = true,
                                onClick = { reloadKey++ },
                            )
                        }
                    }
                }

                filteredTickets.isEmpty() -> {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                text = if (tickets.isEmpty()) "아직 접수한 내역이 없어요" else "조건에 맞는 내역이 없어요",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = if (tickets.isEmpty()) {
                                    "문의하기 또는 개발자에게 요구사항 화면에서 내용을 접수하면 여기에서 진행 상태를 확인할 수 있어요."
                                } else {
                                    "선택한 필터에 맞는 문의내역이 없어요. 유형이나 상태를 바꿔서 다시 확인해 보세요."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 22.sp,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.clickable { onNavigate(MyPageRoute.CONTACT) },
                                ) {
                                    Text(
                                        text = "문의 작성",
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.clickable { onNavigate(MyPageRoute.REQUEST) },
                                ) {
                                    Text(
                                        text = "요구사항 작성",
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }
                            }
                        }
                    }
                }

                selectedTicket != null -> {
                    SupportTicketDetailCard(ticket = selectedTicket!!)
                }

                else -> {
                    InfoCard(
                        title = "접수한 내용을 한곳에서 확인해요",
                        body = "목록에서는 제목과 접수일, 종류만 간단히 보여드리고 있어요. 항목을 누르면 상세 내용과 운영 메모를 확인할 수 있어요.",
                    )

                    FormSectionCard(
                        title = "필터",
                        description = "유형과 진행 상태를 선택해서 필요한 내역만 빠르게 볼 수 있어요.",
                    ) {
                        SelectFieldBlock(
                            label = "유형",
                            options = listOf("전체", "문의", "요구사항"),
                            selected = selectedType,
                            onSelect = { selectedType = it },
                        )
                        SelectFieldBlock(
                            label = "상태",
                            options = listOf("전체", "대기", "처리중", "완료", "반려"),
                            selected = selectedStatus,
                            onSelect = { selectedStatus = it },
                        )
                    }

                    if (filteredTickets.isEmpty()) {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Text(
                                    text = if (tickets.isEmpty()) "아직 접수한 내역이 없어요" else "조건에 맞는 내역이 없어요",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = if (tickets.isEmpty()) {
                                        "문의하기 또는 개발자에게 요구사항 화면에서 내용을 접수하면 여기에서 진행 상태를 확인할 수 있어요."
                                    } else {
                                        "선택한 필터에 맞는 문의내역이 없어요. 유형이나 상태를 바꿔서 다시 확인해 보세요."
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 22.sp,
                                )
                                if (tickets.isEmpty()) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Surface(
                                            shape = RoundedCornerShape(999.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier.clickable { onNavigate(MyPageRoute.CONTACT) },
                                        ) {
                                            Text(
                                                text = "문의 작성",
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontWeight = FontWeight.Medium,
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(999.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier.clickable { onNavigate(MyPageRoute.REQUEST) },
                                        ) {
                                            Text(
                                                text = "요구사항 작성",
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontWeight = FontWeight.Medium,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        filteredTickets.forEach { ticket ->
                            SupportTicketListRow(
                                ticket = ticket,
                                onClick = { selectedTicket = ticket },
                            )
                        }
                    }
                }
            }
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
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
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
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationSettingsPage(
    settingsPreferences: AppSettingsPreferences,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var serviceNotificationsEnabled by remember {
        mutableStateOf(settingsPreferences.serviceNotificationsEnabled)
    }
    var parkingAlertsEnabled by remember {
        mutableStateOf(settingsPreferences.parkingAlertsEnabled)
    }
    val backgroundColor = MaterialTheme.colorScheme.background

    Scaffold(
        modifier = modifier,
        containerColor = backgroundColor,
        topBar = {
            PageTopBar(title = "알림 설정", onBack = onBack)
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
                text = "앱 알림",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "서비스 소식과 주차 관련 알림을 받을지 선택할 수 있어요.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(16.dp))

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                NotificationToggleRow(
                    title = "서비스 알림",
                    description = "공지사항과 주요 업데이트 알림을 받아요",
                    checked = serviceNotificationsEnabled,
                    onCheckedChange = {
                        serviceNotificationsEnabled = it
                        settingsPreferences.serviceNotificationsEnabled = it
                    },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                NotificationToggleRow(
                    title = "주차 알림",
                    description = "주차 관련 안내와 이용 알림을 받아요",
                    checked = parkingAlertsEnabled,
                    onCheckedChange = {
                        parkingAlertsEnabled = it
                        settingsPreferences.parkingAlertsEnabled = it
                    },
                )
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
    val backgroundColor = MaterialTheme.colorScheme.background
    Scaffold(
        modifier = modifier,
        containerColor = backgroundColor,
        topBar = {
            PageTopBar(title = "이용 약관", onBack = onBack)
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(20.dp),
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "서비스 이용 약관",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 22.sp,
            )
        }
    }
}

@Composable
private fun SupportTicketListRow(
    ticket: SupportTicket,
    onClick: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Gray100),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "${supportTicketTypeLabel(ticket.ticketType)} · ${formatSupportTicketDate(ticket.createdAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = Gray500,
            )
            HorizontalDivider(color = Gray100)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = ticket.title.ifBlank { "제목 없는 문의" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Gray900,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Gray400,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun SupportTicketDetailCard(ticket: SupportTicket) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = ticket.title.ifBlank { "제목 없는 문의" },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    TicketInfoRow(
                        label = "종류",
                        value = supportTicketTypeLabel(ticket.ticketType),
                    )
                    TicketInfoRow(
                        label = "상태",
                        value = supportTicketStatusLabel(ticket.status),
                        valueColor = supportTicketStatusColor(ticket.status),
                    )
                    TicketInfoRow(
                        label = "접수일",
                        value = formatSupportTicketDate(ticket.createdAt),
                    )
                    TicketInfoRow(
                        label = "카테고리",
                        value = supportTicketCategoryLabel(ticket.category),
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "내용",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = ticket.content.ifBlank { "내용이 비어 있어요." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp,
                )
            }

            if (!ticket.extraContent1.isNullOrBlank()) {
                TicketMetaBlock(
                    label = if (ticket.ticketType == SupportTicketType.INQUIRY) "추가 상황" else "불편한 점",
                    value = ticket.extraContent1,
                )
            }

            if (!ticket.extraContent2.isNullOrBlank()) {
                TicketMetaBlock(
                    label = if (ticket.ticketType == SupportTicketType.INQUIRY) "회신 이메일" else "기대 개선",
                    value = ticket.extraContent2,
                )
            }

            if (!ticket.adminMemo.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = "운영 메모",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = ticket.adminMemo,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 21.sp,
                        )
                    }
                }
            }

            Text(
                text = "앱 ${ticket.appVersion} · ${ticket.osVersion} · ${ticket.deviceModel}",
                style = MaterialTheme.typography.bodySmall,
                color = Gray400,
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 22.sp,
            )
        }
    }
}

@Composable
private fun TicketMetaBlock(
    label: String,
    value: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 21.sp,
        )
    }
}

@Composable
private fun TicketInfoRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Gray500,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun FormSectionCard(
    title: String,
    description: String,
    content: @Composable () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 21.sp,
                )
            }
            content()
        }
    }
}

@Composable
private fun ChoiceChipGroup(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        options.chunked(3).forEach { rowOptions ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowOptions.forEach { option ->
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = if (selected == option) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.clickable { onSelect(option) },
                    ) {
                        Text(
                            text = option,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected == option) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FormTextFieldBlock(
    label: String,
    placeholder: String,
    value: String,
    maxLength: Int,
    minLines: Int = 1,
    onValueChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = placeholder,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                )
            },
            singleLine = minLines == 1,
            minLines = minLines,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
        )
        Text(
            text = "${value.length}/$maxLength",
            style = MaterialTheme.typography.bodySmall,
            color = Gray400,
            modifier = Modifier.align(Alignment.End),
        )
    }
}

@Composable
private fun SelectFieldBlock(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    placeholder: String = "선택하세요",
) {
    var expanded by remember { mutableStateOf(false) }
    var fieldSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val displayText = selected.ifBlank { placeholder }
    val isPlaceholder = selected.isBlank()

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Box {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { fieldSize = it.size }
                    .clickable { expanded = true },
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, Gray100),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 15.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = displayText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isPlaceholder) MaterialTheme.colorScheme.onSurfaceVariant
                        else Gray900,
                        fontWeight = if (isPlaceholder) FontWeight.Normal else FontWeight.Medium,
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            androidx.compose.material3.DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.width(with(density) { fieldSize.width.toDp() }),
            )
            {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = option,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (option == selected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (option == selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface,
                            )
                        },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun FormSubmitButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
        ),
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun TermsText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        lineHeight = 22.sp,
    )
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun ProfileCard(
    nickname: String,
    loginType: String,
) {
    val avatarBackground = MaterialTheme.colorScheme.surfaceVariant
    val mutedText = MaterialTheme.colorScheme.onSurfaceVariant
    val loginIconTint = if (loginType.uppercase() == "KAKAO") {
        if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) {
            MaterialTheme.colorScheme.onSurface
        } else {
            Color(0xD9000000)
        }
    } else {
        Color.Unspecified
    }
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                    .background(avatarBackground),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = mutedText,
                    modifier = Modifier.size(28.dp),
                )
            }

            Spacer(Modifier.size(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = nickname,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(loginIconRes(loginType)),
                            contentDescription = loginType,
                            tint = loginIconTint,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.size(6.dp))
                        Text(
                            text = loginLabel(loginType),
                            style = MaterialTheme.typography.labelMedium,
                            color = mutedText,
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
        color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(Modifier.height(10.dp))

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        items.forEachIndexed { index, item ->
            MenuRow(item = item)
            if (index != items.lastIndex) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            }
        }
    }
}

@Composable
private fun MenuRow(item: MenuItemData) {
    val isDark = isAppInDarkTheme()
    val iconBackground = MaterialTheme.colorScheme.surfaceVariant
    val iconTint = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
    val secondaryText = MaterialTheme.colorScheme.onSurfaceVariant
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
                .background(iconBackground),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = iconTint,
            )
        }

        Spacer(Modifier.size(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = secondaryText,
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = secondaryText,
        )
    }
}

@Composable
private fun NotificationToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.size(12.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
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
                    color = MaterialTheme.colorScheme.onSurface,
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

private fun inquiryCategoryOf(label: String): InquiryCategory {
    return when (label) {
        "계정/로그인" -> InquiryCategory.INQUIRY_ACCOUNT
        "앱 사용" -> InquiryCategory.INQUIRY_APP_USAGE
        "버그/오류" -> InquiryCategory.INQUIRY_BUG_REPORT
        else -> InquiryCategory.INQUIRY_OTHER
    }
}

private fun featureRequestCategoryOf(label: String): FeatureRequestCategory {
    return when (label) {
        "UI/UX" -> FeatureRequestCategory.FEATURE_UI_UX
        "성능" -> FeatureRequestCategory.FEATURE_PERFORMANCE
        "기타" -> FeatureRequestCategory.FEATURE_OTHER
        else -> FeatureRequestCategory.FEATURE_NEW_CAPABILITY
    }
}

private fun supportTicketTypeLabel(type: SupportTicketType): String {
    return when (type) {
        SupportTicketType.INQUIRY -> "문의"
        SupportTicketType.FEATURE_REQUEST -> "요구사항"
    }
}

private fun supportTicketStatusLabel(status: SupportTicketStatus): String {
    return when (status) {
        SupportTicketStatus.PENDING -> "대기"
        SupportTicketStatus.IN_PROGRESS -> "처리중"
        SupportTicketStatus.RESOLVED -> "완료"
        SupportTicketStatus.REJECTED -> "반려"
    }
}

private fun supportTicketStatusColor(status: SupportTicketStatus): Color {
    return when (status) {
        SupportTicketStatus.PENDING -> Amber
        SupportTicketStatus.IN_PROGRESS -> Primary
        SupportTicketStatus.RESOLVED -> Green
        SupportTicketStatus.REJECTED -> Red
    }
}

private fun supportTicketCategoryLabel(category: String): String {
    return when (category) {
        "INQUIRY_ACCOUNT" -> "계정/로그인"
        "INQUIRY_APP_USAGE" -> "앱 사용"
        "INQUIRY_BUG_REPORT" -> "버그/오류"
        "INQUIRY_OTHER" -> "기타 문의"
        "FEATURE_NEW_CAPABILITY" -> "새 기능"
        "FEATURE_UI_UX" -> "UI/UX"
        "FEATURE_PERFORMANCE" -> "성능"
        "FEATURE_OTHER" -> "기타 요구사항"
        else -> category
    }
}

private fun formatSupportTicketDate(value: String): String {
    return value.take(10).replace("-", ".")
}
