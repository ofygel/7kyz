package com.freedomapp.data

import com.freedomapp.model.BannerSeverity
import com.freedomapp.model.City
import com.freedomapp.model.NotificationBanner
import com.freedomapp.model.Order
import com.freedomapp.model.OrderStatus
import com.freedomapp.model.OrderType
import com.freedomapp.model.SubscriptionStatus
import com.freedomapp.model.UserProfile
import com.freedomapp.model.VerificationRequest
import com.freedomapp.model.VerificationStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

class FreedomRepository {
    val supportedCities = listOf(
        City("ala", "Алматы"),
        City("ast", "Астана"),
        City("shy", "Шымкент"),
        City("akt", "Актобе")
    )

    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders

    private val _verificationQueue = MutableStateFlow<List<VerificationRequest>>(emptyList())
    val verificationQueue: StateFlow<List<VerificationRequest>> = _verificationQueue

    private val _banners = MutableStateFlow<List<NotificationBanner>>(emptyList())
    val banners: StateFlow<List<NotificationBanner>> = _banners

    private val _safeMode = MutableStateFlow(false)
    val safeMode: StateFlow<Boolean> = _safeMode

    init {
        seedOrders()
    }

    fun createOrder(profile: UserProfile, input: CreateOrderInput): Order {
        val newOrder = Order(
            type = input.type,
            city = profile.selectedCity,
            pickupAddress = input.pickup,
            dropOffAddress = input.dropOff,
            budget = input.budget,
            note = input.note,
            clientName = profile.displayName
        )
        _orders.update { listOf(newOrder) + it }
        return newOrder
    }

    fun claimOrder(orderId: UUID, executor: UserProfile): Order? {
        var claimed: Order? = null
        _orders.update { orders ->
            orders.map { order ->
                if (order.id == orderId && order.status == OrderStatus.PENDING) {
                    claimed = order.copy(status = OrderStatus.CLAIMED, executorName = executor.displayName)
                    claimed!!
                } else {
                    order
                }
            }
        }
        return claimed
    }

    fun advanceOrder(orderId: UUID): Order? {
        var updated: Order? = null
        _orders.update { orders ->
            orders.map { order ->
                if (order.id == orderId) {
                    val nextStatus = when (order.status) {
                        OrderStatus.PENDING -> OrderStatus.CLAIMED
                        OrderStatus.CLAIMED -> OrderStatus.IN_PROGRESS
                        OrderStatus.IN_PROGRESS -> OrderStatus.COMPLETED
                        else -> order.status
                    }
                    updated = order.copy(status = nextStatus)
                    updated!!
                } else {
                    order
                }
            }
        }
        return updated
    }

    fun cancelOrder(orderId: UUID): Order? {
        var cancelled: Order? = null
        _orders.update { orders ->
            orders.map { order ->
                if (order.id == orderId) {
                    cancelled = order.copy(status = OrderStatus.CANCELLED)
                    cancelled!!
                } else {
                    order
                }
            }
        }
        return cancelled
    }

    fun submitVerification(executor: UserProfile, attachments: List<String>): VerificationRequest {
        val request = VerificationRequest(
            executorName = executor.displayName,
            phone = executor.phone,
            city = executor.selectedCity,
            attachments = attachments
        )
        _verificationQueue.update { it + request }
        return request
    }

    fun reviewVerification(requestId: UUID, approved: Boolean, moderatorName: String): VerificationStatus {
        var decision: VerificationStatus = VerificationStatus.Rejected("Не найдено")
        _verificationQueue.update { queue ->
            val remaining = queue.filterNot { request ->
                if (request.id == requestId) {
                    decision = if (approved) {
                        VerificationStatus.Approved
                    } else {
                        VerificationStatus.Rejected("Отклонено модератором $moderatorName")
                    }
                    true
                } else {
                    false
                }
            }
            remaining
        }
        return decision
    }

    fun toggleSafeMode(enabled: Boolean) {
        _safeMode.value = enabled
        if (enabled) {
            addBanner(
                NotificationBanner(
                    title = "Техработы",
                    message = "Сервис временно в режиме обслуживания. Мы уже работаем над решением.",
                    severity = BannerSeverity.Critical
                )
            )
        } else {
            clearBanner(BannerSeverity.Critical)
        }
    }

    fun refreshTrial(user: UserProfile): SubscriptionStatus {
        return SubscriptionStatus.Trial(Duration.ofHours(48), Instant.now())
    }

    fun activateSubscription(): SubscriptionStatus = SubscriptionStatus.Active

    private fun seedOrders() {
        val sample = listOf(
            Order(
                type = OrderType.DELIVERY,
                city = supportedCities.first(),
                pickupAddress = "пр. Абая 15, ЖК Көк Тау",
                dropOffAddress = "ул. Панфилова 100",
                budget = 4500,
                note = "Документы до 18:00",
                status = OrderStatus.PENDING,
                clientName = "Айгүл",
                createdAt = Instant.now().minus(2, ChronoUnit.HOURS)
            ),
            Order(
                type = OrderType.TAXI,
                city = supportedCities[1],
                pickupAddress = "Аэропорт Нур-Султан",
                dropOffAddress = "ул. Қабанбай батыра 21",
                budget = 2500,
                note = "Водитель с табличкой",
                status = OrderStatus.CLAIMED,
                clientName = "Расул",
                executorName = "Алексей",
                createdAt = Instant.now().minus(30, ChronoUnit.MINUTES)
            )
        )
        _orders.value = sample
    }

    private fun addBanner(banner: NotificationBanner) {
        _banners.update { current ->
            if (current.none { it.severity == banner.severity }) current + banner else current
        }
    }

    private fun clearBanner(severity: BannerSeverity) {
        _banners.update { current -> current.filterNot { it.severity == severity } }
    }
}

data class CreateOrderInput(
    val type: OrderType,
    val pickup: String,
    val dropOff: String?,
    val budget: Int,
    val note: String
)
