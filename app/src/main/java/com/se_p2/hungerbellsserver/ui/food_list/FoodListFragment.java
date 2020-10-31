package com.se_p2.hungerbellsserver.ui.food_list;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import dmax.dialog.SpotsDialog;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.se_p2.hungerbellsserver.eventBus.AddonSizeEditEvent;
import com.se_p2.hungerbellsserver.eventBus.ChangeMenuClick;
import com.se_p2.hungerbellsserver.eventBus.ToastEvent;
import com.se_p2.hungerbellsserver.R;
import com.se_p2.hungerbellsserver.SizeAddonEditActivity;
import com.se_p2.hungerbellsserver.adapter.MyFoodListAdapter;
import com.se_p2.hungerbellsserver.common.Common;
import com.se_p2.hungerbellsserver.common.MySwipeHelper;
import com.se_p2.hungerbellsserver.model.FoodModel;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FoodListFragment extends Fragment {

    private static final int PICK_IMAGE_REQUEST =1234 ;
    private ImageView img_food;
    FirebaseStorage storage;
    StorageReference storageReference;
    private android.app.AlertDialog dialog;

    private FoodListViewModel foodListViewModel;
    private List<FoodModel> foodModelList;

    Unbinder unbinder;
    @BindView(R.id.recycler_food_list)
    RecyclerView recyclerView_food_list;

    LayoutAnimationController layoutAnimationController;
    MyFoodListAdapter adapter;
    private Uri imageUri=null;


    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.food_list_menu,menu);

        MenuItem menuItem=menu.findItem(R.id.action_search);

        SearchManager searchManager= (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView= (SearchView) menuItem.getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));

        //Event
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                startSearchFood(s);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                return false;
            }
        });

        //Clear text
        ImageView closeButton=searchView.findViewById(R.id.search_close_btn);
        closeButton.setOnClickListener(view -> {
            EditText ed=searchView.findViewById(R.id.search_src_text);
            ed.setText("");
            searchView.setQuery("",false);
            searchView.onActionViewCollapsed();
            menuItem.collapseActionView();
            foodListViewModel.getMutableLiveDataFoodList().setValue(Common.categorySelected.getFoods());
        });

    }

    private void startSearchFood(String s) {
        List<FoodModel> resultFood=new ArrayList<>();
        for(int i=0;i<Common.categorySelected.getFoods().size();i++) {
            FoodModel foodModel = Common.categorySelected.getFoods().get(i);
            if (foodModel.getName().toLowerCase().contains(s.toLowerCase())) {
                foodModel.setPositionInList(i);
                resultFood.add(foodModel);
            }
        }
            foodListViewModel.getMutableLiveDataFoodList().setValue(resultFood);

    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        foodListViewModel =
                new ViewModelProvider(this).get(FoodListViewModel.class);
        View root = inflater.inflate(R.layout.fragment_food_list, container, false);
        unbinder= ButterKnife.bind(this,root);
        initViews();
        foodListViewModel.getMutableLiveDataFoodList().observe(getViewLifecycleOwner(), foodModels -> {
            if(foodModels !=null){
                foodModelList=foodModels;
                adapter=new MyFoodListAdapter(getContext(),foodModelList);
                recyclerView_food_list.setAdapter(adapter);
                recyclerView_food_list.setLayoutAnimation(layoutAnimationController);
            }
        }
        );
        return root;
    }
    private void initViews() {

        setHasOptionsMenu(true);

        dialog=new SpotsDialog.Builder().setContext(getContext()).setCancelable(false).build();
        storage=FirebaseStorage.getInstance();
        storageReference=storage.getReference();

        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(Common.categorySelected.getName());
        recyclerView_food_list.setHasFixedSize(true);
        recyclerView_food_list.setLayoutManager(new LinearLayoutManager(getContext()));
        layoutAnimationController= AnimationUtils.loadLayoutAnimation(getContext(),R.anim.layout_item_from_left);

        //window size
        DisplayMetrics displayMetrics=new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int width=displayMetrics.widthPixels;
        MySwipeHelper mySwipeHelper = new MySwipeHelper(getContext(), recyclerView_food_list, width/6) {
            @Override
            public void instantiateMyButton(RecyclerView.ViewHolder viewHolder, List<MyButton> buf) {
                buf.add(new MyButton(getContext(), "Delete", 30, 0, Color.parseColor("#9B0000"),
                        pos -> {
                            if(foodModelList!=null) {
                                Common.selectedFood = foodModelList.get(pos);
                                AlertDialog.Builder builder=new AlertDialog.Builder(getContext());
                                builder.setTitle("Delete")
                                        .setMessage("Do you want to delete this food ?")
                                        .setNegativeButton("CANCEL", (dialogInterface, i) -> dialogInterface.dismiss())
                                        .setPositiveButton("DELETE", (dialogInterface, i) -> {
                                            FoodModel foodModel=adapter.getItemAtPosition(pos);
                                            if(foodModel.getPositionInList()==-1){//default,do nothing
                                                Common.categorySelected.getFoods().remove(pos);
                                            }else{
                                                Common.categorySelected.getFoods().remove(foodModel.getPositionInList());//Remove by index
                                            }
                                            updateFood(Common.categorySelected.getFoods(), Common.ACTION.DELETE);
                                        });
                                AlertDialog deleteDialog=builder.create();
                                deleteDialog.show();
                            }

                        }));
                buf.add(new MyButton(getContext(), "Update", 30, 0, Color.parseColor("#560027"),
                        pos -> {
                            FoodModel foodModel=adapter.getItemAtPosition(pos);
                            if(foodModel.getPositionInList()==-1){//default,do nothing
                                showUpdateDialog(pos,foodModel);
                            }else{
                                showUpdateDialog(foodModel.getPositionInList(),foodModel);
                            }


                        }));

                buf.add(new MyButton(getContext(), "Size", 30, 0, Color.parseColor("#12005E"),
                        pos -> {
                            FoodModel foodModel=adapter.getItemAtPosition(pos);
                            if(foodModel.getPositionInList()==-1) {//default,do nothing
                                Common.selectedFood = foodModelList.get(pos);
                            }else{
                                Common.selectedFood=foodModel;
                            }
                            startActivity(new Intent(getContext(), SizeAddonEditActivity.class));

                            if(foodModel.getPositionInList()==-1) {
                                EventBus.getDefault().postSticky(new AddonSizeEditEvent(false, pos));
                            }else{
                                EventBus.getDefault().postSticky(new AddonSizeEditEvent(false, foodModel.getPositionInList()));
                            }
                        }));
                buf.add(new MyButton(getContext(), "Addon", 30, 0, Color.parseColor("#336699"),
                        pos -> {
                            FoodModel foodModel=adapter.getItemAtPosition(pos);
                            if(foodModel.getPositionInList()==-1) {//default,do nothing
                                Common.selectedFood = foodModelList.get(pos);
                            }else{
                                Common.selectedFood=foodModel;
                            }
                            startActivity(new Intent(getContext(), SizeAddonEditActivity.class));

                            if(foodModel.getPositionInList()==-1) {
                                EventBus.getDefault().postSticky(new AddonSizeEditEvent(true, pos));
                            }else{
                                EventBus.getDefault().postSticky(new AddonSizeEditEvent(true, foodModel.getPositionInList()));

                            }

                        }));
            }
        };

    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() ==R.id.action_create){
            showAddDialog();
        }
        return super.onOptionsItemSelected(item);
    }

    private void showAddDialog() {
        android.app.AlertDialog.Builder builder=new android.app.AlertDialog.Builder(getContext());
        builder.setTitle("Create");
        builder.setMessage("Please fill information");

        View itemView=LayoutInflater.from(getContext()).inflate(R.layout.layout_update_food,null);
        EditText edt_food_name=itemView.findViewById(R.id.edt_food_name);
        EditText edt_food_price=itemView.findViewById(R.id.edt_food_price);
        EditText edt_food_description=itemView.findViewById(R.id.edt_food_description);
        img_food=itemView.findViewById(R.id.img_food);

        Glide.with(getContext()).load(R.drawable.ic_launcher_foreground).into(img_food);

        img_food.setOnClickListener(view -> {
            Intent intent=new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent,"Select Picture"),PICK_IMAGE_REQUEST);
        });

        builder.setNegativeButton("CANCEL", (dialogInterface, i) -> dialogInterface.dismiss())
                .setPositiveButton("CREATE", (dialogInterface, i) -> {

                    FoodModel updateFood=new FoodModel();

                    updateFood.setName(edt_food_name.getText().toString());
                    updateFood.setId(UUID.randomUUID().toString());
                    updateFood.setDescription(edt_food_description.getText().toString());
                    updateFood.setPrice(TextUtils.isEmpty(edt_food_price.getText()) ? 0:
                            Long.parseLong(edt_food_price.getText().toString()));
                    if(imageUri!=null){
                        dialog.setMessage("Uploading...");
                        dialog.show();

                        String imageName= UUID.randomUUID().toString();
                        StorageReference imageFolder=storageReference.child("images/"+imageName);

                        imageFolder.putFile(imageUri)
                                .addOnFailureListener(e -> {
                                    dialogInterface.dismiss();
                                    Toast.makeText(getContext(),""+e.getMessage(),Toast.LENGTH_SHORT).show();
                                }).addOnCompleteListener(task -> {
                            dialog.dismiss();
                            imageFolder.getDownloadUrl().addOnSuccessListener(uri -> {
                                updateFood.setImage(uri.toString());
                                if(Common.categorySelected.getFoods() ==null){
                                    Common.categorySelected.setFoods(new ArrayList<>());
                                }
                                Common.categorySelected.getFoods().add(updateFood);
                                updateFood(Common.categorySelected.getFoods(), Common.ACTION.CREATE);
                            });
                        }).addOnProgressListener(taskSnapshot ->{
                            double progress=(100.0 *taskSnapshot.getBytesTransferred()/taskSnapshot.getTotalByteCount());
                            dialog.setMessage("Uploading: "+progress+"%");
                        });

                    }else{
                        if(Common.categorySelected.getFoods() ==null){
                            Common.categorySelected.setFoods(new ArrayList<>());
                        }
                        Common.categorySelected.getFoods().add(updateFood);
                        updateFood(Common.categorySelected.getFoods(), Common.ACTION.CREATE);
                    }
                });

        builder.setView(itemView);
        android.app.AlertDialog updateDialog=builder.create();
        updateDialog.show();
    }

    private void showUpdateDialog(int pos, FoodModel foodModel) {
        android.app.AlertDialog.Builder builder=new android.app.AlertDialog.Builder(getContext());
        builder.setTitle("Update");
        builder.setMessage("Please fill information");

        View itemView=LayoutInflater.from(getContext()).inflate(R.layout.layout_update_food,null);
        EditText edt_food_name=itemView.findViewById(R.id.edt_food_name);
        EditText edt_food_price=itemView.findViewById(R.id.edt_food_price);
        EditText edt_food_description=itemView.findViewById(R.id.edt_food_description);
        img_food=itemView.findViewById(R.id.img_food);

        //Set data
        edt_food_name.setText(new StringBuilder("")
        .append(foodModel.getName()));
        edt_food_price.setText(new StringBuilder("")
                .append(foodModel.getPrice()));
        edt_food_description.setText(new StringBuilder("")
                .append(foodModel.getDescription()));

        Glide.with(getContext()).load(foodModel.getImage()).into(img_food);

        img_food.setOnClickListener(view -> {
            Intent intent=new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent,"Select Picture"),PICK_IMAGE_REQUEST);
        });

        builder.setNegativeButton("CANCEL", (dialogInterface, i) -> dialogInterface.dismiss())
                .setPositiveButton("UPDATE", (dialogInterface, i) -> {
                    foodModel.setName(edt_food_name.getText().toString());
                    foodModel.setDescription(edt_food_description.getText().toString());
                    foodModel.setPrice(TextUtils.isEmpty(edt_food_price.getText()) ? 0:
                            Long.parseLong(edt_food_price.getText().toString()));
                    if(imageUri!=null){
                        dialog.setMessage("Uploading...");
                        dialog.show();

                        String imageName= UUID.randomUUID().toString();
                        StorageReference imageFolder=storageReference.child("images/"+imageName);

                        imageFolder.putFile(imageUri)
                                .addOnFailureListener(e -> {
                                    dialogInterface.dismiss();
                                    Toast.makeText(getContext(),""+e.getMessage(),Toast.LENGTH_SHORT).show();
                                }).addOnCompleteListener(task -> {
                            dialog.dismiss();
                            imageFolder.getDownloadUrl().addOnSuccessListener(uri -> {
                                foodModel.setImage(uri.toString());
                                updateFood(Common.categorySelected.getFoods(), Common.ACTION.UPDATE);
                            });
                        }).addOnProgressListener(taskSnapshot ->{
                            double progress=(100.0 *taskSnapshot.getBytesTransferred()/taskSnapshot.getTotalByteCount());
                            dialog.setMessage("Uploading: "+progress+"%");
                        });

                    }else{
                        Common.categorySelected.getFoods().set(pos, foodModel);
                        updateFood(Common.categorySelected.getFoods(), Common.ACTION.UPDATE);
                    }
                });

        builder.setView(itemView);
        android.app.AlertDialog updateDialog=builder.create();
        updateDialog.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==PICK_IMAGE_REQUEST && resultCode== Activity.RESULT_OK){
            if(data!=null && data.getData()!=null){
                imageUri=data.getData();
                img_food.setImageURI(imageUri);
            }
        }
    }

    private void updateFood(List<FoodModel> foods,Common.ACTION action) {
        Map<String,Object> updateData=new HashMap<>();
        updateData.put("foods",foods);

        FirebaseDatabase.getInstance()
                .getReference(Common.CATEGORY_REF)
                .child(Common.categorySelected.getMenu_id())
                .updateChildren(updateData)
                .addOnFailureListener(e -> Toast.makeText(getContext(),""+e.getMessage(),Toast.LENGTH_SHORT).show())
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful()){
                        foodListViewModel.getMutableLiveDataFoodList();
                        EventBus.getDefault().postSticky(new ToastEvent(action,true));
                    }
                });
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().postSticky(new ChangeMenuClick(true));
        super.onDestroy();
    }
}