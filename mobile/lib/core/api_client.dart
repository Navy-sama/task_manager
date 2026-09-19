import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';

import 'auth_interceptor.dart';
import 'config.dart';
import 'token_storage.dart';

/// Owns the app's [Dio] instance and its auth wiring.
///
/// A second, interceptor-free [Dio] ([_refreshDio]) is used exclusively for
/// the `/auth/refresh` call itself, so refreshing never recurses back into
/// [AuthInterceptor.onError].
class ApiClient {
  ApiClient({required TokenStorage tokenStorage}) {
    final baseOptions = BaseOptions(
      baseUrl: '${AppConfig.apiBaseUrl}/api',
      connectTimeout: const Duration(seconds: 10),
      receiveTimeout: const Duration(seconds: 10),
      headers: const {'X-Client-Type': 'mobile'},
    );
    dio = Dio(baseOptions);
    _refreshDio = Dio(baseOptions);
    dio.interceptors.add(
      AuthInterceptor(
        dio: dio,
        refreshDio: _refreshDio,
        tokenStorage: tokenStorage,
        onSessionExpired: () => onSessionExpired?.call(),
      ),
    );
  }

  late final Dio dio;
  late final Dio _refreshDio;

  /// Set by the app's composition root; called when a refresh fails and the
  /// user must be signed out.
  VoidCallback? onSessionExpired;
}
