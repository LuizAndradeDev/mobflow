import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  Conversation,
  ConversationUpsertResponse,
  MarkConversationReadResponse,
  Message,
} from '../models/chat.model';
import { PageResponse } from '../models/social.model';

@Injectable({ providedIn: 'root' })
export class ChatService {
  private readonly apiPath = '/api/chat/conversations';

  constructor(private readonly http: HttpClient) {}

  listConversations(): Observable<Conversation[]> {
    return this.http.get<Conversation[]>(this.apiPath);
  }

  getConversation(conversationId: string): Observable<Conversation> {
    return this.http.get<Conversation>(`${this.apiPath}/${conversationId}`);
  }

  createOrGetPrivateConversation(targetAuthId: string): Observable<ConversationUpsertResponse> {
    return this.http.post<ConversationUpsertResponse>(`${this.apiPath}/private`, { targetAuthId });
  }

  listMessages(conversationId: string, page = 0, size = 20): Observable<PageResponse<Message>> {
    const params = new HttpParams()
      .set('page', page)
      .set('size', size);

    return this.http.get<PageResponse<Message>>(`${this.apiPath}/${conversationId}/messages`, { params });
  }

  markConversationAsRead(conversationId: string): Observable<MarkConversationReadResponse> {
    return this.http.post<MarkConversationReadResponse>(`${this.apiPath}/${conversationId}/read`, {});
  }
}
