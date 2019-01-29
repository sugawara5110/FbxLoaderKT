package jp.sugasato.fbxloaderkt

class FbxLoader {

    var fp: FilePointer = FilePointer()
    var version = 0//23から26バイトまで4バイト分符号なし整数,リトルエンディアン(下から読む)
    var FbxRecord:NodeRecord=NodeRecord()//ファイルそのまま
    var rootNode:NodeRecord? = null//ConnectionID:0のポインタ
    var cnNo:ArrayList<ConnectionNo?> = arrayListOf()
    var cnLi:ArrayList<ConnectionList?> = arrayListOf()
    var NumMesh = 0
    var Mesh:Array<FbxMeshNode?>? = null
    var NumDeformer = 0
    var deformer:Array<Deformer?> = arrayOfNulls(256)//デフォーマーのみのファイル対応
    var rootDeformer:Deformer? = null

    fun setFbxFile(pass: String) {
        fp.setFile(pass)
    }


}