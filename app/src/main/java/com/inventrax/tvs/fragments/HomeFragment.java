package com.inventrax.tvs.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.inventrax.tvs.R;
import com.inventrax.tvs.adapters.HomeAdapter;
import com.inventrax.tvs.pojos.HomeModel;
import com.inventrax.tvs.util.FragmentUtils;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class HomeFragment extends androidx.fragment.app.Fragment  {

    private View rootView;
    Fragment fragment = null;

    private String userId = null, scanType = null, accountId = null,DepartmentIDs="";
    GridView coursesGV;

    RecyclerView pickingRecyclerview;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_home,container,false);
        loadFormControls();

        return rootView;
    }



    private void loadFormControls() {

        SharedPreferences sp = getActivity().getSharedPreferences("LoginActivity", Context.MODE_PRIVATE);
        userId = sp.getString("RefUserId", "");
        scanType = sp.getString("scanType", "");
        accountId = sp.getString("AccountId", "");
        DepartmentIDs= sp.getString("DepartmentIDs", "");
        pickingRecyclerview= (RecyclerView) rootView.findViewById(R.id.idGrid);


        prepareGridList();


    }



    @Override
    public void onResume() {
        super.onResume();

        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle("Home");
    }

  public  void prepareGridList(){

      if(DepartmentIDs.equals("") || DepartmentIDs ==null) {
          DepartmentIDs ="1,2,3,4,5,6,7,8,9,10,11";
      }

      String[] stringsUserRole = DepartmentIDs.split(",");

      ArrayList<HomeModel> courseModelArrayList = new ArrayList<HomeModel>();

      for (String role : stringsUserRole) {
          switch (role) {
              case "1":
                  courseModelArrayList.add(new HomeModel("Receiving", R.drawable.receiving));
                  break;
              case "2":
                  courseModelArrayList.add(new HomeModel("Putaway", R.drawable.putaway));

                  break;
              case "3":
                  courseModelArrayList.add(new HomeModel("Picking", R.drawable.picking));

                  break;

              case "4":
                  courseModelArrayList.add(new HomeModel("Bin to Bin", R.drawable.bin_to_bin));
                  break;

              case "5":
                  courseModelArrayList.add(new HomeModel("Live Stock", R.drawable.live_stock));

                  break;
              case "6":
                  courseModelArrayList.add(new HomeModel("Cycle Count", R.drawable.cycle_count));

                  break;
             /* case "7":
                  courseModelArrayList.add(new HomeModel("Stock Take", R.drawable.picking));

                  break;
              case "8":
                  courseModelArrayList.add(new HomeModel("Material Transfers", R.drawable.housekeeping));

                  break;

              case "9":
                  courseModelArrayList.add(new HomeModel("VNA Putaway", R.drawable.picking));

                  break;

              case "10":
                  courseModelArrayList.add(new HomeModel("VNA Auto Transfers", R.drawable.picking));

                  break;*/
              // Add more cases for other roles as needed
          }
      }


      HomeAdapter adapter = new HomeAdapter(getActivity(),getContext(), courseModelArrayList, new OnListFragmentInteractionListener() {
          @Override
          public void onListFragmentInteraction(int pos) {

              setNavigationPage(courseModelArrayList.get(pos).getCourse_name());

          }
      });
      GridLayoutManager layoutManager=new GridLayoutManager(getContext(),2);

      // at last set adapter to recycler view.
      pickingRecyclerview.setLayoutManager(layoutManager);
      pickingRecyclerview.setAdapter(adapter);
  }

    public interface OnListFragmentInteractionListener {

        void onListFragmentInteraction(int pos);

    }


    public void setNavigationPage(String menuChildText){
        switch (menuChildText) {

            case "Receiving": {
                FragmentUtils.replaceFragmentWithBackStack(getActivity(), R.id.container_body, new PalletReceivingFragment());
            }
            break;


            case "Putaway": {
                FragmentUtils.replaceFragmentWithBackStack(getActivity(), R.id.container_body, new PutawayPalletTransfersFragment());
            }
            break;

            case "Pallet Transfers": {
                FragmentUtils.replaceFragmentWithBackStack(getActivity(), R.id.container_body, new PalletTransfersFragment());
            }
            break;

            case "Material Transfers": {
                FragmentUtils.replaceFragmentWithBackStack(getActivity(), R.id.container_body, new MaterialTransferFragment());
            }
            break;

            case "Bin to Bin": {
                FragmentUtils.replaceFragmentWithBackStack(getActivity(), R.id.container_body, new InnerTransferFragment());
            }
            break;
            case "Live Stock": {
                FragmentUtils.replaceFragmentWithBackStack(getActivity(), R.id.container_body, new LiveStockFragment());
            }
            break;


            case "Cycle Count": {
                FragmentUtils.replaceFragmentWithBackStack(getActivity(), R.id.container_body, new CcHeaderFragment());
            }
            break;

            case "Picking": {
                FragmentUtils.replaceFragmentWithBackStack(getActivity(), R.id.container_body, new GskPickingHeaderFragment());
            }
            break;



            case "Stock Take": {
                FragmentUtils.replaceFragmentWithBackStack(getActivity(), R.id.container_body, new StockTakeFragment());
            }
            break;


            case "VNA Putaway": {
                FragmentUtils.replaceFragmentWithBackStack(getActivity(), R.id.container_body, new ToInDropLocation());
            }

            break;

            case "VNA Auto Transfers": {
                FragmentUtils.replaceFragmentWithBackStack(getActivity(), R.id.container_body, new VNATransfersFragment());
            }

            break;

            default:
                break;
        }
    }
}