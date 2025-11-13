package com.example.armazenadorsenha.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.armazenadorsenha.dao.PasswordData
import com.example.armazenadorsenha.databinding.ItemPasswordEntryBinding


class PasswordAdapter(
    private val onViewClicked: (PasswordData) -> Unit
) : ListAdapter<PasswordData, PasswordAdapter.PasswordViewHolder>(PasswordDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PasswordViewHolder {
        val binding = ItemPasswordEntryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PasswordViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PasswordViewHolder, position: Int) {
        holder.bind(getItem(position), onViewClicked)
    }

    class PasswordViewHolder(private val binding: ItemPasswordEntryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(entry: PasswordData, onViewClicked: (PasswordData) -> Unit) {

            binding.textServiceTitle.text = entry.serviceTitle
            binding.textUsername.text = entry.username

            binding.btnViewPassword.setOnClickListener {
                onViewClicked(entry)
            }
        }
    }
}

class PasswordDiffCallback : DiffUtil.ItemCallback<PasswordData>() {
    override fun areItemsTheSame(oldItem: PasswordData, newItem: PasswordData): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: PasswordData, newItem: PasswordData): Boolean {
        return oldItem.serviceTitle == newItem.serviceTitle &&
                oldItem.username == newItem.username &&
                oldItem.encryptedPasswordBase64 == newItem.encryptedPasswordBase64
    }
}