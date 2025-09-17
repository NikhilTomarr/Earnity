package com.nikhil.earnity.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.nikhil.earnity.R
import com.nikhil.earnity.models.DailyTask

class DailyTaskAdapter(
    private val tasks: List<DailyTask>,
    private val onTaskClicked: (DailyTask) -> Unit
) : RecyclerView.Adapter<DailyTaskAdapter.TaskViewHolder>() {

    class TaskViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val taskTitle: TextView = view.findViewById(R.id.tvTaskTitle)
        val taskDescription: TextView = view.findViewById(R.id.tvTaskDescription)
        val taskReward: TextView = view.findViewById(R.id.tvTaskReward)
        val btnCompleteTask: Button = view.findViewById(R.id.btnCompleteTask)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_daily_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]

        holder.taskTitle.text = task.title
        holder.taskDescription.text = task.description
        holder.taskReward.text = "₹${task.reward}"

        if (task.isCompleted) {
            holder.btnCompleteTask.text = "Completed"
            holder.btnCompleteTask.isEnabled = false
        } else {
            holder.btnCompleteTask.text = if (task.isAdInstallTask) "Install App" else "Watch Video"
            holder.btnCompleteTask.isEnabled = true
            holder.btnCompleteTask.setOnClickListener {
                onTaskClicked(task)
            }
        }
    }

    override fun getItemCount() = tasks.size
}