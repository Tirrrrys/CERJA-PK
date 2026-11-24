package com.vibe.app.feature.agent.tool

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withTimeout

/**
 * Bridge between the on-device agent loop and the chat UI.
 *
 * When the agent calls the `ask_user` tool, it suspends on [ask] while the
 * question is exposed through [pending]. The UI collects [pending], renders
 * the question card, and delivers the user's reply via [submitAnswer], which
 * completes the suspended tool call.
 *
 * One pending question per chat at a time; concurrent asks from another
 * platform in the same chat are rejected so replies stay unambiguous.
 */
@Singleton
class UserQuestionBridge @Inject constructor() {

    data class PendingQuestion(
        val chatId: Int,
        val questions: List<String>,
        val context: String?,
        val suggestedOptions: List<String> = emptyList(),
    )

    private val _pending = MutableStateFlow<PendingQuestion?>(null)
    val pending: StateFlow<PendingQuestion?> = _pending.asStateFlow()

    private var deferred: CompletableDeferred<String>? = null

    /**
     * Suspends until the user answers, the question is cancelled, or [timeoutMs]
     * elapses. Returns the user's answer text, or null on timeout/cancellation.
     */
    suspend fun ask(
        chatId: Int,
        questions: List<String>,
        context: String?,
        suggestedOptions: List<String>,
        timeoutMs: Long,
    ): String? {
        if (_pending.value != null || deferred?.isActive == true) {
            return null
        }
        val waiting = CompletableDeferred<String>()
        deferred = waiting
        _pending.value = PendingQuestion(
            chatId = chatId,
            questions = questions,
            context = context,
            suggestedOptions = suggestedOptions,
        )
        return try {
            withTimeout(timeoutMs) { waiting.await() }
        } catch (_: Exception) {
            null
        } finally {
            _pending.value = null
            deferred = null
        }
    }

    /** Delivers the user's answer to the waiting agent. Returns false if nothing is waiting. */
    fun submitAnswer(chatId: Int, answer: String): Boolean {
        val current = _pending.value ?: return false
        if (current.chatId != chatId) return false
        return deferred?.complete(answer) ?: false
    }

    /** Dismisses any pending question (e.g. user skipped it); the agent sees a timeout-style result. */
    fun cancel(chatId: Int) {
        val current = _pending.value ?: return
        if (current.chatId != chatId) return
        deferred?.cancel()
    }
}
