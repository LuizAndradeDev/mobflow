// ADICIONAR ao workspace-service InternalWorkspaceController existente:
//
// Endpoint: GET /internal/workspaces/{workspaceId}/members/{authId}/role
// Retorna a role do membro no workspace, ou 404 se não for membro.
//
// -----------------------------------------------------------------------
// Arquivo: workspace-service/.../controller/InternalWorkspaceController.java
// Adicione o método abaixo à classe existente:

/*
    @GetMapping("/workspaces/{workspaceId}/members/{authId}/role")
    public ResponseEntity<MemberRoleResponseDTO> getMemberRole(
            @PathVariable UUID workspaceId,
            @PathVariable UUID authId,
            @RequestHeader(INTERNAL_SECRET_HEADER) String secret
    ) {
        if (!internalSecret.equals(secret)) return ResponseEntity.status(403).build();

        WorkspaceMember member = workspaceMemberRepository
                .findByWorkspaceIdAndAuthId(workspaceId, authId)
                .orElseThrow(() -> new WorkspaceServiceException(ErrorType.MEMBER_NOT_FOUND, HttpStatus.NOT_FOUND));

        return ResponseEntity.ok(MemberRoleResponseDTO.of(member.getRole().name()));
    }
*/

// O WorkspaceMemberRepository já tem findByWorkspaceIdAndAuthId conforme a arquitetura existente.
// Basta injetar o repositório e adicionar o método acima.
