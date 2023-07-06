package com.schaldrack.locationreminder.locationreminders.data

import com.schaldrack.locationreminder.data.ReminderDataSource
import com.schaldrack.locationreminder.data.dto.ReminderDTO
import com.schaldrack.locationreminder.data.dto.Result

// Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource(private var reminders: MutableList<ReminderDTO>? = mutableListOf()) : ReminderDataSource {

    private var returnError = false

    // Create a fake data source to act as a double to the real data source
    fun returnError(value: Boolean) {
        returnError = value
    }

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        // that confirm the correct behavior when the reminders list for some reason can't be loaded
        return if (returnError) {
            Result.Error("No reminders found")
        } else {
            Result.Success(ArrayList(reminders ?: emptyList()))
        }
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        // Save the reminder
        reminders?.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        if (returnError) {
            return Result.Error("No reminders found")
        }
        // Return the reminder with the id
        val reminder = reminders?.find {
            it.id == id
        }
        return if (reminder != null) {
            Result.Success(reminder)
        } else {
            Result.Error("No reminders found")
        }
    }

    override suspend fun deleteAllReminders() {
        // Delete all the reminders
        reminders?.clear()
    }

    override suspend fun delete(id: String) {
        val remind = reminders?.find {
            it.id == id
        }
        reminders?.remove(remind)
    }
}
