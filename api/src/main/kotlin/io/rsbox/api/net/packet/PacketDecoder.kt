package io.rsbox.api.net.packet

/**
 * @author Kyle Escobar
 */

abstract class PacketDecoder<T : Packet> {

    /**
     * Decodes the [structure] into value [Map]s that can then be used to create
     * an instance of [T].
     */
    open fun decode(opcode: Int, structure: PacketStructure, reader: GamePacketReader): T {
        val values = hashMapOf<String, Number>()
        val stringValues = hashMapOf<String, String>()
        structure.values.values.forEach { value ->
            when (value.type) {
                DataType.BYTES -> throw Exception("Cannot decode message with type ${value.type}.")
                DataType.STRING -> stringValues[value.id] = reader.string
                DataType.SMART -> {
                    if (value.signature == DataSignature.SIGNED) {
                        values[value.id] = reader.signedSmart
                    } else {
                        values[value.id] = reader.unsignedSmart
                    }
                }
                else -> {
                    if (value.signature == DataSignature.SIGNED) {
                        values[value.id] = reader.getSigned(value.type, value.order, value.transformation)
                    } else {
                        values[value.id] = reader.getUnsigned(value.type, value.order, value.transformation)
                    }
                }
            }
        }
        return decode(opcode, structure.opcodes.indexOf(opcode), values, stringValues)
    }

    /**
     * Create a [T] instance with the decoded values for [PacketHandler]s to handle.
     *
     * @param values
     * A map of [Number] values.
     *
     * @param stringValues
     * A map of [String] values.
     */
    abstract fun decode(opcode: Int, opcodeIndex: Int, values: HashMap<String, Number>, stringValues: HashMap<String, String>): T
}