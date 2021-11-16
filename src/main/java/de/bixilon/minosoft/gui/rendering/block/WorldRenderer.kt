/*
 * Minosoft
 * Copyright (C) 2020 Moritz Zwerger, Lukas Eisenhauer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * This software is not affiliated with Mojang AB, the original developer of Minecraft.
 */

package de.bixilon.minosoft.gui.rendering.block

import de.bixilon.minosoft.data.assets.AssetsUtil
import de.bixilon.minosoft.data.assets.Resources
import de.bixilon.minosoft.data.registries.ResourceLocation
import de.bixilon.minosoft.data.world.Chunk
import de.bixilon.minosoft.data.world.ChunkSection
import de.bixilon.minosoft.data.world.World
import de.bixilon.minosoft.gui.rendering.RenderWindow
import de.bixilon.minosoft.gui.rendering.Renderer
import de.bixilon.minosoft.gui.rendering.RendererBuilder
import de.bixilon.minosoft.gui.rendering.block.mesh.ChunkSectionMesh
import de.bixilon.minosoft.gui.rendering.block.mesh.ChunkSectionMeshes
import de.bixilon.minosoft.gui.rendering.block.preparer.AbstractSectionPreparer
import de.bixilon.minosoft.gui.rendering.block.preparer.CullSectionPreparer
import de.bixilon.minosoft.gui.rendering.modding.events.FrustumChangeEvent
import de.bixilon.minosoft.gui.rendering.models.ModelLoader
import de.bixilon.minosoft.gui.rendering.system.base.RenderSystem
import de.bixilon.minosoft.gui.rendering.system.base.phases.OpaqueDrawable
import de.bixilon.minosoft.gui.rendering.system.base.phases.TranslucentDrawable
import de.bixilon.minosoft.gui.rendering.system.base.phases.TransparentDrawable
import de.bixilon.minosoft.gui.rendering.util.VecUtil.chunkPosition
import de.bixilon.minosoft.gui.rendering.util.VecUtil.empty
import de.bixilon.minosoft.gui.rendering.util.VecUtil.inChunkSectionPosition
import de.bixilon.minosoft.gui.rendering.util.VecUtil.of
import de.bixilon.minosoft.gui.rendering.util.VecUtil.sectionHeight
import de.bixilon.minosoft.gui.rendering.util.VecUtil.toVec3
import de.bixilon.minosoft.gui.rendering.util.vec.vec2.Vec2iUtil.EMPTY
import de.bixilon.minosoft.gui.rendering.util.vec.vec3.Vec3Util.EMPTY
import de.bixilon.minosoft.gui.rendering.util.vec.vec3.Vec3iUtil.EMPTY
import de.bixilon.minosoft.gui.rendering.util.vec.vec3.Vec3iUtil.toVec3
import de.bixilon.minosoft.modding.event.events.BlockSetEvent
import de.bixilon.minosoft.modding.event.events.ChunkDataChangeEvent
import de.bixilon.minosoft.modding.event.events.ChunkUnloadEvent
import de.bixilon.minosoft.modding.event.events.RespawnEvent
import de.bixilon.minosoft.modding.event.invoker.CallbackEventInvoker
import de.bixilon.minosoft.protocol.network.connection.play.PlayConnection
import de.bixilon.minosoft.protocol.protocol.ProtocolDefinition
import de.bixilon.minosoft.util.KUtil.synchronizedListOf
import de.bixilon.minosoft.util.KUtil.synchronizedSetOf
import de.bixilon.minosoft.util.KUtil.toResourceLocation
import de.bixilon.minosoft.util.KUtil.unsafeCast
import de.bixilon.minosoft.util.ReadWriteLock
import de.bixilon.minosoft.util.chunk.ChunkUtil
import de.bixilon.minosoft.util.chunk.ChunkUtil.isInRenderDistance
import de.bixilon.minosoft.util.task.pool.DefaultThreadPool
import de.bixilon.minosoft.util.task.pool.ThreadPool.Priorities.HIGH
import de.bixilon.minosoft.util.task.pool.ThreadPool.Priorities.LOW
import de.bixilon.minosoft.util.task.pool.ThreadPoolRunnable
import glm_.vec2.Vec2i
import glm_.vec3.Vec3
import glm_.vec3.Vec3i
import java.io.FileInputStream
import java.util.zip.GZIPInputStream
import java.util.zip.ZipInputStream

class WorldRenderer(
    private val connection: PlayConnection,
    override val renderWindow: RenderWindow,
) : Renderer, OpaqueDrawable, TranslucentDrawable, TransparentDrawable {
    override val renderSystem: RenderSystem = renderWindow.renderSystem
    private val frustum = renderWindow.inputHandler.camera.frustum
    private val shader = renderSystem.createShader("minosoft:world".toResourceLocation())
    private val transparentShader = renderSystem.createShader("minosoft:world".toResourceLocation())
    private val world: World = connection.world
    private val sectionPreparer: AbstractSectionPreparer = CullSectionPreparer(renderWindow)
    private val lightMap = LightMap(connection)

    private val loadedMeshes: MutableMap<Vec2i, MutableMap<Int, ChunkSectionMeshes>> = mutableMapOf() // all prepared (and up to date) meshes
    private val loadedMeshesLock = ReadWriteLock()

    val maxPreparingTasks = maxOf(DefaultThreadPool.threadCount - 1, 1)
    private val preparingTasks: MutableSet<SectionPrepareTask> = synchronizedSetOf() // current running section preparing tasks

    private val queue: MutableList<WorldQueueItem> = synchronizedListOf() // queue, that is visible, and should be rendered
    private val queueLock = ReadWriteLock()
    private val culledQueue: MutableMap<Vec2i, MutableSet<Int>> = mutableMapOf() // Chunk sections that can be prepared or have changed, but are not required to get rendered yet (i.e. culled chunks)
    private val culledQueueLock = ReadWriteLock()

    val maxMeshesToLoad = 100 // ToDo: Should depend on the system memory and other factors.
    private val meshesToLoad: MutableList<WorldQueueItem> = synchronizedListOf() // prepared meshes, that can be loaded in the (next) frame
    private val meshesToLoadLock = ReadWriteLock()
    private val meshesToUnload: MutableList<ChunkSectionMeshes> = synchronizedListOf() // prepared meshes, that can be loaded in the (next) frame
    private val meshesToUnloadLock = ReadWriteLock()

    // all meshes that will be rendered in the next frame (might be changed, when the frustum changes or a chunk gets loaded, ...)
    private var clearVisibleNextFrame = false
    private var visibleOpaque: MutableList<ChunkSectionMesh> = mutableListOf()
    private var visibleTranslucent: MutableList<ChunkSectionMesh> = mutableListOf()
    private var visibleTransparent: MutableList<ChunkSectionMesh> = mutableListOf()


    private var cameraPosition = Vec3.EMPTY
    private var cameraChunkPosition = Vec2i.EMPTY

    val visibleOpaqueSize: Int
        get() = visibleOpaque.size
    val visibleTranslucentSize: Int
        get() = visibleTranslucent.size
    val visibleTransparentSize: Int
        get() = visibleTransparent.size
    val loadedMeshesSize: Int by loadedMeshes::size
    val culledQueuedSize: Int by culledQueue::size
    val meshesToLoadSize: Int by meshesToLoad::size
    val queueSize: Int by queue::size
    val preparingTasksSize: Int by preparingTasks::size

    override fun init() {
        val asset = Resources.getAssetVersionByVersion(connection.version)
        val zip = ZipInputStream(GZIPInputStream(FileInputStream(AssetsUtil.getAssetDiskPath(asset.clientJarHash!!, true))))
        val modelLoader = ModelLoader(zip, renderWindow)
        modelLoader.load()
    }

    override fun postInit() {
        lightMap.init()

        shader.load()
        renderWindow.textureManager.staticTextures.use(shader)
        renderWindow.textureManager.staticTextures.animator.use(shader)
        lightMap.use(shader)

        transparentShader.defines["TRANSPARENT"] = ""
        transparentShader.load()
        renderWindow.textureManager.staticTextures.use(transparentShader)
        renderWindow.textureManager.staticTextures.animator.use(transparentShader)
        lightMap.use(transparentShader)


        lightMap.update()

        connection.registerEvent(CallbackEventInvoker.of<FrustumChangeEvent> { onFrustumChange() })

        connection.registerEvent(CallbackEventInvoker.of<RespawnEvent> { unloadWorld() })
        connection.registerEvent(CallbackEventInvoker.of<ChunkDataChangeEvent> { queueChunk(it.chunkPosition, it.chunk) })
        connection.registerEvent(CallbackEventInvoker.of<BlockSetEvent> {
            val chunkPosition = it.blockPosition.chunkPosition
            val sectionHeight = it.blockPosition.sectionHeight
            val chunk = world[chunkPosition] ?: return@of
            queueSection(chunkPosition, sectionHeight, chunk)
            val inChunkSectionPosition = it.blockPosition.inChunkSectionPosition

            if (inChunkSectionPosition.y == 0) {
                queueSection(chunkPosition, sectionHeight - 1, chunk)
            } else if (inChunkSectionPosition.y == ProtocolDefinition.SECTION_MAX_Y) {
                queueSection(chunkPosition, sectionHeight + 1, chunk)
            }
            if (inChunkSectionPosition.z == 0) {
                queueSection(Vec2i(chunkPosition.x, chunkPosition.y - 1), sectionHeight)
            } else if (inChunkSectionPosition.z == ProtocolDefinition.SECTION_MAX_Z) {
                queueSection(Vec2i(chunkPosition.x, chunkPosition.y + 1), sectionHeight)
            }
            if (inChunkSectionPosition.x == 0) {
                queueSection(Vec2i(chunkPosition.x - 1, chunkPosition.y), sectionHeight)
            } else if (inChunkSectionPosition.x == ProtocolDefinition.SECTION_MAX_X) {
                queueSection(Vec2i(chunkPosition.x + 1, chunkPosition.y), sectionHeight)
            }
        })
        /*
        connection.registerEvent(CallbackEventInvoker.of<MassBlockSetEvent> {
            val chunk = world[it.chunkPosition] ?: return@of
            if (!chunk.isFullyLoaded || it.chunkPosition in incomplete) {
                return@of
            }
            val neighbourChunks = world.getChunkNeighbours(it.chunkPosition)
            if (!neighbourChunks.fullyLoaded) {
                return@of
            }
            val sectionHeights: MutableSet<Int> = mutableSetOf()
            for (blockPosition in it.blocks.keys) {
                sectionHeights += blockPosition.sectionHeight
            }
            renderWindow.queue += {
                val meshes = loadedMeshes.getOrPut(it.chunkPosition) { mutableMapOf() }
                for (sectionHeight in sectionHeights) {
                    //   updateSection(it.chunkPosition, sectionHeight, chunk, neighbourChunks.unsafeCast(), meshes)
                }
            }
        })
         */
        connection.registerEvent(CallbackEventInvoker.of<ChunkUnloadEvent> { unloadChunk(it.chunkPosition) })
    }

    private fun unloadWorld() {
        queueLock.lock()
        culledQueueLock.lock()
        meshesToLoadLock.lock()
        loadedMeshesLock.lock()

        meshesToUnloadLock.lock()
        for (sections in loadedMeshes.values) {
            meshesToUnload += sections.values
        }
        meshesToUnloadLock.unlock()

        culledQueue.clear()
        loadedMeshes.clear()
        queue.clear()
        meshesToLoad.clear()

        clearVisibleNextFrame = true

        for (task in preparingTasks.toMutableSet()) {
            task.runnable.interrupt()
        }

        loadedMeshesLock.unlock()
        queueLock.unlock()
        culledQueueLock.unlock()
        meshesToLoadLock.unlock()
    }

    private fun unloadChunk(chunkPosition: Vec2i) {
        queueLock.lock()
        culledQueueLock.lock()
        meshesToLoadLock.lock()
        meshesToUnloadLock.lock()
        loadedMeshesLock.lock()
        val meshes = loadedMeshes.remove(chunkPosition)

        culledQueue.remove(chunkPosition)

        queue.removeAll { it.chunkPosition == chunkPosition }

        meshesToLoad.removeAll { it.chunkPosition == chunkPosition }

        for (task in preparingTasks.toMutableSet()) {
            if (task.chunkPosition == chunkPosition) {
                task.runnable.interrupt()
            }
        }
        if (meshes != null) {
            meshesToUnload += meshes.values
        }

        loadedMeshesLock.unlock()
        queueLock.unlock()
        culledQueueLock.unlock()
        meshesToLoadLock.unlock()
        meshesToUnloadLock.unlock()
    }

    private fun removeMesh(mesh: ChunkSectionMeshes) {
        mesh.opaqueMesh?.let { visibleOpaque -= it }
        mesh.translucentMesh?.let { visibleTranslucent -= it }
        mesh.transparentMesh?.let { visibleTransparent -= it }
    }

    private fun sortQueue() {
        queueLock.lock()
        queue.sortBy { (it.center - cameraPosition).length2() }
        queueLock.unlock()
    }

    private fun workQueue() {
        // ToDo: Prepare some of the culledChunks if nothing todo
        val size = preparingTasks.size
        if (size >= maxPreparingTasks && queue.isNotEmpty() || meshesToLoad.size >= maxMeshesToLoad) {
            return
        }
        val items: MutableList<WorldQueueItem> = mutableListOf()
        queueLock.lock()
        for (i in 0 until maxPreparingTasks - size) {
            if (queue.size == 0) {
                break
            }
            items += queue.removeAt(0)
        }
        queueLock.unlock()
        for (item in items) {
            val task = SectionPrepareTask(item.chunkPosition, ThreadPoolRunnable(if (item.chunkPosition == cameraChunkPosition) HIGH else LOW)) // Our own chunk is the most important one ToDo: Also make neighbour chunks important
            task.runnable.runnable = Runnable {

                fun end() {
                    preparingTasks -= task
                    workQueue()
                }

                var locked = false
                try {
                    val chunk = item.chunk ?: world[item.chunkPosition] ?: return@Runnable end()
                    val section = chunk[item.sectionHeight] ?: return@Runnable end()
                    val neighbours = item.neighbours ?: ChunkUtil.getSectionNeighbours(world.getChunkNeighbours(item.chunkPosition).unsafeCast(), chunk, item.sectionHeight)
                    item.mesh = sectionPreparer.prepare(item.chunkPosition, item.sectionHeight, section, neighbours)
                    meshesToLoadLock.lock()
                    locked = true
                    meshesToLoad.removeAll { it == item } // Remove duplicates
                    if (item.chunkPosition == cameraChunkPosition) {
                        // still higher priority
                        meshesToLoad.add(0, item)
                    } else {
                        meshesToLoad += item
                    }
                    meshesToLoadLock.unlock()
                } catch (exception: Throwable) {
                    if (locked) {
                        meshesToLoadLock.unlock()
                    }
                    if (exception is InterruptedException) {
                        // task got interrupted (probably because of chunk unload)
                        preparingTasks -= task
                        return@Runnable
                    }
                    throw exception
                }
                end()
            }
            preparingTasks += task
            DefaultThreadPool += task.runnable
        }
    }

    private fun internalQueueSection(chunkPosition: Vec2i, sectionHeight: Int, chunk: Chunk, section: ChunkSection, ignoreFrustum: Boolean): Boolean {
        if (!chunk.isFullyLoaded || section.blocks.isEmpty) {
            return false
        }

        val visible = ignoreFrustum || isChunkVisible(chunkPosition, sectionHeight, section.blocks.minPosition, section.blocks.maxPosition)
        if (visible) {
            val item = WorldQueueItem(chunkPosition, sectionHeight, chunk, section, Vec3i.of(chunkPosition, sectionHeight, Vec3i(8, 8, 8)).toVec3(), null)
            queueLock.lock()
            queue.removeAll { it == item } // Prevent duplicated entries (to not prepare the same chunk twice (if it changed and was not prepared yet or ...)
            if (chunkPosition == cameraChunkPosition) {
                queue.add(0, item)
            } else {
                queue += item
            }
            queueLock.unlock()
            return true
        } else {
            culledQueueLock.lock()
            culledQueue.getOrPut(chunkPosition) { mutableSetOf() } += sectionHeight
            culledQueueLock.unlock()
        }
        return false
    }

    private fun queueSection(chunkPosition: Vec2i, sectionHeight: Int, chunk: Chunk? = world.chunks[chunkPosition], section: ChunkSection? = chunk?.get(sectionHeight), ignoreFrustum: Boolean = false) {
        if (chunk == null || section == null) {
            return
        }
        val queued = internalQueueSection(chunkPosition, sectionHeight, chunk, section, ignoreFrustum)

        if (queued) {
            sortQueue()
            workQueue()
        }
    }

    private fun queueChunk(chunkPosition: Vec2i, chunk: Chunk = world.chunks[chunkPosition]!!) {
        if (!chunk.isFullyLoaded) {
            return
        }

        var queueChanges = 0
        for (sectionHeight in chunk.lowestSection until chunk.highestSection) {
            val section = chunk[sectionHeight] ?: continue
            val queued = internalQueueSection(chunkPosition, sectionHeight, chunk, section, false)
            if (queued) {
                queueChanges++
            }
        }
        if (queueChanges > 0) {
            sortQueue()
            workQueue()
        }
    }

    private fun loadMeshes() {
        meshesToLoadLock.acquire()
        if (meshesToLoad.isEmpty()) {
            meshesToLoadLock.release()
            return
        }

        val time = System.currentTimeMillis()
        val maxTime = if (connection.player.velocity.empty) 50L else 20L // If the player is still, then we can load more chunks (to not cause lags)

        while ((System.currentTimeMillis() - time < maxTime) && meshesToLoad.isNotEmpty()) {
            val item = meshesToLoad.removeAt(0)
            val mesh = item.mesh ?: throw IllegalStateException("Mesh of queued item is null!")
            mesh.load()
            val visible = isChunkVisible(item.chunkPosition, item.sectionHeight, mesh.minPosition, mesh.maxPosition)
            if (visible) {
                mesh.opaqueMesh?.let { visibleOpaque += it }
                mesh.translucentMesh?.let { visibleTranslucent += it }
                mesh.transparentMesh?.let { visibleTransparent += it }
            }
            loadedMeshesLock.lock()
            val meshes = loadedMeshes.getOrPut(item.chunkPosition) { mutableMapOf() }

            meshes.put(item.sectionHeight, mesh)?.let {
                removeMesh(it)
                it.unload()
            }
            loadedMeshesLock.unlock()
        }
        meshesToLoadLock.release()
    }

    private fun unloadMeshes() {
        meshesToUnloadLock.acquire()
        if (meshesToUnload.isEmpty()) {
            meshesToUnloadLock.release()
            return
        }

        val time = System.currentTimeMillis()
        val maxTime = if (connection.player.velocity.empty) 50L else 20L // If the player is still, then we can load more chunks (to not cause lags)

        while ((System.currentTimeMillis() - time < maxTime) && meshesToUnload.isNotEmpty()) {
            val mesh = meshesToUnload.removeAt(0)
            removeMesh(mesh)
            mesh.unload()
        }
        meshesToUnloadLock.release()
    }

    override fun prepareDraw() {
        if (clearVisibleNextFrame) {
            visibleOpaque.clear()
            visibleTranslucent.clear()
            visibleTransparent.clear()
            clearVisibleNextFrame = false
        }
        unloadMeshes()
        loadMeshes()
    }

    override fun setupOpaque() {
        super.setupOpaque()
        shader.use()
    }

    override fun drawOpaque() {
        for (mesh in visibleOpaque) {
            mesh.draw()
        }
    }

    override fun setupTranslucent() {
        super.setupTranslucent()
        shader.use()
    }

    override fun drawTranslucent() {
        for (mesh in visibleTranslucent) {
            mesh.draw()
        }
    }

    override fun setupTransparent() {
        super.setupTransparent()
        transparentShader.use()
    }

    override fun drawTransparent() {
        for (mesh in visibleTransparent) {
            mesh.draw()
        }
    }

    private fun isChunkVisible(chunkPosition: Vec2i, sectionHeight: Int, minPosition: Vec3i, maxPosition: Vec3i): Boolean {
        if (!chunkPosition.isInRenderDistance(cameraChunkPosition)) {
            return false
        }
        // ToDo: Cave culling, frustum clipping, improve performance
        return frustum.containsChunk(chunkPosition, sectionHeight, minPosition, maxPosition)
    }

    private fun onFrustumChange() {
        val visibleOpaque: MutableList<ChunkSectionMesh> = mutableListOf()
        val visibleTranslucent: MutableList<ChunkSectionMesh> = mutableListOf()
        val visibleTransparent: MutableList<ChunkSectionMesh> = mutableListOf()

        loadedMeshesLock.acquire()
        for ((chunkPosition, meshes) in this.loadedMeshes) {
            for ((sectionHeight, mesh) in meshes) {
                if (!isChunkVisible(chunkPosition, sectionHeight, mesh.minPosition, mesh.maxPosition)) {
                    continue
                }
                mesh.opaqueMesh?.let { visibleOpaque += it }
                mesh.translucentMesh?.let { visibleTranslucent += it }
                mesh.transparentMesh?.let { visibleTransparent += it }
            }
        }
        loadedMeshesLock.release()

        culledQueueLock.acquire()
        val queue: MutableMap<Vec2i, MutableSet<Int>> = mutableMapOf() // The queue method needs the full lock of the culledQueue
        for ((chunkPosition, sectionHeights) in this.culledQueue) {
            var chunkQueue: MutableSet<Int>? = null
            for (sectionHeight in sectionHeights) {
                if (!isChunkVisible(chunkPosition, sectionHeight, Vec3i.EMPTY, Vec3i(16))) {
                    continue
                }
                if (chunkQueue == null) {
                    chunkQueue = queue.getOrPut(chunkPosition) { mutableSetOf() }
                }
                chunkQueue += sectionHeight
            }
        }
        culledQueueLock.release()


        for ((chunkPosition, sectionHeights) in queue) {
            for (sectionHeight in sectionHeights) {
                queueSection(chunkPosition, sectionHeight, ignoreFrustum = true)
            }
        }
        if (queue.isNotEmpty()) {
            sortQueue()
            workQueue()
        }

        culledQueueLock.acquire()
        for ((chunkPosition, sectionHeights) in queue) {
            val originalSectionHeight = this.culledQueue[chunkPosition] ?: continue
            originalSectionHeight -= sectionHeights
            if (originalSectionHeight.isEmpty()) {
                this.culledQueue -= chunkPosition
            }
        }
        culledQueueLock.release()


        val cameraPosition = connection.player.cameraPosition.toVec3

        visibleOpaque.sortBy { (it.center - cameraPosition).length2() }
        this.visibleOpaque = visibleOpaque

        visibleTranslucent.sortBy { -(it.center - cameraPosition).length2() }
        this.visibleTranslucent = visibleTranslucent

        visibleTransparent.sortBy { (it.center - cameraPosition).length2() }
        this.visibleTransparent = visibleTransparent

        if (this.cameraPosition != cameraPosition) {
            this.cameraPosition = cameraPosition
            this.cameraChunkPosition = connection.player.positionInfo.chunkPosition
            sortQueue()
        }
    }


    companion object : RendererBuilder<WorldRenderer> {
        override val RESOURCE_LOCATION = ResourceLocation("minosoft:world_renderer")

        override fun build(connection: PlayConnection, renderWindow: RenderWindow): WorldRenderer {
            return WorldRenderer(connection, renderWindow)
        }
    }
}
