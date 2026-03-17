package com.v8.global.sniffer.game;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.v8.global.sniffer.R;
import java.util.List;

public class CardAdapter extends BaseAdapter {

    private Context context;
    private List<CardModel> cards;
    private LayoutInflater inflater;

    public CardAdapter(Context context, List<CardModel> cards) {
        this.context = context;
        this.cards = cards;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() { return cards.size(); }

    @Override
    public Object getItem(int position) { return cards.get(position); }

    @Override
    public long getItemId(int position) { return position; }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.card_item, parent, false);
            holder = new ViewHolder();
            holder.tvCard = convertView.findViewById(R.id.tv_card);
            holder.ivCard = convertView.findViewById(R.id.iv_card);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        CardModel card = cards.get(position);

        if (card.isMatched()) {
            holder.tvCard.setVisibility(View.INVISIBLE);
            holder.ivCard.setVisibility(View.INVISIBLE);
        } else if (card.isFlipped()) {
            holder.tvCard.setVisibility(View.VISIBLE);
            holder.ivCard.setVisibility(View.GONE);
            holder.tvCard.setText(card.getImage());
            holder.tvCard.setTextSize(32);
        } else {
            holder.tvCard.setVisibility(View.GONE);
            holder.ivCard.setVisibility(View.VISIBLE);
            holder.ivCard.setImageResource(android.R.drawable.ic_menu_help);
        }

        return convertView;
    }

    private static class ViewHolder {
        TextView tvCard;
        ImageView ivCard;
    }
}
