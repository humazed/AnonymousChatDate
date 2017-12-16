package humazed.github.com.anonymouschatanddate.chat.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.support.v7.app.AppCompatActivity
import android.transition.Transition
import android.transition.TransitionInflater
import android.view.View
import android.view.ViewAnimationUtils
import android.view.animation.AccelerateInterpolator
import android.widget.Toast
import humazed.github.com.anonymouschatanddate.R
import humazed.github.com.anonymouschatanddate.chat.data.StaticConfig
import humazed.github.com.kotlinandroidutils.ContextLocale
import kotlinx.android.synthetic.main.activity_register.*
import java.util.regex.Pattern


class RegisterActivity : AppCompatActivity() {
    private val VALID_EMAIL_ADDRESS_REGEX = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) showEnterAnimation()
        fab.setOnClickListener { v -> animateRevealClose() }
    }

    private fun showEnterAnimation() {
        val transition = TransitionInflater.from(this).inflateTransition(R.transition.fabtransition)
        if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
            window.sharedElementEnterTransition = transition
        }

        transition.addListener(object : Transition.TransitionListener {
            override fun onTransitionStart(transition: Transition) {
                addCardView.visibility = View.GONE
            }

            override fun onTransitionEnd(transition: Transition) {
                transition.removeListener(this)
                if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) animateRevealShow()
            }

            override fun onTransitionCancel(transition: Transition) {}
            override fun onTransitionPause(transition: Transition) {}
            override fun onTransitionResume(transition: Transition) {}
        })
    }

    @RequiresApi(VERSION_CODES.LOLLIPOP)
    fun animateRevealShow() {
        val mAnimator = ViewAnimationUtils.createCircularReveal(addCardView, addCardView.width / 2, 0,
                (fab.width / 2).toFloat(), addCardView.height.toFloat())
        mAnimator.duration = 500
        mAnimator.interpolator = AccelerateInterpolator()
        mAnimator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                addCardView.visibility = View.VISIBLE
                super.onAnimationStart(animation)
            }
        })
        mAnimator.start()
    }

    @SuppressLint("NewApi")
    private fun animateRevealClose() {
        if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
            val mAnimator = ViewAnimationUtils.createCircularReveal(addCardView, addCardView.width / 2, 0, addCardView.height.toFloat(), (fab.width / 2).toFloat())
            mAnimator.duration = 500
            mAnimator.interpolator = AccelerateInterpolator()
            mAnimator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    addCardView.visibility = View.INVISIBLE
                    super.onAnimationEnd(animation)
                    fab.setImageResource(R.drawable.ic_signup)
                    super@RegisterActivity.onBackPressed()
                }

            })
            mAnimator.start()
        } else {
            super@RegisterActivity.onBackPressed()
        }
    }

    override fun onBackPressed() = animateRevealClose()

    fun clickRegister(view: View) {
        val username = usernameEditText.text.toString()
        val password = passwordEditText.text.toString()
        val repeatPassword = repeatPasswordEditText.text.toString()
        if (validate(username, password, repeatPassword)) {
            val data = Intent()
            data.putExtra(StaticConfig.STR_EXTRA_USERNAME, username)
            data.putExtra(StaticConfig.STR_EXTRA_PASSWORD, password)
            data.putExtra(StaticConfig.STR_EXTRA_ACTION, STR_EXTRA_ACTION_REGISTER)
            setResult(Activity.RESULT_OK, data)
            finish()
        } else {
            Toast.makeText(this, "Invalid email or not match password", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Validate email, pass == re_pass
     *
     * @param emailStr
     * @param password
     * @return
     */
    private fun validate(emailStr: String, password: String, repeatPassword: String): Boolean {
        val matcher = VALID_EMAIL_ADDRESS_REGEX.matcher(emailStr)
        return password.isNotEmpty() && repeatPassword == password && matcher.find()
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(ContextLocale.wrap(newBase))
    }

    companion object {
        var STR_EXTRA_ACTION_REGISTER = "register"
    }
}
