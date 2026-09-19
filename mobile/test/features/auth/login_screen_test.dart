import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/core/api_exception.dart';
import 'package:mobile/features/auth/auth_repository.dart';
import 'package:mobile/features/auth/auth_view_model.dart';
import 'package:mobile/features/auth/login_screen.dart';
import 'package:mobile/features/auth/models/user.dart';
import 'package:provider/provider.dart';

class FakeAuthRepository implements AuthRepository {
  bool loginCalled = false;

  @override
  Future<User> login(String email, String password) async {
    loginCalled = true;
    return const User(id: 1, email: 'a@b.com');
  }

  @override
  Future<User> register(String email, String password) =>
      throw UnimplementedError();

  @override
  Future<void> logout() async {}

  @override
  Future<User> fetchCurrentUser() => throw UnimplementedError();

  @override
  Future<bool> hasStoredSession() async => false;
}

Widget _wrap(FakeAuthRepository repository) {
  return ChangeNotifierProvider<AuthViewModel>(
    create: (_) => AuthViewModel(repository: repository),
    child: const MaterialApp(home: LoginScreen()),
  );
}

void main() {
  testWidgets('shows validation errors and does not submit invalid input', (
    tester,
  ) async {
    final repository = FakeAuthRepository();
    await tester.pumpWidget(_wrap(repository));

    await tester.tap(find.widgetWithText(FilledButton, 'Log in'));
    await tester.pumpAndSettle();

    expect(find.text('Enter your email.'), findsOneWidget);
    expect(find.text('Enter your password.'), findsOneWidget);
    expect(repository.loginCalled, isFalse);
  });

  testWidgets('submits with valid credentials', (tester) async {
    final repository = FakeAuthRepository();
    await tester.pumpWidget(_wrap(repository));

    await tester.enterText(find.byType(TextFormField).first, 'a@b.com');
    await tester.enterText(find.byType(TextFormField).last, 'password123');
    await tester.tap(find.widgetWithText(FilledButton, 'Log in'));
    await tester.pumpAndSettle();

    expect(repository.loginCalled, isTrue);
  });

  testWidgets('password visibility toggle switches obscureText', (
    tester,
  ) async {
    final repository = FakeAuthRepository();
    await tester.pumpWidget(_wrap(repository));

    TextField passwordField() => tester.widget<TextField>(
      find
          .descendant(
            of: find.byType(TextFormField).last,
            matching: find.byType(TextField),
          )
          .first,
    );

    expect(passwordField().obscureText, isTrue);

    await tester.tap(find.byIcon(Icons.visibility));
    await tester.pump();

    expect(passwordField().obscureText, isFalse);
    expect(find.byIcon(Icons.visibility_off), findsOneWidget);
  });

  testWidgets('shows a snackbar with the server error on failed login', (
    tester,
  ) async {
    final repository = _FailingAuthRepository();
    await tester.pumpWidget(_wrap(repository));

    await tester.enterText(find.byType(TextFormField).first, 'a@b.com');
    await tester.enterText(find.byType(TextFormField).last, 'password123');
    await tester.tap(find.widgetWithText(FilledButton, 'Log in'));
    await tester.pumpAndSettle();

    expect(find.text('Invalid email or password.'), findsOneWidget);
  });
}

class _FailingAuthRepository extends FakeAuthRepository {
  @override
  Future<User> login(String email, String password) async {
    loginCalled = true;
    throw const ApiException(
      statusCode: 401,
      code: 'INVALID_CREDENTIALS',
      detail: 'Invalid email or password.',
    );
  }
}
