package com.lkboard.app.collaboration

import com.lkboard.app.data.BoardDocument
import com.lkboard.app.model.BoardElement
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Информация о подключенном участнике совместной сессии.
 */
@Serializable
data class CollabPeer(
    val id: String,
    val name: String,
    val colorHex: Long,
    val isHost: Boolean,
    val clientType: String = "Desktop" // "Desktop" или "Web"
)

/**
 * Состояние удаленного курсора или лазерной указки участника.
 */
data class RemotePointer(
    val peerId: String,
    val peerName: String,
    val x: Float,
    val y: Float,
    val colorHex: Long,
    val isLaser: Boolean,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Serializable
data class CollabSyncResponse(
    val currentVersion: Long,
    val actions: List<CollabActionRecord>
)

@Serializable
data class CollabActionRecord(
    val id: Long,
    val action: CollabAction
)

/**
 * Запись о действии с планшета для ленты последних 3 действий.
 */
@Serializable
data class TabletActionRecord(
    val id: String = java.util.UUID.randomUUID().toString(),
    val peerId: String = "",
    val peerName: String = "",
    val actionTitle: String = "",
    val icon: String = "✏️",
    val detail: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Геометрия видимой области экрана компьютера для точной синхронизации стилуса смартфона.
 */
@Serializable
data class ViewportInfo(
    val panX: Float = 0f,
    val panY: Float = 0f,
    val scale: Float = 1f,
    val width: Float = 1366f,
    val height: Float = 850f,
    val backgroundColorHex: Long = 0xFFFFFFFFL
)

/**
 * Протокол сообщений совместной работы в реальном времени.
 */
@Serializable
sealed class CollabAction {

    /**
     * Полный снимок доски (при подключении нового клиента).
     */
    @Serializable
    @SerialName("snapshot")
    data class Snapshot(
        val document: BoardDocument,
        val senderId: String,
        val senderName: String
    ) : CollabAction()

    /**
     * Добавление или обновление элемента (стикер, фигура, путь рисования, веха и т.д.).
     */
    @Serializable
    @SerialName("upsert_element")
    data class UpsertElement(
        val element: BoardElement,
        val senderId: String,
        val senderName: String
    ) : CollabAction()

    /**
     * Удаление одного или нескольких элементов.
     */
    @Serializable
    @SerialName("delete_elements")
    data class DeleteElements(
        val elementIds: List<String>,
        val senderId: String
    ) : CollabAction()

    /**
     * Перемещение курсора или лазерной указки участника.
     */
    @Serializable
    @SerialName("pointer_move")
    data class PointerMove(
        val senderId: String,
        val senderName: String,
        val x: Float,
        val y: Float,
        val colorHex: Long,
        val isLaser: Boolean
    ) : CollabAction()

    /**
     * Переключение чекбокса To-Do в стикере.
     */
    @Serializable
    @SerialName("toggle_sticky_todo")
    data class ToggleStickyTodo(
        val noteId: String,
        val lineIndex: Int,
        val senderId: String
    ) : CollabAction()

    /**
     * Уведомление о подключении нового участника.
     */
    @Serializable
    @SerialName("peer_joined")
    data class PeerJoined(
        val peer: CollabPeer
    ) : CollabAction()

    /**
     * Уведомление об отключении участника.
     */
    @Serializable
    @SerialName("peer_left")
    data class PeerLeft(
        val peerId: String
    ) : CollabAction()

    /**
     * Очистка доски.
     */
    @Serializable
    @SerialName("clear_board")
    data class ClearBoard(
        val senderId: String
    ) : CollabAction()

    /**
     * Панорамирование / перемещение холста ПК со смартфона (жест 2 пальцами).
     */
    @Serializable
    @SerialName("pan_viewport")
    data class PanViewport(
        val dx: Float,
        val dy: Float,
        val senderId: String
    ) : CollabAction()
}
