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
import com.inventrax.jungheinrich.fragments.InternalPickingHeaderFragment;
import com.inventrax.jungheinrich.pojos.OutbountDTO;

import java.util.List;

public class InternalPickingAdapter extends RecyclerView.Adapter<com.inventrax.jungheinrich.adapters.InternalPickingAdapter.Recycle> {
    Context context;
    FragmentActivity activity;
    List<OutbountDTO>lstOutbound;
    private String pickRefNo = "", pickobdId;
    public final InternalPickingHeaderFragment.OnListFragmentInteractionListener mListener;


    public InternalPickingAdapter(FragmentActivity activity, Context context, List<OutbountDTO> lstOutbound, InternalPickingHeaderFragment.OnListFragmentInteractionListener listener
    ) {
        this.context=context;
        this.activity=activity;
        this.lstOutbound=lstOutbound;
        this.mListener=listener;
    }

    @Override
    public Recycle onCreateViewHolder(ViewGroup parent, int viewType) {
        View v= LayoutInflater.from(context).inflate(R.layout.internalpicking_layout,parent,false);
        return new Recycle(v);
    }

    @Override
    public void onBindViewHolder(Recycle holder, @SuppressLint("RecyclerView") final int position) {
        holder.sku.setText(lstOutbound.get(position).getMaterialCode());
        holder.qty.setText(lstOutbound.get(position).getQuantity());
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
