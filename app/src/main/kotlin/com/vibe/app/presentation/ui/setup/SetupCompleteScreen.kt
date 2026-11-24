package com.vibe.app.presentation.ui.setup

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibe.app.R
import com.vibe.app.presentation.common.PrimaryLongButton
import com.vibe.app.presentation.common.Route
import com.vibe.app.presentation.icons.Done

@Composable
fun SetupCompleteScreen(
    modifier: Modifier = Modifier,
    onNavigate: (route: String) -> Unit,
    onBackAction: () -> Unit,
    environmentViewModel: SetupEnvironmentViewModel = hiltViewModel()
) {
    val configuration = LocalWindowInfo.current
    val screenWidth = with(LocalDensity.current) { configuration.containerSize.width.toDp() }
    val environmentState by environmentViewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { SetupAppBar(onBackAction) }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            SetupCompleteText()
            SetupCompleteLogo(
                Modifier
                    .widthIn(min = screenWidth)
                    .heightIn(min = screenWidth)
                    .padding(screenWidth * 0.1f)
            )
            BuildEnvironmentChecklist(environmentState)
            Spacer(modifier = Modifier.weight(1f))
            PrimaryLongButton(
                onClick = { onNavigate(Route.CHAT_LIST) },
                text = stringResource(R.string.done)
            )
        }
    }
}

@Composable
private fun BuildEnvironmentChecklist(
    state: SetupEnvironmentViewModel.EnvironmentState,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Text(
            text = stringResource(R.string.setup_environment_title),
            style = MaterialTheme.typography.labelLarge
        )
        Spacer(modifier = Modifier.padding(2.dp))
        Text(
            text = stringResource(R.string.setup_environment_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.padding(6.dp))
        state.steps.forEach { step ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(22.dp), contentAlignment = Alignment.Center) {
                    when {
                        step.isDone -> Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        step.isFailed -> Text(
                            text = "!",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelLarge
                        )
                        state.isRunning -> CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
                Spacer(modifier = Modifier.size(10.dp))
                Text(
                    text = stringResource(step.titleRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (step.isFailed) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
            Spacer(modifier = Modifier.padding(2.dp))
        }
        when {
            state.isComplete -> Text(
                text = stringResource(R.string.setup_environment_done),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 6.dp)
            )
            state.hasFailed -> Text(
                text = stringResource(R.string.setup_environment_failed, state.errorMessage ?: ""),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

@Preview
@Composable
private fun SetupCompleteText(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        Text(
            modifier = Modifier
                .padding(4.dp)
                .semantics { heading() },
            text = stringResource(R.string.setup_complete),
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            modifier = Modifier.padding(4.dp),
            text = stringResource(R.string.setup_complete_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SetupCompleteLogo(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Image(
            imageVector = Done,
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
        )
    }
}
