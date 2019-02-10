package jp.sugasato.fbxloaderkt

class FbxMeshNode {

    var Name: NameSet = NameSet()
    var vertices: DoubleArray? = null//頂点
    var NumVertices: Int = 0//頂点数, xyzで1組
    var polygonVertices: IntArray? = null//頂点インデックス
    var NumPolygonVertices: Int = 0//頂点インデックス数
    var NumPolygon: Int = 0//ポリゴン数
    var PolygonSize: IntArray? = null//各ポリゴン頂点インデックス数
    var NumMaterial: Int = 0//マテリアル数
    var material: Array<FbxMaterialNode?> = arrayOfNulls(5)
    var Material: Array<LayerElement?> = arrayOfNulls(5)
    var Normals: Array<LayerElement?> = arrayOfNulls(5)
    var UV: Array<LayerElement?> = arrayOfNulls(5)
    var NumDeformer: Int = 0
    var deformer: Array<Deformer?> = arrayOfNulls(256)
    var rootDeformer: Deformer? = null

    fun GetName(): NameSet {
        return Name
    }

    fun GetNumVertices(): Int {
        return NumVertices
    }

    fun GetVertices(): DoubleArray? {
        return vertices
    }

    fun GetNumPolygonVertices(): Int {
        return NumPolygonVertices;
    }

    fun GetPolygonVertices(): IntArray? {
        return polygonVertices
    }

    fun GetNumPolygon(): Int {
        return NumPolygon;
    }

    fun getPolygonSize(pind: Int): Int {
        return PolygonSize!![pind]
    }

    fun GetNumMaterial(): Int {
        return NumMaterial
    }

    //Material
    fun getMaterialName(layerIndex: Int): NameSet {
        return material[layerIndex]!!.MaterialName
    }

    fun getMaterialMappingInformationType(layerIndex: Int): NameSet {
        return Material[layerIndex]!!.MappingInformationType
    }

    fun getMaterialNoOfPolygon(polygonNo: Int, layerIndex: Int): Int {
        if (Material[layerIndex]!!.Nummaterialarr <= polygonNo) return 0
        return Material[layerIndex]!!.materials!![polygonNo]
    }

    fun getDiffuseTextureName(Index: Int): NameSet {
        return material[Index]!!.textureDifName
    }

    fun getNormalTextureName(Index: Int): NameSet {
        return material[Index]!!.textureNorName
    }

    fun getDiffuseColor(Index: Int, ColIndex: Int): Double {
        return material[Index]!!.Diffuse[ColIndex]
    }

    fun getSpecularColor(Index: Int, ColIndex: Int): Double {
        return material[Index]!!.Specular[ColIndex]
    }

    fun getAmbientColor(Index: Int, ColIndex: Int): Double {
        return material[Index]!!.Ambient[ColIndex]
    }

    //Normal
    fun getNumNormal(layerIndex: Int): Int {
        return Normals[layerIndex]!!.Numnormals
    }

    fun getNormalName(layerIndex: Int): NameSet {
        return Normals[layerIndex]!!.name
    }

    fun getNormalMappingInformationType(layerIndex: Int): NameSet {
        return Normals[layerIndex]!!.MappingInformationType
    }

    fun getNormal(layerIndex: Int): DoubleArray? {
        return Normals[layerIndex]!!.normals
    }

    //UV
    fun getNumUV(layerIndex: Int): Int {
        return UV[layerIndex]!!.NumUV
    }

    fun getUVName(layerIndex: Int): NameSet {
        return UV[layerIndex]!!.name
    }

    fun getUVMappingInformationType(layerIndex: Int): NameSet {
        return UV[layerIndex]!!.MappingInformationType
    }

    fun getUV(layerIndex: Int): DoubleArray? {
        return UV[layerIndex]!!.UV
    }

    fun getNumUVindex(layerIndex: Int): Int {
        return UV[layerIndex]!!.NumUVindex
    }

    fun getUVindex(layerIndex: Int): IntArray? {
        return UV[layerIndex]!!.UVindex
    }

    fun getAlignedUV(layerIndex: Int): DoubleArray? {
        return UV[layerIndex]!!.AlignedUV
    }

    //Deformer
    fun GetNumDeformer(): Int {
        return NumDeformer
    }

    fun getDeformer(index: Int): Deformer? {
        return deformer[index]
    }
}