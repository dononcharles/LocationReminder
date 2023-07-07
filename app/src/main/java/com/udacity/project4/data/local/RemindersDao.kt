package com.udacity.project4.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.udacity.project4.data.dto.ReminderDTO

/**
 * Data Access Object for the reminders table.
 */
@Dao
interface RemindersDao {

    /**
     * @return all reminders.
     */
    @Query("SELECT * FROM reminders")
    suspend fun getReminders(): List<ReminderDTO>

    /**
     * @param reminderId the id of the reminder
     * @return the reminder object with the reminderId
     */
    @Query("SELECT * FROM reminders where entry_id = :reminderId")
    suspend fun getReminderById(reminderId: String): ReminderDTO?

    /**
     * Insert a reminder in the database. If the reminder already exists, replace it.
     * @param reminder the reminder to be inserted.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveReminder(reminder: ReminderDTO)

    /**
     * Delete all reminders.
     */
    @Query("DELETE FROM reminders")
    suspend fun deleteAllReminders()

    /**
     * Delete a reminder by id.
     * @return the number of reminders deleted. This should always be 1.
     */
    @Query("DELETE FROM reminders WHERE entry_id = :reminderId")
    suspend fun deleteById(reminderId: String): Int
}