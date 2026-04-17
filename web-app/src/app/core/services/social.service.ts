import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  CreateCommentRequest,
  Friend,
  FriendRequest,
  PageResponse,
  SendFriendRequestRequest,
  SocialComment,
  UpdateCommentRequest,
} from '../models/social.model';

@Injectable({ providedIn: 'root' })
export class SocialService {
  private readonly apiPath = '/social/api';

  constructor(private readonly http: HttpClient) {}

  createTaskComment(taskId: string, data: CreateCommentRequest): Observable<SocialComment> {
    return this.http.post<SocialComment>(`${this.apiPath}/tasks/${taskId}/comments`, data);
  }

  listTaskComments(taskId: string, page = 0, size = 20): Observable<PageResponse<SocialComment>> {
    const params = new HttpParams()
      .set('page', page)
      .set('size', size);

    return this.http.get<PageResponse<SocialComment>>(`${this.apiPath}/tasks/${taskId}/comments`, { params });
  }

  updateComment(commentId: string, data: UpdateCommentRequest): Observable<SocialComment> {
    return this.http.put<SocialComment>(`${this.apiPath}/comments/${commentId}`, data);
  }

  deleteComment(commentId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiPath}/comments/${commentId}`);
  }

  sendFriendRequest(data: SendFriendRequestRequest): Observable<FriendRequest> {
    return this.http.post<FriendRequest>(`${this.apiPath}/friends/request`, data);
  }

  listFriendRequests(): Observable<FriendRequest[]> {
    return this.http.get<FriendRequest[]>(`${this.apiPath}/friends/requests`);
  }

  acceptFriendRequest(requestId: string): Observable<FriendRequest> {
    return this.http.post<FriendRequest>(`${this.apiPath}/friends/${requestId}/accept`, {});
  }

  declineFriendRequest(requestId: string): Observable<FriendRequest> {
    return this.http.post<FriendRequest>(`${this.apiPath}/friends/${requestId}/decline`, {});
  }

  listFriends(): Observable<Friend[]> {
    return this.http.get<Friend[]>(`${this.apiPath}/friends`);
  }
}
