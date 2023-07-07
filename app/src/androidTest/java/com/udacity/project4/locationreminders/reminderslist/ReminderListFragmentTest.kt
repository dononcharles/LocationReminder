package com.udacity.project4.locationreminders.reminderslist

import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.R
import com.udacity.project4.data.ReminderDataSource
import com.udacity.project4.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.FakeDataSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
// UI Testing
@MediumTest
class ReminderListFragmentTest : KoinTest {

    private val dataSource: ReminderDataSource by inject()
    private val rem1 = ReminderDTO("Title1", "Description1", "Location1", 1.0, 1.0)
    private val rem2 = ReminderDTO("Title2", "Description2", "Location2", 2.0, 2.0)
    private val rem3 = ReminderDTO("Title3", "Description3", "Location3", 3.0, 3.0)

    @Before
    fun init() {
        stopKoin() // stop the original app koin
        val myModule = module {
            viewModel { RemindersListViewModel(get(), get()) }
            single<ReminderDataSource> { FakeDataSource() }
        }
        startKoin {
            androidContext(getApplicationContext())
            modules(listOf(myModule))
        }
    }

    @After
    fun clear() {
        runTest {
            dataSource.deleteAllReminders()
        }
    }

    // Click on the FAB and navigate to the SaveReminderFragment.
    @Test
    fun clickFAB_navigateToSaveReminderFragment() {
        // Given - On the reminders list screen.
        val scenario = launchFragmentInContainer<ReminderListFragment>(null, R.style.Theme_LocationReminder)
        val navController = mock(NavController::class.java)
        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }

        // When - Click on the FAB.
        onView(withId(R.id.addReminderFAB)).perform(click())

        // Then - Verify that we navigate to the save reminder screen.
        verify(navController).navigate(ReminderListFragmentDirections.toSaveReminder())
    }

    // test the displayed data on the UI
    @Test
    fun remindersList_DisplayedInUi() {
        // Given - Add 3 reminders to the DB.
        runTest {
            dataSource.saveReminder(rem1)
            dataSource.saveReminder(rem2)
            dataSource.saveReminder(rem3)
        }

        // When - On the reminders list screen.
        val scenario = launchFragmentInContainer<ReminderListFragment>(null, R.style.Theme_LocationReminder)
        val navController = mock(NavController::class.java)
        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }

        // Then - Verify that the reminders are displayed on the screen.
        onView(withId(R.id.reminderssRecyclerView)).check { view, _ ->
            assertNotNull(view)
        }
    }

    // test what will happen if there's no data
    @Test
    fun noData_DisplayedInUi() = runTest {
        // Given - No reminders in the DB.
        dataSource.deleteAllReminders()

        // When - On the reminders list screen.
        val scenario = launchFragmentInContainer<ReminderListFragment>(null, R.style.Theme_LocationReminder)
        val navController = mock(NavController::class.java)
        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }

        // Then - Verify that the no data message is displayed on the screen.
        onView(withText(R.string.no_data)).check(matches(isDisplayed()))
        onView(withId(R.id.noDataTextView)).check { view, _ -> assertNotNull(view) }
    }
}
