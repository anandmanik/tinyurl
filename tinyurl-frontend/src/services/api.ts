import {
  TokenRequest,
  TokenResponse,
  CreateUrlRequest,
  CreateUrlResponse,
  UrlItem,
  ApiError
} from '../types';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080';

class ApiService {
  private getAuthHeaders(): HeadersInit {
    const token = sessionStorage.getItem('jwt_token');
    return {
      'Content-Type': 'application/json',
      ...(token && { 'Authorization': `Bearer ${token}` })
    };
  }

  private async handleResponse<T>(response: Response): Promise<T> {
    if (!response.ok) {
      const errorData: ApiError = await response.json().catch(() => ({
        error: 'Network error',
        code: 'NETWORK_ERROR'
      }));
      throw new Error(errorData.error);
    }
    return response.json();
  }

  async generateToken(request: TokenRequest): Promise<TokenResponse> {
    const response = await fetch(`${API_BASE_URL}/api/token`, {
      method: 'POST',
      headers: this.getAuthHeaders(),
      body: JSON.stringify(request)
    });

    const result = await this.handleResponse<TokenResponse>(response);

    // Store token in session storage
    sessionStorage.setItem('jwt_token', result.token);
    sessionStorage.setItem('user_id', result.userId);

    return result;
  }

  async createUrl(request: CreateUrlRequest): Promise<CreateUrlResponse> {
    const response = await fetch(`${API_BASE_URL}/api/urls`, {
      method: 'POST',
      headers: this.getAuthHeaders(),
      body: JSON.stringify(request)
    });

    return this.handleResponse<CreateUrlResponse>(response);
  }

  async getUserUrls(): Promise<UrlItem[]> {
    const response = await fetch(`${API_BASE_URL}/api/urls`, {
      method: 'GET',
      headers: this.getAuthHeaders()
    });

    return this.handleResponse<UrlItem[]>(response);
  }

  async deleteUrl(code: string): Promise<void> {
    const response = await fetch(`${API_BASE_URL}/api/urls/${code}`, {
      method: 'DELETE',
      headers: this.getAuthHeaders()
    });

    if (!response.ok) {
      const errorData: ApiError = await response.json().catch(() => ({
        error: 'Failed to delete URL',
        code: 'DELETE_ERROR'
      }));
      throw new Error(errorData.error);
    }
  }

  isAuthenticated(): boolean {
    return !!sessionStorage.getItem('jwt_token');
  }

  getUserId(): string | null {
    return sessionStorage.getItem('user_id');
  }

  logout(): void {
    sessionStorage.removeItem('jwt_token');
    sessionStorage.removeItem('user_id');
  }
}

export const apiService = new ApiService();