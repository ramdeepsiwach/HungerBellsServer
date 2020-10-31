package com.se_p2.hungerbellsserver.ui.category;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
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
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import dmax.dialog.SpotsDialog;

import com.bumptech.glide.Glide;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.se_p2.hungerbellsserver.eventBus.ToastEvent;
import com.se_p2.hungerbellsserver.R;
import com.se_p2.hungerbellsserver.adapter.MyCategoriesAdapter;
import com.se_p2.hungerbellsserver.common.Common;
import com.se_p2.hungerbellsserver.common.MySwipeHelper;
import com.se_p2.hungerbellsserver.model.CategoryModel;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CategoryFragment extends Fragment {

    private static final int PICK_IMAGE_REQUEST =1234 ;
    private CategoryViewModel categoryViewModel;

    Unbinder unbinder;

    @BindView(R.id.recycler_menu)
    RecyclerView recycler_menu;
    AlertDialog dialog;
    LayoutAnimationController layoutAnimationController;
    MyCategoriesAdapter adapter;

    List<CategoryModel> categoryModels;

    ImageView category_image;
    private Uri imageUri=null;
    FirebaseStorage storage;
    StorageReference storageReference;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        categoryViewModel =
                new ViewModelProvider(this).get(CategoryViewModel.class);
        View root = inflater.inflate(R.layout.fragment_category, container, false);

        unbinder= ButterKnife.bind(this,root);
        initViews();

        categoryViewModel.getMessageError().observe(getViewLifecycleOwner(), s -> {
            Toast.makeText(getContext(),""+s,Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
        categoryViewModel.getCategoryListMutable().observe(getViewLifecycleOwner(), categoryModelList ->{
            dialog.dismiss();
            categoryModels=categoryModelList;
            adapter=new MyCategoriesAdapter(getContext(),categoryModels);
            recycler_menu.setAdapter(adapter);
            recycler_menu.setLayoutAnimation(layoutAnimationController);
        } );
        return root;
    }

    private void initViews() {
        storage=FirebaseStorage.getInstance();
        storageReference=storage.getReference();

        dialog=new SpotsDialog.Builder().setContext(getContext()).setCancelable(false).build();
        //dialog.show();
        layoutAnimationController= AnimationUtils.loadLayoutAnimation(getContext(),R.anim.layout_item_from_left);
        LinearLayoutManager layoutManager=new LinearLayoutManager(getContext());
        recycler_menu.setLayoutManager(layoutManager);
        recycler_menu.addItemDecoration(new DividerItemDecoration(getContext(),layoutManager.getOrientation()));

        MySwipeHelper mySwipeHelper = new MySwipeHelper(getContext(), recycler_menu, 300) {
            @Override
            public void instantiateMyButton(RecyclerView.ViewHolder viewHolder, List<MyButton> buf) {
                buf.add(new MyButton(getContext(), "Delete", 30, 0, Color.parseColor("#333639"),
                        pos -> {
                            Common.categorySelected=categoryModels.get(pos);
                            showDeleteDialog();
                        }));
                buf.add(new MyButton(getContext(), "Update", 30, 0, Color.parseColor("#FF3C30"),
                        pos -> {
                        Common.categorySelected=categoryModels.get(pos);
                        showUpdateDialog();
                        }));
            }
        };

        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.action_bar_menu,menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() ==R.id.action_create){
            showAddDialog();
        }
        return super.onOptionsItemSelected(item);
    }

    private void showAddDialog() {
        AlertDialog.Builder builder=new AlertDialog.Builder(getContext());
        builder.setTitle("Create");
        builder.setMessage("Please fill information");

        View itemView=LayoutInflater.from(getContext()).inflate(R.layout.layout_update_category,null);
        EditText edt_category_name=itemView.findViewById(R.id.edt_category_name);
        category_image=itemView.findViewById(R.id.img_category);

        Glide.with(getContext()).load(R.drawable.ic_launcher_foreground).into(category_image);

        category_image.setOnClickListener(view -> {
            Intent intent=new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent,"Select Picture"),PICK_IMAGE_REQUEST);
        });

        builder.setNegativeButton("CANCEL", (dialogInterface, i) -> dialogInterface.dismiss());
        builder.setPositiveButton("CREATE", (dialogInterface, i) -> {

            CategoryModel categoryModel=new CategoryModel();
            categoryModel.setName(edt_category_name.getText().toString());
            categoryModel.setFoods(new ArrayList<>());

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
                        categoryModel.setImage(uri.toString());
                        addCategory(categoryModel);
                    });
                }).addOnProgressListener(taskSnapshot ->{
                    double progress=(100.0 *taskSnapshot.getBytesTransferred()/taskSnapshot.getTotalByteCount());
                    dialog.setMessage("Uploading: "+progress+"%");
                });

            }else {
                addCategory(categoryModel);
            }
        });

        builder.setView(itemView);
        AlertDialog dialog=builder.create();
        dialog.show();
    }

    private void addCategory(CategoryModel categoryModel) {
        FirebaseDatabase.getInstance()
                .getReference(Common.CATEGORY_REF)
                .push()
                .setValue(categoryModel)
                .addOnFailureListener(e -> Toast.makeText(getContext(),""+e.getMessage(),Toast.LENGTH_SHORT).show())
                .addOnCompleteListener(task -> {
                    categoryViewModel.loadCategories();
                    EventBus.getDefault().postSticky(new ToastEvent(Common.ACTION.CREATE,true));
                });

    }

    private void showDeleteDialog() {
        AlertDialog.Builder builder=new AlertDialog.Builder(getContext());
        builder.setTitle("Delete");
        builder.setMessage("Do you really want to delete this category ?");
        builder.setNegativeButton("CANCEL", (dialog, which) -> dialog.dismiss());
        builder.setPositiveButton("Delete", (dialog, which) -> deleteCategory());

        AlertDialog dialog=builder.create();
        dialog.show();
    }

    private void deleteCategory() {
        FirebaseDatabase.getInstance()
                .getReference(Common.CATEGORY_REF)
                .child(Common.categorySelected.getMenu_id())
                .removeValue()
                .addOnFailureListener(e -> Toast.makeText(getContext(),""+e.getMessage(),Toast.LENGTH_SHORT).show())
                .addOnCompleteListener(task -> {
                    categoryViewModel.loadCategories();
                    EventBus.getDefault().postSticky(new ToastEvent( Common.ACTION.DELETE,true));
                });
    }

    private void showUpdateDialog() {
        AlertDialog.Builder builder=new AlertDialog.Builder(getContext());
        builder.setTitle("Update");
        builder.setMessage("Please fill information");

        View itemView=LayoutInflater.from(getContext()).inflate(R.layout.layout_update_category,null);
        EditText edt_category_name=itemView.findViewById(R.id.edt_category_name);
        category_image=itemView.findViewById(R.id.img_category);

        edt_category_name.setText(new StringBuilder().append(Common.categorySelected.getName()));
        Glide.with(getContext()).load(Common.categorySelected.getImage()).into(category_image);

        category_image.setOnClickListener(view -> {
            Intent intent=new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent,"Select Picture"),PICK_IMAGE_REQUEST);
        });

        builder.setNegativeButton("CANCEL", (dialogInterface, i) -> dialogInterface.dismiss());
        builder.setPositiveButton("UPDATE", (dialogInterface, i) -> {
            Map<String, Object> updateData=new HashMap<>();
            updateData.put("name",edt_category_name.getText().toString());
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
                                updateData.put("image",uri.toString());
                                updateCategory(updateData);
                            });
                        }).addOnProgressListener(taskSnapshot ->{
                            double progress=(100.0 *taskSnapshot.getBytesTransferred()/taskSnapshot.getTotalByteCount());
                            dialog.setMessage("Uploading: "+progress+"%");
                });

            }else {
                updateCategory(updateData);
            }
        });

        builder.setView(itemView);
        AlertDialog dialog=builder.create();
        dialog.show();
    }

    private void updateCategory(Map<String, Object> updateData) {
        FirebaseDatabase.getInstance()
                .getReference(Common.CATEGORY_REF)
                .child(Common.categorySelected.getMenu_id())
                .updateChildren(updateData)
                .addOnFailureListener(e -> Toast.makeText(getContext(),""+e.getMessage(),Toast.LENGTH_SHORT).show())
                .addOnCompleteListener(task -> {
                    categoryViewModel.loadCategories();
                    EventBus.getDefault().postSticky(new ToastEvent(Common.ACTION.UPDATE,true));
                });

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==PICK_IMAGE_REQUEST && resultCode== Activity.RESULT_OK){
            if(data!=null && data.getData()!=null){
                imageUri=data.getData();
                category_image.setImageURI(imageUri);
            }
        }
    }
}