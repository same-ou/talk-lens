package com.example.talklens.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.talklens.R
import com.example.talklens.activity.ui.main.MainFragment

class CamActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cam)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, MainFragment.newInstance())
                .commitNow()
        }
    }
}