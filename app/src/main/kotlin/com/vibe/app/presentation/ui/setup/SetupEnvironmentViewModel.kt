package com.vibe.app.presentation.ui.setup

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibe.app.feature.projectinit.ProjectInitializer
import com.vibe.build.engine.internal.BuildEnvironmentInstaller
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Prepares the on-device build environment the first time the app runs so the
 * user can immediately generate an installable APK from any project:
 *
 * 1. Extracts the packaged compiler/packaging binaries (aapt2, zipalign) from
 *    the APK's native libraries into the app's private storage.
 * 2. Materializes the bundled project template.
 * 3. Verifies the debug signing key is available.
 */
@HiltViewModel
class SetupEnvironmentViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val projectInitializer: ProjectInitializer,
) : ViewModel() {

    data class EnvironmentStep(
        val titleRes: Int,
        val isDone: Boolean = false,
        val isFailed: Boolean = false,
    )

    data class EnvironmentState(
        val steps: List<EnvironmentStep> = defaultSteps(),
        val isRunning: Boolean = false,
        val isComplete: Boolean = false,
        val errorMessage: String? = null,
    ) {
        val hasFailed: Boolean get() = steps.any { it.isFailed }
    }

    private val _state = MutableStateFlow(EnvironmentState())
    val state: StateFlow<EnvironmentState> = _state.asStateFlow()

    /** True once the environment has been fully prepared at least once. */
    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    init {
        start()
    }

    fun start() {
        if (_state.value.isRunning || _state.value.isComplete) return
        _state.value = EnvironmentState(isRunning = true)
        viewModelScope.launch {
            val state = _state.value
            runCatching {
                // 1. Native build binaries
                BuildEnvironmentInstaller.prepareNativeTools(context)
                updateStep(0, done = true)
                // 2. Project template
                projectInitializer.ensureTemplateProject()
                updateStep(1, done = true)
                // 3. Debug signing key (bundled; fail fast if unusable)
                checkSigningKeyAvailable()
                updateStep(2, done = true)
            }.onFailure { e ->
                Log.e(TAG, "Build environment preparation failed", e)
                val failedIndex = _state.value.steps.indexOfFirst { !it.isDone }
                _state.value = _state.value.copy(
                    isRunning = false,
                    errorMessage = e.message ?: e.javaClass.simpleName,
                    steps = _state.value.steps.mapIndexed { i, s ->
                        if (i == failedIndex) s.copy(isFailed = true) else s
                    },
                )
                return@launch
            }
            _isReady.value = true
            _state.value = _state.value.copy(isRunning = false, isComplete = true)
        }
    }

    private fun updateStep(index: Int, done: Boolean, failed: Boolean = false) {
        _state.value = _state.value.copy(
            steps = _state.value.steps.mapIndexed { i, s ->
                if (i == index) s.copy(isDone = done, isFailed = failed) else s
            }
        )
    }

    private fun checkSigningKeyAvailable() {
        // The debug signer extracts testkey.pk8.zip / testkey.x509.pem.zip from the
        // merged assets on demand. Verify they are present so failures surface now
        // instead of during the user's first build.
        val hasKey = runCatching {
            context.assets.open("testkey.pk8.zip").use { stream ->
                stream.read() >= 0
            }
        }.getOrDefault(false)
        check(hasKey) { "Bundled debug signing key (testkey.pk8.zip) missing from assets" }
    }

    companion object {
        private const val TAG = "SetupEnvironmentVM"

        fun defaultSteps(): List<EnvironmentStep> = listOf(
            EnvironmentStep(com.vibe.app.R.string.setup_environment_step_binaries),
            EnvironmentStep(com.vibe.app.R.string.setup_environment_step_template),
            EnvironmentStep(com.vibe.app.R.string.setup_environment_step_keystore),
        )
    }
}
