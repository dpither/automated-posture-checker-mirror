package com.example.automatedposturechecker;


import static com.example.automatedposturechecker.Utils.EXTRA_SESSION_ID;
import static com.example.automatedposturechecker.Utils.EXTRA_USER_ID;

import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SessionListAdapter extends RecyclerView.Adapter<SessionListAdapter.ViewHolder> {

    private List<SessionItem> sessionList;

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public TextView session_name;
        public TextView session_duration;
        public TextView session_date;

        public ViewHolder(View view) {
            super(view);
            session_name = view.findViewById(R.id.session_item_name);
            session_duration = view.findViewById(R.id.session_item_length);
            session_date = view.findViewById(R.id.session_item_date);
            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            int position = getAdapterPosition();
            SessionItem item = sessionList.get(position);
            gotoSessionInfo(view, item);
        }
    }

    public SessionListAdapter(List<SessionItem> sessions) {
        sessionList = sessions;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.session_item, viewGroup, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        SessionItem item = sessionList.get(position);

        TextView session_name = holder.session_name;
        TextView session_length = holder.session_duration;
        TextView session_date = holder.session_date;
        session_name.setText(item.getName());
        session_length.setText(item.getLength());
        String date = item.getDate();
        date = date.split(" ")[0];
        session_date.setText(date);
    }

    @Override
    public int getItemCount() {
        return sessionList.size();
    }

    private void gotoSessionInfo(View view, SessionItem item) {
        Context context = view.getContext();
        ActivityOptions options = ActivityOptions.makeCustomAnimation(context, R.anim.slide_in_bot, R.anim.fade_out);
        Intent intent = new Intent(context, SessionInfo.class);
        intent.putExtra(EXTRA_USER_ID, item.getUserId());
        intent.putExtra(EXTRA_SESSION_ID, item.getSessionId());
        context.startActivity(intent, options.toBundle());
    }

}
