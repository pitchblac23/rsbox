package io.rsbox.server.serializer.player

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.yaml.toYaml
import io.rsbox.server.Server
import io.rsbox.server.ServerConstants
import io.rsbox.server.config.SettingsSpec
import io.rsbox.server.model.entity.Client
import io.rsbox.api.world.Tile
import io.rsbox.server.net.login.LoginRequest
import org.mindrot.jbcrypt.BCrypt
import java.io.File

/**
 * @author Kyle Escobar
 */

object PlayerSave {
    fun check(username: String, password: String): PlayerLoadResult {
        val rusername = filterUsername(username)
        val file = File("${ServerConstants.SAVES_PATH}$rusername.yml")

        if(!file.exists()) {
            return PlayerLoadResult.NEW_ACCOUNT
        }

        val save = Config { addSpec(PlayerSpec) }.from.yaml.file(file)

        if(!BCrypt.checkpw(password, save[PlayerSpec.password])) {
            return PlayerLoadResult.INVALID
        }

        return PlayerLoadResult.ACCEPTABLE
    }

    fun filterUsername(username: String): String {
        val regex = Regex("[^A-Za-z0-9]")
        return regex.replace(username, "")
    }

    fun create(request: LoginRequest) {
        val save = Config { addSpec(PlayerSpec) }
        save[PlayerSpec.username] = request.username
        save[PlayerSpec.password] = BCrypt.hashpw(request.password, BCrypt.gensalt(16))
        save[PlayerSpec.displayName] = ""
        save[PlayerSpec.privilege] = 0
        save[PlayerSpec.x] = Server.settings[SettingsSpec.home_x]
        save[PlayerSpec.z] = Server.settings[SettingsSpec.home_z]
        save[PlayerSpec.height] = Server.settings[SettingsSpec.home_height]
        save[PlayerSpec.uuid] = ""

        val file = File("${ServerConstants.SAVES_PATH}${filterUsername(request.username)}.yml")
        save.toYaml.toFile(file)
    }

    fun load(client: Client, request: LoginRequest) {
        val file = File("${ServerConstants.SAVES_PATH}${PlayerSave.filterUsername(request.username)}.yml")
        val save = Config { addSpec(PlayerSpec) }.from.yaml.file(file)

        client.username = PlayerSave.filterUsername(save[PlayerSpec.username])
        client.password = save[PlayerSpec.password]
        client.displayName = save[PlayerSpec.displayName]
        client.privilege = save[PlayerSpec.privilege]
        client.uuid = save[PlayerSpec.uuid]
        client.clientResizable = request.resizableClient
        client.clientWidth = request.clientWidth
        client.clientHeight = request.clientHeight
        client.tile = Tile(save[PlayerSpec.x], save[PlayerSpec.z], save[PlayerSpec.height])
    }
}