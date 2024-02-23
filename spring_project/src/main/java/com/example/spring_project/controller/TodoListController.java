package com.example.spring_project.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


import com.example.exceptions.ResourceNotFoundException;
import com.example.spring_project.entity.Task;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_project.repository.TaskRepository;



@RestController
@CrossOrigin(origins = {"http://localhost:9000"})
public class TodoListController {

  private TaskRepository taskRepository; // タスクを管理するためのリポジトリ

  public TodoListController(TaskRepository taskRepository) {
    this.taskRepository = taskRepository;
  }

    // タスクの一覧を取得
    @GetMapping("/getAllTasks")
    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    // 新しいタスクを追加
    @PostMapping("/createTask")
    public Task createTask(@RequestBody Task task) {
        return taskRepository.save(task);       
    }

    // タスクを更新
    @PutMapping("/updateTask/{id}")
    public Task updateTask(@PathVariable Long id, @RequestBody Task taskDetails) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found for this id :: " + id));

        task.setTitle(taskDetails.getTitle());
        task.setCompleted(taskDetails.isCompleted());
        return taskRepository.save(task);
    }

    // タスクを削除
    @DeleteMapping("/deleteTask/{id}")
    public Map<String, Boolean> deleteTask(@PathVariable Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found for this id :: " + id));

        taskRepository.delete(task);
        Map<String, Boolean> response = new HashMap<>();
        response.put("deleted", Boolean.TRUE);
        return response;
    }
}