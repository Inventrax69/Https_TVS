package com.inventrax.jungheinrich.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.inventrax.jungheinrich.R;
import com.inventrax.jungheinrich.fragments.HomeFragment;
import com.inventrax.jungheinrich.pojos.HomeModel;

import java.util.List;

public class HomeAdapter extends RecyclerView.Adapter<com.inventrax.jungheinrich.adapters.HomeAdapter.Recycle>{


    Context context;
    FragmentActivity activity;
    List<HomeModel> lstOutbound;
    private String pickRefNo = "", pickobdId;
    public final HomeFragment.OnListFragmentInteractionListener mListener;

    public HomeAdapter(FragmentActivity activity, Context context, List<HomeModel> lstOutbound, HomeFragment.OnListFragmentInteractionListener listener) {
        this.context=context;
        this.activity=activity;
        this.lstOutbound=lstOutbound;
        this.mListener=listener;

    }
    @NonNull
    @Override
    public HomeAdapter.Recycle onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v= LayoutInflater.from(context).inflate(R.layout.home_grid,parent,false);
        return new HomeAdapter.Recycle(v);
    }



    @Override
    public void onBindViewHolder(HomeAdapter.Recycle holder, @SuppressLint("RecyclerView") final int position) {
        HomeModel recyclerData = lstOutbound.get(position);
        holder.screen_name.setText(recyclerData.getCourse_name());
        holder.imgview.setImageResource(recyclerData.getImgid());
    }

    @Override
    public int getItemCount() {
        return lstOutbound.size();
    }



    public class Recycle extends RecyclerView.ViewHolder {
         TextView screen_name;
         ImageView imgview;
         LinearLayout layout;
        public Recycle(View itemView) {
            super(itemView);
            screen_name = itemView.findViewById(R.id.screen_name);
            imgview = itemView.findViewById(R.id.imgview);
            layout= itemView.findViewById(R.id.layout);
            layout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (null != mListener) {
                        int position=getAdapterPosition();
                        mListener.onListFragmentInteraction(position);



                    }
                }
            });
        }
    }
}
