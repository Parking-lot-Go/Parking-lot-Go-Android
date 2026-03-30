package com.carpark.android.ui.login

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.carpark.android.BuildConfig
import com.carpark.android.R
import com.carpark.android.viewmodel.AuthState
import com.carpark.android.viewmodel.AuthViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: () -> Unit,
) {
    val context = LocalContext.current
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Success -> onLoginSuccess()
            is AuthState.Error -> {
                Toast.makeText(context, (authState as AuthState.Error).message, Toast.LENGTH_SHORT).show()
                viewModel.resetState()
            }
            else -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .systemBarsPadding(),
    ) {
        // 로고 + 앱 이름
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 160.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_home),
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color(0xFF2563EB),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "주차장Go",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "내 주변 주차장을 한눈에",
                fontSize = 15.sp,
                color = Color(0xFF94A3B8),
            )
        }

        // 로그인 버튼 영역
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp, vertical = 48.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 카카오 로그인 버튼
            Button(
                onClick = { viewModel.loginWithKakao(context) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFEE500),
                    contentColor = Color(0xD9000000),
                ),
                enabled = authState !is AuthState.Loading,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_kakao),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Color(0xD9000000),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "카카오로 시작하기",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            // 구글 로그인 버튼
            OutlinedButton(
                onClick = {
                    scope.launch {
                        handleGoogleLogin(context, viewModel)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF0F172A),
                ),
                enabled = authState !is AuthState.Loading,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_google),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Color.Unspecified,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "구글로 시작하기",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        // 로딩 인디케이터
        if (authState is AuthState.Loading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = Color(0xFF2563EB))
            }
        }
    }
}

private suspend fun handleGoogleLogin(
    context: android.content.Context,
    viewModel: AuthViewModel,
) {
    try {
        val credentialManager = CredentialManager.create(context)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(BuildConfig.GOOGLE_CLIENT_ID)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
        val result = credentialManager.getCredential(context, request)
        val credential = result.credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data)
            viewModel.handleGoogleLoginResult(
                idToken = googleIdToken.idToken,
                displayName = googleIdToken.displayName,
                photoUrl = googleIdToken.profilePictureUri?.toString(),
            )
        }
    } catch (_: GetCredentialCancellationException) {
        // 사용자가 취소
    } catch (_: NoCredentialException) {
        viewModel.setError("사용 가능한 구글 계정이 없습니다")
    } catch (e: Exception) {
        viewModel.setError(e.message ?: "구글 로그인에 실패했습니다")
    }
}
