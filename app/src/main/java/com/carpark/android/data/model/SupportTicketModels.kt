package com.carpark.android.data.model

enum class SupportTicketType {
    INQUIRY,
    FEATURE_REQUEST,
}

enum class SupportTicketStatus {
    PENDING,
    IN_PROGRESS,
    RESOLVED,
    REJECTED,
}

enum class InquiryCategory {
    INQUIRY_ACCOUNT,
    INQUIRY_APP_USAGE,
    INQUIRY_BUG_REPORT,
    INQUIRY_OTHER,
}

enum class FeatureRequestCategory {
    FEATURE_NEW_CAPABILITY,
    FEATURE_UI_UX,
    FEATURE_PERFORMANCE,
    FEATURE_OTHER,
}

data class SupportTicket(
    val id: Long,
    val userId: Long,
    val ticketType: SupportTicketType,
    val category: String,
    val title: String,
    val content: String,
    val extraContent1: String? = null,
    val extraContent2: String? = null,
    val appVersion: String,
    val osVersion: String,
    val deviceModel: String,
    val status: SupportTicketStatus,
    val adminMemo: String? = null,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String? = null,
)

data class CreateSupportTicketRequest(
    val ticketType: SupportTicketType,
    val category: String,
    val title: String,
    val content: String,
    val extraContent1: String? = null,
    val extraContent2: String? = null,
    val appVersion: String,
    val osVersion: String,
    val deviceModel: String,
)

data class UpdateSupportTicketRequest(
    val title: String? = null,
    val content: String? = null,
    val extraContent1: String? = null,
    val extraContent2: String? = null,
    val deviceModel: String? = null,
)
