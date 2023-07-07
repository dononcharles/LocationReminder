package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.data.dto.ReminderDTO
import com.udacity.project4.data.dto.Result
import com.udacity.project4.data.local.RemindersDatabase
import com.udacity.project4.data.local.RemindersLocalRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
// Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {

    private lateinit var repository: RemindersLocalRepository
    private lateinit var database: RemindersDatabase

    private val rem1 = ReminderDTO("Title1", "Description1", "Location1", 1.0, 1.0)
    private val rem2 = ReminderDTO("Title2", "Description2", "Location2", 2.0, 2.0)
    private val rem3 = ReminderDTO("Title3", "Description3", "Location3", 3.0, 3.0)

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        // using an in-memory database for testing, since it doesn't survive killing the process
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), RemindersDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        repository = RemindersLocalRepository(database.reminderDao(), Dispatchers.Main)
    }

    @After
    fun cleanUp() {
        database.close()
    }

    // Save a reminder and get it by ID
    @Test
    fun saveReminder_getReminderById() = runTest {
        // GIVEN - Insert a reminder.
        repository.saveReminder(rem1)

        // WHEN - Get the reminder by id from the database.
        val result = repository.getReminder(rem1.id)

        // THEN - The loaded data contains the expected values.
        assert(result is Result.Success)
        result as Result.Success

        assertThat(result.data.title, `is`(rem1.title))
        assertThat(result.data.description, `is`(rem1.description))
        assertThat(result.data.location, `is`(rem1.location))
        assertThat(result.data.latitude, `is`(rem1.latitude))
        assertThat(result.data.longitude, `is`(rem1.longitude))
    }

    // Save all reminders and get them by all
    @Test
    fun saveAllReminders_getAllReminders() = runTest {
        // GIVEN - Insert a reminder.
        repository.saveReminder(rem1)
        repository.saveReminder(rem2)
        repository.saveReminder(rem3)

        // WHEN - Get the reminder by id from the database.
        val result = repository.getReminders()

        // THEN - The loaded data contains the expected values.
        assert(result is Result.Success)
        result as Result.Success

        assertThat(result.data.size, `is`(3))
        assertThat(result.data[0].title, `is`(rem1.title))
        assertThat(result.data[1].title, `is`(rem2.title))
        assertThat(result.data[2].title, `is`(rem3.title))
    }

    // Save a reminder and delete it
    @Test
    fun saveReminder_deleteReminder() = runTest {
        // GIVEN - Insert a reminder.
        repository.saveReminder(rem1)

        // WHEN - Delete the reminder by id from the database.
        repository.delete(rem1.id)

        // THEN - The loaded data contains the expected values.
        val result = repository.getReminder(rem1.id)
        assert(result is Result.Error)
    }

    // Save a reminder and delete all
    @Test
    fun saveReminder_deleteAllReminders() = runTest {
        // GIVEN - Insert a reminder.
        repository.saveReminder(rem1)
        repository.saveReminder(rem2)
        repository.saveReminder(rem3)

        // WHEN - Delete the reminder by id from the database.
        repository.deleteAllReminders()

        // THEN - The loaded data contains the expected values.
        val result = repository.getReminders()
        assert(result is Result.Success)
        result as Result.Success
        assertThat(result.data.size, `is`(0))
    }

    // Save a reminder and update it
    @Test
    fun saveReminder_updateReminder() = runTest {
        // GIVEN - Insert a reminder.
        repository.saveReminder(rem1)

        // WHEN - Update the reminder by id from the database.
        val updatedReminder = ReminderDTO("Title1", "Description1", "Location1", 4.0, 4.0, rem1.id)
        repository.saveReminder(updatedReminder)

        // THEN - The loaded data contains the expected values.
        val result = repository.getReminder(rem1.id)
        assert(result is Result.Success)
        result as Result.Success
        assertThat(result.data.latitude, `is`(4.0))
        assertThat(result.data.longitude, `is`(4.0))
    }

    // remove item and try to get it
    @Test
    fun deleteReminder_getReminderById() = runTest {
        // GIVEN - Insert a reminder.
        repository.saveReminder(rem1)

        // WHEN - Delete the reminder by id from the database.
        repository.delete(rem1.id)

        // THEN - The loaded data contains the expected values.
        val result = repository.getReminder(rem1.id)
        assert(result is Result.Error)
    }
}
