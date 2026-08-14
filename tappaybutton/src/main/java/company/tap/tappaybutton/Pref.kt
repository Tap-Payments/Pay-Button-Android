package company.tap.tappaybutton

import android.content.Context
import android.content.SharedPreferences

object Pref {

    private var sharedPreferences: SharedPreferences? = null

    private fun openPref(context: Context) {
        val prefFile = context.packageName.replace(".", "_")

        sharedPreferences = context.getSharedPreferences(
            prefFile,
            Context.MODE_PRIVATE
        )
    }

    fun getValue(
        context: Context,
        key: String?,
        defaultValue: String?
    ): String {
        openPref(context)

        val result = sharedPreferences?.getString(
            key,
            defaultValue
        ) ?: ""

        sharedPreferences = null
        return result
    }

    fun setValue(
        context: Context,
        key: String?,
        value: String?
    ) {
        openPref(context)

        sharedPreferences?.edit()
            ?.putString(key, value)
            ?.apply()

        sharedPreferences = null
    }
}