/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.better.alarm.view

import android.app.Activity
import android.app.Fragment
import android.content.Context
import android.content.Intent
import android.content.res.TypedArray
import android.media.RingtoneManager
import android.net.Uri
import android.preference.Preference
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.better.alarm.R
import com.better.alarm.checkPermissions
import com.better.alarm.configuration.AlarmApplication.container
import com.better.alarm.model.Alarmtone
import io.reactivex.disposables.Disposable

class RingtonePreference(val mContext: Context, attrs: AttributeSet) : Preference(mContext, attrs) {
    private var host: Fragment? = null
    private var view: View? = null
    private var disposable: Disposable? = null
    private var active = false

    init {
        layoutResource = R.layout.ringtone_preference
    }

    override fun onGetDefaultValue(a: TypedArray?, index: Int): Any {
        return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString()
    }

    override fun onBindView(view: View) {
        super.onBindView(view)
        this.view = view
        host?.let { host ->
            if (!active) resume(host, view)
        }
    }

    fun onResume(host: Fragment) {
        this.host = host
        view?.let { view ->
            if (!active) resume(host, view)
        }
    }

    private fun resume(host: Fragment, view: View) {
        active = true
        val ringtoneSummary = view.findViewById<TextView>(R.id.settings_ringtone_summary)
        val ringtoneLayout = view.findViewById<View>(R.id.settings_ringtone)
        ringtoneLayout.setOnClickListener {
            showPicker(
                    fragment = host,
                    ringtoneManagerString = Uri.parse(container().rxPrefs.getString("ringtone_default").get()),
                    showDefault = true,
                    showSilent = false
            )
        }
        // TODO default / unbekannt
        // TODO handle default case better
        disposable = container().rxPrefs.getString("ringtone_default")
                .asObservable()
                .map { ringtoneUri ->
                    // TODO silent
                    RingtoneManager.getRingtone(context, Uri.parse(ringtoneUri))
                            ?.getTitle(context)
                            ?: context.getText(R.string.silent_alarm_summary)
                }
                .subscribe { title ->
                    ringtoneSummary.text = title
                }
    }

    fun onPause() {
        active = false
        disposable?.dispose()
    }

    fun onActivityResult(alarmtone: Alarmtone) {
        container().rxPrefs.getString("ringtone_default").set(alarmtone.ringtoneManagerString()?.toString()
                ?: "silent")
    }

    companion object {
        fun showPicker(
                fragment: Fragment? = null,
                fragmentCompat: android.support.v4.app.Fragment? = null,
                ringtoneManagerString: Uri?,
                showDefault: Boolean,
                showSilent: Boolean
        ) {
            try {
                val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                    putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, ringtoneManagerString)

                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, showDefault)
                    putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))

                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, showSilent)
                    putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                }

                fragment?.startActivityForResult(intent, 42)
                fragmentCompat?.startActivityForResult(intent, 42)
            } catch (e: Exception) {
                fragment?.activity?.run {
                    Toast.makeText(this, getString(R.string.details_no_ringtone_picker), Toast.LENGTH_LONG).show()
                }
                fragmentCompat?.activity?.run {
                    Toast.makeText(this, getString(R.string.details_no_ringtone_picker), Toast.LENGTH_LONG).show()
                }
            }
        }

        fun onActivityResult(activity: Activity, requestCode: Int, data: Intent?, consumer: (Alarmtone) -> Unit) {
            if (data != null && requestCode == 42) {
                val alert: String? = data.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)?.toString()

                container().logger.d("Got ringtone: $alert")

                val alarmtone = when (alert) {
                    null -> Alarmtone.Silent()
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString() -> Alarmtone.Default()
                    else -> Alarmtone.Sound(alert)
                }

                container().logger.d("onActivityResult $alert -> $alarmtone")

                checkPermissions(activity, listOf(alarmtone))

                consumer(alarmtone)
            }
        }
    }
}