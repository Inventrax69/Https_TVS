package com.inventrax.jungheinrich.adapters;

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

import androidx.fragment.app.FragmentActivity;

import com.inventrax.jungheinrich.R;
import com.inventrax.jungheinrich.fragments.PutawayDetailsFragment;
import com.inventrax.jungheinrich.util.FragmentUtils;

import java.util.HashMap;
import java.util.List;

public class ExpandableListAdapter extends BaseExpandableListAdapter {

	private Context _context;
	private List<String> _listDataHeader; // header titles
	// child data in format of header title, child title
	private HashMap<String, List<String>> _listDataChild;
	Fragment fragment=null;
	private FragmentActivity fragmentActivity;
	TextView lblListHeader;

	public ExpandableListAdapter(androidx.fragment.app.FragmentActivity fragmentActivity, Context context, List<String> listDataHeader,

								 HashMap<String, List<String>> listChildData) {
		this._context = context;
		this._listDataHeader = listDataHeader;
		this._listDataChild = listChildData;
		this.fragmentActivity = fragmentActivity;
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
				Bundle bundle = new Bundle();
				String str = childText.replace("\r","").replace("\n","/");
				bundle.putString("SuggestedId", str.split("[/]", 6)[3]);
				bundle.putString("SuggestedSKU", str.split("[/]", 6)[1]);
				bundle.putString("SuggestedQty", str.split("[/]", 6)[2]);
				bundle.putString("suggestedLoc", str.split("[/]", 6)[0]);
				bundle.putString("tenantId", str.split("[/]", 6)[4]);
				bundle.putString("warehouseId", str.split("[/]", 6)[5]);
				bundle.putString("palletNumber", lblListHeader.getText().toString());

//			m
				PutawayDetailsFragment putawayDetailsFragment = new PutawayDetailsFragment();
				putawayDetailsFragment.setArguments(bundle);
				FragmentUtils.replaceFragmentWithBackStack(fragmentActivity, R.id.container_body, putawayDetailsFragment);
			}
		});

		txtListChild.setText(childText.split("[/]", 2)[0] + System.getProperty("line.separator") + childText.split("[/]", 2)[1].split("[/]", 2)[0]);
		return convertView;
	}

	@Override
	public int getChildrenCount(int groupPosition) {
		return this._listDataChild.get(this._listDataHeader.get(groupPosition))
				.size();
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
