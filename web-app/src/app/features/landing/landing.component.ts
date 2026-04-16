import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { NavbarPublicComponent } from '../../shared/components/navbar-public/navbar-public.component';

interface LandingMetric {
  label: string;
  value: string;
}

interface LandingCard {
  kicker: string;
  title: string;
  description: string;
}

interface LandingTechnology {
  accent: string;
  badge: string;
  description: string;
  group: string;
  name: string;
}

interface LandingService {
  description: string;
  name: string;
  port: string;
  status: 'Live' | 'Planned';
}

interface LandingListItem {
  description: string;
  name: string;
}

interface LandingRoadmapItem {
  description: string;
  status: string;
  title: string;
}

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [RouterLink, NavbarPublicComponent],
  templateUrl: './landing.component.html',
})
export class LandingComponent {
  readonly heroMetrics: LandingMetric[] = [
    { label: 'live services', value: '5' },
    { label: 'frontends', value: '1' },
    { label: 'infra services', value: '7' },
    { label: 'dockerized', value: '100%' },
  ];

  readonly highlights: LandingCard[] = [
    {
      kicker: 'Workspace domain',
      title: 'Equipes, convites e papéis',
      description: 'Fluxos completos de owner, admin e member para organizar colaboração real em times.',
    },
    {
      kicker: 'Task domain',
      title: 'Kanban com boards, listas e tarefas',
      description: 'Gestão de trabalho com prioridades, datas, responsáveis, drag and drop e visão resumida por workspace.',
    },
    {
      kicker: 'Analytics',
      title: 'Indicadores por usuário e workspace',
      description: 'Métricas de tarefas atribuídas, concluídas, vencidas e progresso agregado no frontend.',
    },
    {
      kicker: 'Notifications',
      title: 'Eventos e notificações',
      description: 'Pipeline já integrado com Kafka e MongoDB para consumir eventos e entregar notificações.',
    },
  ];

  readonly currentCapabilities: string[] = [
    'Autenticação JWT e perfil de usuário',
    'Workspaces com membros e convites',
    'Kanban com boards, lists e tasks',
    'Analytics por usuário e por workspace',
    'Notificações persistidas e leitura no frontend',
    'Upload de avatar com MinIO',
  ];

  readonly technologies: LandingTechnology[] = [
    {
      accent: 'linear-gradient(135deg, #2563eb, #06b6d4)',
      badge: 'NG',
      description: 'Standalone components, signals, typed reactive forms, interceptors e build moderno.',
      group: 'Frontend',
      name: 'Angular 21 + TypeScript',
    },
    {
      accent: 'linear-gradient(135deg, #16a34a, #65a30d)',
      badge: 'SB',
      description: 'APIs REST separadas por domínio com segurança, validação, cache e integração entre serviços.',
      group: 'Backend',
      name: 'Java 21 + Spring Boot 3.5',
    },
    {
      accent: 'linear-gradient(135deg, #0f766e, #14b8a6)',
      badge: 'TW',
      description: 'Camada visual utilitária com dark mode, responsividade e composição rápida de layout.',
      group: 'UI',
      name: 'Tailwind CSS 4',
    },
    {
      accent: 'linear-gradient(135deg, #ea580c, #f59e0b)',
      badge: 'PG',
      description: 'Base relacional principal dos serviços de auth, users, workspace e tasks.',
      group: 'Data',
      name: 'PostgreSQL 16',
    },
    {
      accent: 'linear-gradient(135deg, #7c3aed, #a855f7)',
      badge: 'MG',
      description: 'Armazenamento do notification-service para documentos e histórico de leitura.',
      group: 'Data',
      name: 'MongoDB 7',
    },
    {
      accent: 'linear-gradient(135deg, #dc2626, #ef4444)',
      badge: 'KF',
      description: 'Eventos assíncronos entre serviços para fluxos de notificação.',
      group: 'Messaging',
      name: 'Apache Kafka',
    },
    {
      accent: 'linear-gradient(135deg, #0284c7, #38bdf8)',
      badge: 'IO',
      description: 'Armazenamento S3-compatible para avatar e arquivos de mídia.',
      group: 'Storage',
      name: 'MinIO',
    },
    {
      accent: 'linear-gradient(135deg, #111827, #475569)',
      badge: 'OPS',
      description: 'Orquestração local, reverse proxy e isolamento de serviços com Docker Compose e Nginx.',
      group: 'Infra',
      name: 'Docker + Nginx',
    },
  ];

  readonly services: LandingService[] = [
    { description: 'Cadastro, login, confirmação e autenticação JWT.', name: 'auth-service', port: '8080', status: 'Live' },
    { description: 'Perfis, avatar, integração com MinIO e cache.', name: 'user-service', port: '8081', status: 'Live' },
    { description: 'Workspaces, membros, convites e papéis.', name: 'workspace-service', port: '8082', status: 'Live' },
    { description: 'Boards, lists, tasks, drag and drop e summaries.', name: 'task-service', port: '8083', status: 'Live' },
    { description: 'Persistência de notificações e consumo de eventos.', name: 'notification-service', port: '8084', status: 'Live' },
  ];

  readonly infrastructure: LandingListItem[] = [
    { description: 'Reverse proxy e entrega do frontend Angular.', name: 'Nginx' },
    { description: 'Orquestração local de todos os containers da stack.', name: 'Docker Compose' },
    { description: 'Cache distribuído já usado no ecossistema do projeto.', name: 'Redis' },
    { description: 'SMTP local para desenvolvimento e inspeção de e-mails.', name: 'MailHog' },
    { description: 'Broker base do fluxo assíncrono do projeto.', name: 'Zookeeper + Kafka' },
    { description: 'Storage S3-compatible para mídia do usuário.', name: 'MinIO' },
  ];

  readonly roadmap: LandingRoadmapItem[] = [
    {
      description: 'Expandir a malha de eventos e desacoplamento entre serviços de domínio.',
      status: 'planned',
      title: 'Mais fluxos event-driven',
    },
    {
      description: 'Adicionar chat em tempo real sobre infraestrutura dedicada para mensageria instantânea.',
      status: 'planned',
      title: 'chat-service',
    },
    {
      description: 'Observabilidade e métricas operacionais mais profundas para produção.',
      status: 'planned',
      title: 'Tracing e monitoramento',
    },
  ];
}
