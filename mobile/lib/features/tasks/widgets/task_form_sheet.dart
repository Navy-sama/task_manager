import 'package:flutter/material.dart';

import '../../../core/strings.dart';
import '../models/task.dart';
import '../models/task_status.dart';

/// Bottom sheet used for both creating and editing a task, so the two flows
/// share one form implementation. Pass [existingTask] to edit.
class TaskFormSheet extends StatefulWidget {
  const TaskFormSheet({super.key, this.existingTask, required this.onSubmit});

  final Task? existingTask;

  /// Returns true on success; the sheet stays open with an error on failure.
  final Future<bool> Function({
    required String title,
    String? description,
    required TaskStatus status,
  })
  onSubmit;

  @override
  State<TaskFormSheet> createState() => _TaskFormSheetState();
}

class _TaskFormSheetState extends State<TaskFormSheet> {
  final _formKey = GlobalKey<FormState>();
  late final TextEditingController _titleController;
  late final TextEditingController _descriptionController;
  late TaskStatus _status;
  bool _isSubmitting = false;
  String? _errorMessage;

  @override
  void initState() {
    super.initState();
    final task = widget.existingTask;
    _titleController = TextEditingController(text: task?.title ?? '');
    _descriptionController = TextEditingController(
      text: task?.description ?? '',
    );
    _status = task?.status ?? TaskStatus.todo;
  }

  @override
  void dispose() {
    _titleController.dispose();
    _descriptionController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!(_formKey.currentState?.validate() ?? false)) return;
    setState(() {
      _isSubmitting = true;
      _errorMessage = null;
    });
    final description = _descriptionController.text.trim();
    final success = await widget.onSubmit(
      title: _titleController.text.trim(),
      description: description.isEmpty ? null : description,
      status: _status,
    );
    if (!mounted) return;
    if (success) {
      Navigator.of(context).pop();
    } else {
      setState(() {
        _isSubmitting = false;
        _errorMessage = Strings.somethingWentWrong;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final isEditing = widget.existingTask != null;
    return Padding(
      padding: EdgeInsets.only(
        left: 16,
        right: 16,
        top: 16,
        bottom: MediaQuery.of(context).viewInsets.bottom + 16,
      ),
      child: Form(
        key: _formKey,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text(
              isEditing ? Strings.editTask : Strings.newTask,
              style: Theme.of(context).textTheme.titleLarge,
            ),
            const SizedBox(height: 16),
            TextFormField(
              controller: _titleController,
              textInputAction: TextInputAction.next,
              decoration: const InputDecoration(
                labelText: Strings.title,
                border: OutlineInputBorder(),
              ),
              validator: (value) => (value == null || value.trim().isEmpty)
                  ? Strings.titleRequired
                  : null,
            ),
            const SizedBox(height: 16),
            TextFormField(
              controller: _descriptionController,
              minLines: 2,
              maxLines: 4,
              decoration: const InputDecoration(
                labelText: Strings.description,
                border: OutlineInputBorder(),
              ),
            ),
            const SizedBox(height: 16),
            DropdownButtonFormField<TaskStatus>(
              value: _status,
              decoration: const InputDecoration(
                labelText: Strings.status,
                border: OutlineInputBorder(),
              ),
              items: TaskStatus.values
                  .map(
                    (status) => DropdownMenuItem(
                      value: status,
                      child: Text(_statusLabel(status)),
                    ),
                  )
                  .toList(),
              onChanged: (value) {
                if (value != null) setState(() => _status = value);
              },
            ),
            if (_errorMessage != null) ...[
              const SizedBox(height: 12),
              Text(
                _errorMessage!,
                style: TextStyle(color: Theme.of(context).colorScheme.error),
              ),
            ],
            const SizedBox(height: 24),
            FilledButton(
              onPressed: _isSubmitting ? null : _submit,
              child: _isSubmitting
                  ? const SizedBox(
                      height: 20,
                      width: 20,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    )
                  : const Text(Strings.save),
            ),
          ],
        ),
      ),
    );
  }
}

String _statusLabel(TaskStatus status) => switch (status) {
  TaskStatus.todo => Strings.filterTodo,
  TaskStatus.inProgress => Strings.filterInProgress,
  TaskStatus.done => Strings.filterDone,
};
