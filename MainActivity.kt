package jp.sugasato.fbxloaderkt

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.util.TypedValue
import android.widget.TextView
import android.view.Gravity
import android.graphics.Color;

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var fbx: FbxLoader = FbxLoader()
        fbx.setFbxFile(this, R.raw.player1_fbx_att)

        // レイアウトの設定
        val linearLayout = LinearLayout(this)
        // 垂直方向
        linearLayout.orientation = LinearLayout.VERTICAL
        // レイアウト中央寄せ
        linearLayout.gravity = Gravity.CENTER

        // linearLayoutをContentViewにセット
        setContentView(linearLayout)
        // TextView インスタンス生成
        val textView = TextView(this)
        val str = "Test TextView"
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
