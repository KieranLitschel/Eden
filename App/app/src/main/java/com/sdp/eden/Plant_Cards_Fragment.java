package com.sdp.eden;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.SyncStateContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.TextView;
import com.google.firebase.auth.FirebaseAuth;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Objects;

public class Plant_Cards_Fragment extends Fragment {

    private static final String TAG = "PlantListFragment";
    private ArrayList<Plant> plants; // list of plants pulled from firebase
    private RecyclerView recyclerView;
    private ImageView plantPic;

    public View onCreateView (@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.fragment_plants, container, false);
        getLatestPlantList();    // Query the database to get latest list
        recyclerView = view.findViewById(R.id.Plants_Recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity())); // sets layout for recycler view, linear list in this case
        return view;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        FloatingActionButton addPlantButton = view.findViewById(R.id.addPlantButton);
        addPlantButton.setOnClickListener(v -> {
            Log.d(TAG, "User input: click on Add");

            AlertDialog.Builder builder = new AlertDialog.Builder(Objects.requireNonNull(getActivity()));
            builder.setTitle("Add a new plant!");

            View viewInflated = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_add_plant, (ViewGroup) getView(), false);

            final EditText plantName = viewInflated.findViewById(R.id.plantName);
            final Spinner plantSpecies = viewInflated.findViewById(R.id.plantSpecies);
            plantPic = viewInflated.findViewById(R.id.plantPic);
            builder.setView(viewInflated);

            // TODO: Kieran W - Maybe add more species here for the user interview?
            // TODO: perhaps changing specicies to a short description of the plant (E.G. location or characteristic)
            String[] species = new String[]{"Select Species","cacti","daisy","lily","orchid"};
            ArrayAdapter<String> speciesAdapter = new ArrayAdapter<>(Objects.requireNonNull(getContext()), R.layout.species_option, species);
            plantSpecies.setAdapter(speciesAdapter); // creates the drop down selection

            plantPic.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE); // kieran - opens camera
                    // TODO: Kieran - upload to firebase storage and pull for each card (having trouble with this)
                    if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
                        startActivityForResult(takePictureIntent, 111);
                    }
                }
            });

            builder.setPositiveButton("Add", (dialog, which) -> {
                Log.d(TAG, "New plant to add to database:");
                Log.d(TAG, "Plant name: " + plantName.getText().toString());
                Log.d(TAG, "Plant species: " + plantSpecies.getSelectedItem().toString()); // extracts the plant data from user input

                // Checks for empty plant name
                if (plantName.getText().toString().trim().length() == 0) {
                    Log.d(TAG, "Plant name format incorrect. Rejected further operations.");
                    Snackbar.make(Objects.requireNonNull(getView()).findViewById(R.id.viewSnack), "Name your plant!", Snackbar.LENGTH_SHORT).show();
                }
                else {
                    Log.d(TAG, "Plant name format correct.");

                    // For now any new plant would be added with this default drawable.
                    // To implement actual photo functionality later.
                    Plant plant = new Plant(plantName.getText().toString(),
                            plantSpecies.getSelectedItem().toString(),
                            R.drawable.plant1);
                    DbOps.instance.addPlant(plant, success -> getLatestPlantList());
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 111 && resultCode == getActivity().RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            plantPic.setImageBitmap(imageBitmap);
            //encodeBitmapAndSaveToFirebase(imageBitmap); // TODO upload image to fire base for each card
        }
    }




    @Override
    public void onResume() {
        super.onResume();
        getLatestPlantList();    // Query the database to get latest list
    }

    public static Fragment newInstance() {
        return new Plant_Cards_Fragment(); // new instance of the fragment
    }

    private void populateRecyclerView(ArrayList<Plant> plants){ // Bianca - changed this to accept a list parameter
        RecyclerViewAdapter adapter = new RecyclerViewAdapter(plants);
        recyclerView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }


    // Queries the database to get the most recent list of plants
    public void getLatestPlantList() {
        DbOps.instance.getPlantList(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getEmail(),
                plantsFromDB -> {
                    if (plantsFromDB==null) return;

                    Log.d(TAG, "Obtained list of plants from DB of size: "+plantsFromDB.size());

                    // Refreshes the recyclerview:
                    plants = new ArrayList<>(plantsFromDB);
                    populateRecyclerView(new ArrayList<>(plantsFromDB)); // calling method to display the list
                });
    }


    private class RecyclerViewHolder extends RecyclerView.ViewHolder{

        private CardView mCardView; // card for data display
        private TextView plantName; // plants name from firebase
        private TextView plantDetail; // plants detail (currently species)
        private ImageView plantImage; // plants picture


        RecyclerViewHolder(LayoutInflater inflater, ViewGroup container){
            super(inflater.inflate(R.layout.card_layout, container, false));
            //finding the location for each container in the display card
            mCardView = itemView.findViewById(R.id.card_view);
            plantName = itemView.findViewById(R.id.card_plant_name);
            plantDetail = itemView.findViewById(R.id.card_plant_detail);
            plantImage = itemView.findViewById(R.id.card_plant_image);

        }
    }

    private class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewHolder>{

        ArrayList<Plant> plantsList; // plants list

        RecyclerViewAdapter(ArrayList<Plant> list){
            this.plantsList = list;
        } // Adapter for the recycler view

        @NonNull
        @Override
        public RecyclerViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            LayoutInflater inflater = LayoutInflater.from(getActivity()); // inflates the view to the fragment
            return new RecyclerViewHolder(inflater, viewGroup);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerViewHolder recyclerViewHolder, int i) {
            ImageButton mImageButton = recyclerViewHolder.mCardView.findViewById(R.id.popup_menu); // creates the drop down menu in each card

            recyclerViewHolder.plantName.setText(plantsList.get(i).getName()); // populating the cards with the object details
            recyclerViewHolder.plantDetail.setText(plantsList.get(i).getSpecies());
            recyclerViewHolder.plantImage.setImageResource(plantsList.get(i).getPhoto());

            //snackbar location
            recyclerViewHolder.mCardView.setOnClickListener(v -> Snackbar.make(Objects.requireNonNull(getView()).findViewById(R.id.viewSnack), "Name: " + plantsList.get(i).getName(), Snackbar.LENGTH_SHORT).show());
            //calls method to display menu
            mImageButton.setOnClickListener(v -> showPopupMenu(mImageButton, i));

        }


        private void showPopupMenu(View view,int position) {
            // inflate menu
            PopupMenu popup = new PopupMenu(view.getContext(),view );
            MenuInflater inflater = popup.getMenuInflater();
            inflater.inflate(R.menu.card_menu, popup.getMenu());
            popup.setOnMenuItemClickListener(new MyMenuItemClickListener(position));
            popup.show();
        }


        @Override
        public int getItemCount() {
            return plantsList.size();
        }
    }


    class MyMenuItemClickListener implements PopupMenu.OnMenuItemClickListener { // class for when an item is clicked withing the popup menu

        private int position;

        MyMenuItemClickListener(int positon) {
            this.position=positon;
        }

        @Override
        public boolean onMenuItemClick(MenuItem menuItem) {
            switch (menuItem.getItemId()) {

                case R.id.card_delete: // delete is selected
                    DbOps.instance.deletePlant(plants.get(position), success -> getLatestPlantList());
                    return true;
                case R.id.card_edit: // edit is selected
                    Snackbar.make(Objects.requireNonNull(getView()).findViewById(R.id.viewSnack), "selected edit on plant: " + plants.get(position).getName(), Snackbar.LENGTH_SHORT).show();
                    return true;
            }
            return false;
        }
    }

}