package edu.umich.interestingco.rememri

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import android.content.Context;
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager2.adapter.FragmentStateAdapter

class PageAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {

    var mFragmentList = ArrayList<Fragment>()
    var mFragmentTitleList = ArrayList<String>()

    override fun getPageTitle(position: Int): CharSequence {
        return "OBJECT ${(position + 1)}"
    }

    // this is for fragment tabs
    override fun getItem(position: Int): Fragment {
        return mFragmentList[position]
    }

    // this counts total number of tabs
    override fun getCount(): Int = 3
}