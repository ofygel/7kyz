package com.freedomapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freedomapp.data.CreateOrderInput
import com.freedomapp.data.FreedomRepository
import com.freedomapp.model.OrderStatus
import com.freedomapp.model.OrderType
import com.freedomapp.model.SubscriptionStatus
import com.freedomapp.model.UiEvent
import com.freedomapp.model.UserProfile
import com.freedomapp.model.UserRole
import com.freedomapp.model.VerificationStatus
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FreedomAppViewModel(
    private val repository: FreedomRepository = FreedomRepository()
) : ViewModel() {

    private val profiles: MutableMap<UserRole, UserProfile> = mutableMapOf()

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    init {
        _uiState.update { state ->
            state.copy(availableCities = repository.supportedCities)
        }
        viewModelScope.launch {
            combine(
                repository.orders,
                repository.verificationQueue,
                repository.banners,
                repository.safeMode
            ) { orders, queue, banners, safeMode ->
                Quadruple(orders, queue, banners, safeMode)
            }.collect { (orders, queue, banners, safeMode) ->
                _uiState.update { state ->
                    val executorProfile = state.executorState.profile ?: profiles[UserRole.EXECUTOR]
                    val assignedOrders = executorProfile?.let { profile ->
                        orders.filter { it.executorName == profile.displayName }
                    } ?: emptyList()
                    val availableOrders = orders.filter { order ->
                        order.status == OrderStatus.PENDING ||
                            (executorProfile != null && order.executorName == executorProfile.displayName &&
                                order.status != OrderStatus.COMPLETED && order.status != OrderStatus.CANCELLED)
                    }

                    state.copy(
                        clientState = state.clientState.copy(orders = orders),
                        executorState = state.executorState.copy(
                            availableOrders = availableOrders,
                            assignedOrders = assignedOrders
                        ),
                        moderatorState = state.moderatorState.copy(pendingRequests = queue),
                        adminState = state.adminState.copy(
                            safeModeEnabled = safeMode,
                            banners = banners
                        )
                    )
                }
            }
        }
    }

    fun onPhoneSubmitted(phone: String) {
        _uiState.update { state ->
            state.copy(
                phone = phone,
                isOnboardingComplete = state.selectedCity != null
            )
        }
        updateProfiles { profile -> profile.copy(phone = phone) }
    }

    fun onCitySelected(city: String) {
        val selectedCity = repository.supportedCities.firstOrNull { it.code == city }
            ?: repository.supportedCities.first()
        _uiState.update { state ->
            state.copy(
                selectedCity = selectedCity,
                isOnboardingComplete = state.phone != null
            )
        }
        updateProfiles { profile -> profile.copy(selectedCity = selectedCity) }
    }

    fun onRoleSelected(role: UserRole) {
        val profile = ensureProfile(role)
        _uiState.update { state ->
            when (role) {
                UserRole.CLIENT -> state.copy(
                    selectedRole = role,
                    clientState = state.clientState.copy(profile = profile)
                )
                UserRole.EXECUTOR -> state.copy(
                    selectedRole = role,
                    executorState = state.executorState.copy(
                        profile = profile,
                        verificationStatus = state.executorState.verificationStatus,
                        subscriptionStatus = state.executorState.subscriptionStatus
                            ?: SubscriptionStatus.Trial(Duration.ofHours(48), Instant.now())
                    )
                )
                UserRole.MODERATOR -> state.copy(selectedRole = role)
                UserRole.ADMIN -> state.copy(selectedRole = role)
            }
        }
    }

    fun onCreateOrder(
        type: OrderType,
        pickup: String,
        dropOff: String?,
        budget: Int,
        note: String
    ) {
        val profile = ensureProfile(UserRole.CLIENT)
        val order = repository.createOrder(
            profile,
            CreateOrderInput(type = type, pickup = pickup, dropOff = dropOff, budget = budget, note = note)
        )
        _uiState.update { state ->
            state.copy(
                selectedRole = UserRole.CLIENT,
                clientState = state.clientState.copy(
                    profile = profile,
                    lastCreatedOrderId = order.id.toString()
                ),
                event = UiEvent.Toast("Заказ опубликован в ${profile.selectedCity.title}")
            )
        }
    }

    fun onClaimOrder(orderId: UUID) {
        val executorState = _uiState.value.executorState
        val profile = executorState.profile ?: ensureProfile(UserRole.EXECUTOR)

        if (executorState.verificationStatus != VerificationStatus.Approved) {
            pushToast("Требуется пройти верификацию, прежде чем брать заказы")
            return
        }
        val subscription = executorState.subscriptionStatus
        if (subscription is SubscriptionStatus.Expired) {
            pushToast("Срок подписки истёк. Оформите подписку, чтобы продолжить")
            return
        }

        val claimed = repository.claimOrder(orderId, profile)
        if (claimed != null) {
            _uiState.update { state ->
                state.copy(
                    executorState = state.executorState.copy(profile = profile),
                    event = UiEvent.Toast("Заказ закреплён за вами")
                )
            }
        } else {
            pushToast("Заказ уже взят другим исполнителем")
        }
    }

    fun onAdvanceOrder(orderId: UUID) {
        val updated = repository.advanceOrder(orderId)
        if (updated != null) {
            pushToast("Статус заказа обновлён: ${updated.status.name}")
        }
    }

    fun onCancelOrder(orderId: UUID) {
        val cancelled = repository.cancelOrder(orderId)
        if (cancelled != null) {
            pushToast("Заказ отменён")
        }
    }

    fun onSubmitVerification(attachments: List<String>) {
        val profile = ensureProfile(UserRole.EXECUTOR)
        val request = repository.submitVerification(profile, attachments)
        _uiState.update { state ->
            state.copy(
                executorState = state.executorState.copy(
                    profile = profile,
                    verificationStatus = VerificationStatus.Pending,
                    lastSubmittedRequestId = request.id.toString()
                ),
                event = UiEvent.Toast("Документы отправлены на модерацию")
            )
        }
    }

    fun onModeratorDecision(requestId: UUID, approved: Boolean) {
        val moderatorName = _uiState.value.moderatorState.moderatorName
        val decision = repository.reviewVerification(requestId, approved, moderatorName)
        _uiState.update { state ->
            val current = state.executorState
            val shouldUpdateExecutor = current.lastSubmittedRequestId == requestId.toString()
            state.copy(
                executorState = if (shouldUpdateExecutor) {
                    current.copy(
                        verificationStatus = decision,
                        profile = current.profile?.copy(verification = decision)
                    )
                } else {
                    current
                },
                event = UiEvent.Toast(
                    if (approved) "Исполнитель одобрен" else "Заявка отклонена"
                )
            )
        }
    }

    fun onToggleSafeMode(enabled: Boolean) {
        repository.toggleSafeMode(enabled)
    }

    fun onActivateSubscription() {
        val status = repository.activateSubscription()
        _uiState.update { state ->
            state.copy(
                executorState = state.executorState.copy(
                    subscriptionStatus = status,
                    profile = state.executorState.profile?.copy(subscription = status)
                ),
                event = UiEvent.Toast("Подписка активирована")
            )
        }
    }

    fun onRenewTrial() {
        val profile = ensureProfile(UserRole.EXECUTOR)
        val refreshed = repository.refreshTrial(profile)
        _uiState.update { state ->
            state.copy(
                executorState = state.executorState.copy(
                    profile = profile.copy(subscription = refreshed),
                    subscriptionStatus = refreshed
                ),
                event = UiEvent.Toast("Пробный период обновлён")
            )
        }
    }

    fun consumeEvent() {
        _uiState.update { state -> state.copy(event = null) }
    }

    private fun ensureProfile(role: UserRole): UserProfile {
        val currentCity = _uiState.value.selectedCity ?: repository.supportedCities.first()
        val phone = _uiState.value.phone ?: "+7"
        val existing = profiles[role]
        val displayName = existing?.displayName ?: when (role) {
            UserRole.CLIENT -> "Клиент ${phone.takeLast(4)}"
            UserRole.EXECUTOR -> "Исполнитель ${phone.takeLast(4)}"
            UserRole.MODERATOR -> "Модератор"
            UserRole.ADMIN -> "Администратор"
        }

        val profile = existing?.copy(
            phone = phone,
            selectedCity = currentCity
        ) ?: UserProfile(
            phone = phone,
            role = role,
            selectedCity = currentCity,
            displayName = displayName,
            verification = if (role == UserRole.EXECUTOR) VerificationStatus.NotSubmitted else VerificationStatus.Approved,
            subscription = when (role) {
                UserRole.EXECUTOR -> SubscriptionStatus.Trial(Duration.ofHours(48), Instant.now())
                else -> SubscriptionStatus.Active
            }
        )
        profiles[role] = profile
        return profile
    }

    private fun pushToast(message: String) {
        _uiState.update { state -> state.copy(event = UiEvent.Toast(message)) }
    }

    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    private fun updateProfiles(transform: (UserProfile) -> UserProfile) {
        val snapshot = profiles.toMap()
        snapshot.forEach { (role, profile) ->
            profiles[role] = transform(profile)
        }
    }
}
