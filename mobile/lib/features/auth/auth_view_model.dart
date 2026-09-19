import 'package:flutter/foundation.dart';

import '../../core/api_exception.dart';
import '../../core/strings.dart';
import 'auth_repository.dart';
import 'models/user.dart';

enum AuthStatus {
  /// Session restoration in progress; show a splash screen.
  unknown,
  authenticated,
  unauthenticated,

  /// Session restoration failed for a recoverable reason (network/server
  /// error) rather than an invalid session. Tokens are kept; the UI should
  /// offer a way to retry [AuthViewModel.bootstrap] instead of logging out.
  error,
}

class AuthViewModel extends ChangeNotifier {
  AuthViewModel({required AuthRepository repository})
    : _repository = repository;

  final AuthRepository _repository;

  AuthStatus _status = AuthStatus.unknown;
  User? _user;
  String? _errorMessage;
  bool _isSubmitting = false;

  AuthStatus get status => _status;
  User? get user => _user;
  String? get errorMessage => _errorMessage;
  bool get isSubmitting => _isSubmitting;

  /// Restores a previous session, if any. Called once at app start, and
  /// again by the "Retry" action if it lands on [AuthStatus.error].
  ///
  /// Only treats the session as invalid (clearing it, per [ApiException]'s
  /// own handling) on a `401` — meaning a token refresh was already
  /// attempted and failed — or when no tokens are stored at all. Any other
  /// failure (no connectivity, a `5xx`) is a recoverable condition: tokens
  /// are left untouched and [AuthStatus.error] is exposed instead, so the
  /// UI can offer a retry rather than bouncing the user to the login screen.
  Future<void> bootstrap() async {
    if (!await _repository.hasStoredSession()) {
      _status = AuthStatus.unauthenticated;
      notifyListeners();
      return;
    }
    try {
      _user = await _repository.fetchCurrentUser();
      _status = AuthStatus.authenticated;
      _errorMessage = null;
    } on ApiException catch (e) {
      if (e.statusCode == 401) {
        _status = AuthStatus.unauthenticated;
      } else {
        _status = AuthStatus.error;
        _errorMessage = e.detail;
      }
    } catch (_) {
      _status = AuthStatus.error;
      _errorMessage = Strings.somethingWentWrong;
    }
    notifyListeners();
  }

  Future<bool> login(String email, String password) async {
    return _submit(() => _repository.login(email, password));
  }

  Future<bool> register(String email, String password) async {
    return _submit(() => _repository.register(email, password));
  }

  Future<bool> _submit(Future<User> Function() action) async {
    _isSubmitting = true;
    _errorMessage = null;
    notifyListeners();
    try {
      _user = await action();
      _status = AuthStatus.authenticated;
      return true;
    } on ApiException catch (e) {
      _errorMessage = e.detail;
      return false;
    } finally {
      _isSubmitting = false;
      notifyListeners();
    }
  }

  Future<void> logout() async {
    await _repository.logout();
    _user = null;
    _status = AuthStatus.unauthenticated;
    notifyListeners();
  }

  /// Invoked by [ApiClient.onSessionExpired] when a token refresh fails.
  void handleSessionExpired() {
    if (_status != AuthStatus.authenticated) return;
    _user = null;
    _status = AuthStatus.unauthenticated;
    notifyListeners();
  }
}
