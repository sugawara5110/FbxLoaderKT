package jp.sugasato.fbxloaderkt

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.util.TypedValue
import android.widget.TextView
import android.view.Gravity
import android.graphics.Color;
import android.R.attr.x
import android.R.attr.y



class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("TAG", "スタート")
        var fbx: FbxLoader = FbxLoader()
        fbx.setFbxFile(this, R.raw.player1_fbx_att)

        val NumMesh = fbx.getNumFbxMeshNode()
        //Log.d("TAG", "メッシュ数" + NumMesh)

        for(i in 0..NumMesh - 1) {
            val mesh = fbx.getFbxMeshNode(i)
            if (mesh!!.getNormalTextureName(0)!!.getName() != null) {
                for (i1 in 0..mesh.getNormalTextureName(0).getSize() - 1) {
                    //Log.d("TAG", "マテリアル毎のノーマルテクスチャ名" + mesh.getNormalTextureName(0)!!.getName()!![i1])
                }
            }
            if (mesh!!.getDiffuseTextureName(0)!!.getName() != null) {
                for (i1 in 0..mesh.getDiffuseTextureName(0).getSize() - 1) {
                    //Log.d("TAG", "マテリアル毎のディフェーズテクスチャ名" + mesh.getDiffuseTextureName(0)!!.getName()!![i1])
                }
            }
            for (i1 in 0..mesh.getMaterialName(0).getSize() - 1) {
                //Log.d("TAG", "マテリアル毎マテリアル名" + mesh.getMaterialName(0)!!.getName()!![i1])
            }
            /*Log.d("TAG", "マテリアル毎ディフェーズ色" + mesh.getDiffuseColor(0,0))
            Log.d("TAG", "マテリアル毎ディフェーズ色" + mesh.getDiffuseColor(0,1))
            Log.d("TAG", "マテリアル毎ディフェーズ色" + mesh.getDiffuseColor(0,2))
            Log.d("TAG", "マテリアル毎スぺキュラ色" + mesh.getSpecularColor(0,0))
            Log.d("TAG", "マテリアル毎スぺキュラ色" + mesh.getSpecularColor(0,1))
            Log.d("TAG", "マテリアル毎スぺキュラ色" + mesh.getSpecularColor(0,2))*/
        }

        for (i in 0..NumMesh - 1) {
            val mesh = fbx.getFbxMeshNode(i)
            val meshName = mesh!!.GetName()
            val NumVertices = mesh.GetNumVertices()
            val vertices = mesh!!.GetVertices()
            val NumIndex = mesh!!.GetNumPolygonVertices()
            val Index = mesh!!.GetPolygonVertices()
            Log.d("TAG", "メッシュName長さ: " + meshName!!.getSize())
            for (i1 in 0..meshName!!.getSize() - 1) {
                Log.d("TAG", "メッシュName: " + meshName!!.getName()!![i1])
            }
            //Log.d("TAG", "頂点インデックス数: " + mesh.GetNumPolygonVertices())
            for (i1 in 0..20) {
               // Log.d("TAG", "頂点インデックス: " + Index!![i1])
            }
            for (i1 in NumIndex - 20..NumIndex - 1) {
                //Log.d("TAG", "頂点インデックス: " + Index!![i1])
            }
            //Log.d("TAG", "頂点インデックス数: " + NumVertices)
            for (i1 in 0..20) {
                Log.d("TAG", "頂点" + vertices!![i1])
            }
            for (i1 in NumVertices - 20..NumVertices - 1) {
                Log.d("TAG", "頂点" + vertices!![i1])
            }
           // Log.d("TAG", "メッシュ毎マテリアル数" + mesh.GetNumMaterial())
           // Log.d("TAG", "メッシュ毎ポリゴン数" + mesh.GetNumPolygon())
            for (i1 in 0..20) {
           //     Log.d("TAG", "ポリゴン毎の頂点インデックス数" + mesh.getPolygonSize(i1))
            }
            for (i1 in mesh.GetNumPolygon() - 20..mesh.GetNumPolygon() - 1) {
           //     Log.d("TAG", "ポリゴン毎の頂点インデックス数" + mesh.getPolygonSize(i1))
            }
            val nor = mesh.getNormal(0)
            for (i1 in 0..20) {
          //      Log.d("TAG", "ポリゴン毎の法線" + nor!![i1])
            }
            for (i1 in mesh.getNumNormal(0) - 20..mesh.getNumNormal(0) - 1) {
          //      Log.d("TAG", "ポリゴン毎の法線" + nor!![i1])
            }
            val uv = mesh.getAlignedUV(0)
            for (i1 in 0..20) {
            //    Log.d("TAG", "ポリゴン毎のUV" + uv!![i1])
            }
            for (i1 in mesh.getNumUV(0) - 20..mesh.getNumUV(0) - 1) {
            //    Log.d("TAG", "ポリゴン毎のUV" + uv!![i1])
            }

           // Log.d("TAG", "メッシュ毎のボーン数" + mesh.GetNumDeformer())
            val defo = mesh.getDeformer(i)
            val bNum = defo!!.getIndicesCnt()
            val bInd = defo.GetIndices()
            val bWei = defo.GetWeights()
            val bname2 = defo.getName()
            for (i1 in 0..mesh.GetNumDeformer() - 1) {
                for (i2 in 0..bname2.getSize() - 1) {
                   //Log.d("TAG", "ボーン名" + bname2.getName()!![i2])
                }
                //Log.d("TAG", "ボーン毎の影響受ける頂点インデックス数" + bNum)
                for (i2 in 0..bNum - 1) {
                   // Log.d("TAG", "ボーン毎の影響受ける頂点インデックス" + bInd!![i2])
                   // Log.d("TAG", "ボーン毎の影響受ける頂点ウエイト" + bWei!![i2])
                }
            }
        }

        val mesh = fbx.getFbxMeshNode(0)

        for (i in 0..mesh!!.GetNumDeformer() - 1) {
            val defo = mesh.getDeformer(i)
            val bname2 = defo!!.getName()
            for (i2 in 0..bname2.getSize() - 1) {
              //  Log.d("TAG", "ボーン名" + bname2.getName()!![i2])
            }
            for (y in 0..3) {
                for (x in 0..3) {
                //    Log.d("TAG", "TransformLinkMatrix" + defo.getTransformLinkMatrix(y, x))
                }
            }
        }

        for (i in 0..mesh!!.GetNumDeformer() - 1) {
            val defo = mesh.getDeformer(i)
            val bname2 = defo!!.getName()
            for (i2 in 0..bname2.getSize() - 1) {
              //  Log.d("TAG", "ボーン名" + bname2.getName()!![i2])
            }
            defo.EvaluateGlobalTransform(0)
            for (y in 0..3) {
                for (x in 0..3) {
                   // Log.d("TAG", "getEvaluateGlobalTransform" + defo.getEvaluateGlobalTransform(y, x))
                }
            }
        }


        val linearLayout = LinearLayout(this)
        // 垂直方向
        linearLayout.orientation = LinearLayout.VERTICAL
        // レイアウト中央寄せ
        linearLayout.gravity = Gravity.CENTER

        // linearLayoutをContentViewにセット
        setContentView(linearLayout)
        // TextView インスタンス生成
        val textView = TextView(this)
        val str = "完走"
        textView.text = str
        // テキストカラー
        textView.setTextColor(Color.rgb(0x0, 0x0, 0xaa))
        // テキストサイズ
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 50f)
        linearLayout.addView(
            textView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )
    }
}
