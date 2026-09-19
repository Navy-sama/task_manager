enum TaskStatus {
  todo,
  inProgress,
  done;

  /// The exact wire value used by the API (query params and JSON bodies).
  String get apiValue => switch (this) {
    TaskStatus.todo => 'TODO',
    TaskStatus.inProgress => 'IN_PROGRESS',
    TaskStatus.done => 'DONE',
  };

  static TaskStatus fromApiValue(String value) {
    return switch (value) {
      'TODO' => TaskStatus.todo,
      'IN_PROGRESS' => TaskStatus.inProgress,
      'DONE' => TaskStatus.done,
      _ => throw ArgumentError('Unknown task status: $value'),
    };
  }
}
