package pw.phylame.github.activity

import android.app.ProgressDialog
import android.content.Context
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_login.*
import pw.phylame.github.GitHub
import pw.phylame.github.R
import pw.phylame.support.tintDrawables
import retrofit2.HttpException

class LoginActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (GitHub.isSignedIn) {
            startApp()
        }

        setContentView(R.layout.activity_login)
        setImmersive()

        headerView.tintDrawables()

        loginView.tintDrawables()
        loginView.setOnEditorActionListener { view, _, _ ->
            if (view.text.isEmpty()) {
                view.error = getString(R.string.error_field_required)
            } else {
                passwordView.requestFocus()
            }
            true
        }

        passwordView.tintDrawables()
        passwordView.setOnEditorActionListener({ view, id, _ ->
            if (view.text.isEmpty()) {
                view.error = getString(R.string.error_field_required)
            } else {
                attemptLogin()
            }
            true
        })

        submit.setOnClickListener { attemptLogin() }
    }

    private fun startApp() {
        startActivity(MainActivity::class.java)
        finish()
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private fun attemptLogin() {
        loginView.error = null
        passwordView.error = null

        val username = loginView.text.toString()
        val password = passwordView.text.toString()

        // Check for a valid email address.
        if (username.isEmpty()) {
            loginView.error = getString(R.string.error_field_required)
            loginView.requestFocus()
            return
        }

        // Check for a valid password
        if (password.isEmpty()) {
            passwordView.error = getString(R.string.error_field_required)
            passwordView.requestFocus()
            return
        }

        currentFocus?.also {
            (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                    .hideSoftInputFromWindow(it.windowToken, 0)
        }

        var dialog: ProgressDialog? = null
        GitHub.login(username, password)
                .doOnSubscribe {
                    dialog = ProgressDialog.show(this, null, getString(R.string.sign_in_loading))
                }
                .subscribe({ user ->
                    dialog?.dismiss()
                    if (user != null) {
                        startApp()
                    } else {
                        Toast.makeText(this, "Login Failed", Toast.LENGTH_SHORT).show()
                    }
                }, { err ->
                    dialog?.dismiss()
                    val msg = if (err is HttpException) {
                        getString(when (err.code()) {
                            401 -> R.string.login_error_unauthorized
                            else -> R.string.error_network_connection
                        })
                    } else {
                        err.printStackTrace()
                        getString(R.string.error_internal_bug)
                    }
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                })
    }
}

