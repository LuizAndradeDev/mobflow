package com.mobflow.workspaceservice.service;

import com.mobflow.workspaceservice.config.UserServiceClient;
import com.mobflow.workspaceservice.exception.*;
import com.mobflow.workspaceservice.model.dto.request.AddMemberByUsernameDTO;
import com.mobflow.workspaceservice.model.dto.request.CreateWorkspaceDTO;
import com.mobflow.workspaceservice.model.dto.request.UpdateMemberRoleDTO;
import com.mobflow.workspaceservice.model.dto.request.UpdateWorkspaceDTO;
import com.mobflow.workspaceservice.model.dto.response.WorkspaceMemberWithProfileDTO;
import com.mobflow.workspaceservice.model.entities.Workspace;
import com.mobflow.workspaceservice.model.entities.WorkspaceMember;
import com.mobflow.workspaceservice.model.enums.WorkspaceRole;
import com.mobflow.workspaceservice.repository.WorkspaceMemberRepository;
import com.mobflow.workspaceservice.repository.WorkspaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class WorkspaceService {

    private static final String CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int CODE_LENGTH = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserServiceClient userServiceClient;

    public WorkspaceService(
            WorkspaceRepository workspaceRepository,
            WorkspaceMemberRepository workspaceMemberRepository,
            UserServiceClient userServiceClient
    ) {
        this.workspaceRepository = workspaceRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.userServiceClient = userServiceClient;
    }

    @Transactional
    public Workspace createWorkspace(CreateWorkspaceDTO dto, UUID ownerAuthId) {
        String publicCode = generateUniqueCode();
        Workspace workspace = Workspace.create(dto.getName(), dto.getDescription(), ownerAuthId, publicCode);
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

    public Workspace getWorkspaceByCode(String code) {
        return workspaceRepository.findByPublicCode(code)
                .orElseThrow(WorkspaceNotFoundException::new);
    }

    @Transactional
    public WorkspaceMember joinByCode(String code, UUID authId) {
        Workspace workspace = workspaceRepository.findByPublicCode(code)
                .orElseThrow(WorkspaceNotFoundException::new);
        if (workspaceMemberRepository.existsByWorkspaceIdAndAuthId(workspace.getId(), authId)) {
            throw new MemberAlreadyExistsException();
        }
        return workspaceMemberRepository.save(WorkspaceMember.create(workspace, authId, WorkspaceRole.MEMBER));
    }

    @Transactional
    public Workspace updateWorkspace(UUID workspaceId, UpdateWorkspaceDTO dto, UUID authId) {
        Workspace workspace = findWorkspaceOrThrow(workspaceId);
        ensureIsAdminOrOwner(workspaceId, authId);
        if (dto.getName() != null) workspace.setName(dto.getName());
        if (dto.getDescription() != null) workspace.setDescription(dto.getDescription());
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
    public WorkspaceMember addMemberByUsername(UUID workspaceId, AddMemberByUsernameDTO dto, UUID requestingAuthId) {
        Workspace workspace = findWorkspaceOrThrow(workspaceId);
        ensureIsAdminOrOwner(workspaceId, requestingAuthId);
        UUID targetAuthId = userServiceClient.resolveAuthIdByUsername(dto.getUsername());
        if (targetAuthId == null) throw new MemberNotFoundException();
        if (workspaceMemberRepository.existsByWorkspaceIdAndAuthId(workspaceId, targetAuthId)) {
            throw new MemberAlreadyExistsException();
        }
        return workspaceMemberRepository.save(WorkspaceMember.create(workspace, targetAuthId, WorkspaceRole.MEMBER));
    }

    public List<WorkspaceMemberWithProfileDTO> listMembersWithProfiles(UUID workspaceId, UUID authId) {
        findWorkspaceOrThrow(workspaceId);
        ensureIsMember(workspaceId, authId);

        List<WorkspaceMember> members = workspaceMemberRepository.findAllByWorkspaceId(workspaceId);

        List<UUID> authIds = members.stream().map(WorkspaceMember::getAuthId).toList();
        Map<UUID, UserServiceClient.UserProfileResponse> profiles =
                userServiceClient.fetchProfilesBatch(authIds);

        return members.stream().map(member -> {
            UserServiceClient.UserProfileResponse profile = profiles.get(member.getAuthId());
            if (profile != null) {
                return WorkspaceMemberWithProfileDTO.fromMemberWithProfile(
                        member, profile.displayName(), profile.avatarUrl());
            }
            return WorkspaceMemberWithProfileDTO.fromMember(member);
        }).toList();
    }

    @Transactional
    public void removeMember(UUID workspaceId, UUID targetAuthId, UUID requestingAuthId) {
        findWorkspaceOrThrow(workspaceId);
        ensureIsAdminOrOwner(workspaceId, requestingAuthId);
        WorkspaceMember member = workspaceMemberRepository
                .findByWorkspaceIdAndAuthId(workspaceId, targetAuthId)
                .orElseThrow(MemberNotFoundException::new);
        if (member.getRole() == WorkspaceRole.OWNER) throw new CannotRemoveOwnerException();
        workspaceMemberRepository.delete(member);
    }

    @Transactional
    public WorkspaceMember updateMemberRole(UUID workspaceId, UUID targetAuthId, UpdateMemberRoleDTO dto, UUID requestingAuthId) {
        findWorkspaceOrThrow(workspaceId);
        ensureIsOwner(workspaceId, requestingAuthId);
        WorkspaceMember member = workspaceMemberRepository
                .findByWorkspaceIdAndAuthId(workspaceId, targetAuthId)
                .orElseThrow(MemberNotFoundException::new);
        if (member.getRole() == WorkspaceRole.OWNER) throw new CannotRemoveOwnerException();
        member.setRole(dto.getRole());
        return workspaceMemberRepository.save(member);
    }

    @Transactional
    public void leaveWorkspace(UUID workspaceId, UUID authId) {
        findWorkspaceOrThrow(workspaceId);
        WorkspaceMember member = workspaceMemberRepository
                .findByWorkspaceIdAndAuthId(workspaceId, authId)
                .orElseThrow(MemberNotFoundException::new);
        if (member.getRole() == WorkspaceRole.OWNER) throw new CannotRemoveOwnerException();
        workspaceMemberRepository.delete(member);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Workspace findWorkspaceOrThrow(UUID workspaceId) {
        return workspaceRepository.findById(workspaceId).orElseThrow(WorkspaceNotFoundException::new);
    }

    private WorkspaceMember findMemberOrThrow(UUID workspaceId, UUID authId) {
        return workspaceMemberRepository.findByWorkspaceIdAndAuthId(workspaceId, authId)
                .orElseThrow(UnauthorizedWorkspaceActionException::new);
    }

    private void ensureIsMember(UUID workspaceId, UUID authId) {
        if (!workspaceMemberRepository.existsByWorkspaceIdAndAuthId(workspaceId, authId))
            throw new UnauthorizedWorkspaceActionException();
    }

    private void ensureIsAdminOrOwner(UUID workspaceId, UUID authId) {
        if (findMemberOrThrow(workspaceId, authId).getRole() == WorkspaceRole.MEMBER)
            throw new UnauthorizedWorkspaceActionException();
    }

    private void ensureIsOwner(UUID workspaceId, UUID authId) {
        if (findMemberOrThrow(workspaceId, authId).getRole() != WorkspaceRole.OWNER)
            throw new UnauthorizedWorkspaceActionException();
    }

    private String generateUniqueCode() {
        String code;
        do { code = generateCode(); } while (workspaceRepository.existsByPublicCode(code));
        return code;
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++)
            sb.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
        return sb.toString();
    }
}
