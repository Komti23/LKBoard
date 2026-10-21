package com.lkboard.app.data

import com.lkboard.app.model.BoardState
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Сервис сериализации и сохранения состояния доски в JSON (.lkboard).
 */
object BoardSerializer {

    val jsonFormat = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Сериализует документ в JSON-строку.
     */
    fun serialize(document: BoardDocument): String {
        return jsonFormat.encodeToString(document)
    }

    /**
     * Десериализует JSON-строку в документ.
     */
    fun deserialize(jsonString: String): BoardDocument {
        return jsonFormat.decodeFromString(jsonString)
    }

    /**
     * Преобразует рантайм-состояние доски [BoardState] в сохраняемый документ [BoardDocument].
     */
    fun stateToDocument(state: BoardState): BoardDocument {
        return BoardDocument(
            title = state.title,
            elements = state.sortedElements,
            transform = state.transform,
            gridType = state.gridType,
            backgroundColorHex = state.backgroundColorHex,
            penSettings = state.penSettings,
            shapeSettings = state.shapeSettings
        )
    }

    /**
     * Преобразует документ [BoardDocument] в рантайм-состояние [BoardState].
     */
    fun documentToState(document: BoardDocument): BoardState {
        val penSettings = document.penSettings ?: AppPreferences.loadPenSettings()
        val shapeSettings = document.shapeSettings ?: AppPreferences.loadShapeSettings()
        return BoardState(
            title = document.title,
            elements = document.elements.associateBy { it.id },
            transform = document.transform,
            gridType = document.gridType,
            backgroundColorHex = document.backgroundColorHex,
            penSettings = penSettings,
            shapeSettings = shapeSettings,
            selectedElementIds = emptySet()
        )
    }

    /**
     * Сохраняет состояние доски в файл.
     */
    fun save(file: File, state: BoardState) {
        val doc = stateToDocument(state)
        val json = serialize(doc)
        file.writeText(json, Charsets.UTF_8)
    }

    /**
     * Загружает состояние доски из файла.
     */
    fun load(file: File): BoardState {
        val json = file.readText(Charsets.UTF_8)
        val doc = deserialize(json)
        return documentToState(doc)
    }
}
