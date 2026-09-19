import 'user.dart';

/// Response of `register`, `login` and `refresh`. In mobile mode all fields
/// are present; `accessToken`/`refreshToken`/`expiresIn` are only sent to
/// mobile clients (web mode relies on cookies instead).
class AuthResponse {
  const AuthResponse({
    required this.user,
    this.accessToken,
    this.refreshToken,
    this.expiresIn,
  });

  final User user;
  final String? accessToken;
  final String? refreshToken;
  final int? expiresIn;

  factory AuthResponse.fromJson(Map<String, dynamic> json) {
    return AuthResponse(
      user: User.fromJson(json['user'] as Map<String, dynamic>),
      accessToken: json['accessToken'] as String?,
      refreshToken: json['refreshToken'] as String?,
      expiresIn: json['expiresIn'] as int?,
    );
  }
}
