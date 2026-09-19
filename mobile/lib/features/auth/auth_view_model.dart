import 'package:flutter/foundation.dart';

import '../../core/api_exception.dart';
import 'auth_repository.dart';
import 'models/user.dart';

enum AuthStatus {
  /// Session restoration in progress; show a splash screen.
  unknown,
  authenticated,
  unauthenticated,
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

  /// Restores a previous session, if any. Called once at app start.
  Future<void> bootstrap() async {
    if (!await _repository.hasStoredSession()) {
      _status = AuthStatus.unauthenticated;
      notifyListeners();
      return;
    }
    try {
      _user = await _repository.fetchCurrentUser();
      _status = AuthStatus.authenticated;
    } catch (_) {
      _status = AuthStatus.unauthenticated;
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
