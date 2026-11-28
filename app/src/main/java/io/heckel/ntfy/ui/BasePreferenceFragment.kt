package io.heckel.ntfy.ui

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import io.heckel.ntfy.BuildConfig
import io.heckel.ntfy.R

abstract class BasePreferenceFragment : PreferenceFragmentCompat() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Apply window insets to ensure content is not covered by navigation bar
        listView?.let { recyclerView ->
            recyclerView.clipToPadding = false
            ViewCompat.setOnApplyWindowInsetsListener(recyclerView) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.updatePadding(bottom = systemBars.bottom)
                insets
            }
        }
    }

    /**
     * Show [ListPreference] and [EditTextPreference] dialog by [MaterialAlertDialogBuilder]
     */
    override fun onDisplayPreferenceDialog(preference: Preference) {
        // E Ink: the stock choice and text-input sheets instead of dimmed dialogs
        if (BuildConfig.E_INK && preference is ListPreference) {
            val prefIndex = preference.entryValues.indexOf(preference.value)
            EInkSheet.choiceDialog(requireContext(), preference.title.toString(), preference.entries.map { it.toString() }, prefIndex) { index ->
                val newValue = preference.entryValues[index].toString()
                if (preference.callChangeListener(newValue)) {
                    preference.value = newValue
                }
            }.show()
            return
        }
        if (BuildConfig.E_INK && preference is EditTextPreference) {
            val view = layoutInflater.inflate(R.layout.preference_dialog_edittext_edited, null)
            view.findViewById<TextView>(android.R.id.message).text =
                preference.dialogMessage?.toString() ?: preference.extras.getString("message") ?: ""
            val editText = view.findViewById<TextInputEditText>(android.R.id.edit)
            editText.setText(preference.text ?: "")
            editText.hint = preference.extras.getString("hint") ?: ""
            val dialog = EInkSheet.inputDialog(requireContext(), preference.title.toString(), view, getString(android.R.string.ok)) {
                val newValue = editText.text.toString()
                if (preference.callChangeListener(newValue)) {
                    preference.text = newValue
                }
            }
            dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
            dialog.setOnShowListener {
                editText.requestFocus()
                editText.setSelection(editText.text?.length ?: 0)
            }
            dialog.show()
            return
        }
        when (preference) {
            is ListPreference -> {
                val prefIndex = preference.entryValues.indexOf(preference.value)
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(preference.title)
                    .setSingleChoiceItems(preference.entries, prefIndex) { dialog, index ->
                        val newValue = preference.entryValues[index].toString()
                        if (preference.callChangeListener(newValue)) {
                            preference.value = newValue
                        }
                        dialog.dismiss()
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }
            is EditTextPreference -> {
                val view = layoutInflater.inflate(R.layout.preference_dialog_edittext_edited, null)

                // Description/message: Use dialogMessage if set, otherwise check extras
                val messageView = view.findViewById<TextView>(android.R.id.message)
                val message = preference.dialogMessage?.toString()
                    ?: preference.extras.getString("message")
                    ?: ""
                messageView.text = message

                // Text field: Handle null text by using empty string instead of "null"
                val editText = view.findViewById<TextInputEditText>(android.R.id.edit)
                val hint = preference.extras.getString("hint") ?: ""
                editText.setText(preference.text ?: "")
                editText.hint = hint

                // Configure dialog
                val dialog = MaterialAlertDialogBuilder(requireContext())
                    .setTitle(preference.title)
                    .setView(view)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        val newValue = editText.text.toString()
                        if (preference.callChangeListener(newValue)) {
                            preference.text = newValue
                        }
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .create()

                // Show keyboard when dialog is shown
                dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
                dialog.setOnShowListener {
                    editText.requestFocus()
                    editText.setSelection(editText.text?.length ?: 0)
                }
                dialog.show()
            }
            else -> super.onDisplayPreferenceDialog(preference)
        }
    }
}
