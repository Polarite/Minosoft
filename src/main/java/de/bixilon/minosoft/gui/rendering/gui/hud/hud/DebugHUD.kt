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

package de.bixilon.minosoft.gui.rendering.gui.hud.hud

import de.bixilon.minosoft.Minosoft
import de.bixilon.minosoft.data.abilities.Gamemodes
import de.bixilon.minosoft.data.direction.Directions
import de.bixilon.minosoft.data.registries.other.game.event.handlers.GameMoveChangeGameEventHandler
import de.bixilon.minosoft.data.text.BaseComponent
import de.bixilon.minosoft.data.text.ChatColors
import de.bixilon.minosoft.data.text.TextComponent
import de.bixilon.minosoft.data.world.Chunk
import de.bixilon.minosoft.gui.rendering.RenderWindow
import de.bixilon.minosoft.gui.rendering.block.WorldRenderer
import de.bixilon.minosoft.gui.rendering.gui.elements.Element
import de.bixilon.minosoft.gui.rendering.gui.elements.ElementAlignments
import de.bixilon.minosoft.gui.rendering.gui.elements.layout.RowLayout
import de.bixilon.minosoft.gui.rendering.gui.elements.layout.grid.GridGrow
import de.bixilon.minosoft.gui.rendering.gui.elements.layout.grid.GridLayout
import de.bixilon.minosoft.gui.rendering.gui.elements.spacer.LineSpacerElement
import de.bixilon.minosoft.gui.rendering.gui.elements.text.AutoTextElement
import de.bixilon.minosoft.gui.rendering.gui.elements.text.TextElement
import de.bixilon.minosoft.gui.rendering.gui.hud.HUDRenderer
import de.bixilon.minosoft.gui.rendering.modding.events.ResizeWindowEvent
import de.bixilon.minosoft.gui.rendering.particle.ParticleRenderer
import de.bixilon.minosoft.modding.event.events.DifficultyChangeEvent
import de.bixilon.minosoft.modding.event.events.GameEventChangeEvent
import de.bixilon.minosoft.modding.event.events.TimeChangeEvent
import de.bixilon.minosoft.modding.event.invoker.CallbackEventInvoker
import de.bixilon.minosoft.modding.loading.ModLoader
import de.bixilon.minosoft.protocol.protocol.ProtocolDefinition
import de.bixilon.minosoft.terminal.RunConfiguration
import de.bixilon.minosoft.util.GitInfo
import de.bixilon.minosoft.util.KUtil.format
import de.bixilon.minosoft.util.MMath.round10
import de.bixilon.minosoft.util.SystemInformation
import de.bixilon.minosoft.util.UnitFormatter.formatBytes
import glm_.vec2.Vec2i
import glm_.vec4.Vec4i
import kotlin.math.abs

class DebugHUD(val hudRenderer: HUDRenderer) : HUD<GridLayout> {
    override val renderWindow: RenderWindow = hudRenderer.renderWindow
    private val connection = renderWindow.connection
    override val layout = GridLayout(hudRenderer, Vec2i(3, 1)).apply {
        columnConstraints[0].apply {
            grow = GridGrow.NEVER
        }
        columnConstraints[2].apply {
            grow = GridGrow.NEVER
            alignment = ElementAlignments.RIGHT
        }

        apply()
    }


    override fun init() {
        layout[Vec2i(0, 0)] = initLeft()
        layout[Vec2i(2, 0)] = initRight()
    }

    private fun initLeft(): Element {
        val layout = RowLayout(hudRenderer)
        layout.margin = Vec4i(2)
        layout += TextElement(hudRenderer, TextComponent(RunConfiguration.VERSION_STRING, ChatColors.RED))
        layout += AutoTextElement(hudRenderer, 1) { "FPS ${renderWindow.renderStats.smoothAvgFPS.round10}" }
        renderWindow[WorldRenderer]?.apply {
            layout += AutoTextElement(hudRenderer, 1) { "C v=${visibleChunks.size}, p=${allChunkSections.size}, q=${queuedChunks.size}, t=${connection.world.chunks.size}" }
        }
        layout += AutoTextElement(hudRenderer, 1) { "E t=${connection.world.entities.size}" }

        renderWindow[ParticleRenderer]?.apply {
            layout += AutoTextElement(hudRenderer, 1) { "P t=$size" }
        }

        if (Minosoft.config.config.game.sound.enabled) {
            layout += AutoTextElement(hudRenderer, 1) {
                BaseComponent().apply {
                    this += "S "

                    val audioPlayer = renderWindow.rendering.audioPlayer

                    this += audioPlayer.availableSources
                    this += " / "
                    this += audioPlayer.sourcesCount
                }
            }
        }

        layout += LineSpacerElement(hudRenderer)

        layout += TextElement(hudRenderer, BaseComponent("Account ", connection.account.username))
        layout += TextElement(hudRenderer, BaseComponent("Address ", connection.address))
        layout += TextElement(hudRenderer, BaseComponent("Network version ", connection.version))
        layout += TextElement(hudRenderer, BaseComponent("Server brand ", connection.serverInfo.brand))

        layout += LineSpacerElement(hudRenderer)


        connection.player.apply {
            // ToDo: Only update when the position changesEntityMoveAndRotateS2CP
            layout += AutoTextElement(hudRenderer, 1) { with(position) { "XYZ ${x.format()} / ${y.format()} / ${z.format()}" } }
            layout += AutoTextElement(hudRenderer, 1) { with(positionInfo.blockPosition) { "Block $x $y $z" } }
            layout += AutoTextElement(hudRenderer, 1) { with(positionInfo) { "Chunk $inChunkSectionPosition in (${chunkPosition.x} $sectionHeight ${chunkPosition.y})" } }
            layout += AutoTextElement(hudRenderer, 1) {
                val text = BaseComponent("Facing ")

                Directions.byDirection(hudRenderer.renderWindow.inputHandler.camera.cameraFront).apply {
                    text += this
                    text += " "
                    text += vector
                }

                hudRenderer.renderWindow.connection.player.rotation.apply {
                    text += " yaw="
                    text += headYaw.round10
                    text += ", pitch="
                    text += pitch.round10
                }

                text
            }
        }

        layout += LineSpacerElement(hudRenderer)

        val chunk = connection.world[connection.player.positionInfo.chunkPosition]

        if (chunk == null) {
            layout += DebugWorldInfo(hudRenderer)
        }

        layout += LineSpacerElement(hudRenderer)

        layout += TextElement(hudRenderer, BaseComponent("Gamemode ", connection.player.gamemode)).apply {
            connection.registerEvent(CallbackEventInvoker.of<GameEventChangeEvent> {
                if (it.event.resourceLocation != GameMoveChangeGameEventHandler.RESOURCE_LOCATION) {
                    return@of
                }
                // ToDo: Improve game mode change event
                text = BaseComponent("Gamemode ", Gamemodes[it.data.toInt()])
            })
        }

        layout += TextElement(hudRenderer, BaseComponent("Difficulty ", connection.world.difficulty, ", locked=", connection.world.difficultyLocked)).apply {
            connection.registerEvent(CallbackEventInvoker.of<DifficultyChangeEvent> {
                text = BaseComponent("Difficulty ", it.difficulty, ", locked=", it.locked)
            })
        }

        layout += TextElement(hudRenderer, "Time TBA").apply {
            connection.registerEvent(CallbackEventInvoker.of<TimeChangeEvent> {
                text = BaseComponent("Time ", abs(it.time % ProtocolDefinition.TICKS_PER_DAY), ", moving=", it.time >= 0, ", day=", abs(it.age) / ProtocolDefinition.TICKS_PER_DAY)
            })
        }

        return layout
    }

    private fun initRight(): Element {
        val layout = RowLayout(hudRenderer, ElementAlignments.RIGHT)
        layout.margin = Vec4i(2)
        layout += TextElement(hudRenderer, "Java ${Runtime.version()} ${System.getProperty("sun.arch.data.model")}bit", ElementAlignments.RIGHT)
        layout += TextElement(hudRenderer, "OS ${SystemInformation.OS_TEXT}", ElementAlignments.RIGHT)

        layout += LineSpacerElement(hudRenderer)

        SystemInformation.RUNTIME.apply {
            layout += AutoTextElement(hudRenderer, 1) {
                val total = maxMemory()
                val used = totalMemory() - freeMemory()
                "Memory ${(used * 100.0 / total).round10}% ${used.formatBytes()} / ${total.formatBytes()}"
            }
            layout += AutoTextElement(hudRenderer, 1) {
                val total = maxMemory()
                val allocated = totalMemory()
                "Allocated ${(allocated * 100.0 / total).round10}% ${allocated.formatBytes()} / ${total.formatBytes()}"
            }
        }

        layout += LineSpacerElement(hudRenderer)

        layout += TextElement(hudRenderer, "CPU ${SystemInformation.PROCESSOR_TEXT}", ElementAlignments.RIGHT)
        layout += TextElement(hudRenderer, "Memory ${SystemInformation.SYSTEM_MEMORY.formatBytes()}")


        layout += LineSpacerElement(hudRenderer)

        layout += TextElement(hudRenderer, "Display TBA", ElementAlignments.RIGHT).apply {
            hudRenderer.connection.registerEvent(CallbackEventInvoker.of<ResizeWindowEvent> {
                text = "Display ${it.size.x}x${it.size.y}"
            })
        }

        renderWindow.renderSystem.apply {
            layout += TextElement(hudRenderer, "GPU $gpuType", ElementAlignments.RIGHT)
            layout += TextElement(hudRenderer, "Version $version", ElementAlignments.RIGHT)
        }

        if (GitInfo.IS_INITIALIZED) {
            layout += LineSpacerElement(hudRenderer)

            GitInfo.apply {
                layout += TextElement(hudRenderer, "Git $GIT_COMMIT_ID_ABBREV: $GIT_COMMIT_MESSAGE_SHORT", ElementAlignments.RIGHT)
            }
        }

        layout += LineSpacerElement(hudRenderer)

        layout += TextElement(hudRenderer, "Mods ${ModLoader.MOD_MAP.size}x loaded, ${connection.size}x listeners", ElementAlignments.RIGHT)

        layout += LineSpacerElement(hudRenderer)

        renderWindow.inputHandler.camera.apply {
            layout += AutoTextElement(hudRenderer, 1, ElementAlignments.RIGHT) {
                // ToDo: Tags
                target ?: return@AutoTextElement ""
            }
        }
        return layout
    }

    private class DebugWorldInfo(hudRenderer: HUDRenderer) : RowLayout(hudRenderer) {
        private var lastChunk: Chunk? = null
        private val world = hudRenderer.connection.world
        private val entity = hudRenderer.connection.player

        init {
            showWait()
        }

        private fun showWait() {
            clear()
            this += TextElement(hudRenderer, "Waiting for chunk...")
        }

        private fun updateInformation() {
            entity.positionInfo.apply {
                val chunk = world[chunkPosition]

                if ((chunk == null && lastChunk == null) || (chunk != null && lastChunk != null)) {
                    // No update, elements will update themselves
                    return
                }
                if (chunk == null) {
                    lastChunk = null
                    showWait()
                    return
                }
                clear()

                this@DebugWorldInfo += AutoTextElement(hudRenderer, 1) { BaseComponent("Dimension ", connection.world.dimension?.resourceLocation) }
                this@DebugWorldInfo += AutoTextElement(hudRenderer, 1) { BaseComponent("Biome ", connection.world.getBiome(blockPosition)) }
                this@DebugWorldInfo += AutoTextElement(hudRenderer, 1) { with(connection.world.worldLightAccessor) { BaseComponent("Light block=", getBlockLight(blockPosition), ", sky=", getSkyLight(blockPosition)) } }

                lastChunk = chunk
            }
        }

        override fun tick() {
            // ToDo: Make event driven
            updateInformation()

            super.tick()
        }
    }
}
