import 'dart:convert';
import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/core/auth_interceptor.dart';
import 'package:mobile/core/token_storage.dart';

/// In-memory [TokenStorage] fake, so tests never touch the platform secure
/// storage channel.
class InMemoryTokenStorage implements TokenStorage {
  String? accessToken;
  String? refreshToken;

  @override
  Future<void> saveTokens({
    required String accessToken,
    required String refreshToken,
  }) async {
    this.accessToken = accessToken;
    this.refreshToken = refreshToken;
  }

  @override
  Future<String?> readAccessToken() async => accessToken;

  @override
  Future<String?> readRefreshToken() async => refreshToken;

  @override
  Future<void> clear() async {
    accessToken = null;
    refreshToken = null;
  }
}

/// A scripted fake server. `/tasks` accepts only requests bearing the
/// current [validAccessToken]; anything else gets a `401`. `/auth/refresh`
/// always succeeds unless [refreshSucceeds] is false, in which case it
/// returns a `401 INVALID_REFRESH_TOKEN`.
class ScriptedAdapter implements HttpClientAdapter {
  ScriptedAdapter({this.refreshSucceeds = true, this.tasksAlwaysFail = false});

  String validAccessToken = 'unset';
  bool refreshSucceeds;

  /// Simulates a `/tasks` endpoint that keeps rejecting every token, even a
  /// freshly refreshed one (e.g. a broken deployment) — used to assert the
  /// interceptor gives up after one retry instead of looping.
  bool tasksAlwaysFail;
  int refreshCalls = 0;
  int tasksCalls = 0;

  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) async {
    if (options.path == '/auth/refresh') {
      refreshCalls++;
      if (!refreshSucceeds) {
        return _json(401, {
          'code': 'INVALID_REFRESH_TOKEN',
          'detail': 'Refresh failed.',
        });
      }
      validAccessToken = 'refreshed-$refreshCalls';
      return _json(200, {
        'user': {'id': 1, 'email': 'a@b.com'},
        'accessToken': validAccessToken,
        'refreshToken': 'new-refresh-$refreshCalls',
      });
    }
    if (options.path == '/tasks') {
      tasksCalls++;
      final header = options.headers['Authorization'];
      if (!tasksAlwaysFail && header == 'Bearer $validAccessToken') {
        return _json(200, {
          'content': [],
          'page': 0,
          'size': 20,
          'totalElements': 0,
          'totalPages': 0,
        });
      }
      return _json(401, {
        'code': 'UNAUTHENTICATED',
        'detail': 'Access token expired.',
      });
    }
    throw UnimplementedError('Unhandled path: ${options.path}');
  }

  @override
  void close({bool force = false}) {}

  ResponseBody _json(int status, Map<String, dynamic> body) {
    return ResponseBody.fromString(
      jsonEncode(body),
      status,
      headers: {
        Headers.contentTypeHeader: [Headers.jsonContentType],
      },
    );
  }
}

void main() {
  late InMemoryTokenStorage tokenStorage;
  late ScriptedAdapter adapter;
  late Dio dio;
  late Dio refreshDio;
  late int sessionExpiredCalls;

  void wireInterceptor() {
    dio.interceptors.add(
      AuthInterceptor(
        dio: dio,
        refreshDio: refreshDio,
        tokenStorage: tokenStorage,
        onSessionExpired: () => sessionExpiredCalls++,
      ),
    );
  }

  setUp(() {
    tokenStorage = InMemoryTokenStorage()
      ..accessToken = 'expired-token'
      ..refreshToken = 'valid-refresh-token';
    adapter = ScriptedAdapter();
    sessionExpiredCalls = 0;
    final options = BaseOptions(baseUrl: 'https://api.test');
    dio = Dio(options)..httpClientAdapter = adapter;
    refreshDio = Dio(options)..httpClientAdapter = adapter;
  });

  test('attaches the stored bearer token to outgoing requests', () async {
    adapter.validAccessToken = 'expired-token';
    wireInterceptor();

    final response = await dio.get('/tasks');

    expect(response.statusCode, 200);
    expect(adapter.tasksCalls, 1);
  });

  test('refreshes once on a 401 and retries the original request', () async {
    wireInterceptor();

    final response = await dio.get('/tasks');

    expect(response.statusCode, 200);
    expect(adapter.refreshCalls, 1);
    expect(adapter.tasksCalls, 2);
    expect(tokenStorage.accessToken, 'refreshed-1');
  });

  test('concurrent 401s share a single refresh call', () async {
    wireInterceptor();

    final results = await Future.wait([dio.get('/tasks'), dio.get('/tasks')]);

    expect(results.every((r) => r.statusCode == 200), isTrue);
    expect(adapter.refreshCalls, 1);
    expect(adapter.tasksCalls, 4); // 2 initial 401s + 2 successful retries.
  });

  test('a failed refresh clears tokens and notifies session expiry', () async {
    adapter.refreshSucceeds = false;
    wireInterceptor();

    await expectLater(dio.get('/tasks'), throwsA(isA<DioException>()));

    expect(adapter.refreshCalls, 1);
    expect(sessionExpiredCalls, 1);
    expect(tokenStorage.accessToken, isNull);
    expect(tokenStorage.refreshToken, isNull);
  });

  test('does not attempt to refresh a request that was already retried', () async {
    // A 401 that keeps happening after the single retry must surface as an
    // error instead of looping.
    adapter.tasksAlwaysFail = true;
    wireInterceptor();

    await expectLater(dio.get('/tasks'), throwsA(isA<DioException>()));

    // One refresh attempt, one retry: no infinite loop.
    expect(adapter.refreshCalls, 1);
    expect(adapter.tasksCalls, 2);
  });
}
