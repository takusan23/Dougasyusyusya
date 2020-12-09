package io.github.takusan23.dougasyusyusya.Fragment

import android.content.Intent
import androidx.core.net.toUri
import io.github.takusan23.searchpreferencefragment.SearchPreferenceFragment

/**
 * 設定Fragment
 * */
class SettingFragment : SearchPreferenceFragment() {

    init {
        onPreferenceClickFunc = { preference ->
            when (preference?.key) {
                "preference_source_code" -> {
                    val intent = Intent(Intent.ACTION_VIEW, "https://github.com/takusan23/Dougasyusyusya".toUri())
                    startActivity(intent)
                }
            }
        }
    }

}