package by.mazokaleh.skarnik.baselineprofile

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Cold start to the History tab, without any ahead-of-time compilation and with the Baseline Profile:
 * `./gradlew :baselineprofile:connectedBenchmarkReleaseAndroidTest
 * -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.enabledRules=Macrobenchmark`.
 */
@RunWith(Parameterized::class)
class StartupBenchmark(private val compilation: Compilation) {
    @get:Rule
    val rule = MacrobenchmarkRule()

    /** The first launch after install unpacks the search index and asks for consent; neither is a usual start. */
    @Before
    fun firstLaunch() = prepareFirstLaunch(targetAppId)

    @Test
    fun coldStart() = rule.measureRepeated(
        packageName = targetAppId,
        metrics = listOf(StartupTimingMetric()),
        compilationMode = compilation.mode,
        startupMode = StartupMode.COLD,
        iterations = ITERATIONS
    ) {
        startToHistory()
    }

    enum class Compilation(val mode: CompilationMode) {
        None(CompilationMode.None()),
        BaselineProfile(CompilationMode.Partial(BaselineProfileMode.Require))
    }

    companion object {
        private const val ITERATIONS = 10

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun compilations() = Compilation.entries
    }
}
