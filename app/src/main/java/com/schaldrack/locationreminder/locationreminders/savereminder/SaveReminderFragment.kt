package com.schaldrack.locationreminder.locationreminders.savereminder

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity.RESULT_OK
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.Geofence.NEVER_EXPIRE
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.google.android.material.snackbar.Snackbar
import com.schaldrack.locationreminder.R
import com.schaldrack.locationreminder.base.BaseFragment
import com.schaldrack.locationreminder.base.NavigationCommand
import com.schaldrack.locationreminder.databinding.FragmentSaveReminderBinding
import com.schaldrack.locationreminder.locationreminders.geofence.GeofenceBroadcastReceiver
import com.schaldrack.locationreminder.locationreminders.reminderslist.ReminderDataItem
import com.schaldrack.locationreminder.utils.requirePermissionSnackBar
import com.schaldrack.locationreminder.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import java.util.UUID

class SaveReminderFragment : BaseFragment() {

    // Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private val binding by lazy {
        DataBindingUtil.inflate<FragmentSaveReminderBinding>(
            layoutInflater,
            R.layout.fragment_save_reminder,
            null,
            false,
        )
    }

    private val runningQOrLater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    private val geoClient: GeofencingClient by lazy { LocationServices.getGeofencingClient(requireContext()) }
    private lateinit var reminderDataItem: ReminderDataItem

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(requireActivity(), GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(requireContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setDisplayHomeAsUpEnabled(true)
        binding.viewModel = _viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        binding.selectLocation.setOnClickListener {
            // Navigate to another fragment to get the user location
            val directions = SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment()
            _viewModel.navigationCommand.value = NavigationCommand.To(directions)
        }

        binding.saveReminder.setOnClickListener {
            val title = _viewModel.reminderTitle.value
            val description = _viewModel.reminderDescription.value
            val location = _viewModel.reminderSelectedLocationStr.value
            val latitude = _viewModel.latitude.value
            val longitude = _viewModel.longitude.value

            val id = if (_viewModel.reminderId.value != null) {
                _viewModel.reminderId.value.toString()
            } else {
                UUID.randomUUID().toString()
            }

            reminderDataItem = ReminderDataItem(title, description, location, latitude, longitude, id)

            if (_viewModel.validateEnteredData(reminderDataItem)) {
                checkPermissions()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _viewModel.onClear()
    }

    private fun checkPermissions() {
        if (foregroundPermission() && backgroundPermission()) {
            checkLocationSettings()
        } else {
            if (foregroundPermission() && backgroundPermission()) {
                checkLocationSettings()
            }
            if (!backgroundPermission()) {
                requestBackgroundPermission()
            }
            if (!foregroundPermission()) {
                requestForegroundPermissions()
            }
        }
    }

    private fun requestForegroundPermissions() {
        when {
            foregroundPermission() -> {
                return
            }

            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                requirePermissionSnackBar(R.string.select_poi)
            }

            else -> {
                launcherPermissions.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            }
        }
    }

    private fun foregroundPermission(): Boolean {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    @TargetApi(Build.VERSION_CODES.Q)
    private fun backgroundPermission(): Boolean {
        return if (runningQOrLater) {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    @TargetApi(Build.VERSION_CODES.Q)
    private fun requestBackgroundPermission() {
        if (backgroundPermission()) {
            checkLocationSettings()
            return
        }
        if (runningQOrLater) {
            launcherPermissions.launch(arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION))
        } else {
            return
        }
    }

    private fun checkLocationSettings(resolve: Boolean = true) {
        val locationRequest = LocationRequest.Builder(Long.MAX_VALUE).apply {
            setPriority(Priority.PRIORITY_LOW_POWER)
        }.build()

        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)

        val settingsClient = LocationServices.getSettingsClient(requireActivity())
        val locationSettingsResponseTask =
            settingsClient.checkLocationSettings(builder.build())

        locationSettingsResponseTask.addOnCompleteListener {
            if (it.isSuccessful) {
                geoReminder()
            }
        }
        locationSettingsResponseTask.addOnFailureListener { exp ->
            if (exp is ResolvableApiException && resolve) {
                try {
                    val request = IntentSenderRequest.Builder(exp.resolution).build()
                    launcherLocation.launch(request)
                } catch (error: IntentSender.SendIntentException) {
                    Log.d("TAG", "Error getting location settings resolution: ${error.message}")
                }
            } else {
                Snackbar.make(binding.saveReminder, R.string.location_required_error, Snackbar.LENGTH_INDEFINITE)
                    .setAction(android.R.string.ok) {
                        checkLocationSettings()
                    }.show()
            }
        }
    }

    private fun geoReminder() {
        val geofence = Geofence.Builder()
            .setRequestId(reminderDataItem.id)
            .setCircularRegion(
                reminderDataItem.latitude ?: 0.0,
                reminderDataItem.longitude ?: 0.0,
                _viewModel.remindingLocationRange.value?.toFloat()!!,
            )
            .setExpirationDuration(NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        geoClient.addGeofences(geofencingRequest, geofencePendingIntent).run {
            addOnSuccessListener {
                Log.e("TAG", geofence.requestId)
                _viewModel.validateAndSaveReminder(reminderDataItem)
                /*_viewModel.navigationCommand.value =
                    NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToReminderListFragment())*/
            }
            addOnFailureListener {
                Toast.makeText(requireContext(), R.string.geofences_not_added, Toast.LENGTH_SHORT).show()
                if ((it.message != null)) {
                    Log.w("TAG", it.message.toString())
                }
            }
        }
    }

    private val launcherLocation = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { res ->
        if (res.resultCode == RESULT_OK) {
            geoReminder()
        }
    }

    private val launcherPermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.all { res -> res.value }) {
            checkPermissions()
        } else {
            requirePermissionSnackBar(R.string.select_poi)
        }
    }
}
