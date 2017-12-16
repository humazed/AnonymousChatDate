package humazed.github.com.anonymouschatanddate

import android.animation.Animator
import android.content.Context
import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import humazed.github.com.anonymouschatanddate.chat.MainChatActivity
import humazed.github.com.kotlinandroidutils.ContextLocale
import humazed.github.com.kotlinandroidutils.getLanguage
import kotlinx.android.synthetic.main.activity_splash.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.warn

class SplashActivity : AppCompatActivity(),AnkoLogger {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        initView()

        warn { "getLanguage() = ${getLanguage()}" }

        startChatButton.setOnClickListener {
            /* loadingView.show()
             startChatTextView.text = getString(R.string.loading)*/
            startActivity<MainChatActivity>()
            finish()
        }
    }

    private fun initView() {
        (imgBackgroundFire.drawable as AnimationDrawable).start()

        showButtonStartChat()
    }

    private fun showButtonStartChat() {
        startChatButton.alpha = 0f
        startChatButton.animate()
                .alpha(1f)
                .setDuration(2000)
                .setListener(object : Animator.AnimatorListener {
                    override fun onAnimationStart(animation: Animator) {
                        if (startChatButton.visibility != View.VISIBLE)
                            startChatButton.visibility = View.VISIBLE
                    }

                    override fun onAnimationEnd(animation: Animator) {}

                    override fun onAnimationCancel(animation: Animator) {}

                    override fun onAnimationRepeat(animation: Animator) {}
                }).start()
    }

    override fun attachBaseContext(newBase: Context?) = super.attachBaseContext(ContextLocale.wrap(newBase!!))
}
