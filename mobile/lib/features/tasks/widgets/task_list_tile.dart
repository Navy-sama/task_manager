import 'package:flutter/material.dart';

import '../../../core/strings.dart';
import '../models/task.dart';
import '../models/task_status.dart';

class TaskListTile extends StatelessWidget {
  const TaskListTile({super.key, required this.task, required this.onTap});

  final Task task;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return ListTile(
      onTap: onTap,
      title: Text(task.title, maxLines: 1, overflow: TextOverflow.ellipsis),
      subtitle: task.description == null || task.description!.isEmpty
          ? null
          : Text(
              task.description!,
              maxLines: 2,
              overflow: TextOverflow.ellipsis,
            ),
      trailing: _StatusChip(status: task.status),
    );
  }
}

class _StatusChip extends StatelessWidget {
  const _StatusChip({required this.status});

  final TaskStatus status;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final (label, color) = switch (status) {
      TaskStatus.todo => (Strings.filterTodo, scheme.secondaryContainer),
      TaskStatus.inProgress => (Strings.filterInProgress, scheme.tertiaryContainer),
      TaskStatus.done => (Strings.filterDone, scheme.primaryContainer),
    };
    return Chip(
      label: Text(label),
      backgroundColor: color,
      visualDensity: VisualDensity.compact,
      materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
    );
  }
}
