package com.example.interactivitydemo

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.commit
import com.example.interactivitydemo.databinding.ActivityMainBinding
import com.example.interactivitydemo.fragments.BrowseFragment
import com.example.interactivitydemo.fragments.JoinCreateFragment
import com.example.interactivitydemo.services.NetworkingService
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.voxeet.VoxeetSDK
import com.voxeet.promise.solve.PromiseExec
import com.voxeet.promise.solve.ThenVoid

private const val MAIN_TAG = "MainActivity"

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding // view binding

    lateinit var bottomNavBar: BottomNavigationView
    lateinit var toolbar: Toolbar

    private lateinit var mService : NetworkingService
    private var mBound: Boolean = false

    private val connection = object : ServiceConnection { // used to bind to the NetworkingService to provide networking operations
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as NetworkingService.NetworkingBinder
            mService = binder.getService()
            mBound = true

        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mBound = false
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_InteractivityDemo)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                add(R.id.fragment_container_view, JoinCreateFragment.newInstance()) // start on the join fragment by default
            }
        }


        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bottomNavBar = binding.navBar
        toolbar = binding.toolbar

        setSupportActionBar(toolbar)

        setListeners()
    }

    override fun onStart() { // bind to service
        super.onStart()
        val serviceIntent = Intent(this, NetworkingService::class.java)
        bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() { // unbind from service
        super.onStop()
        unbindService(connection)
        mBound = false
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_logout -> { // close session
                onBackPressed()
                return true
            }
            else -> {
                return super.onOptionsItemSelected(item)
            }
        }
    }

    private fun setListeners() {
        bottomNavBar.setOnNavigationItemSelectedListener {
            switchFragments(it.itemId)
        }
    }

    private fun switchFragments(itemId: Int):Boolean { // swap out
        when(itemId) {
            R.id.action_joincreate -> {
                supportFragmentManager.commit {
                    setReorderingAllowed(true)
                    replace(R.id.fragment_container_view, JoinCreateFragment.newInstance())
                }
                return true
            }
            R.id.action_browse -> {
                supportFragmentManager.commit {
                    setReorderingAllowed(true)
                    replace(R.id.fragment_container_view, BrowseFragment.newInstance())
                }
                return true
            }
        }
        return false
    }

    override fun onBackPressed() { // close session
        if (mBound) {
            mService.closeSession()
                    .then(ThenVoid {
                        super.onBackPressed()
                        Log.d(MAIN_TAG, "logged out successfully")
                    })
                    .error {
                        Log.e(MAIN_TAG, "can't log out ${it.printStackTrace()}")
                    }
        }
    }
}