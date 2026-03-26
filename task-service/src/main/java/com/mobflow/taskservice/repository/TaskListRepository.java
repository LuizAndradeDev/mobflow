package com.mobflow.taskservice.repository;

import com.mobflow.taskservice.model.entities.TaskList;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskListRepository extends JpaRepository<TaskList, UUID> {

    List<TaskList> findByBoardIdOrderByPositionAsc(UUID boardId);

    Optional<TaskList> findByIdAndBoardId(UUID id, UUID boardId);

    int countByBoardId(UUID boardId);
}
