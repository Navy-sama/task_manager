import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../core/strings.dart';
import '../auth/auth_view_model.dart';
import 'models/task.dart';
import 'models/task_status.dart';
import 'tasks_repository.dart';
import 'tasks_view_model.dart';
import 'widgets/task_form_sheet.dart';
import 'widgets/task_list_tile.dart';

class TasksScreen extends StatelessWidget {
  const TasksScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return ChangeNotifierProvider(
      create: (context) =>
          TasksViewModel(repository: context.read<TasksRepository>())
            ..loadInitial(),
      child: const _TasksView(),
    );
  }
}

class _TasksView extends StatefulWidget {
  const _TasksView();

  @override
  State<_TasksView> createState() => _TasksViewState();
}

class _TasksViewState extends State<_TasksView> {
  final _searchController = TextEditingController();
  final _scrollController = ScrollController();
  String? _lastShownError;

  @override
  void initState() {
    super.initState();
    _scrollController.addListener(_onScroll);
  }

  @override
  void dispose() {
    _scrollController.removeListener(_onScroll);
    _scrollController.dispose();
    _searchController.dispose();
    super.dispose();
  }

  void _onScroll() {
    if (!_scrollController.hasClients) return;
    final threshold = _scrollController.position.maxScrollExtent - 200;
    if (_scrollController.position.pixels >= threshold) {
      context.read<TasksViewModel>().loadMore();
    }
  }

  void _maybeShowError(TasksViewModel viewModel) {
    final error = viewModel.errorMessage;
    if (error == null || error == _lastShownError) return;
    _lastShownError = error;
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(error)));
    });
  }

  Future<void> _openTaskForm(TasksViewModel viewModel, {Task? task}) {
    return showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      builder: (_) => TaskFormSheet(
        existingTask: task,
        onSubmit: ({required title, description, required status}) {
          return task == null
              ? viewModel.createTask(
                  title: title,
                  description: description,
                  status: status,
                )
              : viewModel.updateTask(
                  task,
                  title: title,
                  description: description,
                  status: status,
                );
        },
      ),
    );
  }

  Future<bool> _confirmDelete(BuildContext context) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text(Strings.deleteConfirmTitle),
        content: const Text(Strings.deleteConfirmBody),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: const Text(Strings.cancel),
          ),
          TextButton(
            onPressed: () => Navigator.of(context).pop(true),
            child: const Text(Strings.delete),
          ),
        ],
      ),
    );
    return confirmed ?? false;
  }

  @override
  Widget build(BuildContext context) {
    final viewModel = context.watch<TasksViewModel>();
    _maybeShowError(viewModel);

    return Scaffold(
      appBar: AppBar(
        title: const Text(Strings.tasks),
        actions: [
          IconButton(
            icon: const Icon(Icons.logout),
            tooltip: Strings.logout,
            onPressed: () => context.read<AuthViewModel>().logout(),
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () => _openTaskForm(viewModel),
        tooltip: Strings.newTask,
        child: const Icon(Icons.add),
      ),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 8),
            child: TextField(
              controller: _searchController,
              textInputAction: TextInputAction.search,
              decoration: const InputDecoration(
                labelText: Strings.searchTasks,
                prefixIcon: Icon(Icons.search),
                border: OutlineInputBorder(),
              ),
              onChanged: viewModel.setQuery,
            ),
          ),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 12),
            child: SingleChildScrollView(
              scrollDirection: Axis.horizontal,
              child: Row(
                children: [
                  _FilterChip(
                    label: Strings.filterAll,
                    selected: viewModel.statusFilter == null,
                    onSelected: () => viewModel.setStatusFilter(null),
                  ),
                  for (final status in TaskStatus.values)
                    Padding(
                      padding: const EdgeInsets.only(left: 8),
                      child: _FilterChip(
                        label: _statusLabel(status),
                        selected: viewModel.statusFilter == status,
                        onSelected: () => viewModel.setStatusFilter(status),
                      ),
                    ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 8),
          Expanded(child: _buildBody(context, viewModel)),
        ],
      ),
    );
  }

  Widget _buildBody(BuildContext context, TasksViewModel viewModel) {
    if (viewModel.isLoading) {
      return const Center(child: CircularProgressIndicator());
    }
    if (viewModel.isEmpty) {
      return RefreshIndicator(
        onRefresh: viewModel.refresh,
        child: ListView(
          physics: const AlwaysScrollableScrollPhysics(),
          children: [
            const SizedBox(height: 96),
            Center(
              child: Text(
                viewModel.query.isEmpty ? Strings.noTasks : Strings.noTasksMatch,
                style: Theme.of(context).textTheme.bodyLarge,
                textAlign: TextAlign.center,
              ),
            ),
          ],
        ),
      );
    }
    return RefreshIndicator(
      onRefresh: viewModel.refresh,
      child: ListView.separated(
        controller: _scrollController,
        physics: const AlwaysScrollableScrollPhysics(),
        itemCount: viewModel.tasks.length + (viewModel.isLoadingMore ? 1 : 0),
        separatorBuilder: (_, _) => const Divider(height: 1),
        itemBuilder: (context, index) {
          if (index >= viewModel.tasks.length) {
            return const Padding(
              padding: EdgeInsets.symmetric(vertical: 16),
              child: Center(child: CircularProgressIndicator()),
            );
          }
          final task = viewModel.tasks[index];
          return Dismissible(
            key: ValueKey(task.id),
            direction: DismissDirection.endToStart,
            background: Container(
              color: Theme.of(context).colorScheme.errorContainer,
              alignment: Alignment.centerRight,
              padding: const EdgeInsets.symmetric(horizontal: 24),
              child: Icon(
                Icons.delete,
                color: Theme.of(context).colorScheme.onErrorContainer,
              ),
            ),
            confirmDismiss: (_) => _confirmDelete(context),
            onDismissed: (_) => viewModel.deleteTask(task),
            child: TaskListTile(
              task: task,
              onTap: () => _openTaskForm(viewModel, task: task),
            ),
          );
        },
      ),
    );
  }
}

class _FilterChip extends StatelessWidget {
  const _FilterChip({
    required this.label,
    required this.selected,
    required this.onSelected,
  });

  final String label;
  final bool selected;
  final VoidCallback onSelected;

  @override
  Widget build(BuildContext context) {
    return ChoiceChip(
      label: Text(label),
      selected: selected,
      onSelected: (_) => onSelected(),
    );
  }
}

String _statusLabel(TaskStatus status) => switch (status) {
  TaskStatus.todo => Strings.filterTodo,
  TaskStatus.inProgress => Strings.filterInProgress,
  TaskStatus.done => Strings.filterDone,
};
