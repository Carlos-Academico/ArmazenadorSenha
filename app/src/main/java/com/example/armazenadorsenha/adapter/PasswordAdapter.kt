package com.example.armazenadorsenha.adapter

import android.content.Context // NOVO: Importar Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load // NOVO: Importar Coil
import com.example.armazenadorsenha.R // NOVO: Importar R para acessar Drawables
import com.example.armazenadorsenha.dao.PasswordData
import com.example.armazenadorsenha.databinding.ItemPasswordEntryBinding
import java.util.Locale


class PasswordAdapter(
    private val context: Context,
    private val onViewClicked: (PasswordData) -> Unit
) : ListAdapter<PasswordData, PasswordAdapter.PasswordViewHolder>(PasswordDiffCallback()) {

    private var fullList = listOf<PasswordData>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PasswordViewHolder {
        val binding = ItemPasswordEntryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PasswordViewHolder(context, binding, onViewClicked)
    }

    override fun onBindViewHolder(holder: PasswordViewHolder, position: Int) {
        holder.bind(getItem(position))
    }


    fun updateFullList(list: List<PasswordData>) {
        fullList = list
        super.submitList(list)
    }

    fun filter(query: String?) {
        val filteredList = if (query.isNullOrBlank()) {
            fullList
        } else {
            val lowerCaseQuery = query.lowercase(Locale.ROOT)
            fullList.filter { entry ->
                entry.serviceTitle.lowercase(Locale.ROOT).contains(lowerCaseQuery) ||
                        entry.username.lowercase(Locale.ROOT).contains(lowerCaseQuery)
            }
        }
        super.submitList(filteredList)
    }

    class PasswordViewHolder(
        private val context: Context, // NOVO: Context
        private val binding: ItemPasswordEntryBinding,
        private val onViewClicked: (PasswordData) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(entry: PasswordData) {

            val iconResId = getIconResId(entry.serviceTitle)

            binding.imageServiceIcon.load(iconResId) {
                placeholder(R.drawable.ic_default_lock)
                error(R.drawable.ic_default_lock)
            }

            binding.textServiceTitle.text = entry.serviceTitle
            binding.textUsername.text = entry.username

            binding.btnViewPassword.setOnClickListener {
                onViewClicked(entry)
            }
        }

        // Mapeamento de icones
        private fun getIconResId(serviceTitle: String): Int {
            return when (serviceTitle.lowercase(Locale.ROOT)) {
                "netflix" -> R.drawable.ic_netflix_logo
                "google", "gmail" -> R.drawable.ic_google_logo
                else -> 0
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