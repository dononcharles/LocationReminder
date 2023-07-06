package com.schaldrack.locationreminder.locationreminders.geofence

import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.schaldrack.locationreminder.data.ReminderDataSource
import com.schaldrack.locationreminder.data.dto.ReminderDTO
import com.schaldrack.locationreminder.locationreminders.reminderslist.ReminderDataItem
import com.schaldrack.locationreminder.utils.sendNotification
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * @author Komi Donon
 * @since 7/7/2023
 */
class GeofenceTransitionsWorker(private val intent: Intent, private val context: Context, params: WorkerParameters, private val dispatcher: CoroutineDispatcher = Dispatchers.IO) : CoroutineWorker(context, params), KoinComponent {

    override suspend fun doWork(): Result {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)

        if (geofencingEvent?.hasError() == true) {
            return Result.failure()
        }

        if (geofencingEvent?.geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            geofencingEvent.triggeringGeofences?.let { sendNotification(it, context = context) }
        }

        return Result.success()
    }

    private suspend fun sendNotification(triggeringGeofences: List<Geofence>, context: Context) {
        triggeringGeofences.forEach {
            val requestId = it.requestId

            // Get the local repository instance
            val remindersLocalRepository: ReminderDataSource by inject()
            //  Interaction to the repository has to be through a coroutine scope
            withContext(dispatcher) {
                // get the reminder with the request id
                val result = remindersLocalRepository.getReminder(requestId) as com.schaldrack.locationreminder.data.dto.Result.Success<ReminderDTO>
                val reminderDTO = result.data
                // send a notification to the user with the reminder details
                sendNotification(
                    context = context,
                    ReminderDataItem(
                        reminderDTO.title,
                        reminderDTO.description,
                        reminderDTO.location,
                        reminderDTO.latitude,
                        reminderDTO.longitude,
                        reminderDTO.id,
                    ),
                )
            }
        }
    }

    companion object {
        const val WORK_NAME = "GeofenceTransitionsWorker"
    }
}
