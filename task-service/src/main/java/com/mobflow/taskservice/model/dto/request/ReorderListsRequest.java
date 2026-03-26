package com.mobflow.taskservice.model.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class ReorderListsRequest {

    @NotNull(message = "Ordered list IDs are required")
    private List<UUID> orderedIds;
}
