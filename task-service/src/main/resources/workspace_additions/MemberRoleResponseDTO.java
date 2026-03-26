// ARQUIVO NOVO: workspace-service/src/main/java/com/mobflow/workspaceservice/model/dto/response/MemberRoleResponseDTO.java

package com.mobflow.workspaceservice.model.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MemberRoleResponseDTO {
    private String role;

    public static MemberRoleResponseDTO of(String role) {
        return MemberRoleResponseDTO.builder().role(role).build();
    }
}
