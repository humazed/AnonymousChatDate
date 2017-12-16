package humazed.github.com.anonymouschatanddate.chat

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import humazed.github.com.anonymouschatanddate.R
import humazed.github.com.anonymouschatanddate.chat.data.StaticConfig
import humazed.github.com.anonymouschatanddate.chat.service.ServiceUtils
import humazed.github.com.anonymouschatanddate.chat.ui.LoginActivity
import humazed.github.com.anonymouschatanddate.chat.ui.UserProfileFragment
import humazed.github.com.anonymouschatanddate.chat.ui.friendslist.FriendsFragment
import humazed.github.com.kotlinandroidutils.ContextLocale
import kotlinx.android.synthetic.main.activity_chat_main.*
import kotlinx.android.synthetic.main.content_main.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.warn
import java.util.*

class MainChatActivity : AppCompatActivity(), AnkoLogger {

    private val STR_FRIEND_FRAGMENT = "FRIEND"
    private val STR_INFO_FRAGMENT = "INFO"

    private var adapter: ViewPagerAdapter? = null

    private var mAuth: FirebaseAuth? = null
    private var mAuthListener: FirebaseAuth.AuthStateListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_main)
        setSupportActionBar(toolbar)

        initFirebase()
    }

    private fun initFirebase() {
        //Initialize the component to login, register
        mAuth = FirebaseAuth.getInstance()
        mAuthListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                StaticConfig.UID = user.uid
                initTab()
            } else {
                startActivity<LoginActivity>()
                finish()
                warn { "onAuthStateChanged:signed_out" }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        mAuth!!.addAuthStateListener(mAuthListener!!)
        ServiceUtils.stopServiceFriendChat(applicationContext, false)
    }

    override fun onStop() {
        super.onStop()
        if (mAuthListener != null) {
            mAuth!!.removeAuthStateListener(mAuthListener!!)
        }
    }

    override fun onDestroy() {
        ServiceUtils.startServiceFriendChat(applicationContext)
        super.onDestroy()
    }

    /**
     * Khoi tao 2 tab
     */
    private fun initTab() {
        tabLayout.setSelectedTabIndicatorColor(resources.getColor(R.color.colorIndivateTab))
        setupViewPager(viewPager)
        tabLayout.setupWithViewPager(viewPager)
        setupTabIcons()
    }


    private fun setupTabIcons() {
        val tabIcons = intArrayOf(R.drawable.ic_tab_person, R.drawable.ic_tab_infor)

        tabLayout.getTabAt(0)!!.setIcon(tabIcons[0])
        tabLayout.getTabAt(1)!!.setIcon(tabIcons[1])
    }

    private fun setupViewPager(viewPager: ViewPager?) {
        adapter = ViewPagerAdapter(supportFragmentManager)
        adapter!!.addFrag(FriendsFragment(), STR_FRIEND_FRAGMENT)
        adapter!!.addFrag(UserProfileFragment(), STR_INFO_FRAGMENT)
        fab.setOnClickListener((adapter!!.getItem(0) as FriendsFragment).FragFriendClickFloatButton().getInstance(this))
        viewPager!!.adapter = adapter
        viewPager.offscreenPageLimit = 2
        viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

            override fun onPageSelected(position: Int) {
                ServiceUtils.stopServiceFriendChat(this@MainChatActivity.applicationContext, false)
                if (adapter!!.getItem(position) is FriendsFragment) {
                    fab.visibility = View.VISIBLE
                    fab.setOnClickListener((adapter!!.getItem(position) as FriendsFragment).FragFriendClickFloatButton()
                            .getInstance(this@MainChatActivity))
                    fab.setImageResource(R.drawable.plus)
                } else {
                    fab.visibility = View.GONE
                }
            }

            override fun onPageScrollStateChanged(state: Int) {}
        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.about) {
            Toast.makeText(this, getString(R.string.app_name), Toast.LENGTH_LONG).show()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    /**
     * Adapter hien thi tab
     */
    internal inner class ViewPagerAdapter(manager: FragmentManager) : FragmentPagerAdapter(manager) {
        private val mFragmentList = ArrayList<Fragment>()
        private val mFragmentTitleList = ArrayList<String>()

        override fun getItem(position: Int): Fragment {
            return mFragmentList[position]
        }

        override fun getCount(): Int {
            return mFragmentList.size
        }

        fun addFrag(fragment: Fragment, title: String) {
            mFragmentList.add(fragment)
            mFragmentTitleList.add(title)
        }

        override fun getPageTitle(position: Int): CharSequence? {
            // return null to display only the icon
            return null
        }
    }

    override fun attachBaseContext(newBase: Context?) = super.attachBaseContext(ContextLocale.wrap(newBase!!))
}