package com.inventrax.tvs.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.inventrax.tvs.R;


import com.inventrax.tvs.fragments.WorkorderRevertFragment;
import com.inventrax.tvs.pojos.OutbountDTO;

import java.util.List;

public class WorkOrderAdapter extends RecyclerView.Adapter<com.inventrax.tvs.adapters.WorkOrderAdapter.Recycle> {
    Context context;
    FragmentActivity activity;
    List<OutbountDTO>lstOutbound;
    private String pickRefNo = "", pickobdId;
    public final WorkorderRevertFragment.OnListFragmentInteractionListener mListener;


    public WorkOrderAdapter(FragmentActivity activity, Context context, List<OutbountDTO> lstOutbound, WorkorderRevertFragment.OnListFragmentInteractionListener listener) {
        this.context=context;
        this.activity=activity;
        this.lstOutbound=lstOutbound;
        this.mListener=listener;
    }

    @Override
    public Recycle onCreateViewHolder(ViewGroup parent, int viewType) {
        View v= LayoutInflater.from(context).inflate(R.layout.fragment_work_order_adapter,parent,false);
        return new Recycle(v);
    }





    @Override
    public void onBindViewHolder(Recycle holder, @SuppressLint("RecyclerView") final int position) {
        holder.sku.setText(lstOutbound.get(position).getSKU());
        holder.qty.setText(lstOutbound.get(position).getRevertQty() +"/"+lstOutbound.get(position).getQty());
        holder.batch.setText(lstOutbound.get(position).getBatchNo());
        //  holder.pickQuantity.setText(lstOutbound.get(position).getPickedQty());
//        + "/" + lstOutbound.get(position).getAssignedQuantity()

    }

    @Override
    public int getItemCount() {
        return lstOutbound.size();
    }

    public class Recycle extends RecyclerView.ViewHolder {
        TextView sku,qty,batch;
        View mView;
        LinearLayout layout;
        //Button btnPicking;
        public Recycle(View itemView) {
            super(itemView);
            sku= (TextView) itemView.findViewById(R.id.sku);
            qty= (TextView) itemView.findViewById(R.id.qty);
            batch= (TextView) itemView.findViewById(R.id.batch);
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
