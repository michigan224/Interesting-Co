package edu.umich.interestingco.rememri

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import edu.umich.interestingco.rememri.databinding.ListitemCommentBinding

class CommentAdapter(context: Context, comments: ArrayList<Comment?>) :
    ArrayAdapter<Comment?>(context, 0, comments) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val listItemView = (convertView?.tag /* reuse binding */ ?: run {
            val rowView = LayoutInflater.from(context).inflate(R.layout.listitem_comment, parent, false)
            rowView.tag = ListitemCommentBinding.bind(rowView) // cache binding
            rowView.tag
        }) as ListitemCommentBinding

        getItem(position)?.run {
            val myString = "$owner_id:\n$text"
            listItemView.titleNameText.text = myString

            //listItemView.root.setBackgroundColor(Color.parseColor(if (position % 2 == 0) "#E0E0E0" else "#EEEEEE"))
        }

        return listItemView.root
    }
}