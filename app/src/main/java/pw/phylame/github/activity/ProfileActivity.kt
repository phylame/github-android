package pw.phylame.github.activity

import android.os.Bundle
import com.bumptech.glide.Glide
import jp.wasabeef.glide.transformations.BlurTransformation
import kotlinx.android.synthetic.main.activity_profile.*
import pw.phylame.github.GitHub
import pw.phylame.github.R
import pw.phylame.support.dip

class ProfileActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        setupToolbar(toolbar, true)
        setImmersive()

        val size = dip(64F).toInt()
        GitHub.getUser(intent.getStringExtra("username"))
                .subscribe({ user ->
                    Glide.with(this)
                            .load(user.avatar)
                            .bitmapTransform(BlurTransformation(this, 25))
                            .into(banner)
                    Glide.with(this)
                            .load(user.avatar)
                            .override(size, size)
                            .into(avatar)
                    username.text = user.username
                }, { error ->
                })
    }
}