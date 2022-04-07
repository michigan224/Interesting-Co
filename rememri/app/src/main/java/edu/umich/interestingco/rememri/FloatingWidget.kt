//package edu.umich.interestingco.rememri
//
//import android.R
//import android.app.Service
//import android.content.Intent
//import android.graphics.PixelFormat
//import android.os.IBinder
//import android.view.*
//import android.widget.ImageView
//
//class FloatWidgetService : Service() {
//    private var mWindowManager: WindowManager? = null
//    private var mFloatingWidget: View? = null
//    override fun onBind(intent: Intent): IBinder? {
//        return null
//    }
//
//    override fun onCreate() {
//        super.onCreate()
//        mFloatingWidget = LayoutInflater.from(this).inflate(R.layout.layout_floating_widget, null)
//        val params = WindowManager.LayoutParams(
//            WindowManager.LayoutParams.WRAP_CONTENT,
//            WindowManager.LayoutParams.WRAP_CONTENT,
//            WindowManager.LayoutParams.TYPE_PHONE,
//            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
//            PixelFormat.TRANSLUCENT
//        )
//        params.gravity = Gravity.TOP or Gravity.LEFT
//        params.x = 0
//        params.y = 100
//        mWindowManager = getSystemService(WINDOW_SERVICE) as WindowManager
//        mWindowManager!!.addView(mFloatingWidget, params)
//        val collapsedView = mFloatingWidget!!.findViewById<View>(R.id.collapse_view)
//        val expandedView = mFloatingWidget!!.findViewById<View>(R.id.expanded_container)
//        val closeButtonCollapsed = mFloatingWidget!!.findViewById<View>(R.id.close_btn) as ImageView
//        closeButtonCollapsed.setOnClickListener { stopSelf() }
//        val closeButton = mFloatingWidget!!.findViewById<View>(R.id.close_button) as ImageView
//        closeButton.setOnClickListener {
//            collapsedView.visibility = View.VISIBLE
//            expandedView.visibility = View.GONE
//        }
//        mFloatingWidget!!.findViewById<View>(R.id.root_container)
//            .setOnTouchListener(object : View.OnTouchListener {
//                private var initialX = 0
//                private var initialY = 0
//                private var initialTouchX = 0f
//                private var initialTouchY = 0f
//                override fun onTouch(v: View, event: MotionEvent): Boolean {
//                    when (event.action) {
//                        MotionEvent.ACTION_DOWN -> {
//                            initialX = params.x
//                            initialY = params.y
//                            initialTouchX = event.rawX
//                            initialTouchY = event.rawY
//                            return true
//                        }
//                        MotionEvent.ACTION_UP -> {
//                            val Xdiff = (event.rawX - initialTouchX).toInt()
//                            val Ydiff = (event.rawY - initialTouchY).toInt()
//                            if (Xdiff < 10 && Ydiff < 10) {
//                                if (isViewCollapsed) {
//                                    collapsedView.visibility = View.GONE
//                                    expandedView.visibility = View.VISIBLE
//                                }
//                            }
//                            return true
//                        }
//                        MotionEvent.ACTION_MOVE -> {
//                            params.x = initialX + (event.rawX - initialTouchX).toInt()
//                            params.y = initialY + (event.rawY - initialTouchY).toInt()
//                            mWindowManager!!.updateViewLayout(mFloatingWidget, params)
//                            return true
//                        }
//                    }
//                    return false
//                }
//            })
//    }
//
//    private val isViewCollapsed: Boolean
//        private get() = mFloatingWidget == null || mFloatingWidget!!.findViewById<View>(R.id.collapse_view).visibility == View.VISIBLE
//
//    override fun onDestroy() {
//        super.onDestroy()
//        if (mFloatingWidget != null) mWindowManager!!.removeView(mFloatingWidget)
//    }
//}