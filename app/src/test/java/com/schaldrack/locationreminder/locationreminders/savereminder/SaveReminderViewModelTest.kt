package com.schaldrack.locationreminder.locationreminders.savereminder

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.schaldrack.locationreminder.R
import com.schaldrack.locationreminder.data.dto.Result
import com.schaldrack.locationreminder.locationreminders.MainDispatcherRule
import com.schaldrack.locationreminder.locationreminders.data.FakeDataSource
import com.schaldrack.locationreminder.locationreminders.getOrAwaitValue
import com.schaldrack.locationreminder.locationreminders.reminderslist.ReminderDataItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext.stopKoin

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest {

    private lateinit var saveReminder: SaveReminderViewModel
    private lateinit var data: FakeDataSource

    private val item1 = ReminderDataItem("Reminder1", "Description1", "Location1", 1.55, 1.777, "1")
    private val item2 = ReminderDataItem("", "Description2", "location2", 2.5263, 5.7896, "2")
    private val item3 = ReminderDataItem("Reminder3", "Description3", "", 5.789254, 7.58645, "3")

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    // For unit testing, set the primary coroutine dispatcher.
    @get:Rule
    var coroutineRule = MainDispatcherRule()

    @Before
    fun setUpViewModel() {
        stopKoin()
        data = FakeDataSource()
        saveReminder = SaveReminderViewModel(ApplicationProvider.getApplicationContext(), data)
    }

    // In this function we testing set the Live Data of reminder to be edited
    @Test
    fun editReminder_SetsLiveDataOfReminder_ToBeEdited() {
        // call Edit reminder and passing item1
        saveReminder.editReminder(item1)
        // We expect that our saveReminderViewModel is holding the data of reminder1.
        assertThat(saveReminder.reminderTitle.getOrAwaitValue(), `is`(item1.title))
        assertThat(saveReminder.reminderDescription.getOrAwaitValue(), `is`(item1.description))
        assertThat(saveReminder.reminderSelectedLocationStr.getOrAwaitValue(), `is`(item1.location))
        assertThat(saveReminder.latitude.getOrAwaitValue(), `is`(item1.latitude))
        assertThat(saveReminder.longitude.getOrAwaitValue(), `is`(item1.longitude))
        assertThat(saveReminder.reminderId.getOrAwaitValue(), `is`(item1.id))
    }

    // In this function we testing saveReminder by passing item1
    @Test
    fun saveReminder_AndAddsReminder_ToDataSource() = runTest {
        // call save reminder passing item1
        saveReminder.saveReminder(item1)
        // Call get reminder that has id 1
        val checkReminder = data.getReminder("1") as Result.Success<ReminderDataItem>
        // expect to get item1
        assertThat(checkReminder.data.title, `is`(item1.title))
        assertThat(checkReminder.data.description, `is`(item1.description))
        assertThat(checkReminder.data.location, `is`(item1.location))
        assertThat(checkReminder.data.latitude, `is`(item1.latitude))
        assertThat(checkReminder.data.longitude, `is`(item1.longitude))
        assertThat(checkReminder.data.id, `is`(item1.id))
    }

    @Test
    fun validateData_missingLocation_showSnackAndReturnFalse() {
        // Calling validateEnteredData and passing no location
        val valid = saveReminder.validateEnteredData(item3)
        // expect a SnackBar to be shown displaying err_select_location string and validate return false
        assertThat(saveReminder.showSnackBarInt.getOrAwaitValue(), `is`(R.string.err_select_location))
        assertThat(valid, `is`(false))
    }

    // test check Loading
    @Test
    fun saveReminder_AndCheckLoading() = runTest {
        // Pause dispatcher so we can verify initial values
        Dispatchers.setMain(StandardTestDispatcher())
        // item1 to be saved
        saveReminder.saveReminder(item1)
        // loading indicator is shown
        assertThat(saveReminder.showLoading.getOrAwaitValue(), `is`(true))

        // Execute pending coroutines actions.
        advanceUntilIdle()

        // loading indicator is hidden
        assertThat(saveReminder.showLoading.getOrAwaitValue(), `is`(false))
    }

    // test validateData by passing null title and we expect
    @Test
    fun validateData_missingTitle_showSnackAndReturnFalse() {
        // Calling validateEnteredData and passing no title
        val valid = saveReminder.validateEnteredData(item2)
        // expect a SnackBar to be shown displaying err_enter_title string and validate return false
        assertThat(saveReminder.showSnackBarInt.getOrAwaitValue(), `is`(R.string.err_enter_title))
        assertThat(valid, `is`(false))
    }

    // test clear Reminder live data items
    @Test
    fun onClearsReminder_ReturnNullForLiveData() {
        // Data to the variables
        saveReminder.reminderTitle.value = item1.title
        saveReminder.reminderDescription.value = item1.description
        saveReminder.reminderSelectedLocationStr.value = item1.location
        saveReminder.latitude.value = item1.latitude
        saveReminder.longitude.value = item1.longitude
        saveReminder.reminderId.value = item1.id
        // call on clear
        saveReminder.onClear()
        // expect all null
        assertThat(saveReminder.reminderTitle.getOrAwaitValue(), `is`(nullValue()))
        assertThat(saveReminder.reminderDescription.getOrAwaitValue(), `is`(nullValue()))
        assertThat(saveReminder.reminderSelectedLocationStr.getOrAwaitValue(), `is`(nullValue()))
        assertThat(saveReminder.latitude.getOrAwaitValue(), `is`(nullValue()))
        assertThat(saveReminder.longitude.getOrAwaitValue(), `is`(nullValue()))
        assertThat(saveReminder.reminderId.getOrAwaitValue(), `is`(nullValue()))
    }
}
