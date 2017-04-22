package pw.phylame.github.activity

import android.databinding.DataBindingUtil
import android.graphics.Color
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.app.Fragment
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.graphics.Palette
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.GlideBitmapDrawable
import com.bumptech.glide.load.resource.drawable.GlideDrawable
import com.bumptech.glide.request.target.ImageViewTarget
import jp.wasabeef.glide.transformations.BlurTransformation
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.nav_header.*
import pw.phylame.github.GitHub
import pw.phylame.github.R
import pw.phylame.github.app
import pw.phylame.github.databinding.NavHeaderBinding
import pw.phylame.github.fragment.GistFragment
import pw.phylame.github.fragment.IssueFragment
import pw.phylame.github.fragment.RepositoryFragment
import rx.android.schedulers.AndroidSchedulers
import java.util.*

class MainActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener, View.OnClickListener {
    companion object {
        const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        setImmersive()

        val toggle = object : ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {
            var isLoaded = false
            override fun onDrawerSlide(drawerView: View?, slideOffset: Float) {
                super.onDrawerSlide(drawerView, slideOffset)
                if (!isLoaded) {
                    initNavigation()
                    isLoaded = true
                    info_bar.setOnClickListener(this@MainActivity)
                    nav_stars.setOnClickListener(this@MainActivity)
                    nav_followers.setOnClickListener(this@MainActivity)
                    nav_following.setOnClickListener(this@MainActivity)
                }
            }
        }
        drawer.addDrawerListener(toggle)
        toggle.syncState()

        nav.setNavigationItemSelectedListener(this)
        switchFragment(RepositoryFragment::class.java)

        initNavigation()
    }

    override fun onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_repository -> {
                switchFragment(RepositoryFragment::class.java)
            }
            R.id.nav_issue -> {
                switchFragment(IssueFragment::class.java)
            }
            R.id.nav_gist -> {
                switchFragment(GistFragment::class.java)
            }
        }
        drawer.closeDrawers()
        return true
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.info_bar -> {
                drawer.closeDrawers()
                startActivity(ProfileActivity::class.java) {
                    putExtra("username", app.githubPreferences.getString("username", null))
                }
            }
        }
    }

    val fragments = IdentityHashMap<Class<*>, Fragment>()

    fun switchFragment(target: Class<out Fragment>) {
        val fm = supportFragmentManager
        if (fm.fragments?.lastOrNull()?.javaClass !== target) {
            fm.beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                    .replace(R.id.container, fragments.getOrPut(target) {
                        newFragment(target) {
                            putString("username", app.githubPreferences.getString("username", null))
                        }
                    })
                    .commit()
        }
    }

    fun initNavigation() {
        val prefs = app.githubPreferences
        val username = prefs.getString("username", null)
        if (username.isNullOrEmpty()) {
            Toast.makeText(this, "Internal BUG: Not found user signed in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        if (nav_header != null) {
            GitHub.getUser(username)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ user ->
                        val binding: NavHeaderBinding = DataBindingUtil.bind(nav_header)
                        binding.user = user
                        if (!user.avatar.isNullOrEmpty()) {
                            loadAvatar(user.avatar!!, binding)
                        }
                    }, { error ->
                        Log.e(TAG, "load user error", error)
                        Toast.makeText(this, "Connection Error!", Toast.LENGTH_SHORT).show()
                    })
        }
    }

    fun loadAvatar(url: String, binding: NavHeaderBinding) {
        Glide.with(this)
                .load(url)
                .bitmapTransform(BlurTransformation(this, 25))
                .into(object : ImageViewTarget<GlideDrawable>(nav_background) {
                    override fun setResource(drawable: GlideDrawable?) {
                        nav_background.setImageDrawable(drawable)
                        if (drawable != null) {
                            val swatch = Palette.from((drawable as GlideBitmapDrawable).bitmap)
                                    .generate()
                                    .vibrantSwatch
                            if (swatch != null) {
                                binding.tintColor = swatch.rgb
                            } else {
                                binding.tintColor = Color.WHITE
                            }
                        }
                    }
                })

        val size = resources.getDimensionPixelSize(R.dimen.nav_avatar_size)
        Glide.with(this)
                .load(url)
                .override(size, size)
                .into(nav_avatar)
    }
}
