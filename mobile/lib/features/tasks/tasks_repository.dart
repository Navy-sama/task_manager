import 'package:dio/dio.dart';

import '../../core/api_exception.dart';
import 'models/page_response.dart';
import 'models/task.dart';
import 'models/task_status.dart';

abstract class TasksRepository {
  Future<PageResponse<Task>> fetchTasks({
    TaskStatus? status,
    String? query,
    required int page,
    required int size,
  });

  Future<Task> createTask({
    required String title,
    String? description,
    TaskStatus status,
  });

  Future<Task> updateTask({
    required int id,
    required String title,
    String? description,
    required TaskStatus status,
  });

  Future<void> deleteTask(int id);
}

class DioTasksRepository implements TasksRepository {
  DioTasksRepository(this._dio);

  final Dio _dio;

  @override
  Future<PageResponse<Task>> fetchTasks({
    TaskStatus? status,
    String? query,
    required int page,
    required int size,
  }) async {
    try {
      final response = await _dio.get<Map<String, dynamic>>(
        '/tasks',
        queryParameters: {
          if (status != null) 'status': status.apiValue,
          if (query != null && query.trim().isNotEmpty) 'q': query.trim(),
          'page': page,
          'size': size,
        },
      );
      return PageResponse.fromJson(response.data!, Task.fromJson);
    } on DioException catch (e) {
      throw ApiException.fromDioError(e);
    }
  }

  @override
  Future<Task> createTask({
    required String title,
    String? description,
    TaskStatus status = TaskStatus.todo,
  }) async {
    try {
      final response = await _dio.post<Map<String, dynamic>>(
        '/tasks',
        data: {
          'title': title,
          'description': description,
          'status': status.apiValue,
        },
      );
      return Task.fromJson(response.data!);
    } on DioException catch (e) {
      throw ApiException.fromDioError(e);
    }
  }

  @override
  Future<Task> updateTask({
    required int id,
    required String title,
    String? description,
    required TaskStatus status,
  }) async {
    try {
      final response = await _dio.put<Map<String, dynamic>>(
        '/tasks/$id',
        data: {
          'title': title,
          'description': description,
          'status': status.apiValue,
        },
      );
      return Task.fromJson(response.data!);
    } on DioException catch (e) {
      throw ApiException.fromDioError(e);
    }
  }

  @override
  Future<void> deleteTask(int id) async {
    try {
      await _dio.delete<void>('/tasks/$id');
    } on DioException catch (e) {
      throw ApiException.fromDioError(e);
    }
  }
}
