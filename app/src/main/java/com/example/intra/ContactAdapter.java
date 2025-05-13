package com.example.intra;

import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.util.List;

public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ContactViewHolder> {
    private final List<Contact> contacts;
    private final OnContactClickListener clickListener;

    public interface OnContactClickListener {
        void onContactClick(Contact contact);
    }

    public ContactAdapter(List<Contact> contacts, OnContactClickListener clickListener) {
        this.contacts = contacts;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_contact, parent, false);
        return new ContactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        Contact contact = contacts.get(position);
        holder.contactName.setText(contact.getName());
        holder.contactEmail.setText(contact.getEmail());

        // Load contact picture using Glide with ProgressBar
        if (contact.getProfilePicURL() != null && !contact.getProfilePicURL().isEmpty()) {
            holder.contactLoadingSpinner.setVisibility(View.VISIBLE);
            Glide.with(holder.contactPicture.getContext())
                    .load(contact.getProfilePicURL())
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .circleCrop()
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            holder.contactLoadingSpinner.setVisibility(View.GONE);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            holder.contactLoadingSpinner.setVisibility(View.GONE);
                            return false;
                        }
                    })
                    .into(holder.contactPicture);
        } else {
            holder.contactLoadingSpinner.setVisibility(View.GONE);
            holder.contactPicture.setImageResource(R.drawable.ic_person);
        }

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onContactClick(contact);
            }
        });
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    static class ContactViewHolder extends RecyclerView.ViewHolder {
        TextView contactName, contactEmail;
        ImageView contactPicture;
        ProgressBar contactLoadingSpinner;

        ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            contactName = itemView.findViewById(R.id.contact_name);
            contactEmail = itemView.findViewById(R.id.contact_email);
            contactPicture = itemView.findViewById(R.id.contact_picture);
            contactLoadingSpinner = itemView.findViewById(R.id.contact_loading_spinner);
        }
    }
}