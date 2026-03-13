import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { SignupRequest, AuthResponse } from '../models/auth.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly API = 'http://localhost:8080/api/auth';

  constructor(private http: HttpClient) {}

  register(data: SignupRequest) {
    return this.http.post<AuthResponse>(`${this.API}/signup`, data);
  }
}
