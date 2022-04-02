package edu.umich.interestingco.rememri

import android.content.Intent
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity


class PostViewActivity : AppCompatActivity()  {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.post_view)

        val textView = findViewById<View>(R.id.textView) as TextView
        textView.setTextColor(Color.RED)
        textView.textSize = 20f
        val mimageView = findViewById<View>(R.id.imageView) as ImageView
        val mbitmap = (resources.getDrawable(R.drawable.duck) as BitmapDrawable).bitmap
        val imageRounded = Bitmap.createBitmap(mbitmap.width, mbitmap.height, mbitmap.config)
        val canvas = Canvas(imageRounded)
        val mpaint = Paint()
        mpaint.isAntiAlias = true
        mpaint.shader = BitmapShader(mbitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        canvas.drawRoundRect(
            RectF(0F, 0F, mbitmap.width.toFloat(), mbitmap.height.toFloat()),
            100f,
            100f,
            mpaint
        ) // Round Image Corner 100 100 100 100
        mimageView.setImageBitmap(imageRounded)
    }

    fun postComment() {
        
    }

    fun submitComment() {

    }

    fun returnMain(view: View?) = startActivity(Intent(this, MainActivity::class.java))

    fun returnAccount(view: View?) = startActivity(Intent(this, AccountActivity::class.java))

    fun returnFriends(view: View?) = startActivity(Intent(this, FriendActivity::class.java))

    fun returnPost(view: View?) = startActivity(Intent(this, PostViewActivity::class.java))

}