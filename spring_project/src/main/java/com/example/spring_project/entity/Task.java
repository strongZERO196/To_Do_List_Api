package com.example.spring_project.entity;

import java.time.ZonedDateTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Column;
import org.hibernate.annotations.CreationTimestamp;

@Entity
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private boolean completed;

    @Column(name = "created_at")
    @CreationTimestamp
    private ZonedDateTime createdAt;

    // IDのゲッター
    public Long getId() {
        return id;
    }

    // IDのセッター
    public void setId(Long id) {
        this.id = id;
    }

    // タイトルのゲッター
    public String getTitle() {
        return title;
    }

    // タイトルのセッター
    public void setTitle(String title) {
        this.title = title;
    }

    // 完了状態のゲッター
    public boolean isCompleted() {
        return completed;
    }

    // 完了状態のセッター
    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    // createdAtのゲッター（ZonedDateTimeからLocalDateTimeへの変換）
    public LocalDateTime getCreatedAt() {
        return createdAt == null ? null : createdAt.withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
    }

    // createdAtのセッター（LocalDateTimeからZonedDateTimeへの変換）
    public void setCreatedAt(LocalDateTime localDateTime) {
        this.createdAt = localDateTime == null ? null : ZonedDateTime.of(localDateTime, ZoneId.systemDefault());
    }
}
