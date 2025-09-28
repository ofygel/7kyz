package com.freedomapp.viewmodel

import com.freedomapp.model.City
import com.freedomapp.model.NotificationBanner
import com.freedomapp.model.Order
import com.freedomapp.model.UiEvent
import com.freedomapp.model.UserProfile
import com.freedomapp.model.UserRole
import com.freedomapp.model.VerificationRequest
import com.freedomapp.model.VerificationStatus
import com.freedomapp.model.SubscriptionStatus

data class ClientDashboardState(
    val profile: UserProfile? = null,
    val orders: List<Order> = emptyList(),
    val lastCreatedOrderId: String? = null
)

data class ExecutorDashboardState(
    val profile: UserProfile? = null,
    val verificationStatus: VerificationStatus = VerificationStatus.NotSubmitted,
    val subscriptionStatus: SubscriptionStatus? = null,
    val availableOrders: List<Order> = emptyList(),
    val assignedOrders: List<Order> = emptyList(),
    val lastSubmittedRequestId: String? = null
)

data class ModeratorDashboardState(
    val moderatorName: String = "Айгерим",
    val pendingRequests: List<VerificationRequest> = emptyList()
)

data class AdminDashboardState(
    val safeModeEnabled: Boolean = false,
    val banners: List<NotificationBanner> = emptyList()
)

data class AppUiState(
    val phone: String? = null,
    val selectedCity: City? = null,
    val availableCities: List<City> = emptyList(),
    val selectedRole: UserRole? = null,
    val isOnboardingComplete: Boolean = false,
    val clientState: ClientDashboardState = ClientDashboardState(),
    val executorState: ExecutorDashboardState = ExecutorDashboardState(),
    val moderatorState: ModeratorDashboardState = ModeratorDashboardState(),
    val adminState: AdminDashboardState = AdminDashboardState(),
    val event: UiEvent? = null
)
