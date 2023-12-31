package com.udacity.project4

import android.app.Application
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.udacity.project4.data.ReminderDataSource
import com.udacity.project4.data.local.LocalDB
import com.udacity.project4.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.monitorActivity
import com.udacity.project4.utils.EspressoIdlingResource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.get

// END TO END test to black box test the app
@RunWith(AndroidJUnit4::class)
@LargeTest
class RemindersActivityTest : KoinTest { // Extended Koin Test - embed autoClose @after method to close Koin after every test

    private lateinit var repository: ReminderDataSource
    private lateinit var appContext: Application
    private var decorView: View? = null

    // An idle resource that waits for all outstanding data bindings to be completed
    private val dataBindingResource = DataBindingIdlingResource()

    @JvmField
    @Rule
    var activityRule: ActivityScenarioRule<RemindersActivity> = ActivityScenarioRule(RemindersActivity::class.java)

    @Before
    fun setUp() {
        activityRule.scenario.onActivity { activity -> decorView = activity.window.decorView }
    }

    /**
     * As we use Koin as a Service Locator Library to develop our code, we'll also use Koin to test our code.
     * at this step we will initialize Koin related code to be able to use it in out testing.
     */
    @Before
    fun init() {
        stopKoin() // stop the original app koin
        appContext = getApplicationContext()
        val myModule = module {
            viewModel { RemindersListViewModel(appContext, get() as ReminderDataSource) }
            single { SaveReminderViewModel(appContext, get() as ReminderDataSource) }
            single<ReminderDataSource> { RemindersLocalRepository(get()) }
            single { LocalDB.createRemindersDao(appContext) }
        }
        // declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }
        // Get our real repository
        repository = get()

        // clear the data to start fresh
        runBlocking {
            repository.deleteAllReminders()
        }
    }

    // In order to be garbage collected and prevent memory leaks, deregister your idle resource.
    @After
    fun unregisterResource() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().unregister(dataBindingResource)
    }

    @Before
    fun registerResource() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().register(dataBindingResource)
    }

    // This function tests adding a reminder and displaying saved toast.
    @ExperimentalCoroutinesApi
    @Test
    fun reminderSaved_ShowToast() = runBlocking {
        // GIVEN - Launch Reminder activity
        val scenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingResource.monitorActivity(scenario)

        // WHEN - We begin entering information for the reminder.
        onView(withId(R.id.addReminderFAB)).perform(click())
        onView(withId(R.id.reminderTitle)).perform(typeText("TITLE1"), closeSoftKeyboard())
        onView(withId(R.id.reminderDescription)).perform(typeText("DESC1"), closeSoftKeyboard())
        onView(withId(R.id.selectLocation)).perform(click())

        // Performing Long click on the map to select a location
        onView(withId(android.R.id.button1)).perform(click())
        onView(withId(R.id.map)).perform(longClick())
        onView(withId(R.id.save_button)).perform(click())
        onView(withId(R.id.saveReminder)).perform(click())

        // THEN - expect to have a Toast displaying reminder_saved String.
        // TODO - I can not test the toast with expresso on android > 11
        /*  onView(withText(R.string.reminder_saved))
              .inRoot(RootMatchers.withDecorView(not(decorView)))
              .check(matches(isDisplayed()))*/

        scenario.close()
    }

    // We test adding a reminder in this method without a title.
    @Test
    fun showSnack_ForToEnterTitle() {
        // GIVEN - Launch Reminders Activity
        val scenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingResource.monitorActivity(scenario)
        // WHEN - click on add reminder and try to save the reminder without giving any inputs
        onView(withId(R.id.addReminderFAB)).perform(click())
        onView(withId(R.id.saveReminder)).perform(click())
        // THEN - expect we have a SnackBar displaying err_enter_title
        onView(withId(com.google.android.material.R.id.snackbar_text)).check(matches(withText(R.string.err_enter_title)))

        scenario.close()
    }

    // We test adding a reminder using this function without entering a location.
    @Test
    fun showSnack_ForToEnterLocation() {
        // GIVEN - Launch Reminders Activity
        val scenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingResource.monitorActivity(scenario)
        // WHEN - click on add reminder and try to save the reminder without giving a location
        onView(withId(R.id.addReminderFAB)).perform(click())
        onView(withId(R.id.reminderTitle)).perform(typeText("TITLE1"), closeSoftKeyboard())
        onView(withId(R.id.saveReminder)).perform(click())
        // THEN - expect we have a SnackBar displaying err_select_location
        onView(withId(com.google.android.material.R.id.snackbar_text)).check(matches(withText(R.string.err_select_location)))

        scenario.close()
    }
}
