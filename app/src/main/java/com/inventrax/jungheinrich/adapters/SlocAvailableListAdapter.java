package com.inventrax.jungheinrich.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.inventrax.jungheinrich.R;
import com.inventrax.jungheinrich.pojos.InventoryDTO;

import java.util.List;

public class SlocAvailableListAdapter extends RecyclerView.Adapter {

    private static final String classCode = "API_FRAG_009";
    private List<InventoryDTO> inventoryDTOS;
    Context context;
    OnCheckChangeListner onCheckChangeListner;
    boolean isCheck;

    public SlocAvailableListAdapter(Context context, List<InventoryDTO> inventoryDTOS, OnCheckChangeListner onCheckChangeListner){
        this.context=context;
        this.inventoryDTOS=inventoryDTOS;
        this.onCheckChangeListner=onCheckChangeListner;
    }


    public class MyViewHolder extends RecyclerView.ViewHolder {

        TextView FromSloc,AvaQty;// init the item view's
        RadioButton radio_check;

        public MyViewHolder(View itemView) {
            super(itemView);
            // get the reference of item view's
            FromSloc = (TextView) itemView.findViewById(R.id.FromSloc);
            AvaQty = (TextView) itemView.findViewById(R.id.AvaQty);
            radio_check = (RadioButton) itemView.findViewById(R.id.radio_check);

        }
    }


    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // infalte the item Layout
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.sloc_avaiable_list, parent, false);

        // set the view's size, margins, paddings and layout parameters
        return new MyViewHolder(v);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, @SuppressLint("RecyclerView") final int position) {

        InventoryDTO inventoryDTO = (InventoryDTO) inventoryDTOS.get(position);
        ((MyViewHolder) holder).FromSloc.setText(""+inventoryDTO.getStorageLocation());
        ((MyViewHolder) holder).AvaQty.setText(""+inventoryDTO.getQuantity());

        if(inventoryDTO.isChecked()){
            ((MyViewHolder) holder).radio_check.setChecked(true);
        }else{
            ((MyViewHolder) holder).radio_check.setChecked(false);
        }

/*        if(position==0){
            onCheckChangeListner.onCheckChange(position,true);
        }*/

        ((MyViewHolder) holder).radio_check.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onCheckChangeListner.onCheckChange(position,true);
            }
        });

/*        ((MyViewHolder) holder).radio_check.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    onCheckChangeListner.onCheckChange(position,isChecked);
            }
        });*/

    }


    @Override
    public int getItemCount() {
        return inventoryDTOS.size();
    }

    // Item click listener interface
    public interface OnCheckChangeListner {
        void onCheckChange(int pos, boolean isChecked);
    }

}
