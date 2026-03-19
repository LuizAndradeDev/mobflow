import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { WorkspaceService } from '../../../../core/services/workspace.service';
import { AlertService } from '../../../../shared/components/alert/service/alert.service';
import { Workspace } from '../../../../core/models/workspace.model';

@Component({
  selector: 'app-workspace-list',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './workspace-list.component.html',
})
export class WorkspaceListComponent implements OnInit {
  workspaces: Workspace[] = [];
  isLoading = true;

  constructor(
    private workspaceService: WorkspaceService,
    private alertService: AlertService,
    private router: Router,
  ) {}

  ngOnInit() {
    this.workspaceService.listMine().subscribe({
      next: (data) => {
        this.workspaces = data;
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
      },
    });
  }

  navigateTo(id: string) {
    this.router.navigate(['/workspaces', id]);
  }
}
