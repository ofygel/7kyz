package com.freedomapp.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.freedomapp.model.BannerSeverity
import com.freedomapp.model.City
import com.freedomapp.model.Order
import com.freedomapp.model.OrderStatus
import com.freedomapp.model.OrderType
import com.freedomapp.model.SubscriptionStatus
import com.freedomapp.model.UiEvent
import com.freedomapp.model.UserRole
import com.freedomapp.model.VerificationRequest
import com.freedomapp.model.VerificationStatus
import com.freedomapp.viewmodel.AppUiState
import com.freedomapp.viewmodel.ClientDashboardState
import com.freedomapp.viewmodel.ExecutorDashboardState
import com.freedomapp.viewmodel.ModeratorDashboardState
import java.util.UUID
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FreedomAppRoot(
    state: AppUiState,
    onPhoneSubmitted: (String) -> Unit,
    onCitySelected: (String) -> Unit,
    onRoleSelected: (UserRole) -> Unit,
    onCreateOrder: (OrderType, String, String?, Int, String) -> Unit,
    onClaimOrder: (UUID) -> Unit,
    onAdvanceOrder: (UUID) -> Unit,
    onCancelOrder: (UUID) -> Unit,
    onSubmitVerification: (List<String>) -> Unit,
    onModeratorDecision: (UUID, Boolean) -> Unit,
    onToggleSafeMode: (Boolean) -> Unit,
    onActivateSubscription: () -> Unit,
    onRenewTrial: () -> Unit,
    onConsumeEvent: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.event) {
        when (val event = state.event) {
            is UiEvent.Toast -> {
                scope.launch {
                    snackbarHostState.showSnackbar(event.message)
                    onConsumeEvent()
                }
            }
            null -> Unit
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = "Freedom", style = MaterialTheme.typography.titleMedium)
                        state.selectedCity?.let {
                            Text(
                                text = "${it.title} · ${state.phone ?: "номер не указан"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.phone.isNullOrBlank() -> PhoneCaptureScreen(onPhoneSubmitted)
                state.selectedCity == null -> CitySelectionScreen(state.availableCities, onCitySelected)
                else -> Dashboard(
                    state = state,
                    onRoleSelected = onRoleSelected,
                    onCreateOrder = onCreateOrder,
                    onClaimOrder = onClaimOrder,
                    onAdvanceOrder = onAdvanceOrder,
                    onCancelOrder = onCancelOrder,
                    onSubmitVerification = onSubmitVerification,
                    onModeratorDecision = onModeratorDecision,
                    onToggleSafeMode = onToggleSafeMode,
                    onActivateSubscription = onActivateSubscription,
                    onRenewTrial = onRenewTrial
                )
            }
        }
    }
}

@Composable
private fun PhoneCaptureScreen(onPhoneSubmitted: (String) -> Unit) {
    var phone by remember { mutableStateOf("+7") }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Phone,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Чтобы продолжить, поделитесь номером",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Мы используем номер только для связи по заказам.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it.take(16) },
            label = { Text("Номер телефона") }
        )
        Button(
            onClick = { onPhoneSubmitted(phone) },
            modifier = Modifier.fillMaxWidth(),
            enabled = phone.length >= 6
        ) {
            Text("Поделиться контактом")
        }
    }
}

@Composable
private fun CitySelectionScreen(cities: List<City>, onCitySelected: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "Выберите город", style = MaterialTheme.typography.headlineMedium)
        if (cities.isEmpty()) {
            Text(
                text = "Загрузка списка городов...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            cities.forEach { city ->
                ElevatedCard(
                    onClick = { onCitySelected(city.code) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = city.title, style = MaterialTheme.typography.titleMedium)
                        Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null)
                    }
                }
            }
        }
    }
}

@Composable
private fun Dashboard(
    state: AppUiState,
    onRoleSelected: (UserRole) -> Unit,
    onCreateOrder: (OrderType, String, String?, Int, String) -> Unit,
    onClaimOrder: (UUID) -> Unit,
    onAdvanceOrder: (UUID) -> Unit,
    onCancelOrder: (UUID) -> Unit,
    onSubmitVerification: (List<String>) -> Unit,
    onModeratorDecision: (UUID, Boolean) -> Unit,
    onToggleSafeMode: (Boolean) -> Unit,
    onActivateSubscription: () -> Unit,
    onRenewTrial: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        RoleSelector(
            selectedRole = state.selectedRole,
            onRoleSelected = onRoleSelected
        )
        Spacer(modifier = Modifier.height(16.dp))
        state.adminState.banners.takeIf { it.isNotEmpty() }?.let { banners ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                banners.forEach { banner ->
                    BannerCard(banner.title, banner.message, banner.severity)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        when (state.selectedRole ?: UserRole.CLIENT) {
            UserRole.CLIENT -> ClientDashboard(
                state = state.clientState,
                onCreateOrder = onCreateOrder,
                modifier = Modifier.weight(1f)
            )
            UserRole.EXECUTOR -> ExecutorDashboard(
                state = state.executorState,
                onClaimOrder = onClaimOrder,
                onAdvanceOrder = onAdvanceOrder,
                onCancelOrder = onCancelOrder,
                onSubmitVerification = onSubmitVerification,
                onActivateSubscription = onActivateSubscription,
                onRenewTrial = onRenewTrial,
                modifier = Modifier.weight(1f)
            )
            UserRole.MODERATOR -> ModeratorDashboard(
                state = state.moderatorState,
                onModeratorDecision = onModeratorDecision,
                modifier = Modifier.weight(1f)
            )
            UserRole.ADMIN -> AdminDashboard(
                state = state,
                onToggleSafeMode = onToggleSafeMode,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun RoleSelector(selectedRole: UserRole?, onRoleSelected: (UserRole) -> Unit) {
    val roles = listOf(
        Triple(UserRole.CLIENT, "Клиент", Icons.Default.Person),
        Triple(UserRole.EXECUTOR, "Исполнитель", Icons.Default.LocalShipping),
        Triple(UserRole.MODERATOR, "Модератор", Icons.Default.Security),
        Triple(UserRole.ADMIN, "Администратор", Icons.Default.AdminPanelSettings)
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        roles.forEach { (role, title, icon) ->
            val isSelected = role == (selectedRole ?: UserRole.CLIENT)
            ElevatedCard(
                onClick = { onRoleSelected(role) },
                modifier = Modifier.weight(1f),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                )
            ) {
                Column(
                    modifier = Modifier
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = icon, contentDescription = null)
                    Text(text = title, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun ClientDashboard(
    state: ClientDashboardState,
    onCreateOrder: (OrderType, String, String?, Int, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreate by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "Создать заказ", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Такси или доставка с публикацией в канал исполнителей",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(onClick = { showCreate = true }) {
                            Icon(imageVector = Icons.Default.AssignmentTurnedIn, contentDescription = null)
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("Новый заказ")
                        }
                    }
                }
            }
            item {
                Text(text = "Активные заказы", style = MaterialTheme.typography.titleMedium)
            }
            items(state.orders) { order ->
                OrderCard(order)
            }
        }
    }

    if (showCreate) {
        OrderCreationDialog(
            onDismiss = { showCreate = false },
            onConfirm = { type, pickup, dropOff, budget, note ->
                onCreateOrder(type, pickup, dropOff, budget, note)
                showCreate = false
            }
        )
    }
}

@Composable
private fun ExecutorDashboard(
    state: ExecutorDashboardState,
    onClaimOrder: (UUID) -> Unit,
    onAdvanceOrder: (UUID) -> Unit,
    onCancelOrder: (UUID) -> Unit,
    onSubmitVerification: (List<String>) -> Unit,
    onActivateSubscription: () -> Unit,
    onRenewTrial: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item { VerificationBanner(state.verificationStatus, onSubmitVerification) }
        item { SubscriptionBanner(state.subscriptionStatus, onActivateSubscription, onRenewTrial) }
        item { Text(text = "Доступные заказы", style = MaterialTheme.typography.titleMedium) }
        items(state.availableOrders) { order ->
            OrderActionCard(
                order = order,
                primaryAction = {
                    if (order.executorName == state.profile?.displayName) {
                        onAdvanceOrder(order.id)
                    } else {
                        onClaimOrder(order.id)
                    }
                },
                secondaryAction = {
                    onCancelOrder(order.id)
                },
                isAssignedToMe = order.executorName == state.profile?.displayName
            )
        }
    }
}

@Composable
private fun ModeratorDashboard(
    state: ModeratorDashboardState,
    onModeratorDecision: (UUID, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item { Text(text = "Очередь верификации", style = MaterialTheme.typography.titleMedium) }
        if (state.pendingRequests.isEmpty()) {
            item { EmptyStateCard(text = "Новых заявок пока нет") }
        } else {
            items(state.pendingRequests) { request ->
                VerificationRequestCard(request, onModeratorDecision)
            }
        }
    }
}

@Composable
private fun AdminDashboard(
    state: AppUiState,
    onToggleSafeMode: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item { Text(text = "Администрирование", style = MaterialTheme.typography.titleMedium) }
        item {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Safe-mode", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = if (state.adminState.safeModeEnabled) "Включен" else "Выключен",
                            color = if (state.adminState.safeModeEnabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(onClick = { onToggleSafeMode(!state.adminState.safeModeEnabled) }) {
                        Text(if (state.adminState.safeModeEnabled) "Выключить" else "Включить")
                    }
                }
            }
        }
        if (state.adminState.banners.isNotEmpty()) {
            item { Text(text = "Баннеры", style = MaterialTheme.typography.titleMedium) }
            items(state.adminState.banners) { banner ->
                BannerCard(banner.title, banner.message, banner.severity)
            }
        }
    }
}

@Composable
private fun OrderCard(order: Order) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = if (order.type == OrderType.TAXI) Icons.Default.LocalShipping else Icons.Default.AssignmentTurnedIn,
                    contentDescription = null
                )
                Text(text = order.type.name, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.weight(1f))
                StatusChip(order.status)
            }
            Text(text = "Откуда: ${order.pickupAddress}", style = MaterialTheme.typography.bodyMedium)
            order.dropOffAddress?.let {
                Text(text = "Куда: $it", style = MaterialTheme.typography.bodyMedium)
            }
            Text(text = "Бюджет: ${order.budget} ₸", fontWeight = FontWeight.Bold)
            Text(text = order.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun OrderActionCard(
    order: Order,
    primaryAction: () -> Unit,
    secondaryAction: () -> Unit,
    isAssignedToMe: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(imageVector = Icons.Default.LocalShipping, contentDescription = null)
                Column {
                    Text(text = order.pickupAddress, style = MaterialTheme.typography.titleMedium)
                    order.dropOffAddress?.let { Text(text = it, style = MaterialTheme.typography.bodyMedium) }
                }
                Spacer(modifier = Modifier.weight(1f))
                StatusChip(order.status)
            }
            Text(text = "Бюджет: ${order.budget} ₸", fontWeight = FontWeight.Bold)
            Text(text = order.note, style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = primaryAction) {
                    Text(if (isAssignedToMe) "Обновить статус" else "Взять заказ")
                }
                OutlinedButton(onClick = secondaryAction) {
                    Text("Отменить")
                }
            }
        }
    }
}

@Composable
private fun StatusChip(status: OrderStatus) {
    val color = when (status) {
        OrderStatus.PENDING -> MaterialTheme.colorScheme.surfaceVariant
        OrderStatus.CLAIMED -> MaterialTheme.colorScheme.primaryContainer
        OrderStatus.IN_PROGRESS -> MaterialTheme.colorScheme.secondaryContainer
        OrderStatus.COMPLETED -> Color(0xFF00B894)
        OrderStatus.CANCELLED -> MaterialTheme.colorScheme.errorContainer
    }
    Box(
        modifier = Modifier
            .background(color = color, shape = RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text = status.name, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun VerificationBanner(status: VerificationStatus, onSubmitVerification: (List<String>) -> Unit) {
    val (title, description, accent, action) = when (status) {
        VerificationStatus.NotSubmitted -> Quad("Верификация", "Прикрепите фото документов. Без них доступ ограничен.", MaterialTheme.colorScheme.error, "Загрузить")
        VerificationStatus.Pending -> Quad("Документы на проверке", "Модераторы ответят в течение часа.", MaterialTheme.colorScheme.secondary, null)
        VerificationStatus.Approved -> Quad("Верификация пройдена", "Доступ к заказам открыт.", MaterialTheme.colorScheme.primary, null)
        is VerificationStatus.Rejected -> Quad("Документы отклонены", status.reason, MaterialTheme.colorScheme.error, "Отправить снова")
    }
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(text = description, style = MaterialTheme.typography.bodyMedium)
            if (action != null) {
                OutlinedButton(onClick = { onSubmitVerification(listOf("front.jpg", "back.jpg")) }) {
                    Icon(imageVector = Icons.Default.FileUpload, contentDescription = null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(action)
                }
            }
        }
    }
}

@Composable
private fun SubscriptionBanner(
    status: SubscriptionStatus?,
    onActivateSubscription: () -> Unit,
    onRenewTrial: () -> Unit
) {
    val body = when (status) {
        is SubscriptionStatus.Trial -> {
            val hours = status.remaining.toHours()
            "Вам доступен бесплатный доступ ещё: ⏳ ${hours}ч"
        }
        SubscriptionStatus.Active -> "Подписка активна"
        is SubscriptionStatus.Expired -> "Срок бесплатного доступа истёк. Оформите подписку, чтобы продолжить."
        null -> "Оформите подписку, чтобы видеть заказы"
    }
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = "Подписка", style = MaterialTheme.typography.titleMedium)
                Text(text = body, style = MaterialTheme.typography.bodyMedium)
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.End) {
                Button(onClick = onActivateSubscription) {
                    Text("Активировать")
                }
                OutlinedButton(onClick = onRenewTrial) {
                    Text("Продлить trial")
                }
            }
        }
    }
}

@Composable
private fun EmptyStateCard(text: String) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(imageVector = Icons.Default.Verified, contentDescription = null)
            Text(text = text, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun VerificationRequestCard(
    request: VerificationRequest,
    onModeratorDecision: (UUID, Boolean) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = request.executorName, style = MaterialTheme.typography.titleMedium)
            Text(text = request.phone, style = MaterialTheme.typography.bodyMedium)
            Text(text = "Город: ${request.city.title}")
            Text(text = "Файлы: ${request.attachments.joinToString()}")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { onModeratorDecision(request.id, true) }) {
                    Text("Одобрить")
                }
                OutlinedButton(onClick = { onModeratorDecision(request.id, false) }) {
                    Text("Отклонить")
                }
            }
        }
    }
}

@Composable
private fun BannerCard(title: String, message: String, severity: BannerSeverity) {
    val color = when (severity) {
        BannerSeverity.Info -> MaterialTheme.colorScheme.primaryContainer
        BannerSeverity.Warning -> MaterialTheme.colorScheme.tertiaryContainer
        BannerSeverity.Critical -> MaterialTheme.colorScheme.errorContainer
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(text = message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun OrderCreationDialog(
    onDismiss: () -> Unit,
    onConfirm: (OrderType, String, String?, Int, String) -> Unit
) {
    var pickup by remember { mutableStateOf("") }
    var dropOff by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var budget by remember { mutableStateOf("2000") }
    var type by remember { mutableStateOf(OrderType.TAXI) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = {
                onConfirm(type, pickup, dropOff.takeIf { it.isNotBlank() }, budget.toIntOrNull() ?: 0, note)
            }, enabled = pickup.isNotBlank() && budget.toIntOrNull() != null) {
                Text("Опубликовать")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Отмена") }
        },
        title = { Text("Новый заказ") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    RadioButton(selected = type == OrderType.TAXI, onClick = { type = OrderType.TAXI })
                    Text("Такси")
                    RadioButton(selected = type == OrderType.DELIVERY, onClick = { type = OrderType.DELIVERY })
                    Text("Доставка")
                }
                OutlinedTextField(
                    value = pickup,
                    onValueChange = { pickup = it },
                    label = { Text("Откуда") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = dropOff,
                    onValueChange = { dropOff = it },
                    label = { Text("Куда") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = budget,
                    onValueChange = { budget = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Бюджет, ₸") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Комментарий") }
                )
            }
        }
    )
}

data class Quad(val title: String, val description: String, val accent: Color, val action: String?)
