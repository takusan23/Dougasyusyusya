package io.github.takusan23.dougasyusyusya.Activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import io.github.takusan23.dougasyusyusya.R
import io.github.takusan23.dougasyusyusya.databinding.ActivityLicenseBinding
import java.util.zip.Inflater

/**
 * ライセンス画面
 * */
class LicenseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val activityLicenseBinding = ActivityLicenseBinding.inflate(layoutInflater)
        setContentView(activityLicenseBinding.root)

        activityLicenseBinding.activityLicenseTextview.text = """
            
    --- yausername/youtubedl-android ---

    GNU General Public License v3.0

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.


    --- google/exoplayer ---
    
    Apache License 2.0
  
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    
        http://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

            
    --- takusan23/SearchPreferenceFragment ---
      
    Apache License 2.0
              
    Copyright 2020 takusan_23
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    
    http://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License. 
    
    --- google/material-design-icons ---

    We have made these icons available for you to incorporate into your products under the Apache License Version 2.0.
    Feel free to remix and re-share these icons and documentation in your products. 
    We'd love attribution in your app's about screen, but it's not required. The only thing we ask is that you not re-sell these icons.

    --- material-components/material-components-android ---

    Apache License 2.0
    
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    
    http://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License. 

        """.trimIndent()

    }

}