package jp.sugasato.fbxloaderkt

class FbxMaterialNode {

    var Diffuse = DoubleArray(3, { 0.0 })
    var Specular = DoubleArray(3, { 0.0 })
    var Ambient = DoubleArray(3, { 0.0 })
    var MaterialName: CharArray? = null
    var textureDifName: CharArray? = null
    var textureNorName: CharArray? = null
}