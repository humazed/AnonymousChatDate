package humazed.github.com.anonymouschatanddate.chat.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.yarolegovich.lovelydialog.LovelyInfoDialog
import com.yarolegovich.lovelydialog.LovelyProgressDialog
import humazed.github.com.anonymouschatanddate.R
import humazed.github.com.anonymouschatanddate.chat.MainChatActivity
import humazed.github.com.anonymouschatanddate.chat.data.SharedPreferenceHelper
import humazed.github.com.anonymouschatanddate.chat.data.StaticConfig
import humazed.github.com.anonymouschatanddate.chat.model.User
import humazed.github.com.anonymouschatanddate.questions.QuestionStepperActivity
import humazed.github.com.kotlinandroidutils.ContextLocale
import kotlinx.android.synthetic.main.activity_login.*
import org.jetbrains.anko.startActivity
import java.util.*
import java.util.regex.Pattern

class LoginActivity : AppCompatActivity() {
    private val VALID_EMAIL_ADDRESS_REGEX = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE)

    private var waitingDialog: LovelyProgressDialog? = null

    private var authUtils: AuthUtils = AuthUtils()
    private var mAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private var mAuthListener: FirebaseAuth.AuthStateListener? = null
    private var user: FirebaseUser? = null
    private var firstTimeAccess: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        firstTimeAccess = true
        initFirebase()

        fab.setOnClickListener { clickRegisterLayout() }
        goButton.setOnClickListener { clickLogin() }
        resetPasswordTextView.setOnClickListener { clickResetPassword() }
    }

    /**
     * Initialization components needed for log management.
     */
    private fun initFirebase() {
        // Initialize the component to login, register.
        mAuthListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            user = firebaseAuth.currentUser
            if (user != null) {
                // User is signed in
                StaticConfig.UID = user!!.uid
                Log.d(TAG, "onAuthStateChanged:signed_in:" + user!!.uid)
                if (firstTimeAccess) {
                    startActivity(Intent(this@LoginActivity, MainChatActivity::class.java))
                    this@LoginActivity.finish()
                }
            } else {
                Log.d(TAG, "onAuthStateChanged:signed_out")
            }
            firstTimeAccess = false
        }


        // Initialize dialog waiting when logged in.
        waitingDialog = LovelyProgressDialog(this).setCancelable(false)
    }

    @SuppressLint("RestrictedApi", "NewApi")
    private fun clickRegisterLayout() {
        if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
            window.exitTransition = null
            window.enterTransition = null
        }

        if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
            val options = ActivityOptions.makeSceneTransitionAnimation(this, fab, fab.transitionName)
            startActivityForResult(Intent(this, RegisterActivity::class.java), StaticConfig.REQUEST_CODE_REGISTER, options.toBundle())
        } else {
            startActivityForResult(Intent(this, RegisterActivity::class.java), StaticConfig.REQUEST_CODE_REGISTER)
        }
    }

    private fun clickLogin() {
        val username = usernameEditText.text.toString()
        val password = passwordEditText.text.toString()
        if (validate(username, password)) {
            authUtils.signIn(username, password)
        } else {
            Toast.makeText(this, "Invalid email or empty password", Toast.LENGTH_SHORT).show()
        }
    }

    private fun clickResetPassword() {
        val username = usernameEditText.text.toString()
        if (validate(username, ";")) {
            authUtils.resetPassword(username)
        } else {
            Toast.makeText(this, "Invalid email", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == StaticConfig.REQUEST_CODE_REGISTER && resultCode == Activity.RESULT_OK) {
            authUtils.createUser(data.getStringExtra(StaticConfig.STR_EXTRA_USERNAME),
                    data.getStringExtra(StaticConfig.STR_EXTRA_PASSWORD))
        }
    }

    override fun onStart() {
        super.onStart()
        mAuth.addAuthStateListener(mAuthListener!!)
    }

    override fun onStop() {
        super.onStop()
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener!!)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        setResult(Activity.RESULT_CANCELED, null)
        finish()
    }

    private fun validate(emailStr: String, password: String): Boolean {
        val matcher = VALID_EMAIL_ADDRESS_REGEX.matcher(emailStr)
        return (password.isNotEmpty() || password == ";") && matcher.find()
    }

    override fun attachBaseContext(newBase: Context) = super.attachBaseContext(ContextLocale.wrap(newBase))


    /**
     * Definition of utility functions for the process of registration, ...
     */
    internal inner class AuthUtils {
        /**
         * Action register
         *
         * @param email
         * @param password
         */
        fun createUser(email: String, password: String) {
            waitingDialog!!.setIcon(R.drawable.ic_add_friend)
                    .setTitle("Registering....")
                    .setTopColorRes(R.color.colorPrimary)
                    .show()
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this@LoginActivity) { task ->
                        Log.d(TAG, "createUserWithEmail:onComplete:" + task.isSuccessful)
                        waitingDialog!!.dismiss()
                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (!task.isSuccessful) {
                            object : LovelyInfoDialog(this@LoginActivity) {
                                override fun setConfirmButtonText(text: String): LovelyInfoDialog {
                                    findView<View>(com.yarolegovich.lovelydialog.R.id.ld_btn_confirm).setOnClickListener { view -> dismiss() }
                                    return super.setConfirmButtonText(text)
                                }
                            }.setTopColorRes(R.color.colorAccent)
                                    .setIcon(R.drawable.ic_add_friend)
                                    .setTitle("Register false")
                                    .setMessage("Email exist or weak password!")
                                    .setConfirmButtonText("ok")
                                    .setCancelable(false)
                                    .show()
                        } else {
                            initNewUserInfo()
                            Toast.makeText(this@LoginActivity, "Register and Login success", Toast.LENGTH_SHORT).show()
                            startActivity<QuestionStepperActivity>()
                            finish()
                        }
                    }
                    .addOnFailureListener { waitingDialog!!.dismiss() }
        }


        /**
         * Action Login
         *
         * @param email
         * @param password
         */
        fun signIn(email: String, password: String) {
            waitingDialog!!.setIcon(R.drawable.ic_person_low)
                    .setTitle("Login....")
                    .setTopColorRes(R.color.colorPrimary)
                    .show()
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this@LoginActivity) { task ->
                        Log.d(TAG, "signInWithEmail:onComplete:" + task.isSuccessful)
                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        waitingDialog!!.dismiss()
                        if (!task.isSuccessful) {
                            Log.w(TAG, "signInWithEmail:failed", task.exception)
                            object : LovelyInfoDialog(this@LoginActivity) {
                                override fun setConfirmButtonText(text: String): LovelyInfoDialog {
                                    findView<View>(com.yarolegovich.lovelydialog.R.id.ld_btn_confirm).setOnClickListener { dismiss() }
                                    return super.setConfirmButtonText(text)
                                }
                            }.setTopColorRes(R.color.colorAccent)
                                    .setIcon(R.drawable.ic_person_low)
                                    .setTitle("Login false")
                                    .setMessage("Email not exist or wrong password!")
                                    .setCancelable(false)
                                    .setConfirmButtonText("Ok")
                                    .show()
                        } else {
                            saveUserInfo()
                            startActivity(Intent(this@LoginActivity, MainChatActivity::class.java))
                            this@LoginActivity.finish()
                        }
                    }
                    .addOnFailureListener { e -> waitingDialog!!.dismiss() }
        }

        /**
         * Action reset password
         *
         * @param email
         */
        fun resetPassword(email: String) {
            mAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener {
                        object : LovelyInfoDialog(this@LoginActivity) {
                            override fun setConfirmButtonText(text: String): LovelyInfoDialog {
                                findView<View>(com.yarolegovich.lovelydialog.R.id.ld_btn_confirm).setOnClickListener { view -> dismiss() }
                                return super.setConfirmButtonText(text)
                            }
                        }
                                .setTopColorRes(R.color.colorPrimary)
                                .setIcon(R.drawable.ic_pass_reset)
                                .setTitle("Password Recovery")
                                .setMessage("Sent email to " + email)
                                .setConfirmButtonText("Ok")
                                .show()
                    }
                    .addOnFailureListener {
                        object : LovelyInfoDialog(this@LoginActivity) {
                            override fun setConfirmButtonText(text: String): LovelyInfoDialog {
                                findView<View>(com.yarolegovich.lovelydialog.R.id.ld_btn_confirm).setOnClickListener { view -> dismiss() }
                                return super.setConfirmButtonText(text)
                            }
                        }
                                .setTopColorRes(R.color.colorAccent)
                                .setIcon(R.drawable.ic_pass_reset)
                                .setTitle("False")
                                .setMessage("False to sent email to " + email)
                                .setConfirmButtonText("Ok")
                                .show()
                    }
        }

        /**
         * Luu thong tin user info cho nguoi dung dang nhap
         */
        fun saveUserInfo() {
            FirebaseDatabase.getInstance().reference.child("user/" + StaticConfig.UID).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    waitingDialog!!.dismiss()
                    val hashUser = dataSnapshot.value as HashMap<*, *>?
                    val userInfo = User()
                    userInfo.name = hashUser!!["name"] as String
                    userInfo.email = hashUser["email"] as String
                    userInfo.avata = hashUser["avata"] as String
                    SharedPreferenceHelper.getInstance(this@LoginActivity).saveUserInfo(userInfo)
                }

                override fun onCancelled(databaseError: DatabaseError) {}
            })
        }

        /**
         * Khoi tao thong tin mac dinh cho tai khoan moi
         */
        private fun initNewUserInfo() {
            val newUser = User()
            newUser.email = user!!.email!!
            newUser.name = user!!.email!!.substring(0, user!!.email!!.indexOf("@"))
            newUser.avata = StaticConfig.STR_DEFAULT_BASE64
            FirebaseDatabase.getInstance().reference.child("user/" + user!!.uid).setValue(newUser)
        }
    }

    companion object {
        private val TAG = "LoginActivity"
    }
}
