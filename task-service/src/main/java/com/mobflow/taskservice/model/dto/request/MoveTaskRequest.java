package com.mobflow.taskservice.model.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class MoveTaskRequest {

    @NotNull(message = "Target list ID is required")
    private UUID targetListId;

    @Min(value = 0, message = "Position must be zero or greater")
    private int position;
}
