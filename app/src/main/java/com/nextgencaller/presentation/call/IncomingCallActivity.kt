package com.nextgencaller.presentation.call

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.nextgencaller.domain.model.CallState
import com.nextgencaller.presentation.theme.NextGenCallerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class IncomingCallActivity : ComponentActivity() {

    private val viewModel: CallViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupWindowFlags()

        setContent {
            NextGenCallerTheme {
                IncomingCallScreen(
                    viewModel = viewModel,
                    onFinish = { finish() }
                )
            }
        }
    }

    private fun setupWindowFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
    }
}

@Composable
fun IncomingCallScreen(
    viewModel: CallViewModel,
    onFinish: () -> Unit
) {
    val callState by viewModel.callState.collectAsState()

    LaunchedEffect(callState) {
        when (callState) {
            is CallState.Idle, is CallState.Ended -> {
                onFinish()
            }
            is CallState.Ongoing -> {
                onFinish()
            }
            else -> {}
        }
    }

    CallScreen(
        viewModel = viewModel,
        onNavigateBack = onFinish
    )
}