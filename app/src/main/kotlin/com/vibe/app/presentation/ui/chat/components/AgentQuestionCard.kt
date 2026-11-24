package com.vibe.app.presentation.ui.chat.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vibe.app.feature.agent.tool.UserQuestionBridge

/**
 * Card shown while the agent is waiting on a reply from the ask_user tool.
 * Renders the agent's questions, optional quick-pick options, and a free-text
 * answer field. Black-first monochrome styling follows the app theme.
 */
@Composable
fun AgentQuestionCard(
    question: UserQuestionBridge.PendingQuestion,
    onAnswer: (String) -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by rememberSaveable(question.chatId, question.questions) {
        mutableStateOf(question.suggestedOptions.firstOrNull() ?: "")
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            question.context?.takeIf { it.isNotBlank() }?.let { ctx ->
                Text(
                    text = ctx,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            question.questions.forEach { q ->
                Text(
                    text = "• $q",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            if (question.suggestedOptions.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    question.suggestedOptions.take(3).forEach { option ->
                        OutlinedButton(
                            onClick = { text = option },
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text(
                                text = option,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "Your answer…",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                singleLine = false,
                maxLines = 4,
                shape = RoundedCornerShape(10.dp),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                OutlinedButton(
                    onClick = onSkip,
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text(text = "Skip", style = MaterialTheme.typography.labelLarge)
                }
                Button(
                    onClick = { onAnswer(text) },
                    enabled = text.isNotBlank(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Text(text = "Send", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
