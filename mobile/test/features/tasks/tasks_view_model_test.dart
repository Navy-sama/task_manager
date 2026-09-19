import 'dart:async';

import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/core/api_exception.dart';
import 'package:mobile/features/tasks/models/page_response.dart';
import 'package:mobile/features/tasks/models/task.dart';
import 'package:mobile/features/tasks/models/task_status.dart';
import 'package:mobile/features/tasks/tasks_repository.dart';
import 'package:mobile/features/tasks/tasks_view_model.dart';

Task _task(int id, {TaskStatus status = TaskStatus.todo, String title = 't'}) {
  return Task(
    id: id,
    title: '$title$id',
    description: null,
    status: status,
    createdAt: DateTime.utc(2026),
    updatedAt: DateTime.utc(2026),
  );
}

class FakeTasksRepository implements TasksRepository {
  List<Task> allTasks = [];
  int pageSizeUsed = 0;
  int fetchCalls = 0;
  TaskStatus? lastStatus;
  String? lastQuery;
  ApiException? failNextFetchWith;

  /// When true, `fetchTasks` never resolves on its own: each call appends a
  /// [Completer] to [pendingFetches] that the test completes explicitly, to
  /// control the order in which concurrent requests resolve.
  bool manualCompletion = false;
  final List<Completer<PageResponse<Task>>> pendingFetches = [];

  @override
  Future<PageResponse<Task>> fetchTasks({
    TaskStatus? status,
    String? query,
    required int page,
    required int size,
  }) async {
    fetchCalls++;
    lastStatus = status;
    lastQuery = query;
    pageSizeUsed = size;
    if (failNextFetchWith != null) {
      final error = failNextFetchWith!;
      failNextFetchWith = null;
      throw error;
    }
    if (manualCompletion) {
      final completer = Completer<PageResponse<Task>>();
      pendingFetches.add(completer);
      return completer.future;
    }
    var filtered = allTasks.where((t) => status == null || t.status == status);
    if (query != null && query.isNotEmpty) {
      filtered = filtered.where((t) => t.title.contains(query));
    }
    final list = filtered.toList();
    final start = page * size;
    final end = (start + size).clamp(0, list.length);
    final content = start >= list.length ? <Task>[] : list.sublist(start, end);
    final totalPages = (list.length / size).ceil().clamp(1, 1 << 30);
    return PageResponse(
      content: content,
      page: page,
      size: size,
      totalElements: list.length,
      totalPages: totalPages,
    );
  }

  @override
  Future<Task> createTask({
    required String title,
    String? description,
    TaskStatus status = TaskStatus.todo,
  }) async {
    final task = _task(allTasks.length + 1, status: status, title: title);
    allTasks.insert(0, task);
    return task;
  }

  @override
  Future<Task> updateTask({
    required int id,
    required String title,
    String? description,
    required TaskStatus status,
  }) async {
    final updated = _task(id, status: status, title: title);
    allTasks = [
      for (final t in allTasks) if (t.id == id) updated else t,
    ];
    return updated;
  }

  @override
  Future<void> deleteTask(int id) async {
    allTasks.removeWhere((t) => t.id == id);
  }
}

void main() {
  late FakeTasksRepository repository;
  late TasksViewModel viewModel;

  setUp(() {
    repository = FakeTasksRepository();
    viewModel = TasksViewModel(
      repository: repository,
      searchDebounce: const Duration(milliseconds: 10),
      pageSize: 2,
    );
  });

  test('loadInitial populates tasks and clears loading state', () async {
    repository.allTasks = [_task(1), _task(2)];

    await viewModel.loadInitial();

    expect(viewModel.isLoading, isFalse);
    expect(viewModel.tasks, hasLength(2));
    expect(viewModel.errorMessage, isNull);
  });

  test('setStatusFilter refetches with the new filter and resets paging', () async {
    repository.allTasks = [
      _task(1, status: TaskStatus.todo),
      _task(2, status: TaskStatus.done),
    ];
    await viewModel.loadInitial();

    viewModel.setStatusFilter(TaskStatus.done);
    await Future<void>.delayed(Duration.zero);

    expect(repository.lastStatus, TaskStatus.done);
    expect(viewModel.tasks, hasLength(1));
    expect(viewModel.tasks.single.status, TaskStatus.done);
  });

  test('setQuery debounces and only fetches once after the delay', () async {
    repository.allTasks = [_task(1, title: 'alpha'), _task(2, title: 'beta')];
    await viewModel.loadInitial();
    final fetchCallsBefore = repository.fetchCalls;

    viewModel.setQuery('a');
    viewModel.setQuery('al');
    viewModel.setQuery('alp');
    expect(repository.fetchCalls, fetchCallsBefore); // not yet, still debouncing

    await Future<void>.delayed(const Duration(milliseconds: 30));

    expect(repository.fetchCalls, fetchCallsBefore + 1);
    expect(repository.lastQuery, 'alp');
  });

  test('loadMore appends the next page and stops when exhausted', () async {
    repository.allTasks = [_task(1), _task(2), _task(3)];
    await viewModel.loadInitial(); // page size 2 -> [1, 2], hasMore true

    expect(viewModel.tasks, hasLength(2));
    expect(viewModel.hasMore, isTrue);

    await viewModel.loadMore(); // -> [1, 2, 3]

    expect(viewModel.tasks, hasLength(3));
    expect(viewModel.hasMore, isFalse);

    final fetchCallsBefore = repository.fetchCalls;
    await viewModel.loadMore(); // no-op, already exhausted

    expect(repository.fetchCalls, fetchCallsBefore);
  });

  test('a fetch failure surfaces the error message', () async {
    repository.failNextFetchWith = const ApiException(detail: 'boom');

    await viewModel.loadInitial();

    expect(viewModel.errorMessage, 'boom');
    expect(viewModel.tasks, isEmpty);
  });

  test('deleteTask removes the task optimistically and keeps it removed on success', () async {
    repository.allTasks = [_task(1), _task(2)];
    await viewModel.loadInitial();

    final ok = await viewModel.deleteTask(viewModel.tasks.first);

    expect(ok, isTrue);
    expect(viewModel.tasks, hasLength(1));
  });

  test('does not notify or throw when disposed while a fetch is pending', () async {
    repository.manualCompletion = true;
    final pending = viewModel.loadInitial();
    expect(repository.pendingFetches, hasLength(1));

    viewModel.dispose();

    repository.pendingFetches.single.complete(
      const PageResponse<Task>(
        content: [],
        page: 0,
        size: 2,
        totalElements: 0,
        totalPages: 1,
      ),
    );

    // Would throw (ChangeNotifier disallows notifyListeners after dispose)
    // if the view model didn't guard against notifying post-dispose.
    await pending;
  });

  test(
    'discards a stale response when a newer request resolves first',
    () async {
      repository.manualCompletion = true;

      // Slow request: the initial load.
      unawaited(viewModel.loadInitial());
      expect(repository.pendingFetches, hasLength(1));

      // Fast request: a filter change started before the slow one resolves.
      viewModel.setStatusFilter(TaskStatus.done);
      expect(repository.pendingFetches, hasLength(2));

      // The fast (latest) request resolves first.
      repository.pendingFetches[1].complete(
        PageResponse<Task>(
          content: [_task(2, status: TaskStatus.done)],
          page: 0,
          size: 2,
          totalElements: 1,
          totalPages: 1,
        ),
      );
      await Future<void>.delayed(Duration.zero);

      expect(viewModel.tasks, hasLength(1));
      expect(viewModel.tasks.single.status, TaskStatus.done);
      expect(viewModel.isLoading, isFalse);

      // The slow (stale) request resolves last and must be discarded.
      repository.pendingFetches[0].complete(
        PageResponse<Task>(
          content: [_task(1, status: TaskStatus.todo)],
          page: 0,
          size: 2,
          totalElements: 1,
          totalPages: 1,
        ),
      );
      await Future<void>.delayed(Duration.zero);

      expect(viewModel.tasks, hasLength(1));
      expect(viewModel.tasks.single.status, TaskStatus.done);
    },
  );
}
