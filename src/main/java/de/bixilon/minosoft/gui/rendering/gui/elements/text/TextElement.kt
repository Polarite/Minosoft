/*
 * Minosoft
 * Copyright (C) 2021 Moritz Zwerger
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * This software is not affiliated with Mojang AB, the original developer of Minecraft.
 */

package de.bixilon.minosoft.gui.rendering.gui.elements.text

import de.bixilon.minosoft.data.text.ChatComponent
import de.bixilon.minosoft.data.text.RGBColor
import de.bixilon.minosoft.gui.rendering.RenderConstants
import de.bixilon.minosoft.gui.rendering.font.Font
import de.bixilon.minosoft.gui.rendering.font.renderer.ChatComponentRenderer
import de.bixilon.minosoft.gui.rendering.font.renderer.TextRenderInfo
import de.bixilon.minosoft.gui.rendering.gui.elements.Element
import de.bixilon.minosoft.gui.rendering.gui.elements.HorizontalAlignments
import de.bixilon.minosoft.gui.rendering.gui.elements.HorizontalAlignments.Companion.getOffset
import de.bixilon.minosoft.gui.rendering.gui.elements.InfiniteSizeElement
import de.bixilon.minosoft.gui.rendering.gui.hud.HUDRenderer
import de.bixilon.minosoft.gui.rendering.gui.mesh.GUIVertexConsumer
import de.bixilon.minosoft.gui.rendering.util.vec.Vec2Util.EMPTY
import de.bixilon.minosoft.gui.rendering.util.vec.Vec4Util.offset
import glm_.vec2.Vec2i

open class TextElement(
    hudRenderer: HUDRenderer,
    text: Any,
    override var fontAlignment: HorizontalAlignments = HorizontalAlignments.LEFT,
    var background: Boolean = true,
    var backgroundColor: RGBColor = RenderConstants.TEXT_BACKGROUND_COLOR,
    parent: Element? = null,
) : LabeledElement(hudRenderer) {
    private var previousMaxSize = Vec2i.EMPTY
    private var preparedSize = Vec2i.EMPTY
    var renderInfo = TextRenderInfo()

    override var text: Any = text
        set(value) {
            textComponent = ChatComponent.of(value)
            field = value
        }

    private var emptyMessage: Boolean = true

    override var textComponent: ChatComponent = ChatComponent.of("")
        protected set(value) {
            field = value
            emptyMessage = value.message.isEmpty()
            val prefSize = Vec2i.EMPTY
            if (!emptyMessage) {
                ChatComponentRenderer.render(Vec2i.EMPTY, Vec2i.EMPTY, prefSize, 0, InfiniteSizeElement(hudRenderer), fontAlignment, renderWindow, null, TextRenderInfo(), value)
            }
            this.prefSize = prefSize
            cacheUpToDate = false
            apply()
        }

    override var prefSize: Vec2i = Vec2i.EMPTY

    init {
        this.parent = parent
        textComponent = ChatComponent.of(text)
    }

    override fun silentApply() {
        val maxSize = maxSize
        val size = Vec2i.EMPTY
        if (!emptyMessage) {
            val renderInfo = TextRenderInfo()
            ChatComponentRenderer.render(Vec2i.EMPTY, Vec2i.EMPTY, size, 0, this, fontAlignment, renderWindow, null, renderInfo, textComponent)
            renderInfo.currentLineNumber = 0
            this.renderInfo = renderInfo
        }


        this.previousMaxSize = maxSize
        this.cacheUpToDate = false
        this.size = size
        preparedSize = size
    }

    override fun onChildChange(child: Element?) = error("A TextElement can not have a child!")

    override fun checkSilentApply() {
        val maxSize = maxSize
        if (previousMaxSize == maxSize) {
            // no change in size
            return
        }
        val prefSize = prefSize

        if (preparedSize.x < prefSize.x || preparedSize.x > maxSize.x) {
            return silentApply()
        }
        if (preparedSize.y < prefSize.y || preparedSize.y > maxSize.y) {
            return silentApply()
        }
    }


    override fun forceRender(offset: Vec2i, z: Int, consumer: GUIVertexConsumer): Int {
        if (emptyMessage) {
            return 0
        }
        val initialOffset = offset + margin.offset

        ChatComponentRenderer.render(initialOffset, Vec2i(initialOffset), Vec2i.EMPTY, z + 1, this, fontAlignment, renderWindow, consumer, renderInfo, textComponent)
        renderInfo.currentLineNumber = 0

        if (background) {
            for ((line, info) in renderInfo.lines.withIndex()) {
                val start = initialOffset + Vec2i(fontAlignment.getOffset(size.x, info.width), line * Font.TOTAL_CHAR_HEIGHT)
                consumer.addQuad(start, start + Vec2i(info.width + Font.CHAR_MARGIN, Font.TOTAL_CHAR_HEIGHT), z, renderWindow.WHITE_TEXTURE, backgroundColor)
            }
        }

        return LAYERS
    }

    override fun toString(): String {
        return textComponent.toString()
    }

    companion object {
        const val LAYERS = 5 // 1 layer for the text, 1 for strikethrough, * 2 for shadow, 1 for background
    }
}
