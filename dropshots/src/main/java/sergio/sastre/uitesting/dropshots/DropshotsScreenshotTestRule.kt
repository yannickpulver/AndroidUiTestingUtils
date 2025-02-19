package sergio.sastre.uitesting.dropshots

import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.drawToBitmap
import androidx.test.core.app.ActivityScenario
import com.dropbox.dropshots.Dropshots
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runners.model.Statement
import sergio.sastre.uitesting.utils.BitmapCaptureMethod
import sergio.sastre.uitesting.utils.LibraryConfig
import sergio.sastre.uitesting.utils.ScreenshotConfig
import sergio.sastre.uitesting.utils.ScreenshotTestRule
import sergio.sastre.uitesting.utils.activityscenario.ActivityScenarioConfigurator
import sergio.sastre.uitesting.utils.utils.drawToBitmapWithElevation
import sergio.sastre.uitesting.utils.utils.waitForActivity

class DropshotsScreenshotTestRule(
    private val screenshotConfig: ScreenshotConfig = ScreenshotConfig(),
) : ScreenshotTestRule, TestWatcher() {

    private val activityScenario: ActivityScenario<out ComponentActivity> by lazy {
        ActivityScenarioConfigurator.ForComposable()
            .setLocale(screenshotConfig.locale)
            .setInitialOrientation(screenshotConfig.orientation)
            .setUiMode(screenshotConfig.uiMode)
            .setFontSize(screenshotConfig.fontScale)
            .launchConfiguredActivity()
    }

    private val dropshotsRule: Dropshots by lazy {
        Dropshots(resultValidator = dropshotsConfig.resultValidator)
    }

    private var dropshotsConfig: DropshotsConfig = DropshotsConfig()

    override fun snapshot(composable: @Composable () -> Unit) {
        takeSnapshot(null, composable)
    }

    override fun snapshot(name: String?, composable: @Composable () -> Unit) {
        takeSnapshot(name, composable)
    }

    override fun apply(base: Statement, description: Description): Statement =
        dropshotsRule.apply(base, description)

    private fun takeSnapshot(name: String?, composable: @Composable () -> Unit) {
        val activity =
            activityScenario
                .onActivity {
                    it.setContent {
                        composable.invoke()
                    }
                }.waitForActivity()

        val existingComposeView =
            activity.window.decorView
                .findViewById<ViewGroup>(android.R.id.content)
                .getChildAt(0) as ComposeView

        when(val bitmapCaptureMethod = dropshotsConfig.bitmapCaptureMethod){
            is BitmapCaptureMethod.Canvas ->
                takeSnapshotWithCanvas(bitmapCaptureMethod.config, existingComposeView, name)
            is BitmapCaptureMethod.PixelCopy ->
                takeSnapshotWithPixelCopy(bitmapCaptureMethod.config, existingComposeView, name)
            null -> takeSnapshotOfView(existingComposeView, name)
        }
    }

    private fun takeSnapshotOfView(view: View, name: String?) {
        dropshotsRule.assertSnapshot(
            view = view,
            name = name.orEmpty(),
        )
    }

    private fun takeSnapshotWithPixelCopy(bitmapConfig: Bitmap.Config, view: View, name: String?) {
        dropshotsRule.assertSnapshot(
            bitmap = view.drawToBitmapWithElevation(config = bitmapConfig),
            name = name.orEmpty(),
        )
    }

    private fun takeSnapshotWithCanvas(bitmapConfig: Bitmap.Config, view: View, name: String?) {
        dropshotsRule.assertSnapshot(
            bitmap = view.drawToBitmap(config = bitmapConfig),
            name = name.orEmpty(),
        )
    }

    override fun configure(config: LibraryConfig): ScreenshotTestRule = apply {
        if (config is DropshotsConfig) {
            dropshotsConfig = config
        }
    }

    override fun finished(description: Description?) {
        super.finished(description)
        activityScenario.close()
    }
}
