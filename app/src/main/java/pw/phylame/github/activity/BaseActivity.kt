package pw.phylame.github.activity

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.View
import android.view.WindowManager

abstract class BaseActivity : AppCompatActivity() {
    fun <T : Fragment> newFragment(clazz: Class<T>, init: (Bundle.() -> Unit)? = null): T {
        val fragment = clazz.newInstance()
        if (init != null) {
            fragment.arguments = Bundle()
            init(fragment.arguments)
        }
        return fragment
    }

    fun startActivity(target: Class<out Activity>, init: (Intent.() -> Unit)? = null) {
        val intent = Intent(this, target)
        init?.invoke(intent)
        startActivity(intent)
    }

    fun setImmersive() {
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = Color.TRANSPARENT
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        }
    }

    fun setupToolbar(toolbar: Toolbar, showHomeAsUp: Boolean = false) {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(showHomeAsUp)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}