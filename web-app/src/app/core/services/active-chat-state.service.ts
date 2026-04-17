import { Injectable, computed, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class ActiveChatStateService {
  private readonly _activeConversationId = signal<string | null>(null);

  readonly activeConversationId = this._activeConversationId.asReadonly();
  readonly hasActiveConversation = computed(() => this._activeConversationId() !== null);

  setActiveConversation(conversationId: string): void {
    this._activeConversationId.set(conversationId);
  }

  clear(): void {
    this._activeConversationId.set(null);
  }

  isViewingConversation(conversationId: string | null | undefined): boolean {
    return !!conversationId && this._activeConversationId() === conversationId;
  }
}
