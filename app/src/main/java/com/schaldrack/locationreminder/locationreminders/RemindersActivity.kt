package com.schaldrack.locationreminder.locationreminders

import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import com.schaldrack.locationreminder.R
import com.schaldrack.locationreminder.databinding.ActivityRemindersBinding
import com.schaldrack.locationreminder.locationreminders.reminderslist.ReminderDataItem
import com.schaldrack.locationreminder.locationreminders.reminderslist.ReminderListFragmentDirections
import com.schaldrack.locationreminder.locationreminders.savereminder.SaveReminderViewModel
import org.koin.android.ext.android.inject

/**
 * The RemindersActivity that holds the reminders fragments
 */
class RemindersActivity : AppCompatActivity() {

    private val binding by lazy { ActivityRemindersBinding.inflate(layoutInflater) }

    private val navController by lazy { (supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment).navController }

    private lateinit var appBarConfiguration: AppBarConfiguration

    val viewModel: SaveReminderViewModel by inject()

    companion object {
        const val EXTRA_ReminderDataItemId = "EXTRA_ReminderDataItemId"
    }

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setSupportActionBar(findViewById(R.id.toolbar))
        appBarConfiguration = AppBarConfiguration(navController.graph, null)
        setupActionBarWithNavController(navController, appBarConfiguration)

        retrieveReminderDataItem()
    }

    /**
     * If the user clicks on edit button from Detail view
     * we need to navigate to SaveReminderFragment with the reminder data
     */
    private fun retrieveReminderDataItem() {
        val reminderDataItem = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(EXTRA_ReminderDataItemId, ReminderDataItem::class.java)
        } else {
            intent.getSerializableExtra(EXTRA_ReminderDataItemId) as ReminderDataItem?
        }

        if (reminderDataItem != null) {
            viewModel.editReminder(reminderDataItem)
            navController.navigate(ReminderListFragmentDirections.toSaveReminder())
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                navController.popBackStack()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
