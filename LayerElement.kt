package jp.sugasato.fbxloaderkt

class LayerElement {
    var MappingInformationType: NameSet = NameSet()
    var name: NameSet = NameSet()
    var Nummaterialarr: Int = 0
    var materials: IntArray? = null
    var Numnormals: Int = 0
    var normals: DoubleArray? = null
    var NumUV: Int = 0
    var UV: DoubleArray? = null
    var NumUVindex: Int = 0
    var UVindex: IntArray? = null
    var AlignedUV: DoubleArray? = null
}