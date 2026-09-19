import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import 'core/strings.dart';
import 'features/auth/auth_view_model.dart';
import 'features/auth/login_screen.dart';
import 'features/tasks/tasks_screen.dart';

class TaskManagerApp extends StatelessWidget {
  const TaskManagerApp({super.key});

  @override
  Widget build(BuildContext context) {
    final seedColor = Colors.indigo;
    return MaterialApp(
      title: Strings.appTitle,
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: seedColor),
        useMaterial3: true,
      ),
      darkTheme: ThemeData(
        colorScheme: ColorScheme.fromSeed(
          seedColor: seedColor,
          brightness: Brightness.dark,
        ),
        useMaterial3: true,
      ),
      home: const _AuthGate(),
    );
  }
}

/// Shows a splash screen while a previous session is being restored, then
/// routes to the task list or the login screen based on [AuthViewModel].
class _AuthGate extends StatefulWidget {
  const _AuthGate();

  @override
  State<_AuthGate> createState() => _AuthGateState();
}

class _AuthGateState extends State<_AuthGate> {
  @override
  void initState() {
    super.initState();
    context.read<AuthViewModel>().bootstrap();
  }

  @override
  Widget build(BuildContext context) {
    final viewModel = context.watch<AuthViewModel>();
    return switch (viewModel.status) {
      AuthStatus.unknown => const Scaffold(
        body: Center(child: CircularProgressIndicator()),
      ),
      AuthStatus.authenticated => const TasksScreen(),
      AuthStatus.unauthenticated => const LoginScreen(),
      AuthStatus.error => _BootstrapErrorView(
        message: viewModel.errorMessage ?? Strings.somethingWentWrong,
        onRetry: viewModel.bootstrap,
      ),
    };
  }
}

/// Shown when restoring a previous session failed for a recoverable reason
/// (no connectivity, a server error) — keeps the stored session and lets the
/// user retry instead of dropping them onto the login screen.
class _BootstrapErrorView extends StatelessWidget {
  const _BootstrapErrorView({required this.message, required this.onRetry});

  final String message;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(message, textAlign: TextAlign.center),
              const SizedBox(height: 16),
              FilledButton(onPressed: onRetry, child: const Text(Strings.retry)),
            ],
          ),
        ),
      ),
    );
  }
}
