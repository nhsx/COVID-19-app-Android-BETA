package uk.nhs.nhsx.sonar.android.app

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.joda.time.DateTime
import org.joda.time.DateTimeZone.UTC
import uk.nhs.nhsx.sonar.android.app.status.ExposedState
import uk.nhs.nhsx.sonar.android.app.status.AtRiskRobot
import uk.nhs.nhsx.sonar.android.app.status.DefaultState
import uk.nhs.nhsx.sonar.android.app.status.IsolateRobot
import uk.nhs.nhsx.sonar.android.app.status.OkRobot
import uk.nhs.nhsx.sonar.android.app.status.SymptomaticState
import uk.nhs.nhsx.sonar.android.app.status.StatusFooterRobot
import uk.nhs.nhsx.sonar.android.app.status.Symptom.TEMPERATURE
import uk.nhs.nhsx.sonar.android.app.testhelpers.TestApplicationContext
import uk.nhs.nhsx.sonar.android.app.testhelpers.checkViewHasText
import uk.nhs.nhsx.sonar.android.app.util.nonEmptySetOf

class MainActivityTest(private val testAppContext: TestApplicationContext) {

    private val okRobot = OkRobot()
    private val atRiskRobot = AtRiskRobot()
    private val isolateRobot = IsolateRobot()
    private val statusFooterRobot = StatusFooterRobot()

    fun testUnsupportedDevice() {
        testAppContext.simulateUnsupportedDevice()

        startMainActivity()

        checkViewHasText(R.id.edgeCaseTitle, R.string.device_not_supported_title)
    }

    fun testTabletNotSupported() {
        testAppContext.simulateTablet()

        startMainActivity()

        checkViewHasText(R.id.edgeCaseTitle, R.string.tablet_support_title)
    }

    fun testLaunchWhenOnboardingIsFinishedButNotRegistered() {
        testAppContext.setFinishedOnboarding()

        startMainActivity()

        okRobot.checkActivityIsDisplayed()
    }

    fun testLaunchWhenStateIsDefault() {
        testAppContext.setFullValidUser(DefaultState)

        startMainActivity()

        okRobot.checkActivityIsDisplayed()
        statusFooterRobot.checkFooterIsDisplayed()
    }

    fun testLaunchWhenStateIsExposed() {
        val exposedState = ExposedState(DateTime.now(UTC), DateTime.now(UTC).plusDays(1))

        testAppContext.setFullValidUser(exposedState)
        startMainActivity()

        atRiskRobot.checkActivityIsDisplayed()
        statusFooterRobot.checkFooterIsDisplayed()
    }

    fun testLaunchWhenStateIsSymptomatic() {
        val date = DateTime.now(UTC).plusDays(1)
        val symptomaticState = SymptomaticState(date, date, nonEmptySetOf(TEMPERATURE))

        testAppContext.setFullValidUser(symptomaticState)
        startMainActivity()

        isolateRobot.checkActivityIsDisplayed()
        statusFooterRobot.checkFooterIsDisplayed()
    }

    private fun startMainActivity() {
        onView(withId(R.id.start_main_activity)).perform(click())
    }
}
