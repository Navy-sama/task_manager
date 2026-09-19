import 'dart:async';

import 'package:flutter/foundation.dart';

import '../../core/api_exception.dart';
import '../../core/strings.dart';
import 'models/task.dart';
import 'models/task_status.dart';
import 'tasks_repository.dart';

class TasksViewModel extends ChangeNotifier {
  TasksViewModel({
    required TasksRepository repository,
    Duration searchDebounce = const Duration(milliseconds: 300),
    int pageSize = 20,
  }) : _repository = repository,
       _searchDebounce = searchDebounce,
       _pageSize = pageSize;

  final TasksRepository _repository;
  final Duration _searchDebounce;
  final int _pageSize;

  TaskStatus? _statusFilter;
  String _query = '';
  int _page = 0;
  bool _hasMore = true;
  bool _isLoading = false;
  bool _isLoadingMore = false;
  String? _errorMessage;
  List<Task> _tasks = const [];
  Timer? _debounceTimer;

  TaskStatus? get statusFilter => _statusFilter;
  String get query => _query;
  bool get isLoading => _isLoading;
  bool get isLoadingMore => _isLoadingMore;
  bool get hasMore => _hasMore;
  String? get errorMessage => _errorMessage;
  List<Task> get tasks => _tasks;
  bool get isEmpty => _tasks.isEmpty && !_isLoading;

  Future<void> loadInitial() => _fetch(reset: true);

  Future<void> refresh() => _fetch(reset: true);

  Future<void> loadMore() {
    if (_isLoading || _isLoadingMore || !_hasMore) {
      return Future.value();
    }
    return _fetch(reset: false);
  }

  void setStatusFilter(TaskStatus? status) {
    if (_statusFilter == status) return;
    _statusFilter = status;
    loadInitial();
  }

  /// Debounces search input by [_searchDebounce] before refetching, so
  /// keystrokes don't each trigger a request.
  void setQuery(String value) {
    _query = value;
    _debounceTimer?.cancel();
    _debounceTimer = Timer(_searchDebounce, loadInitial);
  }

  Future<bool> createTask({
    required String title,
    String? description,
    TaskStatus status = TaskStatus.todo,
  }) async {
    try {
      await _repository.createTask(
        title: title,
        description: description,
        status: status,
      );
      await refresh();
      return true;
    } on ApiException catch (e) {
      _errorMessage = e.detail;
      notifyListeners();
      return false;
    }
  }

  Future<bool> updateTask(
    Task task, {
    required String title,
    String? description,
    required TaskStatus status,
  }) async {
    try {
      await _repository.updateTask(
        id: task.id,
        title: title,
        description: description,
        status: status,
      );
      await refresh();
      return true;
    } on ApiException catch (e) {
      _errorMessage = e.detail;
      notifyListeners();
      return false;
    }
  }

  Future<bool> deleteTask(Task task) async {
    final previous = _tasks;
    _tasks = previous.where((t) => t.id != task.id).toList();
    notifyListeners();
    try {
      await _repository.deleteTask(task.id);
      return true;
    } on ApiException catch (e) {
      _tasks = previous;
      _errorMessage = e.detail;
      notifyListeners();
      return false;
    }
  }

  Future<void> _fetch({required bool reset}) async {
    if (reset) {
      _page = 0;
      _hasMore = true;
      _isLoading = true;
    } else {
      _isLoadingMore = true;
    }
    _errorMessage = null;
    notifyListeners();

    try {
      final result = await _repository.fetchTasks(
        status: _statusFilter,
        query: _query,
        page: _page,
        size: _pageSize,
      );
      _tasks = reset ? result.content : [..._tasks, ...result.content];
      _page += 1;
      _hasMore = _page < result.totalPages;
    } on ApiException catch (e) {
      _errorMessage = e.detail;
    } catch (_) {
      _errorMessage = Strings.somethingWentWrong;
    } finally {
      _isLoading = false;
      _isLoadingMore = false;
      notifyListeners();
    }
  }

  @override
  void dispose() {
    _debounceTimer?.cancel();
    super.dispose();
  }
}
