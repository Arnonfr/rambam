package com.example

import android.content.ComponentName
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.test.core.app.ApplicationProvider
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.robolectric.Shadows

/** Release APKs correctly exclude ui-test-manifest. Register only the Robolectric host activity. */
internal fun withComposeHost(compose: ComposeContentTestRule): TestRule = RuleChain.outerRule(object : ExternalResource() {
    override fun before() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        Shadows.shadowOf(context.packageManager)
            .addActivityIfNotPresent(ComponentName(context, ComponentActivity::class.java)).exported = true
    }
}).around(compose)
