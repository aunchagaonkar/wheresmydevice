package com.supernova.wheresmydevice.dialogs

import android.app.Activity
import android.app.AlertDialog
import androidx.annotation.StringRes

/**
 * Dialog to explain why a permission is needed before requesting it
 */
class PermissionExplanationDialog(
    private val activity: Activity,
    @StringRes private val explanationResId: Int,
    private val onConfirm: () -> Unit,
    private val onDismiss: () -> Unit
) {
    fun show() {
        val builder = AlertDialog.Builder(activity)
            .setTitle(activity.getString(android.R.string.dialog_alert_title))
            .setMessage(activity.getString(explanationResId))
            .setPositiveButton(android.R.string.ok) { _, _ -> onConfirm() }
            .setNegativeButton(android.R.string.cancel) { _, _ -> onDismiss() }
            .setCancelable(false)
        
        builder.create().show()
    }
} 