import 'package:flutter/widgets.dart';

import '../../core/strings.dart';

/// Client-side validation mirroring the backend rules in
/// `docs/api-contract.md` (§ register/login). The server remains the source
/// of truth; this only avoids obviously-doomed round trips.
class AuthValidators {
  const AuthValidators._();

  static final _emailPattern = RegExp(r'^[^@\s]+@[^@\s]+\.[^@\s]+$');

  static String? email(String? value) {
    final trimmed = value?.trim() ?? '';
    if (trimmed.isEmpty) return Strings.emailRequired;
    if (!_emailPattern.hasMatch(trimmed)) return Strings.emailInvalid;
    return null;
  }

  static String? password(String? value) {
    final password = value ?? '';
    if (password.isEmpty) return Strings.passwordRequired;
    if (password.length < 8 || password.length > 72) {
      return Strings.passwordLength;
    }
    return null;
  }

  static String? Function(String?) confirmPassword(
    TextEditingController passwordController,
  ) {
    return (value) {
      if (value != passwordController.text) {
        return Strings.confirmPasswordMismatch;
      }
      return null;
    };
  }
}
