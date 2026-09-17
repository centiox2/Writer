package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.navigation.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Songwriter", appName)
  }

  @Test
  fun `navigation screens are properly initialized and not null`() {
    val items = Screen.bottomNavItems
    assertEquals(4, items.size)
    items.forEach { screen ->
      assertNotNull("Screen in bottomNavItems must not be null", screen)
      assertNotNull("Screen route must not be null", screen.route)
      assertTrue("Screen route must not be empty", screen.route.isNotEmpty())
      assertNotNull("Screen title must not be null", screen.title)
      assertNotNull("Screen icon must not be null", screen.icon)
    }
  }
}
