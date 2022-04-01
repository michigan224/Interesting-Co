package edu.umich.interestingco.rememri

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout

class FriendActivity : AppCompatActivity() {

    private var tabLayout: TabLayout? = null
    private var viewPager: ViewPager? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val oldSharedPref = getSharedPreferences("mypref", 0)
        val token = oldSharedPref.getString("token", "")

        if (token == "") {
            Toast.makeText(this@FriendActivity, "Not Logged In", Toast.LENGTH_LONG)
                .show()
            startActivity(Intent(this, AccountActivity::class.java))
        }

        setContentView(R.layout.activity_friend)

        tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        viewPager = findViewById<ViewPager>(R.id.viewPager)
        tabLayout?.setupWithViewPager(viewPager)

        //tabLayout!!.addTab(tabLayout!!.newTab().setText("View"))
        //tabLayout!!.addTab(tabLayout!!.newTab().setText("Add"))
        //tabLayout!!.addTab(tabLayout!!.newTab().setText("Requests"))
        //tabLayout!!.tabGravity = TabLayout.GRAVITY_FILL

        val myAdapter = PageAdapter(supportFragmentManager)
        myAdapter.addFragment(FriendListFragment(), "View")
        myAdapter.addFragment(FriendAddFragment(), "Add")
        myAdapter.addFragment(FriendRequestFragment(), "Requests")
        viewPager!!.adapter = myAdapter

        //viewPager!!.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(tabLayout))

        //tabLayout!!.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            //override fun onTabSelected(tab: TabLayout.Tab) {
                //viewPager!!.currentItem = tab.position
            //}
            //override fun onTabUnselected(tab: TabLayout.Tab) {

            //}
            //override fun onTabReselected(tab: TabLayout.Tab) {

            //}
        //})

    }

    fun returnMain(view: View?) = startActivity(Intent(this, MainActivity::class.java))

    fun returnAccount(view: View?) = startActivity(Intent(this, AccountActivity::class.java))
}

