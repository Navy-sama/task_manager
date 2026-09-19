import 'package:dio/dio.dart';

import '../../core/api_exception.dart';
import '../../core/token_storage.dart';
import 'models/auth_response.dart';
import 'models/user.dart';

abstract class AuthRepository {
  Future<User> register(String email, String password);
  Future<User> login(String email, String password);
  Future<void> logout();
  Future<User> fetchCurrentUser();

  /// Whether a refresh token is stored, i.e. a session might still be valid.
  /// Used to decide between showing a splash-then-`me` check or going
  /// straight to the login screen at app start.
  Future<bool> hasStoredSession();
}

class DioAuthRepository implements AuthRepository {
  DioAuthRepository({required Dio dio, required TokenStorage tokenStorage})
    : _dio = dio,
      _tokenStorage = tokenStorage;

  final Dio _dio;
  final TokenStorage _tokenStorage;

  @override
  Future<User> register(String email, String password) async {
    return _authenticate(
      '/auth/register',
      {'email': email, 'password': password},
    );
  }

  @override
  Future<User> login(String email, String password) async {
    return _authenticate(
      '/auth/login',
      {'email': email, 'password': password},
    );
  }

  Future<User> _authenticate(String path, Map<String, dynamic> body) async {
    try {
      final response = await _dio.post<Map<String, dynamic>>(
        path,
        data: body,
      );
      final auth = AuthResponse.fromJson(response.data!);
      if (auth.accessToken != null && auth.refreshToken != null) {
        await _tokenStorage.saveTokens(
          accessToken: auth.accessToken!,
          refreshToken: auth.refreshToken!,
        );
      }
      return auth.user;
    } on DioException catch (e) {
      throw ApiException.fromDioError(e);
    }
  }

  @override
  Future<void> logout() async {
    final refreshToken = await _tokenStorage.readRefreshToken();
    try {
      if (refreshToken != null) {
        await _dio.post<void>(
          '/auth/logout',
          data: {'refreshToken': refreshToken},
        );
      }
    } on DioException {
      // Logout is idempotent server-side; a network failure here should
      // never block the local sign-out.
    } finally {
      await _tokenStorage.clear();
    }
  }

  @override
  Future<User> fetchCurrentUser() async {
    try {
      final response = await _dio.get<Map<String, dynamic>>('/auth/me');
      return User.fromJson(response.data!);
    } on DioException catch (e) {
      throw ApiException.fromDioError(e);
    }
  }

  @override
  Future<bool> hasStoredSession() async {
    final refreshToken = await _tokenStorage.readRefreshToken();
    return refreshToken != null;
  }
}
