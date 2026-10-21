package com.lkboard.app.collaboration

import kotlin.test.Test
import kotlin.test.assertTrue

class WebClientHtmlTest {

    @Test
    fun testGenerateHtmlValid() {
        val html = WebClientHtml.generateHtml("TestBoard", "HostUser", 8765)
        assertTrue(html.contains("<canvas id=\"canvas\"></canvas>"))
    }
}
