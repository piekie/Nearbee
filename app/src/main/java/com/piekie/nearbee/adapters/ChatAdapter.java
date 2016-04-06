package com.piekie.nearbee.adapters;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.piekie.nearbee.utils.OneComment;
import com.piekie.nearbee.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Class to work with listView and list
 * helping with displaying
 */
public class ChatAdapter extends ArrayAdapter<OneComment> {

    /**
     * List of the displayed messages
     */
    private List<OneComment> chat = new ArrayList<>();

    @Override
    public void add(OneComment object) {
        chat.add(object);
        super.add(object);
    }

    public ChatAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
    }

    public int getCount() {
        return this.chat.size();
    }

    public OneComment getItem(int index) {
        return this.chat.get(index);
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        if (row == null) {
            LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            row = inflater.inflate(R.layout.chat_item, parent, false);
        }

        LinearLayout wrapper = (LinearLayout) row.findViewById(R.id.wrapper);

        OneComment comment = getItem(position);

        TextView text = (TextView) row.findViewById(R.id.chat_item);

        text.setText(comment.comment);

        text.setBackgroundResource(comment.left ? R.drawable.notmine : R.drawable.mine);
        wrapper.setGravity(comment.left ? Gravity.START : Gravity.END);

        return row;
    }
}
