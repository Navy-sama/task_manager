/// Runtime configuration resolved from `--dart-define` build arguments.
class AppConfig {
  const AppConfig._();

  /// Base URL of the backend API, without the `/api` suffix.
  ///
  /// Defaults to `10.0.2.2`, the special alias the Android emulator uses to
  /// reach the host machine's `localhost`. Override for a physical device
  /// with `--dart-define=API_BASE_URL=http://<lan-ip>:8080`.
  static const String apiBaseUrl = String.fromEnvironment(
    'API_BASE_URL',
    defaultValue: 'http://10.0.2.2:8080',
  );
}
