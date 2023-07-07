package com.udacity.project4.di

import android.app.Application
import com.udacity.project4.data.ReminderDataSource
import com.udacity.project4.data.local.LocalDB
import com.udacity.project4.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.DataStoreManager
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * @author Komi Donon
 * @since 7/3/2023
 */

/**
 * use Koin Library as a service locator
 */
val myModule = module {
    single { LocalDB.createRemindersDao(androidContext()) }

    single<ReminderDataSource> { RemindersLocalRepository(get()) }

    single { DataStoreManager(androidContext()) }

    viewModel { RemindersListViewModel(get() as Application, get() as ReminderDataSource) }

    // This view model is declared singleton to be used across multiple fragments
    single { SaveReminderViewModel(get() as Application, get()) }
}
