package com.schaldrack.locationreminder.di

import android.app.Application
import com.schaldrack.locationreminder.data.ReminderDataSource
import com.schaldrack.locationreminder.data.local.LocalDB
import com.schaldrack.locationreminder.data.local.RemindersLocalRepository
import com.schaldrack.locationreminder.locationreminders.reminderslist.RemindersListViewModel
import com.schaldrack.locationreminder.locationreminders.savereminder.SaveReminderViewModel
import com.schaldrack.locationreminder.utils.DataStoreManager
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
