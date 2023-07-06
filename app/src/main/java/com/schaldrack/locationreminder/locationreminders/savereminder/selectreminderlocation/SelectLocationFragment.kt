package com.schaldrack.locationreminder.locationreminders.savereminder.selectreminderlocation

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Lifecycle
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PointOfInterest
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.schaldrack.locationreminder.R
import com.schaldrack.locationreminder.base.BaseFragment
import com.schaldrack.locationreminder.base.NavigationCommand
import com.schaldrack.locationreminder.databinding.FragmentSelectLocationBinding
import com.schaldrack.locationreminder.locationreminders.savereminder.SaveReminderViewModel
import com.schaldrack.locationreminder.utils.requirePermissionSnackBar
import com.schaldrack.locationreminder.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    override val _viewModel: SaveReminderViewModel by inject()
    private val binding by lazy {
        DataBindingUtil.inflate<FragmentSelectLocationBinding>(layoutInflater, R.layout.fragment_select_location, null, false)
    }

    private lateinit var map: GoogleMap
    private var poiMarker: Marker? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.viewModel = _viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        addMenuToToolbar()
        setDisplayHomeAsUpEnabled(true)
        (childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment).getMapAsync(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        binding.saveButton.setOnClickListener {
            onLocationSelected()
        }
    }

    private fun showDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.reminder_location))
            .setMessage(getString(R.string.select_poi))
            .setPositiveButton(getString(android.R.string.ok)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun onLocationSelected() {
        _viewModel.latitude.value = poiMarker?.position?.latitude
        _viewModel.longitude.value = poiMarker?.position?.longitude
        _viewModel.reminderSelectedLocationStr.value = poiMarker?.title
        _viewModel.selectedPOI.value = poiMarker?.position?.let { PointOfInterest(it, poiMarker?.id.toString(), poiMarker?.title.toString()) }
        _viewModel.navigationCommand.value = NavigationCommand.Back
    }

    private fun addMenuToToolbar() {
        requireActivity().addMenuProvider(
            object : MenuProvider {

                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.map_options, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    return when (menuItem.itemId) {
                        R.id.normal_map -> {
                            map.mapType = GoogleMap.MAP_TYPE_NORMAL
                            true
                        }

                        R.id.hybrid_map -> {
                            map.mapType = GoogleMap.MAP_TYPE_HYBRID
                            true
                        }

                        R.id.satellite_map -> {
                            map.mapType = GoogleMap.MAP_TYPE_SATELLITE
                            true
                        }

                        R.id.terrain_map -> {
                            map.mapType = GoogleMap.MAP_TYPE_TERRAIN
                            true
                        }

                        else -> false
                    }
                }
            },
            viewLifecycleOwner,
            Lifecycle.State.RESUMED,
        )
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        setMapLongClick(map)
        setPoiClick(map)
        setMapStyle(map)
        enableMyLocation()
        showDialog()
    }

    private fun setMapStyle(map: GoogleMap) {
        map.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style))
    }

    private fun setMapLongClick(map: GoogleMap) {
        map.setOnMapLongClickListener { latLng ->
            map.clear()
            val snippet = String.format(getString(R.string.lat_long_snippet), latLng.latitude, latLng.longitude)
            poiMarker = map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(getString(R.string.dropped_pin))
                    .snippet(snippet)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)),
            )

            binding.saveButton.isVisible = true
        }
    }

    private fun setPoiClick(map: GoogleMap) {
        map.setOnPoiClickListener { poi ->
            map.clear()
            poiMarker = map.addMarker(
                MarkerOptions()
                    .position(poi.latLng)
                    .title(poi.name)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)),
            )
            poiMarker?.showInfoWindow()
            binding.saveButton.isVisible = true
        }
    }

    private fun enableMyLocation() {
        when {
            checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED -> {
                map.isMyLocationEnabled = true
                fusedLocationClient.lastLocation.addOnSuccessListener { locate: Location? ->
                    if (locate != null) {
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(locate.latitude, locate.longitude), 17f))
                    }
                }
            }

            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                requirePermissionSnackBar(R.string.permission_denied_explanation)
            }

            else -> {
                activityResultLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            }
        }
    }

    private val activityResultLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        val granted = result.entries.filter { it.value }.map { it.key }
        if (granted.contains(Manifest.permission.ACCESS_FINE_LOCATION) && granted.contains(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            enableMyLocation()
        } else {
            requirePermissionSnackBar(R.string.permission_denied_explanation)
        }
    }
}
