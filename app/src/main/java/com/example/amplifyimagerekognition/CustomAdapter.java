package com.example.amplifyimagerekognition;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;



import java.util.ArrayList;


public class CustomAdapter extends BaseAdapter {

    Context context;
    ArrayList<LabelModel> arrayList;
    public CustomAdapter(Context context, ArrayList<LabelModel> arrayList) {
        this.context = context;
        this.arrayList = arrayList;
    }



    @Override
    public int getCount() {
        return arrayList.size();
    }

    @Override
    public Object getItem(int position) {
        return arrayList.get(position);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public  View getView(final int position, View convertView, ViewGroup parent) {
        if (convertView ==  null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_list, parent, false);
        }
        TextView txtlabelName, txtConfidence;
        txtlabelName = (TextView) convertView.findViewById(R.id.labelname);
        txtConfidence = (TextView) convertView.findViewById(R.id.confidence);
        txtlabelName.setText(arrayList.get(position).getLabelName());
        txtConfidence.setText(arrayList.get(position).getConfidence());

        return convertView;
    }
}
