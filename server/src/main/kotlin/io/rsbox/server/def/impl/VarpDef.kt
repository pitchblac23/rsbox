package io.rsbox.server.def.impl

import io.netty.buffer.ByteBuf
import io.rsbox.server.def.Definition

/**
 * @author Tom <rspsmods@gmail.com>
 */
class VarpDef(override val id: Int) : Definition(id) {

    var configType = 0

    override fun decode(buf: ByteBuf, opcode: Int) {
        when (opcode) {
            5 -> configType = buf.readUnsignedShort()
        }
    }
}