package com.mobflow.workspaceservice.service;

import com.mobflow.workspaceservice.exception.*;
import com.mobflow.workspaceservice.model.dto.request.AddMemberDTO;
import com.mobflow.workspaceservice.model.dto.request.CreateWorkspaceDTO;
import com.mobflow.workspaceservice.model.dto.request.UpdateMemberRoleDTO;
import com.mobflow.workspaceservice.model.dto.request.UpdateWorkspaceDTO;
import com.mobflow.workspaceservice.model.entities.Workspace;
import com.mobflow.workspaceservice.model.entities.WorkspaceMember;
import com.mobflow.workspaceservice.model.enums.WorkspaceRole;
import com.mobflow.workspaceservice.repository.WorkspaceMemberRepository;
import com.mobflow.workspaceservice.repository.WorkspaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    public WorkspaceService(
            WorkspaceRepository workspaceRepository,
            WorkspaceMemberRepository workspaceMemberRepository
    ) {
        this.workspaceRepository = workspaceRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
    }

    @Transactional
    public Workspace createWorkspace(CreateWorkspaceDTO dto, UUID ownerAuthId) {
        Workspace workspace = Workspace.create(dto.getName(), dto.getDescription(), ownerAuthId);
        workspace = workspaceRepository.save(workspace);

        WorkspaceMember owner = WorkspaceMember.create(workspace, ownerAuthId, WorkspaceRole.OWNER);
        workspaceMemberRepository.save(owner);

        return workspace;
    }

    public List<Workspace> listMyWorkspaces(UUID authId) {
        return workspaceRepository.findAllByMemberAuthId(authId);
    }

    public Workspace getWorkspaceById(UUID workspaceId, UUID authId) {
        Workspace workspace = findWorkspaceOrThrow(workspaceId);
        ensureIsMember(workspaceId, authId);
        return workspace;
    }

    @Transactional
    public Workspace updateWorkspace(UUID workspaceId, UpdateWorkspaceDTO dto, UUID authId) {
        Workspace workspace = findWorkspaceOrThrow(workspaceId);
        ensureIsAdminOrOwner(workspaceId, authId);

        if (dto.getName() != null) {
            workspace.setName(dto.getName());
        }
        if (dto.getDescription() != null) {
            workspace.setDescription(dto.getDescription());
        }

        return workspaceRepository.save(workspace);
    }

    @Transactional
    public void deleteWorkspace(UUID workspaceId, UUID authId) {
        findWorkspaceOrThrow(workspaceId);
        ensureIsOwner(workspaceId, authId);
        workspaceMemberRepository.findAllByWorkspaceId(workspaceId)
                .forEach(workspaceMemberRepository::delete);
        workspaceRepository.deleteById(workspaceId);
    }

    @Transactional
    public WorkspaceMember addMember(UUID workspaceId, AddMemberDTO dto, UUID requestingAuthId) {
        Workspace workspace = findWorkspaceOrThrow(workspaceId);
        ensureIsAdminOrOwner(workspaceId, requestingAuthId);

        if (workspaceMemberRepository.existsByWorkspaceIdAndAuthId(workspaceId, dto.getAuthId())) {
            throw new MemberAlreadyExistsException();
        }

        WorkspaceMember member = WorkspaceMember.create(workspace, dto.getAuthId(), WorkspaceRole.MEMBER);
        return workspaceMemberRepository.save(member);
    }

    public List<WorkspaceMember> listMembers(UUID workspaceId, UUID authId) {
        findWorkspaceOrThrow(workspaceId);
        ensureIsMember(workspaceId, authId);
        return workspaceMemberRepository.findAllByWorkspaceId(workspaceId);
    }

    @Transactional
    public void removeMember(UUID workspaceId, UUID targetAuthId, UUID requestingAuthId) {
        findWorkspaceOrThrow(workspaceId);
        ensureIsAdminOrOwner(workspaceId, requestingAuthId);

        WorkspaceMember member = workspaceMemberRepository
                .findByWorkspaceIdAndAuthId(workspaceId, targetAuthId)
                .orElseThrow(MemberNotFoundException::new);

        if (member.getRole() == WorkspaceRole.OWNER) {
            throw new CannotRemoveOwnerException();
        }

        workspaceMemberRepository.delete(member);
    }

    @Transactional
    public WorkspaceMember updateMemberRole(UUID workspaceId, UUID targetAuthId, UpdateMemberRoleDTO dto, UUID requestingAuthId) {
        findWorkspaceOrThrow(workspaceId);
        ensureIsOwner(workspaceId, requestingAuthId);

        WorkspaceMember member = workspaceMemberRepository
                .findByWorkspaceIdAndAuthId(workspaceId, targetAuthId)
                .orElseThrow(MemberNotFoundException::new);

        if (member.getRole() == WorkspaceRole.OWNER) {
            throw new CannotRemoveOwnerException();
        }

        member.setRole(dto.getRole());
        return workspaceMemberRepository.save(member);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Workspace findWorkspaceOrThrow(UUID workspaceId) {
        return workspaceRepository.findById(workspaceId)
                .orElseThrow(WorkspaceNotFoundException::new);
    }

    private WorkspaceMember findMemberOrThrow(UUID workspaceId, UUID authId) {
        return workspaceMemberRepository
                .findByWorkspaceIdAndAuthId(workspaceId, authId)
                .orElseThrow(UnauthorizedWorkspaceActionException::new);
    }

    private void ensureIsMember(UUID workspaceId, UUID authId) {
        if (!workspaceMemberRepository.existsByWorkspaceIdAndAuthId(workspaceId, authId)) {
            throw new UnauthorizedWorkspaceActionException();
        }
    }

    private void ensureIsAdminOrOwner(UUID workspaceId, UUID authId) {
        WorkspaceMember member = findMemberOrThrow(workspaceId, authId);
        if (member.getRole() == WorkspaceRole.MEMBER) {
            throw new UnauthorizedWorkspaceActionException();
        }
    }

    private void ensureIsOwner(UUID workspaceId, UUID authId) {
        WorkspaceMember member = findMemberOrThrow(workspaceId, authId);
        if (member.getRole() != WorkspaceRole.OWNER) {
            throw new UnauthorizedWorkspaceActionException();
        }
    }
}
