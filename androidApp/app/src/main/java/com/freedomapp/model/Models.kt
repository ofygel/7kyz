package com.freedomapp.model

import java.time.Duration
import java.time.Instant
import java.util.UUID

enum class UserRole { CLIENT, EXECUTOR, MODERATOR, ADMIN }

enum class OrderType { TAXI, DELIVERY }

enum class OrderStatus { PENDING, CLAIMED, IN_PROGRESS, COMPLETED, CANCELLED }

data class City(
    val code: String,
    val title: String
)

data class UserProfile(
    val id: UUID = UUID.randomUUID(),
    val phone: String,
    val role: UserRole,
    val selectedCity: City,
    val displayName: String,
    val verification: VerificationStatus = VerificationStatus.NotSubmitted,
    val subscription: SubscriptionStatus = SubscriptionStatus.Trial(Duration.ofHours(48), Instant.now())
)

sealed interface VerificationStatus {
    data object NotSubmitted : VerificationStatus
    data object Pending : VerificationStatus
    data object Approved : VerificationStatus
    data class Rejected(val reason: String) : VerificationStatus
}

sealed interface SubscriptionStatus {
    data class Trial(val remaining: Duration, val startedAt: Instant) : SubscriptionStatus
    data object Active : SubscriptionStatus
    data class Expired(val expiredAt: Instant) : SubscriptionStatus
}

data class Order(
    val id: UUID = UUID.randomUUID(),
    val type: OrderType,
    val city: City,
    val pickupAddress: String,
    val dropOffAddress: String?,
    val budget: Int,
    val note: String,
    val status: OrderStatus = OrderStatus.PENDING,
    val createdAt: Instant = Instant.now(),
    val clientName: String,
    val executorName: String? = null
)

data class VerificationRequest(
    val id: UUID = UUID.randomUUID(),
    val executorName: String,
    val phone: String,
    val submittedAt: Instant = Instant.now(),
    val city: City,
    val attachments: List<String>
)

data class NotificationBanner(
    val id: UUID = UUID.randomUUID(),
    val title: String,
    val message: String,
    val severity: BannerSeverity = BannerSeverity.Info
)

enum class BannerSeverity { Info, Warning, Critical }

sealed interface UiEvent {
    data class Toast(val message: String) : UiEvent
}
