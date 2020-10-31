package com.se_p2.hungerbellsserver.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.se_p2.hungerbellsserver.eventBus.UpdateAddonModel;
import com.se_p2.hungerbellsserver.R;
import com.se_p2.hungerbellsserver.callback.IRecyclerClickListener;
import com.se_p2.hungerbellsserver.model.AddonModel;
import com.se_p2.hungerbellsserver.model.SelectedAddonModel;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class MyAddonAdapter extends RecyclerView.Adapter<MyAddonAdapter.MyViewHolder> {

    Context context;
    List<AddonModel> addonModels;

    UpdateAddonModel updateAddonModel;
    int editPos;

    public MyAddonAdapter(Context context, List<AddonModel> addonModels) {
        this.context = context;
        this.addonModels = addonModels;
        editPos=-1;
        updateAddonModel=new UpdateAddonModel();

    }

    @NonNull
    @Override
    public MyAddonAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyAddonAdapter.MyViewHolder(LayoutInflater.from(context)
                .inflate(R.layout.layout_size_addon_display,parent,false));
    }


    @Override
    public void onBindViewHolder(@NonNull MyAddonAdapter.MyViewHolder holder, int position) {
        holder.txt_name.setText(addonModels.get(position).getName());
        holder.txt_price.setText(String.valueOf(addonModels.get(position).getPrice()));

        holder.img_delete.setOnClickListener(view -> {
            addonModels.remove(position); //delete current item
            notifyItemRemoved(position);
            updateAddonModel.setAddonModels(addonModels);
            EventBus.getDefault().postSticky(updateAddonModel); //Send event
        });

        holder.setListener(new IRecyclerClickListener() {
            @Override
            public void onItemClickListener(View view, int pos) {
                editPos=position;
                EventBus.getDefault().postSticky(new SelectedAddonModel(addonModels.get(pos)));
            }
        });
    }

    @Override
    public int getItemCount() {
        return addonModels.size();
    }

    public void addNewSize(AddonModel addonModel) {
        addonModels.add(addonModel);
        notifyItemInserted(addonModels.size()-1);
        updateAddonModel.setAddonModels(addonModels);
        EventBus.getDefault().postSticky(updateAddonModel);
    }

    public void editSize(AddonModel addonModel) {
        if(editPos!=-1){
            addonModels.set(editPos,addonModel);
            notifyItemChanged(editPos);
            editPos=-1;
            updateAddonModel.setAddonModels(addonModels);
            EventBus.getDefault().postSticky(updateAddonModel);

        }
    }
    public class MyViewHolder extends RecyclerView.ViewHolder{
        @BindView(R.id.txt_name)
        TextView txt_name;
        @BindView(R.id.txt_price)
        TextView txt_price;
        @BindView(R.id.img_delete)
        ImageView img_delete;

        Unbinder unbinder;
        IRecyclerClickListener listener;

        public void setListener(IRecyclerClickListener listener) {
            this.listener = listener;
        }
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            unbinder= ButterKnife.bind(this,itemView);
            itemView.setOnClickListener(view -> listener.onItemClickListener(view,getAdapterPosition()));
        }
    }
}