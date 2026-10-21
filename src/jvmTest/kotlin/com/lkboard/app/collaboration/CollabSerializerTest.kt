package com.lkboard.app.collaboration

import com.lkboard.app.data.BoardDocument
import com.lkboard.app.data.BoardSerializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class CollabSerializerTest {

    @Test
    fun testDeserializeRelaySnapshot() {
        val json = """{"title": "LKBoard LK-1234", "elements": [], "version": 0}"""
        val doc = BoardSerializer.deserialize(json)
        assertEquals("LKBoard LK-1234", doc.title)
    }

    @Test
    fun testDeserializeRelaySnapshotAction() {
        val json = """{"type": "snapshot", "document": {"title": "LKBoard LK-1234", "elements": []}, "senderId": "server", "senderName": "Cloud Relay"}"""
        val action = BoardSerializer.jsonFormat.decodeFromString<CollabAction>(json)
        assertNotNull(action)
    }
}
