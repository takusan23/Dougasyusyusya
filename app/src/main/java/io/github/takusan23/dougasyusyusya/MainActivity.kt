package io.github.takusan23.dougasyusyusya

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import io.github.takusan23.dougasyusyusya.Fragment.SettingFragment
import io.github.takusan23.dougasyusyusya.ViewModel.DownloadFragment
import io.github.takusan23.dougasyusyusya.ViewModel.VideoListFragment
import io.github.takusan23.dougasyusyusya.ViewModel.regexUrl
import io.github.takusan23.dougasyusyusya.databinding.ActivityMainBinding
import io.github.takusan23.searchpreferencefragment.SearchPreferenceChildFragment
import io.github.takusan23.searchpreferencefragment.SearchPreferenceFragment

class MainActivity : AppCompatActivity() {

    private val activityMainViewBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    private val permissionResultCallback = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            Toast.makeText(this, "権限取得できたよ", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(activityMainViewBinding.root)

        // 権限リクエスト。Android 9以前のユーザーのみ
        if (Build.VERSION_CODES.P >= Build.VERSION.SDK_INT) {
            permissionResultCallback.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        // Fragment置き換え
        activityMainViewBinding.activityMainBottomNav.setOnNavigationItemSelectedListener {
            val fragment = when (it.itemId) {
                R.id.activity_main_bottom_nav_menu_download -> DownloadFragment()
                R.id.activity_main_bottom_nav_menu_file -> VideoListFragment()
                R.id.activity_main_bottom_nav_setting -> SettingFragment().apply {
                    arguments = Bundle().apply {
                        // 最初に表示するリソースID
                        putInt(SearchPreferenceChildFragment.PREFERENCE_XML_RESOURCE_ID, R.xml.preference)
                        val map = hashMapOf<String, Int>()
                        putSerializable(SearchPreferenceFragment.PREFERENCE_XML_FRAGMENT_NAME_HASH_MAP, map)
                    }
                }
                else -> DownloadFragment()
            }
            setFragment(fragment)
            true
        }

        activityMainViewBinding.activityMainBottomNav.selectedItemId = R.id.activity_main_bottom_nav_menu_download

        // 共有から来た時
        launchFromShare()

    }

    private fun setDownloadFragment(url: String) {
        // URL付きで渡す
        val fragment = DownloadFragment().apply {
            arguments = Bundle().apply {
                putString("url", url)
            }
        }
        setFragment(fragment)
    }

    /**
     * 共有から開いたとき
     * */
    private fun launchFromShare() {
        if (Intent.ACTION_SEND == intent.action) {
            val extras = intent.extras
            // URL
            val text = extras?.getCharSequence(Intent.EXTRA_TEXT) ?: ""
            // 正規表現で取り出す
            val youtubeUrl = regexUrl(text.toString()) ?: return
            // URL付きで渡す
            setDownloadFragment(youtubeUrl)
        }
    }


    /**
     * Fragmentを置く
     * @param fragment その名の通り
     * */
    private fun setFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.activity_main_fragment_host_framelayout, fragment)
            commit()
        }
    }
}