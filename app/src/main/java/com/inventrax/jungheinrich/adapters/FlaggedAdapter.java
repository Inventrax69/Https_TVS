package com.inventrax.jungheinrich.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.inventrax.jungheinrich.R;
import com.inventrax.jungheinrich.fragments.HeaderFlaggedBinsFragment;
import com.inventrax.jungheinrich.pojos.OutbountDTO;

import java.util.List;

public class FlaggedAdapter extends RecyclerView.Adapter<com.inventrax.jungheinrich.adapters.FlaggedAdapter.Recycle> {
    Context context;
    androidx.fragment.app.FragmentActivity activity;
    List<OutbountDTO>lstOutbound;
    private String pickRefNo = "", pickobdId;
    public final HeaderFlaggedBinsFragment.OnListFragmentInteractionListener mListener;


    public FlaggedAdapter(FragmentActivity activity, Context context, List<OutbountDTO> lstOutbound, HeaderFlaggedBinsFragment.OnListFragmentInteractionListener listener
    ) {
        this.context=context;
        this.activity=activity;
        this.lstOutbound=lstOutbound;
        this.mListener=listener;
    }

    @Override
    public Recycle onCreateViewHolder(ViewGroup parent, int viewType) {
        View v= LayoutInflater.from(context).inflate(R.layout.flaggedlayout,parent,false);
        return new Recycle(v);
    }

    @Override
    public void onBindViewHolder(Recycle holder, @SuppressLint("RecyclerView") final int position) {
        holder.pickLocation.setText(lstOutbound.get(position).getLocation());
        holder.pickSku.setText(lstOutbound.get(position).getSKU());
        holder.pickQuantity.setText(lstOutbound.get(position).getPickedQty());
//        + "/" + lstOutbound.get(position).getAssignedQuantity()

    }

    @Override
    public int getItemCount() {
        return lstOutbound.size();
    }

    public class Recycle extends RecyclerView.ViewHolder {
        TextView pickLocation,pickSku,pickQuantity;
        View mView;
        Button btnPutaway;
        public Recycle(View itemView) {
            super(itemView);
            pickLocation= (TextView) itemView.findViewById(R.id.pickLocation);
            pickSku= (TextView) itemView.findViewById(R.id.pickSku);
            pickQuantity= (TextView) itemView.findViewById(R.id.pickQuantity);
            btnPutaway= (Button) itemView.findViewById(R.id.btnPutaway);


            btnPutaway.setOnClickListener(new View.OnClickListener() {
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
