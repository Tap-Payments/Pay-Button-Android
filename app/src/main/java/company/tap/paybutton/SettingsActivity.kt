/**
 *  Created by AhlaamK on 10/26/23, 2:14 PM
 *  Copyright (c) 2023 .
 *  All rights reserved Tap Payments.
 **
 */

package company.tap.paybutton

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.preference.Preference
import com.chillibits.simplesettings.core.SimpleSettings
import com.chillibits.simplesettings.core.SimpleSettingsConfig
import com.chillibits.simplesettings.tool.getPrefStringValue

class SettingsActivity : AppCompatActivity(),SimpleSettingsConfig.PreferenceCallback  {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        val configuration = SimpleSettingsConfig.Builder()
            .setActivityTitle("Configuration")
            .setPreferenceCallback(this)
            .displayHomeAsUpEnabled(false)
            .build()


        SimpleSettings(this, configuration).show(R.xml.preferences)

    }


    override fun onPreferenceClick(context: Context, key: String): Preference.OnPreferenceClickListener? {
        return when(key) {
            "dialog_preference" -> Preference.OnPreferenceClickListener {
                navigateToMainActivity()
                true
            }
            else -> super.onPreferenceClick(context, key)
        }
    }
    /**
     * Opens the demo.
     *
     * It used to carry every setting across as an intent extra .. some thirty of them, none of
     * which the screen ever read: it reads the preferences directly, which is the same store the
     * settings screen just wrote to
     */
    fun navigateToMainActivity() {
        finish()
        startActivity(Intent(this, MainActivity::class.java))
    }
}