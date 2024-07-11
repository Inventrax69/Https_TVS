package com.inventrax.jungheinrich.adapters;

import android.annotation.SuppressLint;
import android.content.Context;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.inventrax.jungheinrich.R;
import com.inventrax.jungheinrich.fragments.CcHeaderFragment;
import com.inventrax.jungheinrich.pojos.CycleCountDTO;

import java.util.List;

public class CycleCountLocationListAdapter extends RecyclerView.Adapter<com.inventrax.jungheinrich.adapters.CycleCountLocationListAdapter.Recycle> {
    Context context;
    FragmentActivity activity;
    List<CycleCountDTO>lstCyclecount;
    private String pickRefNo = "", pickobdId;
    public final CcHeaderFragment.OnListFragmentInteractionListener mListener;


    public CycleCountLocationListAdapter(androidx.fragment.app.FragmentActivity activity, Context context, List<CycleCountDTO> lstCyclecount, CcHeaderFragment.OnListFragmentInteractionListener listener
    ) {
        this.context=context;
        this.activity=activity;
        this.lstCyclecount=lstCyclecount;
        this.mListener=listener;
    }

    @Override
    public Recycle onCreateViewHolder(ViewGroup parent, int viewType) {
        View v= LayoutInflater.from(context).inflate(R.layout.cyclecount_location_layout,parent,false);
        return new Recycle(v);
    }

    @Override
    public void onBindViewHolder(Recycle holder, @SuppressLint("RecyclerView") final int position) {
        holder.lbl_location.setText(lstCyclecount.get(position).getLocation());


        //  holder.pickQuantity.setText(lstOutbound.get(position).getPickedQty());
//        + "/" + lstOutbound.get(position).getAssignedQuantity()

    }

    @Override
    public int getItemCount() {
        return lstCyclecount.size();
    }

    public class Recycle extends RecyclerView.ViewHolder {
        TextView lbl_location;
        View mView;
        LinearLayout layout;
        //Button btnPicking;
        public Recycle(View itemView) {
            super(itemView);
            lbl_location= (TextView) itemView.findViewById(R.id.lbl_location);

            layout= (LinearLayout) itemView.findViewById(R.id.layout);

            layout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (null != mListener) {
                        // returns selected position to the fragment
                        int position=getAdapterPosition();
                        mListener.onListFragmentInteraction(position);


                    }
                }
            });
        }
    }
}
