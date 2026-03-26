package com.mobflow.taskservice.model.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateTaskListRequest {

    @Size(min = 1, max = 100, message = "List name must be between 1 and 100 characters")
    private String name;
}
