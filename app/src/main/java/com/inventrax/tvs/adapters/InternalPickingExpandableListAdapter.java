package com.inventrax.tvs.adapters;

import android.app.Fragment;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;

import com.inventrax.tvs.R;
import com.inventrax.tvs.fragments.InternalBinTransferFragment;
import com.inventrax.tvs.util.FragmentUtils;

import java.util.HashMap;
import java.util.List;

public class InternalPickingExpandableListAdapter extends BaseExpandableListAdapter {

    private Context _context;
    private List<String> _listDataHeader; // header titles
    // child data in format of header title, child title
    private HashMap<String, List<String>> _listDataChild;
    Fragment fragment=null;
    private androidx.fragment.app.FragmentActivity fragmentActivity;
    TextView lblListHeader;
    private String pickRefNo;
    private String PickQuantity,reftypevalue,vldpID,pickOBDID;


    public InternalPickingExpandableListAdapter(FragmentActivity fragmentActivity, Context context, List<String> listDataHeader,

                                                HashMap<String, List<String>> listChildData, String pickRefNo, String PickQuantity, String reftypevalue, String vldpID, String pickOBDID) {
        this._context = context;
        this._listDataHeader = listDataHeader;
        this._listDataChild = listChildData;
        this.fragmentActivity = fragmentActivity;
        this.pickRefNo =pickRefNo;
        this.PickQuantity =PickQuantity;
        this.reftypevalue =reftypevalue;
        this.vldpID =vldpID;
        this.pickOBDID =pickOBDID;
    }

    @Override
    public Object getChild(int groupPosition, int childPosititon) {
        return this._listDataChild.get(this._listDataHeader.get(groupPosition))
                .get(childPosititon);
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }


    @Override
    public View getChildView(int groupPosition, final int childPosition,
                             boolean isLastChild, View convertView, final ViewGroup parent) {

        final String childText = (String) getChild(groupPosition, childPosition);

        if (convertView == null) {
            LayoutInflater infalInflater = (LayoutInflater) this._context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = infalInflater.inflate(R.layout.list_item, null);
        }

        TextView txtListChild = (TextView) convertView.findViewById(R.id.lblListItem);

        Button btnPutaway = (Button) convertView.findViewById(R.id.btnPutaway) ;

        btnPutaway.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!pickRefNo.equalsIgnoreCase("Select Location")) {
                    Bundle bundle = new Bundle();
                    String str = childText;
                    bundle.putString("FromLocation", str.split("[/]", 5)[0]);
                    bundle.putString("Quantity", str.split("[/]", 5)[1]);
                    bundle.putString("BatchNo", str.split("[/]", 5)[2]);
                    bundle.putString("MaterialCode", str.split("[/]", 5)[3]);
                    bundle.putString("ToLocation", pickRefNo);
                    bundle.putString("SKU", lblListHeader.getText().toString());
                    bundle.putString("PickQuantity", PickQuantity);
                    bundle.putString("reftypevalue", reftypevalue);
                    bundle.putString("vldpID", vldpID);
                    bundle.putString("pickOBDID", pickOBDID);



//			m  fronlocation qty batch materialcode  to loaction
                InternalBinTransferFragment internalBinTransferFragment = new InternalBinTransferFragment();
                internalBinTransferFragment.setArguments(bundle);
                FragmentUtils.replaceFragmentWithBackStack(fragmentActivity, R.id.container_body, internalBinTransferFragment);
            }
                else {
                    Toast.makeText(_context,"Please Select Location",Toast.LENGTH_LONG).show();
                }
            }

        });

        txtListChild.setText(childText.split("[/]", 5)[0] + System.getProperty("line.separator") + childText.split("[/]", 5)[1] + System.getProperty("line.separator") +childText.split("[/]", 5)[2]);
        return convertView;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return this._listDataChild.get(this._listDataHeader.get(groupPosition)).size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return this._listDataHeader.get(groupPosition);
    }

    @Override
    public int getGroupCount() {
        return this._listDataHeader.size();
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded,
                             View convertView, ViewGroup parent) {

        String headerTitle = (String) getGroup(groupPosition);
        if (convertView == null) {
            LayoutInflater infalInflater = (LayoutInflater) this._context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = infalInflater.inflate(R.layout.list_group, null);
        }

        lblListHeader = (TextView) convertView
                .findViewById(R.id.lblListHeader);
        lblListHeader.setTypeface(null, Typeface.BOLD);
        lblListHeader.setText(headerTitle);

        return convertView;

    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }


}
