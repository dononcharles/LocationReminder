package com.schaldrack.locationreminder.locationreminders.reminderslist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.schaldrack.locationreminder.data.dto.ReminderDTO
import com.schaldrack.locationreminder.locationreminders.MainDispatcherRule
import com.schaldrack.locationreminder.locationreminders.data.FakeDataSource
import com.schaldrack.locationreminder.locationreminders.getOrAwaitValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext.stopKoin

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RemindersListViewModelTest {
    //  under test
    private lateinit var remindersListViewModel: RemindersListViewModel

    // Use a fake repository to be injected into the view-model
    private lateinit var data: FakeDataSource

    private val item1 = ReminderDTO("Reminder1", "Description1", "Location1", 1.55, 1.777, "1")
    private val item2 = ReminderDTO("Reminder2", "Description2", "location2", 2.5263, 5.7896, "2")
    private val item3 = ReminderDTO("Reminder3", "Description3", "location3", 5.789254, 7.58645, "3")

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    // For unit testing, set the primary coroutine dispatcher.
    @get:Rule
    var coroutineRule = MainDispatcherRule()

    @Before
    fun setViewModel() {
        stopKoin()
        data = FakeDataSource()
        remindersListViewModel = RemindersListViewModel(ApplicationProvider.getApplicationContext(), data)
    }

    @After
    fun clearData() = runTest {
        data.deleteAllReminders()
    }

    @Test
    fun invalidateShowNoData_ShowNoDataIsTrue() = runTest {
        // When: Empty DB and load Reminders
        data.deleteAllReminders()
        remindersListViewModel.loadReminders()

        // THEN : expect that our reminder list Live data size is 0 and show no data is true
        assertThat(remindersListViewModel.remindersList.getOrAwaitValue().size, `is`(0))
        assertThat(remindersListViewModel.showNoData.getOrAwaitValue(), `is`(true))
    }

    // We test retrieving the three reminders we're placing in this method.
    @Test
    fun loadReminders_LoadsThreeReminders() = runTest {
        // When: Empty DB and add 3 Reminders
        data.deleteAllReminders()

        data.saveReminder(item1)
        data.saveReminder(item2)
        data.saveReminder(item3)

        // try to load Reminders
        remindersListViewModel.loadReminders()

        // THEN : expect to have only 3 reminders in remindersList and showNoData is false cause we have data
        assertThat(remindersListViewModel.remindersList.getOrAwaitValue().size, `is`(3))
        assertThat(remindersListViewModel.showNoData.getOrAwaitValue(), `is`(false))
    }

    // Here, we are testing checkLoading in this test.
    @Test
    fun loadReminders_AndCheckLoading() = runTest {
        // Pause dispatcher so we can verify initial values.
        Dispatchers.setMain(StandardTestDispatcher())

        //  Only 1 Reminder
        data.deleteAllReminders()
        data.saveReminder(item1)
        // load Reminders
        remindersListViewModel.loadReminders()

        // The loading indicator is displayed, then it is hidden after we are done.
        assertThat(remindersListViewModel.showLoading.getOrAwaitValue(), `is`(true))

        // Execute pending coroutines actions.
        advanceUntilIdle()

        // Then loading indicator is hidden
        assertThat(remindersListViewModel.showLoading.getOrAwaitValue(), `is`(false))
    }

    // testing showing an Error
    @Test
    fun loadReminders_ShouldReturnError() = runTest {
        // give : set should return error to "true
        data.returnError(true)
        // when : we load Reminders
        remindersListViewModel.loadReminders()
        // then : We get showSnackBar in the view model giving us "not found"
        assertThat(remindersListViewModel.showSnackBar.getOrAwaitValue(), `is`("No reminders found"))
    }
}
