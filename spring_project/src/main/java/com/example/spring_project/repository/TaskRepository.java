package com.example.spring_project.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.spring_project.entity.Task;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

}
