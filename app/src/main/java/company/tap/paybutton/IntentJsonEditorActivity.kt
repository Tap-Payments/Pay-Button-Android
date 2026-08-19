package company.tap.paybutton

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONException
import org.json.JSONObject

/**
 * Android counterpart of the iOS demo's IntentJSONEditorViewController.
 *
 * The payload the button is about to be configured with, editable by hand. Save validates
 * before it hands anything back, so a payload that would not parse never reaches the sdk ..
 * the same two checks the iOS editor makes, minus the utf8 one, which a Kotlin String
 * cannot fail.
 *
 * What is saved here is what creates the intent. It is not a preview of the payload, it is
 * the payload.
 */
class IntentJsonEditorActivity : AppCompatActivity() {

    private lateinit var editor: EditText
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_intent_json_editor)

        title = getString(R.string.intent_json)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        editor = findViewById(R.id.intent_json)
        status = findViewById(R.id.status)

        // Anything that rewrites what is typed would corrupt json, ex a smart quote
        editor.inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS

        editor.setText(intent.getStringExtra(EXTRA_JSON).orEmpty())
        status.setText(R.string.json_editor_hint)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(0, MENU_SAVE, 0, R.string.save)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            MENU_SAVE -> {
                save()
                true
            }

            android.R.id.home -> {
                setResult(Activity.RESULT_CANCELED)
                finish()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    /** Validates, then hands the payload back. A payload that does not parse stays here */
    private fun save() {
        val edited: String = editor.text.toString()

        val parsed: JSONObject = try {
            JSONObject(edited)
        } catch (error: JSONException) {
            status.setTextColor(getColor(android.R.color.holo_red_dark))
            status.text = "Invalid json: ${error.message}"
            return
        }

        setResult(
            Activity.RESULT_OK,
            Intent().putExtra(EXTRA_JSON, parsed.toString())
        )
        finish()
    }

    companion object {
        /** The payload, going in and coming back out */
        const val EXTRA_JSON = "intent_json"
        private const val MENU_SAVE = 1
    }
}
