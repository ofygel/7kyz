package com.freedomapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.freedomapp.ui.screens.FreedomAppRoot
import com.freedomapp.ui.theme.FreedomAppTheme
import com.freedomapp.viewmodel.FreedomAppViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: FreedomAppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FreedomAppTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                Surface(modifier = Modifier.fillMaxSize()) {
                    FreedomAppRoot(
                        state = uiState,
                        onPhoneSubmitted = viewModel::onPhoneSubmitted,
                        onCitySelected = viewModel::onCitySelected,
                        onRoleSelected = viewModel::onRoleSelected,
                        onCreateOrder = viewModel::onCreateOrder,
                        onClaimOrder = viewModel::onClaimOrder,
                        onAdvanceOrder = viewModel::onAdvanceOrder,
                        onCancelOrder = viewModel::onCancelOrder,
                        onSubmitVerification = viewModel::onSubmitVerification,
                        onModeratorDecision = viewModel::onModeratorDecision,
                        onToggleSafeMode = viewModel::onToggleSafeMode,
                        onActivateSubscription = viewModel::onActivateSubscription,
                        onRenewTrial = viewModel::onRenewTrial,
                        onConsumeEvent = viewModel::consumeEvent
                    )
                }
            }
        }
    }
}
