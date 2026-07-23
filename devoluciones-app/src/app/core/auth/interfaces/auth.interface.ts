export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  issuedAt: string;
  expiration: string;
  token: string;
}

export interface AuthErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
}

export interface JwtPayload {
  sub?: string;
  email?: string;
  roles?: string[];
  exp?: number;
  iat?: number;
}
