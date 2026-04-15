package com.mobflow.workspaceservice.repository;

import com.mobflow.workspaceservice.model.entities.Workspace;
import com.mobflow.workspaceservice.model.entities.WorkspaceMember;
import com.mobflow.workspaceservice.model.enums.WorkspaceRole;
import com.mobflow.workspaceservice.testsupport.AbstractPostgresWorkspaceServiceTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.UUID;

import static com.mobflow.workspaceservice.testsupport.WorkspaceServiceTestFixtures.workspace;
import static com.mobflow.workspaceservice.testsupport.WorkspaceServiceTestFixtures.workspaceMember;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class WorkspaceRepositoryTest extends AbstractPostgresWorkspaceServiceTest {

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Autowired
    private WorkspaceInviteRepository workspaceInviteRepository;

    @Test
    void findAllByMemberAuthId_memberExists_returnsWorkspacesOrderedByCreatedAtDesc() {
        UUID authId = UUID.randomUUID();
        Workspace first = workspace(authId);
        first.setId(null);
        Workspace second = workspace(authId);
        second.setId(null);
        second.setPublicCode("XYZ98765");
        first = workspaceRepository.save(first);
        second = workspaceRepository.save(second);
        WorkspaceMember firstMember = workspaceMember(first, authId, WorkspaceRole.MEMBER);
        firstMember.setId(null);
        WorkspaceMember secondMember = workspaceMember(second, authId, WorkspaceRole.MEMBER);
        secondMember.setId(null);
        workspaceMemberRepository.save(firstMember);
        workspaceMemberRepository.save(secondMember);

        assertThat(workspaceRepository.findAllByMemberAuthId(authId))
                .extracting(Workspace::getId)
                .contains(second.getId(), first.getId());
    }

    @Test
    void findByPublicCode_existingWorkspace_returnsWorkspace() {
        Workspace candidate = workspace(UUID.randomUUID());
        candidate.setId(null);
        Workspace saved = workspaceRepository.save(candidate);

        assertThat(workspaceRepository.findByPublicCode(saved.getPublicCode())).isPresent();
        assertThat(workspaceRepository.existsByPublicCode(saved.getPublicCode())).isTrue();
    }
}
