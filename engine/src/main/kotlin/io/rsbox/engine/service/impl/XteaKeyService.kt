package io.rsbox.engine.service.impl

import com.google.gson.Gson
import io.rsbox.engine.Server
import io.rsbox.engine.model.world.World
import io.rsbox.engine.service.Service
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import mu.KotlinLogging
import net.runelite.cache.IndexType
import org.apache.commons.io.FilenameUtils
import java.io.FileNotFoundException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * @author Kyle Escobar
 */

class XteaKeyService : Service() {

    private val keys = Int2ObjectOpenHashMap<IntArray>()

    val validRegions: IntArray
        get() = keys.keys.toIntArray()

    override fun onStart(server: Server) {
        val path = Paths.get("rsbox/data/xteas/")

        if(!Files.exists(path)) {
            throw FileNotFoundException("Path does not exist. $path")
        }

        val singleFile = path.resolve("xteas.json")
        if(Files.exists(singleFile)) {
            loadSingleFile(singleFile)
        } else {
            loadDirectory(path)
        }

        loadKeys(server.world)
    }

    override fun onStop(server: Server) {

    }

    private fun loadSingleFile(path: Path) {
        val reader = Files.newBufferedReader(path)
        val xteas = Gson().fromJson(reader, Array<XteaFile>::class.java)
        reader.close()

        xteas?.forEach { xtea ->
            keys[xtea.region] = xtea.keys
        }
    }

    private fun loadDirectory(path: Path) {
        Files.list(path).forEach { list ->
            val region = FilenameUtils.removeExtension(list.fileName.toString()).toInt()
            val keys = IntArray(4)
            Files.newBufferedReader(list).useLines { lines ->
                lines.forEachIndexed { index, line ->
                    val key = line.toInt()
                    keys[index] = key
                }
            }
            this.keys[region] = keys
        }
    }

    private fun loadKeys(world: World) {
        /*
         * Get the total amount of valid regions and which keys we are missing.
         */
        val maxRegions = Short.MAX_VALUE
        var totalRegions = 0
        val missingKeys = mutableListOf<Int>()

        val regionIndex = world.cacheStore.getIndex(IndexType.MAPS)
        for (regionId in 0 until maxRegions) {
            val x = regionId shr 8
            val z = regionId and 0xFF

            /*
             * Check if the region corresponding to the x and z can be
             * found in our cache.
             */
            regionIndex.findArchiveByName("m${x}_$z") ?: continue
            regionIndex.findArchiveByName("l${x}_$z") ?: continue

            /*
             * The region was found in the regionIndex.
             */
            totalRegions++

            /*
             * If the XTEA is not found in our xteaService, we know the keys
             * are missing.
             */
            if (get(regionId).contentEquals(EMPTY_KEYS)) {
                missingKeys.add(regionId)
            }
        }

        /*
         * Set the XTEA service for the [World].
         */
        world.xteaKeyService = this

        val validKeys = totalRegions - missingKeys.size
        logger.info("Loaded {} / {} ({}%) XTEA keys.", validKeys, totalRegions,
            String.format("%.2f", (validKeys.toDouble() * 100.0) / totalRegions.toDouble()))
    }

    fun get(region: Int): IntArray {
        if (keys[region] == null) {
            logger.trace { "No XTEA keys found for region $region." }
            keys[region] = EMPTY_KEYS
        }
        return keys[region]!!
    }


    private data class XteaFile(val region: Int, val keys: IntArray) {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as XteaFile

            if (region != other.region) return false
            if (!keys.contentEquals(other.keys)) return false

            return true
        }
        override fun hashCode(): Int {
            var result = region
            result = 31 * result + keys.contentHashCode()
            return result
        }
    }

    companion object {
        val logger = KotlinLogging.logger {}
        val EMPTY_KEYS = intArrayOf(0, 0, 0, 0)
    }
}