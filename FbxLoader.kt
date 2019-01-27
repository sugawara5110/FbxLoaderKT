package jp.sugasato.fbxloaderkt

import java.io.File

class FbxLoader {

    fun setFbxFile(pass: CharArray) {
        val file = File("player1_FBX_att.fbx")
        var bytes:ByteArray = file.readBytes()
    }
}