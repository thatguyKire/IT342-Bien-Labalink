package edu.cit.bien.labalink.ui

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.material.bottomnavigation.BottomNavigationView
import edu.cit.bien.labalink.R
import edu.cit.bien.labalink.ui.bookings.BookingsActivity
import edu.cit.bien.labalink.ui.home.HomeActivity
import edu.cit.bien.labalink.ui.home.WalletActivity

/** The three top-level screens behind the shared bottom bar. */
enum class Tab(@IdRes val menuId: Int, val screen: Class<out Activity>) {
    HOME(R.id.tab_home, HomeActivity::class.java),
    BOOKINGS(R.id.tab_bookings, BookingsActivity::class.java),
    WALLET(R.id.tab_wallet, WalletActivity::class.java),
}

/**
 * Wires the bottom bar from layout/view_bottom_nav.xml.
 *
 * Home always stays at the bottom of the back stack; Bookings and Wallet
 * replace each other above it, so Back from either returns Home. Tabs
 * cross-fade instead of sliding like pushed screens do.
 */
fun AppCompatActivity.setupBottomNav(current: Tab, onReselect: () -> Unit = {}) {
    val nav = findViewById<BottomNavigationView>(R.id.bottomNav)
    val markCurrent = { nav.menu.findItem(current.menuId).isChecked = true }
    markCurrent()

    nav.setOnItemReselectedListener { onReselect() }
    nav.setOnItemSelectedListener { item ->
        val target = Tab.entries.first { it.menuId == item.itemId }
        openTab(current, target)
        true
    }
    // Coming back (e.g. Back from Bookings to Home) must show this tab again.
    lifecycle.addObserver(LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_RESUME) markCurrent()
    })
}

/** Opens a tab from anywhere, e.g. the wallet pill on Home. */
fun AppCompatActivity.openTab(from: Tab, to: Tab) {
    if (from == to) return
    val intent = Intent(this, to.screen)
    if (to == Tab.HOME) {
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }
    startActivity(
        intent,
        ActivityOptions.makeCustomAnimation(this, R.anim.tab_enter, R.anim.tab_exit)
            .toBundle())
    if (from != Tab.HOME) finish()
}
