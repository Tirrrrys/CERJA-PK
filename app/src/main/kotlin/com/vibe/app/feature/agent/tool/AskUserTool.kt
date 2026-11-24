package com.vibe.app.feature.agent.tool

import com.vibe.app.feature.agent.AgentTool
import com.vibe.app.feature.agent.AgentToolCall
import com.vibe.app.feature.agent.AgentToolContext
import com.vibe.app.feature.agent.AgentToolDefinition
import com.vibe.app.feature.agent.AgentToolResult
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Lets the agent pause and ask the user clarifying questions mid-turn.
 *
 * Use this when the user's request is ambiguous enough that guessing could
 * waste a full build cycle: missing feature details, unclear visual style,
 * unspecified screens, or conflicting requirements. Ask BEFORE writing code
 * — never after a failed build as a substitute for reading the error.
 */
@Singleton
class AskUserTool @Inject constructor(
    private val bridge: UserQuestionBridge,
) : AgentTool {

    override val definition = AgentToolDefinition(
        name = "ask_user",
        description = "Ask the user short clarifying questions and wait for their reply. " +
            "Use BEFORE building when the request is ambiguous: missing app purpose details, " +
            "unclear screens/features, unspecified visual style, or conflicting requirements. " +
            "Ask 1-4 focused questions in one call — do not spam multiple calls. " +
            "Do NOT ask when the request is already specific or answerable from the project memo. " +
            "The user's reply arrives as the tool result; if it is TIMEOUT, proceed with your " +
            "best documented assumptions instead of asking again.",
        inputSchema = buildJsonObject {
            put("type", JsonPrimitive("object"))
            put("properties", buildJsonObject {
                put("context", stringProp(
                    "One short sentence on why you are asking (e.g. 'before I create the plan')",
                ))
                put("questions", buildJsonObject {
                    put("type", JsonPrimitive("array"))
                    put("items", stringProp("A single focused question"))
                })
                put("options", buildJsonObject {
                    put("type", JsonPrimitive("array"))
                    put("items", stringProp("Optional quick-pick answer the user can tap"))
                })
            })
            put("required", requiredFields("questions"))
        },
    )

    override suspend fun execute(
        call: AgentToolCall,
        context: AgentToolContext,
    ): AgentToolResult {
        val questions = call.arguments.jsonObject["questions"]?.let { element ->
            runCatching { element.jsonArray }.getOrNull()
        }?.mapNotNull { item ->
            runCatching { item.jsonPrimitive.content }.getOrNull()
        }?.filter { it.isNotBlank() }.orEmpty()

        if (questions.isEmpty()) {
            return call.errorResult("Missing required field: questions (array of strings)")
        }

        val askContext = call.arguments.optionalString("context")
        val options = call.arguments.jsonObject["options"]?.let { element ->
            runCatching { element.jsonArray }.getOrNull()
        }?.mapNotNull { item ->
            runCatching { item.jsonPrimitive.content }.getOrNull()
        }?.filter { it.isNotBlank() }.orEmpty()

        // AskUserTool is exempt from the coordinator's default timeout; this cap
        // below only guards against a reply that never arrives at all.
        val answer = bridge.ask(
            chatId = context.chatId,
            questions = questions.take(MAX_QUESTIONS),
            context = askContext?.take(MAX_CONTEXT_CHARS),
            suggestedOptions = options.take(MAX_OPTIONS),
            timeoutMs = REPLY_TIMEOUT_MS,
        )

        return if (answer != null) {
            call.result(buildJsonObject {
                put("answer", JsonPrimitive(answer.take(MAX_ANSWER_CHARS)))
            })
        } else {
            call.result(buildJsonObject {
                put("answer", JsonPrimitive("TIMEOUT"))
                put(
                    "note",
                    JsonPrimitive("No reply in time — continue with reasonable assumptions and state them."),
                )
            })
        }
    }

    private companion object {
        const val MAX_QUESTIONS = 4
        const val MAX_OPTIONS = 6
        const val MAX_CONTEXT_CHARS = 200
        const val MAX_ANSWER_CHARS = 4000
        const val REPLY_TIMEOUT_MS = 5 * 60_000L
    }
}
