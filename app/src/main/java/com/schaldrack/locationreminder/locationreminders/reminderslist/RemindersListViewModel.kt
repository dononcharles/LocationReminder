package com.schaldrack.locationreminder.locationreminders.reminderslist

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.schaldrack.locationreminder.base.BaseViewModel
import com.schaldrack.locationreminder.data.ReminderDataSource
import com.schaldrack.locationreminder.data.dto.ReminderDTO
import com.schaldrack.locationreminder.data.dto.Result
import com.schaldrack.locationreminder.utils.DataStoreManager
import com.schaldrack.locationreminder.utils.DataStoreManager.Companion.PREF_KEY_IS_USER_CONNECTED
import kotlinx.coroutines.launch
import org.koin.core.component.inject

class RemindersListViewModel(val app: Application, private val dataSource: ReminderDataSource) : BaseViewModel(app) {
    // list that holds the reminder data to be displayed on the UI
    val remindersList = MutableLiveData<List<ReminderDataItem>>()

    private val dataStoreManager: DataStoreManager by inject()

    /**
     * Get all the reminders from the DataSource and add them to the remindersList to be shown on the UI,
     * or show error if any
     */
    fun loadReminders() {
        showLoading.value = true
        viewModelScope.launch {
            // interacting with the dataSource has to be through a coroutine
            val result = dataSource.getReminders()
            showLoading.postValue(false)
            when (result) {
                is Result.Success<*> -> {
                    val dataList = ArrayList<ReminderDataItem>()
                    dataList.addAll(
                        (result.data as List<ReminderDTO>).map { reminder ->
                            // map the reminder data from the DB to the be ready to be displayed on the UI
                            ReminderDataItem(
                                reminder.title,
                                reminder.description,
                                reminder.location,
                                reminder.latitude,
                                reminder.longitude,
                                reminder.id,
                            )
                        },
                    )
                    remindersList.value = dataList
                }

                is Result.Error ->
                    showSnackBar.value = result.message
            }

            // check if no data has to be shown
            invalidateShowNoData()
        }
    }

    /**
     * Inform the user that there's not any data if the remindersList is empty
     */
    private fun invalidateShowNoData() {
        showNoData.value = remindersList.value == null || remindersList.value!!.isEmpty()
    }

    fun logout() {
        viewModelScope.launch {
            dataStoreManager.saveBooleanDataStore(PREF_KEY_IS_USER_CONNECTED, false)
        }
    }
}
