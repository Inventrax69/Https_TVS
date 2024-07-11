 package com.inventrax.tvs.fragments;


import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.inventrax.tvs.R;
import com.inventrax.tvs.adapters.NavigationDrawerAdapter;
import com.inventrax.tvs.adapters.NewExpandableListAdapter;
import com.inventrax.tvs.model.NavDrawerItem;
import com.inventrax.tvs.util.DialogUtils;
import com.inventrax.tvs.util.FragmentUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

 /**
  * Created by Padmaja.B on 20/12/2018.
  */

 public class DrawerFragment extends Fragment implements View.OnClickListener {

     private static String TAG = com.inventrax.tvs.fragments.DrawerFragment.class.getSimpleName();
     private RecyclerView recyclerView;
     private ActionBarDrawerToggle mDrawerToggle;
     private DrawerLayout mDrawerLayout;
     private NavigationDrawerAdapter adapter;
     private View containerView;
     private FragmentDrawerListener drawerListener;
     private View layout;
     private TextView txtLoginUser,txtHome;
     private AppCompatActivity appCompatActivity;
     private List<NavDrawerItem> menuItemList;
     private IntentFilter mIntentFilter;


     private String userName = "",scanType="",UserRole="",DepartmentIDs="";
     List<String> listDataParent;
     HashMap<String, List<String>> listDataChild;
     ExpandableListView expandable_list_view;
     NewExpandableListAdapter.OnItemClick onItemClick;

     RelativeLayout rr;
     boolean isSupervisor=false;


     public void setDrawerListener(FragmentDrawerListener listener) {
         this.drawerListener = listener;
     }

     @Override
     public void onCreate(Bundle savedInstanceState) {
         super.onCreate(savedInstanceState);
     }

     @Override
     public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
         // Inflating view layout
         layout = inflater.inflate(R.layout.fragment_navigation_drawer, container, false);

         appCompatActivity = (AppCompatActivity) getActivity();

         menuItemList = new ArrayList<>();

         loadFormControls();

         return layout;
     }

     private void createListData() {


         listDataParent = new ArrayList<String>();
         listDataChild = new HashMap<String, List<String>>();

     if(DepartmentIDs.equals("") || DepartmentIDs ==null) {
            DepartmentIDs ="1,2,3,4,5,6,7,8,9,10,11";
         }

         String[] stringsUserRole = DepartmentIDs.split(",");
         List<String> mainListInbound = new ArrayList<String>();
         List<String> mainListOutbound  = new ArrayList<String>();
         List<String> mainListHouseKeeping = new ArrayList<String>();

             for (String role : stringsUserRole) {
                 switch (role) {
                     case "1":
                         mainListInbound.add("Receiving");
                         break;
                     case "2":
                         mainListInbound.add("AGV Putaway");
                         break;
                     case "3":
                         mainListOutbound.add("Picking");
                         break;
                     case "4":
                         mainListHouseKeeping.add("Bin to Bin");
                         break;
                     case "5":
                         mainListHouseKeeping.add("Live Stock");
                         break;
                     case "6":
                         mainListHouseKeeping.add("Cycle Count");
                         break;
                    case "7":
                         mainListHouseKeeping.add("Stock Take");
                         break;
                     case "8":
                         mainListHouseKeeping.add("Material Transfers");
                         break;
                     case "9":
                         mainListInbound.add("AGV Auto Transfers");
                         break;
                     case "10":
                         mainListInbound.add("Putaway");
                         break;
                     case "11":
                         mainListInbound.add("Item  Putaway");
                         break;

                 }
             }

         if (mainListInbound.size() > 0) {
             listDataParent.add("Inbound");
             listDataChild.put(listDataParent.get(listDataParent.size() - 1), mainListInbound);
         }

         if (mainListOutbound.size() > 0) {
             listDataParent.add("Outbound");
             listDataChild.put(listDataParent.get(listDataParent.size() - 1), mainListOutbound);
         }

         if (mainListHouseKeeping.size() > 0) {
             listDataParent.add("House Keeping");
             listDataChild.put(listDataParent.get(listDataParent.size() - 1), mainListHouseKeeping);
         }

         NewExpandableListAdapter listAdapter = new NewExpandableListAdapter(getActivity(), listDataParent, listDataChild, new NewExpandableListAdapter.OnItemClick() {

             @Override
             public void onItemClick(int gpos, int cpos, String text) {
                 mDrawerLayout.closeDrawer(containerView);
                 setNavigationPage(text);
             }
         });

         expandable_list_view.setAdapter(listAdapter);
         expandable_list_view.expandGroup(0);
         expandable_list_view.setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {
             int previousGroup = 0;

             @Override
             public void onGroupExpand(int groupPosition) {
                 if(groupPosition != previousGroup)
                     expandable_list_view.collapseGroup(previousGroup);
                 previousGroup = groupPosition;
             }
         });
     }

     private void createListDataAuto() {


         listDataParent = new ArrayList<String>();
         listDataChild = new HashMap<String, List<String>>();

         // Adding child data
        // listDataParent.add("Inbound");
         listDataParent.add("Outbound");
       //  listDataParent.add("House Keeping");

         // Adding child data List one
         List<String> mainListInbound = new ArrayList<String>();
         mainListInbound.add("Receiving");
//         mainListInbound.add("Palletization");
 //        mainListInbound.add("Putaway");
         //mainListInbound.add("Pallet Transfers");
         mainListInbound.add("Putaway");

         // Adding child data List two
         List<String> mainListOutbound  = new ArrayList<String>();
        // mainListOutbound.add("OBD Picking");
 //        mainListOutbound.add("Packing");
         mainListOutbound.add("Packing Info");
         mainListOutbound.add("Load Generation");
         mainListOutbound.add("Loading");
//         mainListOutbound.add("Sorting");
         mainListOutbound.add("Picking");
         mainListOutbound.add("WorkOrder Pick");
         mainListInbound.add("Internal Transfer");

         // Adding child data List three
         List<String> mainListHouseKeeping = new ArrayList<String>();
         if(isSupervisor){
             mainListHouseKeeping.add("Material Transfers");
         }
         mainListHouseKeeping.add("Bin to Bin");
         mainListHouseKeeping.add("Live Stock");
         mainListHouseKeeping.add("Cycle Count");
         mainListHouseKeeping.add("Stock Take");
        // mainListHouseKeeping.add("Flagged Bins");

       //  listDataChild.put(listDataParent.get(0), mainListInbound); // Header, Child data
         listDataChild.put(listDataParent.get(0), mainListOutbound); // Header, Child data
        // listDataChild.put(listDataParent.get(2), mainListHouseKeeping); // Header, Child data

         NewExpandableListAdapter listAdapter = new NewExpandableListAdapter(getActivity(), listDataParent, listDataChild, new NewExpandableListAdapter.OnItemClick() {

             @Override
             public void onItemClick(int gpos, int cpos, String text) {
                 mDrawerLayout.closeDrawer(containerView);
                 setNavigationPage(text);
             }
         });

         expandable_list_view.setAdapter(listAdapter);
         expandable_list_view.expandGroup(0);
         expandable_list_view.setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {
             int previousGroup = 0;

             @Override
             public void onGroupExpand(int groupPosition) {
                 if(groupPosition != previousGroup)
                     expandable_list_view.collapseGroup(previousGroup);
                 previousGroup = groupPosition;
             }
         });
     }

     public void loadFormControls(){
         try {
             SharedPreferences sp = getContext().getSharedPreferences("LoginActivity", Context.MODE_PRIVATE);
             userName = sp.getString("UserName", "");
             scanType = sp.getString("scanType", "");
             UserRole = sp.getString("UserRole", "");
             DepartmentIDs= sp.getString("DepartmentIDs", "");

             if(!UserRole.isEmpty()){
                 String[] stringsUserRole=UserRole.split("[,]");
                 for (String s : stringsUserRole) {
                     if (s.trim().equals("6") || s.trim().equals("11") ) {
                         isSupervisor = true;
                         break;
                     } else {
                         isSupervisor = false;
                     }
                 }
             }else{
                 isSupervisor=false;
             }


             mIntentFilter = new IntentFilter();
             mIntentFilter.addAction("com.example.broadcast.counter");

             txtLoginUser = (TextView) layout.findViewById(R.id.txtLoginUser);
             txtHome = (TextView) layout.findViewById(R.id.txtHome);

             rr = (RelativeLayout) layout.findViewById(R.id.rr);

             txtLoginUser.setText(userName);

 /*            Fragment fragment = new HomeFragment();
             if(fragment != null && fragment.isVisible() && fragment instanceof HomeFragment ){
                 rr.setBackgroundColor(Color.parseColor("#000000"));
             }else{
                 rr.setBackgroundColor(Color.parseColor("#FF0000"));
             }*/

             txtHome.setOnClickListener(new View.OnClickListener() {
                 @Override
                 public void onClick(View v) {
                     mDrawerLayout.closeDrawer(containerView);
                     FragmentUtils.replaceFragmentWithBackStack(getActivity(), R.id.container_body, new HomeFragment());
                 }
             });

             expandable_list_view = (ExpandableListView) layout.findViewById(R.id.expandable_list_view);

             if(scanType.equals("Auto")){
                 createListDataAuto();
             }else{
                 createListData();
             }


         }catch (Exception ex) {
             DialogUtils.showAlertDialog(getActivity(), "Error while loading menu list");
             return;
         }
     }

     /*    public void loadFormControls() {
         try {

             SharedPreferences sp = getContext().getSharedPreferences("LoginActivity", Context.MODE_PRIVATE);
             userName = sp.getString("UserName", "");
             mIntentFilter = new IntentFilter();
             mIntentFilter.addAction("com.example.broadcast.counter");

             counterBroadcastReceiver = new CounterBroadcastReceiver();

             menuItemList = getMenuItemsByUserType("1");

             new ProgressDialogUtils(getContext());

             recyclerView = (RecyclerView) layout.findViewById(R.id.drawerList);
             txtLoginUser = (TextView) layout.findViewById(R.id.txtLoginUser);

             txtLoginUser.setText(userName);


             adapter = new NavigationDrawerAdapter(getActivity(), menuItemList);
             recyclerView.setAdapter(adapter);
             recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
             recyclerView.addOnItemTouchListener(new RecyclerTouchListener(getActivity(), recyclerView, new ClickListener() {
                 @Override
                 public void onClick(View view, int position) {

                     NavigationDrawerAdapter.setSelectedItemPosition(position);
                     recyclerView.getAdapter().notifyDataSetChanged();
                     drawerListener.onDrawerItemSelected(view, position, menuItemList.get(position));
                     mDrawerLayout.closeDrawer(containerView);

                 }

                 @Override
                 public void onLongClick(View view, int position) {

                 }
             }));


         } catch (Exception ex) {
             //Logger.Log(DrawerFragment.class.getName(), ex);
             DialogUtils.showAlertDialog(getActivity(), "Error while loading menu list");
             return;
         }
     }*/

     @Override
     public void onActivityCreated(Bundle savedInstanceState) {
         super.onActivityCreated(savedInstanceState);

     }

     public void setUp(int fragmentId, androidx.drawerlayout.widget.DrawerLayout drawerLayout, final Toolbar toolbar) {
         try {
             containerView = getActivity().findViewById(fragmentId);
             mDrawerLayout = drawerLayout;
             mDrawerToggle = new ActionBarDrawerToggle(getActivity(), drawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close) {
                 @Override
                 public void onDrawerOpened(View drawerView) {
                     super.onDrawerOpened(drawerView);
                     getActivity().invalidateOptionsMenu();
                 }

                 @Override
                 public void onDrawerClosed(View drawerView) {
                     super.onDrawerClosed(drawerView);
                     getActivity().invalidateOptionsMenu();
                 }

                 @Override
                 public void onDrawerSlide(View drawerView, float slideOffset) {
                     super.onDrawerSlide(drawerView, slideOffset);
                     toolbar.setAlpha(1 - slideOffset / 2);
                 }
             };



             mDrawerLayout.setDrawerListener(mDrawerToggle);
             mDrawerLayout.post(new Runnable() {
                 @Override
                 public void run() {
                     mDrawerToggle.syncState();
                 }
             });
         } catch (Exception ex) {
             // Logger.Log(DrawerFragment.class.getName(),ex);
             return;
         }

     }

     @Override
     public void onClick(View v) {
         switch (v.getId()) { }
     }

     @Override
     public void onResume() {
         super.onResume();

     }

     @Override
     public void onPause() {
         super.onPause();

     }


     public void setNavigationPage(String menuChildText){
         switch (menuChildText) {
          /*   case "Receiving": {
                 FragmentUtils.replaceFragmentWithBackStack(getActivity(), R.id.container_body, new UnloadingFragment());
             }
             break;*/
             case "Receiving": {
                 FragmentUtils.replaceFragmentWithBackStack(getActivity(), R.id.container_body, new PalletReceivingFragment());
             }
             break;

             case "Putaway": {
                 FragmentUtils.replaceFragmentWithBackStack(getActivity(), R.id.container_body, new PutawayPalletTransfersFragment());
             }
             break;

             case "Item  Putaway": {
                 FragmentUtils.replaceFragmentWithBackStack(getActivity(), R.id.container_body, new ItemPutawayFragment());
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

             case "Internal Transfer": {
                 FragmentUtils.replaceFragmentWithBackStack(getActivity(), R.id.container_body, new InternalPickingHeaderFragment());
             }
             break;

             case "Stock Take": {
                 FragmentUtils.replaceFragmentWithBackStack(getActivity(), R.id.container_body, new StockTakeFragment());
             }
             break;

             case "AGV Putaway": {
                 FragmentUtils.replaceFragmentWithBackStack(getActivity(), R.id.container_body, new ToInDropLocation());
             }

             break;

             case "AGV Auto Transfers": {
                 FragmentUtils.replaceFragmentWithBackStack(getActivity(), R.id.container_body, new VNATransfersFragment());
             }

             break;

             default:
             break;
         }
     }





     public interface ClickListener {
         void onClick(View view, int position);

         void onLongClick(View view, int position);
     }

     public interface FragmentDrawerListener {
         void onDrawerItemSelected(View view, int position, NavDrawerItem menuItem);
     }

     static class RecyclerTouchListener implements RecyclerView.OnItemTouchListener {

         private GestureDetector gestureDetector;
         private ClickListener clickListener;

         public RecyclerTouchListener(Context context, final RecyclerView recyclerView, final ClickListener clickListener) {
             this.clickListener = clickListener;
             gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
                 @Override
                 public boolean onSingleTapUp(MotionEvent e) {
                     return true;
                 }

                 @Override
                 public void onLongPress(MotionEvent e) {
                     View child = recyclerView.findChildViewUnder(e.getX(), e.getY());
                     if (child != null && clickListener != null) {
                         clickListener.onLongClick(child, recyclerView.getChildPosition(child));
                     }
                 }
             });
         }

         @Override
         public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {

             try {

                 View child = rv.findChildViewUnder(e.getX(), e.getY());
                 if (child != null && clickListener != null && gestureDetector.onTouchEvent(e)) {
                     clickListener.onClick(child, rv.getChildPosition(child));
                 }
                 return false;

             } catch (Exception ex) {
                 // Logger.Log(DrawerFragment.class.getName(),ex);
                 return false;

             }
         }

         @Override
         public void onTouchEvent(RecyclerView rv, MotionEvent e) {
         }

         @Override
         public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

         }
     }



 }