package edu.umich.interestingco.rememri

import android.content.Context
import android.widget.Toast

// Used in Main Activity for the camera functionality
fun Context.toast(message: String, short: Boolean = true) {
    Toast.makeText(this, message, if (short) Toast.LENGTH_SHORT else Toast.LENGTH_LONG).show()
}
