package com.piekie.nearbee.adapters;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.piekie.nearbee.R;
import com.piekie.nearbee.utils.OneComment;

import java.util.ArrayList;
import java.util.List;

/**
 * Class to work with listView and list
 * helping with displaying
 */
public class ChatAdapter extends ArrayAdapter<OneComment> {
    private final int MAX_STREAMS = 5;
    /**
     * List of the displayed messages
     */
    private List<OneComment> chat = new ArrayList<>();
    private int lastPosition = -1;
    private SoundPool soundPool;
    private int soundReceived;
    private int soundSent;

    public ChatAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);

        soundPool = new SoundPool(MAX_STREAMS, AudioManager.STREAM_MUSIC, 0);
        soundReceived = soundPool.load(context, R.raw.receive, 1);
        soundSent = soundPool.load(context, R.raw.send, 1);
    }

    @Override
    public void add(OneComment object) {
        chat.add(object);
        super.add(object);
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

        if (position != 0 && chat.get(position - 1).left == comment.left) {
            text.setBackgroundResource(comment.left ? R.drawable.notminenext : R.drawable.minenext);
        } else {
            text.setBackgroundResource(comment.left ? R.drawable.notmine : R.drawable.mine);
        }

        wrapper.setGravity(comment.left ? Gravity.START : Gravity.END);

        if (position > lastPosition) {
            int typeOfAnim = comment.left ? R.anim.slide_out_left : R.anim.slide_out_right;
            int sound = comment.left ? soundReceived : soundSent;

            Animation anim = AnimationUtils.loadAnimation(getContext(), typeOfAnim);
            anim.setDuration(1000);
            row.startAnimation(anim);

            lastPosition = position;

            soundPool.play(sound, 1, 1, 1, 0, 1);
        }

        return row;
    }
}
