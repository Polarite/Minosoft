package de.bixilon.minosoft.gui.rendering.block.preparer

import de.bixilon.minosoft.data.direction.Directions
import de.bixilon.minosoft.data.registries.blocks.BlockState
import de.bixilon.minosoft.data.world.Chunk
import de.bixilon.minosoft.data.world.ChunkSection
import de.bixilon.minosoft.gui.rendering.RenderWindow
import de.bixilon.minosoft.gui.rendering.block.mesh.ChunkSectionMeshes
import de.bixilon.minosoft.gui.rendering.util.VecUtil
import de.bixilon.minosoft.protocol.protocol.ProtocolDefinition
import de.bixilon.minosoft.util.chunk.ChunkUtil.acquire
import de.bixilon.minosoft.util.chunk.ChunkUtil.release
import glm_.vec2.Vec2i
import glm_.vec3.Vec3i
import java.util.*

class CullSectionPreparer(
    val renderWindow: RenderWindow,
) : AbstractSectionPreparer {
    private val tintColorCalculator = renderWindow.tintManager
    private val ambientLight = floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f)

    override fun prepare(chunkPosition: Vec2i, sectionHeight: Int, chunk: Chunk, section: ChunkSection, neighbours: Array<ChunkSection?>, neighbourChunks: Array<Chunk>): ChunkSectionMeshes {
        val mesh = ChunkSectionMeshes(renderWindow, chunkPosition, sectionHeight)
        val random = Random(0L)

        val blocks = section.blocks
        section.acquire()
        neighbours.acquire()
        var blockState: BlockState?
        var position: Vec3i
        val neighbourBlocks: Array<BlockState?> = arrayOfNulls(Directions.SIZE)

        val offsetX = chunkPosition.x * ProtocolDefinition.SECTION_WIDTH_X
        val offsetY = sectionHeight * ProtocolDefinition.SECTION_HEIGHT_Y
        val offsetZ = chunkPosition.y * ProtocolDefinition.SECTION_WIDTH_Z

        for (x in 0 until ProtocolDefinition.SECTION_WIDTH_X) {
            for (y in 0 until ProtocolDefinition.SECTION_HEIGHT_Y) {
                for (z in 0 until ProtocolDefinition.SECTION_WIDTH_Z) {
                    blockState = blocks.unsafeGet(x, y, z)
                    val model = blockState?.model ?: continue

                    // ToDo: Chunk borders
                    neighbourBlocks[Directions.DOWN.ordinal] = if (y == 0) {
                        neighbours[Directions.DOWN.ordinal]?.blocks?.unsafeGet(x, ProtocolDefinition.SECTION_MAX_Y, z)
                    } else {
                        blocks.unsafeGet(x, y - 1, z)
                    }
                    neighbourBlocks[Directions.UP.ordinal] = if (y == ProtocolDefinition.SECTION_MAX_Y) {
                        neighbours[Directions.UP.ordinal]?.blocks?.unsafeGet(x, 0, z)
                    } else {
                        blocks.unsafeGet(x, y + 1, z)
                    }

                    neighbourBlocks[Directions.NORTH.ordinal] = if (z == 0) {
                        neighbours[Directions.NORTH.ordinal]?.blocks?.unsafeGet(x, y, ProtocolDefinition.SECTION_MAX_Z)
                    } else {
                        blocks.unsafeGet(x, y, z - 1)
                    }
                    neighbourBlocks[Directions.SOUTH.ordinal] = if (z == ProtocolDefinition.SECTION_MAX_Z) {
                        neighbours[Directions.SOUTH.ordinal]?.blocks?.unsafeGet(x, y, 0)
                    } else {
                        blocks.unsafeGet(x, y, z + 1)
                    }

                    neighbourBlocks[Directions.WEST.ordinal] = if (x == 0) {
                        neighbours[Directions.WEST.ordinal]?.blocks?.unsafeGet(ProtocolDefinition.SECTION_MAX_X, y, z)
                    } else {
                        blocks.unsafeGet(x - 1, y, z)
                    }
                    neighbourBlocks[Directions.EAST.ordinal] = if (x == ProtocolDefinition.SECTION_MAX_X) {
                        neighbours[Directions.EAST.ordinal]?.blocks?.unsafeGet(0, y, z)
                    } else {
                        blocks.unsafeGet(x + 1, y, z)
                    }

                    position = Vec3i(offsetX + x, offsetY + y, offsetZ + z)
                    random.setSeed(VecUtil.generatePositionHash(position.x, position.y, position.z))
                    val tints: IntArray? = tintColorCalculator.getAverageTint(chunk, neighbourChunks, blockState, x, y, z)
                    val rendered = model.singleRender(position, mesh, random, blockState, neighbourBlocks, 0xFF, ambientLight, tints)
                    if (rendered) {
                        mesh.addBlock(x, y, z)
                    }
                }
            }
        }
        section.release()
        neighbours.release()


        return mesh
    }
}
