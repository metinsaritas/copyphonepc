package com.metinsaritas.copyphone_pc;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Parcelable;
import android.telephony.PhoneNumberUtils;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Array;
import java.security.cert.CertificateNotYetValidException;
import java.util.ArrayList;

import javax.xml.validation.Validator;

/**
 * Created by User on 15-Aug-17.
 */

public class ListAdapterCopy extends BaseAdapter implements AdapterView.OnItemLongClickListener, AdapterView.OnItemClickListener {

    private ArrayList<Copy> copyList;
    private ClipboardManager clipboardManager;
    public static int MAX_COPIED_COUNT = 20;
    private Context context;
    private LayoutInflater inflater;
    public ListAdapterCopy(Context context, ArrayList<Copy> copyList) {
        this.context = context;
        this.copyList = copyList;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
    }

    @Override
    public int getCount() {
        return copyList.size();
    }

    @Override
    public Copy getItem(int i) {
        return copyList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        View row = inflater.inflate(R.layout.listview_allcopied, null);

        Copy copy = getItem(i);

        TextView tvAllCopiedText = row.findViewById(R.id.tvAllCopiedText);
        tvAllCopiedText.setText(copy.copiedText);
        if (copy.type != MyValidator.OTHER) {
            tvAllCopiedText.setPaintFlags(tvAllCopiedText.getPaintFlags() |   Paint.UNDERLINE_TEXT_FLAG);
            tvAllCopiedText.setTextColor(context.getResources().getColor(R.color.validate_color));
        }


        return row;
    }

    @Override
    public void notifyDataSetChanged() {
        if (copyList.size() > MAX_COPIED_COUNT) {
            copyList.remove(MAX_COPIED_COUNT);
        }
        super.notifyDataSetChanged();
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
        String copiedText = copyList.get(i).copiedText;
        if (copyList.size() > 1)
            copyList.remove(i);
        clipboardManager.setPrimaryClip(ClipData.newPlainText("text", copiedText));
        Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show();
        return  false;
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

    }
}
