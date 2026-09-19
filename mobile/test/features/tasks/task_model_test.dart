import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/features/tasks/models/page_response.dart';
import 'package:mobile/features/tasks/models/task.dart';
import 'package:mobile/features/tasks/models/task_status.dart';

void main() {
  group('Task', () {
    test('fromJson parses all fields including UTC instants', () {
      final json = {
        'id': 42,
        'title': 'Write the README',
        'description': 'Screenshots included',
        'status': 'IN_PROGRESS',
        'createdAt': '2026-09-19T08:15:30.123Z',
        'updatedAt': '2026-09-19T09:00:00.000Z',
      };

      final task = Task.fromJson(json);

      expect(task.id, 42);
      expect(task.title, 'Write the README');
      expect(task.description, 'Screenshots included');
      expect(task.status, TaskStatus.inProgress);
      expect(task.createdAt, DateTime.parse('2026-09-19T08:15:30.123Z'));
      expect(task.createdAt.isUtc, isTrue);
      expect(task.updatedAt, DateTime.parse('2026-09-19T09:00:00.000Z'));
    });

    test('fromJson handles a null description', () {
      final task = Task.fromJson({
        'id': 1,
        'title': 'No description',
        'description': null,
        'status': 'TODO',
        'createdAt': '2026-01-01T00:00:00.000Z',
        'updatedAt': '2026-01-01T00:00:00.000Z',
      });

      expect(task.description, isNull);
    });

    test('toRequestJson emits the exact API field names', () {
      final task = Task(
        id: 1,
        title: 'Write the README',
        description: null,
        status: TaskStatus.done,
        createdAt: DateTime.utc(2026),
        updatedAt: DateTime.utc(2026),
      );

      expect(task.toRequestJson(), {
        'title': 'Write the README',
        'description': null,
        'status': 'DONE',
      });
    });
  });

  group('TaskStatus', () {
    test('round-trips through the API wire values', () {
      for (final status in TaskStatus.values) {
        expect(TaskStatus.fromApiValue(status.apiValue), status);
      }
    });

    test('rejects an unknown wire value', () {
      expect(() => TaskStatus.fromApiValue('CANCELLED'), throwsArgumentError);
    });
  });

  group('PageResponse', () {
    test('fromJson decodes content with the provided mapper', () {
      final page = PageResponse.fromJson({
        'content': [
          {
            'id': 1,
            'title': 'A',
            'description': null,
            'status': 'TODO',
            'createdAt': '2026-01-01T00:00:00.000Z',
            'updatedAt': '2026-01-01T00:00:00.000Z',
          },
        ],
        'page': 0,
        'size': 20,
        'totalElements': 1,
        'totalPages': 1,
      }, Task.fromJson);

      expect(page.content, hasLength(1));
      expect(page.content.first.title, 'A');
      expect(page.page, 0);
      expect(page.totalPages, 1);
      expect(page.totalElements, 1);
    });
  });
}
