package com.se_p2.hungerbellsserver.adapter;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.se_p2.hungerbellsserver.eventBus.CategoryClick;
import com.se_p2.hungerbellsserver.callback.IRecyclerClickListener;
import com.se_p2.hungerbellsserver.common.Common;
import com.se_p2.hungerbellsserver.model.CategoryModel;
import com.se_p2.hungerbellsserver.R;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class MyCategoriesAdapter extends RecyclerView.Adapter<MyCategoriesAdapter.MyViewHolder> {

    Context context;
    List<CategoryModel> categoryModelList;

    public MyCategoriesAdapter(Context context, List<CategoryModel> categoryModelList) {
        this.context = context;
        this.categoryModelList = categoryModelList;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(context).inflate(R.layout.layout_category_item, parent, false));
    }


    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        //Uri uri=Uri.parse(categoryModelList.get(position).getImage());
        //StorageReference mStorageRef = FirebaseStorage.getInstance().getReference().child(Objects.requireNonNull(uri.getPath()));
        //Glide.with(context).load(mStorageRef).into(holder.category_image);
        Glide.with(context).load(categoryModelList.get(position).getImage()).into(holder.category_image);
        holder.category_name.setText(new StringBuilder(categoryModelList.get(position).getName()));

        //Event listener
        holder.setListener((view, pos) -> {
            Common.categorySelected=categoryModelList.get(pos);
            EventBus.getDefault().postSticky(new CategoryClick(true,categoryModelList.get(pos)));
        });
    }

    @Override
    public int getItemCount() {
        return categoryModelList.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        Unbinder unbinder;
        @BindView(R.id.img_category)
        ImageView category_image;
        @BindView(R.id.txt_category)
        TextView category_name;

        IRecyclerClickListener listener;

        public void setListener(IRecyclerClickListener listener) {
            this.listener = listener;
        }

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            unbinder=ButterKnife.bind(this,itemView);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            listener.onItemClickListener(view,getAdapterPosition());
        }
    }

    @Override
    public int getItemViewType(int position) {
        if(categoryModelList.size()==1)
            return 0;
        else{
            if(categoryModelList.size()%2==0)
                return 0;
            else
                return (position >1 && position == categoryModelList.size()-1) ? 1:0;

        }
    }
}
