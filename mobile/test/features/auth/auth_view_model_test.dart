import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/core/api_exception.dart';
import 'package:mobile/features/auth/auth_repository.dart';
import 'package:mobile/features/auth/auth_view_model.dart';
import 'package:mobile/features/auth/models/user.dart';

class FakeAuthRepository implements AuthRepository {
  bool storedSession = true;
  ApiException? failBootstrapWith;

  @override
  Future<bool> hasStoredSession() async => storedSession;

  @override
  Future<User> fetchCurrentUser() async {
    final failure = failBootstrapWith;
    if (failure != null) throw failure;
    return const User(id: 1, email: 'a@b.com');
  }

  @override
  Future<User> login(String email, String password) =>
      throw UnimplementedError();

  @override
  Future<User> register(String email, String password) =>
      throw UnimplementedError();

  @override
  Future<void> logout() async {}
}

void main() {
  group('bootstrap', () {
    test('goes unauthenticated when no session is stored', () async {
      final repository = FakeAuthRepository()..storedSession = false;
      final viewModel = AuthViewModel(repository: repository);

      await viewModel.bootstrap();

      expect(viewModel.status, AuthStatus.unauthenticated);
    });

    test('goes authenticated when the current user is fetched', () async {
      final repository = FakeAuthRepository();
      final viewModel = AuthViewModel(repository: repository);

      await viewModel.bootstrap();

      expect(viewModel.status, AuthStatus.authenticated);
      expect(viewModel.user?.email, 'a@b.com');
    });

    test('goes unauthenticated on a 401 (refresh already failed)', () async {
      final repository = FakeAuthRepository()
        ..failBootstrapWith = const ApiException(
          statusCode: 401,
          detail: 'Session expired.',
        );
      final viewModel = AuthViewModel(repository: repository);

      await viewModel.bootstrap();

      expect(viewModel.status, AuthStatus.unauthenticated);
    });

    test(
      'exposes a recoverable error instead of logging out on a network failure',
      () async {
        final repository = FakeAuthRepository()
          ..failBootstrapWith = const ApiException(
            detail: 'Could not reach the server. Check your connection and try again.',
          );
        final viewModel = AuthViewModel(repository: repository);

        await viewModel.bootstrap();

        expect(viewModel.status, AuthStatus.error);
        expect(
          viewModel.errorMessage,
          'Could not reach the server. Check your connection and try again.',
        );
      },
    );

    test(
      'exposes a recoverable error instead of logging out on a 5xx',
      () async {
        final repository = FakeAuthRepository()
          ..failBootstrapWith = const ApiException(
            statusCode: 503,
            detail: 'Service unavailable.',
          );
        final viewModel = AuthViewModel(repository: repository);

        await viewModel.bootstrap();

        expect(viewModel.status, AuthStatus.error);
        expect(viewModel.errorMessage, 'Service unavailable.');
      },
    );

    test('retrying after a recoverable error can succeed', () async {
      final repository = FakeAuthRepository()
        ..failBootstrapWith = const ApiException(detail: 'boom');
      final viewModel = AuthViewModel(repository: repository);

      await viewModel.bootstrap();
      expect(viewModel.status, AuthStatus.error);

      repository.failBootstrapWith = null;
      await viewModel.bootstrap();

      expect(viewModel.status, AuthStatus.authenticated);
    });
  });
}
