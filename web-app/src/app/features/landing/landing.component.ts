import { NgClass } from '@angular/common';
import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { NavbarPublicComponent } from '../../shared/components/navbar-public/navbar-public.component';

type Highlight = {
  label: string;
  value: string;
  tone: string;
};

type Capability = {
  title: string;
  description: string;
  icon: string;
  tone: string;
  span?: string;
};

type JourneyStep = {
  title: string;
  description: string;
  points: string[];
};

type TechGroup = {
  title: string;
  description: string;
  badges: string[];
};

type ServiceCard = {
  name: string;
  description: string;
  port: string;
};

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [NgClass, RouterLink, NavbarPublicComponent],
  templateUrl: './landing.component.html',
})
export class LandingComponent {
  readonly highlights: Highlight[] = [
    { label: 'Spring services', value: '8', tone: 'blue' },
    { label: 'Realtime + async flows', value: 'WebSocket + Kafka', tone: 'indigo' },
    { label: 'Observability stack', value: 'Prometheus + Grafana', tone: 'emerald' },
  ];

  readonly capabilities: Capability[] = [
    {
      title: 'Collaborative workspaces',
      description:
        'Teams, members, invitations, join codes and role-aware access keep collaboration organized from the first login.',
      icon: 'workspace',
      tone: 'blue',
    },
    {
      title: 'Task execution flow',
      description:
        'Boards, lists, tasks, deadlines, assignees, comments, mentions and analytics cover the full delivery cycle.',
      icon: 'tasks',
      tone: 'emerald',
    },
    {
      title: 'Social and realtime layer',
      description:
        'Friendships, conversations, read receipts and WebSocket updates add a live collaboration layer on top of the core product.',
      icon: 'chat',
      tone: 'indigo',
    },
    {
      title: 'Event-driven notifications',
      description:
        'Kafka-backed consumers generate in-app notifications, unread state and email delivery without blocking user requests.',
      icon: 'notification',
      tone: 'amber',
      span: 'md:col-span-2 xl:col-span-1',
    },
  ];

  readonly journeySteps: JourneyStep[] = [
    {
      title: 'Single public entry point',
      description:
        'Nginx serves the Angular app and forwards API and WebSocket traffic through a consistent browser-facing origin.',
      points: ['Angular 21 SPA', 'Nginx edge routing', 'Light and dark theme support'],
    },
    {
      title: 'Gateway and bounded contexts',
      description:
        'Spring Cloud Gateway centralizes routing, JWT validation, request tracing and rate limiting before traffic reaches each domain service.',
      points: ['API Gateway', 'JWT auth', 'Correlation IDs'],
    },
    {
      title: 'Polyglot persistence and async processing',
      description:
        'Transactional domains stay in PostgreSQL, interaction-heavy services use MongoDB, and Kafka connects side effects across the platform.',
      points: ['PostgreSQL + Redis', 'MongoDB + Kafka', 'Prometheus + Grafana'],
    },
  ];

  readonly techGroups: TechGroup[] = [
    {
      title: 'Frontend',
      description: 'Application shell, UX state and realtime client integration.',
      badges: ['Angular 21', 'TypeScript 5.9', 'Tailwind CSS 4', 'RxJS 7', 'Signals', 'STOMP.js'],
    },
    {
      title: 'Backend',
      description: 'Domain services and platform security across the microservices layer.',
      badges: [
        'Java 21',
        'Spring Boot 3.5',
        'Spring Security',
        'Spring Cloud Gateway',
        'Spring Data JPA',
        'Spring Data MongoDB',
        'WebSocket/STOMP',
      ],
    },
    {
      title: 'Data and messaging',
      description: 'Purpose-fit storage and event distribution for each bounded context.',
      badges: ['PostgreSQL 16', 'MongoDB', 'Redis 7', 'Apache Kafka', 'Zookeeper', 'MinIO'],
    },
    {
      title: 'Platform and ops',
      description: 'Local orchestration, delivery support and runtime visibility.',
      badges: ['Docker', 'Docker Compose', 'Nginx', 'MailHog', 'Prometheus', 'Grafana'],
    },
  ];

  readonly serviceCards: ServiceCard[] = [
    { name: 'auth-service', description: 'Signup, login, JWT issuing and email confirmation flow.', port: '8080' },
    { name: 'user-service', description: 'User profiles, avatar uploads, Redis cache and MinIO integration.', port: '8081' },
    { name: 'workspace-service', description: 'Workspace lifecycle, memberships, roles, invites and join codes.', port: '8082' },
    { name: 'task-service', description: 'Boards, task lists, tasks, analytics, comments and mentions.', port: '8083' },
    { name: 'notification-service', description: 'Kafka consumers, notification persistence and email delivery.', port: '8084' },
    { name: 'social-service', description: 'Friend requests, friendships and collaborative social interactions.', port: '8085' },
    { name: 'chat-service', description: 'Realtime conversations, read receipts and WebSocket messaging.', port: '8086' },
    { name: 'api-gateway', description: 'Central routing, rate limiting, JWT validation and request tracing.', port: '8087' },
  ];

  iconPath(icon: Capability['icon']): string {
    switch (icon) {
      case 'workspace':
        return 'M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10';
      case 'tasks':
        return 'M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4';
      case 'chat':
        return 'M8 10h.01M12 10h.01M16 10h.01M21 12c0 4.418-4.03 8-9 8a9.77 9.77 0 01-4-.8L3 20l1.2-3.2A7.963 7.963 0 013 12c0-4.418 4.03-8 9-8s9 3.582 9 8z';
      default:
        return 'M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.389 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9';
    }
  }

  toneClasses(tone: string): string {
    switch (tone) {
      case 'emerald':
        return 'bg-emerald-100 text-emerald-600 dark:bg-emerald-900/40 dark:text-emerald-400';
      case 'indigo':
        return 'bg-indigo-100 text-indigo-600 dark:bg-indigo-900/40 dark:text-indigo-400';
      case 'amber':
        return 'bg-amber-100 text-amber-600 dark:bg-amber-900/40 dark:text-amber-400';
      default:
        return 'bg-blue-100 text-blue-600 dark:bg-blue-900/40 dark:text-blue-400';
    }
  }
}
