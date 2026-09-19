import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import 'app.dart';
import 'core/api_client.dart';
import 'core/token_storage.dart';
import 'features/auth/auth_repository.dart';
import 'features/auth/auth_view_model.dart';
import 'features/tasks/tasks_repository.dart';

void main() {
  final tokenStorage = SecureTokenStorage();
  final apiClient = ApiClient(tokenStorage: tokenStorage);
  final authRepository = DioAuthRepository(
    dio: apiClient.dio,
    tokenStorage: tokenStorage,
  );
  final tasksRepository = DioTasksRepository(apiClient.dio);
  final authViewModel = AuthViewModel(repository: authRepository);
  apiClient.onSessionExpired = authViewModel.handleSessionExpired;

  runApp(
    MultiProvider(
      providers: [
        ChangeNotifierProvider<AuthViewModel>.value(value: authViewModel),
        Provider<TasksRepository>.value(value: tasksRepository),
      ],
      child: const TaskManagerApp(),
    ),
  );
}
