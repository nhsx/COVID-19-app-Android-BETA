/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app

import android.content.Context
import android.content.Intent
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verifyAll
import org.joda.time.DateTime
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.ble.BluetoothService
import uk.nhs.nhsx.sonar.android.app.notifications.Reminders
import uk.nhs.nhsx.sonar.android.app.registration.SonarIdProvider
import uk.nhs.nhsx.sonar.android.app.status.EmberState
import uk.nhs.nhsx.sonar.android.app.status.RedState
import uk.nhs.nhsx.sonar.android.app.status.StateStorage
import uk.nhs.nhsx.sonar.android.app.status.Symptom
import uk.nhs.nhsx.sonar.android.app.util.nonEmptySetOf

class BootCompletedReceiverTest {

    private val sonarIdProvider = mockk<SonarIdProvider>()
    private val stateStorage = mockk<StateStorage>()
    private val reminders = mockk<Reminders>()

    private val receiver = BootCompletedReceiver().also {
        it.sonarIdProvider = sonarIdProvider
        it.stateStorage = stateStorage
        it.reminders = reminders
    }

    @Before
    fun setUp() {
        mockkObject(BluetoothService)
    }

    @Test
    fun `onReceive - with unknown intent action`() {
        val intent = TestIntent("SOME_OTHER_ACTION")

        receiver.handle(mockk(), intent)

        verifyAll {
            sonarIdProvider wasNot Called
            BluetoothService wasNot Called
        }
    }

    @Test
    fun `onReceive - with sonarId, with not expired red state`() {
        val until = DateTime.now().plusDays(1)
        val context = mockk<Context>()

        every { sonarIdProvider.hasProperSonarId() } returns true
        every { stateStorage.get() } returns RedState(until, nonEmptySetOf(Symptom.COUGH))
        every { reminders.scheduleCheckInReminder(any()) } returns Unit
        every { BluetoothService.start(any()) } returns Unit

        receiver.handle(context, TestIntent(Intent.ACTION_BOOT_COMPLETED))

        verifyAll {
            BluetoothService.start(context)
            reminders.scheduleCheckInReminder(until)
        }
    }

    @Test
    fun `onReceive - with sonarId, with expired red state`() {
        val until = DateTime.now().minusDays(1)
        val context = mockk<Context>()

        every { sonarIdProvider.hasProperSonarId() } returns true
        every { stateStorage.get() } returns RedState(until, nonEmptySetOf(Symptom.COUGH))
        every { BluetoothService.start(any()) } returns Unit

        receiver.handle(context, TestIntent(Intent.ACTION_BOOT_COMPLETED))

        verifyAll {
            BluetoothService.start(context)
            reminders wasNot Called
        }
    }

    @Test
    fun `onReceive - without sonarId`() {
        every { sonarIdProvider.hasProperSonarId() } returns false
        every { stateStorage.get() } returns RedState(DateTime.now(), nonEmptySetOf(Symptom.COUGH))
        every { reminders.scheduleCheckInReminder(any()) } returns Unit

        receiver.handle(mockk(), TestIntent(Intent.ACTION_BOOT_COMPLETED))

        verifyAll {
            BluetoothService wasNot Called
        }
    }

    @Test
    fun `onReceive - without red state`() {
        every { stateStorage.get() } returns EmberState(DateTime.now())
        every { sonarIdProvider.hasProperSonarId() } returns true
        every { BluetoothService.start(any()) } returns Unit

        receiver.handle(mockk(), TestIntent(Intent.ACTION_BOOT_COMPLETED))

        verifyAll {
            reminders wasNot Called
        }
    }

    class TestIntent(private val actionName: String) : Intent() {
        override fun getAction() = actionName
    }
}
