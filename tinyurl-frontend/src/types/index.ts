export interface TokenRequest {
  userId: string;
}

export interface TokenResponse {
  token: string;
  userId: string;
}

export interface CreateUrlRequest {
  url: string;
}

export interface CreateUrlResponse {
  code: string;
  shortUrl: string;
  url: string;
  createdAt: string;
  existed: boolean;
}

export interface UrlItem {
  code: string;
  shortUrl: string;
  url: string;
  createdAt: string;
}

export interface ApiError {
  error: string;
  code: string;
}