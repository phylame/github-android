package pw.phylame.github.activity

import android.os.Bundle
import kotlinx.android.synthetic.main.activity_profile.*
import pw.phylame.github.R

class ProfileActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        setSupportActionBar(toolbar)
        val actionBar = supportActionBar!!
        actionBar.setDisplayHomeAsUpEnabled(true)
    }
}