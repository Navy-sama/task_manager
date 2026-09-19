import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';

import 'token_storage.dart';

/// Attaches the bearer access token to outgoing requests and transparently
/// refreshes an expired one.
///
/// On a `401` from any endpoint other than `/auth/**`, a single in-flight
/// refresh call is shared between all callers that hit it concurrently (see
/// `docs/api-contract.md`, "Expired access token"). The failed request is
/// then replayed exactly once with the new token. If the refresh itself
/// fails, stored tokens are cleared and [onSessionExpired] is invoked so the
/// app can fall back to the login screen.
class AuthInterceptor extends Interceptor {
  AuthInterceptor({
    required Dio dio,
    required Dio refreshDio,
    required TokenStorage tokenStorage,
    required VoidCallback onSessionExpired,
  }) : _dio = dio,
       _refreshDio = refreshDio,
       _tokenStorage = tokenStorage,
       _onSessionExpired = onSessionExpired;

  final Dio _dio;
  final Dio _refreshDio;
  final TokenStorage _tokenStorage;
  final VoidCallback _onSessionExpired;

  /// Shared by every caller while a refresh is in flight, so concurrent
  /// `401`s trigger exactly one `/auth/refresh` call.
  Future<String?>? _refreshFuture;

  static const _retriedKey = 'auth_interceptor.retried';

  static bool _isAuthEndpoint(String path) => path.startsWith('/auth/');

  @override
  void onRequest(
    RequestOptions options,
    RequestInterceptorHandler handler,
  ) async {
    final token = await _tokenStorage.readAccessToken();
    if (token != null && token.isNotEmpty) {
      options.headers['Authorization'] = 'Bearer $token';
    }
    handler.next(options);
  }

  @override
  void onError(DioException err, ErrorInterceptorHandler handler) async {
    final requestOptions = err.requestOptions;
    final shouldRefresh =
        err.response?.statusCode == 401 &&
        !_isAuthEndpoint(requestOptions.path) &&
        requestOptions.extra[_retriedKey] != true;

    if (!shouldRefresh) {
      handler.next(err);
      return;
    }

    try {
      final newAccessToken = await _refreshAccessToken();
      if (newAccessToken == null) {
        throw StateError('No refresh token available.');
      }
      requestOptions.headers['Authorization'] = 'Bearer $newAccessToken';
      requestOptions.extra = {...requestOptions.extra, _retriedKey: true};
      final response = await _dio.fetch(requestOptions);
      handler.resolve(response);
    } catch (_) {
      await _tokenStorage.clear();
      _onSessionExpired();
      handler.next(err);
    }
  }

  Future<String?> _refreshAccessToken() {
    return _refreshFuture ??= _performRefresh().whenComplete(() {
      _refreshFuture = null;
    });
  }

  Future<String?> _performRefresh() async {
    final refreshToken = await _tokenStorage.readRefreshToken();
    if (refreshToken == null) return null;

    final response = await _refreshDio.post<Map<String, dynamic>>(
      '/auth/refresh',
      data: {'refreshToken': refreshToken},
    );
    final data = response.data;
    final accessToken = data?['accessToken'] as String?;
    final newRefreshToken = data?['refreshToken'] as String?;
    if (accessToken == null || newRefreshToken == null) return null;

    await _tokenStorage.saveTokens(
      accessToken: accessToken,
      refreshToken: newRefreshToken,
    );
    return accessToken;
  }
}
