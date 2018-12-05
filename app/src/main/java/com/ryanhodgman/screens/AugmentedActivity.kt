package com.ryanhodgman.screens

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.ryanhodgman.R
import com.ryanhodgman.framework.CoroutineActivity

class AugmentedActivity : CoroutineActivity() {

    companion object {
        fun newIntent(context: Context) = Intent(context, AugmentedActivity::class.java)
    }

    private lateinit var augmentedFragment: AugmentedFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ar)
        augmentedFragment = supportFragmentManager.findFragmentById(R.id.fragment_ar) as AugmentedFragment
    }
}
