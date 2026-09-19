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
    final status = context.watch<AuthViewModel>().status;
    return switch (status) {
      AuthStatus.unknown => const Scaffold(
        body: Center(child: CircularProgressIndicator()),
      ),
      AuthStatus.authenticated => const TasksScreen(),
      AuthStatus.unauthenticated => const LoginScreen(),
    };
  }
}
