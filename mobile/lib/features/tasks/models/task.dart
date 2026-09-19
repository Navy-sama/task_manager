import 'task_status.dart';

class Task {
  const Task({
    required this.id,
    required this.title,
    required this.description,
    required this.status,
    required this.createdAt,
    required this.updatedAt,
  });

  final int id;
  final String title;
  final String? description;
  final TaskStatus status;

  /// ISO-8601 UTC instant, as sent by the API.
  final DateTime createdAt;
  final DateTime updatedAt;

  factory Task.fromJson(Map<String, dynamic> json) {
    return Task(
      id: json['id'] as int,
      title: json['title'] as String,
      description: json['description'] as String?,
      status: TaskStatus.fromApiValue(json['status'] as String),
      createdAt: DateTime.parse(json['createdAt'] as String),
      updatedAt: DateTime.parse(json['updatedAt'] as String),
    );
  }

  /// Body for `POST /api/tasks` and `PUT /api/tasks/{id}`.
  Map<String, dynamic> toRequestJson() {
    return {'title': title, 'description': description, 'status': status.apiValue};
  }
}
